# Course Requirements Coverage

This document maps each topic from the course syllabus (weeks 1–10) to the place in the Missive codebase where it is demonstrated. Every reference uses absolute paths and line numbers so it can be opened directly in the IDE.

---

## Week 1 — Conditions, loops, methods

> Defining a method · calling a method · `if` / `switch` · `for` / `while`

Used in **every** class. Representative examples:

- **Switch on enum (modern arrow syntax):**
  `src/main/java/missive/server/ClientHandler.java:54-71` — dispatches every `PacketType` to a dedicated handler method.
- **For-each loop:**
  `src/main/java/missive/server/ClientHandler.java:107-116` — iterates DM rooms when building the auth response.
- **While loop:**
  `src/main/java/missive/server/ClientHandler.java:42-44` — `while ((line = in.readLine()) != null)` — main read loop on the server socket.
- **Method definition + call:** every controller / DAO / service has private helpers (`maybePlayNotify()`, `dmDisplayName()`, `mapRow()`, etc.).

---

## Week 2 — Arrays, ArrayList, LinkedList, String

| Concept | Where |
|---|---|
| **`byte[]` arrays** | `src/main/java/missive/common/crypto/CryptoImpl.java` — RSA/AES encrypt/decrypt return raw byte buffers |
| **`String[]` from `split`** | `src/main/java/missive/client/ui/LoginController.java:60` — `serverAddr.split(":")` |
| **`ArrayList<T>`** | `src/main/java/missive/server/db/MessageDAO.java:35` — `new ArrayList<>()` for paginated history; many other DAOs |
| **`LinkedList<T>` (as a deque / sliding-window queue)** | `src/main/java/missive/client/ui/ChatController.java:47` — `recentNotifyTimes` throttles the notification sound. `pollFirst()` / `addLast()` / `peekFirst()` are used at lines 106–110 |
| **`String` operations** | `String.format`, `replaceAll`, `trim`, `isBlank`, `split` everywhere |

---

## Week 3 — Classes, objects, encapsulation, getters/setters

All model classes use **private** fields with public getters and setters.

- `src/main/java/missive/common/model/User.java`
- `src/main/java/missive/common/model/Room.java` — 9 private fields (`id`, `name`, `description`, `isPrivate`, `isDm`, `unreadCount`, `members`) all with getters/setters
- `src/main/java/missive/common/model/Message.java` — abstract base class with private fields and protected access via getters
- `src/main/java/missive/common/protocol/Packet.java` — DTO with ~20 private fields, all encapsulated (lines 200–215)

**Array of objects:** `Packet.UserRecord[]`, `List<Packet.RoomRecord>`, `List<Packet.MessageRecord>` — passed across the network as arrays of model objects.

---

## Week 4 — Inheritance, polymorphism, `super`, override, casting, `instanceof`

| Concept | Where |
|---|---|
| **Abstract superclass + subclasses** | `src/main/java/missive/common/model/Message.java:9` (abstract) → `ChatMessage.java`, `SystemMessage.java` |
| **`super(...)` constructor call** | `src/main/java/missive/common/model/ChatMessage.java` and `SystemMessage.java` — both invoke `super(roomId, timestamp)` |
| **Method override** | `getFormattedDisplay()` declared in `Message`, overridden in both subclasses |
| **`instanceof` + pattern variable + cast** | `src/main/java/missive/client/ui/MessageCell.java` — `if (msg instanceof ChatMessage cm)` decides which layout to render. Also `src/main/java/missive/client/ui/ChatController.java:206` — `if (m instanceof ChatMessage cm)` for export stats |
| **Polymorphic collection** | `ObservableList<Message> messages` holds both `ChatMessage` and `SystemMessage` instances |
| **`extends` other than the model hierarchy** | `ClientHandler extends Thread`, `ChatClient extends Thread`, `App extends javafx.application.Application` |

---

## Week 5 — Abstract classes & interfaces

| Type | File | Purpose |
|---|---|---|
| Abstract class | `src/main/java/missive/common/model/Message.java:9` | base class for chat / system messages |
| Generic interface | `src/main/java/missive/server/db/DAO.java:6` — `public interface DAO<T>` | uniform DAO contract — `UserDAO`, `RoomDAO`, `MessageDAO` all implement it |
| Interface | `src/main/java/missive/common/crypto/CryptoService.java:8` | crypto contract — `CryptoImpl` implements it; `CryptoManager` depends on the interface |
| Interface | `src/main/java/missive/client/MessageListener.java:5` | observer pattern — both `LoginController` and `ChatController` implement it |

This shows all three: **abstract class** vs **interface (with default method behaviour via dispatch)** vs **interface as a pure contract**.

---

## Week 6 — Sets, maps (HashSet, HashMap, TreeMap)

| Collection | Where |
|---|---|
| **`HashMap`** | `src/main/java/missive/client/ui/ChatController.java:42` — `Map<Integer, ObservableList<Message>> roomMessages` (per-room message buffer) |
| **`ConcurrentHashMap`** | `src/main/java/missive/server/RoomManager.java:13-15` — thread-safe maps for online users and room membership |
| **`HashSet`** | `src/main/java/missive/client/ui/ChatController.java:45` — `seenMessageIds` deduplicates incoming broadcasts (O(1) `add` + boolean return short-circuits duplicates at line 309) |
| **`TreeMap`** | `src/main/java/missive/client/ui/ChatController.java:204` — `TreeMap<String, Integer> stats` aggregates messages-per-sender for the export header. `TreeMap` keeps senders sorted alphabetically when written out (lines 218–221) |

---

## Week 7 — JavaFX

The entire client UI is built on JavaFX 21:

- **`Application` lifecycle:** `src/main/java/missive/client/ui/App.java:10`
- **FXML:** `src/main/resources/fxml/login.fxml`, `src/main/resources/fxml/chat.fxml`
- **Controllers with `@FXML` injection:** `LoginController`, `ChatController`
- **Custom `ListCell` factories:** `RoomCell`, `MessageCell`, `UserCell` — all extend `ListCell<T>` and override `updateItem()`
- **CSS theming:** `src/main/resources/css/terminal.css` — full custom theme (colours, scroll-bars, dialogs, focus states)
- **Layout:** `BorderPane`, `VBox`, `HBox`, `StackPane`, `Region`
- **Controls:** `ListView`, `TextField`, `PasswordField`, `Button`, `Label`, `Tooltip`, `Alert`, `TextInputDialog`
- **Property binding / observables:** `ObservableList<Room>` drives the room list; selection listener on line 60 of `ChatController.java`
- **Event handlers:** `setOnKeyPressed`, `setOnMouseClicked`, `setOnAction` (FXML)

---

## Week 8 — JavaFX multimedia

| Asset | Where loaded | Where used |
|---|---|---|
| `src/main/resources/images/logo.png` (256×256) | `src/main/resources/fxml/login.fxml:15-17` — `<ImageView><Image url="@/images/logo.png"/></ImageView>` | login screen |
| `src/main/resources/images/icon.png` (64×64) | `src/main/java/missive/client/ui/App.java:21-24` — `new Image(stream)` → `stage.getIcons().add(...)` | window/taskbar icon |
| `src/main/resources/sounds/notify.wav` (~8 KB) | `src/main/java/missive/client/ui/ChatController.java:94-99` — `new AudioClip(url.toExternalForm())` | played at `ChatController.java:111` (`notifySound.play(0.45)`) on incoming messages in inactive rooms |

The notification sound is **throttled** via the `LinkedList` sliding window from week 2 (max 3 plays per 5 seconds) — see `maybePlayNotify()` at `ChatController.java:101-112`.

---

## Week 9 — Files & network

### Files

| API | Where |
|---|---|
| `FileInputStream` | `src/main/java/missive/server/db/Database.java:47` — loads `server.properties` |
| `BufferedReader` | `src/main/java/missive/server/ClientHandler.java:40`, `src/main/java/missive/client/ChatClient.java:50` — wraps the socket input stream |
| `BufferedWriter` | `src/main/java/missive/client/ui/ChatController.java:211` — writes export transcript; also `PrintWriter(new BufferedWriter(...))` on both server and client sockets |
| `Files.createDirectories` / `Files.exists` / `Files.writeString` / `Files.readString` / `Files.newBufferedWriter` | `src/main/java/missive/common/crypto/CryptoManager.java:23,27,36,43`, `src/main/java/missive/client/ui/ChatController.java:169,173` |
| `Path` / `Paths.get` | `CryptoManager.java:19,24,25`, `ChatController.java:168,172` |

### Network

| Layer | Where |
|---|---|
| **`ServerSocket`** | `src/main/java/missive/server/ChatServer.java` — `serverSocket.accept()` loop |
| **`Socket`** | `src/main/java/missive/client/ChatClient.java:48` — `new Socket(host, port)` |
| **Custom protocol** | line-delimited JSON over TCP, `src/main/java/missive/common/protocol/Packet.java` (Gson serialisation at lines 57–63) |

---

## Week 10 — Multithreading

| Concept | Where |
|---|---|
| **Thread per connection** (server) | `src/main/java/missive/server/ClientHandler.java:13` — `public class ClientHandler extends Thread`. `ChatServer` calls `new ClientHandler(...).start()` on each accept |
| **Background listener thread** (client) | `src/main/java/missive/client/ChatClient.java:13` — `public class ChatClient extends Thread`; the `run()` method reads packets in a loop |
| **Daemon threads** | `src/main/java/missive/client/ui/LoginController.java:74-87` — connection attempt runs on a daemon thread to keep the UI responsive |
| **Cross-thread UI dispatch** | `Platform.runLater(...)` used in `ChatController.onPacketReceived` (line 245) and `LoginController.onPacketReceived` — bridges the network thread back to the JavaFX Application Thread |
| **Concurrent collections** | `src/main/java/missive/server/RoomManager.java:13-15` — `ConcurrentHashMap`, `CopyOnWriteArrayList` provide thread-safe access shared across handler threads |
| **Throw-away thread for timed UI clear** | `src/main/java/missive/client/ui/ChatController.java` — typing-indicator clears itself via a sleeping thread that calls `Platform.runLater` |

---

## Bonus: features beyond the syllabus

- **End-to-end encryption** — RSA-2048 key pair per user (`CryptoManager`), AES-256-GCM for message bodies, server stores ciphertext only. Implemented in `CryptoImpl` and used by the client when sending DMs.
- **Generic DAO pattern** — `DAO<T>` interface with type-safe `findById`, `save`, `update`, `delete`.
- **Try-with-resources** — every JDBC call, file write, and socket creation is wrapped (e.g. `try (Connection c = db.getConnection(); PreparedStatement ps = ...)` in every DAO).
- **Modern Java features** — switch expressions, pattern matching for `instanceof`, text blocks for SQL (`Database.java`, `MessageDAO.java`), `var` for local-type inference.
- **bcrypt password hashing** — `UserDAO.register()` / `authenticate()` use `BCrypt.hashpw` / `BCrypt.checkpw`.
- **systemd-managed VPS deployment** — see `deploy/DEPLOY-VPS.md` and `deploy/missive.service`.
- **Standalone Windows distribution** — `package.bat` builds a self-contained `Missive.exe` via `jlink` + `jpackage` (no Java install required for the user).

---

## Live demo at hand

| Demonstrates | How to show it |
|---|---|
| Network + client-server | run client against the public VPS `45.150.32.198:9090` (no setup needed) |
| Multithreading | open 2 clients, observe simultaneous chat |
| Direct messages + encryption | double-click a user → DM opens, toolbar shows `direct message · end-to-end encrypted` |
| File I/O | click `export` in any room → file appears in `~/.missive/exports/`, header shows `TreeMap` stats |
| Search | type in the search bar of any channel |
| Multimedia | another client sends a message while a different room is active → notification sound plays |
| Inheritance / polymorphism | system messages (`user joined`, `user left`) render differently from chat messages — same `ListView`, different `instanceof` branch in `MessageCell` |
