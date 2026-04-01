# Claude Code Android GUI客户端 开发计划书
## Test git
## 一、项目概述
 
**项目名称**: ClaudeBox (暂定)

**项目目标**: 开发一款Android图形界面客户端，通过Termux作为后端运行环境，调用运行在Termux中的Claude Code CLI，实现AI编程辅助功能的移动端体验。

**核心思路**:
```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│  Android GUI    │ ---> │  Termux API     │ ---> │  Claude Code    │
│  (Java/Kotlin)  │      │  (Shell/SSH)    │      │  CLI            │
└─────────────────┘      └─────────────────┘      └─────────────────┘
```

## 二、技术架构

### 2.1 整体架构: MVVM + Repository模式

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│   Activities / Fragments / ViewModels                        │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                             │
│   UseCases / Repository Interfaces                           │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                              │
│   TermuxRepoImpl / LocalCache / SessionManager               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 与Termux的通信方案

**方案A: Termux Boot / SharedPreferences + Intent触发**
- 通过Intent触发Termux执行命令
- 通过SharedPreferences或文件进行数据交换

**方案B: Termux API App (推荐)**
- 安装Termux:API应用
- 通过Android Intent调用 `am broadcast` 或 `termux-clipboard-*`
- 适合轻量级交互

**方案C: SSH连接到Termux**
- 在Termux中运行SSH服务
- App通过SSH执行命令并获取输出
- 适合实时会话场景

**推荐方案C**，因为Claude Code是对话式交互，需要实时双向通信。

## 三、核心功能模块

### 3.1 会话管理模块
- 创建/删除/切换Claude Code会话
- 会话历史记录本地存储
- 会话状态持久化

### 3.2 终端模拟器模块
- 集成终端模拟器核心（使用webview或(chromium)）
- 支持ANSI颜色转义序列
- 代码语法高亮显示
- 支持多会话Tab

### 3.3 文件浏览器模块
- 展示Termux工作目录文件树
- 文件预览（代码文件高亮）
- 常用操作：新建/编辑/删除/重命名
- 快捷目录导航

### 3.4 Claude Code交互模块
- 发送消息到Claude Code CLI
- 实时接收AI响应流
- 支持Markdown渲染
- 代码块识别与高亮
- 复制/重发消息

### 3.5 设置模块
- Termux连接配置（主机/端口/密钥）
- Claude Code模型选择
- 主题设置（深色/浅色）
- 字体大小调整

## 四、技术栈与依赖库

### 4.1 基础框架
- **语言**: Java 17 (或 Kotlin 用于UI层)
- **最小SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 14 (API 34)
- **构建工具**: Gradle 8.x + AGP 8.x

### 4.2 核心依赖
```gradle
// 终端模拟器核心
implementation 'com.mikeolleng.ip:chromium-view:1.0.0'  // 或使用 webview + xterm.js

// SSH通信
implementation 'com.sshtools:maverick:1.0.0'

// 代码高亮
implementation 'io.github.nicklass:highlightjs:11.9.0'

// Markdown渲染
implementation 'io.noties.markwon:core:4.6.2'

// 依赖注入
implementation 'com.google.dagger:hilt-android:2.48'

// 响应式编程
implementation 'io.reactivex.rxjava3:rxjava:3.1.8'
implementation 'io.reactivex.rxjava3:rxandroid:3.0.2'

// 数据库 (会话存储)
implementation 'androidx.room:room-runtime:2.6.1'

// 协程
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### 4.3 Termux相关
- Termux App (用户需独立安装)
- Termux:API App (可选，用于传感器等)
- termux-sshd (在Termux内安装openssh)

## 五、UI/UX设计

### 5.1 整体风格
- **设计语言**: Material Design 3
- **主题**: 深色模式优先（开发者友好）
- **配色方案**:
  - 主色: #6750A4 (Material You 紫色)
  - 背景: #1C1B1F (深色) / #FFFBFE (浅色)
  - 代码背景: #2D2D2D

### 5.2 页面结构

```
MainActivity
├── BottomNavigation
│   ├── 会话 (ChatFragment)
│   ├── 终端 (TerminalFragment)
│   ├── 文件 (FilesFragment)
│   └── 设置 (SettingsFragment)
└── ViewPager2 (用于多会话)
```

### 5.3 关键界面

**会话页面**:
- 类似聊天界面
- 左侧: Claude回复 (头像+气泡)
- 右侧: 用户消息 (气泡)
- 底部: 输入框 + 发送按钮
- 支持Markdown/代码高亮

**终端页面**:
- 全屏终端模拟器
- 支持多点触控滚动
- 长按选择文本
- 浮动按钮: 新建Tab/会话列表

**文件页面**:
- 双栏布局 (目录树 + 文件预览)
- 文件图标根据类型区分
- 长按菜单: 打开/编辑/删除/重命名

## 六、文件结构规划

```
app/
├── src/main/
│   ├── java/com/claudebox/
│   │   ├── ClaudeBoxApp.java
│   │   │
│   │   ├── ui/
│   │   │   ├── main/
│   │   │   │   ├── MainActivity.java
│   │   │   │   └── MainViewModel.java
│   │   │   ├── chat/
│   │   │   │   ├── ChatFragment.java
│   │   │   │   ├── ChatViewModel.java
│   │   │   │   └── ChatAdapter.java
│   │   │   ├── terminal/
│   │   │   │   ├── TerminalFragment.java
│   │   │   │   └── TerminalViewModel.java
│   │   │   ├── files/
│   │   │   │   ├── FilesFragment.java
│   │   │   │   └── FilesViewModel.java
│   │   │   └── settings/
│   │   │       ├── SettingsFragment.java
│   │   │       └── SettingsViewModel.java
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Message.java
│   │   │   │   ├── Session.java
│   │   │   │   └── FileItem.java
│   │   │   ├── repository/
│   │   │   │   ├── SessionRepository.java
│   │   │   │   └── TermuxRepository.java
│   │   │   └── usecase/
│   │   │       ├── SendMessageUseCase.java
│   │   │       └── ManageSessionUseCase.java
│   │   │
│   │   └── data/
│   │       ├── repository/
│   │       │   ├── SessionRepositoryImpl.java
│   │       │   └── TermuxRepositoryImpl.java
│   │       ├── local/
│   │       │   ├── AppDatabase.java
│   │       │   └── dao/
│   │       │       └── SessionDao.java
│   │       └── ssh/
│   │           └── SSHClient.java
│   │
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   └── drawable/
│   │
│   └── AndroidManifest.xml
│
├── build.gradle
└── proguard-rules.pro
```

## 七、开发里程碑

### Phase 1: 基础框架搭建 (第1-2周)
- [ ] 项目初始化，Gradle配置
- [ ] MVVM架构架子搭建
- [ ] 导航框架实现 (BottomNavigation + Fragment)
- [ ] 基础UI组件封装

### Phase 2: Termux连接模块 (第3-4周)
- [ ] SSH连接实现
- [ ] 连接配置UI
- [ ] 连接状态管理
- [ ] 自动重连机制

### Phase 3: 会话功能 (第5-7周)
- [ ] 终端模拟器集成
- [ ] 会话列表页面
- [ ] 消息发送/接收
- [ ] Markdown渲染
- [ ] 代码高亮

### Phase 4: 文件管理 (第8-9周)
- [ ] 文件浏览器UI
- [ ] 文件树展示
- [ ] 文件操作（增删改）
- [ ] 代码预览

### Phase 5: 完善与优化 (第10-12周)
- [ ] 主题切换
- [ ] 离线缓存
- [ ] 性能优化
- [ ] Bug修复
- [ ] 测试

## 八、关键问题与解决方案

### Q1: 如何与Termux中的Claude Code通信？
**解决**: 通过SSH连接到Termux中运行的SSH服务，执行命令并通过stdout获取结果。

### Q2: Claude Code是对话式CLI，如何处理多轮交互？
**解决**: 维护一个Python/Shell包装脚本，在Termux中管理会话状态，App端只负责传递消息和展示结果。

### Q3: 代码如何实时显示？
**解决**: 使用终端模拟器库（如xterm.js配合WebView），通过SSH的pseudo-terminal获取实时输出流。

## 九、总结

本项目采用 **Java + Android SDK + MVVM** 架构，通过 **SSH** 与 **Termux** 中运行的 Claude Code CLI 通信，实现一个功能完整的移动端AI编程助手界面。
