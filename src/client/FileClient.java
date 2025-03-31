package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.prefs.Preferences;
import client.FileDownloadUI;

public class FileClient {
    private static final int PORT = 5000;
    private final JTextField hostField, usernameField;
    private final JPasswordField passwordField;
    private final JCheckBox rememberMeCheckbox;
    private final Preferences prefs;

    // ⬅️ DODAJ TO!
    private final byte[] encryptionKey = "1234567890123456".getBytes(); // Ten sam klucz AES co w serwerze

    public FileClient() {
        prefs = Preferences.userNodeForPackage(FileClient.class);

        JFrame frame = new JFrame("File Client - Logowanie");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Serwer:") );
        hostField = new JTextField(prefs.get("host", "127.0.0.1"));
        panel.add(hostField);

        panel.add(new JLabel("Użytkownik:") );
        usernameField = new JTextField(prefs.get("username", ""));
        panel.add(usernameField);

        panel.add(new JLabel("Hasło:") );
        passwordField = new JPasswordField();
        panel.add(passwordField);

        rememberMeCheckbox = new JCheckBox("Zapamiętaj mnie", prefs.getBoolean("rememberMe", false));
        panel.add(rememberMeCheckbox);

        JButton loginButton = new JButton("Zaloguj");
        panel.add(loginButton);

        frame.add(panel);
        frame.setVisible(true);

        loginButton.addActionListener(e -> connectToServer(hostField.getText(), usernameField.getText(), new String(passwordField.getPassword())));
    }

    private void connectToServer(String host, String username, String password) {
        try (Socket socket = new Socket(host, PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            dos.writeUTF(username);
            dos.writeUTF(password);

            String response = dis.readUTF();
            if ("OK".equals(response)) {
                JOptionPane.showMessageDialog(null, "Logowanie powiodło się!");
                new FileDownloadUI(socket, dis, dos, encryptionKey); // ⬅️ teraz kompiluje się poprawnie
            } else {
                JOptionPane.showMessageDialog(null, "Błąd logowania!");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Błąd połączenia z serwerem!");
        }
    }

    public static void main(String[] args) {
        new FileClient();
    }
}
