package ROYALCAFE;

/**
 * Utilitas format angka Rupiah dengan pemisah ribuan titik.
 * Dipakai bersama agar tampilan nominal konsisten di seluruh aplikasi
 * (mis. 25000 -> "25.000").
 */
public final class Rupiah {

    private Rupiah() {
    }

    public static String format(long nilai) {
        return String.format("%,d", nilai).replace(',', '.');
    }
}
