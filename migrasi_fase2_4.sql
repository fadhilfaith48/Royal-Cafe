-- ============================================
-- Migrasi Fase 2.4 — Konfigurasi Pajak & Diskon
-- Jalankan di MySQL / phpMyAdmin setelah update kode
-- Aman dijalankan berulang (idempotent-ish)
-- ============================================

USE faith_coffe;

-- 1. Tabel pengaturan (key-value) untuk konfigurasi aplikasi
CREATE TABLE IF NOT EXISTS pengaturan (
    `key` VARCHAR(50) NOT NULL PRIMARY KEY,
    `value` TEXT,
    `description` VARCHAR(200),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Data default (hanya insert jika belum ada)
INSERT INTO pengaturan (`key`, `value`, `description`) VALUES
('pajak_persen', '10', 'Persentase pajak (contoh: 10 = 10%)'),
('nama_toko', 'ROYAL CAFE', 'Nama toko untuk struk/laporan'),
('alamat_toko', 'Jl. Contoh No. 123, Kota', 'Alamat toko untuk struk'),
('footer_struk', 'Terima kasih atas kunjungan Anda!', 'Footer struk pembayaran')
ON DUPLICATE KEY UPDATE
    `value` = VALUES(`value`),
    `description` = VALUES(`description`);

-- 2. Tambah kolom diskon ke tabel transaksi (opsional, untuk fitur diskon)
-- Jalankan ini jika mau fitur diskon nominal/persen per transaksi
ALTER TABLE transaksi
    ADD COLUMN IF NOT EXISTS diskon_tipe ENUM('persen','nominal') DEFAULT 'persen',
    ADD COLUMN IF NOT EXISTS diskon_nilai DECIMAL(10,2) DEFAULT 0;

-- Catatan: MySQL 8 tidak suport ADD COLUMN IF NOT EXISTS.
-- Jika pakai MySQL 8, jalankan manual:
-- ALTER TABLE transaksi ADD COLUMN diskon_tipe ENUM('persen','nominal') DEFAULT 'persen';
-- ALTER TABLE transaksi ADD COLUMN diskon_nilai DECIMAL(10,2) DEFAULT 0;

-- 3. Index untuk performa filter tanggal (Fase 2.3)
CREATE INDEX IF NOT EXISTS idx_transaksi_tanggal ON transaksi(tanggal);

-- ============================================
-- Verifikasi
-- ============================================
-- SELECT * FROM pengaturan;
-- DESCRIBE transaksi;