# Retrofit源码分析

源码的分析将从基本的使用方法入手，分析retrofit的实现方案，以及其中涉及到的一些有趣的技巧。并且建议大家也去github下载一份源码，跟着本文理一遍基本的流程。

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

retrofit实例的创建，使用了<span id='retrofit__build'>`builder`</span>模式，从下面的源码中可以看出。

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

## retrofit.create

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
在`invoke`方法中，首先会通过`Platform.get()`方法判断出当前代码的执行环境，之后会先把`Object`和Java8的默认方法进行一个处理，也是在进行后续处理之前进行去噪。其中的关键代码其实就是最后三句，这也是这篇文章将要分析的。

### 创建ServiceMethod

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

可以看到在build方法中，对`CallAdapter`与`Converter`进行了创建，这里跟踪之后将会回到`retrofit`类中，在其中将会获取对应列表中的第一个！null对象，之后将会对API的方法和参数注解进行解析。

#### 注解的解析
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
在@Query中，将分成Collection、array、other三种情况处理参数，之后根据这些参数，调用ParameterHandler中的Query静态类，创建出一个ParameterHandler实例。这样循环直到解析了所有的参数注解，组合成为全局变量<span id='parameterHandlers'>`parameterHandlers`<span>，之后构建请求时会用到。

### OkHttpCall

`ServiceMethod`创建完成之后，我们来看看下一行代码中的`OkHttpCall`类，里面的包含了请求的执行和响应处理，我们来看看异步请求的做法。
```Java
OkHttpCall(ServiceMethod<T, ?> serviceMethod, Object[] args) {
  this.serviceMethod = serviceMethod;
  this.args = args;
}

@Override public void enqueue(final Callback<T> callback) {
checkNotNull(callback, "callback == null");

okhttp3.Call call;
Throwable failure;

synchronized (this) {
  if (executed) throw new IllegalStateException("Already executed.");
  executed = true;

  call = rawCall;
  failure = creationFailure;
  if (call == null && failure == null) {
	try {
	  call = rawCall = createRawCall();// 创建OkHttp3.Call
	} catch (Throwable t) {
	  failure = creationFailure = t;
	}
  }
}

if (failure != null) {
  callback.onFailure(this, failure);
  return;
}

if (canceled) {
  call.cancel();
}

call.enqueue(new okhttp3.Callback() {
  @Override public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse)
	  throws IOException {
	Response<T> response;
	try {
	  response = parseResponse(rawResponse);// ->
	} catch (Throwable e) {
	  callFailure(e);
	  return;
	}
	callSuccess(response);
  }

  @Override public void onFailure(okhttp3.Call call, IOException e) {
	try {
	  callback.onFailure(OkHttpCall.this, e);
	} catch (Throwable t) {
	  t.printStackTrace();
	}
  }

  private void callFailure(Throwable e) {
	try {
	  callback.onFailure(OkHttpCall.this, e);
	} catch (Throwable t) {
	  t.printStackTrace();
	}
  }

  private void callSuccess(Response<T> response) {
	try {
	  callback.onResponse(OkHttpCall.this, response);
	} catch (Throwable t) {
	  t.printStackTrace();
	}
  }
});
}


private okhttp3.Call createRawCall() throws IOException {
  Request request = serviceMethod.toRequest(args);// 根据ParameterHandler组装Request.Builder，生成Request
  okhttp3.Call call = serviceMethod.callFactory.newCall(request);// Retrofit中创建的new OkHttpClient().newCall(request)
  ...
  return call;
}

```

首先在构造函数中传入了之前新建的`serviceMethod`和动态代理`invoke`方法传递来的`args`参数。我们来看看其异步方法`enqueue`，将会调用`createRawCall()`方法，跟进来可以看到，做了两件事情，第一件事情，调用`serviceMethod.toRequest`方法，创造出一个`Request`对象，这个`Request`对象就是根据之前提到的方法参数注解的集合<a href = '#parameterHandlers'>`parameterHandlers`</a>创建的。第二件事是创建一个`okhttp3.Call`对象，我们都知道Okhttp中创建这个对象的方法就是newCall，这和上面的代码如出一辙，那么`callFactory`参数是不是就是`OkHttpClient`呢？bingo！确实如此，稍微跟踪一下就可以发现，它的创建出现在<a href='#retrofit__build'>`Retrofit.Builder.build()`</a>方法中，而参数就使用刚刚创建的`request`对象，构成`okhttp3.Call`，并返回。<br>

### CallAdapter

现在来看看`enqueue`传入的参数`callback`,这个参数可能和很多人心中想的并不一样，它并不是用户在使用时传入的那个`Callback`对象。那么他是从哪里来的呢？不知道你还记不记得我之前在<a href='#retrofit__build'>`Retrofit.Builder.build()`</a>方法中提到过一句代码`Platform.get()`。在不使用`addCallAdapterFactory`的情况下。将会使用`Platform`的一种内部类，在Android环境下将会使用到`Android`类（这其实是个策略模式）。
```Java
static class Android extends Platform {
  @Override public Executor defaultCallbackExecutor() {
    return new MainThreadExecutor();
  }
  @Override CallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
    return new ExecutorCallAdapterFactory(callbackExecutor);
  }
  static class MainThreadExecutor implements Executor {
	// Looper.getMainLooper()就是为嘛响应会在主线程的原因
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override public void execute(Runnable r) {
      handler.post(r);
    }
  }
}
```

上面的代码先稍微放一下，我们继续看`retrofit.Bulider.build`,其中有几句比较关键的代码。
```Java
callFactory = new OkHttpClient();
callbackExecutor = platform.defaultCallbackExecutor();
adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));
```
结合`Android`类中的代码可以看出，其最后生成了`ExecutorCallAdapterFactory`类。虽然看到了`CallAdapter.Factory`，但是到底是哪里执行了`enqueue`方法呢？现在我们来看看`retrofit.create`的最后一句代码`serviceMethod.callAdapter.adapt(okHttpCall)`。
<br>

这里的`callAdapter`在不使用`addCallAdapterFactory`的Android环境中，就是上面我们说到`new ExecutorCallAdapterFactory`中get方法返回的对象。
```Java
@Override
public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
  if (getRawType(returnType) != Call.class) {
    return null;
  }
  final Type responseType = Utils.getCallResponseType(returnType);
  return new CallAdapter<Object, Call<?>>() {
    @Override public Type responseType() {
      return responseType;
    }
    @Override public Call<Object> adapt(Call<Object> call) {// Retrofit动态代理serviceMethod.callAdapter.adapt(okHttpCall);调用到这里
      return new ExecutorCallbackCall<>(callbackExecutor, call);
    }
  };
}
```
接下来再继续看看其调用`adapter`方法生成的`ExecutorCallbackCall`对象。
```Java
ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
  this.callbackExecutor = callbackExecutor;
  this.delegate = delegate;
}

@Override public void enqueue(final Callback<T> callback) {
  checkNotNull(callback, "callback == null");
  delegate.enqueue(new Callback<T>() {
    @Override public void onResponse(Call<T> call, final Response<T> response) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          if (delegate.isCanceled()) {
            // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
            callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
          } else {
            callback.onResponse(ExecutorCallbackCall.this, response);
          }
        }
      });
    }
    @Override public void onFailure(Call<T> call, final Throwable t) {
      callbackExecutor.execute(new Runnable() {
        @Override public void run() {
          callback.onFailure(ExecutorCallbackCall.this, t);
        }
      });
    }
  });
}
```

这里的参数`callback`才是用户输入的回调对象，而其中的`delegate`就是之前的`okhttpCall`。所以`delegate.enqueue`就是调用了`OkhttpCall.enqueue`，而其中的`callbackExecutor`就是刚刚的主线程。

~~RxJava这段，等晚上再梳理下~~

### Converter

在OkhttpCall.enqueue方法中还有一句重要的代码没有看，那就是`response = parseResponse(rawResponse);`,我们来看看这其中做了什么。

```Java
Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException
  ResponseBody rawBody = rawResponse.body();
  // Remove the body's source (the only stateful object) so we can pass th
  rawResponse = rawResponse.newBuilder()
      .body(new NoContentResponseBody(rawBody.contentType(), rawBody.conte
      .build();
  ...
  ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
  try {
    T body = serviceMethod.toResponse(catchingBody);// 解析body，比如Gson解析
    return Response.success(body, rawResponse);
  } catch (RuntimeException e) {
    // If the underlying source threw an exception, propagate that rather 
    // a runtime exception.
    catchingBody.throwIfCaught();
    throw e;
  }
}

### ServiceMethod
R toResponse(ResponseBody body) throws IOException {
  return responseConverter.convert(body);
}
```
可以看出parseResponse最终调用了`Converter.convert`方法。这里以常用的GsonConverterFactory为例。
```Java
@Override public T convert(ResponseBody value) throws IOException {
    JsonReader jsonReader = gson.newJsonReader(value.charStream());
    try {
      return adapter.read(jsonReader);
    } finally {
      value.close();
    }
  }
}
```
OkHttpCall在这之后的代码就比较简单了，通过回调将转换后得响应数据发送出去即可。

## 总结

本文分析了Retrofit的执行流程，其实包含了Retrofit、ServiceMethod、OkHttpCall、CallAdapter、Converter等方面，希望阅读本文后可以让我们在使用Retrofit的同时，也能了解其内部的执行过程。如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

## 参考

[Retrofit分析-漂亮的解耦套路](http://www.jianshu.com/p/45cb536be2f4)<br>
[拆轮子系列：拆 Retrofit](http://static.blog.piasy.com/2016/06/25/Understand-Retrofit/)<br>
[Retrofit](https://square.github.io/retrofit/)