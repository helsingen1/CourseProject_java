package server;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import common.Message;

public class ServerGUI extends JFrame {
    private ChatServer server;
    private JTextArea logArea;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;

    public ServerGUI(ChatServer server) {
        this.server = server;
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("聊天服务器控制台");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("服务器日志"));

        // 客户端列表
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(BorderFactory.createTitledBorder("在线用户"));
        clientScroll.setPreferredSize(new Dimension(150, 0));

        // 控制按钮
        JPanel controlPanel = new JPanel();
        JButton startButton = new JButton("启动服务器");
        JButton stopButton = new JButton("停止服务器");
        JButton clearButton = new JButton("清空日志");

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(clearButton);

        // 布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logScroll, clientScroll);
        splitPane.setDividerLocation(400);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 事件监听
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearButton.addActionListener(e -> logArea.setText(""));

        setVisible(true);
    }

    private void startServer() {
        new Thread(() -> server.startServer()).start();
    }

    private void stopServer() {
        appendLog("服务器停止功能待实现");
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void appendMessage(Message message) {
        appendLog(message.toString());
    }

    public void updateClientList(List<String> clients) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String client : clients) {
                clientListModel.addElement(client);
            }
        });
    }
}