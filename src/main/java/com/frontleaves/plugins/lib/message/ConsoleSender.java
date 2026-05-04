package com.frontleaves.plugins.lib.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * 控制台消息发送器，向服务器控制台发送消息。
 * <p>
 * 纯文本消息会自动拼接 PREFIX 并尝试进行 MiniMessage 反序列化；
 * 若反序列化失败则降级为纯文本发送并输出警告日志。
 * 富文本消息直接发送，不拼接 PREFIX。
 * <p>
 * 提供 {@link #info(String)}、{@link #warning(String)}、{@link #severe(String)} 日志级别方法，
 * 在控制台输出带颜色级别标识的富文本消息，同时写入 Bukkit 日志文件。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public final class ConsoleSender implements MessageSender {
    private final JavaPlugin plugin;
    private final String prefix;
    private final MiniMessage miniMessage;

    /**
     * 创建控制台消息发送器。
     *
     * @param plugin 所属插件实例
     * @param prefix  消息前缀（MiniMessage 格式）
     */
    @Contract(pure = true)
    ConsoleSender(@NotNull JavaPlugin plugin, @NotNull String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 向控制台发送 INFO 级别消息。
     * <p>
     * 输出格式：{@code [INFO] [锋楪XXX] 消息内容}，INFO 为绿色。
     * 同时写入 Bukkit 日志文件（{@code Level.INFO}）。
     *
     * @param message 要发送的消息内容
     */
    public void info(@NotNull String message) {
        plugin.getLogger().info(message);
    }

    /**
     * 向控制台发送 WARNING 级别消息。
     * <p>
     * 输出格式：{@code [WARN] [锋楪XXX] 消息内容}，WARN 为金色。
     * 同时写入 Bukkit 日志文件（{@code Level.WARNING}）。
     *
     * @param message 要发送的消息内容
     */
    public void warning(@NotNull String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * 向控制台发送 SEVERE 级别消息。
     * <p>
     * 输出格式：{@code [ERROR] [锋楪XXX] 消息内容}，ERROR 为红色。
     * 同时写入 Bukkit 日志文件（{@code Level.SEVERE}）。
     *
     * @param message 要发送的消息内容
     */
    public void severe(@NotNull String message) {
        plugin.getLogger().severe(message);
    }

    /**
     * 向控制台发送纯文本消息。
     * <p>
     * 消息会拼接 PREFIX 后进行 MiniMessage 反序列化，
     * 若反序列化失败则降级为纯文本发送并输出警告日志。
     *
     * @param message 要发送的消息内容
     */
    @Override
    public void sendMessage(@NotNull String message) {
        try {
            Bukkit.getConsoleSender().sendMessage(miniMessage.deserialize(prefix + message));
        } catch (ParsingException e) {
            Bukkit.getConsoleSender().sendMessage(prefix + message);
            plugin.getLogger().log(Level.WARNING, "MiniMessage 反序列化失败，已降级为纯文本发送: " + message, e);
        }
    }

    /**
     * 向控制台发送富文本组件消息。
     * <p>
     * 直接发送组件，不拼接 PREFIX。
     *
     * @param component 要发送的富文本组件
     */
    @Override
    public void sendComponent(@NotNull Component component) {
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
