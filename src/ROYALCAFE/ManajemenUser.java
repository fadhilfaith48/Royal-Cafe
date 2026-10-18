package ROYALCAFE;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.ArrayList;

/**
 * Manajemen User - Royal Cafe
 * Hanya bisa diakses oleh admin.
 * Bisa tambah user, reset password, hapus user (dengan pengaman self-lock).
 */
public class ManajemenUser extends javax.swing.JFrame {

    private DefaultTableModel tableModel;
    private final String currentAdmin;
    private final Runnable onDataChanged;

    public ManajemenUser(String currentAdmin) {
        this(currentAdmin, null);
    }

    public ManajemenUser(String currentAdmin, Runnable onDataChanged) {
        this.currentAdmin = currentAdmin;
        this.onDataChanged = onDataChanged;
        initComponents();
        setLocationRelativeTo(null);
        initTable();
        loadUserData();
    }

    private void notifyChanged() {
        if (onDataChanged != null) onDataChanged.run();
    }

    private void initTable() {
        tableModel = new DefaultTableModel(
            new String[]{"Username", "Role", "Dibuat"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUser.setModel(tableModel);
        tblUser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUser.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillFormFromTable();
        });

        // Styling header
        tblUser.getTableHeader().setBackground(new java.awt.Color(51, 0, 255));
        tblUser.getTableHeader().setForeground(java.awt.Color.WHITE);
        tblUser.getTableHeader().setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 12));
        tblUser.setBackground(java.awt.Color.WHITE);
        tblUser.setGridColor(new java.awt.Color(180, 180, 220));
        tblUser.setFont(new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12));
        tblUser.setRowHeight(22);
    }

    private void loadUserData() {
        tableModel.setRowCount(0);
        String sql = "SELECT username, role, created_at FROM users ORDER BY username";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillFormFromTable() {
        int row = tblUser.getSelectedRow();
        if (row < 0) return;
        txtUsername.setText(tableModel.getValueAt(row, 0).toString());
        cmbRole.setSelectedItem(tableModel.getValueAt(row, 1).toString());
        txtPassword.setText("");
        txtKonfirmasi.setText("");
    }

    private void clearForm() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtKonfirmasi.setText("");
        cmbRole.setSelectedIndex(0);
        tblUser.clearSelection();
    }

    private boolean validateForm(boolean isNew) {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username tidak boleh kosong!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!username.matches("[A-Za-z0-9_]{3,50}")) {
            JOptionPane.showMessageDialog(this, "Username 3-50 karakter, hanya huruf, angka, underscore.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        char[] pwd = txtPassword.getPassword();
        char[] konf = txtKonfirmasi.getPassword();
        if (isNew || pwd.length > 0) {
            if (pwd.length < 4) {
                JOptionPane.showMessageDialog(this, "Password minimal 4 karakter.", "Validasi", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (!java.util.Arrays.equals(pwd, konf)) {
                JOptionPane.showMessageDialog(this, "Konfirmasi password tidak cocok.", "Validasi", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        return true;
    }

    private void tambahUser() {
        if (!validateForm(true)) return;
        String username = txtUsername.getText().trim();
        String role = cmbRole.getSelectedItem().toString();
        char[] pwd = txtPassword.getPassword();
        String hash = PasswordUtil.hash(pwd);
        java.util.Arrays.fill(pwd, '\0');

        String sql = "INSERT INTO users (username, password, role) VALUES (?,?,?) "
                   + "ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, role);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "User '" + username + "' berhasil disimpan.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadUserData();
                notifyChanged();
            } else {
                JOptionPane.showMessageDialog(this, "Tidak ada data tersimpan.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(this, "Username '" + username + "' sudah ada!", "Duplikat", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menyimpan user: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resetPassword() {
        int row = tblUser.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih user yang ingin direset password-nya!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String targetUser = tableModel.getValueAt(row, 0).toString();
        if (targetUser.equals(currentAdmin)) {
            JOptionPane.showMessageDialog(this, "Tidak bisa reset password user sendiri dari sini. Gunakan menu Ganti Password.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Dialog password baru
        JPasswordField pf1 = new JPasswordField();
        JPasswordField pf2 = new JPasswordField();
        Object[] msg = {"Password baru:", pf1, "Ulangi:", pf2};
        int opt = JOptionPane.showConfirmDialog(this, msg, "Reset Password: " + targetUser, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        char[] p1 = pf1.getPassword();
        char[] p2 = pf2.getPassword();
        if (p1.length < 4) {
            JOptionPane.showMessageDialog(this, "Password minimal 4 karakter.", "Validasi", JOptionPane.WARNING_MESSAGE);
            java.util.Arrays.fill(p1, '\0'); java.util.Arrays.fill(p2, '\0');
            return;
        }
        if (!java.util.Arrays.equals(p1, p2)) {
            JOptionPane.showMessageDialog(this, "Konfirmasi password tidak cocok.", "Validasi", JOptionPane.WARNING_MESSAGE);
            java.util.Arrays.fill(p1, '\0'); java.util.Arrays.fill(p2, '\0');
            return;
        }
        String hash = PasswordUtil.hash(p1);
        java.util.Arrays.fill(p1, '\0'); java.util.Arrays.fill(p2, '\0');

        String sql = "UPDATE users SET password=? WHERE username=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, targetUser);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Password user '" + targetUser + "' berhasil direset.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            loadUserData();
            notifyChanged();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal reset password: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hapusUser() {
        int row = tblUser.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih user yang ingin dihapus!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String targetUser = tableModel.getValueAt(row, 0).toString();
        String targetRole = tableModel.getValueAt(row, 1).toString();

        if (targetUser.equals(currentAdmin)) {
            JOptionPane.showMessageDialog(this, "Tidak bisa menghapus user yang sedang login!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Cek admin terakhir
        if ("admin".equalsIgnoreCase(targetRole)) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role='admin'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) <= 1) {
                    JOptionPane.showMessageDialog(this, "Tidak bisa menghapus admin terakhir!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Gagal cek admin: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin hapus user '" + targetUser + "'?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM users WHERE username=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetUser);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "User '" + targetUser + "' berhasil dihapus.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadUserData();
            notifyChanged();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal hapus user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanelHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        jPanelForm = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cmbRole = new javax.swing.JComboBox<>(new String[]{"kasir", "admin"});
        jLabel3 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        jLabel4 = new javax.swing.JLabel();
        txtKonfirmasi = new javax.swing.JPasswordField();
        btnTambah = new javax.swing.JButton();
        btnResetPwd = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnBersih = new javax.swing.JButton();
        jPanelTable = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblUser = new javax.swing.JTable();
        btnKembali = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Manajemen User - Royal Cafe");
        setPreferredSize(new java.awt.Dimension(700, 500));

        // Header
        jPanelHeader.setBackground(new java.awt.Color(51, 0, 255));
        lblTitle.setFont(new java.awt.Font("Bell MT", 3, 22));
        lblTitle.setForeground(java.awt.Color.WHITE);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setText("Manajemen User - Royal Cafe");
        jPanelHeader.add(lblTitle);

        // Form panel
        jPanelForm.setBackground(new java.awt.Color(204, 204, 204));
        jPanelForm.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 0, 255), 2),
            "Form User",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Bell MT", java.awt.Font.BOLD, 13),
            new java.awt.Color(51, 0, 255)
        ));

        java.awt.Font labelFont = new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13);
        jLabel1.setText("Username:"); jLabel1.setFont(labelFont);
        jLabel2.setText("Role:");      jLabel2.setFont(labelFont);
        jLabel3.setText("Password:");  jLabel3.setFont(labelFont);
        jLabel4.setText("Konfirmasi:"); jLabel4.setFont(labelFont);

        btnTambah.setText("Tambah");
        btnTambah.setBackground(new java.awt.Color(0, 153, 76));
        btnTambah.setForeground(java.awt.Color.WHITE);
        btnTambah.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnTambah.setFocusPainted(false);
        btnTambah.addActionListener(e -> tambahUser());

        btnResetPwd.setText("Reset Password");
        btnResetPwd.setBackground(new java.awt.Color(51, 0, 255));
        btnResetPwd.setForeground(java.awt.Color.WHITE);
        btnResetPwd.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnResetPwd.setFocusPainted(false);
        btnResetPwd.addActionListener(e -> resetPassword());

        btnHapus.setText("Hapus");
        btnHapus.setBackground(new java.awt.Color(204, 0, 0));
        btnHapus.setForeground(java.awt.Color.WHITE);
        btnHapus.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnHapus.setFocusPainted(false);
        btnHapus.addActionListener(e -> hapusUser());

        btnBersih.setText("Bersih");
        btnBersih.setBackground(new java.awt.Color(255, 140, 0));
        btnBersih.setForeground(java.awt.Color.WHITE);
        btnBersih.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnBersih.setFocusPainted(false);
        btnBersih.addActionListener(e -> clearForm());

        javax.swing.GroupLayout jPanelFormLayout = new javax.swing.GroupLayout(jPanelForm);
        jPanelForm.setLayout(jPanelFormLayout);
        jPanelFormLayout.setHorizontalGroup(jPanelFormLayout.createParallelGroup()
            .addGroup(jPanelFormLayout.createSequentialGroup().addGap(10)
                .addGroup(jPanelFormLayout.createParallelGroup()
                    .addComponent(jLabel1).addComponent(jLabel2)
                    .addComponent(jLabel3).addComponent(jLabel4))
                .addGap(10)
                .addGroup(jPanelFormLayout.createParallelGroup()
                    .addComponent(txtUsername, 200, 200, 200)
                    .addComponent(cmbRole, 200, 200, 200)
                    .addComponent(txtPassword, 200, 200, 200)
                    .addComponent(txtKonfirmasi, 200, 200, 200)
                    .addGroup(jPanelFormLayout.createSequentialGroup()
                        .addComponent(btnTambah).addGap(5).addComponent(btnResetPwd)
                        .addGap(5).addComponent(btnHapus).addGap(5).addComponent(btnBersih)))
                .addGap(10))
        );
        jPanelFormLayout.setVerticalGroup(jPanelFormLayout.createSequentialGroup().addGap(10)
            .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel1).addComponent(txtUsername)).addGap(8)
            .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2).addComponent(cmbRole)).addGap(8)
            .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel3).addComponent(txtPassword)).addGap(8)
            .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel4).addComponent(txtKonfirmasi)).addGap(15)
            .addGroup(jPanelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btnTambah).addComponent(btnResetPwd)
                .addComponent(btnHapus).addComponent(btnBersih))
            .addGap(10)
        );

        // Table panel
        jPanelTable.setBackground(new java.awt.Color(204, 204, 204));
        jPanelTable.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 0, 255), 2),
            "Daftar User",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Bell MT", java.awt.Font.BOLD, 13),
            new java.awt.Color(51, 0, 255)
        ));
        jScrollPane1.setViewportView(tblUser);

        btnKembali.setText("Kembali ke Dashboard");
        btnKembali.setBackground(new java.awt.Color(204, 0, 0));
        btnKembali.setForeground(java.awt.Color.WHITE);
        btnKembali.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnKembali.setFocusPainted(false);
        btnKembali.addActionListener(e -> this.dispose());

        javax.swing.GroupLayout jPanelTableLayout = new javax.swing.GroupLayout(jPanelTable);
        jPanelTable.setLayout(jPanelTableLayout);
        jPanelTableLayout.setHorizontalGroup(jPanelTableLayout.createParallelGroup()
            .addGroup(jPanelTableLayout.createSequentialGroup().addGap(10)
                .addComponent(jScrollPane1).addGap(10))
            .addGroup(jPanelTableLayout.createSequentialGroup().addGap(10)
                .addComponent(btnKembali).addGap(10))
        );
        jPanelTableLayout.setVerticalGroup(jPanelTableLayout.createSequentialGroup().addGap(8)
            .addComponent(jScrollPane1, 250, 250, Short.MAX_VALUE).addGap(8)
            .addComponent(btnKembali).addGap(8)
        );

        // Main layout
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(jPanelHeader)
            .addGroup(layout.createSequentialGroup().addGap(10)
                .addComponent(jPanelForm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10)
                .addComponent(jPanelTable).addGap(10))
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(jPanelHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(10)
            .addGroup(layout.createParallelGroup()
                .addComponent(jPanelForm)
                .addComponent(jPanelTable))
            .addGap(10)
        );

        pack();
    }

    // Variables declaration
    private javax.swing.JPanel jPanelHeader, jPanelForm, jPanelTable;
    private javax.swing.JLabel lblTitle, jLabel1, jLabel2, jLabel3, jLabel4;
    private javax.swing.JTextField txtUsername;
    private javax.swing.JComboBox<String> cmbRole;
    private javax.swing.JPasswordField txtPassword, txtKonfirmasi;
    private javax.swing.JButton btnTambah, btnResetPwd, btnHapus, btnBersih, btnKembali;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblUser;
}