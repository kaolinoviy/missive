# missive

desktop messenger with E2E encryption. client-server architecture over TCP, JavaFX UI.

## stack

- java 21 + javafx 21
- mysql 8 (jdbc)
- gson (json protocol over tcp)
- jbcrypt (password hashing)
- RSA-2048 + AES-256-GCM (end-to-end encryption)

## project structure

```
src/main/java/missive/
  Main.java               -- entry point (--server flag = server mode)
  common/
    model/                -- User, Message (abstract), ChatMessage, SystemMessage, Room
    protocol/             -- Packet, PacketType (network layer)
    crypto/               -- CryptoService (interface), CryptoImpl, CryptoManager
  server/
    ChatServer.java       -- listens for connections, spawns ClientHandler threads
    ClientHandler.java    -- one thread per client, handles all packet types
    RoomManager.java      -- tracks online users and room membership
    db/                   -- DAO<T> interface, Database, UserDAO, MessageDAO, RoomDAO
  client/
    ChatClient.java       -- socket client running as a background thread
    MessageListener.java  -- interface for packet dispatch
    ui/                   -- JavaFX: App, LoginController, ChatController, cells
```

## setup

### database

```sql
mysql -u root -p < setup.sql
```

### run server

locally:
```
mvn package -q
java -jar target/missive.jar --server
```

on VPS (edit server.properties first):
```
scp target/missive.jar server.properties user@62.60.245.52:~/missive/
ssh user@62.60.245.52 "cd ~/missive && java -jar missive.jar --server"
```

### run client

```
java -jar target/missive.jar
```

or via maven:
```
mvn javafx:run
```

enter server address in the login screen (default `localhost:9090`, or `62.60.245.52:9090` for VPS).

## encryption

on first launch the client generates an RSA-2048 key pair stored in `~/.missive/keys/`.
the public key is uploaded to the server. when sending messages, the content is encrypted
with AES-256-GCM using a random key, and that key is RSA-encrypted for each recipient.
the server stores and forwards ciphertext — it cannot read messages.

## how rooms work

public channels (general, random, dev) are created from `setup.sql`.
every user is auto-joined to them on login. new channels can be created from the UI.
chat history (last 50 messages) is loaded from MySQL when switching rooms.

## key bindings

- `enter` — send message
- click `+ new channel` — create a channel
- click `export` — save current room history to `~/.missive/exports/`
