package com.frontleaves.plugins.lib.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 世界消息发送器，向指定世界中的所有玩家发送消息。
 * <p>
 * 纯文本消息会自动拼接 PREFIX 并尝试进行 MiniMessage 反序列化；
 * 若反序列化失败则降级为纯文本发送。
 * 富文本消息直接发送，不拼接 PREFIX。
 * <p>
 * 所有消息发送操作均会在主线程上执行，若当前不在主线程则自动调度。
 * 世界未加载时静默忽略，不抛出异常。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public final class WorldSender implements MessageSender {

    private final JavaPlugin plugin;
    private final String prefix;
    private final String worldName;
    private final MiniMessage miniMessage;

    /**
     * 创建世界消息发送器。
     *
     * @param plugin    所属插件实例
     * @param prefix    消息前缀（MiniMessage 格式）
     * @param worldName 目标世界名称
     */
    WorldSender(@NotNull JavaPlugin plugin, @NotNull String prefix, @NotNull String worldName) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.worldName = worldName;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 向指定世界中的所有玩家发送纯文本消息。
     * <p>
     * 消息会拼接 PREFIX 后进行 MiniMessage 反序列化，
     * 若反序列化失败则降级为纯文本发送。
     * 若当前不在主线程，将自动调度到主线程执行。
     * 世界未加载时静默忽略。
     *
     * @param message 要发送的消息内容
     */
    @Override
    public void sendMessage(@NotNull String message) {
        if (Bukkit.isPrimaryThread()) {
            sendWorldMessage(message);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sendWorldMessage(message));
        }
    }

    /**
     * 向指定世界中的所有玩家发送富文本组件消息。
     * <p>
     * 直接发送组件，不拼接 PREFIX。
     * 若当前不在主线程，将自动调度到主线程执行。
     * 世界未加载时静默忽略。
     *
     * @param component 要发送的富文本组件
     */
    @Override
    public void sendComponent(@NotNull Component component) {
        if (Bukkit.isPrimaryThread()) {
            sendWorldComponent(component);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sendWorldComponent(component));
        }
    }

    /**
     * 拼接 PREFIX 并反序列化后发送给世界内所有玩家，失败则降级纯文本。
     */
    private void sendWorldMessage(@NotNull String message) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        try {
            Component deserialized = miniMessage.deserialize(prefix + message);
            world.getPlayers().forEach(player -> player.sendMessage(deserialized));
        } catch (ParsingException e) {
            String text = prefix + message;
            world.getPlayers().forEach(player -> player.sendMessage(text));
        }
    }

    /**
     * 直接发送富文本组件给世界内所有玩家。
     */
    private void sendWorldComponent(@NotNull Component component) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        world.getPlayers().forEach(player -> player.sendMessage(component));
    }
}
