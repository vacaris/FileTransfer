package server;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileServer {
    private static final int PORT = 5000;
    private static final Logger LOGGER = Logger.getLogger(FileServer.class.getName());
    private final File sharedDirectory;
    private final DefaultListModel<String> fileListModel;
    private final Map<String, String> users = new HashMap<>();
    private final byte[] encryptionKey = "1234567890123456".getBytes();

    public FileServer() {
        sharedDirectory = new File("shared");
        if (!sharedDirectory.exists()) sharedDirectory.mkdir();

        loadUsers();

        JFrame frame = new JFrame("File Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);

        fileListModel = new DefaultListModel<>();
        JList<String> fileList = new JList<>(fileListModel);
        refreshFileList();

        JButton refreshButton = new JButton("Odśwież listę plików");
        refreshButton.addActionListener(e -> refreshFileList());

        frame.add(new JScrollPane(fileList), BorderLayout.CENTER);
        frame.add(refreshButton, BorderLayout.SOUTH);
        frame.setVisible(true);

        startServer();
    }

    private void loadUsers() {
        File userFile = new File("users.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(userFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Nie można wczytać pliku users.txt", e);
        }
    }

    private void refreshFileList() {
        fileListModel.clear();
        File[] files = sharedDirectory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.getName().equals("users.txt")) {
                    fileListModel.addElement(f.getName());
                }
            }
        }
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Błąd serwera", e);
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            String username = dis.readUTF();
            String password = dis.readUTF();

            if (!users.containsKey(username) || !users.get(username).equals(password)) {
                dos.writeUTF("ERROR");
                return;
            }
            dos.writeUTF("OK");

            while (true) {
                String command = dis.readUTF();
                if (command.equals("LIST")) {
                    File[] files = sharedDirectory.listFiles();
                    dos.writeInt(files.length);
                    for (File f : files) {
                        dos.writeUTF(f.getName());
                    }
                } else if (command.startsWith("DOWNLOAD ")) {
                    String fileName = command.substring(9);
                    File file = new File(sharedDirectory, fileName);
                    if (!file.exists()) {
                        dos.writeUTF("ERROR");
                        continue;
                    }
                    dos.writeUTF("OK");
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            byte[] encrypted = encryptData(buffer, bytesRead);
                            dos.write(encrypted, 0, encrypted.length);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Połączenie z klientem zakończone", e);
        }
    }

    private byte[] encryptData(byte[] data, int length) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"));
            return cipher.doFinal(data, 0, length);
        } catch (Exception e) {
            throw new IOException("Błąd szyfrowania danych", e);
        }
    }

    public static void main(String[] args) {
        new FileServer();
    }
}
