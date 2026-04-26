package missive.server;

import missive.server.db.Database;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class ChatServer {

    private final int port;
    private final RoomManager roomManager = new RoomManager();
    private boolean running = false;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        running = true;
        System.out.println("[server] starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("[server] ready");
            while (running) {
                Socket client = serverSocket.accept();
                System.out.println("[server] connection from " + client.getInetAddress());
                ClientHandler handler = new ClientHandler(client, roomManager);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("[server] error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) throws Exception {
        Properties props = Database.loadServerProperties();
        Database.init(props);
        int port = Integer.parseInt(props.getProperty("port", "9090"));
        new ChatServer(port).start();
    }
}
