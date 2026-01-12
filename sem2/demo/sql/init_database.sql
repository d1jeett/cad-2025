-- Создание базы данных
CREATE DATABASE hotel_booking_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Подключение к базе данных
\c hotel_booking_db;

-- Создание таблицы users (пользователи)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы rooms (комнаты)
CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(20) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.0,
    capacity INTEGER NOT NULL DEFAULT 1,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы bookings (бронирования)
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guest_name VARCHAR(100) NOT NULL,
    guest_email VARCHAR(100) NOT NULL,
    special_requests TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATE DEFAULT CURRENT_DATE,
    total_price DECIMAL(10, 2),
    
    -- Внешние ключи
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) 
        REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Проверки
    CONSTRAINT check_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT check_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'))
);

-- Индексы для ускорения поиска
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

CREATE INDEX idx_rooms_number ON rooms(number);
CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_available ON rooms(available);

CREATE INDEX idx_bookings_room_id ON bookings(room_id);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_dates ON bookings(check_in_date, check_out_date);
CREATE INDEX idx_bookings_guest_email ON bookings(guest_email);

-- Триггер для обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rooms_updated_at BEFORE UPDATE ON rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Вставка тестовых данных
INSERT INTO users (username, password, email, full_name, role) VALUES
    ('admin', '$2a$10$YourHashedPasswordHere', 'admin@hotel.com', 'Администратор', 'ROLE_ADMIN'),
    ('moderator', '$2a$10$YourHashedPasswordHere', 'moderator@hotel.com', 'Модератор', 'ROLE_MODERATOR'),
    ('user', '$2a$10$YourHashedPasswordHere', 'user@hotel.com', 'Пользователь', 'ROLE_USER');

INSERT INTO rooms (number, type, description, price, capacity) VALUES
    ('101', 'STANDARD', 'Стандартный номер с видом на город', 2500.00, 2),
    ('102', 'STANDARD', 'Стандартный номер с двуспальной кроватью', 2700.00, 2),
    ('201', 'VIP', 'VIP номер с джакузи', 5000.00, 2),
    ('202', 'VIP', 'VIP номер с балконом', 5500.00, 3),
    ('301', 'DELUXE', 'Делюкс номер с гостиной', 7500.00, 4),
    ('302', 'SUITE', 'Люкс номер с кухней', 10000.00, 4);

-- Комментарии к таблицам
COMMENT ON TABLE users IS 'Таблица пользователей системы';
COMMENT ON TABLE rooms IS 'Таблица номеров отеля';
COMMENT ON TABLE bookings IS 'Таблица бронирований номеров';

COMMENT ON COLUMN users.role IS 'Роль пользователя: ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN';
COMMENT ON COLUMN bookings.status IS 'Статус бронирования: PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED';
COMMENT ON COLUMN rooms.type IS 'Тип номера: STANDARD, VIP, DELUXE, SUITE, FAMILY, EXECUTIVE';