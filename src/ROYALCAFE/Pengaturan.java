package ROYALCAFE;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitas memuat & menyimpan pengaturan aplikasi dari tabel `pengaturan`.
 * Key-value sederhana, cocok untuk konfigurasi pajak, nama toko, dll.
 */
public final class Pengaturan {

    private static final Map<String, String> cache = new HashMap<>();
    private static boolean loaded = false;

    private Pengaturan() {
    }

    /** Muat semua pengaturan ke cache (dipanggil sekali saat startup). */
    public static void loadAll() {
        cache.clear();
        String sql = "SELECT `key`, `value` FROM pengaturan";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cache.put(rs.getString("key"), rs.getString("value"));
            }
            loaded = true;
        } catch (SQLException e) {
            System.err.println("Gagal memuat pengaturan: " + e.getMessage());
        }
    }

    /** Ambil nilai string (null jika tidak ada). */
    public static String get(String key) {
        if (!loaded) loadAll();
        return cache.get(key);
    }

    /** Ambil nilai int (default 0 jika tidak ada/error). */
    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        if (v == null) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Ambil nilai double (default 0.0 jika tidak ada/error). */
    public static double getDouble(String key, double defaultValue) {
        String v = get(key);
        if (v == null) return defaultValue;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Simpan/update satu pengaturan. */
    public static boolean set(String key, String value, String description) {
        String sql = "INSERT INTO pengaturan (`key`, `value`, `description`) VALUES (?,?,?) "
                   + "ON DUPLICATE KEY UPDATE `value`=VALUES(`value`), `description`=VALUES(`description`)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);
            ps.executeUpdate();
            // Update cache
            cache.put(key, value);
            return true;
        } catch (SQLException e) {
            System.err.println("Gagal simpan pengaturan " + key + ": " + e.getMessage());
            return false;
        }
    }

    /** Reload cache dari database (mis. setelah admin ubah lewat UI). */
    public static void reload() {
        loadAll();
    }
}