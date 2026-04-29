package com.frontleaves.plugins.lib.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * 广播消息发送器，向所有在线玩家及服务器控制台同时发送消息。
 * <p>
 * 纯文本消息会自动拼接 PREFIX 并尝试进行 MiniMessage 反序列化；
 * 若反序列化失败则降级为纯文本发送并输出警告日志。
 * 富文本消息直接发送，不拼接 PREFIX。
 * <p>
 * 所有消息发送操作均会在主线程上执行，若当前不在主线程则自动调度。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public final class BroadcastSender implements MessageSender {

    private final JavaPlugin plugin;
    private final String prefix;
    private final MiniMessage miniMessage;

    /**
     * 创建广播消息发送器。
     *
     * @param plugin 所属插件实例
     * @param prefix 消息前缀（MiniMessage 格式）
     */
    BroadcastSender(@NotNull JavaPlugin plugin, @NotNull String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 向所有在线玩家及控制台广播纯文本消息。
     * <p>
     * 消息会拼接 PREFIX 后进行 MiniMessage 反序列化，
     * 若反序列化失败则降级为纯文本发送并输出警告日志。
     * 若当前不在主线程，将自动调度到主线程执行。
     *
     * @param message 要广播的消息内容
     */
    @Override
    public void sendMessage(@NotNull String message) {
        if (Bukkit.isPrimaryThread()) {
            broadcastMessage(message);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> broadcastMessage(message));
        }
    }

    /**
     * 向所有在线玩家及控制台广播富文本组件消息。
     * <p>
     * 直接发送组件，不拼接 PREFIX。
     * 若当前不在主线程，将自动调度到主线程执行。
     *
     * @param component 要广播的富文本组件
     */
    @Override
    public void sendComponent(@NotNull Component component) {
        if (Bukkit.isPrimaryThread()) {
            broadcastComponent(component);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> broadcastComponent(component));
        }
    }

    /**
     * 拼接 PREFIX 并反序列化后广播，失败则降级纯文本。
     */
    private void broadcastMessage(@NotNull String message) {
        try {
            Component compiled = miniMessage.deserialize(prefix + message);
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(compiled));
            Bukkit.getConsoleSender().sendMessage(compiled);
        } catch (ParsingException e) {
            String plainText = prefix + message;
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(plainText));
            Bukkit.getConsoleSender().sendMessage(plainText);
            plugin.getLogger().log(Level.WARNING, "MiniMessage 反序列化失败，已降级为纯文本发送: " + message, e);
        }
    }

    /**
     * 直接广播富文本组件。
     */
    private void broadcastComponent(@NotNull Component component) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
