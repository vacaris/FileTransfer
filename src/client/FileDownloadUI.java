package client;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class FileDownloadUI {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final JFrame frame;
    private final DefaultListModel<String> fileListModel;
    private final JList<String> fileList;
    private final SecretKeySpec encryptionKey;

    public FileDownloadUI(Socket socket, DataInputStream dis, DataOutputStream dos, byte[] key) {
        this.dis = dis;
        this.dos = dos;
        this.encryptionKey = new SecretKeySpec(key, "AES");

        frame = new JFrame("Pobieranie plików");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        JButton downloadButton = new JButton("Pobierz");

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(fileList), BorderLayout.CENTER);
        frame.add(downloadButton, BorderLayout.SOUTH);

        downloadButton.addActionListener(e -> downloadFile(fileList.getSelectedValue()));

        frame.setVisible(true);
        fetchFileList();
    }

    private void fetchFileList() {
        try {
            dos.writeUTF("LIST");
            int fileCount = dis.readInt();
            fileListModel.clear();
            for (int i = 0; i < fileCount; i++) {
                fileListModel.addElement(dis.readUTF());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Błąd pobierania listy plików!");
        }
    }

    private void downloadFile(String fileName) {
        if (fileName == null) {
            JOptionPane.showMessageDialog(frame, "Wybierz plik do pobrania!");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));
        int result = fileChooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File saveFile = fileChooser.getSelectedFile();

        try {
            dos.writeUTF("DOWNLOAD " + fileName);
            String response = dis.readUTF();
            if (!"OK".equals(response)) {
                JOptionPane.showMessageDialog(frame, "Błąd: Plik nie znaleziony na serwerze!");
                return;
            }

            FileOutputStream fos = new FileOutputStream(saveFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = dis.read(buffer)) != -1) {
                byte[] decryptedData = decryptData(buffer, bytesRead);
                fos.write(decryptedData, 0, decryptedData.length);
            }

            fos.close();
            JOptionPane.showMessageDialog(frame, "Plik pobrany pomyślnie!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Błąd pobierania pliku!");
        }
    }

    private byte[] decryptData(byte[] data, int length) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
            return cipher.doFinal(data, 0, length);
        } catch (Exception e) {
            throw new IOException("Błąd deszyfrowania danych", e);
        }
    }
}
