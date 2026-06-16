package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import common.*;

public class ChatServer {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<String, ClientHandler> clients;
    private ServerGUI gui;
    private boolean isRunning;

    public ChatServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.clients = new ConcurrentHashMap<>();
        this.gui = new ServerGUI(this);
        this.isRunning = false;
    }

    public void startServer() {
        if (isRunning) {
            gui.appendLog("服务器已经在运行中");
            return;
        }

        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            gui.appendLog("服务器启动成功，监听端口: " + PORT);
            gui.appendLog("等待客户端连接...");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    threadPool.execute(clientHandler);
                    gui.appendLog("新的客户端连接: " + clientSocket.getInetAddress());
                } catch (SocketException e) {
                    if (isRunning) {
                        gui.appendLog("服务器socket异常: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            gui.appendLog("服务器启动失败: " + e.getMessage());
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // 通知所有客户端服务器关闭
            broadcastMessage(new Message(MessageType.SYSTEM, "系统", "服务器已关闭"));

            // 关闭所有客户端连接
            for (ClientHandler client : clients.values()) {
                client.closeConnection();
            }
            clients.clear();

            threadPool.shutdown();
            gui.appendLog("服务器已停止");
        } catch (IOException e) {
            gui.appendLog("停止服务器时出错: " + e.getMessage());
        }
    }

    public void broadcastMessage(Message message) {
        gui.appendMessage(message);
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }

    public void addClient(String username, ClientHandler client) {
        clients.put(username, client);
        updateUserList();
        broadcastMessage(new Message(MessageType.USER_JOIN, "系统",
                username + " 加入了聊天室"));
    }

    public void removeClient(String username) {
        clients.remove(username);
        updateUserList();
        broadcastMessage(new Message(MessageType.USER_LEAVE, "系统",
                username + " 离开了聊天室"));
    }

    private void updateUserList() {
        List<String> userList = new ArrayList<>(clients.keySet());
        gui.updateClientList(userList);

        // 通知所有客户端更新用户列表
        Message userListMessage = new Message(MessageType.USER_LIST, "系统",
                String.join(",", userList));
        for (ClientHandler client : clients.values()) {
            client.sendMessage(userListMessage);
        }
    }

    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    public void appendLog(String message) {
        gui.appendLog(message);
    }

    public static void main(String[] args) {
        new ChatServer();
    }
}