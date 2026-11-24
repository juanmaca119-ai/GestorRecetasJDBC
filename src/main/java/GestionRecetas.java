import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class GestionRecetas {
    private static final String SQL_MOSTRAR_TODAS = """
            SELECT id, nombre, categoria, tiempo_preparacion, dificultad, raciones
            FROM recetas
            """;

    private static final String SQL_BUSCAR_ID = """
            SELECT id, nombre, categoria, tiempo_preparacion, dificultad, raciones
            FROM recetas
            WHERE id = ?
            """;

    private static final String SQL_INSERTAR = """
            INSERT INTO recetas (nombre, categoria, tiempo_preparacion, dificultad, raciones)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_MODIFICAR = """
            UPDATE recetas
            SET tiempo_preparacion = ?, raciones = ?
            WHERE id = ?
            """;

    private static final String SQL_ELIMINAR = """
            DELETE FROM recetas
            WHERE id = ?
            """;

    private static final String SQL_ACTUALIZAR_ESTADISTICAS = """
            UPDATE estadisticas
            SET total_recetas = total_recetas + 1
            WHERE id = 1
            """;

    private static final String SQL_PROCEDIMIENTO_BUSCAR = "{CALL buscarReceta(?)}";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int opcion = -1;

        do {
            System.out.println("\nGESTOR DE RECETAS");
            System.out.println("1. Mostrar todas las recetas");
            System.out.println("2. Buscar receta por ID");
            System.out.println("3. Añadir nueva receta");
            System.out.println("4. Modificar tiempo y raciones de una receta");
            System.out.println("5. Eliminar receta");
            System.out.println("6. Añadir receta y actualizar estadísticas (Transacción)");
            System.out.println("7. Buscar receta por nombre (Procedimiento almacenado)");
            System.out.println("0. Salir");
            System.out.print("Elige una opción: ");

            opcion = sc.nextInt();
            sc.nextLine(); //salto de linea
            switch (opcion) {
                case 1:
                    mostrarTodasLasRecetas();
                    break;
                case 2:
                    System.out.print("Introduce el ID de la receta: ");
                    int idBuscar = sc.nextInt();
                    sc.nextLine();
                    buscarRecetaPorId(idBuscar);
                    break;
                case 3:
                    Receta nuevaReceta = pedirDatosReceta(sc);
                    anadirReceta(nuevaReceta);
                    break;
                case 4:
                    System.out.print("Introduce el ID de la receta a modificar: ");
                    int idModificar = sc.nextInt();
                    System.out.print("Nuevo tiempo de preparación (min): ");
                    int nuevoTiempo = sc.nextInt();
                    System.out.print("Nuevo número de raciones: ");
                    int nuevasRaciones = sc.nextInt();
                    sc.nextLine();
                    modificarReceta(idModificar, nuevoTiempo, nuevasRaciones);
                    break;
                case 5:
                    System.out.print("Introduce el ID de la receta a eliminar: ");
                    int idEliminar = sc.nextInt();
                    sc.nextLine();
                    eliminarReceta(idEliminar);
                    break;
                case 6:
                    Receta recetaTransaccion = pedirDatosReceta(sc);
                    anadirRecetaConTransaccion(recetaTransaccion);
                    break;
                case 7:
                    System.out.print("Introduce el nombre exacto de la receta: ");
                    String nombreBuscar = sc.nextLine();
                    buscarRecetaProcedimiento(nombreBuscar);
                    break;
                case 0:
                    System.out.println("Saliendo del gestor de recetas...");
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        } while (opcion != 0);

        sc.close();
    }

    //número 1: mostrar todas las recetas almacenadas
    public static void mostrarTodasLasRecetas() {
        try (Connection con = ConexionBD.getConexion();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_MOSTRAR_TODAS)) {

            System.out.println("\nLISTADO DE RECETAS");
            while (rs.next()) {
                Receta r = construirReceta(rs);
                System.out.println(r);
            }

        } catch (SQLException e) {
            mostrarErrorSQL(e);
        }
    }

    //número 2: buscar una receta mediante su identificador
    public static void buscarRecetaPorId(int id) {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement pstmt = con.prepareStatement(SQL_BUSCAR_ID)) {

            //asigna el parámetro
            pstmt.setInt(1, id);

            //ejecuta la consulta
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Receta r = construirReceta(rs);
                    System.out.println("Receta encontrada: " + r);
                } else {
                    System.out.println("Receta no encontrada.");
                }
            }

        } catch (SQLException e) {
            mostrarErrorSQL(e);
        }
    }

    //número 3: añadir una nueva receta
    public static void anadirReceta(Receta receta) {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement pstmt = con.prepareStatement(SQL_INSERTAR)) {

            //asigna valores a los parámetros
            pstmt.setString(1, receta.getNombre());
            pstmt.setString(2, receta.getCategoria());
            pstmt.setInt(3, receta.getTiempoPreparacion());
            pstmt.setString(4, receta.getDificultad());
            pstmt.setInt(5, receta.getRaciones());

            //ejecuta la inserción
            int filas = pstmt.executeUpdate();
            System.out.println("Filas insertadas: " + filas);

        } catch (SQLException e) {
            mostrarErrorSQL(e);
        }
    }

    //número 4: modificar tiempo de preparación y raciones
    public static void modificarReceta(int id, int nuevoTiempo, int nuevasRaciones) {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement pstmt = con.prepareStatement(SQL_MODIFICAR)) {

            pstmt.setInt(1, nuevoTiempo);
            pstmt.setInt(2, nuevasRaciones);
            pstmt.setInt(3, id);

            int filas = pstmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Receta modificada correctamente. Filas afectadas: " + filas);
            } else {
                System.out.println("No existe ninguna receta con el ID " + id);
            }

        } catch (SQLException e) {
            mostrarErrorSQL(e);
        }
    }

    //número 5: eliminar una receta del catálogo (PreparedStatement)
    public static void eliminarReceta(int id) {
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement pstmt = con.prepareStatement(SQL_ELIMINAR)) {

            pstmt.setInt(1, id);

            int filas = pstmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Receta eliminada correctamente. Filas afectadas: " + filas);
            } else {
                System.out.println("No existe ninguna receta con el ID " + id);
            }

        } catch (SQLException e) {
            mostrarErrorSQL(e);
        }
    }

    //número 6: añadir receta y actualizar tabla estadisticas en una misma transacción
    public static void anadirRecetaConTransaccion(Receta receta) {
        //declarados fuera del try para usarlos en catch y finally
        Connection con = null;
        PreparedStatement pstmtInsertar = null;
        PreparedStatement pstmtEstadisticas = null;

        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false);

            //primera operación: insertar la nueva receta
            pstmtInsertar = con.prepareStatement(SQL_INSERTAR);
            pstmtInsertar.setString(1, receta.getNombre());
            pstmtInsertar.setString(2, receta.getCategoria());
            pstmtInsertar.setInt(3, receta.getTiempoPreparacion());
            pstmtInsertar.setString(4, receta.getDificultad());
            pstmtInsertar.setInt(5, receta.getRaciones());
            pstmtInsertar.executeUpdate();

            //segunda operación: actualizar el contador en estadisticas
            pstmtEstadisticas = con.prepareStatement(SQL_ACTUALIZAR_ESTADISTICAS);
            pstmtEstadisticas.executeUpdate();

            //confirma la transacción si ambas operaciones fueron correctas
            con.commit();
            System.out.println("Transacción confirmada: receta añadida y estadísticas actualizadas.");

        } catch (SQLException e) {
            mostrarErrorSQL(e);
            try {
                if (con != null) {
                    //cancela la transacción y deshace los cambios
                    con.rollback();
                    System.out.println("Transacción cancelada.");
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        } finally {
            try {
                //cierra las sentencias
                if (pstmtEstadisticas != null) {
                    pstmtEstadisticas.close();
                }
                if (pstmtInsertar != null) {
                    pstmtInsertar.close();
                }
                //cierra la conexión
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    //número 7: ejecutar procedimiento almacenado buscarReceta
    public static void buscarRecetaProcedimiento(String nombre) {
        try (Connection con = ConexionBD.getConexion();
             CallableStatement cstmt = con.prepareCall(SQL_PROCEDIMIENTO_BUSCAR)) {

            //asigna el parámetro de entrada
            cstmt.setString(1, nombre);

            //ejecuta el procedimiento y procesa el ResultSet
            try (ResultSet rs = cstmt.executeQuery()) {
                boolean encontrada = false;
                while (rs.next()) {
                    Receta r = construirReceta(rs);
                    System.out.println("Resultado procedimiento: " + r);
                    encontrada = true;
                }
                if (!encontrada) {
                    System.out.println("No se encontró ninguna receta con ese nombre.");
                }
            }

        } catch (SQLException e) {
            mostrarErrorSQL(e);
        }
    }

    //método auxiliar para pedir por teclado los datos de una receta
    private static Receta pedirDatosReceta(Scanner sc) {
        System.out.print("Nombre: ");
        String nombre = sc.nextLine();
        System.out.print("Categoría: ");
        String categoria = sc.nextLine();
        System.out.print("Tiempo de preparación (min): ");
        int tiempo = sc.nextInt();
        sc.nextLine();
        System.out.print("Dificultad: ");
        String dificultad = sc.nextLine();
        System.out.print("Raciones: ");
        int raciones = sc.nextInt();
        sc.nextLine();

        return new Receta(nombre, categoria, tiempo, dificultad, raciones);
    }

    //método auxiliar para convertir la fila actual del ResultSet en un objeto Receta
    private static Receta construirReceta(ResultSet rs) throws SQLException {
        return new Receta(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                rs.getInt("tiempo_preparacion"),
                rs.getString("dificultad"),
                rs.getInt("raciones")
        );
    }

    //método auxiliar para mostrar información detallada de SQLException
    private static void mostrarErrorSQL(SQLException e) {
        System.out.println("Se ha producido un error en la base de datos.");
        System.out.println("Mensaje: " + e.getMessage());
        System.out.println("SQLState: " + e.getSQLState());
        System.out.println("Código de error: " + e.getErrorCode());
    }
}