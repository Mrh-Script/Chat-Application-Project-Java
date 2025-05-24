import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        System.out.println("Server started on port 1234...");

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String name;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);
                this.name = in.readLine();

                synchronized (clients) {
                    clients.add(this);
                    broadcastUserList();
                    broadcastMessage("\uD83D\uDCAC " + name + " is Online.");
                }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        break; // Allow graceful exit on /quit
                    }
                    broadcastMessage(name + ": " + message);
                }

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                synchronized (clients) {
                    clients.remove(this);
                    broadcastMessage("\u274C " + name + " is Offline.");
                    broadcastUserList();
                }
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println("MSG:" + message);
                }
            }
        }

        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder();
            for (ClientHandler client : clients) {
                userList.append(client.name).append(",");
            }
            String listMessage = "USERS:" + userList.toString();
            for (ClientHandler client : clients) {
                client.out.println(listMessage);
            }
        }
    }
}
