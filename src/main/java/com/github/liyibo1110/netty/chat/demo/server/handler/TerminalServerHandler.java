package com.github.liyibo1110.netty.chat.demo.server.handler;

import com.github.liyibo1110.netty.chat.demo.protocol.IMMessage;
import com.github.liyibo1110.netty.chat.demo.server.processor.MessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author liyibo
 */
public class TerminalServerHandler extends SimpleChannelInboundHandler<IMMessage> {

    private MessageProcessor processor = new MessageProcessor();

    protected void channelRead0(ChannelHandlerContext ctx, IMMessage msg) throws Exception {
        processor.process(ctx.channel(), msg);
    }
}
