-- Car Rental System Database Schema
-- For MySQL 8.0+

-- Create database
CREATE DATABASE IF NOT EXISTS car_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE car_rental;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    real_name VARCHAR(50),
    id_card VARCHAR(20),
    driving_license VARCHAR(50),
    member_level ENUM('NORMAL', 'GOLD', 'DIAMOND') DEFAULT 'NORMAL',
    points INT DEFAULT 0,
    balance DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Stores table
CREATE TABLE IF NOT EXISTS stores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    business_hours VARCHAR(100),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    status ENUM('OPEN', 'CLOSED', 'OFFLINE') DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_city (city),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    vin VARCHAR(50),
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    category ENUM('ECONOMY', 'COMFORT', 'SUV', 'BUSINESS', 'LUXURY', 'NEW_ENERGY', 'SEVEN_SEATS', 'PICKUP') DEFAULT 'ECONOMY',
    color VARCHAR(20),
    seats INT DEFAULT 5,
    transmission ENUM('AUTO', 'MANUAL') DEFAULT 'AUTO',
    fuel_type ENUM('GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID') DEFAULT 'GASOLINE',
    daily_price DECIMAL(10, 2) NOT NULL,
    deposit DECIMAL(10, 2) DEFAULT 0.00,
    deposit_free BOOLEAN DEFAULT FALSE,
    status ENUM('AVAILABLE', 'RENTED', 'MAINTENANCE', 'CLEANING', 'OFFLINE') DEFAULT 'AVAILABLE',
    store_id BIGINT,
    image_url VARCHAR(500),
    description TEXT,
    mileage INT DEFAULT 0,
    purchase_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE SET NULL,
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_store (store_id),
    INDEX idx_brand (brand)
) ENGINE=InnoDB;

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    store_id BIGINT,
    pickup_time TIMESTAMP,
    return_time TIMESTAMP,
    actual_pickup_time TIMESTAMP NULL,
    actual_return_time TIMESTAMP NULL,
    pickup_method ENUM('STORE', 'DELIVERY') DEFAULT 'STORE',
    return_method ENUM('STORE', 'PICKUP') DEFAULT 'STORE',
    daily_price DECIMAL(10, 2) NOT NULL,
    rental_days INT DEFAULT 1,
    total_price DECIMAL(10, 2) NOT NULL,
    deposit DECIMAL(10, 2) DEFAULT 0.00,
    insurance_fee DECIMAL(10, 2) DEFAULT 0.00,
    service_fee DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    coupon_id VARCHAR(50),
    status ENUM('PENDING', 'PAID', 'CONFIRMED', 'PICKED_UP', 'RETURNED', 'COMPLETED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
    payment_status ENUM('UNPAID', 'PAID', 'REFUNDED') DEFAULT 'UNPAID',
    payment_method ENUM('ALIPAY', 'WECHAT') NULL,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE SET NULL,
    INDEX idx_order_no (order_no),
    INDEX idx_user (user_id),
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB;

-- Coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type ENUM('FIXED', 'PERCENT') DEFAULT 'FIXED',
    value DECIMAL(10, 2) NOT NULL,
    min_amount DECIMAL(10, 2) DEFAULT 0.00,
    max_discount DECIMAL(10, 2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    total_count INT DEFAULT 0,
    used_count INT DEFAULT 0,
    status ENUM('ACTIVE', 'INACTIVE', 'EXPIRED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- User Coupons table
CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status ENUM('UNUSED', 'USED', 'EXPIRED') DEFAULT 'UNUSED',
    used_at TIMESTAMP NULL,
    order_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Insert sample data

-- Sample stores
INSERT INTO stores (name, address, city, phone, business_hours) VALUES
('北京首都机场店', '北京市顺义区首都机场T3航站楼', '北京', '010-64561234', '06:00-23:00'),
('上海虹桥机场店', '上海市闵行区虹桥机场T2航站楼', '上海', '021-54321234', '06:00-23:00'),
('广州白云机场店', '广州市白云区白云国际机场', '广州', '020-86121234', '06:00-23:00');

-- Sample vehicles
INSERT INTO vehicles (plate_number, brand, model, category, color, seats, transmission, fuel_type, daily_price, deposit, deposit_free, status, store_id, description) VALUES
('京A12345', 'Toyota', 'Camry', 'COMFORT', '白色', 5, 'AUTO', 'GASOLINE', 299.00, 3000.00, FALSE, 'AVAILABLE', 1, '丰田凯美瑞，舒适型轿车，适合商务出行'),
('京B23456', 'Honda', 'CR-V', 'SUV', '黑色', 5, 'AUTO', 'GASOLINE', 399.00, 5000.00, FALSE, 'AVAILABLE', 1, '本田CR-V，城市SUV，空间宽敞'),
('沪A34567', 'BMW', '5 Series', 'BUSINESS', '深蓝色', 5, 'AUTO', 'GASOLINE', 699.00, 10000.00, FALSE, 'AVAILABLE', 2, '宝马5系，商务首选'),
('沪B45678', 'Tesla', 'Model 3', 'NEW_ENERGY', '白色', 5, 'AUTO', 'ELECTRIC', 499.00, 5000.00, TRUE, 'AVAILABLE', 2, '特斯拉Model 3，纯电动，环保出行'),
('粤A56789', 'Mercedes', 'E-Class', 'LUXURY', '银色', 5, 'AUTO', 'GASOLINE', 899.00, 15000.00, FALSE, 'AVAILABLE', 3, '奔驰E级，豪华轿车'),
('粤B67890', 'Buick', 'GL8', 'SEVEN_SEATS', '黑色', 7, 'AUTO', 'GASOLINE', 599.00, 8000.00, FALSE, 'AVAILABLE', 3, '别克GL8，7座商务MPV，适合家庭出游');

-- Sample coupons
INSERT INTO coupons (code, name, type, value, min_amount, start_date, end_date, total_count, status) VALUES
('NEWUSER100', '新用户专享券', 'FIXED', 100.00, 300.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1000, 'ACTIVE'),
('WEEKEND50', '周末特惠券', 'FIXED', 50.00, 200.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 500, 'ACTIVE'),
('VIP10', 'VIP九折券', 'PERCENT', 10.00, 500.00, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 200, 'ACTIVE');
