package client;

import javax.swing.*;
import java.awt.*;  // 添加这个导入
import java.io.*;

public class FileTransfer {

    public static byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        // 修复：正确处理read()方法的返回值
        while ((bytesRead = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }

        fis.close();
        return bos.toByteArray();
    }

    public static void saveFile(byte[] data, String fileName, Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                FileOutputStream fos = new FileOutputStream(fileChooser.getSelectedFile());
                fos.write(data);
                fos.close();
                JOptionPane.showMessageDialog(parent, "文件保存成功: " + fileName);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent,
                        "保存文件失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}