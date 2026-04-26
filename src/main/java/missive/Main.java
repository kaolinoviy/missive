package missive;

import missive.client.ui.App;
import missive.server.ChatServer;
import missive.server.db.Database;
import javafx.application.Application;

import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--server")) {
            Properties props = Database.loadServerProperties();
            Database.init(props);
            int port = Integer.parseInt(props.getProperty("port", "9090"));
            System.out.println("[missive-server] port=" + port);
            new ChatServer(port).start();
        } else {
            Application.launch(App.class, args);
        }
    }
}
