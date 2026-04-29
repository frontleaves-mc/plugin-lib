package com.frontleaves.plugins.lib.grpc;

import com.frontleaves.plugins.lib.config.GrpcConfig;
import com.frontleaves.plugins.lib.message.Message;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 带退避策略的 gRPC 调用执行器。
 * <p>
 * 对可恢复错误（{@link #isRecoverable(Status.Code)}）执行指数退避重试，
 * 超过阈值后降低日志级别以避免日志洪泛。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public class ResilientCallExecutor {

    private final JavaPlugin plugin;
    private final GrpcConfig config;
    private final String tag;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private long lastFailureTimeNanos = 0;
    private long currentBackoffSecs;

    public ResilientCallExecutor(@NotNull JavaPlugin plugin, @NotNull GrpcConfig config, @NotNull String tag) {
        this.plugin = plugin;
        this.config = config;
        this.tag = tag;
        this.currentBackoffSecs = config.retryInitialIntervalSecs();
    }

    public void execute(@NotNull String rpcName, @NotNull Runnable action) {
        executeWithResult(rpcName, () -> {
            action.run();
            return null;
        });
    }

    public <T> T executeWithResult(@NotNull String rpcName, @NotNull Supplier<T> action) {
        if (!config.retryEnabled()) {
            return executeOnce(rpcName, action);
        }

        StatusRuntimeException lastException = null;
        for (int attempt = 1; attempt <= config.retryMaxAttempts(); attempt++) {
            try {
                T result = action.get();
                onCallSucceeded();
                return result;
            } catch (StatusRuntimeException e) {
                lastException = e;
                if (!isRecoverable(e.getStatus().getCode())) {
                    logFailure(rpcName, e, true);
                    return null;
                }
                if (attempt < config.retryMaxAttempts()) {
                    sleepForBackoff();
                }
            }
        }
        if (lastException != null) {
            logFailure(rpcName, lastException, false);
        }
        return null;
    }

    private <T> T executeOnce(@NotNull String rpcName, @NotNull Supplier<T> action) {
        try {
            T result = action.get();
            onCallSucceeded();
            return result;
        } catch (StatusRuntimeException e) {
            logFailure(rpcName, e, !isRecoverable(e.getStatus().getCode()));
            return null;
        }
    }

    public boolean isInBackoff() {
        if (consecutiveFailures.get() == 0) {
            return false;
        }
        long elapsedNanos = System.nanoTime() - lastFailureTimeNanos;
        return elapsedNanos < TimeUnit.SECONDS.toNanos(currentBackoffSecs);
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public void reset() {
        consecutiveFailures.set(0);
        currentBackoffSecs = config.retryInitialIntervalSecs();
    }

    private void onCallSucceeded() {
        int prev = consecutiveFailures.getAndSet(0);
        if (prev > 0) {
            Message.of(plugin).console().info("[" + tag + "] gRPC 调用恢复正常（此前连续失败 " + prev + " 次）");
        }
        currentBackoffSecs = config.retryInitialIntervalSecs();
    }

    private void logFailure(@NotNull String rpcName, @NotNull StatusRuntimeException e, boolean permanent) {
        int failures = consecutiveFailures.incrementAndGet();
        lastFailureTimeNanos = System.nanoTime();

        String message = "RPC " + rpcName + " 调用失败: "
                + Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName());

        if (permanent) {
            Message.of(plugin).console().warning("[" + tag + "] " + message + " (不可恢复: " + e.getStatus().getCode() + ")");
        } else if (failures > config.retrySuppressAfter()) {
            Message.of(plugin).console().info("[" + tag + "] " + message + " (第 " + failures + " 次连续失败，退避 " + currentBackoffSecs + "s)");
        } else {
            Message.of(plugin).console().warning("[" + tag + "] " + message);
        }
    }

    private void sleepForBackoff() {
        try {
            TimeUnit.SECONDS.sleep((long) Math.ceil(currentBackoffSecs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        currentBackoffSecs = Math.min(
                (long) Math.ceil(currentBackoffSecs * config.retryMultiplier()),
                config.retryMaxIntervalSecs()
        );
    }

    /**
     * 判断 gRPC 状态码是否属于可恢复错误。
     * <p>
     * 可恢复错误通常由网络瞬态问题导致，适合重试：
     * <ul>
     *   <li>{@code UNAVAILABLE} - 服务不可达</li>
     *   <li>{@code DEADLINE_EXCEEDED} - 调用超时</li>
     *   <li>{@code RESOURCE_EXHAUSTED} - 资源临时耗尽</li>
     *   <li>{@code ABORTED} - 操作被中止（并发冲突等）</li>
     *   <li>{@code INTERNAL} - HTTP/2 协议异常（连接失效后重连可恢复）</li>
     * </ul>
     */
    static boolean isRecoverable(@NotNull Status.Code code) {
        return code == Status.Code.UNAVAILABLE
                || code == Status.Code.DEADLINE_EXCEEDED
                || code == Status.Code.RESOURCE_EXHAUSTED
                || code == Status.Code.ABORTED
                || code == Status.Code.INTERNAL;
    }
}
