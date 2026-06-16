package client;

import java.io.*;
import java.net.*;
import common.*;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientGUI gui;
    private String username;
    private boolean connected;

    public ChatClient(ClientGUI gui) {
        this.gui = gui;
        this.connected = false;
    }

    public boolean connectToServer(String host, int port, String username) {
        try {
            this.username = username;
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            // 发送认证消息
            sendMessage(new Message(MessageType.USER_JOIN, username, "加入聊天"));

            // 启动消息接收线程
            new Thread(this::listenForMessages).start();
            return true;
        } catch (IOException e) {
            gui.showError("连接服务器失败: " + e.getMessage());
            return false;
        }
    }

    private void listenForMessages() {
        try {
            while (connected) {
                Message message = (Message) in.readObject();
                if (message != null) {
                    gui.displayMessage(message);

                    // 处理用户列表更新
                    if (message.getType() == MessageType.USER_LIST) {
                        gui.updateUserList(message.getContent());
                    }
                }
            }
        } catch (EOFException e) {
            // 服务器正常关闭
            gui.showError("服务器连接已关闭");
        } catch (Exception e) {
            if (connected) {
                gui.showError("连接异常: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    public void sendMessage(Message message) {
        if (connected && out != null) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                gui.showError("发送消息失败: " + e.getMessage());
                disconnect();
            }
        }
    }

    public void sendFile(File file) {
        try {
            byte[] fileData = FileTransfer.readFile(file);

            Message fileMessage = new Message(MessageType.FILE, username,
                    "发送了文件: " + file.getName());
            fileMessage.setFileData(fileData);
            fileMessage.setFileName(file.getName());

            sendMessage(fileMessage);
            gui.appendMessage("文件发送: " + file.getName());
        } catch (IOException e) {
            gui.showError("发送文件失败: " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (out != null) {
                sendMessage(new Message(MessageType.USER_LEAVE, username, "离开聊天"));
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        gui.onDisconnect();
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return username;
    }
}