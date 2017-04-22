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
