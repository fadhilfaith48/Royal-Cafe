-- ============================================
-- Database: faith_coffe
-- Jalankan script ini di MySQL / phpMyAdmin
-- ============================================

CREATE DATABASE IF NOT EXISTS faith_coffe;
USE faith_coffe;

-- Tabel users untuk login
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role ENUM('admin','kasir') DEFAULT 'kasir',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel menu
CREATE TABLE IF NOT EXISTS menu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nama_menu VARCHAR(100) NOT NULL UNIQUE,
    kategori VARCHAR(50),
    harga INT NOT NULL,
    stok INT DEFAULT 0,
    gambar VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel transaksi (header)
CREATE TABLE IF NOT EXISTS transaksi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    kode_transaksi VARCHAR(20) NOT NULL UNIQUE,
    kasir VARCHAR(50),
    subtotal INT DEFAULT 0,
    pajak INT DEFAULT 0,
    total INT DEFAULT 0,
    bayar BIGINT DEFAULT 0,
    kembalian BIGINT DEFAULT 0,
    tanggal TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel detail transaksi
CREATE TABLE IF NOT EXISTS detail_transaksi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    kode_transaksi VARCHAR(20) NOT NULL,
    nama_menu VARCHAR(100),
    harga INT,
    qty INT,
    subtotal INT,
    FOREIGN KEY (kode_transaksi) REFERENCES transaksi(kode_transaksi)
);

-- Default admin user (password: GANTI_PASSWORD_ADMIN)
INSERT INTO users (username, password, role) VALUES
('admin', 'GANTI_PASSWORD_ADMIN', 'admin'),
('kasir', 'GANTI_PASSWORD_KASIR', 'kasir')
ON DUPLICATE KEY UPDATE username=username;

-- ============================================
-- Data menu (nama & harga sesuai Dashboard.java)
-- Jika data lama ada, UPDATE harga & stoknya
-- ============================================

-- Hapus data lama yang nama-nya sudah diganti agar tidak konflik UNIQUE
DELETE FROM menu WHERE nama_menu IN ('Kopi Nadi Klasik', 'Nadi Platter');

INSERT INTO menu (nama_menu, kategori, harga, stok, gambar) VALUES
('Kopi Senja Klasik',        'Minuman', 25000, 70, 'kopisusu.jpg'),
('Americano',                'Minuman', 33000, 55, 'amerikano.jpg'),
('Matcha Zen',               'Minuman', 22000, 50, 'macha latte.jpg'),
('Strawberry Fields',        'Minuman', 24000, 50, 'karamel macchiato.jpg'),
('Midnight Mocha',           'Minuman', 54000, 55, 'latte art.jpg'),
('Twilight Latte',           'Minuman', 25000, 50, 'karamel macchiato.jpg'),
('Senja Platter',            'Makanan', 27000, 30, 'nasgor.jpg'),
('Classic Butter Croissant', 'Makanan', 18000, 30, 'rotilapis.jpg'),
('Choco Lava Cake',          'Makanan', 28000, 30, 'cuasong.jpg')
ON DUPLICATE KEY UPDATE
    harga   = VALUES(harga),
    stok    = VALUES(stok),
    gambar  = VALUES(gambar),
    kategori = VALUES(kategori);
