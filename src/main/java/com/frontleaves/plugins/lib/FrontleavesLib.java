package com.frontleaves.plugins.lib;

import com.frontleaves.plugins.lib.config.GrpcConfig;
import com.frontleaves.plugins.lib.grpc.ClientAuthInterceptor;
import com.frontleaves.plugins.lib.grpc.ConnectivityMonitor;
import com.frontleaves.plugins.lib.grpc.ResilientCallExecutor;
import com.frontleaves.plugins.lib.message.Message;
import com.frontleaves.plugins.lib.message.MessageBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Frontleaves 通用库主类，为插件提供 gRPC 客户端基础设施。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public final class FrontleavesLib extends JavaPlugin {

    private static FrontleavesLib instance;
    private final List<ManagedChannel> activeChannels = new ArrayList<>();
    private GrpcConfig grpcConfig;

    public static Optional<FrontleavesLib> getInstance() {
        return Optional.ofNullable(instance);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        grpcConfig = GrpcConfig.fromConfig(getConfig());
        instance = this;
        Message.of(this, "前置").console().info("Frontleaves 通用库已加载，gRPC 客户端基础设施就绪");
    }

    @Override
    public void onDisable() {
        for (ManagedChannel channel : activeChannels) {
            if (!channel.isShutdown()) {
                // 优雅关闭：让 Netty EventLoop 线程自行完成清理，
                // 避免 shutdownNow() 中断线程后线程仍需加载类但 ClassLoader 已销毁
                channel.shutdown();
                try {
                    if (!channel.awaitTermination(3, TimeUnit.SECONDS)) {
                        // 优雅关闭超时，强制终止残留线程
                        channel.shutdownNow();
                        channel.awaitTermination(2, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    channel.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                Message.of(this, "前置").console().warning("发现未关闭的 gRPC 通道，已自动清理");
            }
        }
        activeChannels.clear();
        instance = null;
        Message.of(this, "前置").console().info("Frontleaves 通用库已卸载");
    }

    public @NotNull GrpcConfig getGrpcConfig() {
        return grpcConfig;
    }

    /**
     * 重新加载 config.yml 并刷新 gRPC 配置。
     * 已创建的通道不会自动重建，需调用方重新创建。
     */
    public void reloadGrpcConfig() {
        reloadConfig();
        grpcConfig = GrpcConfig.fromConfig(getConfig());
        Message.of(this, "前置").console().info("gRPC 配置已重新加载");
    }

    /**
     * 创建一个配置好的 gRPC ManagedChannel，自动追踪生命周期。
     * <p>
     * 已内置 HTTP/2 keepalive、空闲超时等连接保活配置，
     * 可通过 config.yml 动态调整参数。
     *
     * @param host       Go 后端地址
     * @param port       Go 后端端口
     * @param pluginName 插件名称
     * @param secretKey  插件密钥
     * @return 已配置的 ManagedChannel
     */
    public @NotNull ManagedChannel createChannel(
            @NotNull String host, int port,
            @NotNull String pluginName,
            @NotNull String secretKey
    ) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(host, port)
                .intercept(new ClientAuthInterceptor(pluginName, secretKey))
                .usePlaintext()
                .keepAliveTime(grpcConfig.keepAliveTimeSeconds(), TimeUnit.SECONDS)
                .keepAliveTimeout(grpcConfig.keepAliveTimeoutSeconds(), TimeUnit.SECONDS)
                .keepAliveWithoutCalls(grpcConfig.keepAliveWithoutCalls());

        if (grpcConfig.idleTimeoutSeconds() > 0) {
            builder.idleTimeout(grpcConfig.idleTimeoutSeconds(), TimeUnit.SECONDS);
        }

        ManagedChannel channel = builder.build();
        activeChannels.add(channel);
        return channel;
    }

    /**
     * 为指定插件创建带退避策略的 gRPC 调用执行器。
     *
     * @param plugin 业务插件实例
     * @param tag    日志标识（通常为插件名）
     * @return 配置好的 {@link ResilientCallExecutor}
     */
    public @NotNull ResilientCallExecutor createCallExecutor(@NotNull JavaPlugin plugin, @NotNull String tag) {
        return new ResilientCallExecutor(plugin, grpcConfig, tag);
    }

    /**
     * 为指定通道创建连接状态监控器。
     *
     * @param plugin  业务插件实例
     * @param channel 已创建的 ManagedChannel
     * @param tag     日志标识（通常为插件名）
     * @return 配置好的 {@link ConnectivityMonitor}（需调用 {@code startMonitoring()} 启动）
     */
    public @NotNull ConnectivityMonitor createConnectivityMonitor(
            @NotNull JavaPlugin plugin,
            @NotNull ManagedChannel channel,
            @NotNull String tag
    ) {
        return new ConnectivityMonitor(plugin, channel, tag);
    }

    /**
     * 为指定插件创建消息构建器，用于发送 MiniMessage 格式化消息。
     * <p>
     * 等价于 {@code Message.of(plugin)}，提供与 gRPC 工厂方法一致的调用风格。
     *
     * @param plugin 业务插件实例
     * @return 配置好的 {@link MessageBuilder}
     */
    public @NotNull MessageBuilder createMessageBuilder(@NotNull JavaPlugin plugin) {
        return Message.of(plugin);
    }
}
