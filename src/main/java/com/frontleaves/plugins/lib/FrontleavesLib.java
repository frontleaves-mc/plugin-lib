package com.frontleaves.plugins.lib;

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

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Frontleaves 通用库已加载，gRPC 客户端基础设施就绪");
    }

    @Override
    public void onDisable() {
        for (ManagedChannel channel : activeChannels) {
            if (!channel.isShutdown()) {
                channel.shutdownNow();
                try {
                    channel.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                getLogger().warning("发现未关闭的 gRPC 通道，已自动清理");
            }
        }
        activeChannels.clear();
        instance = null;
        getLogger().info("Frontleaves 通用库已卸载");
    }

    /**
     * 获取库插件实例。
     */
    public static Optional<FrontleavesLib> getInstance() {
        return Optional.ofNullable(instance);
    }

    /**
     * 创建一个配置好的 gRPC ManagedChannel，自动追踪生命周期。
     * 插件卸载时未关闭的通道会被自动清理。
     *
     * @param host Go 后端地址
     * @param port Go 后端端口
     * @param pluginName 插件名称
     * @param secretKey 插件密钥
     * @return 已配置的 ManagedChannel
     */
    public @NotNull ManagedChannel createChannel(@NotNull String host, int port, @NotNull String pluginName, @NotNull String secretKey) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .intercept(new com.frontleaves.plugins.lib.grpc.ClientAuthInterceptor(pluginName, secretKey))
                .usePlaintext()
                .build();
        activeChannels.add(channel);
        return channel;
    }
}
