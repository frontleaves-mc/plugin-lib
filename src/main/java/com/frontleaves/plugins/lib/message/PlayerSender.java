package com.frontleaves.plugins.lib.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 玩家消息发送器，向指定玩家发送消息。
 * <p>
 * 纯文本消息会自动拼接 PREFIX 并尝试进行 MiniMessage 反序列化；
 * 若反序列化失败则降级为纯文本发送。
 * 富文本消息直接发送，不拼接 PREFIX。
 * <p>
 * 所有消息发送操作均会在主线程上执行，若当前不在主线程则自动调度。
 * 玩家离线时静默忽略，不抛出异常。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public final class PlayerSender implements MessageSender {

    private final JavaPlugin plugin;
    private final String prefix;
    private final UUID playerId;
    private final MiniMessage miniMessage;

    /**
     * 创建玩家消息发送器。
     *
     * @param plugin   所属插件实例
     * @param prefix   消息前缀（MiniMessage 格式）
     * @param playerId 目标玩家的 UUID
     */
    PlayerSender(@NotNull JavaPlugin plugin, @NotNull String prefix, @NotNull UUID playerId) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.playerId = playerId;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 向指定玩家发送纯文本消息。
     * <p>
     * 消息会拼接 PREFIX 后进行 MiniMessage 反序列化，
     * 若反序列化失败则降级为纯文本发送。
     * 若当前不在主线程，将自动调度到主线程执行。
     * 玩家离线时静默忽略。
     *
     * @param message 要发送的消息内容
     */
    @Override
    public void sendMessage(@NotNull String message) {
        if (Bukkit.isPrimaryThread()) {
            sendPlayerMessage(message);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sendPlayerMessage(message));
        }
    }

    /**
     * 向指定玩家发送富文本组件消息。
     * <p>
     * 直接发送组件，不拼接 PREFIX。
     * 若当前不在主线程，将自动调度到主线程执行。
     * 玩家离线时静默忽略。
     *
     * @param component 要发送的富文本组件
     */
    @Override
    public void sendComponent(@NotNull Component component) {
        if (Bukkit.isPrimaryThread()) {
            sendPlayerComponent(component);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> sendPlayerComponent(component));
        }
    }

    /**
     * 拼接 PREFIX 并反序列化后发送给玩家，失败则降级纯文本。
     */
    private void sendPlayerMessage(@NotNull String message) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        try {
            player.sendMessage(miniMessage.deserialize(prefix + message));
        } catch (ParsingException e) {
            player.sendMessage(prefix + message);
        }
    }

    /**
     * 直接发送富文本组件给玩家。
     */
    private void sendPlayerComponent(@NotNull Component component) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.sendMessage(component);
    }
}
