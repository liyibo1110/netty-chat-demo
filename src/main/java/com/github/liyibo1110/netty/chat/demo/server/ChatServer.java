package com.github.liyibo1110.netty.chat.demo.server;

import com.github.liyibo1110.netty.chat.demo.protocol.IMDecoder;
import com.github.liyibo1110.netty.chat.demo.protocol.IMEncoder;
import com.github.liyibo1110.netty.chat.demo.server.handler.HttpServerHandler;
import com.github.liyibo1110.netty.chat.demo.server.handler.TerminalServerHandler;
import com.github.liyibo1110.netty.chat.demo.server.handler.WebSocketServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liyibo
 */
public class ChatServer {

    private static Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private int port = 8080;

    public void start(int port) {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 解析自定义协议
                            pipeline.addLast(new IMDecoder());  // Inbound
                            pipeline.addLast(new IMEncoder());  // Outbound
                            // 处理直接发送IMMessage对象，针对IDE控制台
                            pipeline.addLast(new TerminalServerHandler());  // Inbound

                            // 解析http类型请求
                            pipeline.addLast(new HttpServerCodec());    // Outbound
                            // 将同一个http请求的多个消息变成一个fullHttpRequest的完整请求对象
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));  // Inbound
                            // 处理大数据流，避免数据过大造成OOM
                            pipeline.addLast(new ChunkedWriteHandler()); // Inbound、Outbound
                            // 处理并解析来自Web页面的请求
                            pipeline.addLast(new HttpServerHandler());  // Inbound

                            // 解析WebSocket的请求
                            pipeline.addLast(new WebSocketServerProtocolHandler("/im"));    // Inbound
                            // 处理并解析来自WebSocket的请求
                            pipeline.addLast(new WebSocketServerHandler()); // Inbound
                        }
                    });
            ChannelFuture future = server.bind(port).sync();
            logger.info("服务已启动， 监听自端口：" + port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void start() {
        start(port);
    }

    public static void main(String[] args) {
        if(args.length > 0) {
            new ChatServer().start(Integer.parseInt(args[0]));
        }else {
            new ChatServer().start();
        }
    }
}
