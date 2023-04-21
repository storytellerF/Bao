# bao

枹

[![](https://jitpack.io/v/storytellerF/Bao.svg)](https://jitpack.io/#storytellerF/Bao)

拦截未处理的Exception

支持native

## 使用

引入jitpack

### 手动

```kts
implementation("com.github.storytellerF.Bao:bao-library:$baoVersion")
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

构造Bao 的时候可以传入一个函数用于发生异常时处理，默认有`defaultBaoHandler` ,打开**ExceptionActivity**，并将异常信息通过Intent 传入。

虽然Throwable 是可序列化的，但是与Android 的Parcelable不同，编程语言并不会进行严格限制，也就是说某些字段可能并非可序列化的，
比如Compose 中的`DiagnosticCoroutineContextException` 异常中会包含`CoroutineContext` 。所以，
传入的异常信息是字符串。

```
val block: Context.(Throwable?) -> Boolean = ::defaultBaoHandler
```

### 自动

```kts
implementation("com.github.storytellerF.Bao:startup:$baoVersion")
```

使用`defaultBaoHandler` 处理，暂不支持自定义。

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

## 限制

因为父协程依赖子协程的结果，所以**CoroutineExceptionHandler** 需要设置到父协程中才能够起作用，但是这在*Compose* 中几乎是不可能的。
虽然一个点击事件没有使用协程处理，但是实际上依然会在协程下执行，并且*context* 是**AndroidUiDispatcher**，所有的异常如果没有设置
**try-catch** 的话是无法恢复到正常的状态的，而且可能会更遭，因为协程会一遍又一遍的尝试关闭程序。