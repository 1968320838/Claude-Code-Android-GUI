# ClaudeBox 技术栈文档

## 1. 技术栈概览

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 语言 | Java 17 + Kotlin | 架构层用 Java，UI 层用 Kotlin |
| 最小 SDK | API 24 (Android 7.0) | 支持 94%+ 设备 |
| 目标 SDK | API 34 (Android 14) | 最新 Android |
| 构建工具 | Gradle 8.x + AGP 8.x | |
| 架构模式 | MVVM + Repository | 清晰分层 |

---

## 2. 核心依赖

### 2.1 SSH 通信

| 库 | 版本 | 说明 |
|----|------|------|
| **JSch** | 0.2.x | SSH2 协议实现，支持密码/密钥认证 |
| 备选: sshtools:maverick | 1.0.0+ | 更现代的 SSH 库 |

**选型理由**：JSch 成熟稳定，文档丰富，是 Android SSH 实现的事实标准。

### 2.2 终端模拟器

| 库 | 版本 | 说明 |
|----|------|------|
| **xterm.js** | 4.x | 终端模拟器核心，支持 ANSI |
| **WebView** | System WebView | 加载 xterm.js 的容器 |

**备选方案**：chromium-view（独立 Chromium 视图，更好的兼容性）

**选型理由**：xterm.js + WebView 组合成熟，ANSI 支持完善，体积小。

### 2.3 依赖注入

| 库 | 版本 | 说明 |
|----|------|------|
| **Dagger Hilt** | 2.48+ | Google 推荐的 Android DI 方案 |

**选型理由**：Hilt 与 Android 生命周期深度集成，减少样板代码。

### 2.4 异步编程

| 库 | 版本 | 说明 |
|----|------|------|
| **Kotlin Coroutines** | 1.7.x | 官方推荐的异步方案 |
| **RxJava3** | 3.1.x | 响应式编程，复杂异步场景 |

**选型理由**：UI 层用 Coroutines，SSH 数据流处理用 RxJava。

### 2.5 数据库

| 库 | 版本 | 说明 |
|----|------|------|
| **Room** | 2.6.x | Android 官方数据库，SQLite 封装 |

**选型理由**：与生命周期集成，支持 LiveData/Flow，方便会话持久化。

### 2.6 Markdown 渲染

| 库 | 版本 | 说明 |
|----|------|------|
| **Markwon** | 4.6.x | 无依赖的 Markdown 渲染库 |

**选型理由**：纯 Android 实现，无需 WebView，渲染性能好。

### 2.7 代码高亮

| 库 | 版本 | 说明 |
|----|------|------|
| **Highlight.js** | 11.9.x | 主流代码高亮库 |
| **CodeView** | 1.3.x | Android 代码高亮控件 |

**选型理由**：Highlight.js 支持语言多，高亮效果好。

---

## 3. UI 框架

### 3.1 基础 UI

| 库 | 说明 |
|----|------|
| **AndroidX AppCompat** | 向下兼容 |
| **Material Components** | Material Design 3 组件 |
| **ConstraintLayout** | 响应式布局 |
| **ViewPager2** | 多 Tab 滑动 |

### 3.2 导航

| 库 | 说明 |
|----|------|
| **BottomNavigationView** | 底部导航栏 |
| **Navigation Component** | Fragment 导航管理 |

### 3.3 响应式 UI

| 库 | 说明 |
|----|------|
| **LiveData** | 生命周期感知的可观察数据 |
| **ViewModel** | UI 相关数据的生命周期管理 |

---

## 4. 网络与安全

### 4.1 网络

| 库 | 说明 |
|----|------|
| **OkHttp** | HTTP 客户端（用于可能的 API 调用） |
| **Retrofit** | REST API 封装（备用） |

### 4.2 安全

| 库 | 说明 |
|----|------|
| **Android Keystore** | SSH 密钥加密存储 |
| **EncryptedSharedPreferences** | 敏感配置加密 |

---

## 5. 构建配置

### 5.1 Gradle 依赖示例

```gradle
// SSH
implementation 'com.jcraft:jsch:0.2.18'

// DI
implementation 'com.google.dagger:hilt-android:2.48'
annotationProcessor 'com.google.dagger:hilt-compiler:2.48'

// Database
implementation 'androidx.room:room-runtime:2.6.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// Markdown
implementation 'io.noties.markwon:core:4.6.2'

// UI
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
implementation 'androidx.viewpager2:viewpager2:1.0.0'
```

---

## 6. 开发工具

| 工具 | 说明 |
|------|------|
| **Android Studio** | 官方 IDE |
| **Gradle** | 构建工具 |
| **Git** | 版本控制 |
| **GitHub** | 代码托管 |

---

## 7. 技术选型总结

### 7.1 MVP 版本技术栈（简化）

为了项目简单健壮，建议 MVP 阶段使用最小依赖集：

| 类别 | MVP 技术 |
|------|----------|
| 语言 | Kotlin |
| SSH | JSch |
| 终端 | WebView + xterm.js |
| DI | Hilt |
| 数据库 | Room |
| 异步 | Coroutines |
| Markdown | Markwon |
| UI | Material Components + ViewPager2 |

### 7.2 后续扩展技术

| 阶段 | 可添加技术 |
|------|------------|
| 完善期 | RxJava3、CodeView |
| 稳定期 | 单元测试（JUnit、Espresso）、CI/CD |
| 扩展期 | 深度链接、数据统计、推送通知 |

---

## 8. 依赖版本兼容性

| 依赖 | 最低版本 | 推荐版本 | AGP 兼容性 |
|------|---------|---------|-----------|
| Java | 17 | 17 | AGP 8.x 需要 JDK 17 |
| Gradle | 8.0 | 8.4 | AGP 8.2 最好 |
| AGP | 8.0 | 8.2.x | |
| Kotlin | 1.9.0 | 1.9.21 | AGP 8.2 兼容 |
| Hilt | 2.48 | 2.48.1 | |
| Room | 2.6.0 | 2.6.1 | |

---

## 9. 注意事项

1. **xterm.js 加载**：需要将 xterm.js 和相关 CSS/字体文件放入 `assets` 目录
2. **Termux 依赖**：用户需独立安装 Termux App 和 openssh
3. **Claude Code CLI**：需在 Termux 中手动安装
4. **Android 权限**：需要 `INTERNET` 权限，用于 SSH 连接
