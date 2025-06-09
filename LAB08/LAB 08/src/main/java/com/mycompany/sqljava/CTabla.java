/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sqljava;
import java.sql.*;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
/**
 *
 * @author wbbtester
 */
public class CTabla {
    
    /**
     * Muestra los datos de cualquier tabla en el JTable
     */
    public void Mostrar(JTable tabla, String nomTabla) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (Connection conn = new CConexion().estableceConexion()) {
            String consulta = "SELECT * FROM `" + nomTabla + "`";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(consulta);
            ResultSetMetaData metaData = rs.getMetaData();
            
            int columnCount = metaData.getColumnCount();
            
            // Limpiar el modelo antes de agregar columnas
            modelo.setRowCount(0);
            modelo.setColumnCount(0);
            
            // Agregar columnas al modelo
            for (int i = 1; i <= columnCount; i++) {
                modelo.addColumn(metaData.getColumnName(i));
            }
            
            // Agregar filas
            while (rs.next()) {
                Object[] fila = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    fila[i-1] = rs.getObject(i);
                }
                modelo.addRow(fila);
            }
            
            // Establecer el modelo en la tabla
            tabla.setModel(modelo);
            
            // Configurar el ordenamiento
            TableRowSorter<TableModel> OrdTabla = new TableRowSorter<>(modelo);
            tabla.setRowSorter(OrdTabla);
            
            System.out.println("Tabla " + nomTabla + " cargada con " + modelo.getRowCount() + 
                             " filas y " + modelo.getColumnCount() + " columnas");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al mostrar tabla " + nomTabla + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inserta un registro con validaciones completas
     * CORREGIDO para manejar ingeniero_proyecto sin restricciones
     */
    public boolean InsertarConValidaciones(String nombreTabla, List<CConexion.ColumnMetadata> metadatos, String[] valores) {
        CConexion conexion = new CConexion();
        
        try (Connection conn = conexion.estableceConexion()) {
            // Preparar listas para columnas y valores no auto-increment
            List<String> columnasParaInsertar = new ArrayList<>();
            List<String> valoresParaInsertar = new ArrayList<>();
            
            for (int i = 0; i < metadatos.size() && i < valores.length; i++) {
                CConexion.ColumnMetadata metadata = metadatos.get(i);
                String valor = valores[i];
                
                // Saltar campos auto-increment vacíos
                if (metadata.isAutoIncrement && (valor == null || valor.trim().isEmpty() || valor.equals("(Auto)"))) {
                    continue;
                }
                
                columnasParaInsertar.add(metadata.columnName);
                
                // Procesar valor según el tipo
                String valorProcesado = procesarValorParaBD(valor, metadata);
                valoresParaInsertar.add(valorProcesado);
            }
            
            if (columnasParaInsertar.isEmpty()) {
                throw new RuntimeException("No hay campos válidos para insertar");
            }
            
            // VALIDACIONES ESPECIALES PARA INGENIERO_PROYECTO
            if (nombreTabla.equalsIgnoreCase("ingeniero_proyecto") || 
                nombreTabla.equalsIgnoreCase("Ingeniero_Proyecto")) {
                validarIngenieroProyecto(conn, columnasParaInsertar, valoresParaInsertar, metadatos);
            }
            
            // Construir consulta INSERT
            StringBuilder consulta = new StringBuilder("INSERT INTO `" + nombreTabla + "` (");
            StringBuilder placeholders = new StringBuilder(" VALUES (");
            
            for (int i = 0; i < columnasParaInsertar.size(); i++) {
                consulta.append("`").append(columnasParaInsertar.get(i)).append("`");
                placeholders.append("?");
                
                if (i < columnasParaInsertar.size() - 1) {
                    consulta.append(", ");
                    placeholders.append(", ");
                }
            }
            
            consulta.append(")").append(placeholders).append(")");
            
            PreparedStatement ps = conn.prepareStatement(consulta.toString());
            
            // Asignar valores
            for (int i = 0; i < valoresParaInsertar.size(); i++) {
                String valor = valoresParaInsertar.get(i);
                CConexion.ColumnMetadata metadata = metadatos.get(obtenerIndiceMetadata(metadatos, columnasParaInsertar.get(i)));
                
                setParameterByType(ps, i + 1, valor, metadata);
            }
            
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Registro insertado exitosamente en " + nombreTabla);
                return true;
            }
            
        } catch (SQLException e) {
            String mensaje = analizarErrorSQL(e);
            throw new RuntimeException("Error al insertar en " + nombreTabla + ": " + mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al insertar en " + nombreTabla + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * NUEVO: Validaciones específicas para la tabla ingeniero_proyecto
     */
    private void validarIngenieroProyecto(Connection conn, List<String> columnas, List<String> valores, 
                                        List<CConexion.ColumnMetadata> metadatos) throws SQLException {
        String idIng = null;
        String idProy = null;
        String fechaAsignacion = null;
        
        // Extraer valores de IDIng, IDProy y Fecha_Asignacion
        for (int i = 0; i < columnas.size(); i++) {
            String columna = columnas.get(i);
            String valor = valores.get(i);
            
            if (columna.equalsIgnoreCase("IDIng")) {
                idIng = valor;
            } else if (columna.equalsIgnoreCase("IDProy")) {
                idProy = valor;
            } else if (columna.equalsIgnoreCase("Fecha_Asignacion")) {
                fechaAsignacion = valor;
            }
        }
        
        // Validar que IDIng existe en tabla Ingeniero
        if (idIng != null && !idIng.trim().isEmpty()) {
            if (!existeEnTabla(conn, "Ingeniero", "IDIng", idIng)) {
                throw new RuntimeException("El Ingeniero con ID " + idIng + " no existe. Debe crear el ingeniero primero.");
            }
        }
        
        // Validar que IDProy existe en tabla Proyecto
        if (idProy != null && !idProy.trim().isEmpty()) {
            if (!existeEnTabla(conn, "Proyecto", "IDProy", idProy)) {
                throw new RuntimeException("El Proyecto con ID " + idProy + " no existe. Debe crear el proyecto primero.");
            }
        }
        
        // Validar que no existe ya esta combinación IDIng-IDProy
        if (idIng != null && idProy != null && !idIng.trim().isEmpty() && !idProy.trim().isEmpty()) {
            String consultaDuplicado = "SELECT COUNT(*) FROM ingeniero_proyecto WHERE IDIng = ? AND IDProy = ?";
            try (PreparedStatement ps = conn.prepareStatement(consultaDuplicado)) {
                ps.setString(1, idIng);
                ps.setString(2, idProy);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new RuntimeException("El Ingeniero " + idIng + " ya está asignado al Proyecto " + idProy);
                }
            }
        }
        
        // Si no se proporciona fecha de asignación, usar la fecha actual
        if (fechaAsignacion == null || fechaAsignacion.trim().isEmpty()) {
            LocalDate hoy = LocalDate.now();
            String fechaHoy = hoy.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Agregar fecha actual a la lista
            boolean fechaEncontrada = false;
            for (int i = 0; i < columnas.size(); i++) {
                if (columnas.get(i).equalsIgnoreCase("Fecha_Asignacion")) {
                    valores.set(i, fechaHoy);
                    fechaEncontrada = true;
                    break;
                }
            }
            
            // Si la columna Fecha_Asignacion no estaba en la lista, agregarla
            if (!fechaEncontrada) {
                columnas.add("Fecha_Asignacion");
                valores.add(fechaHoy);
            }
        }
    }
    
    /**
     * Modifica un registro con validaciones completas
     * CORREGIDO para manejar ingeniero_proyecto
     */
    public boolean ModificarConValidaciones(String nombreTabla, List<CConexion.ColumnMetadata> metadatos, String[] valores) {
        CConexion conexion = new CConexion();
        
        try (Connection conn = conexion.estableceConexion()) {
            // Encontrar la clave primaria
            CConexion.ColumnMetadata pkMetadata = null;
            String valorPK = null;
            
            for (int i = 0; i < metadatos.size() && i < valores.length; i++) {
                if (metadatos.get(i).isPrimaryKey) {
                    pkMetadata = metadatos.get(i);
                    valorPK = valores[i];
                    break;
                }
            }
            
            if (pkMetadata == null || valorPK == null || valorPK.trim().isEmpty()) {
                throw new RuntimeException("No se encontró la clave primaria para actualizar");
            }
            
            // Construir consulta UPDATE (omitir PK y auto-increment)
            StringBuilder consulta = new StringBuilder("UPDATE `" + nombreTabla + "` SET ");
            List<String> valoresParaUpdate = new ArrayList<>();
            List<CConexion.ColumnMetadata> metadatosParaUpdate = new ArrayList<>();
            
            boolean primerCampo = true;
            for (int i = 0; i < metadatos.size() && i < valores.length; i++) {
                CConexion.ColumnMetadata metadata = metadatos.get(i);
                String valor = valores[i];
                
                // Saltar PK y auto-increment
                if (metadata.isPrimaryKey || metadata.isAutoIncrement) {
                    continue;
                }
                
                if (!primerCampo) {
                    consulta.append(", ");
                }
                
                consulta.append("`").append(metadata.columnName).append("` = ?");
                valoresParaUpdate.add(procesarValorParaBD(valor, metadata));
                metadatosParaUpdate.add(metadata);
                primerCampo = false;
            }
            
            consulta.append(" WHERE `").append(pkMetadata.columnName).append("` = ?");
            
            PreparedStatement ps = conn.prepareStatement(consulta.toString());
            
            // Asignar valores para UPDATE
            for (int i = 0; i < valoresParaUpdate.size(); i++) {
                setParameterByType(ps, i + 1, valoresParaUpdate.get(i), metadatosParaUpdate.get(i));
            }
            
            // Asignar valor de PK para WHERE
            setParameterByType(ps, valoresParaUpdate.size() + 1, valorPK, pkMetadata);
            
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Registro modificado exitosamente en " + nombreTabla);
                return true;
            } else {
                throw new RuntimeException("No se encontró el registro con ID: " + valorPK);
            }
            
        } catch (SQLException e) {
            String mensaje = analizarErrorSQL(e);
            throw new RuntimeException("Error al modificar en " + nombreTabla + ": " + mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al modificar en " + nombreTabla + ": " + e.getMessage());
        }
    }
    
    /**
     * Elimina un registro
     * CORREGIDO para manejar ingeniero_proyecto sin restricciones excesivas
     */
    public boolean EliminarConValidaciones(String nombreTabla, List<CConexion.ColumnMetadata> metadatos, String valorId) {
        CConexion conexion = new CConexion();
        
        try (Connection conn = conexion.estableceConexion()) {
            // Encontrar la clave primaria
            CConexion.ColumnMetadata pkMetadata = null;
            for (CConexion.ColumnMetadata metadata : metadatos) {
                if (metadata.isPrimaryKey) {
                    pkMetadata = metadata;
                    break;
                }
            }
            
            if (pkMetadata == null) {
                throw new RuntimeException("No se encontró la clave primaria");
            }
            
            // Para ingeniero_proyecto, permitir eliminación libre (es tabla de relación)
            if (!nombreTabla.equalsIgnoreCase("ingeniero_proyecto") && 
                !nombreTabla.equalsIgnoreCase("Ingeniero_Proyecto")) {
                
                // Verificar integridad referencial solo para tablas principales
                if (!verificarIntegridadReferencial(conn, nombreTabla, pkMetadata.columnName, valorId)) {
                    throw new RuntimeException("No se puede eliminar: existen registros relacionados");
                }
            }
            
            String consulta = "DELETE FROM `" + nombreTabla + "` WHERE `" + pkMetadata.columnName + "` = ?";
            PreparedStatement ps = conn.prepareStatement(consulta);
            setParameterByType(ps, 1, valorId, pkMetadata);
            
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Registro eliminado exitosamente de " + nombreTabla);
                return true;
            } else {
                throw new RuntimeException("No se encontró el registro con ID: " + valorId);
            }
            
        } catch (SQLException e) {
            String mensaje = analizarErrorSQL(e);
            throw new RuntimeException("Error al eliminar de " + nombreTabla + ": " + mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar de " + nombreTabla + ": " + e.getMessage());
        }
    }
    
    /**
     * Procesa valores según el tipo de dato para la base de datos
     */
    private String procesarValorParaBD(String valor, CConexion.ColumnMetadata metadata) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        
        String tipo = metadata.dataType.toLowerCase();
        valor = valor.trim();
        
        // Procesar fechas
        if (tipo.contains("date") || tipo.contains("timestamp") || tipo.contains("datetime")) {
            try {
                // Convertir de dd-MM-yyyy a yyyy-MM-dd
                if (valor.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    LocalDate fecha = LocalDate.parse(valor, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    return fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            } catch (Exception e) {
                System.err.println("Error al procesar fecha: " + valor);
            }
        }
        
        return valor;
    }
    
    /**
     * Establece parámetros según el tipo de dato
     */
    private void setParameterByType(PreparedStatement ps, int index, String valor, CConexion.ColumnMetadata metadata) 
            throws SQLException {
        if (valor == null) {
            ps.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        
        String tipo = metadata.dataType.toLowerCase();
        
        try {
            if (tipo.contains("int")) {
                ps.setLong(index, Long.parseLong(valor));
            } else if (tipo.contains("decimal") || tipo.contains("double") || tipo.contains("float")) {
                ps.setDouble(index, Double.parseDouble(valor));
            } else if (tipo.contains("date") || tipo.contains("timestamp") || tipo.contains("datetime")) {
                if (valor.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    ps.setDate(index, Date.valueOf(valor));
                } else {
                    ps.setString(index, valor);
                }
            } else {
                ps.setString(index, valor);
            }
        } catch (NumberFormatException e) {
            ps.setString(index, valor); // Fallback a string
        }
    }
    
    /**
     * Verifica integridad referencial antes de eliminar
     * CORREGIDO: No aplicar restricciones a ingeniero_proyecto
     */
    private boolean verificarIntegridadReferencial(Connection conn, String tabla, String columna, String valor) {
        // Para ingeniero_proyecto, siempre permitir eliminación
        if (tabla.equalsIgnoreCase("ingeniero_proyecto") || 
                tabla.equalsIgnoreCase("Ingeniero_Proyecto")) {
            return true;
        }
        // Verificar relaciones conocidas para otras tablas
        if (tabla.equalsIgnoreCase("Departamento")) {
            return !existeEnTabla(conn, "Proyecto", "IDDpto", valor);
        } else if (tabla.equalsIgnoreCase("Proyecto")) {
            return !existeEnTabla(conn, "Ingeniero_Proyecto", "IDProy", valor);
        } else if (tabla.equalsIgnoreCase("Ingeniero")) {
            return !existeEnTabla(conn, "Ingeniero_Proyecto", "IDIng", valor);
        }
        return true; // Por defecto permitir eliminación
    }
    
    /**
     * Verifica si existe un valor en una tabla específica
     */
    private boolean existeEnTabla(Connection conn, String tabla, String columna, String valor) {
        try {
            String consulta = "SELECT COUNT(*) FROM `" + tabla + "` WHERE `" + columna + "` = ?";
            PreparedStatement ps = conn.prepareStatement(consulta);
            ps.setString(1, valor);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error verificando existencia: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Analiza errores SQL para dar mensajes más amigables
     */
    private String analizarErrorSQL(SQLException e) {
        String mensaje = e.getMessage().toLowerCase();
        
        if (mensaje.contains("duplicate entry")) {
            return "Ya existe un registro con esa combinación de valores";
        } else if (mensaje.contains("cannot be null")) {
            return "Hay campos obligatorios sin completar";
        } else if (mensaje.contains("foreign key constraint")) {
            return "Error de integridad: verifica que los IDs de Ingeniero y Proyecto existan";
        } else if (mensaje.contains("data too long")) {
            return "Algún campo excede la longitud máxima permitida";
        } else if (mensaje.contains("out of range")) {
            return "Algún valor numérico está fuera del rango permitido";
        } else if (mensaje.contains("incorrect date")) {
            return "Formato de fecha incorrecto";
        }
        
        return e.getMessage(); // Mensaje original si no se reconoce
    }
    
    /**
     * Busca el índice de metadatos por nombre de columna
     */
    private int obtenerIndiceMetadata(List<CConexion.ColumnMetadata> metadatos, String nombreColumna) {
        for (int i = 0; i < metadatos.size(); i++) {
            if (metadatos.get(i).columnName.equals(nombreColumna)) {
                return i;
            }
        }
        return -1;
    }
    
    // ===== MÉTODOS DE CONSULTAS ESPECIALES (sin cambios) =====
    
    public void ConsultarProyectosPorDepartamento(JTable tabla, String nombreDepartamento) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        modelo.setRowCount(0);
        modelo.setColumnCount(0);
        
        modelo.addColumn("ID Proyecto");
        modelo.addColumn("Nombre Proyecto");
        modelo.addColumn("Fecha Inicio");
        modelo.addColumn("Fecha Término");
        modelo.addColumn("Departamento");
        
        CConexion conexion = new CConexion();
        try (Connection conn = conexion.estableceConexion()) {
            String consulta = "SELECT p.IDProy, p.Nombre, p.Fec_Inicio, p.Fec_Termino, d.Nombre as Departamento " +
                             "FROM Proyecto p " +
                             "INNER JOIN Departamento d ON p.IDDpto = d.IDDpto " +
                             "WHERE d.Nombre = ?";
            
            PreparedStatement ps = conn.prepareStatement(consulta);
            ps.setString(1, nombreDepartamento);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = {
                    rs.getObject("IDProy"),
                    rs.getObject("Nombre"),
                    rs.getObject("Fec_Inicio"),
                    rs.getObject("Fec_Termino"),
                    rs.getObject("Departamento")
                };
                modelo.addRow(fila);
            }
            
            tabla.setModel(modelo);
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(modelo);
            tabla.setRowSorter(sorter);
            
            System.out.println("Proyectos del departamento " + nombreDepartamento + " cargados: " + modelo.getRowCount() + " proyectos");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error en consulta de proyectos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void ConsultarIngenierosPorProyecto(JTable tabla, String nombreProyecto) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        modelo.setRowCount(0);
        modelo.setColumnCount(0);
        
        modelo.addColumn("ID Ingeniero");
        modelo.addColumn("Especialidad");
        modelo.addColumn("Cargo");
        modelo.addColumn("Proyecto");
        modelo.addColumn("Fecha Asignación");
        
        CConexion conexion = new CConexion();
        try (Connection conn = conexion.estableceConexion()) {
            String consulta = "SELECT i.IDIng, i.Especialidad, i.Cargo, p.Nombre as Proyecto, ip.Fecha_Asignacion " +
                             "FROM Ingeniero i " +
                             "INNER JOIN Ingeniero_Proyecto ip ON i.IDIng = ip.IDIng " +
                             "INNER JOIN Proyecto p ON ip.IDProy = p.IDProy " +
                             "WHERE p.Nombre = ?";
            
            PreparedStatement ps = conn.prepareStatement(consulta);
            ps.setString(1, nombreProyecto);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Object[] fila = {
                    rs.getObject("IDIng"),
                    rs.getObject("Especialidad"),
                    rs.getObject("Cargo"),
                    rs.getObject("Proyecto"),
                    rs.getObject("Fecha_Asignacion")
                };
                modelo.addRow(fila);
            }
            
            tabla.setModel(modelo);
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(modelo);
            tabla.setRowSorter(sorter);
            
            System.out.println("Ingenieros del proyecto " + nombreProyecto + " cargados: " + modelo.getRowCount() + " ingenieros");
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error en consulta de ingenieros: " + e.getMessage());
            e.printStackTrace();
        }
    }
}