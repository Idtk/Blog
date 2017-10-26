# Retrofit2 + OkHttp3 配置及Interceptor原理

前段时间在给公司的新app做一些基础模块的封装，把Http模块中的一些基础配置，比如设置链接超时、Http Log Interceptor、Access Token Interceptor、Status Code Interceptor，以及Json转换、RxJava适配等设置做一下分享，再简单说说其拦截器原理。

## 一、基本配置

### 1、OkHttp配置

OkHttp主要采用Builder模式进行配置

```java
HttpLoggingInterceptor.Logger CUSTOM = new HttpLoggingInterceptor.Logger() {
    @Override
    public void log(String message) {
        Platform.get().log(INFO, "OkHttp-idtk： " + message, null);
    }
};
HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(CUSTOM);
loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
OkHttpClient client = new OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(statusInterceptor)
        .addInterceptor(loggingInterceptor)
        .addNetworkInterceptor(tokenInterceptor)
        .build();
```

* retryOnConnectionFailure设置为true，表示请求失败后将会重连。
* HttpLoggingInterceptor用于拦截网络请求和响应，并输出Log，我在输出log的标识方面稍微修改了一下，便于使用时的区别。等级分为NONE/BASIC/HEADERS/BODY，其中BODY打印出的最为详细。
* addInterceptor与addNetworkInterceptor都是增加OkHttp的网络请求拦截器，但是其中是有一定区别的，前者是添加在与服务器连接之前和之后，后者是添加在与服务器建立连接和发起请求的之间。
* tokenInterceptor 是用来设置的token的拦截器，用于网络请求token的统一添加，具体内容会在接下来说明。
* statusInterceptor 用于响应返回状态码的处理，比如token过期、注册码无效等状态的处理。



#### token拦截器

让所有网络请求都添加上token

```java
@Override
public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    String url = request.url().toString();
    if (NetConfig.needAddToken(request)) {
        Uri uri = Uri.parse(url);
        Set<String> oldParam = uri.getQueryParameterNames();
        if (!oldParam.contains("token")){
            String token = getToken();
            if (oldParam.size() >0){
                url += "&token=" + token;
            }else {
                url += "?token=" +token;
            }
        }
        request = request.newBuilder().url(url).build();
    }
    return chain.proceed(request);
}
```

* 对于登录请求等情况，是不需要添加token的，所以在needAddToken方法中进行了判断。
* 我这里后台的要求是把token放在url中，所以我进行了如上的写法，你也可以根据自己的需求进行修改。



#### 状态码拦截

对于后台定义的各种状态码进行处理

```java
@Override
public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Response response = chain.proceed(request);
    BaseResult data = new Gson().fromJson(response.body().string(), BaseResult.class);
    if (data == null) {
        throw new RuntimeException("返回数据结构不合法： " + response.body().string());
    }
    int status = data.status;
    switch (status){
        case NetConfig.TOKEN:
            throw new TokenException(data.msg);
        case NetConfig.REGISTER_CODE:
            throw new RegisterException(data.msg);
    }
    return response;
}

public class BaseResult {
    public int status;
    public String msg;
    public JsonElement data;
}
```

* BaseResult是根据与后台的协定设置的，这里首先对data是否为null进行了检测。
* 在获取状态码之后，对其进行token失效和注册码失效的检测，这里处理抛出异常，之后可以在使用时的基类进行统一处理。当然你也可以使用接口等别的方式进行处理。



### 2、Retrofit配置

```java
retrofit = new Retrofit.Builder()
        .client(buildClient())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .baseUrl(baseUrl)
        .build();
```

* addConverterFactory  添加Gson转换器
* addCallAdapterFactory  添加Rxjava2适配器



## 二、拦截器原理

因为之前写过[文章](http://www.idtkm.com/2017/04/27/Retrofit/)分析，所以这里便不再赘述，而对于OkHttp，现在来简单说说其拦截器原理。

OkHttp的使用我想大家都知道，在调用`client.newCall(request)`将会调用到`RealCall.newRealCall(this, request, false);`方法，之后将会调用到`RealCall.getResponseWithInterceptorChain()`函数，而在其中将进行拦截器链的构建。

```java
Response getResponseWithInterceptorChain() throws IOException {
  // Build a full stack of interceptors.
  List<Interceptor> interceptors = new ArrayList<>();
  // client中配置的interceptors
  interceptors.addAll(client.interceptors());
  // 重定向与失败重试
  interceptors.add(retryAndFollowUpInterceptor);
  // 用户的请求头处理，响应处理
  // (Cookie持久性策略)
  interceptors.add(new BridgeInterceptor(client.cookieJar()));
  // 缓存请求、响应缓存的写入 (客户端设置的缓存策略)
  interceptors.add(new CacheInterceptor(client.internalCache()));
  // 与服务器建立连接
  interceptors.add(new ConnectInterceptor(client));
  if (!forWebSocket) {
    // client配置的networkInterceptors，用于观察请求和响应
    interceptors.addAll(client.networkInterceptors());
  }
  // 发送请求，读取服务器的响应
  interceptors.add(new CallServerInterceptor(forWebSocket));
  // 设置完整的OkHttp拦截链
  Interceptor.Chain chain = new RealInterceptorChain(
      interceptors, null, null, null, 0, originalRequest);
  // 调用链中的下一个拦截器，在这里是开始调用第一个拦截器
  return chain.proceed(originalRequest);
}
```

可以看出拦截器列表是从interceptors开始添加的，也就是之前说的HttpLoggingInterceptor、StatusInterceptor，之后添加重定向、cookie、缓存、连接建立的拦截器，然后添加上networkInterceptors，也就是我们之前说的TokenInterceptor，最后通过RealInterceptorChain调用proceed接口启动拦截器。

如果你查看上面各个拦截器的源码，包括我上面自定义的三个拦截器，你会发现其中几乎都使用了`chain.proceed`方法来生成response，当然CallServerInterceptor其中并没有这个方法，因为它是真正使用去进行请求的拦截器。根据这些我们就可以绘制出一个事件流在拦截其中的过程：

<img src="http://ompb0h8qq.bkt.clouddn.com/Retrofit2%20+%20OkHttp3%20Interceptor.png" alt="Interceptor" title="Interceptor"/>

我们可以在`CallServerInterceptor`之前对request的header、url等参数进行检测和配置，比如我们自定义的TokenInterceptor，在`CallServerInterceptor`之后我们可以对response的code、body等进行检测和配置，比如我们之前定义的StatusInterceptor,而HttpLoggingInterceptor则是对request和response都进行检测和配置。



# 3、小结

当然Retrofit+OkHttp还有更多的属性配置，比如证书、Cookie等，但这些属性在网络请求中的实现都是在OkHttp Interceptor中进行的，感兴趣的同学建议阅读下源码。

如果在阅读过程中，有任何疑问与问题，欢迎与我联系。<br>
**博客:www.idtkm.com**<br>
**GitHub:https://github.com/Idtk**<br>
**微博:http://weibo.com/Idtk**<br>
**邮箱:IdtkMa@gmail.com**<br>
<br>

