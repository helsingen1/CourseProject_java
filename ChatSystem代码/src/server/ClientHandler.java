package server;

import java.io.*;
import java.net.*;
import common.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private boolean connected;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
    }

    @Override
    public void run() {
        try {
            // 用户认证
            Message authMessage = (Message) in.readObject();
            this.username = authMessage.getSender();

            if (server.isUsernameTaken(username)) {
                sendMessage(new Message(MessageType.SYSTEM, "系统", "用户名已存在，连接拒绝"));
                closeConnection();
                return;
            }

            server.addClient(username, this);
            sendMessage(new Message(MessageType.SYSTEM, "系统", "欢迎 " + username + " 进入聊天室！"));
            server.appendLog("用户 " + username + " 加入聊天室");

            // 处理客户端消息
            while (connected && socket.isConnected()) {
                try {
                    Message message = (Message) in.readObject();
                    if (message != null) {
                        switch (message.getType()) {
                            case TEXT:
                            case FILE:
                                server.broadcastMessage(message);
                                break;
                            case USER_LEAVE:
                                closeConnection();
                                return;
                        }
                    }
                } catch (EOFException e) {
                    // 客户端正常断开
                    break;
                } catch (SocketException e) {
                    // 客户端异常断开
                    break;
                }
            }
        } catch (Exception e) {
            server.appendLog("客户端处理异常: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(Message message) {
        if (connected && out != null) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                server.appendLog("发送消息失败: " + e.getMessage());
                closeConnection();
            }
        }
    }

    public void closeConnection() {
        connected = false;
        try {
            if (username != null) {
                server.removeClient(username);
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}