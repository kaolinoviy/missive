-- missive db schema
-- run: mysql -u root -p < setup.sql

CREATE DATABASE IF NOT EXISTS missive CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE missive;

CREATE USER IF NOT EXISTS 'missive_user'@'localhost' IDENTIFIED BY 'missive_pass';
GRANT ALL PRIVILEGES ON missive.* TO 'missive_user'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    public_key TEXT,
    avatar_color VARCHAR(7) DEFAULT '#00cc44',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_private BOOLEAN DEFAULT FALSE,
    is_dm BOOLEAN DEFAULT FALSE,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS room_members (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_id INT NOT NULL,
    sender_id INT NOT NULL,
    sender_name VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    encrypted BOOLEAN DEFAULT FALSE,
    iv VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- default public channels
INSERT IGNORE INTO rooms (id, name, description, is_private, created_by)
VALUES (1, 'general', 'general chat', FALSE, NULL),
       (2, 'random', 'off-topic', FALSE, NULL),
       (3, 'dev', 'development stuff', FALSE, NULL);
