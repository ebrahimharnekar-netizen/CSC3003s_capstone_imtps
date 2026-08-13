CREATE DATABASE IF NOT EXISTS imtps_db;
USE imtps_db;

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL, 
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE tasks (
    task_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    task_name    VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    importance   INT CHECK (importance BETWEEN 1 AND 5),
    duration     DECIMAL(5,2),
    due_date     DATETIME NULL,
    start_time   DATETIME NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE preferences (
    preference_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    peak_prodtime  INT CHECK (peak_prodtime BETWEEN 1 AND 3),
    daily_prod_hours    INT CHECK (daily_prod_hours BETWEEN 1 AND 10),
    preferred_quick_first BOOLEAN,

    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
