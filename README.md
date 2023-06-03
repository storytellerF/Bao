# bao

枹

[![](https://jitpack.io/v/storytellerF/Bao.svg)](https://jitpack.io/#storytellerF/Bao)

拦截未处理的Exception

支持native

## 使用

引入jitpack

手动

```kts
implementation("com.github.storytellerF.Bao:bao-library:2.2.0")
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
implementation("com.github.storytellerF.Bao:startup:2.2.0")
```

使用`stringBaoHandler` 处理，暂不支持自定义。

## 指定Theme

**ExceptionActivity** 未显示指定theme， 默认使用`<application/>` 的theme。

指定的theme 必需继承自 `Theme.AppCompat`，如果`<application/>` 已经使用了其他的theme（比如material 的theme），可以添加一条attr。

```xml
<resources>
    <style name="Theme.Fei" parent="android:Theme.Material.Light.NoActionBar">
        <item name="exceptionPageTheme">@style/Theme.Bao</item>
    </style>
    <style name="Theme.Bao" parent="Theme.AppCompat.Light" />
</resources>
```
