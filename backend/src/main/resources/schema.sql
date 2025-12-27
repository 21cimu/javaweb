-- Car Rental System Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS car_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE car_rental;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    real_name VARCHAR(50),
    id_card VARCHAR(20),
    driver_license VARCHAR(30),
    driver_license_image VARCHAR(500),
    id_card_front VARCHAR(500),
    id_card_back VARCHAR(500),
    verification_status TINYINT DEFAULT 0 COMMENT '0-未认证, 1-待审核, 2-已认证, 3-认证失败',
    avatar VARCHAR(500),
    gender TINYINT DEFAULT 0 COMMENT '0-未知, 1-男, 2-女',
    birthday DATETIME,
    role VARCHAR(20) DEFAULT 'user' COMMENT 'user, admin, superadmin',
    status TINYINT DEFAULT 1 COMMENT '0-禁用, 1-正常',
    balance DECIMAL(10, 2) DEFAULT 0.00,
    points INT DEFAULT 0,
    invite_code VARCHAR(20) UNIQUE,
    inviter_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_username (username),
    INDEX idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add VIP columns to users
ALTER TABLE users
    ADD COLUMN cumulative_spending DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN vip_level TINYINT NOT NULL DEFAULT 0 COMMENT '0-normal,1-gold,2-diamond';

-- Stores table
CREATE TABLE IF NOT EXISTS stores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    phone VARCHAR(20),
    business_hours VARCHAR(100) DEFAULT '08:00-22:00',
    image VARCHAR(500),
    status TINYINT DEFAULT 1 COMMENT '0-关闭, 1-营业中',
    vehicle_count INT DEFAULT 0,
    available_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_city (city),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vin VARCHAR(50) UNIQUE,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    series VARCHAR(50),
    year INT,
    color VARCHAR(20),
    seats INT DEFAULT 5,
    fuel_type VARCHAR(20) COMMENT 'gasoline, diesel, electric, hybrid',
    transmission VARCHAR(20) DEFAULT 'auto' COMMENT 'auto, manual',
    category VARCHAR(20) COMMENT 'economy, compact, midsize, suv, luxury, minivan',
    store_id BIGINT,
    daily_price DECIMAL(10, 2) NOT NULL,
    weekly_price DECIMAL(10, 2),
    monthly_price DECIMAL(10, 2),
    deposit DECIMAL(10, 2) DEFAULT 0.00,
    mileage INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '0-下架, 1-可租, 2-预订中, 3-出租中, 4-维修中, 5-清洁中, 6-事故',
    main_image VARCHAR(500),
    images TEXT,
    features TEXT COMMENT 'JSON array of features',
    description TEXT,
    purchase_date DATE,
    purchase_price DECIMAL(12, 2),
    insurance_expiry DATE,
    inspection_expiry DATE,
    registration_expiry DATE,
    last_maintenance_date DATE,
    view_count INT DEFAULT 0,
    order_count INT DEFAULT 0,
    rating DECIMAL(2, 1) DEFAULT 5.0,
    is_hot TINYINT(1) DEFAULT 0,
    is_new TINYINT(1) DEFAULT 0,
    no_deposit TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store (store_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_brand (brand),
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    user_phone VARCHAR(20),
    vehicle_id BIGINT NOT NULL,
    vehicle_name VARCHAR(100),
    vehicle_plate VARCHAR(20),
    pickup_store_id BIGINT,
    pickup_store_name VARCHAR(100),
    return_store_id BIGINT,
    return_store_name VARCHAR(100),
    delivery_address VARCHAR(500),
    delivery_city VARCHAR(50),
    delivery_district VARCHAR(50),
    delivery_lng DECIMAL(10, 7),
    delivery_lat DECIMAL(10, 7),
    pickup_time DATETIME NOT NULL,
    return_time DATETIME NOT NULL,
    actual_pickup_time DATETIME,
    actual_return_time DATETIME,
    rental_days INT NOT NULL,
    daily_price DECIMAL(10, 2) NOT NULL,
    rental_amount DECIMAL(10, 2) NOT NULL,
    deposit DECIMAL(10, 2) DEFAULT 0.00,
    insurance_amount DECIMAL(10, 2) DEFAULT 0.00,
    service_amount DECIMAL(10, 2) DEFAULT 0.00,
    extra_amount DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2) DEFAULT 0.00,
    refund_amount DECIMAL(10, 2) DEFAULT 0.00,
    coupon_id BIGINT,
    coupon_code VARCHAR(50),
    status TINYINT DEFAULT 1 COMMENT '0-已取消, 1-待审核, 2-审核失败, 3-待支付, 4-待取车, 5-用车中, 6-待还车, 7-待结算, 8-已完成, 9-退款中, 10-已退款',
    pickup_code VARCHAR(10),
    pickup_mileage INT,
    return_mileage INT,
    pickup_fuel INT,
    return_fuel INT,
    pickup_images TEXT,
    return_images TEXT,
    pickup_note TEXT,
    return_note TEXT,
    contract_url VARCHAR(500),
    payment_method TINYINT COMMENT '1-微信, 2-支付宝, 3-银行卡, 4-余额',
    payment_no VARCHAR(100),
    payment_time DATETIME,
    insurance_type VARCHAR(20),
    add_services TEXT COMMENT 'JSON array',
    cancel_reason VARCHAR(500),
    cancel_time DATETIME,
    rating TINYINT,
    review TEXT,
    review_images TEXT,
    review_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_status (status),
    INDEX idx_order_no (order_no),
    INDEX idx_created (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add spending_accounted to orders (after orders table definition)
ALTER TABLE orders
    ADD COLUMN spending_accounted TINYINT NOT NULL DEFAULT 0 COMMENT '0-not accounted,1-accounted';

-- Add delivery address fields to orders (after orders table definition)
ALTER TABLE orders
    ADD COLUMN delivery_address VARCHAR(500),
    ADD COLUMN delivery_city VARCHAR(50),
    ADD COLUMN delivery_district VARCHAR(50),
    ADD COLUMN delivery_lng DECIMAL(10, 7),
    ADD COLUMN delivery_lat DECIMAL(10, 7);

-- Coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    type TINYINT NOT NULL COMMENT '1-满减, 2-折扣, 3-固定金额',
    min_amount DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2),
    discount_rate DECIMAL(3, 2),
    max_discount DECIMAL(10, 2),
    total_count INT,
    used_count INT DEFAULT 0,
    per_user_limit INT DEFAULT 1,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    applicable_categories VARCHAR(200),
    applicable_vehicles TEXT,
    status TINYINT DEFAULT 1 COMMENT '0-禁用, 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add vip_level_required to coupons
ALTER TABLE coupons
    ADD COLUMN vip_level_required TINYINT NOT NULL DEFAULT 0 COMMENT '0-none,1-gold,2-diamond';

-- User Coupons table
CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    order_id BIGINT,
    status TINYINT DEFAULT 0 COMMENT '0-未使用, 1-已使用, 2-已过期',
    used_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- admin issuance tracking
    issued_by_admin_id BIGINT,
    assigned_at DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_coupon (coupon_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Addresses table
CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    address VARCHAR(500) NOT NULL,
    is_default TINYINT(1) DEFAULT 0,
    address_type TINYINT DEFAULT 3 COMMENT '1-取车地址, 2-还车地址, 3-通用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 售后订单表
CREATE TABLE IF NOT EXISTS after_sales_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    after_no VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(50),
    user_phone VARCHAR(20),
    type TINYINT NOT NULL COMMENT '1-报修,2-退款,3-投诉,4-其他',
    reason_code VARCHAR(50),
    description TEXT NOT NULL,
    expected_solution VARCHAR(200),
    refund_amount DECIMAL(10,2) DEFAULT 0.00,
    approved_refund_amount DECIMAL(10,2) DEFAULT 0.00,
    evidence_images TEXT,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-已关闭,1-待审核,2-已同意,3-已拒绝,4-已完成,5-退款中',
    audit_admin_id BIGINT,
    audit_admin_name VARCHAR(50),
    audit_time DATETIME,
    audit_remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_after_order (order_id),
    INDEX idx_after_user (user_id),
    INDEX idx_after_status (status),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Login security logs
CREATE TABLE IF NOT EXISTS login_security_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    account VARCHAR(100),
    ip VARCHAR(45),
    location VARCHAR(100),
    device VARCHAR(255),
    result VARCHAR(20) NOT NULL,
    message VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_login_user (user_id),
    INDEX idx_login_account (account),
    INDEX idx_login_result (result),
    INDEX idx_login_created (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Admin operation audit logs
CREATE TABLE IF NOT EXISTS admin_operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT,
    operator_name VARCHAR(50),
    operator_role VARCHAR(20),
    module VARCHAR(50),
    action VARCHAR(100),
    target VARCHAR(200),
    result VARCHAR(20),
    ip VARCHAR(45),
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_operator (operator_id),
    INDEX idx_admin_module (module),
    INDEX idx_admin_result (result),
    INDEX idx_admin_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Order event logs
CREATE TABLE IF NOT EXISTS order_event_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT,
    order_no VARCHAR(50),
    event_type VARCHAR(50),
    stage VARCHAR(50),
    operator_id BIGINT,
    operator_name VARCHAR(50),
    operator_role VARCHAR(20),
    message VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_event_order (order_id),
    INDEX idx_order_event_no (order_no),
    INDEX idx_order_event_type (event_type),
    INDEX idx_order_event_created (created_at),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Funds flow logs
CREATE TABLE IF NOT EXISTS funds_flow_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_no VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT,
    order_no VARCHAR(50),
    type VARCHAR(20),
    amount DECIMAL(12,2),
    channel VARCHAR(20),
    operator_id BIGINT,
    operator_name VARCHAR(50),
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_funds_flow_no (flow_no),
    INDEX idx_funds_order_no (order_no),
    INDEX idx_funds_type (type),
    INDEX idx_funds_channel (channel),
    INDEX idx_funds_created (created_at),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vehicle status change logs
CREATE TABLE IF NOT EXISTS vehicle_status_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT,
    vehicle_name VARCHAR(100),
    plate_number VARCHAR(20),
    from_status TINYINT,
    to_status TINYINT,
    operator_id BIGINT,
    operator_name VARCHAR(50),
    operator_role VARCHAR(20),
    remark VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_status_vehicle (vehicle_id),
    INDEX idx_vehicle_status_plate (plate_number),
    INDEX idx_vehicle_status_to (to_status),
    INDEX idx_vehicle_status_created (created_at),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample data

-- Admin user (password: admin123)
INSERT INTO users (username, password, phone, role, status, verification_status, invite_code) VALUES 
('admin', '240be518fabd2724ddb6f04eeb9d5b0adba7c8d0c1fe4f97d60c4a9e14f8e9fb', '13800000000', 'admin', 1, 2, 'ADMIN001');

-- Sample stores
INSERT INTO stores (name, address, city, district, latitude, longitude, phone, business_hours, status, vehicle_count, available_count) VALUES
('北京朝阳门店', '北京市朝阳区建国路88号', '北京', '朝阳区', 39.9087, 116.4074, '010-12345678', '08:00-22:00', 1, 10, 8),
('北京海淀门店', '北京市海淀区中关村大街1号', '北京', '海淀区', 39.9847, 116.3074, '010-87654321', '08:00-22:00', 1, 8, 6),
('上海浦东门店', '上海市浦东新区陆家嘴环路1000号', '上海', '浦东新区', 31.2304, 121.4737, '021-12345678', '08:00-22:00', 1, 12, 10),
('广州天河门店', '广州市天河区天河路385号', '广州', '天河区', 23.1291, 113.2644, '020-12345678', '08:00-22:00', 1, 10, 8),
('深圳福田门店', '深圳市福田区深南大道6008号', '深圳', '福田区', 22.5431, 114.0579, '0755-12345678', '08:00-22:00', 1, 8, 6);

-- Sample vehicles
INSERT INTO vehicles (vin, plate_number, brand, model, series, year, color, seats, fuel_type, transmission, category, store_id, daily_price, weekly_price, monthly_price, deposit, mileage, status, main_image, features, description, is_hot, is_new, no_deposit) VALUES
('LSVNV2182E2100001', '京A12345', '大众', '朗逸', '2024款 1.5L', 2024, '白色', 5, 'gasoline', 'auto', 'compact', 1, 158.00, 980.00, 3500.00, 3000.00, 5000, 1, '/images/vehicles/lavida.jpg', '["倒车雷达","GPS导航","蓝牙","USB接口"]', '大众朗逸，省油耐用，家庭出行首选', 1, 1, 0),
('LSVNV2182E2100002', '京A12346', '丰田', '卡罗拉', '2024款 1.8L', 2024, '银色', 5, 'hybrid', 'auto', 'compact', 1, 178.00, 1100.00, 4000.00, 3000.00, 3000, 1, '/images/vehicles/corolla.jpg', '["倒车影像","车道偏离预警","自动空调","CarPlay"]', '丰田卡罗拉混动版，节能环保，品质之选', 1, 1, 0),
('LSVNV2182E2100003', '京A12347', '本田', 'CR-V', '2024款 2.0L', 2024, '黑色', 5, 'gasoline', 'auto', 'suv', 1, 268.00, 1680.00, 6000.00, 5000.00, 8000, 1, '/images/vehicles/crv.jpg', '["全景天窗","360度全景影像","自适应巡航","真皮座椅"]', '本田CR-V，空间宽敞，动力充沛，越野与城市兼顾', 1, 0, 0),
('LSVNV2182E2100004', '京B12345', '奔驰', 'C级', '2024款 C200L', 2024, '黑色', 5, 'gasoline', 'auto', 'luxury', 2, 458.00, 2800.00, 10000.00, 10000.00, 2000, 1, '/images/vehicles/benzc.jpg', '["柏林之声音响","全液晶仪表","HUD抬头显示","座椅按摩"]', '奔驰C级，豪华商务座驾，彰显品位', 1, 1, 0),
('LSVNV2182E2100005', '京B12346', '宝马', '3系', '2024款 325Li', 2024, '白色', 5, 'gasoline', 'auto', 'luxury', 2, 468.00, 2900.00, 10500.00, 10000.00, 1500, 1, '/images/vehicles/bmw3.jpg', '["哈曼卡顿音响","全液晶仪表","无线充电","主动刹车"]', '宝马3系，运动与豪华的完美结合', 0, 1, 0),
('LSVNV2182E2100006', '沪A12345', '别克', 'GL8', '2024款 艾维亚', 2024, '黑色', 7, 'gasoline', 'auto', 'minivan', 3, 398.00, 2500.00, 9000.00, 6000.00, 10000, 1, '/images/vehicles/gl8.jpg', '["航空座椅","双侧电动门","后排娱乐系统","无钥匙进入"]', '别克GL8艾维亚，商务接待首选', 1, 0, 0),
('LSVNV2182E2100007', '沪A12346', '特斯拉', 'Model 3', '2024款 长续航', 2024, '红色', 5, 'electric', 'auto', 'midsize', 3, 328.00, 2000.00, 7500.00, 5000.00, 6000, 1, '/images/vehicles/model3.jpg', '["自动驾驶辅助","超充网络","OTA升级","玻璃车顶"]', '特斯拉Model 3，科技感满满，环保出行', 1, 1, 1),
('LSVNV2182E2100008', '粤A12345', '日产', '轩逸', '2024款 1.6L', 2024, '蓝色', 5, 'gasoline', 'auto', 'economy', 4, 128.00, 800.00, 3000.00, 2000.00, 15000, 1, '/images/vehicles/sylphy.jpg', '["倒车雷达","多功能方向盘","电动天窗","自动空调"]', '日产轩逸，舒适省油，经济实惠', 0, 0, 0),
('LSVNV2182E2100009', '粤A12346', '奥迪', 'A4L', '2024款 45TFSI', 2024, '灰色', 5, 'gasoline', 'auto', 'luxury', 4, 428.00, 2600.00, 9500.00, 8000.00, 4000, 1, '/images/vehicles/audia4.jpg', '["quattro四驱","B&O音响","虚拟座舱","矩阵大灯"]', '奥迪A4L，科技豪华，商务之选', 1, 0, 0),
('LSVNV2182E2100010', '粤B12345', '领克', '01', '2024款 2.0T', 2024, '橙色', 5, 'gasoline', 'auto', 'suv', 5, 238.00, 1500.00, 5500.00, 4000.00, 7000, 1, '/images/vehicles/lynk01.jpg', '["自动泊车","主动刹车","车道保持","全景影像"]', '领克01，年轻时尚，个性之选', 0, 1, 0);

-- Sample coupons
INSERT INTO coupons (code, name, description, type, min_amount, discount_amount, discount_rate, max_discount, total_count, used_count, per_user_limit, start_time, end_time, status) VALUES
('NEW50', '新用户专享50元券', '新用户首单立减50元', 1, 200.00, 50.00, NULL, NULL, 10000, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1),
('WEEKEND20', '周末8折券', '周末租车享8折优惠', 2, 100.00, NULL, 0.80, 200.00, 5000, 0, 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1),
('VIP100', 'VIP专享100元券', 'VIP用户专享100元优惠', 3, 500.00, 100.00, NULL, NULL, 1000, 0, 1, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), 1),
('LONG7', '周租优惠券', '租满7天立减100元', 1, 700.00, 100.00, NULL, NULL, 3000, 0, 3, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 1);

-- Insert test user (password: test123)
INSERT INTO users (username, password, phone, email, real_name, role, status, verification_status, invite_code, balance, points) VALUES 
('testuser', 'ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae', '13900000001', 'test@example.com', '测试用户', 'user', 1, 2, 'TEST0001', 1000.00, 500);

UPDATE users SET vip_level = CASE
  WHEN cumulative_spending > 5000 THEN 2
  WHEN cumulative_spending >= 1000 THEN 1
  ELSE 0
END;
