package com.frontleaves.plugins.lib.message;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 消息发送契约，定义向不同目标发送消息的统一接口。
 * <p>
 * 所有消息发送器均实现此接口，支持纯文本和富文本两种发送方式。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public sealed interface MessageSender permits ConsoleSender, PlayerSender, BroadcastSender, PermissionSender, WorldSender {

    /**
     * 发送纯文本消息。
     *
     * @param message 要发送的消息内容
     */
    void sendMessage(@NotNull String message);

    /**
     * 发送富文本组件消息。
     *
     * @param component 要发送的富文本组件
     */
    void sendComponent(@NotNull Component component);
}
