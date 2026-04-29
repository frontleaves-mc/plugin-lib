package com.frontleaves.plugins.lib.grpc;

import com.frontleaves.plugins.lib.message.Message;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * gRPC Channel 连接状态监控器。
 * <p>
 * 通过 {@link ManagedChannel#notifyWhenStateChanged} 持续追踪通道状态变化，
 * 在 {@code TRANSIENT_FAILURE} 时通知调用方暂停心跳，在 {@code READY} 时通知恢复。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public class ConnectivityMonitor {

    private final JavaPlugin plugin;
    private final ManagedChannel channel;
    private final String tag;
    private final AtomicBoolean channelReady = new AtomicBoolean(true);
    private Runnable onReadyCallback;
    private Runnable onFailureCallback;

    public ConnectivityMonitor(@NotNull JavaPlugin plugin, @NotNull ManagedChannel channel, @NotNull String tag) {
        this.plugin = plugin;
        this.channel = channel;
        this.tag = tag;
    }

    public ConnectivityMonitor onReady(@NotNull Runnable callback) {
        this.onReadyCallback = callback;
        return this;
    }

    public ConnectivityMonitor onFailure(@NotNull Runnable callback) {
        this.onFailureCallback = callback;
        return this;
    }

    public boolean isReady() {
        return channelReady.get();
    }

    public void startMonitoring() {
        watchState(channel.getState(false));
    }

    private void watchState(@NotNull ConnectivityState currentState) {
        if (channel.isShutdown()) {
            return;
        }

        channel.notifyWhenStateChanged(currentState, () -> {
            ConnectivityState newState = channel.getState(false);
            plugin.getLogger().log(Level.FINE, "[" + tag + "] Channel 状态变更: " + currentState + " -> " + newState);

            switch (newState) {
                case READY -> {
                    if (!channelReady.getAndSet(true)) {
                        Message.of(plugin).console().info("[" + tag + "] gRPC 连接已恢复 (READY)");
                        if (onReadyCallback != null) {
                            onReadyCallback.run();
                        }
                    }
                }
                case TRANSIENT_FAILURE -> {
                    if (channelReady.getAndSet(false)) {
                        Message.of(plugin).console().warning("[" + tag + "] gRPC 连接异常 (TRANSIENT_FAILURE)，暂停心跳");
                        if (onFailureCallback != null) {
                            onFailureCallback.run();
                        }
                    }
                }
                case SHUTDOWN -> {
                    channelReady.set(false);
                    return;
                }
                default -> {
                    // IDLE / CONNECTING - 等待下一个状态变化
                }
            }

            watchState(newState);
        });
    }
}
