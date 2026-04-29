package com.frontleaves.plugins.lib.message;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 消息构建器，提供用于创建各种消息发送器的流畅 API。
 * <p>
 * 通过 {@link #console()}、{@link #player(UUID)}、{@link #broadcast()}、
 * {@link #permission(String)}、{@link #world(String)} 等方法获取对应的消息发送器，
 * 每个发送器都携带统一的插件前缀格式。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MessageBuilder {

    private final JavaPlugin plugin;
    private final String prefix;

    /**
     * 创建一个新的消息构建器。
     *
     * @param plugin     当前的 Bukkit 插件实例
     * @param pluginName 用于生成前缀的插件显示名称
     */
    MessageBuilder(@NotNull JavaPlugin plugin, @NotNull String pluginName) {
        this.plugin = plugin;
        this.prefix = "<dark_gray>[<green>锋楪<dark_green>" + pluginName + "<dark_gray>] <reset>";
    }

    /**
     * 获取一个控制台消息发送器。
     *
     * @return 控制台消息发送器实例
     */
    public @NotNull ConsoleSender console() {
        return new ConsoleSender(plugin, prefix);
    }

    /**
     * 获取一个指定玩家的消息发送器。
     *
     * @param playerId 目标玩家的 UUID
     * @return 玩家消息发送器实例
     */
    public @NotNull PlayerSender player(@NotNull UUID playerId) {
        return new PlayerSender(plugin, prefix, playerId);
    }

    /**
     * 获取一个广播消息发送器，向所有在线玩家发送消息。
     *
     * @return 广播消息发送器实例
     */
    public @NotNull BroadcastSender broadcast() {
        return new BroadcastSender(plugin, prefix);
    }

    /**
     * 获取一个权限消息发送器，仅向拥有指定权限的玩家发送消息。
     *
     * @param permission 目标权限节点
     * @return 权限消息发送器实例
     */
    public @NotNull PermissionSender permission(@NotNull String permission) {
        return new PermissionSender(plugin, prefix, permission);
    }

    /**
     * 获取一个世界消息发送器，向指定世界的所有玩家发送消息。
     *
     * @param worldName 目标世界名称
     * @return 世界消息发送器实例
     */
    public @NotNull WorldSender world(@NotNull String worldName) {
        return new WorldSender(plugin, prefix, worldName);
    }

    /**
     * 获取当前构建器使用的前缀字符串。
     *
     * @return MiniMessage 格式的前缀字符串
     */
    public @NotNull String getPrefix() {
        return prefix;
    }
}
