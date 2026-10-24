-- ============================================
-- Migrasi Fase 2.5 — Void / Refund Transaksi
-- Jalankan di MySQL / phpMyAdmin setelah update kode.
-- Aman dijalankan berulang (kolom dicek aplikasi juga otomatis).
-- ============================================

USE faith_coffe;

-- Tambah kolom status & informasi void ke tabel transaksi.
-- Catatan: MySQL 8 tidak mendukung ADD COLUMN IF NOT EXISTS.
-- Jika kolom sudah ada, perintah ini akan error "Duplicate column name"
-- dan boleh diabaikan (aplikasi juga mengecek otomatis via metadata).
ALTER TABLE transaksi
    ADD COLUMN status ENUM('selesai','void') DEFAULT 'selesai',
    ADD COLUMN alasan_void TEXT,
    ADD COLUMN void_by VARCHAR(50),
    ADD COLUMN void_at TIMESTAMP NULL;

-- Index untuk mempercepat filter status
CREATE INDEX idx_transaksi_status ON transaksi(status);

-- ============================================
-- Verifikasi
-- ============================================
-- DESCRIBE transaksi;
-- SELECT kode_transaksi, status, void_by FROM transaksi;
