import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/recetario";
    private static final String USUARIO = "root";
    private static final String CONTRASENA = "root";

    // Método que usa GestionRecetas
    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
    }

    // Método main para probar la conexión directamente (estilo PruebaConexion)
    public static void main(String[] args) {
        try (Connection conexion = getConexion()) {
            System.out.println("Conexión OK.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}