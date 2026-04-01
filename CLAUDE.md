CLAUDE.md
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
ClaudeBox is an Android GUI client that connects to Termux via SSH and provides a mobile interface for Claude Code CLI (AI programming assistant).

## Architecture
Pattern: MVVM + Repository
UI Layer     → Activities/Fragments/ViewModels
Domain Layer → UseCases/Repository Interfaces
Data Layer   → TermuxRepoImpl/LocalCache/SessionManager
Communication: SSH 连接到 Termux (pseudo-terminal for real-time streaming)

## Four Main Modules:
- `ChatFragment` - Claude Code conversation (Markdown/代码高亮)
- `TerminalFragment` - Full terminal emulator (ANSI color, xterm.js)
- `FilesFragment` - File browser with tree view
- `SettingsFragment` - SSH config, theme, font settings

## Tech Stack
- Language: Java 17 / Kotlin
- Min SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- Build: Gradle 8.x + AGP 8.x
- DI: Dagger Hilt
- Database: Room (session storage)
- SSH: JSch or sshtools:maverick
- Terminal: xterm.js + WebView
- Markdown: Markwon
- Async: RxJava3 / Kotlin Coroutines

## Development Phases
- Phase 1: Project setup, MVVM skeleton, navigation framework
- Phase 2: SSH connection module
- Phase 3: Session/terminal features, chat UI
- Phase 4: File browser
- Phase 5: Polish, testing, bug fixes


## Important Notes (ALWAYS APPLY)MUST
⚠️ **强制规则（AI 必须遵守）：**
# 重要提示：
# 写任何代码前必须完整阅读 memory-bank/PRD-design-document.md
# 写任何代码前必须完整阅读 memory-bank/implementation-plan.md
# 写任何代码前必须完整阅读 memory-bank/tech-stack.md
# 写任何代码前必须完整阅读 memory-bank/progress.md
# 每完成一个重大功能或里程碑后，必须更新 memory-bank/architecture.md

This is a pre-development stage project - no source code exists yet
`memory-bank/` contains planning documents (Research.md, etc.)
Termux and Claude Code CLI must be installed separately on the device
Dark theme (Material You #6750A4) is the primary design