# ClaudeBox 架构文档

## 架构概览

**模式**: MVVM + Repository

```
UI Layer     → Activities/Fragments/ViewModels (Kotlin)
Domain Layer → UseCases/Repository Interfaces (Java)
Data Layer   → Repository Impl/SSH Client/Local Cache (Java)
```

## 目录结构

```
app/src/main/
├── java/com/claudebox/
│   ├── ClaudeBoxApp.kt              # Hilt Application 入口
│   ├── ui/                         # UI 层 (Kotlin)
│   │   ├── main/
│   │   │   └── MainActivity.kt     # 导航宿主，管理 BottomNavigation
│   │   ├── chat/                   # 聊天模块
│   │   │   ├── ChatFragment.kt
│   │   │   └── ChatViewModel.kt
│   │   ├── terminal/               # 终端模块
│   │   │   ├── TerminalFragment.kt
│   │   │   └── TerminalViewModel.kt
│   │   ├── files/                  # 文件模块
│   │   │   ├── FilesFragment.kt
│   │   │   └── FilesViewModel.kt
│   │   └── settings/               # 设置模块
│   │       ├── SettingsFragment.kt
│   │       └── SettingsViewModel.kt
│   ├── domain/                     # 领域层 (Java)
│   │   ├── model/
│   │   │   ├── Session.java        # 会话实体
│   │   │   ├── Message.java        # 消息实体
│   │   │   └── FileItem.java       # 文件项实体
│   │   └── repository/
│   │       ├── SessionRepository.java  # 会话仓库接口
│   │       └── TermuxRepository.java  # Termux 仓库接口
│   ├── data/                       # 数据层 (Java)
│   │   ├── ssh/
│   │   │   ├── SSHConfig.java      # SSH 连接配置
│   │   │   ├── ConnectionState.java # 连接状态（5种状态）
│   │   │   └── SSHClient.java      # SSH 客户端封装
│   │   ├── repository/
│   │   │   ├── TermuxRepositoryImpl.java  # Termux 仓库实现
│   │   │   └── ConnectionManager.java      # 连接管理器（单例）
│   │   └── local/
│   │       └── ConfigManager.java  # 加密配置存储
│   └── di/
│       └── AppModule.java          # Hilt 依赖注入模块
├── res/
│   ├── navigation/nav_graph.xml    # 导航图，定义4个 Fragment 目的地
│   ├── menu/bottom_nav_menu.xml    # 底部导航栏菜单
│   ├── layout/
│   │   ├── activity_main.xml       # 主布局（NavHost + BottomNav）
│   │   └── fragment_*.xml          # 4个 Fragment 布局
│   ├── values/
│   │   ├── themes.xml              # Material3 深色主题
│   │   ├── colors.xml              # 颜色定义（Material You #6750A4）
│   │   └── strings.xml             # 字符串资源
│   └── drawable/
│       ├── ic_*.xml                # Vector 图标
│       └── connection_indicator_*.xml  # 连接状态指示器
└── assets/                         # 预留（xterm.js 等）
```

---

## 核心文件说明

### Gradle 配置

| 文件 | 作用 |
|------|------|
| `settings.gradle.kts` | 项目配置，定义仓库（google/mavenCentral/jitpack）和模块包含 |
| `build.gradle.kts` (根) | 全局插件版本，AGP 8.2.2, Kotlin 1.9.22, Hilt 2.48.1 |
| `gradle.properties` | Gradle 属性，AndroidX 启用，Jetifier |
| `app/build.gradle.kts` | 模块依赖声明：Material, Navigation, Room, Hilt, Coroutines, JSch (mwiede fork), Markwon |
| `proguard-rules.pro` | ProGuard 混淆规则（JSch, Room, Hilt, Markwon） |

### Domain 层 (Java)

| 类 | 作用 |
|----|------|
| `Session` | 会话实体：id, name, createdAt, lastActiveAt |
| `Message` | 消息实体：id, sessionId, content(HTML), rawContent(原始), isFromUser, timestamp |
| `FileItem` | 文件项：name, path, isDirectory, size, modifiedAt |
| `SessionRepository` | 会话 CRUD 接口：getSessions, createSession, deleteSession, getSession |
| `TermuxRepository` | SSH 操作接口：connect, disconnect, executeCommand, isConnected + 回调接口 |

### Data 层 (Java)

| 类 | 作用 |
|----|------|
| `SSHConfig` | SSH 连接参数：host, port, username, authType, password, privateKeyPath, knownHostsPath, claudeWrapperPath |
| `ConnectionState` | 连接状态基类，子类：Disconnected, Connecting, Connected, Error(message), Reconnecting(attempt) |
| `SSHClient` | JSch 封装：connect, disconnect, executeCommand, openShellChannel, ShellChannel 内部类 |
| `TermuxRepositoryImpl` | TermuxRepository 实现：回调模式，ExecutorService 单线程执行，Claude 会话管理 |
| `ConnectionManager` | 单例连接管理器：状态观察，AtomicReference 线程安全，自动重连（3次/指数退避） |
| `ConfigManager` | EncryptedSharedPreferences 封装：AES256_GCM 加密存储 SSH 配置 |

### UI 层 (Kotlin)

| 类 | 作用 |
|----|------|
| `ClaudeBoxApp` | Hilt Application，onCreate 中初始化 |
| `MainActivity` | 导航宿主，setupWithNavController 绑定 BottomNavigation 与 NavController |
| `ChatFragment/ViewModel` | 聊天界面，HiltViewModel 注入 |
| `TerminalFragment/ViewModel` | 终端界面，HiltViewModel 注入 |
| `FilesFragment/ViewModel` | 文件浏览界面，HiltViewModel 注入 |
| `SettingsFragment/ViewModel` | 设置界面：SSH 配置 UI，连接状态显示，连接控制 |

### 资源文件

| 文件 | 作用 |
|------|------|
| `nav_graph.xml` | Navigation Component 图，定义 chat/terminal/files/settings 四个目的地 |
| `bottom_nav_menu.xml` | 底部导航菜单，图标+标题对应四个 Fragment |
| `activity_main.xml` | ConstraintLayout 包裹 NavHostFragment + BottomNavigationView |
| `fragment_settings.xml` | 连接配置表单：主机/端口/用户名/认证方式/密码/私钥路径 |
| `themes.xml` | Material3.Dark.NoActionBar 主题，主色 #6750A4，背景 #1C1B1F |
| `colors.xml` | 完整 Material You 色彩系统定义 + 连接状态颜色 |
| `connection_indicator_*.xml` | 圆形状态指示器：红色(断开)/绿色(连接)/橙色(连接中) |

---

## 语言分层策略

- **Domain 层**: Java（模型、仓库接口）- 可以在 Java/Kotlin 间共享
- **Data 层**: Java（Repository 实现、SSH 客户端）
- **UI 层**: Kotlin（Activity、Fragment、ViewModel、Adapter）

**注意**: Java 层与 Kotlin 层通过回调接口通信，避免在 Java 中使用 Kotlin 协程 API

---

## 依赖注入

使用 **Dagger Hilt**：
- `@HiltAndroidApp` 注解 Application 类
- `@HiltViewModel` 注解 ViewModel
- `@Provides @Singleton` 在 `AppModule` 中提供单例

---

## SSH 连接架构

```
SettingsFragment
    │
    ├── SettingsViewModel
    │       │
    │       ├── ConfigManager.saveSSHConfig() → EncryptedSharedPreferences
    │       │
    │       └── ConnectionManager.connect() ───────────────────────┐
    │                                                               │
    └───────────────────────────────────────────────────────────────┘
                                                                    │
ConnectionManager (单例)                                             │
    │                                                               │
    └── TermuxRepository.connect(config)                            │
            │                                                       │
            └── SSHClient.connect(config)                          │
                    │                                               │
                    └── JSch Session.connect() ←─ 密码/私钥认证      │
                                                                    │
SettingsFragment ←── observeConnectionState() ←────────────────────┘
        │
        └── updateConnectionUI(state) → 红/绿/橙指示器
```

### 连接状态流

```
User 点击 Connect
       │
       ▼
ConnectionManager.connect(config)
       │
       ├── ConnectionState.Connecting (发射)
       │
       ├── TermuxRepository.connect()
       │       │
       │       └── SSHClient.connect()
       │               │
       │               └── 成功 → ConnectionState.Connected
       │               │
       │               └── 失败 → ConnectionState.Error → 自动重连
       │
       ▼
SettingsFragment 观察状态 → 更新 UI
```

---

## 自动重连机制

```
连接失败
    │
    ▼
ConnectionManager.handleReconnect()
    │
    ├── reconnectAttempt++ (AtomicInteger)
    │
    ├── if attempt <= 3:
    │       │
    │       └── Thread.sleep(2000 * attempt)  // 指数退避: 2s, 4s, 8s
    │               │
    │               └── connect(lastConfig) 递归重试
    │
    └── else:
            │
            └── ConnectionState.Error("连接失败，已停止重试")
```

---

## 后续扩展

- Phase 3: 添加 `assets/terminal.html` + xterm.js 终端模拟器
- Phase 3: 添加 `data/local/dao/` - Room 数据库和 DAO
- Phase 3: 添加 `ui/chat/ChatAdapter.kt` - 消息列表适配器
- Phase 4: 文件浏览器完整实现
- Phase 5: 添加 BaseFragment/BaseViewModel 基类
