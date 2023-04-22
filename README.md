# bao

枹

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

