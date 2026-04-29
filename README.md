# Frontleaves-Lib

Frontleaves MC 插件共享库——为所有 Frontleaves 插件提供 gRPC 客户端基础设施（通道管理 + 认证拦截器）。

## 功能

- **gRPC 通道管理**：创建 `ManagedChannel` 并自动追踪生命周期，插件卸载时自动清理未关闭的通道
- **认证拦截器**：通过 `ClientAuthInterceptor` 在每次 gRPC 调用的 metadata 中注入 `plugin-name` 和 `plugin-secret-key`
- **单例访问**：`FrontleavesLib.getInstance()` 供依赖插件获取库实例

## 架构

```
┌───────────────────────────────────────────────────────────┐
│                  Minecraft Server (Paper)                  │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              frontleaves-lib                         │  │
│  │                                                     │  │
│  │  FrontleavesLib                                     │  │
│  │  ├── createChannel(host, port, pluginName, secret)  │  │
│  │  │       └── ClientAuthInterceptor                  │  │
│  │  │             ├── plugin-name (metadata)           │  │
│  │  │             └── plugin-secret-key (metadata)     │  │
│  │  └── 通道生命周期追踪 + 自动清理                     │  │
│  └────────────────────┬────────────────────────────────┘  │
│                       │ ManagedChannel (Plaintext)         │
│         ┌─────────────┼─────────────┐                     │
│         │             │             │                     │
│  ┌──────▼─────┐ ┌─────▼──────┐ ┌───▼──────────┐         │
│  │server-status│ │ plugin-b   │ │ plugin-c     │ ...     │
│  └────────────┘ └────────────┘ └──────────────┘         │
└───────────────────────────────────────────────────────────┘
                        │
                        ▼ gRPC
┌───────────────────────────────────────────────────────────┐
│             frontleaves-plugin (Go 后端)                    │
└───────────────────────────────────────────────────────────┘
```

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| 平台 | Paper API 1.21.1 |
| 构建 | Maven + Shade Plugin |
| gRPC | 1.62.2 |
| Protobuf | 3.25.3 |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+

### 构建

```bash
mvn clean install
```

构建产物安装到本地 Maven 仓库，供其他插件通过 `<dependency>` 引用。

### 作为依赖使用

在业务插件的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.frontleaves.plugins</groupId>
    <artifactId>frontleaves-lib</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

在业务插件的 `paper-plugin.yml` 中声明依赖：

```yaml
dependencies:
  server:
    FrontleavesLib:
      required: true
      load: BEFORE
```

### 创建 gRPC 通道

```java
FrontleavesLib lib = FrontleavesLib.getInstance().orElseThrow();
ManagedChannel channel = lib.createChannel("localhost", 50051, "server-status", "your-secret-key");
```

## 目录结构

```
.
├── pom.xml                                           # Maven 构建（含 shade 插件）
└── src/main/
    ├── java/.../lib/
    │   ├── FrontleavesLib.java                        # 主类：通道管理 + 单例
    │   └── grpc/
    │       └── ClientAuthInterceptor.java             # gRPC 认证拦截器
    └── resources/
        └── paper-plugin.yml                           # Paper 插件描述（STARTUP 加载）
```

## 相关项目

| 项目 | 说明 |
|------|------|
| [server-status](../server-status) | 服务器状态监控插件（Java/gRPC） |
| [frontleaves-plugin](../../frontleaves-plugin) | Go 后端服务（gRPC Server + RESTful API） |

## 许可证

[MIT License](LICENSE)
