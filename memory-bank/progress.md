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

---

## 2026-04-03 - Phase 3.1 完成 ✅

### 完成内容

**终端模拟器集成 (Step 3.1)**
- `assets/terminal.html` - xterm.js 终端页面
  - Material You 深色主题 (#1C1B1F 背景, #6750A4 强调色)
  - Terminal 实例初始化、Fit addon、WebLinks addon
  - JavaScript 接口暴露
- `assets/xterm/xterm.js` (388 KB)
- `assets/xterm/xterm.css` (4.4 KB)
- `assets/xterm/xterm-addon-fit.js` (1.5 KB)
- `assets/xterm/xterm-addon-web-links.js` (2.9 KB)
- `ui/terminal/TerminalWebViewClient.kt` - WebViewClient 处理页面加载
- `ui/terminal/TerminalJavaScriptInterface.kt` - Android-JS 通信接口
- `ui/terminal/TerminalViewModel.kt` - 终端逻辑 (PTY 读写)
- `ui/terminal/TerminalFragment.kt` - WebView 配置、键盘/旋转监听
- `res/layout/fragment_terminal.xml` - WebView 布局 + loading overlay

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/` (7.8 MB)

---

## 2026-04-03 - Phase 3.2 完成 ✅

### 完成内容

**会话列表页面 (Step 3.2)**
- `data/local/entity/SessionEntity.java` - Room 会话实体
- `data/local/entity/MessageEntity.java` - Room 消息实体
- `data/local/dao/SessionDao.java` - 会话 CRUD 操作
- `data/local/dao/MessageDao.java` - 消息 CRUD 操作
- `data/local/AppDatabase.java` - Room 数据库
- `data/repository/SessionRepositoryImpl.java` - 会话仓库实现
- `ui/chat/SessionAdapter.kt` - RecyclerView 适配器
- `ui/chat/ChatAdapter.kt` - 消息列表适配器
- `ui/chat/ChatViewModel.kt` - 聊天/Session 共享 ViewModel (activityViewModels)
- `ui/chat/ChatFragment.kt` - 聊天界面 (会话切换 toolbar)
- `ui/chat/SessionListFragment.kt` - 会话列表 Fragment
- `res/layout/fragment_chat.xml` - 聊天布局 (toolbar + messages + input)
- `res/layout/fragment_session_list.xml` - 会话列表布局
- `res/layout/item_session.xml` - 会话项布局
- `res/layout/item_message_user.xml` - 用户消息气泡
- `res/layout/item_message_bot.xml` - AI 消息气泡
- `res/drawable/ic_add.xml`, `ic_delete.xml`, `ic_send.xml`, `ic_dropdown.xml`
- `res/navigation/nav_graph.xml` - 添加 sessionListFragment

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/` (8.9 MB)

### 技术决策记录
1. **ChatViewModel**: 使用 `activityViewModels()` 在 ChatFragment 和 SessionListFragment 间共享
2. **Room Entity**: 使用 `@Ignore` 标注非静态构造函数，避免 Room 警告
3. **会话选择**: 通过 Dialog 实现，而非新建 Fragment

### 下一步
- Phase 3.3: 消息发送/接收
- Phase 3.4: Markdown 渲染
- Phase 3.5: 代码高亮

---

## 2026-04-03 - Phase 3.3 完成 ✅

### 完成内容

**消息发送/接收修复 (Step 3.3)**
- `TermuxRepository.ShellOutputListener` - 新增多观察者接口
  - `onOutput(data)` - PTY 输出回调
  - `onClosed()` - Shell 关闭回调
- `TermuxRepositoryImpl` - 多监听者支持
  - `CopyOnWriteArrayList<ShellOutputListener>` 存储监听器
  - `readOutputLoop()` 通知所有监听器
  - Shell 只创建一次，后续 `openClaudeSession()` 调用复用
- `ChatViewModel` - 使用 ShellOutputListener 接收 AI 响应
- `TerminalViewModel` - 也使用 ShellOutputListener

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/` (8.9 MB)

### 技术决策记录
1. **多观察者模式**: Terminal 和 Chat 共享同一个 SSH shell，通过 CopyOnWriteArrayList 允许多个观察者接收输出
2. **Shell 复用**: openClaudeSession() 如果 shell 已存在则直接回调，不会重新创建

---

## 2026-04-03 - Phase 3.4 完成 ✅

### 完成内容

**Markdown 渲染 (Step 3.4)**
- `ui/chat/MarkwonFactory.kt` - Markwon 单例工厂
  - 配置 CorePlugin + StrikethroughPlugin
  - `toMarkdown(context, markdown)` 方法将 Markdown 转为 Spanned
- `ChatAdapter` - BotMessageViewHolder 使用 Markwon 渲染消息内容

### 支持的 Markdown 特性
- 标题 (`#`, `##`, etc.)
- 列表 (`-`, `1.`)
- 强调 (`**bold**`, `*italic*`, `~~strikethrough~~`)
- 链接和图片
- 引用 (`>`)
- 代码块和行内代码

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/` (8.9 MB)

---

## 2026-04-03 - Phase 3.5 完成 ✅

### 完成内容

**代码高亮基础 (Step 3.5)**
- `assets/highlight/highlight.js` (75KB) - Highlight.js 核心库 (已下载待用)
- `item_message_bot.xml` - 更新消息布局
  - 消息气泡最大宽度 280dp
  - 代码块使用深色背景 (#2D2D2D)
  - 行间距增加 (lineSpacingExtra="4dp")

### 验证状态
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK 生成在 `app/build/outputs/apk/debug/` (8.9 MB)

### 待增强
- 完整语法高亮（需要配置 SyntaxHighlightPlugin + Highlight.js integration）
- 代码块复制按钮

---

## Phase 3 完成总结

| Step | 内容 | 状态 |
|------|------|------|
| 3.1 | 终端模拟器 (xterm.js) | ✅ |
| 3.2 | 会话列表 + 数据库 | ✅ |
| 3.3 | 消息发送/接收 | ✅ |
| 3.4 | Markdown 渲染 | ✅ |
| 3.5 | 代码高亮基础 | ✅ |

**验证状态**
- [x] `./gradlew assembleDebug` 构建成功
- [x] APK: `app/build/outputs/apk/debug/app-debug.apk` (8.9 MB)

### 技术决策记录
1. **共享 ViewModel**: ChatFragment 和 SessionListFragment 使用 `activityViewModels()` 共享 ChatViewModel
2. **多监听者 Shell**: Terminal 和 Chat 通过 ShellOutputListener 共享同一个 SSH PTY
3. **Markdown 渲染**: 使用 Markwon 库，纯 Android 实现无需 WebView
4. **Room Entity 设计**: 使用 @Ignore 标注非静态构造函数，避免 Room 警告

### 下一步
- Phase 4: 文件浏览器
- Phase 5: 主题、字体、性能优化
