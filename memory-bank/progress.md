# ClaudeBox 开发进度记录

## 2026-04-01 - Phase 1 完成 ✅

### 完成内容

**项目初始化 (Step 1.1)**
- 创建 `settings.gradle.kts` - 项目配置，包含 google/mavenCentral 仓库
- 创建 `build.gradle.kts` - 项目级构建配置 (AGP 8.2.2, Kotlin 1.9.22, Hilt 2.48.1)
- 创建 `gradle.properties` - AndroidX 启用，Jetifier 配置
- 创建 `gradlew` + `gradle/wrapper/gradle-wrapper.jar` - Gradle 8.4 wrapper
- 创建 `local.properties` - SDK 路径配置
- 创建 `app/build.gradle.kts` - 模块级构建配置，包含所有依赖
- 创建 `app/proguard-rules.pro` - ProGuard 混淆规则

**目录结构**
```
app/src/main/
├── java/com/claudebox/
│   ├── ui/              (Kotlin - Activity, Fragment, ViewModel)
│   ├── domain/          (Java - models, repository interfaces)
│   └── data/            (Java - SSH config, connection state)
├── res/                 (layout, values, drawable, menu, navigation)
└── assets/              (预留)
```

**Android 清单配置**
- 权限: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK
- Application class: ClaudeBoxApp (Hilt)
- Activity: MainActivity (导航宿主)

**Domain 层 (Java)**
- `domain/model/Session.java` - 会话模型 (id, name, createdAt, lastActiveAt)
- `domain/model/Message.java` - 消息模型 (id, sessionId, content, rawContent, isFromUser, timestamp)
- `domain/model/FileItem.java` - 文件项模型 (name, path, isDirectory, size, modifiedAt)
- `domain/repository/SessionRepository.java` - 会话仓库接口
- `domain/repository/TermuxRepository.java` - Termux 仓库接口

**Data 层 (Java)**
- `data/ssh/SSHConfig.java` - SSH 配置模型 (host, port, username, authType, password/key paths)
- `data/ssh/ConnectionState.java` - 连接状态抽象类 (Disconnected, Connecting, Connected, Error, Reconnecting)

**UI 层 (Kotlin)**
- `ClaudeBoxApp.kt` - Hilt Application 类
- `ui/main/MainActivity.kt` - 主 Activity，配置 BottomNavigation + NavController
- `ui/chat/ChatFragment.kt` + `ChatViewModel.kt` - 聊天模块
- `ui/terminal/TerminalFragment.kt` + `TerminalViewModel.kt` - 终端模块
- `ui/files/FilesFragment.kt` + `FilesViewModel.kt` - 文件模块
- `ui/settings/SettingsFragment.kt` + `SettingsViewModel.kt` - 设置模块

**资源文件**
- `res/navigation/nav_graph.xml` - 导航图 (4个 Fragment 目的地)
- `res/menu/bottom_nav_menu.xml` - 底部导航菜单
- `res/layout/activity_main.xml` - 主布局 (NavHostFragment + BottomNavigationView)
- `res/layout/fragment_*.xml` - 4个 Fragment 布局
- `res/values/themes.xml` - Material3 深色主题 (#6750A4 主色)
- `res/values/colors.xml` - 颜色定义
- `res/values/strings.xml` - 字符串资源
- `res/drawable/ic_*.xml` - Vector 图标 (chat, terminal, folder, settings)

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/`

### 下一步
- Phase 1.2-1.4: 完善 MVVM 骨架、添加 BaseFragment/BaseViewModel、Room 数据库集成
- Phase 2: SSH 连接模块实现

---

## 2026-04-02 - Phase 2 完成 ✅

### 完成内容

**依赖修复**
- JSch 改用 `com.github.mwiede:jsch:0.2.20` (原 com.jcraft:jsch 已废弃)
- 添加 JitPack 仓库
- 排除 `annotations-java5` 解决与 Room/Hilt 的重复类冲突

**SSH 连接模块 (Step 2.1)**
- `data/ssh/SSHClient.java` - SSH 客户端封装
  - 支持密码认证和私钥认证
  - 支持 PTY (伪终端) 用于交互式 shell
  - `openShellChannel()` 返回输入输出流用于实时通信
  - `ShellChannel` 内部类封装通道和流
- `data/repository/TermuxRepositoryImpl.java` - Repository 实现
  - 纯 Java 回调模式，避免 Kotlin 协程混用复杂性
  - `ConnectionCallback` 接口处理连接状态变化
  - `ClaudeSessionCallback` 接口处理 Claude 会话输出
  - 使用单线程 ExecutorService 管理异步操作
- `data/repository/ConnectionManager.java` - 连接管理器
  - 单例模式，管理整个应用的生命周期
  - `AtomicReference<ConnectionState>` 线程安全的状态存储
  - 自动重连机制：最多 3 次，指数退避 (2s/4s/8s)
  - 监听 `TermuxRepository` 的连接回调并更新状态

**配置管理 (Step 2.2)**
- `data/local/ConfigManager.java` - 配置管理器
  - 使用 `EncryptedSharedPreferences` 加密存储敏感信息
  - AES256_GCM 加密 + AES256_SIV 密钥加密
  - 保存/加载 SSH 配置 (密码、私钥路径等)
  - 降级方案：加密失败时使用普通 SharedPreferences

**UI 交互 (Step 2.2-2.4)**
- `ui/settings/SettingsViewModel.kt` - 设置 ViewModel
  - `validateConfig()` 配置验证
  - `testConnection()` 异步连接测试
  - `connect()` / `disconnect()` 连接控制
  - `createSSHConfig()` 从 UI 构建配置对象
- `ui/settings/SettingsFragment.kt` - 设置界面
  - 认证方式切换 (密码/私钥)
  - 连接状态实时显示 (红/绿/橙指示器)
  - 测试连接、保存配置、连接/断开按钮
  - 每 500ms 轮询 ConnectionManager 状态
- `res/layout/fragment_settings.xml` - 设置布局
  - Material Design 3 TextInputLayout 表单
  - RadioGroup 认证方式选择
  - LinearProgressIndicator 连接进度
- `res/drawable/connection_indicator_*.xml` - 状态指示器
  - `disconnected.xml` - 红色 (错误/断开)
  - `connected.xml` - 绿色 (已连接)
  - `connecting.xml` - 橙色 (连接中/重连中)

**依赖注入 (Step 2.1)**
- `di/AppModule.java` - Hilt 模块
  - `@Provides @Singleton TermuxRepository`
  - `@Provides @Singleton ConnectionManager`

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/` (7.3 MB)
- [x] 真机测试：SSH 密码认证连接 Termux ✅
- [x] 连接状态正确显示 ✅
- [x] 断开后自动重连机制正常 ✅

### 技术决策记录
1. **Java/Kotlin 边界**: Data 层和 Domain 层使用纯 Java，通过回调接口与 Kotlin UI 层通信
2. **协程策略**: 避免在 Java 层使用 Kotlin 协程，使用 ExecutorService + 回调模式
3. **SSH 库选型**: com.jcraft:jsch 已停止维护，改用 mwiede fork (0.2.20)
4. **状态管理**: 使用 `AtomicReference<ConnectionState>` 实现线程安全的状态观察

### 下一步
- Phase 3: 终端模拟器集成 (xterm.js)、会话管理、聊天功能
- Phase 4: 文件浏览器
- Phase 5: 主题、字体、性能优化
