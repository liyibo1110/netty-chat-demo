package com.github.liyibo1110.netty.chat.demo.protocol;

/**
 * @author liyibo
 */
public enum IMProtocol {

    SYSTEM("SYSTEM"),
    LOGIN("LOGIN"),
    LOGOUT("LOGOUT"),
    CHAT("CHAT"),
    FLOWER("FLOWER");

    private String name;

    IMProtocol(String name) {
        this.name = name;
    }

    /**
     * 检查内容是否为可识别的IM协议
     * @param content
     * @return
     */
    public static boolean isIMProtocol(String content) {
        return content.matches("^\\[(SYSTEM|LOGIN|LOGOUT|CHAT|FLOWER)\\]");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
