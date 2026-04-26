# Соответствие требованиям курса

Документ показывает, как каждая тема из программы (недели 1–10) реализована в коде проекта Missive. Все ссылки даны в формате `путь/к/файлу.java:номер_строки` и кликабельны в IDE.

---

## Неделя 1 — Условия, циклы, методы

> Определение метода · вызов метода · `if` / `switch` · `for` / `while`

Используется **повсеместно**. Характерные примеры:

- **Switch по enum (современный синтаксис со стрелкой):**
  `src/main/java/missive/server/ClientHandler.java:54-71` — диспетчеризация всех `PacketType` к соответствующим обработчикам.
- **Цикл `for-each`:**
  `src/main/java/missive/server/ClientHandler.java:107-116` — итерация по DM-комнатам при формировании ответа на авторизацию.
- **Цикл `while`:**
  `src/main/java/missive/server/ClientHandler.java:42-44` — `while ((line = in.readLine()) != null)` — основной цикл чтения серверного сокета.
- **Объявление и вызов методов:** в каждом контроллере / DAO / сервисе есть приватные хелперы (`maybePlayNotify()`, `dmDisplayName()`, `mapRow()` и т.д.).

---

## Неделя 2 — Массивы, ArrayList, LinkedList, String

| Концепция | Где |
|---|---|
| **Массивы `byte[]`** | `src/main/java/missive/common/crypto/CryptoImpl.java` — методы шифрования RSA/AES возвращают сырые буферы байт |
| **`String[]` через `split`** | `src/main/java/missive/client/ui/LoginController.java:60` — `serverAddr.split(":")` |
| **`ArrayList<T>`** | `src/main/java/missive/server/db/MessageDAO.java:35` — `new ArrayList<>()` для постраничной истории; используется во всех DAO |
| **`LinkedList<T>` (как очередь / sliding-window)** | `src/main/java/missive/client/ui/ChatController.java:47` — `recentNotifyTimes` ограничивает частоту звуков уведомлений. Методы `pollFirst()` / `addLast()` / `peekFirst()` — строки 106–110 |
| **`String`-операции** | `String.format`, `replaceAll`, `trim`, `isBlank`, `split` встречаются по всему коду |

---

## Неделя 3 — Классы, объекты, инкапсуляция, getters/setters

Все модельные классы используют **private** поля с публичными геттерами и сеттерами.

- `src/main/java/missive/common/model/User.java`
- `src/main/java/missive/common/model/Room.java` — 9 приватных полей (`id`, `name`, `description`, `isPrivate`, `isDm`, `unreadCount`, `members`), все доступны через getters/setters
- `src/main/java/missive/common/model/Message.java` — абстрактный базовый класс с приватными полями и protected-доступом через геттеры
- `src/main/java/missive/common/protocol/Packet.java` — DTO с ~20 приватными полями, полная инкапсуляция (строки 200–215)

**Массив объектов:** `Packet.UserRecord[]`, `List<Packet.RoomRecord>`, `List<Packet.MessageRecord>` — пересылаются по сети как массивы доменных объектов.

---

## Неделя 4 — Наследование, полиморфизм, `super`, переопределение, приведение, `instanceof`

| Концепция | Где |
|---|---|
| **Абстрактный суперкласс + наследники** | `src/main/java/missive/common/model/Message.java:9` (abstract) → `ChatMessage.java`, `SystemMessage.java` |
| **Вызов `super(...)` конструктора** | `src/main/java/missive/common/model/ChatMessage.java` и `SystemMessage.java` — оба передают `super(roomId, timestamp)` |
| **Переопределение методов** | `getFormattedDisplay()` объявлен в `Message`, переопределён в обоих наследниках |
| **`instanceof` с pattern variable + приведение типов** | `src/main/java/missive/client/ui/MessageCell.java` — `if (msg instanceof ChatMessage cm)` определяет, какой layout рисовать. Также `src/main/java/missive/client/ui/ChatController.java:206` — `if (m instanceof ChatMessage cm)` для статистики экспорта |
| **Полиморфная коллекция** | `ObservableList<Message> messages` хранит одновременно `ChatMessage` и `SystemMessage` |
| **`extends` за пределами модели** | `ClientHandler extends Thread`, `ChatClient extends Thread`, `App extends javafx.application.Application` |

---

## Неделя 5 — Абстрактные классы и интерфейсы

| Тип | Файл | Назначение |
|---|---|---|
| Абстрактный класс | `src/main/java/missive/common/model/Message.java:9` | базовый класс для chat / system сообщений |
| Дженерик-интерфейс | `src/main/java/missive/server/db/DAO.java:6` — `public interface DAO<T>` | единый контракт DAO — `UserDAO`, `RoomDAO`, `MessageDAO` все его реализуют |
| Интерфейс | `src/main/java/missive/common/crypto/CryptoService.java:8` | контракт криптографии — `CryptoImpl` его реализует, `CryptoManager` зависит от интерфейса |
| Интерфейс | `src/main/java/missive/client/MessageListener.java:5` | паттерн «наблюдатель» — `LoginController` и `ChatController` оба его реализуют |

Покрыты все три случая: **абстрактный класс**, **дженерик-интерфейс** и **обычный интерфейс как контракт**.

---

## Неделя 6 — Множества и словари (HashSet, HashMap, TreeMap)

| Коллекция | Где |
|---|---|
| **`HashMap`** | `src/main/java/missive/client/ui/ChatController.java:42` — `Map<Integer, ObservableList<Message>> roomMessages` (буфер сообщений по комнатам) |
| **`ConcurrentHashMap`** | `src/main/java/missive/server/RoomManager.java:13-15` — потокобезопасные словари для онлайн-пользователей и состава комнат |
| **`HashSet`** | `src/main/java/missive/client/ui/ChatController.java:45` — `seenMessageIds` отсеивает дубликаты входящих сообщений (вставка O(1), возвращаемый boolean используется для короткого выхода в строке 309) |
| **`TreeMap`** | `src/main/java/missive/client/ui/ChatController.java:204` — `TreeMap<String, Integer> stats` агрегирует количество сообщений по отправителям для шапки экспорта. `TreeMap` гарантирует алфавитный порядок при выводе (строки 218–221) |

---

## Неделя 7 — JavaFX

Весь клиентский UI построен на JavaFX 21:

- **Жизненный цикл `Application`:** `src/main/java/missive/client/ui/App.java:10`
- **FXML:** `src/main/resources/fxml/login.fxml`, `src/main/resources/fxml/chat.fxml`
- **Контроллеры с `@FXML` инъекцией:** `LoginController`, `ChatController`
- **Кастомные `ListCell`-фабрики:** `RoomCell`, `MessageCell`, `UserCell` — все наследуют `ListCell<T>` и переопределяют `updateItem()`
- **CSS-тема:** `src/main/resources/css/terminal.css` — полная кастомизация (цвета, скроллбары, диалоги, фокус)
- **Layout-менеджеры:** `BorderPane`, `VBox`, `HBox`, `StackPane`, `Region`
- **Контролы:** `ListView`, `TextField`, `PasswordField`, `Button`, `Label`, `Tooltip`, `Alert`, `TextInputDialog`
- **Property binding / observables:** `ObservableList<Room>` управляет списком комнат; selection listener — строка 60 `ChatController.java`
- **Обработчики событий:** `setOnKeyPressed`, `setOnMouseClicked`, `setOnAction` (через FXML)

---

## Неделя 8 — JavaFX multimedia

| Ресурс | Где загружается | Где используется |
|---|---|---|
| `src/main/resources/images/logo.png` (256×256) | `src/main/resources/fxml/login.fxml:15-17` — `<ImageView><Image url="@/images/logo.png"/></ImageView>` | экран логина |
| `src/main/resources/images/icon.png` (64×64) | `src/main/java/missive/client/ui/App.java:21-24` — `new Image(stream)` → `stage.getIcons().add(...)` | иконка окна и панели задач |
| `src/main/resources/sounds/notify.wav` (~8 КБ) | `src/main/java/missive/client/ui/ChatController.java:94-99` — `new AudioClip(url.toExternalForm())` | проигрывается в `ChatController.java:111` (`notifySound.play(0.45)`) при входящих сообщениях в неактивную комнату |

Звук уведомлений ограничивается через **sliding window** на `LinkedList` (тема недели 2) — не более 3 проигрываний за 5 секунд. См. `maybePlayNotify()` в `ChatController.java:101-112`.

---

## Неделя 9 — Файлы и сеть

### Файлы

| API | Где |
|---|---|
| `FileInputStream` | `src/main/java/missive/server/db/Database.java:47` — загрузка `server.properties` |
| `BufferedReader` | `src/main/java/missive/server/ClientHandler.java:40`, `src/main/java/missive/client/ChatClient.java:50` — обёртка над input-stream сокета |
| `BufferedWriter` | `src/main/java/missive/client/ui/ChatController.java:211` — запись экспортированной истории; также `PrintWriter(new BufferedWriter(...))` на серверном и клиентском сокетах |
| `Files.createDirectories` / `Files.exists` / `Files.writeString` / `Files.readString` / `Files.newBufferedWriter` | `src/main/java/missive/common/crypto/CryptoManager.java:23,27,36,43`, `src/main/java/missive/client/ui/ChatController.java:169,173` |
| `Path` / `Paths.get` | `CryptoManager.java:19,24,25`, `ChatController.java:168,172` |

### Сеть

| Слой | Где |
|---|---|
| **`ServerSocket`** | `src/main/java/missive/server/ChatServer.java` — цикл `serverSocket.accept()` |
| **`Socket`** | `src/main/java/missive/client/ChatClient.java:48` — `new Socket(host, port)` |
| **Собственный протокол** | line-delimited JSON поверх TCP, `src/main/java/missive/common/protocol/Packet.java` (Gson-сериализация в строках 57–63) |

---

## Неделя 10 — Многопоточность

| Концепция | Где |
|---|---|
| **Поток на каждое подключение** (сервер) | `src/main/java/missive/server/ClientHandler.java:13` — `public class ClientHandler extends Thread`. `ChatServer` вызывает `new ClientHandler(...).start()` на каждом `accept` |
| **Фоновый поток-listener** (клиент) | `src/main/java/missive/client/ChatClient.java:13` — `public class ChatClient extends Thread`; метод `run()` читает пакеты в цикле |
| **Демон-потоки** | `src/main/java/missive/client/ui/LoginController.java:74-87` — попытка подключения уходит на демон-поток, чтобы UI не подвисал |
| **Передача данных в UI-поток** | `Platform.runLater(...)` — `ChatController.onPacketReceived` (строка 245) и `LoginController.onPacketReceived` — мост между сетевым потоком и JavaFX Application Thread |
| **Потокобезопасные коллекции** | `src/main/java/missive/server/RoomManager.java:13-15` — `ConcurrentHashMap`, `CopyOnWriteArrayList` обеспечивают безопасный доступ из множества handler-потоков |
| **Одноразовый поток для отложенного действия** | `src/main/java/missive/client/ui/ChatController.java` — индикатор «печатает…» сам себя сбрасывает через спящий поток с `Platform.runLater` |

---

## Сверх программы

- **End-to-end шифрование** — пара RSA-2048 на каждого пользователя (`CryptoManager`), AES-256-GCM для тел сообщений, сервер хранит только шифротекст. Реализовано в `CryptoImpl` и используется клиентом при отправке DM.
- **Дженерик-DAO** — интерфейс `DAO<T>` с типобезопасными `findById`, `save`, `update`, `delete`.
- **Try-with-resources** — каждый JDBC-вызов, запись в файл и создание сокета обёрнуты (например `try (Connection c = db.getConnection(); PreparedStatement ps = ...)` во всех DAO).
- **Современные возможности Java** — switch-выражения, pattern matching для `instanceof`, text blocks для SQL (`Database.java`, `MessageDAO.java`), `var` для вывода типов.
- **bcrypt-хеширование паролей** — `UserDAO.register()` / `authenticate()` используют `BCrypt.hashpw` / `BCrypt.checkpw`.
- **Деплой через systemd на VPS** — см. `deploy/DEPLOY-VPS.md` и `deploy/missive.service`.
- **Standalone Windows-сборка** — `package.bat` собирает самодостаточный `Missive.exe` через `jlink` + `jpackage` (Java у пользователя устанавливать не нужно).
