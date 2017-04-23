# Retrofit源码分析

本文使用的源码基于Parent-2.2.0的版本，建议大家也去github下载 一份源码，跟着本文走一遍。

## 分析方法

源码的分析将从基本的使用方法入手，分析retrofit的实现方案，以及其中涉及到的一些有趣的技巧。

## 简单使用

### 定义HTTP API

```Java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```

### 创建Retrofit并生成API的实现

```Java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```

### 调用API方法，生成Call

```Java
Call<List<Repo>> repos = service.listRepos("octocat");
```

## Retrofit的创建

retrofit实例的创建，使用了`builder`模式，从下面的源码中可以看出。

```Java
public static final class Builder {
	Builder(Platform platform) {
		this.platform = platform;
		converterFactories.add(new BuiltInConverters());
	}
	public Builder() {
		// Platform.get()方法可以用于判断当前的环境
		this(Platform.get());
	}
	public Builder baseUrl(String baseUrl) {
      checkNotNull(baseUrl, "baseUrl == null");
      HttpUrl httpUrl = HttpUrl.parse(baseUrl);
      if (httpUrl == null) {
        throw new IllegalArgumentException("Illegal URL: " + baseUrl);
      }
      return baseUrl(httpUrl);
    }
	
	public Retrofit build() {
      if (baseUrl == null) {
        throw new IllegalStateException("Base URL required.");
      }

      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();// 新建Client，留到之后newCall什么的
      }

      Executor callbackExecutor = this.callbackExecutor;
      if (callbackExecutor == null) {
        callbackExecutor = platform.defaultCallbackExecutor();
      }

      // Make a defensive copy of the adapters and add the default Call adapter.
      List<CallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
      adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

      // Make a defensive copy of the converters.
      List<Converter.Factory> converterFactories = new ArrayList<>(this.converterFactories);

      return new Retrofit(callFactory, baseUrl, converterFactories, adapterFactories,
          callbackExecutor, validateEagerly);
    }
}
```

这里除了builder模式以外，还有两个地方需要关注下，一个是`Platform.get()`方法。它通过`Class.forName`获取类名的方式，来判断当前的环境是否在Android中,这在之后获取默认的`CallAdapterFactory`时候将会用到,对这个方法感兴趣的可以跟过去查看下，这里就不贴了。另一个是在`build()`中创建了`OkHttpClient`。

# retrofit.create

好玩的地方开始了，我们先来看看这个方法。

```Java
public <T> T create(final Class<T> service) {
  Utils.validateServiceInterface(service);
  if (validateEagerly) {
    eagerlyValidateMethods(service);
  }
  // 动态代理，啦啦啦
  return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
      new InvocationHandler() {
        // platform 可以分辨出你是在android，还是java8里面玩耍，又或者别的
        private final Platform platform = Platform.get();
        @Override public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
          // If the method is a method from Object then defer to normal invocation.
          // 这里是个搞事情的invoke，Object方法都走这里，比如equals、toString、hashCode什么的
          if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
          }
          // 有时候java8会来玩玩，他会从这里跑掉
          if (platform.isDefaultMethod(method)) {
            return platform.invokeDefaultMethod(method, service, proxy, args);
          }
          // 解析注解的，这个是正事
          ServiceMethod<Object, Object> serviceMethod =
              (ServiceMethod<Object, Object>) loadServiceMethod(method);
          OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
          return serviceMethod.callAdapter.adapt(okHttpCall);
        }
      });
}
```
可以看出创建API使用了动态代理，根据接口动态生成的代理类，将接口的都转发给了负责连接代理类和委托类的`InvocationHandler`实例，接口方法也都通过其`invoke`方法来处理。
在`invoke`方法中，首先会通过`Platform.get()`方法判断出当前代码的执行环境，之后会先把`Object`和Java8的默认方法进行一个处理，也是在进行后续处理之前进行去噪。

# 创建ServiceMethod

```Java
ServiceMethod<?, ?> loadServiceMethod(Method method) {
  // 从缓存里面取出，如果有的话，直接返回好了
  ServiceMethod<?, ?> result = serviceMethodCache.get(method);
  if (result != null) return result;
  synchronized (serviceMethodCache) {
    result = serviceMethodCache.get(method);
    if (result == null) {
      // 为null的话，解析方法的注解和返回类型、参数的注解he参数类型，新建一个ServiceMethod
      result = new ServiceMethod.Builder<>(this, method).build();// ->
      // 新建的ServiceMethod加到缓存列表里面
      serviceMethodCache.put(method, result);
    }
  }
  return result;
}
```

首先会尝试根据方法从缓存中取出`ServiceMethod`实例，如果没有，在锁保护之后，还有再尝试一次，还是没有的情况下，才会去创建`ServiceMethod`。ServiceMethod的创建于Retrofit类似，都是`builder`模式。ServiceMethod创建的实际流程都放在了最后的`build()`方法中。

```java
public ServiceMethod build() {
  callAdapter = createCallAdapter();// ->获取CallAdapter的实现，一般为ExecutorCallAdapterFactory.get实现
  responseType = callAdapter.responseType();
  if (responseType == Response.class || responseType == okhttp3.Response.class) {
    throw methodError("'"
        + Utils.getRawType(responseType).getName()
        + "' is not a valid response body type. Did you mean ResponseBody?");
  }
  responseConverter = createResponseConverter();// 响应的转换工厂，如GsonConverterFactory
  for (Annotation annotation : methodAnnotations) {
    parseMethodAnnotation(annotation);// 真正解析方法注解的地方来了
  }
  if (httpMethod == null) {
    throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
  }
  if (!hasBody) {// POST方法需要有body或者表单
    if (isMultipart) {
      throw methodError(
          "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
    }
    if (isFormEncoded) {
      throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
          + "request body (e.g., @POST).");
    }
  }
  // 上面是请求方法，下面是请求参数
  int parameterCount = parameterAnnotationsArray.length;
  // ParameterHandler的实现类有很多，包括了各种参数，@Field、@Query等
  parameterHandlers = new ParameterHandler<?>[parameterCount];
  for (int p = 0; p < parameterCount; p++) {
    Type parameterType = parameterTypes[p];// 参数类型
    // 和之前一样的泛型、通配符检查
    if (Utils.hasUnresolvableType(parameterType)) {
      throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
          parameterType);
    }
    Annotation[] parameterAnnotations = parameterAnnotationsArray[p];// 参数的注解集合
    if (parameterAnnotations == null) {
      throw parameterError(p, "No Retrofit annotation found.");
    }
	// 生成了对应的参数注解ParameterHandler实例
    parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
  }
  // 对方法的一些检测
  ...
  
  return new ServiceMethod<>(this);
}
```

## 注解的解析
`CallAdapter`和`Converter`等到后面再分析，这里先看看`parseMethodAnnotation(annotation)`，功能和其名字一样，其对方法注解进行了解析。
```Java
/**
 * 解析方法注解，呜啦啦
 * 通过判断注解类型来解析
 * @param annotation
 */
private void parseMethodAnnotation(Annotation annotation) {
  if (annotation instanceof DELETE) {
    parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
  } else if (annotation instanceof GET) {
    parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
  } 
  // 其他的一些方法注解的解析
  ...
}

private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
  if (this.httpMethod != null) {// 已经赋值过了
    throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
        this.httpMethod, httpMethod);
  }
  this.httpMethod = httpMethod;
  this.hasBody = hasBody;
  // value为设置注解方法时候，设置的值，官方例子中的users/{user}/repos or user
  if (value.isEmpty()) {
    return;
  }
  // 查询条件的一些判断
    ...
  this.relativeUrl = value;
  this.relativeUrlParamNames = parsePathParameters(value);
}
````

在解析注解时，先通过`instanceof`判断出注解的类型，之后调用`parseHttpMethodAndPath`方法解析注解参数值，并设置`httpMethod、relativeUrl、relativeUrlParamNames`等属性。<br>

上面说了API中方法注解的解析，现在来看看方法参数注解的解析，这是通过调用`parseParameterAnnotation`方法生成ParameterHandler实例来实现的，代码比较多，这里挑选@Query来看看。

```Java
else if (annotation instanceof Query) {
Query query = (Query) annotation;
String name = query.value();
boolean encoded = query.encoded();

Class<?> rawParameterType = Utils.getRawType(type);// 返回基础的类
gotQuery = true;
// 可以迭代，Collection
if (Iterable.class.isAssignableFrom(rawParameterType)) {
  if (!(type instanceof ParameterizedType)) {
	throw parameterError(p, rawParameterType.getSimpleName()
		+ " must include generic type (e.g., "
		+ rawParameterType.getSimpleName()
		+ "<String>)");
  }
  ParameterizedType parameterizedType = (ParameterizedType) type;
  Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);// 返回基本类型
  Converter<?, String> converter =
	  retrofit.stringConverter(iterableType, annotations);
  return new ParameterHandler.Query<>(name, converter, encoded).iterable();
} else if (rawParameterType.isArray()) {// Array
  Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());// 如果是基本类型，自动装箱
  Converter<?, String> converter =
	  retrofit.stringConverter(arrayComponentType, annotations);
  return new ParameterHandler.Query<>(name, converter, encoded).array();
} else {// Other
  Converter<?, String> converter =
	  retrofit.stringConverter(type, annotations);
  return new ParameterHandler.Query<>(name, converter, encoded);
}
```
在@Query中，将分成Collection、array、other三种情况处理参数，之后根据这些参数，调用ParameterHandler中的Query静态类，创建出一个ParameterHandler实例。这样循环直到解析了所有的参数注解，组合成为全局变量`parameterHandlers`，之后构建请求时会用到。