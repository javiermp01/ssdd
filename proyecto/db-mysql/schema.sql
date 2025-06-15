CREATE SCHEMA IF NOT EXISTS ssdd;
USE ssdd;

CREATE TABLE IF NOT EXISTS users(
    id INT AUTO_INCREMENT,
    email varchar(50),
    password_hash text,
    name text,
    token text,
    visits int,
    PRIMARY KEY(id)
);

-- Para búsquedas con email
CREATE INDEX user_email_idx ON users (email);

-- CUIDADO!! AÑADO UN USUARIO PARA PROBAR, PASSWORD: "admin"
INSERT INTO users VALUES ("1", "dsevilla@um.es", "21232f297a57a5a743894a0e4a801fc3", "diego", "TOKEN", 0);

-- Tabla de conversaciones
CREATE TABLE IF NOT EXISTS conversations (
    dialogue_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    name VARCHAR(100),
    status ENUM('READY', 'BUSY', 'FINISHED') DEFAULT 'READY',
    next_token VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Conversaciones de prueba para el usuario "dsevilla@um.es"
INSERT INTO conversations (user_id, name, status, next_token, created_at)
VALUES
(1, 'Primera conversacion de prueba', 'READY', 'token1', NOW()),
(1, 'Conversacion activa', 'BUSY', 'token2', NOW() - INTERVAL 1 DAY),
(1, 'Conversacion finalizada', 'FINISHED', 'token3', NOW() - INTERVAL 2 DAY);

-- Tabla de mensajes
CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dialogue_id INT,
    prompt TEXT NOT NULL,
    response TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dialogue_id) REFERENCES conversations(dialogue_id) ON DELETE CASCADE
);

-- Mensajes para la primera conversación (dialogue_id = 1)
INSERT INTO messages (dialogue_id, prompt, response)
VALUES
(1, 'Hola, ¿cómo estás?', 'Muy bien, gracias. ¿Y tú?'),
(1, '¿Qué puedes hacer?', 'Puedo ayudarte con muchas cosas.');

-- Mensajes para la segunda conversación (dialogue_id = 2)
INSERT INTO messages (dialogue_id, prompt, response)
VALUES
(2, '¿Cuál es el clima hoy?', 'Hace sol y la temperatura es de 25°C.');

-- No se añaden mensajes a la conversación 3 para dejarla vacía

-- Tabla de estadísticas
CREATE TABLE statistics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNIQUE,
    num_logins INT DEFAULT 0,
    num_prompts INT DEFAULT 0,
    last_activity TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO statistics (user_id, num_logins, num_prompts, last_activity)
VALUES (1, 0, 0, NOW());
