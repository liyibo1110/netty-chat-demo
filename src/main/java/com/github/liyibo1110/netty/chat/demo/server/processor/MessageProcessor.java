package com.github.liyibo1110.netty.chat.demo.server.processor;

import com.alibaba.fastjson.JSONObject;
import com.github.liyibo1110.netty.chat.demo.protocol.IMDecoder;
import com.github.liyibo1110.netty.chat.demo.protocol.IMEncoder;
import com.github.liyibo1110.netty.chat.demo.protocol.IMMessage;
import com.github.liyibo1110.netty.chat.demo.protocol.IMProtocol;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author liyibo
 */
public class MessageProcessor {

    // 记录在线用户
    private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 扩展属性
    public static final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
    public static final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
    public static final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
    public static final AttributeKey<String> FROM = AttributeKey.valueOf("from");

    private IMEncoder imEncoder = new IMEncoder();
    private IMDecoder imDecoder = new IMDecoder();

    public void process(Channel client, IMMessage message) {

        if(message == null) return;

        if(message.getCmd().equals(IMProtocol.LOGIN.getName())) {
            client.attr(NICK_NAME).getAndSet(message.getSender());
            client.attr(IP_ADDR).getAndSet(getAddress(client));
            client.attr(FROM).getAndSet(message.getTerminal());
            onlineUsers.add(client);
            // 通知其他在线用户
            for (Channel channel : onlineUsers) {
                if(client == channel) {
                    message = new IMMessage(IMProtocol.SYSTEM.getName(), System.currentTimeMillis(),
                            onlineUsers.size(), "已与服务器建立连接！");
                }else {
                    message = new IMMessage(IMProtocol.SYSTEM.getName(), System.currentTimeMillis(),
                            onlineUsers.size(), getNickName(client) + "加入");
                }
                // 如果用户为控制台登录，直接输出IMMessage对象
                if("Console".equals(client.attr(FROM).get())) {
                    channel.writeAndFlush(message);
                    continue;
                }
                // 到这里说明用户为WebSocket登录，需要序列化
                String content = imEncoder.encode(message);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }else if(message.getCmd().equals(IMProtocol.CHAT.getName())) {
            for (Channel channel : onlineUsers) {
                if (client == channel) {
                    message.setSender("you");
                } else {
                    message.setSender(getNickName(client));
                }
                message.setTime(System.currentTimeMillis());
                // 如果用户为控制台登录，直接输出IMMessage对象
                if("Console".equals(client.attr(FROM).get())) {
                    channel.writeAndFlush(message);
                    continue;
                }
                // 到这里说明用户为WebSocket登录，需要序列化
                String content = imEncoder.encode(message);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }else if(message.getCmd().equals(IMProtocol.FLOWER.getName())) {
            JSONObject attrs = getAttrs(client);
            long now = System.currentTimeMillis();
            if(attrs != null) {
                long lastTime = attrs.getLongValue("lastFlowerTime");
                // 检查60秒内是否送过花
                long subSeconds = now - lastTime;
                if(subSeconds < 60 * 1000L) {
                    message.setSender("you");
                    message.setCmd(IMProtocol.SYSTEM.getName());
                    message.setContent("您送花太频繁了，" + (60 - subSeconds / 1000L) + "秒后再试");
                    // 只有WebSocket可以送花
                    String content = imEncoder.encode(message);
                    client.writeAndFlush(new TextWebSocketFrame(content));
                }
            }
            // 开始送花
            for (Channel channel : onlineUsers) {
                if(client == channel) {
                    message.setSender("you");
                    message.setContent("你给大家送了一波鲜花雨");
                    setAttrs(client, "lastFlowerTime", now);
                }else {
                    message.setSender(getNickName(client));
                    message.setContent(getNickName(client) + "送来一波鲜花雨");
                }
                message.setTime(System.currentTimeMillis());
                // 只有WebSocket可以送花
                String content = imEncoder.encode(message);
                client.writeAndFlush(new TextWebSocketFrame(content));
            }
        }
    }

    public void process(Channel client, String message) {
        process(client, imDecoder.decode(message));
    }

    public String getNickName(Channel client) {
        return client.attr(NICK_NAME).get();
    }

    public String getAddress(Channel client) {
        return client.remoteAddress().toString().replaceFirst("/", "");
    }

    public JSONObject getAttrs(Channel client) {
        return client.attr(ATTRS).get();
    }

    private void setAttrs(Channel client, String key, Object value) {
        JSONObject obj = client.attr(ATTRS).get();
        obj.put(key, value);
        client.attr(ATTRS).set(obj);
    }
}
