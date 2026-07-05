package koneksi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class koneksi {

    private static Connection conn;

    public static Connection getConnection() {
        if (conn == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");

                String url = "jdbc:mysql://localhost:3306/berjodohwedding";
                String user = "root";
                String password = "";

                conn = DriverManager.getConnection(url, user, password);
                System.out.println("Koneksi berhasil.");
            } catch (ClassNotFoundException e) {
                System.out.println("Driver MySQL tidak ditemukan.");
                e.printStackTrace();
            } catch (SQLException e) {
                System.out.println("Koneksi database gagal.");
                e.printStackTrace();
            }
        }
        return conn;
    }
}