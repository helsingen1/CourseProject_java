package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import common.*;

public class ClientGUI extends JFrame {
    private ChatClient client;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, fileButton, disconnectButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel statusLabel;

    public ClientGUI() {
        initializeLoginDialog();
    }

    private void initializeLoginDialog() {
        JTextField usernameField = new JTextField(15);
        JTextField serverField = new JTextField("localhost", 15);
        JTextField portField = new JTextField("8888", 10);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("用户名:"));
        panel.add(usernameField);
        panel.add(new JLabel("服务器:"));
        panel.add(serverField);
        panel.add(new JLabel("端口:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel, "登录聊天室",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String server = serverField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "端口号必须是数字", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!username.isEmpty()) {
                initializeGUI();
                client = new ChatClient(this);
                if (!client.connectToServer(server, port, username)) {
                    System.exit(1);
                }
            } else {
                JOptionPane.showMessageDialog(null, "用户名不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            System.exit(0);
        }
    }

    private void initializeGUI() {
        setTitle("Java聊天室");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // 状态栏
        statusLabel = new JLabel(" 连接中...");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());

        // 聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("聊天内容"));

        // 用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("在线用户"));
        userScroll.setPreferredSize(new Dimension(150, 0));

        // 输入面板
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setFont(new Font("微软雅黑", Font.PLAIN, 13));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        sendButton = new JButton("发送");
        fileButton = new JButton("发送文件");
        disconnectButton = new JButton("断开连接");

        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(disconnectButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // 主布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScroll, userScroll);
        splitPane.setDividerLocation(600);

        add(splitPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        // 事件监听
        sendButton.addActionListener(e -> sendTextMessage());
        fileButton.addActionListener(e -> sendFile());
        disconnectButton.addActionListener(e -> disconnect());
        messageField.addActionListener(e -> sendTextMessage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });

        setVisible(true);
        messageField.requestFocus();
        updateStatus(true);
    }

    private void sendTextMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty() && client.isConnected()) {
            Message message = new Message(MessageType.TEXT, client.getUsername(), text);
            client.sendMessage(message);
            messageField.setText("");
        }
    }

    private void sendFile() {
        if (!client.isConnected()) {
            showError("未连接到服务器");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.length() > 10 * 1024 * 1024) { // 10MB限制
                showError("文件大小不能超过10MB");
                return;
            }
            client.sendFile(file);
        }
    }

    private void disconnect() {
        if (client != null) {
            client.disconnect();
        }
    }

    public void displayMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message.toString() + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());

            // 处理文件消息
            if (message.getType() == MessageType.FILE && message.getFileData() != null) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "收到文件: " + message.getFileName() + "，是否保存？",
                        "文件接收", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    FileTransfer.saveFile(message.getFileData(), message.getFileName(), this);
                }
            }
        });
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void updateUserList(String userListStr) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (userListStr != null && !userListStr.isEmpty()) {
                String[] users = userListStr.split(",");
                for (String user : users) {
                    if (!user.isEmpty()) {
                        userListModel.addElement(user);
                    }
                }
            }
        });
    }

    public void updateStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                statusLabel.setText(" 已连接 - 用户: " + client.getUsername());
                statusLabel.setForeground(Color.GREEN.darker());
            } else {
                statusLabel.setText(" 未连接");
                statusLabel.setForeground(Color.RED);
            }
        });
    }

    public void onDisconnect() {
        updateStatus(false);
        showError("与服务器的连接已断开");
    }

    public void showError(String error) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, error, "错误", JOptionPane.ERROR_MESSAGE));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI());
    }
}