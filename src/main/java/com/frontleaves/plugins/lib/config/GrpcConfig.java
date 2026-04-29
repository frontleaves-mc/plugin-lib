package com.frontleaves.plugins.lib.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * gRPC 客户端配置，从 config.yml 加载。
 * <p>
 * 所有时间参数均以秒为单位，内部转换为 {@link java.util.concurrent.TimeUnit}。
 *
 * @param keepAliveTimeSeconds     PING 帧发送间隔（秒）
 * @param keepAliveTimeoutSeconds  PING 帧响应超时（秒）
 * @param keepAliveWithoutCalls    无活跃 RPC 时是否也发送 PING
 * @param idleTimeoutSeconds       通道空闲超时（秒），0 表示永不超时
 * @param deadlineSeconds          单次 RPC 调用默认超时（秒），0 表示不设超时
 * @param retryEnabled             是否启用失败重试
 * @param retryMaxAttempts         最大连续重试次数
 * @param retryInitialIntervalSecs 初始退避间隔（秒）
 * @param retryMaxIntervalSecs     最大退避间隔（秒）
 * @param retryMultiplier          退避乘数
 * @param retrySuppressAfter       连续失败多少次后降低日志级别
 * @author xiao_lfeng
 * @version 1.0.0
 */
public record GrpcConfig(
        int keepAliveTimeSeconds,
        int keepAliveTimeoutSeconds,
        boolean keepAliveWithoutCalls,
        int idleTimeoutSeconds,
        int deadlineSeconds,
        boolean retryEnabled,
        int retryMaxAttempts,
        int retryInitialIntervalSecs,
        int retryMaxIntervalSecs,
        double retryMultiplier,
        int retrySuppressAfter
) {

    /**
     * 从 Bukkit {@link ConfigurationSection} 解析配置。
     * 缺失的键使用默认值兜底。
     *
     * @param root 配置根节点
     * @return 解析后的配置实例
     */
    public static @NotNull GrpcConfig fromConfig(@NotNull ConfigurationSection root) {
        ConfigurationSection ka = root.getConfigurationSection("grpc.keepalive");
        ConfigurationSection call = root.getConfigurationSection("grpc.call");
        ConfigurationSection retry = root.getConfigurationSection("grpc.retry");

        return new GrpcConfig(
                ka != null ? ka.getInt("time-seconds", 30) : 30,
                ka != null ? ka.getInt("timeout-seconds", 10) : 10,
                ka != null ? ka.getBoolean("without-calls", true) : true,
                ka != null ? ka.getInt("idle-timeout-seconds", 300) : 300,
                call != null ? call.getInt("deadline-seconds", 10) : 10,
                retry != null ? retry.getBoolean("enabled", true) : true,
                retry != null ? retry.getInt("max-attempts", 3) : 3,
                retry != null ? retry.getInt("initial-interval-seconds", 5) : 5,
                retry != null ? retry.getInt("max-interval-seconds", 60) : 60,
                retry != null ? retry.getDouble("multiplier", 2.0) : 2.0,
                retry != null ? retry.getInt("suppress-after-failures", 5) : 5
        );
    }
}
