package common;

public enum MessageType {
    TEXT,           // 文本消息
    FILE,           // 文件消息
    USER_JOIN,      // 用户加入
    USER_LEAVE,     // 用户离开
    USER_LIST,      // 用户列表更新
    SYSTEM          // 系统消息
}