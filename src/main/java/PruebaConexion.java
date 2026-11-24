import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class PruebaConexion {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/test";
        String usuario = "root";
        String contraseña = "root";
        try (Connection conexion = DriverManager.getConnection(url,
                usuario, contraseña)) {
            System.out.println("Conexión OK.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}