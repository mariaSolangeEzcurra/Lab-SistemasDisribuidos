package com.mycompany.sqljava;


import java.sql.Connection;
import javax.swing.JOptionPane;
import java.sql.DriverManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author wbbtester
 */
public class CConexion {
    Connection conectar = null;
    String usuario="root";
    String contrasena="Yuuki200";
    String bd="empresadb";
    String ip="localhost";
    String puerto = "3306";
    
    
    String cadena = "jdbc:mysql://"+ip+":"+puerto+"/"+bd;

    
    
    public Connection estableceConexion(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conectar=DriverManager.getConnection(cadena, usuario, contrasena);
            System.out.println("Conexion establecidad");
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(null, "No se pudo establecer la conexion " + e.toString());
        }
        return conectar;
    }
    public String[] obtenerNombresColumnas(String tabla) { 
        List<String> columnNames = new ArrayList<>();
        
        
        try (Connection conexion = estableceConexion()) {
            String consulta = "SELECT * FROM `"+tabla+"` LIMIT 1" ; // Reemplaza 'nombre_tabla' por el nombre de tu tabla
            Statement statement = conexion.createStatement();
            ResultSet resultSet = statement.executeQuery(consulta);
            ResultSetMetaData metaData = resultSet.getMetaData();
            
            int cantidadColumnas = metaData.getColumnCount();
            System.out.println("Nombres de columnas:");
            
            for (int i = 1; i <= cantidadColumnas; i++) {
                String nombreColumna = metaData.getColumnName(i);
                columnNames.add(nombreColumna);
                System.out.println(nombreColumna);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al obtener columnas: " + e.getMessage());
        }
        return columnNames.toArray(String[]::new);
    }
    
        public List<ColumnMetadata> obtenerMetadatosColumnas(String nombreTabla) {
        List<ColumnMetadata> metadatos = new ArrayList<>();
        
        try (Connection conexion = estableceConexion()) {
            String consulta = """
                SELECT 
                    COLUMN_NAME,
                    DATA_TYPE,
                    CHARACTER_MAXIMUM_LENGTH,
                    NUMERIC_PRECISION,
                    NUMERIC_SCALE,
                    IS_NULLABLE,
                    COLUMN_DEFAULT,
                    EXTRA,
                    COLUMN_KEY,
                    COLUMN_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? 
                ORDER BY ORDINAL_POSITION
                """;
            
            PreparedStatement ps = conexion.prepareStatement(consulta);
            ps.setString(1, bd);
            ps.setString(2, nombreTabla);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                ColumnMetadata metadata = new ColumnMetadata();
                metadata.columnName = rs.getString("COLUMN_NAME");
                metadata.dataType = rs.getString("DATA_TYPE");
                metadata.maxLength = rs.getObject("CHARACTER_MAXIMUM_LENGTH") != null ? 
                                   rs.getInt("CHARACTER_MAXIMUM_LENGTH") : null;
                metadata.numericPrecision = rs.getObject("NUMERIC_PRECISION") != null ? 
                                           rs.getInt("NUMERIC_PRECISION") : null;
                metadata.numericScale = rs.getObject("NUMERIC_SCALE") != null ? 
                                       rs.getInt("NUMERIC_SCALE") : null;
                metadata.isNullable = "YES".equals(rs.getString("IS_NULLABLE"));
                metadata.columnDefault = rs.getString("COLUMN_DEFAULT");
                metadata.extra = rs.getString("EXTRA");
                metadata.columnKey = rs.getString("COLUMN_KEY");
                metadata.columnType = rs.getString("COLUMN_TYPE");
                metadata.isPrimaryKey = "PRI".equals(metadata.columnKey);
                metadata.isAutoIncrement = metadata.extra != null && 
                                          metadata.extra.toLowerCase().contains("auto_increment");
                
                metadatos.add(metadata);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener metadatos: " + e.getMessage());
        }
        
        return metadatos;
    }
    
    
    public List<String> obtenerTablas() {
        List<String> tablas = new ArrayList<>();
        try (Connection conexion = estableceConexion()) {
            DatabaseMetaData metaData = conexion.getMetaData();
            ResultSet resultSet = metaData.getTables(bd, null, "%", new String[]{"TABLE"});
            
            while (resultSet.next()) {
                tablas.add(resultSet.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al obtener tablas: " + e.getMessage());
        }
        return tablas;
    }
    
    public boolean existeTabla(String nombreTabla) {
        try (Connection conexion = estableceConexion()){
            DatabaseMetaData metaData = conexion.getMetaData();
            ResultSet resultSet = metaData.getTables(null,null,nombreTabla, null);
            return resultSet.next();
        }catch (SQLException e) {
            return false ;
        }
    }
    
    public boolean existeValorEnColumna(String tabla, String columna, String valor, String idExcluir) {
        try (Connection conexion = estableceConexion()) {
            String consulta = "SELECT COUNT(*) FROM `" + tabla + "` WHERE `" + columna + "` = ?";
            
            // Si estamos editando, excluir el registro actual
            if (idExcluir != null && !idExcluir.trim().isEmpty()) {
                // Asumir que la primera columna es la PK
                String[] columnas = obtenerNombresColumnas(tabla);
                if (columnas.length > 0) {
                    consulta += " AND `" + columnas[0] + "` != ?";
                }
            }
            
            PreparedStatement ps = conexion.prepareStatement(consulta);
            ps.setString(1, valor);
            if (idExcluir != null && !idExcluir.trim().isEmpty()) {
                ps.setString(2, idExcluir);
            }
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar duplicados: " + e.getMessage());
        }
        return false;
    }
    public static class ColumnMetadata {
        public String columnName;
        public String dataType;
        public Integer maxLength;
        public Integer numericPrecision;
        public Integer numericScale;
        public boolean isNullable;
        public String columnDefault;
        public String extra;
        public String columnKey;
        public String columnType;
        public boolean isPrimaryKey;
        public boolean isAutoIncrement;
        
        @Override
        public String toString() {
            return String.format("Column: %s, Type: %s, MaxLength: %s, Nullable: %s, PK: %s, AutoInc: %s", 
                               columnName, dataType, maxLength, isNullable, isPrimaryKey, isAutoIncrement);
        }
    }
    
    public void cerrarConexion() {
        try {
            if (conectar != null && !conectar.isClosed()) {
                conectar.close();
                System.out.println("Conexión cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
    
    
    public static void main(String[] args) {
        CConexion conexion = new CConexion();
        conexion.obtenerNombresColumnas("PAIS");
    }
    
}
