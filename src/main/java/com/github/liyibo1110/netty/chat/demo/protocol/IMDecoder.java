package com.github.liyibo1110.netty.chat.demo.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.commons.lang3.StringUtils;
import org.msgpack.MessagePack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liyibo
 */
public class IMDecoder extends ByteToMessageDecoder {

    private Pattern pattern = Pattern.compile("^\\[(.*)\\](\\s\\-\\s(.*))?");

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int length = in.readableBytes();
        byte[] data = new byte[length];
        String content = new String(data, in.readerIndex(), length);
        if(StringUtils.isNotBlank(content)) {
            if(!IMProtocol.isIMProtocol(content)) {
                ctx.channel().pipeline().remove(this);
                return;
            }
        }
        in.getBytes(in.readerIndex(), data, 0, length);
        out.add(new MessagePack().read(data, IMMessage.class));
        in.clear();
    }

    public IMMessage decode(String message) {

        if(StringUtils.isBlank(message)) return null;
        Matcher matcher = pattern.matcher(message);
        String header = null;
        String content = null;
        if(matcher.matches()) {
            header = matcher.group(1);
            content = matcher.group(3);
        }
        // LOGIN][1234567890123][张三][WebSocket
        String[] headers = header.split("\\]\\[");
        long time = Long.parseLong(headers[1]);
        String nickName = headers[2];
        nickName = nickName.length() >= 10 ? nickName.substring(0, 9) : nickName;

        if(message.startsWith("[" + IMProtocol.LOGIN.getName() + "]")) {
            return new IMMessage(headers[0], headers[3], time, nickName);
        }else if(message.startsWith("[" + IMProtocol.CHAT.getName() + "]")) {
            return new IMMessage(headers[0], time, nickName, content);
        }else if(message.startsWith("[" + IMProtocol.FLOWER.getName() + "]")) {
            return new IMMessage(headers[0], headers[3], time, nickName);
        }else {
            return null;
        }
    }
}
