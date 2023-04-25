# bao

枹

[![](https://jitpack.io/v/storytellerF/Bao.svg)](https://jitpack.io/#storytellerF/Bao)

拦截未处理的Exception

支持native

## 使用

引入jitpack

手动
```kts
implementation("com.github.storytellerF.Bao:bao-library:1.26")
```

```kotlin
class App: Application() {
    val bao = Bao(this)
    override fun onCreate() {
        super.onCreate()
        bao.bao()
    }
}
```
构造Bao 的时候可以传入一个函数用于发生异常时处理，默认有两个`defaultBaoHandler` 和`stringBaoHandler`，前者打开**ExceptionActivity**，并将异常信息通过Intent 传入，后者将异常信息写入cache.txt，此过程默认也会打开的**ExceptionActivity**。后者是默认的行为。

**cache.txt** 位于app 内部路径的cache 目录。

Throwable 如果为null，说明异常信息存储在cache.txt 中。使用`Bao.readException(context)` 读取。

```
val block: Context.(Throwable?) -> Boolean = ::stringBaoHandler
```

自动
```kts
implementation("com.github.storytellerF:startup:1.26")
```
