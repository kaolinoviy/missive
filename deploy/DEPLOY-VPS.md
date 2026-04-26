# Deploying Missive Server on Ubuntu 22.04 VPS

Step-by-step guide for `62.60.245.52` (or any Ubuntu 22.04 VPS).

---

## 1. Install dependencies on VPS

```bash
ssh user@62.60.245.52

sudo apt update
sudo apt install -y openjdk-21-jre-headless mysql-server ufw
```

Verify:
```bash
java --version    # should print 21.x
systemctl status mysql
```

---

## 2. Initialize MySQL

```bash
sudo mysql_secure_installation     # set root password, defaults are fine

# create db + user (run from your local missive/setup.sql, or inline):
sudo mysql <<'SQL'
CREATE DATABASE IF NOT EXISTS missive CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'missive_user'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON missive.* TO 'missive_user'@'localhost';
FLUSH PRIVILEGES;
SQL
```

Then load the schema (upload `setup.sql` from your machine first):
```powershell
# on your windows pc:
scp setup.sql user@62.60.245.52:/tmp/
```
```bash
# on vps:
sudo mysql missive < /tmp/setup.sql
```

---

## 3. Create the missive system user and directories

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin missive
sudo mkdir -p /opt/missive /var/log/missive
sudo chown missive:missive /opt/missive /var/log/missive
```

---

## 4. Upload the server JAR + config

From your Windows machine:
```powershell
scp target\missive.jar user@62.60.245.52:/tmp/
scp deploy\missive.service user@62.60.245.52:/tmp/
```

On the VPS:
```bash
sudo mv /tmp/missive.jar /opt/missive/
sudo chown missive:missive /opt/missive/missive.jar

# create server.properties (MUST match the mysql user/pass above)
sudo tee /opt/missive/server.properties >/dev/null <<EOF
port=9090
db.host=localhost
db.port=3306
db.name=missive
db.user=missive_user
db.password=CHANGE_ME_STRONG_PASSWORD
EOF
sudo chown missive:missive /opt/missive/server.properties
sudo chmod 600 /opt/missive/server.properties
```

---

## 5. Install the systemd unit

```bash
sudo mv /tmp/missive.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now missive
sudo systemctl status missive
```

You should see `active (running)`. Logs:
```bash
sudo journalctl -u missive -f
sudo tail -f /var/log/missive/server.log
```

---

## 6. Open the firewall

```bash
sudo ufw allow OpenSSH
sudo ufw allow 9090/tcp comment 'missive chat'
sudo ufw enable
sudo ufw status
```

If your provider has its own firewall (Hetzner / Selectel / Timeweb panel), open port `9090` there too.

---

## 7. Test from your PC

```powershell
# quick smoke test
Test-NetConnection -ComputerName 62.60.245.52 -Port 9090
```

Should show `TcpTestSucceeded : True`.

In the **Missive desktop client**, type `62.60.245.52:9090` in the SERVER field, register, chat.

---

## Common operations

```bash
# restart server (e.g. after uploading a new jar)
sudo systemctl restart missive

# stop / start
sudo systemctl stop missive
sudo systemctl start missive

# view logs
sudo journalctl -u missive -n 200 --no-pager
sudo tail -100 /var/log/missive/server.log

# upgrade to a new build
scp target\missive.jar user@62.60.245.52:/tmp/
ssh user@62.60.245.52 "sudo mv /tmp/missive.jar /opt/missive/ && sudo chown missive:missive /opt/missive/missive.jar && sudo systemctl restart missive"
```

---

## Troubleshooting

**Server won't start, logs say "Communications link failure"**
MySQL not running or wrong password in `server.properties`. Check:
```bash
sudo systemctl status mysql
sudo cat /opt/missive/server.properties
```

**Clients can connect from VPS itself but not from outside**
Firewall. Check both `ufw status` and your hosting provider's panel.

**Port 9090 already in use**
```bash
sudo ss -tlnp | grep 9090
```
Edit `/opt/missive/server.properties` to a different `port=`, then `sudo systemctl restart missive`.

**Want to use port 443 (HTTPS-friendly) instead of 9090**
Either change the port in server.properties (and run as root, since 443 is privileged), OR add a reverse-proxy (haproxy / nginx stream) on 443 → localhost:9090. Easier: just stick with 9090.
