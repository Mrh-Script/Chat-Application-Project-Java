import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Client2 extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton leaveButton;
    private PrintWriter out;
    private Socket socket;
    private BufferedReader in;
    private String name;
    private DefaultListModel<String> userListModel = new DefaultListModel<>();

    public Client2(String name) {
        this.name = name;
        setTitle("ID - " + name);
        setSize(550, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Font cambriaFont = new Font("Cambria", Font.PLAIN, 14);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(0xF1FAEE));
        chatArea.setFont(cambriaFont);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setBackground(Color.WHITE);
        inputField.setFont(cambriaFont);

        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(0x4F8FC0));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setFont(cambriaFont);

        leaveButton = new JButton("Leave");
        leaveButton.setBackground(new Color(0xD9534F));
        leaveButton.setForeground(Color.WHITE);
        leaveButton.setFocusPainted(false);
        leaveButton.setFont(cambriaFont);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(leaveButton);

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel leftPanel = new JPanel(new BorderLayout());
        JList<String> userList = new JList<>(userListModel);
        userList.setFont(cambriaFont);
        userList.setBackground(new Color(0x8DD7BF));
        userList.setForeground(new Color(0x540B0E));
        leftPanel.add(new JLabel("Online Users:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(120, 0));

        add(leftPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        inputField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
        leaveButton.addActionListener(e -> leaveChat());

        setVisible(true);
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send name first
            out.println(name);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        if (msg.startsWith("MSG:")) {
                            chatArea.append(msg.substring(4) + "\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        } else if (msg.startsWith("USERS:")) {
                            updateUserList(msg.substring(6));
                        } else if (msg.equals("CLEAR_CHAT")) {
                            clearChatArea();
                        }
                    }
                } catch (IOException e) {
                    chatArea.append("Disconnected.\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to server.");
            System.exit(1);
        }
    }

    private void updateUserList(String data) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : data.split(",")) {
                if (!user.isEmpty()) {
                    userListModel.addElement(user);
                }
            }
        });
    }

    private void clearChatArea() {
        SwingUtilities.invokeLater(() -> chatArea.setText(""));
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void leaveChat() {
        try {
            if (out != null) {
                out.println("/quit");  // You can implement this command on server to close connection if desired
                out.close();
            }
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String name = JOptionPane.showInputDialog("Enter your name :");
            if (name != null && !name.isEmpty()) {
                new Client2(name);
            }
        });
    }
}
