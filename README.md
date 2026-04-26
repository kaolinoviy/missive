# Missive

Secure desktop messenger with end-to-end encrypted DMs. JavaFX client + custom TCP server, MySQL persistence, RSA-2048 + AES-256-GCM crypto.

![status](https://img.shields.io/badge/status-stable-5fb878)
![java](https://img.shields.io/badge/java-21-blue)
![license](https://img.shields.io/badge/license-MIT-lightgrey)

---

## Download (Windows)

Grab the latest release from the [**Releases**](../../releases) tab → download `Missive-1.0-windows.zip` → extract → run `Missive.exe`.

**No Java installation required** — bundled JRE is included (~60 MB).

The client connects automatically to the public server `45.150.32.198:9090`.

---

## Features

- **Public channels** (`#general`, `#dev`, `#random`) — auto-joined on first login
- **Direct messages** — double-click a username in the MEMBERS panel
- **End-to-end encryption in DMs** — RSA-2048 key exchange + AES-256-GCM payloads. The server stores ciphertext only.
- **Message search** — search bar inside each channel, instant SQL-backed lookup
- **History** — last 50 messages loaded from MySQL when switching rooms
- **Export** — dump current room transcript to `~/.missive/exports/`
- **Online presence** — status dots in the member list update in real time
- **Typing indicator** — `username is typing…`

---

## Stack

| Layer | Tech |
|---|---|
| Client UI | JavaFX 21 + FXML + custom CSS |
| Network | Plain TCP, line-delimited JSON (Gson) |
| Server | Single-process Java, thread-per-client |
| Persistence | MySQL 8 via JDBC |
| Auth | bcrypt (jBCrypt) |
| Crypto | RSA-2048 + AES-256-GCM (`javax.crypto`) |
| Packaging | `jlink` + `jpackage` → standalone `.exe` |

---

## Project layout

```
src/main/java/missive/
  Main.java               — entry point (--server flag selects server mode)
  common/
    model/                — User, Message, ChatMessage, SystemMessage, Room
    protocol/             — Packet, PacketType (network DTOs)
    crypto/               — CryptoService, CryptoImpl, CryptoManager
  server/
    ChatServer.java       — accepts connections, spawns ClientHandler threads
    ClientHandler.java    — one thread per client, handles all packet types
    RoomManager.java      — tracks online users and room membership
    db/                   — DAO<T>, Database, UserDAO, MessageDAO, RoomDAO
  client/
    ChatClient.java       — socket client (background thread)
    MessageListener.java  — interface for packet dispatch
    ui/                   — App, LoginController, ChatController, cells
```

---

## Build from source

Requirements: JDK 21+ (with `jpackage` and `jlink`), Maven (wrapper included).

```powershell
# build the fat jar (~13 MB)
.\mvnw.cmd clean package

# run client locally (against localhost server)
$env:MISSIVE_SERVER = "localhost:9090"
.\mvnw.cmd javafx:run

# build standalone Windows .exe (~60 MB folder bundle)
.\package.bat
.\dist\Missive\Missive.exe
```

The env var **`MISSIVE_SERVER=host:port`** overrides the hardcoded default, useful for development or self-hosting.

---

## Self-host the server

Full step-by-step guide in [`deploy/DEPLOY-VPS.md`](deploy/DEPLOY-VPS.md). TL;DR:

```bash
# on Ubuntu 22.04 VPS
sudo apt install -y openjdk-21-jre-headless mysql-server
# create db, user, run setup.sql
# upload missive.jar + server.properties to /opt/missive/
# install deploy/missive.service as a systemd unit
sudo systemctl enable --now missive
sudo ufw allow 9090/tcp
```

Then point clients at `your-vps-ip:9090` via the `MISSIVE_SERVER` env var.

---

## How encryption works

On first launch the client generates an RSA-2048 keypair under `~/.missive/keys/`. The public key is uploaded to the server.

When sending a DM:
1. Client generates a random 256-bit AES key
2. Encrypts the message body with AES-GCM (random IV, AEAD authenticated)
3. Wraps the AES key with each recipient's RSA public key
4. Server forwards the ciphertext blob; recipients unwrap their copy of the key locally

Public channels (`#general`, etc.) are not encrypted — they're public broadcast.

---

## Key bindings

| Action | Shortcut |
|---|---|
| Send message | `Enter` |
| Search messages in current room | type in the search bar, `Enter` |
| Cancel search | `Esc` or click `clear ✕` |
| Start DM with user | double-click their name in MEMBERS |
| Create channel | click `+ new channel` |
| Export transcript | click `export` in the top bar |

---

## License

MIT
