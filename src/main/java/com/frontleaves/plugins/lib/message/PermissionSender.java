package com.frontleaves.plugins.lib.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 权限组消息发送器，向持有指定权限的在线玩家发送消息。
 * <p>
 * 纯文本消息会自动拼接 PREFIX 并尝试进行 MiniMessage 反序列化；
 * 若反序列化失败则降级为纯文本发送。
 * 富文本消息直接发送，不拼接 PREFIX。
 * <p>
 * 所有消息发送操作均会在主线程上执行，若当前不在主线程则自动调度。
 * 当 permission 为空或 null 时静默忽略，不视为广播。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public final class PermissionSender implements MessageSender {

    private final JavaPlugin plugin;
    private final String prefix;
    private final String permission;
    private final MiniMessage miniMessage;

    /**
     * 创建权限组消息发送器。
     *
     * @param plugin     所属插件实例
     * @param prefix     消息前缀（MiniMessage 格式）
     * @param permission 目标玩家需持有的权限节点
     */
    PermissionSender(@NotNull JavaPlugin plugin, @NotNull String prefix, @NotNull String permission) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.permission = permission;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 向持有指定权限的在线玩家发送纯文本消息。
     * <p>
     * 消息会拼接 PREFIX 后进行 MiniMessage 反序列化，
     * 若反序列化失败则降级为纯文本发送。
     * 若当前不在主线程，将自动调度到主线程执行。
     * 当 permission 为空时静默忽略。
     *
     * @param message 要发送的消息内容
     */
    @Override
    public void sendMessage(@NotNull String message) {
        if (permission.isEmpty()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            sendPermissionMessage(message);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sendPermissionMessage(message));
        }
    }

    /**
     * 向持有指定权限的在线玩家发送富文本组件消息。
     * <p>
     * 直接发送组件，不拼接 PREFIX。
     * 若当前不在主线程，将自动调度到主线程执行。
     * 当 permission 为空时静默忽略。
     *
     * @param component 要发送的富文本组件
     */
    @Override
    public void sendComponent(@NotNull Component component) {
        if (permission.isEmpty()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            sendPermissionComponent(component);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sendPermissionComponent(component));
        }
    }

    /**
     * 拼接 PREFIX 并反序列化后发送给持有权限的玩家，失败则降级纯文本。
     */
    private void sendPermissionMessage(@NotNull String message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .forEach(player -> {
                    try {
                        player.sendMessage(miniMessage.deserialize(prefix + message));
                    } catch (ParsingException e) {
                        player.sendMessage(prefix + message);
                    }
                });
    }

    /**
     * 直接发送富文本组件给持有权限的玩家。
     */
    private void sendPermissionComponent(@NotNull Component component) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .forEach(player -> player.sendMessage(component));
    }
}
