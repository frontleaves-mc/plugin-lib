package com.frontleaves.plugins.lib.message;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 消息 API 入口，提供静态工厂方法创建 {@link MessageBuilder}。
 * <p>
 * 使用示例：
 * <pre>{@code
 * Message.of(plugin)
 *     .console()
 *     .send("服务器启动完成！");
 *
 * Message.of(plugin, "状态监控")
 *     .broadcast()
 *     .send("服务器状态已更新。");
 * }</pre>
 *
 * @author xiao_lfeng
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Message {

    private Message() {
    }

    /**
     * 使用插件名称创建消息构建器。
     *
     * @param plugin 当前的 Bukkit 插件实例
     * @return 消息构建器实例
     */
    public static @NotNull MessageBuilder of(@NotNull JavaPlugin plugin) {
        return new MessageBuilder(plugin, plugin.getName());
    }

    /**
     * 使用自定义显示名称创建消息构建器。
     *
     * @param plugin      当前的 Bukkit 插件实例
     * @param displayName 用于生成前缀的自定义显示名称
     * @return 消息构建器实例
     */
    public static @NotNull MessageBuilder of(@NotNull JavaPlugin plugin, @NotNull String displayName) {
        return new MessageBuilder(plugin, displayName);
    }
}
