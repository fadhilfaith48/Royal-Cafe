package ROYALCAFE;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * CLI untuk membuat user baru atau mereset password
 */
public final class PasswordTool {

    private static final String USAGE = "Usage: java -cp lib/mysql-connector-j-8.0.31.jar"
            + " ROYALCAFE.PasswordTool <username> <admin|kasir>";

    private PasswordTool() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            fail(USAGE);
            return;
        }

        String username = args[0].trim();
        String role = args[1].trim().toLowerCase();

        if (!username.matches("[A-Za-z0-9_]{3,50}")) {
            fail("Username harus 3-50 karakter dan hanya boleh huruf, angka, atau underscore.");
            return;
        }
        if (!role.equals("admin") && !role.equals("kasir")) {
            fail("Role harus 'admin' atau 'kasir'.");
            return;
        }

        char[] password = readPassword();
        if (password == null || password.length == 0) {
            fail("Password tidak boleh kosong.");
            return;
        }
        if (password.length < 4) {
            Arrays.fill(password, '\0');
            fail("Password minimal 4 karakter.");
            return;
        }

        String stored;
        try {
            stored = PasswordUtil.hash(password);
        } finally {
            Arrays.fill(password, '\0');
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, stored);
            ps.setString(3, role);
            ps.executeUpdate();
            System.out.println("User '" + username + "' dengan role '" + role + "' berhasil disimpan.");
            System.out.println("Hash: " + stored);
        } catch (SQLException e) {
            fail("Gagal terhubung / menulis ke database: " + firstLine(e.getMessage())
                    + "\nPastikan MySQL sudah jalan di XAMPP dan variabel DB_* sudah sesuai.");
        } catch (Exception e) {
            fail("Gagal menyimpan user: " + firstLine(e.getMessage()));
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "tidak diketahui";
        }
        int newline = message.indexOf('\n');
        return (newline > 0 ? message.substring(0, newline) : message).trim();
    }

    private static char[] readPassword() {
        Console console = System.console();
        if (console != null) {
            char[] first = console.readPassword("Password     : ");
            char[] second = console.readPassword("Ulangi       : ");
            if (first == null || second == null) {
                return null;
            }
            if (!Arrays.equals(first, second)) {
                Arrays.fill(first, '\0');
                Arrays.fill(second, '\0');
                fail("Password tidak cocok.");
            }
            Arrays.fill(second, '\0');
            return first;
        }

        System.err.println();
        System.err.println("PERINGATAN: console tidak tersedia, password akan terlihat saat diketik.");
        Scanner scanner = new Scanner(System.in);
        System.err.print("Password     : ");
        String first = scanner.nextLine();
        System.err.print("Ulangi       : ");
        String second = scanner.nextLine();
        scanner.close();
        if (!first.equals(second)) {
            fail("Password tidak cocok.");
        }
        return first.toCharArray();
    }

    private static void fail(String message) {
        System.err.println("Error: " + message);
        System.exit(1);
    }
}