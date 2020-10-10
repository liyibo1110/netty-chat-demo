package com.github.liyibo1110.netty.chat.demo.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.commons.lang3.StringUtils;
import org.msgpack.MessagePack;

/**
 * @author liyibo
 */
public class IMEncoder extends MessageToByteEncoder<IMMessage> {

    protected void encode(ChannelHandlerContext ctx, IMMessage msg, ByteBuf out) throws Exception {
        out.writeBytes(new MessagePack().write(msg));
    }

    public String encode(IMMessage message) {
        if(message == null) return "";
        String content = "[" + message.getCmd() + "]" + "[" + message.getTime() + "]";
        if(IMProtocol.LOGIN.getName().equals(message.getCmd())) {
            content += ("[" + message.getSender() + "][" + message.getTerminal() + "]");
        }else if(IMProtocol.FLOWER.getName().equals(message.getCmd())) {
            content += ("[" + message.getSender() + "][" + message.getTerminal() + "][" + message.getReceiver() + "]");
        }else if(IMProtocol.CHAT.getName().equals(message.getCmd())) {
            content += ("[" + message.getSender() + "][" + message.getReceiver() + "]");
        }else if(IMProtocol.SYSTEM.getName().equals(message.getCmd())) {
            content += ("[" + message.getReceiver() + "]");
        }
        // 额外判断是否存在消息体
        if(StringUtils.isNotBlank(message.getContent())) {
            content += (" - " + message.getContent());
        }
        return content;
    }
}
