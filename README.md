# bao

枹

[![](https://jitpack.io/v/storytellerF/Bao.svg)](https://jitpack.io/#storytellerF/Bao)

拦截未处理的Exception

支持native

## 使用

手动

```kotlin
class App: Application() {
    val bao = Bao(this)
    override fun onCreate() {
        super.onCreate()
        bao.bao()
    }
}
```

自动

