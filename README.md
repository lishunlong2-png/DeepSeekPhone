# DeepSeek 手机版（Android）

一个用 **Kotlin + Jetpack Compose** 编写的原生 Android 聊天客户端，直连
[DeepSeek 开放平台](https://platform.deepseek.com) API。

> 本质：App 是「壳」，模型在云端。手机只负责界面与输入输出，每次提问通过
> HTTPS 发到 DeepSeek 服务器，回复以 SSE 流式传回，实现打字机效果。
> 离线时 App 不可用——这是所有主流 AI App 的标准架构。

## 功能

- ✅ 流式回复（打字机效果）
- ✅ 完整对话历史（多轮上下文）
- ✅ 双模型切换：`deepseek-chat`（V3，日常）/ `deepseek-reasoner`（R1，深度推理）
- ✅ API Key 仅存本机（SharedPreferences 私有存储）
- ✅ 首次使用引导界面
- ✅ 自动滚动到底部、发送中状态指示、错误提示

## 目录结构

```
DeepSeekPhone/
├── settings.gradle.kts              # 仓库与项目声明
├── build.gradle.kts                 # 插件版本
├── gradle.properties
├── gradle/wrapper/                  # Gradle Wrapper 配置
└── app/
    ├── build.gradle.kts             # 依赖与构建配置
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/deepseekphone/
        │   ├── MainActivity.kt          # 入口
        │   ├── data/
        │   │   ├── ChatModels.kt        # 消息模型
        │   │   ├── DeepSeekClient.kt    # API 客户端（SSE 流式）
        │   │   └── KeyStore.kt          # API Key 本地存储
        │   └── ui/
        │       ├── ChatViewModel.kt     # 状态管理
        │       └── ChatScreen.kt        # 聊天界面（Compose）
        └── res/                         # 字符串 / 主题 / 图标
```

## 第一步：申请 API Key（约 5 分钟）

1. 打开 **https://platform.deepseek.com**（建议电脑上操作，手机浏览器也行）
2. 注册 / 登录（支持手机号或邮箱）
3. 登录后左侧菜单点 **「API Keys」** → **「创建 API Key」**
4. 复制生成的一串 `sk-` 开头的 Key —— **只显示一次，务必立即保存**
5. 充值：右上角 **「充值」**，按量计费。日常聊天建议先充 **¥10** 就够用很久

> ⚠️ 安全须知
> - Key 就是你的「账号密码」，**不要发给任何人、不要提交到 Git**、不要贴到网上
> - 本项目 Key 只存在你手机的私有存储里，App 不会把它发给除 DeepSeek 官方 API 外的任何地方
> - 若 Key 泄露，在开放平台页面「删除」后重新创建即可

### 费用参考（以官网实时价格为准）

| 模型 | 计费方式 | 大概量级 |
|------|---------|---------|
| deepseek-chat（V3） | 按输入/输出 token 计费 | 一次普通问答通常不到 1 分钱 |
| deepseek-reasoner（R1） | 同上，推理 token 更多 | 约为 V3 的 2~4 倍 |

## 第二步：构建（需要 Android Studio）

环境要求：**Android Studio**（Hedgehog 2023.1.1 或更新版本）、JDK 17（Android Studio 自带）。

1. 下载安装 [Android Studio](https://developer.android.com/studio)（Windows 选 `.exe` 安装包）
2. 启动后 **File → Open**，选择本 `DeepSeekPhone` 文件夹
3. 等待 Gradle 同步完成（首次会下载依赖，需要网络，约几分钟）
4. 连接手机（需开启 **开发者模式**：设置 → 关于手机 → 连点「版本号」7 次 → 打开「USB 调试」）
   或用 Android Studio 自带的模拟器
5. 点顶部绿色三角 **Run ▶**，即可安装到手机

## 第三步：打包 APK 自己装（不上架）

1. Android Studio 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. 产物路径：`app/build/outputs/apk/debug/app-debug.apk`
3. 把 APK 传到手机（微信/QQ/数据线均可），点击安装
   （需允许「安装未知来源应用」，按系统提示操作即可）

## 备选：不装 Android Studio，用 GitHub Actions 云编译

如果你不想在本机安装 Android Studio / JDK，可以用 GitHub 的免费云编译，
全程网页操作，约 5 分钟拿到 APK：

1. 注册 [GitHub](https://github.com)（免费）并登录
2. 点右上角 **+ → New repository** 新建一个仓库（名字随意，如 `DeepSeekPhone`，选 Public 或 Private 均可）
3. 把本项目所有文件上传进去：
   - 网页方式：仓库页面 → **Add file → Upload files** → 把 `DeepSeekPhone` 文件夹里的内容整个拖进去（**注意**：`app/`、`gradle/` 这些子文件夹要一起传，网页拖拽时文件夹会保留）
   - 或命令行方式：在项目目录执行 `git init && git add . && git commit -m "init"` 后 `git push`
4. 仓库页面打开 **Actions** 标签 → 左侧选 **Build APK** → 点右侧 **Run workflow** 按钮
5. 等待约 3~5 分钟，绿色 ✅ 后点进这次运行 → 底部 **Artifacts** → 下载 `app-debug-apk`
6. 解压得到 `app-debug.apk`，用下面的传输方式发到手机安装即可

> 以后每次修改代码推送到 GitHub，都可以再点一次 Run workflow 重新编译。
> 项目里已带好了 `.github/workflows/build-apk.yml`，无需任何配置。

## 使用说明

- 首次打开会要求粘贴 API Key，保存后即进入聊天
- 右上角 **设置**：切换 V3 / R1 模型、更换 Key
- **清空**：重置对话历史（开始新会话）
- 发送中不可再次输入，等待回复流式出现即可

## 常见问题

| 现象 | 原因与处理 |
|------|-----------|
| 提示「HTTP 401」 | Key 错误或已失效 → 设置里更换 Key |
| 提示「HTTP 402」 | 账户余额不足 → 开放平台充值 |
| 提示「HTTP 429」 | 请求过于频繁或额度受限 → 稍等再试 |
| 一直转圈不出字 | 网络问题或 Key 未生效 → 检查网络、确认 Key 以 `sk-` 开头 |
| 回复中途断了 | 偶发网络抖动，重发一条「继续」即可续上上下文 |

## 技术说明

- 接口：`POST https://api.deepseek.com/chat/completions`（OpenAI 兼容格式）
- 流式：`"stream": true`，SSE 逐块返回 `delta.content`
- 网络：OkHttp + okhttp-sse；序列化：kotlinx.serialization
- Key 存储：SharedPreferences（应用私有目录）。如需硬件级加密，可升级
  [EncryptedSharedPreferences](https://developer.android.com/privacy-and-security/security-crypto)
- 最低支持 Android 8.0（API 26）

## 后续可扩展（欢迎按需提需求）

- 语音输入（SpeechRecognizer）
- 深色模式（Compose 自带 `isSystemInDarkTheme` 支持，加两行即可）
- 会话历史持久化（Room / DataStore）
- 联网搜索 / 工具调用（API 支持 `tools` 参数）
- 对话导出 / 分享
