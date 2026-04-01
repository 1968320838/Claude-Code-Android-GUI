# Claude Code Android GUI 客户端 - 详细实施计划

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| v1.2 | 2026-04-01 | Claude | 根据用户澄清完善：Termux辅助脚本、消息持久化、语言分层、权限清单、Termux配置指南、构建签名 |

---

## 概述

本计划基于 PRD-design-document.md、Research.md 和 tech-stack.md 文档编写，提供面向 AI 开发者的详细分步实施指令。

### 技术约束
- **架构分层语言**:
  - Domain 层（模型、Repository 接口、用例）: **Java**
  - Data 层（Repository 实现、SSH 客户端、Room DAO）: **Java**
  - UI 层（Activity、Fragment、ViewModel、Adapter）: **Kotlin**
- **最小 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34 (Android 14)
- **构建工具**: Gradle 8.x + AGP 8.x
- **JDK 版本**: JDK 17

### 开发环境前置条件
- [ ] 安装 JDK 17
- [ ] 安装 Android Studio Hedgehog (2023.1.1) 或更新版本
- [ ] 安装 Android SDK API 34
- [ ] 配置 ANDROID_HOME 环境变量
- [ ] 创建 Android 虚拟设备 (API 34) 用于测试
- [ ] 安装 Termux App 和 openssh（用于真机测试）

### Android 权限清单
必须在 AndroidManifest.xml 中声明以下权限：
- `INTERNET` - SSH 网络连接
- `ACCESS_NETWORK_STATE` - 检测网络连接状态
- `WAKE_LOCK` - 保持 SSH 连接活跃（防止息屏断连）
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` - 仅在需要访问本地文件时（API < 29）

### Termux 端配置前置条件
用户在真机测试前需完成以下 Termux 端配置（参见附录 D「Termux 配置指南」）：
- 安装 Termux App
- 安装 openssh 并启动 sshd 服务
- 生成或导入 SSH 密钥（可选）
- 在 Termux 中安装 Claude Code CLI
- 验证 sshd 可正常连接

---

## Phase 1：基础框架搭建（第 1-2 周）

### 1.1 项目初始化

#### Step 1.1.1: 创建项目结构
1. 打开 Android Studio，选择 "New Project"
2. 选择 "Empty Activity" 模板
3. 配置项目：
   - Name: ClaudeBox
   - Package name: com.claudebox
   - Language: Kotlin
   - Minimum SDK: API 24
   - Build configuration language: Kotlin DSL
4. 创建项目并等待 Gradle 同步完成

**验证**: 运行 `./gradlew assembleDebug` 无报错，项目成功构建 APK

#### Step 1.1.2: 配置 Gradle wrapper
1. 在项目根目录执行 `gradle wrapper --gradle-version 8.4`
2. 验证 wrapper: `./gradlew -v`
3. 确认输出显示 Gradle 8.4

**验证**: 执行 `./gradlew wrapper` 成功，生成 gradle/wrapper/ 目录

#### Step 1.1.3: 配置 build.gradle.kts（项目级）
1. 设置 pluginManagement 块：
   - 添加 google() 和 mavenCentral() 仓库
   - 设置 plugins 的版本策略
2. 配置 buildscript 或 plugins DSL
3. 设置 android.experimental.analytics.native.ide.start=true

**验证**: 运行 `./gradlew buildEnvironment` 显示正确的插件和依赖版本

#### Step 1.1.4: 配置 app/build.gradle.kts（模块级）
1. 设置 plugins:
   - kotlin-android
   - dagger.hilt.android.plugin
   - kotlin-kapt
2. 配置 android{} 块：
   - compileSdk = 34
   - minSdk = 24
   - targetSdk = 34
   - jvmTarget = "17"
3. 添加核心依赖（参见 tech-stack.md 第 5.1 节）

**验证**: `./gradlew :app:dependencies` 显示所有依赖树完整

#### Step 1.1.5: 创建目录结构
1. 在 app/src/main/ 创建以下目录结构：
   - java/com/claudebox/{ui,domain,data}
   - res/{layout,values,drawable,menu,navigation}
   - assets/
2. 验证目录创建成功

**验证**: 执行 `find app/src -type d | sort` 确认目录结构完整

#### Step 1.1.6: 配置 Android 权限
1. 打开 app/src/main/AndroidManifest.xml
2. 在 <manifest> 标签内添加以下权限声明：
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   <uses-permission android:name="android.permission.WAKE_LOCK" />
   ```
3. 可选（API < 29 时）:
   ```xml
   <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
   ```

**验证**: AndroidManifest.xml 包含所有必需权限，应用安装时正确请求权限

### 1.2 MVVM 架构架子搭建

#### Step 1.2.1: 创建 Application 类
1. 创建 ClaudeBoxApp.kt 继承 Application
2. 添加 @HiltAndroidApp 注解
3. 验证 Application 类可被识别

**验证**: 运行应用，确认 Application.onCreate() 被调用，Hilt 生成代码无报错

#### Step 1.2.2: 创建基础包结构
1. 创建 ui 包（Kotlin）：包含 Activity、Fragment、ViewModel、Adapter
2. 创建 domain 包（Java）：包含模型、Repository 接口、用例
3. 创建 data 包（Java）：包含 Repository 实现、SSH 客户端、Room DAO
4. 创建 di 包（Java）：包含 Hilt 模块

**验证**: `./gradlew :app:compileDebugKotlin` 无类找不到错误

#### Step 1.2.3: 创建基础 Domain 模型
1. 创建 Session.java 数据类：
   - id: String
   - name: String
   - createdAt: Long
   - lastActiveAt: Long
2. 创建 Message.java 数据类：
   - id: String
   - sessionId: String
   - content: String（**已渲染的 HTML 文本，持久化到 Room**）
   - rawContent: String（原始文本，仅内存持有）
   - isFromUser: Boolean
   - timestamp: Long
3. 创建 FileItem.java 数据类：
   - name: String
   - path: String
   - isDirectory: Boolean
   - size: Long
   - modifiedAt: Long

**验证**: Java 编译器通过，无语法错误

#### Step 1.2.4: 创建基础 Repository 接口
1. 在 domain/repository/ 创建 SessionRepository.java
   - getSessions(): List<Session>
   - createSession(): Session
   - deleteSession(id: String)
   - getSession(id: String): Session?
2. 在 domain/repository/ 创建 TermuxRepository.java
   - connect(config: SSHConfig): Flow<ConnectionState>
   - disconnect()
   - executeCommand(command: String): Flow<String>
   - isConnected(): Boolean

**验证**: 实现类可被 Hilt 注入，无循环依赖

---

### 1.3 导航框架实现

#### Step 1.3.1: 添加导航依赖
1. 在 app/build.gradle.kts 添加：
   - androidx.navigation:navigation-fragment-ktx
   - androidx.navigation:navigation-ui-ktx
2. 同步 Gradle

**验证**: `./gradlew :app:dependencies | grep navigation` 显示导航库版本

#### Step 1.3.2: 创建导航图
1. 在 res/navigation/ 创建 nav_graph.xml
2. 定义四个 Fragment 目的地：
   - chatFragment
   - terminalFragment
   - filesFragment
   - settingsFragment
3. 配置默认起始目的地为 chatFragment

**验证**: 导航图 XML 格式正确，通过 Android Studio 导航编辑器可正常打开

#### Step 1.3.3: 创建 BottomNavigation 菜单
1. 在 res/menu/ 创建 bottom_nav_menu.xml
2. 添加四个菜单项（对应四个 Fragment）
3. 配置图标和标签

**验证**: 菜单 XML 可被 MenuInflater 正确解析

#### Step 1.3.4: 配置 MainActivity 布局
1. 创建 activity_main.xml
2. 添加 NavHostFragment（id: nav_host_fragment）
3. 添加 BottomNavigationView（id: bottom_nav）
4. 使用 ConstraintLayout 包裹

**验证**: 布局预览显示底部导航栏和 Fragment 容器

#### Step 1.3.5: 实现 MainActivity
1. 创建 MainActivity.kt
2. 配置 NavController
3. 设置 BottomNavigationView 与导航图关联
4. 调用 setSupportActionBar() 配置 toolbar（可选）

**验证**:
- 应用启动后底部导航显示四个 Tab
- 点击 Tab 可切换 Fragment
- 方向键返回正确处理

---

### 1.4 基础 UI 组件封装

#### Step 1.4.1: 配置 Material Design 主题
1. 在 res/values/ 创建 themes.xml
2. 配置 Material3 主题：
   - parent: Theme.Material3.Dark.NoActionBar
   - 主色: #6750A4
   - 背景色: #1C1B1F
3. 配置 colorScheme

**验证**:
- 应用使用深色主题
- 主色正确显示为紫色 (#6750A4)

#### Step 1.4.2: 创建 Fragment 基类
1. 创建 BaseFragment.kt 继承 Fragment
2. 添加 ViewBinding 初始化逻辑
3. 提供抽象方法 onViewCreatedInner()

**验证**: 所有 Fragment 继承 BaseFragment 后可正常使用 ViewBinding

#### Step 1.4.3: 创建基础 ViewModel 基类
1. 创建 BaseViewModel.kt 继承 ViewModel
2. 添加 Hilt 支持（@HiltViewModel）
3. 配置 ViewModelScope

**验证**: ViewModel 可被 by viewModels() delegate 正确创建

---

### Phase 1 验收测试

**功能验收**:
- [ ] APK 可成功构建
- [ ] 应用可安装在模拟器
- [ ] 底部导航显示四个 Tab
- [ ] Tab 切换正常，无崩溃
- [ ] 深色主题正确应用

**代码质量**:
- [ ] MVVM 分层正确（ui/domain/data）
- [ ] 无硬编码字符串（在 strings.xml 中）
- [ ] 所有依赖已声明

---

## Phase 2：Termux 连接模块（第 3-4 周）

### 2.0 Termux 端辅助脚本（Phase 3 前置准备）

Claude Code 是对话式 CLI，需要一个 Termux 端的 Shell 包装脚本来管理会话生命周期。该脚本由 App 通过 SSH 调用。

#### Step 2.0.1: 创建 Claude wrapper 脚本
1. 在 Termux 的用户目录（如 ~/.local/bin/）创建 claude-wrapper.sh 脚本
2. 脚本功能：
   - 接收用户输入消息作为参数
   - 启动 Claude Code CLI 并传递消息
   - 实时输出 Claude Code 的响应流到 stdout
   - 管理会话状态（工作目录、上下文）
3. 脚本示例逻辑：
   ```
   #!/data/data/com.termux/files/usr/bin/bash
   cd <会话工作目录>
   echo "<用户消息>" | claude --print <其他参数>
   ```
4. 确保脚本有执行权限: chmod +x claude-wrapper.sh

**验证**: 在 Termux 中执行 `./claude-wrapper.sh "Hello"` 能看到 Claude Code 响应

#### Step 2.0.2: 在 App 中记录 wrapper 脚本路径
1. 在 SSH 配置中记录 wrapper 脚本路径（默认为 ~/.local/bin/claude-wrapper.sh）
2. 用户可在设置页面自定义 wrapper 路径

**验证**: 配置界面可保存和加载 wrapper 路径

#### Step 2.0.3: 通过 SSH 调用 wrapper 脚本
1. App 通过 SSH 执行命令格式: `bash ~/.local/bin/claude-wrapper.sh "<用户消息>"`
2. 捕获 stdout 作为 AI 响应流
3. 捕获 stderr 用于错误诊断

**验证**: App 发送消息后能接收到 Claude Code 的流式输出

---

### 2.1 SSH 连接实现

#### Step 2.1.1: 添加 SSH 依赖
1. 在 app/build.gradle.kts 添加 JSch 依赖：
   - implementation("com.jcraft:jsch:0.2.18")
2. 同步 Gradle

**验证**: `./gradlew :app:dependencies | grep jsch` 显示 JSch 0.2.18

#### Step 2.1.2: 创建 SSH 配置模型
1. 创建 SSHConfig.java 数据类：
   - host: String
   - port: int (默认 8022)
   - username: String
   - authType: AuthType (PASSWORD 或 PRIVATE_KEY)
   - password: String (可选)
   - privateKeyPath: String (可选)
   - knownHostsPath: String (可选)
   - claudeWrapperPath: String (wrapper 脚本路径)

**验证**: SSHConfig 可序列化/反序列化

#### Step 2.1.3: 创建连接状态模型
1. 创建 ConnectionState.java 密封类：
   - Disconnected
   - Connecting
   - Connected
   - Error(message: String)
   - Reconnecting(attempt: Int)

**验证**: when 表达式覆盖所有状态

#### Step 2.1.4: 实现 SSHClient 类
1. 创建 data/ssh/SSHClient.java
2. 实现连接方法：
   - connect(config: SSHConfig): boolean
   - disconnect()
   - executeCommand(command: String): String
   - getInputStream(): InputStream (用于 PTY 输出)
   - getOutputStream(): OutputStream (用于 PTY 输入)
3. 实现伪终端 (PTY) 支持：
   - openSession(): Session
   - requestPty(term: String, cols: int, rows: int)
   - startShell()

**验证**:
- 使用密码认证可连接 Termux
- 执行 `whoami` 返回正确用户名
- PTY 模式可获取完整输出（包括 ANSI 颜色码）

#### Step 2.1.5: 实现 TermuxRepository 接口
1. 创建 data/repository/TermuxRepositoryImpl.java
2. 实现 SSH 连接生命周期管理
3. 实现命令执行和流式输出
4. 处理连接断开和异常

**验证**:
- 连接成功回调 ConnectionState.Connected
- 连接失败回调 ConnectionState.Error
- 断开连接后正确清理资源

---

### 2.2 连接配置 UI

#### Step 2.2.1: 创建连接配置 Fragment
1. 创建 settings/ConnectionSettingsFragment.kt
2. 创建布局文件 fragment_connection_settings.xml：
   - EditText: 主机地址 (host)
   - EditText: 端口号 (port)
   - EditText: 用户名 (username)
   - RadioGroup: 认证方式 (密码/密钥)
   - EditText: 密码 (条件显示)
   - EditText: 私钥路径 (条件显示)
   - Button: 测试连接
   - Button: 保存配置

**验证**: 布局预览正常，表单元素对齐整齐

#### Step 2.2.2: 创建连接配置 ViewModel
1. 创建 ConnectionSettingsViewModel.kt
2. 管理表单状态 (host, port, username, authType, password, privateKeyPath)
3. 实现 validateConfig(): Boolean
4. 实现 testConnection(): Flow<ConnectionState>
5. 实现 saveConfig(): Boolean

**验证**:
- 表单验证正确（空字段报错）
- ViewModel 状态正确更新 UI

#### Step 2.2.3: 实现配置持久化
1. 使用 EncryptedSharedPreferences 存储敏感信息
2. 保存 SSHConfig 到加密存储
3. 加载时解密还原

**验证**:
- 保存配置后杀掉应用，重新打开配置仍存在
- 密码不在明文存储

#### Step 2.2.4: 实现连接测试功能
1. 在 ViewModel 中实现 testConnection()
2. 显示 Loading 状态
3. 返回成功/失败结果
4. Toast 显示错误原因

**验证**:
- 点击测试连接，显示 loading
- 连接成功显示成功提示
- 连接失败显示具体错误

---

### 2.3 连接状态管理

#### Step 2.3.1: 创建连接状态管理器
1. 创建 ConnectionManager.kt 单例或 Hilt 单例
2. 管理 SSHClient 实例
3. 维护当前连接状态
4. 提供 observeConnectionState(): StateFlow<ConnectionState>

**验证**: 多个 Fragment 可同时观察同一连接状态

#### Step 2.3.2: 在 MainActivity 中集成连接管理
1. MainActivity 持有 ConnectionManager 实例
2. 应用启动时自动尝试恢复上次连接（如果配置存在）
3. 处理连接状态变化更新 UI

**验证**:
- 应用启动自动显示连接状态
- 断开连接时底部状态栏更新

#### Step 2.3.3: 创建连接状态指示器 UI
1. 在布局中添加连接状态指示器：
   - 已连接: 绿色圆点 + "已连接"
   - 连接中: 橙色圆点 + "连接中..."
   - 断开: 红色圆点 + "未连接"
2. 可放在 Toolbar 下方或底部导航上方

**验证**: 状态指示器实时反映连接状态变化

---

### 2.4 自动重连机制

#### Step 2.4.1: 实现重连策略
1. 在 ConnectionManager 中实现重连逻辑
2. 配置重试参数：
   - 最大重试次数: 3
   - 重试间隔: 2 秒
   - 指数退避: 2s, 4s, 8s (可选)
3. 在 ConnectionState.Reconnecting 中记录尝试次数

**验证**:
- 断开连接后自动触发重连
- 重连失败达到最大次数后停止并通知用户

#### Step 2.4.2: 添加手动重连按钮
1. 在连接状态指示器旁边或设置页面添加重连按钮
2. 点击立即尝试连接
3. 重连中显示 loading

**验证**: 点击按钮可触发立即重连

---

### Phase 2 验收测试

**功能验收**:
- [ ] 可使用密码认证成功连接 Termux
- [ ] 可使用私钥认证成功连接 Termux
- [ ] 连接配置可保存和加载
- [ ] 连接状态正确显示
- [ ] 断开连接后自动重连（最多 3 次）
- [ ] 手动重连功能正常
- [ ] 连接测试功能正常

**安全验收**:
- [ ] 密码不以明文存储
- [ ] 私钥路径正确加密存储

---

## Phase 3：会话功能（第 5-7 周）

### 3.1 终端模拟器集成

#### Step 3.1.1: 准备 xterm.js 资源
1. 下载 xterm.js 4.x 版本
2. 复制以下文件到 assets/ 目录：
   - xterm.js
   - xterm.css
   - xterm-addon-fit.js
   - xterm-addon-web-links.js
3. 可选：复制 fonts/ 目录（如果需要自定义字体）

**验证**: assets/ 目录包含所有必需的 xterm.js 文件

#### Step 3.1.2: 创建终端 WebView 配置
1. 创建 TerminalWebViewClient.kt 继承 WebViewClient
2. 处理页面加载完成
3. 配置 WebSettings：
   - setJavaScriptEnabled(true)
   - setDomStorageEnabled(true)
   - setAllowFileAccess(true)
   - setCacheMode(WebSettings.LOAD_NO_CACHE)

**验证**: WebView 可加载本地 HTML 文件

#### Step 3.1.3: 创建终端 HTML 页面
1. 在 assets/ 创建 terminal.html
2. 引入 xterm.js CSS 和 JS
3. 初始化 Terminal 实例
4. 配置 fit addon
5. 暴露 JavaScript 接口供 Android 调用：
   - write(data: String)
   - resize(cols: Int, rows: Int)
   - clear()

**验证**: 在浏览器打开 terminal.html 显示终端

#### Step 3.1.4: 创建 TerminalFragment 布局
1. 创建 fragment_terminal.xml
2. 添加 WebView (id: terminal_webview)
3. 可选：添加底部快捷工具栏（新建Tab、键盘切换）

**验证**: 布局预览显示 WebView 占满屏幕

#### Step 3.1.5: 实现 TerminalFragment
1. 创建 TerminalFragment.kt
2. 加载 terminal.html
3. 配置 WebView 与 JavaScript 交互
4. 实现 JavaScriptInterface:
   - onDataReceived(data: String) - 从 SSH 接收数据写入终端
   - onConnectionStateChanged(state: String)

**验证**:
- 终端页面加载成功
- 可接收并显示 SSH 输出

#### Step 3.1.6: 实现终端输入
1. 添加系统软键盘输入支持
2. 将用户输入通过 JavaScriptInterface 发送到 SSH
3. 实现 SSH PTY 输入流写入

**验证**:
- 在终端页面输入字符，Termux 端可收到
- 执行命令后输出正确显示

#### Step 3.1.7: 处理终端尺寸调整
1. 监听 ViewTreeObserver.OnGlobalLayoutListener
2. 计算可见区域变化
3. 调用 Terminal.fit() 调整终端大小
4. 调用 resize 通知 PTY 新的 cols/rows

**验证**:
- 旋转屏幕终端正确调整大小
- 键盘弹出时终端高度正确减少

---

### 3.2 会话列表页面

#### Step 3.2.1: 创建会话列表布局
1. 创建 fragment_session_list.xml
2. 添加 RecyclerView 显示会话列表
3. 添加 FAB 用于创建新会话
4. 添加空状态视图（无会话时显示）

**验证**: 布局预览正常，空状态视图在列表为空时可见

#### Step 3.2.2: 创建会话列表项布局
1. 创建 item_session.xml
2. 显示会话名称、创建时间、最后活跃时间
3. 支持侧滑删除（SwipeRefreshLayout 或 ItemTouchHelper）

**验证**: 列表项布局显示正确信息

#### Step 3.2.3: 实现会话数据层
1. 创建 Room 数据库 AppDatabase.java
2. 创建 SessionDao.java:
   - getAll(): List<Session>
   - insert(session: Session)
   - update(session: Session)
   - delete(session: Session)
   - getById(id: String): Session?
   - **添加 MessageDao**:
     - getMessagesBySession(sessionId: String): List<Message>
     - insert(message: Message)
     - update(message: Message)
     - delete(message: Message)
     - deleteBySessionId(sessionId: String)
3. 在 SessionRepositoryImpl 中调用 DAO
4. **消息持久化说明**: Message.content 字段存储已渲染的 HTML，App 重启后直接从 Room 加载并显示，无需重新渲染

**验证**:
- 数据库创建成功
- CRUD 操作正常
- 消息持久化和恢复正常

#### Step 3.2.4: 实现 SessionAdapter
1. 创建 SessionAdapter.kt 继承 RecyclerView.Adapter
2. 实现 onCreateViewHolder, onBindViewHolder
3. 处理点击事件：选中会话
4. 处理侧滑删除

**验证**:
- 列表显示所有会话
- 点击选中会话
- 侧滑显示删除确认

#### Step 3.2.5: 实现 ChatViewModel
1. 创建 ChatViewModel.kt
2. 管理当前选中的会话
3. 管理消息列表
4. 调用 SessionRepository 管理会话

**验证**: ViewModel 正确管理会话状态

---

### 3.3 消息发送/接收

#### Step 3.3.1: 创建聊天布局
1. 创建 fragment_chat.xml
2. 上部: RecyclerView 显示消息列表
3. 底部: 输入框 (EditText) + 发送按钮 (ImageButton)
4. 配置键盘行为：scrollToBottomOnKeyboardShown

**验证**: 键盘弹出时输入框不被遮挡

#### Step 3.3.2: 创建消息项布局
1. 创建 item_message_user.xml (用户消息，右对齐)
2. 创建 item_message_bot.xml (AI 消息，左对齐)
3. 气泡样式使用 ShapeDrawable 或 CardView
4. 显示时间戳

**验证**: 两种消息类型布局正确显示

#### Step 3.3.3: 实现 ChatAdapter
1. 创建 ChatAdapter.kt
2. 支持两种 ViewHolder 类型
3. 实现 DiffUtil 计算差异
4. scrollToPosition() 支持底部定位

**验证**:
- 消息列表正确显示两种类型
- 新消息自动滚动到底部

#### Step 3.3.4: 实现消息发送
1. 在 ChatFragment 中实现发送逻辑
2. 获取输入框内容
3. 保存用户消息到列表
4. 通过 SSH 执行 Claude Code 命令
5. 清空输入框

**验证**:
- 发送消息后输入框清空
- 用户消息显示在列表中

#### Step 3.3.5: 实现消息接收
1. 在 ChatViewModel 中启动 SSH 输出流监听
2. 实时接收 Claude Code 输出
3. 追加到当前消息内容或创建新消息
4. 通知 Adapter 更新

**验证**:
- Claude Code 输出实时显示
- 流式输出逐步显示（不是一次性显示）

#### Step 3.3.6: 处理会话切换
1. 切换会话时断开当前 Claude Code 进程
2. 加载新会话的消息历史
3. 启动新会话的 Claude Code

**验证**:
- 切换会话后终端内容更新
- 原会话状态保留

---

### 3.4 Markdown 渲染

#### Step 3.4.1: 添加 Markwon 依赖
1. 在 app/build.gradle.kts 添加：
   - implementation("io.noties.markwon:core:4.6.2")
   - implementation("io.noties.markwon:ext-strikethrough:4.6.2")
   - implementation("io.noties.markwon:syntax-highlight:4.6.2")
2. 同步 Gradle

**验证**: `./gradlew :app:dependencies | grep markwon` 显示 Markwon 4.6.2

#### Step 3.4.2: 配置 Markwon
1. 创建 MarkwonFactory.kt
2. 配置插件：
   - StrikethroughPlugin
   - SyntaxHighlightPlugin (使用 Highlight.js)
   - 链接插件
3. 设置代码块背景色 (#2D2D2D)

**验证**: Markwon 实例正确创建，可渲染 Markdown

#### Step 3.4.3: 修改消息项布局支持 Markdown
1. 在 item_message_bot.xml 中将 TextView 替换为 Markwon 专用 View
2. 或者保持 TextView，在代码中应用 Markwon.toMarkdown()

**验证**: AI 消息中的 Markdown 正确渲染

#### Step 3.4.4: 处理代码块
1. 配置 Highlight.js 语法高亮
2. 支持常见语言检测
3. 代码块添加复制按钮

**验证**:
- 代码块有语法高亮
- 复制按钮可点击

---

### 3.5 代码高亮

#### Step 3.5.1: 添加 Highlight.js
1. 在 assets/ 添加 highlight.js 和 CSS
2. 或使用 Markwon 的 syntax-highlight 插件

**验证**: Highlight.js 正确加载

#### Step 3.5.2: 配置代码块样式
1. 设置代码块背景: #2D2D2D
2. 设置字体: 等宽字体
3. 设置内边距和圆角

**验证**: 代码块样式符合设计规范

---

### Phase 3 验收测试

**功能验收**:
- [ ] 终端输出正确显示（包括 ANSI 颜色）
- [ ] 终端输入正常工作
- [ ] 滚动操作流畅
- [ ] 长按文本可复制
- [ ] 会话列表正常显示
- [ ] 可创建、切换、删除会话
- [ ] 消息发送和接收正常
- [ ] Markdown 正确渲染
- [ ] 代码块语法高亮正确

**性能验收**:
- [ ] 终端输入延迟 < 100ms
- [ ] 列表滚动帧率 60fps
- [ ] 内存占用 < 200MB

---

## Phase 4：文件管理（第 8-9 周）

### 4.1 文件浏览器 UI

#### Step 4.1.1: 创建文件浏览器布局
1. 创建 fragment_files.xml
2. 使用双栏布局（Master-Detail）：
   - 左侧: 文件/目录列表 (RecyclerView)
   - 右侧: 文件预览区 (WebView 或 TextView)
3. 顶部显示当前路径 (Breadcrumb)
4. 底部工具栏: 新建、刷新、返回上级

**验证**: 布局在手机和平板上都正常显示

#### Step 4.1.2: 创建文件项布局
1. 创建 item_file.xml
2. 显示图标（根据文件类型）、文件名
3. 显示文件大小或目录子项数量
4. 支持展开/折叠指示器（目录）

**验证**: 文件图标正确区分类型

#### Step 4.1.3: 创建 FilesViewModel
1. 管理当前路径
2. 管理文件列表
3. 实现 navigateTo(path: String)
4. 实现 navigateUp(): Boolean
5. 实现 refresh()

**验证**: ViewModel 正确管理文件浏览状态

#### Step 4.1.4: 实现 FilesAdapter
1. 创建 FilesAdapter.kt
2. 支持目录和文件两种类型
3. 处理点击事件
4. 实现目录展开动画

**验证**:
- 目录点击进入
- 文件点击预览

---

### 4.2 文件树展示

#### Step 4.2.1: 实现目录列表 API
1. 在 TermuxRepository 中添加 listDirectory(path: String): List<FileItem>
2. 使用 SSH 执行 `ls -la` 命令
3. 解析输出生成 FileItem 列表

**验证**:
- 目录列表正确获取
- 文件类型正确识别

#### Step 4.2.2: 实现文件树逻辑
1. 支持展开目录查看子项
2. 缓存已加载的目录内容
3. 实现返回上级目录

**验证**:
- 可展开目录查看内容
- 可返回上级目录

#### Step 4.2.3: 实现路径导航
1. 显示当前完整路径
2. 点击路径片段快速跳转
3. 面包屑导航 UI

**验证**: 路径导航正确工作

---

### 4.3 文件操作（增删改）

#### Step 4.3.1: 实现文件预览
1. 点击文件时在右侧预览区显示
2. 小文件直接读取显示
3. 大文件分页加载
4. 非文本文件显示文件信息

**验证**:
- 文本文件预览正常
- 大文件不会导致 ANR

#### Step 4.3.2: 实现新建文件/文件夹
1. 底部工具栏添加新建按钮
2. Dialog 输入名称
3. 选择类型（文件/文件夹）
4. SSH 执行 mkdir 或 touch 命令

**验证**:
- 可创建文件
- 可创建文件夹
- 创建后列表刷新

#### Step 4.3.3: 实现删除
1. 长按文件显示上下文菜单
2. 选择删除选项
3. 二次确认 Dialog
4. SSH 执行 rm 或 rm -rf 命令

**验证**:
- 删除前显示确认
- 删除后列表刷新

#### Step 4.3.4: 实现重命名
1. 长按文件选择重命名
2. Dialog 编辑新名称
3. SSH 执行 mv 命令

**验证**: 重命名后文件名更新

---

### 4.4 代码预览

#### Step 4.4.1: 配置代码高亮预览
1. 在文件预览区使用 WebView + Highlight.js
2. 根据文件扩展名设置语言
3. 支持行号显示

**验证**:
- 代码文件语法高亮
- 行号正确显示

#### Step 4.4.2: 处理大文件
1. 设置文件大小限制（如 1MB）
2. 超过限制显示警告
3. 提供分页或只读模式

**验证**: 大文件不会导致 ANR

---

### Phase 4 验收测试

**功能验收**:
- [ ] 可浏览 Termux 文件系统
- [ ] 目录结构正确显示
- [ ] 可进入子目录
- [ ] 可返回上级目录
- [ ] 可预览代码文件
- [ ] 语法高亮正确
- [ ] 可创建文件/文件夹
- [ ] 可删除文件/文件夹
- [ ] 可重命名文件

---

## Phase 5：完善与优化（第 10-12 周）

### 5.1 主题切换

#### Step 5.1.1: 创建主题资源
1. 在 res/values/ 创建 colors.xml:
   - primary: #6750A4
   - onPrimary: #FFFFFF
   - background_dark: #1C1B1F
   - background_light: #FFFBFE
   - surface_dark: #2D2D2D
   - surface_light: #F5F5F5
2. 在 res/values-night/ 创建对应浅色主题 colors

**验证**: 颜色资源正确引用

#### Step 5.1.2: 创建主题选择 Dialog
1. 在设置页面添加主题选项
2. 选项：深色模式、浅色模式、跟随系统
3. 使用 RadioGroup 或 ListPreference

**验证**: 主题选项正确显示

#### Step 5.1.3: 实现主题切换逻辑
1. 在 AppCompatDelegate 中设置模式
2. 保存用户偏好到 SharedPreferences
3. 应用启动时恢复主题设置

**验证**:
- 切换主题后界面立即更新
- 主题偏好正确保存

---

### 5.2 字体大小

#### Step 5.2.1: 创建字体大小选项
1. 在设置页面添加字体大小选项
2. 选项：小、中、大、超大
3. 使用 ListPreference

**验证**: 字体大小选项正确显示

#### Step 5.2.2: 实现字体大小切换
1. 定义字体大小 dimension 资源
2. 应用到终端、聊天、文件浏览器的文本
3. 动态重新创建 View 或通知更新

**验证**: 字体大小切换生效

---

### 5.3 性能优化

#### Step 5.3.1: 优化终端滚动
1. 使用 RecyclerView 替代 ListView
2. 实现 view holder 复用
3. 限制显示的行数（保留最近 N 行）

**验证**: 滚动流畅无卡顿

#### Step 5.3.2: 优化内存占用
1. 使用 LeakCanary 检测内存泄漏
2. 修复发现的泄漏
3. 限制缓存大小

**验证**: 内存占用 < 200MB

#### Step 5.3.3: 优化启动速度
1. 使用 SplashScreen API
2. 延迟加载非必要组件
3. 测量冷启动时间 < 3秒

**验证**: 冷启动时间 < 3秒

---

### 5.4 Bug 修复

#### Step 5.4.1: 集成崩溃报告
1. 集成 Firebase Crashlytics 或 ACRA
2. 配置 ProGuard 混淆
3. 上传 mapping 文件

**验证**: 崩溃报告正确收集

#### Step 5.4.2: 修复已知问题
1. 修复测试阶段发现的所有 bug
2. 修复边缘情况：
   - 网络断开
   - Termux 服务停止
   - Claude Code 异常退出

**验证**: 所有 P0 和 P1 bug 已修复

---

### 5.5 测试

#### Step 5.5.1: 单元测试
1. 为 ViewModel 编写单元测试
2. 为 Repository 编写单元测试
3. 使用 JUnit 4 + Mockito

**验证**: 所有单元测试通过

#### Step 5.5.2: 集成测试
1. 测试 SSH 连接流程
2. 测试会话管理流程
3. 测试文件操作流程

**验证**: 集成测试通过

#### Step 5.5.3: UI 测试
1. 使用 Espresso 编写 UI 测试
2. 测试关键用户流程：
   - 连接 Termux
   - 发送消息
   - 切换会话

**验证**: UI 测试通过

#### Step 5.5.4: 兼容性测试
1. 在不同 Android 版本测试：
   - API 24 (Android 7.0)
   - API 28 (Android 9)
   - API 34 (Android 14)
2. 测试不同屏幕尺寸

**验证**: 各版本和尺寸设备测试通过

---

### 5.6 Release APK 构建与签名

#### Step 5.6.1: 配置 release 签名
1. 在 android{} 块中配置 signingConfigs:
   - 创建 release signing config
   - 指定 keystore 文件路径、keystore 密码、key alias、key 密码
2. 配置 buildTypes:
   - release: 启用 minifyEnabled、shrinkResources
   - 启用 ProGuard/R8 混淆
   - 关联 signing config

**验证**: `./gradlew assembleRelease` 成功生成 APK

#### Step 5.6.2: 配置 ProGuard 规则
1. 创建 app/proguard-rules.pro
2. 添加 JSch 保持规则:
   - `-keep class com.jcraft.** { *; }`
3. 添加 Room 保持规则:
   - `-keep class * extends androidx.room.RoomDatabase`
4. 添加 Hilt 保持规则:
   - `-keep class dagger.hilt.** { *; }`
   - `-keep class javax.inject.** { *; }`
5. 添加 Markwon 保持规则:
   - `-keep class io.noties.markwon.** { *; }`

**验证**: ProGuard 混淆后应用仍可正常运行

#### Step 5.6.3: 生成 signed APK
1. 执行 `./gradlew assembleRelease`
2. 确认输出 APK 已签名（使用 apksigner verify）
3. 确认 APK 位于 app/build/outputs/apk/release/

**验证**: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` 显示签名信息

---

### Phase 5 验收测试

**功能验收**:
- [ ] 深色/浅色主题切换正常
- [ ] 字体大小调整生效
- [ ] 性能指标达标
- [ ] 无 P0/P1 bug

**测试验收**:
- [ ] 单元测试覆盖率 > 70%
- [ ] 所有集成测试通过
- [ ] UI 测试覆盖核心流程
- [ ] 多设备兼容性测试通过
- [ ] Release APK 签名验证通过

---

## 最终验收清单

### 必完成项（MVP）
- [ ] SSH 密码认证连接 Termux
- [ ] SSH 密钥认证连接 Termux
- [ ] 终端显示 Claude Code 输出
- [ ] 发送消息并收到响应
- [ ] 会话创建、切换、删除
- [ ] 连接配置保存/加载
- [ ] 深色主题正确应用
- [ ] **Release APK 成功构建并签名**

### 可选完成项
- [ ] 自动重连
- [ ] Markdown 渲染
- [ ] 代码高亮
- [ ] 文件浏览器
- [ ] 浅色主题
- [ ] 字体大小调整

---

## 附录

### A. 关键文件路径

| 文件 | 路径 | 语言 |
|------|------|------|
| 项目级 build.gradle | app/build.gradle.kts | Kotlin |
| Application 类 | app/src/main/java/com/claudebox/ClaudeBoxApp.kt | Kotlin |
| MainActivity | app/src/main/java/com/claudebox/ui/main/MainActivity.kt | Kotlin |
| Domain 模型 | app/src/main/java/com/claudebox/domain/model/ | **Java** |
| Repository 接口 | app/src/main/java/com/claudebox/domain/repository/ | **Java** |
| SSHClient | app/src/main/java/com/claudebox/data/ssh/SSHClient.java | **Java** |
| Room 数据库 | app/src/main/java/com/claudebox/data/local/AppDatabase.java | **Java** |
| SessionDao / MessageDao | app/src/main/java/com/claudebox/data/local/dao/ | **Java** |
| Repository 实现 | app/src/main/java/com/claudebox/data/repository/ | **Java** |
| Hilt 模块 | app/src/main/java/com/claudebox/di/ | **Java** |
| ViewModel | app/src/main/java/com/claudebox/ui/*/ | Kotlin |
| Fragment / Adapter | app/src/main/java/com/claudebox/ui/*/ | Kotlin |
| 导航图 | app/src/main/res/navigation/nav_graph.xml | XML |
| 终端 HTML | app/src/main/assets/terminal.html | HTML/JS |

### B. 参考文档

| 文档 | 位置 |
|------|------|
| PRD 设计文档 | memory-bank/PRD-design-document.md |
| 技术栈文档 | memory-bank/tech-stack.md |
| 研究文档 | memory-bank/Research.md |
| 架构文档 | memory-bank/architecture.md |

### C. 外部依赖清单

| 依赖 | 版本 | 用途 |
|------|------|------|
| JSch | 0.2.18 | SSH 连接 |
| Dagger Hilt | 2.48 | 依赖注入 |
| Room | 2.6.1 | 数据库 |
| Kotlin Coroutines | 1.7.3 | 异步编程 |
| Markwon | 4.6.2 | Markdown 渲染 |
| xterm.js | 4.x | 终端模拟 |
| Material Components | 1.11.0 | UI 组件 |
| Navigation | 2.7.6 | 导航 |

### D. Termux 配置指南

用户在使用 ClaudeBox 前需在 Termux 中完成以下配置：

#### D.1 安装 Termux
1. 从 F-Droid 或 GitHub 下载并安装 Termux（不要从 Google Play 安装，旧版本有兼容性问题）
2. 首次打开 Termux，执行 `termux-setup-storage` 授权存储权限

#### D.2 安装 openssh
```bash
pkg update
pkg install openssh
```

#### D.3 启动 sshd 服务
```bash
# 设置密码（如果使用密码认证）
passwd

# 启动 sshd（默认监听 8022 端口）
sshd

# 确认 sshd 运行
ps aux | grep sshd
```

#### D.4 生成 SSH 密钥（推荐，免密码）
```bash
ssh-keygen -t ed25519 -C "claudebox"
# 按回车使用默认路径 ~/.ssh/id_ed25519
# 输入密码（可选）

# 将公钥添加到 authorized_keys
cat ~/.ssh/id_ed25519.pub >> ~/.ssh/authorized_keys
```

#### D.5 确认 Termux IP 地址
```bash
# 查看 IP
ip addr show wlan0

# 测试连接（从另一台设备）
ssh -p 8022 <用户名>@<IP>
```

#### D.6 安装 Claude Code CLI
```bash
# 在 Termux 中安装 Claude Code CLI
# 参考官方文档: https://docs.anthropic.com/en/docs/claude-code

# 验证 claude 命令可用
claude --version
```

#### D.7 创建 wrapper 脚本（App 通过 SSH 调用）
```bash
# 创建脚本目录
mkdir -p ~/.local/bin

# 创建 claude-wrapper.sh
cat > ~/.local/bin/claude-wrapper.sh << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
cd ~/projects  # 或其他工作目录
exec claude --print "$@"
EOF

# 添加执行权限
chmod +x ~/.local/bin/claude-wrapper.sh

# 测试
./claude-wrapper.sh "Hello"
```

#### D.8 防火墙注意事项
- Android 6.0+ 需要允许 Termux 的 SSH 端口
- 部分设备需要在开发者选项中关闭 USB 调试限制
