package com.github.liyibo1110.netty.chat.demo.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

/**
 * @author liyibo
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    // 获取classPath路径
    private URL baseURL = HttpServerHandler.class.getResource("");
    private final String webroot = "webroot";

    private File getResource(String fileName) throws Exception {

        String basePath = baseURL.toURI().toString();
        int start = basePath.indexOf("classes/");
        basePath = (basePath.substring(0, start) + "/" + "classes/").replaceAll("/+", "/");

        // 找webroot目录
        String path = basePath + webroot + "/" + fileName;
        path = path.contains("file:") ? path.substring(5) : path;
        path = path.replaceAll("//", "/");
        return new File(path);
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        String uri = request.uri();
        RandomAccessFile file = null;

        String page = uri.equals("/") ? "chat.html" : uri;
        try {
            file = new RandomAccessFile(getResource(page), "r");
        } catch (Exception e) {
            ctx.fireChannelRead(request.retain());
            return;
        }

        // 构建响应
        HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
        String contentType = "text/html;";
        if(uri.endsWith(".css")) {
            contentType = "text/css;";
        }else if(uri.endsWith(".js")) {
            contentType = "text/javascript;";
        }else if(uri.toLowerCase().matches(".*\\.(jpg|png|gif)$")) {
            String ext = uri.substring(uri.lastIndexOf("."));
            contentType = "image/" + ext;
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "charset=utf-8");
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if(keepAlive) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        // 写响应头
        ctx.write(response);
        // 写响应体
        ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
        // 写结束符，异步方式
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if(!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
        file.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel client = ctx.channel();
        logger.info("Client: " + client.remoteAddress() + "异常");
        cause.printStackTrace();
        // 关闭
        client.close();
    }
}
