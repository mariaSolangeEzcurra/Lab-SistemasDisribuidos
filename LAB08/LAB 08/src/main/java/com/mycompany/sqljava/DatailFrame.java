/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.mycompany.sqljava;

import raven.datetime.DatePicker;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
/**
 *
 * @author marro
 */
public class DatailFrame extends javax.swing.JPanel {

    /**
     * Creates new form DatailFrame
     */
    
    // Mapas para almacenar componentes y metadatos
    private Map<String, JComponent> camposPorColumna;
    private Map<String, DatePicker> datePickersPorColumna;
    private Map<String, CConexion.ColumnMetadata> metadatosPorColumna;
    private List<String> nombresColumnas;
    private List<CConexion.ColumnMetadata> metadatosTabla;
    private String tablaActual;
    
    // Componentes dinámicos
    private JPanel panelCampos;
    private JScrollPane scrollPane;
    
    // Patrones de validación
    private static final Pattern PATTERN_EMAIL = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PATTERN_TELEFONO = Pattern.compile(
        "^[0-9+\\-\\s()]{7,15}$");
    private static final Pattern PATTERN_NUMERICO = Pattern.compile(
        "^[0-9]+$");
    private static final Pattern PATTERN_DECIMAL = Pattern.compile(
        "^[0-9]+(\\.[0-9]+)?$");
    
    public DatailFrame() {
        initComponents();
        initCustomComponents();
    }
    
    private void initCustomComponents() {
        camposPorColumna = new HashMap<>();
        datePickersPorColumna = new HashMap<>();
        metadatosPorColumna = new HashMap<>();
        nombresColumnas = new ArrayList<>();
        metadatosTabla = new ArrayList<>();
        
        // Configurar layout principal
        setLayout(new BorderLayout());
        removeAll();
        
        // Panel para los campos con scroll
        panelCampos = new JPanel();
        panelCampos.setLayout(new BoxLayout(panelCampos, BoxLayout.Y_AXIS));
        panelCampos.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        scrollPane = new JScrollPane(panelCampos);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Label de título
        JLabel tituloLabel = new JLabel("Detalles del Registro", JLabel.CENTER);
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 14));
        tituloLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(tituloLabel, BorderLayout.NORTH);
        
        revalidate();
        repaint();
    }
    

    public void generarCamposParaTabla(String nombreTabla) {
        if (nombreTabla == null || nombreTabla.trim().isEmpty()) {
            return;
        }
        
        this.tablaActual = nombreTabla;
        limpiarCampos();
        
        try {
            CConexion conexion = new CConexion();
            metadatosTabla = conexion.obtenerMetadatosColumnas(nombreTabla);
            
            if (!metadatosTabla.isEmpty()) {
                for (CConexion.ColumnMetadata metadata : metadatosTabla) {
                    crearCampoConMetadatos(metadata);
                    nombresColumnas.add(metadata.columnName);
                    metadatosPorColumna.put(metadata.columnName, metadata);
                }
            }
            
            revalidate();
            repaint();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al generar campos: " + e.getMessage());
        }
    }
    

    private void crearCampoConMetadatos(CConexion.ColumnMetadata metadata) {
        JPanel panelCampo = new JPanel(new BorderLayout(5, 5));
        panelCampo.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panelCampo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Label con información adicional
        String labelText = metadata.columnName;
        if (!metadata.isNullable) {
            labelText += " *"; // Indicar campo obligatorio
        }
        
        JLabel label = new JLabel(labelText + ":");
        label.setPreferredSize(new Dimension(120, 25));
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Color rojo para campos obligatorios
        if (!metadata.isNullable) {
            label.setForeground(new Color(139, 69, 19)); // Marrón para obligatorios
        }
        
        panelCampo.add(label, BorderLayout.WEST);
        
        // Componente según el tipo con validaciones
        JComponent componente = crearComponenteConValidaciones(metadata);
        panelCampo.add(componente, BorderLayout.CENTER);
        
        // Tooltip con información de validación
        String tooltip = construirTooltip(metadata);
        componente.setToolTipText(tooltip);
        
        // Guardar referencia
        camposPorColumna.put(metadata.columnName, componente);
        panelCampos.add(panelCampo);
    }
    
    /**
     * Crea componente con validaciones específicas
     */
    private JComponent crearComponenteConValidaciones(CConexion.ColumnMetadata metadata) {
        String tipo = metadata.dataType.toLowerCase();
        
        // Campos de fecha
        if (tipo.contains("date") || tipo.contains("timestamp") || tipo.contains("datetime")) {
            return crearCampoFecha(metadata);
        }
        // Campos de texto largo
        else if (tipo.contains("text") || tipo.contains("longtext")) {
            return crearCampoTextoLargo(metadata);
        }
        // Campos numéricos
        else if (tipo.contains("int") || tipo.contains("decimal") || tipo.contains("double") || 
                 tipo.contains("float") || tipo.contains("numeric")) {
            return crearCampoNumerico(metadata);
        }
        // Campos de texto normal
        else {
            return crearCampoTexto(metadata);
        }
    }
    
    /**
     * Crea campo de texto con validaciones
     */
    private JTextField crearCampoTexto(CConexion.ColumnMetadata metadata) {
        JTextField campo = new JTextField();
        campo.setPreferredSize(new Dimension(200, 25));
        
        // Configurar según metadatos
        if (metadata.isPrimaryKey) {
            campo.setBackground(new Color(240, 240, 240));
        }
        
        if (metadata.maxLength != null && metadata.maxLength > 0) {
            // Limitar caracteres
            campo.setDocument(new LimitedDocument(metadata.maxLength));
        }
        
        // Auto-incremento no editable
        if (metadata.isAutoIncrement) {
            campo.setEditable(false);
            campo.setText("(Auto)");
        }
        
        return campo;
    }
    
    /**
     * Crea campo numérico con validaciones
     */
    private JTextField crearCampoNumerico(CConexion.ColumnMetadata metadata) {
        JTextField campo = new JTextField();
        campo.setPreferredSize(new Dimension(200, 25));
        
        if (metadata.isPrimaryKey) {
            campo.setBackground(new Color(240, 240, 240));
        }
        
        if (metadata.isAutoIncrement) {
            campo.setEditable(false);
            campo.setText("(Auto)");
        } else {
            // Validación en tiempo real para números
            campo.setDocument(new NumericDocument(metadata));
        }
        
        return campo;
    }
    
    /**
     * Crea campo de texto largo
     */
    private JScrollPane crearCampoTextoLargo(CConexion.ColumnMetadata metadata) {
        JTextArea textArea = new JTextArea(3, 20);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        if (metadata.maxLength != null && metadata.maxLength > 0) {
            textArea.setDocument(new LimitedDocument(metadata.maxLength));
        }
        
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(200, 60));
        return scroll;
    }
    
    /**
     * Crea campo de fecha con validaciones
     */
    private JComponent crearCampoFecha(CConexion.ColumnMetadata metadata) {
        JPanel panelFecha = new JPanel(new BorderLayout());
        panelFecha.setPreferredSize(new Dimension(200, 25));
        
        JTextField campoTexto = new JTextField();
        campoTexto.setEditable(false);
        campoTexto.setText("");
        
        DatePicker datePicker = new DatePicker();
        datePicker.setDateSelectionMode(DatePicker.DateSelectionMode.SINGLE_DATE_SELECTED);
        
        JButton botonFecha = new JButton("📅");
        botonFecha.setPreferredSize(new Dimension(30, 25));
        botonFecha.addActionListener(e -> datePicker.showPopup(botonFecha));
        
        datePicker.addDateSelectionListener(dateEvent -> {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            if (datePicker.getDateSelectionMode() == DatePicker.DateSelectionMode.SINGLE_DATE_SELECTED) {
                LocalDate date = datePicker.getSelectedDate();
                if (date != null) {
                    // Validar rango de fecha si es necesario
                    if (validarRangoFecha(date, metadata)) {
                        campoTexto.setText(df.format(date));
                        campoTexto.setBackground(Color.WHITE);
                    } else {
                        campoTexto.setBackground(new Color(255, 200, 200));
                        JOptionPane.showMessageDialog(this, 
                            "Fecha fuera del rango válido", 
                            "Error de Validación", 
                            JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    campoTexto.setText("");
                    campoTexto.setBackground(Color.WHITE);
                }
            }
        });
        
        panelFecha.add(campoTexto, BorderLayout.CENTER);
        panelFecha.add(botonFecha, BorderLayout.EAST);
        
        datePickersPorColumna.put(metadata.columnName, datePicker);
        return panelFecha;
    }
    
    /**
     * Valida rango de fechas
     */
    private boolean validarRangoFecha(LocalDate fecha, CConexion.ColumnMetadata metadata) {
        // Validaciones generales de fecha
        LocalDate hoy = LocalDate.now();
        LocalDate fechaMinima = LocalDate.of(1900, 1, 1);
        LocalDate fechaMaxima = LocalDate.of(2100, 12, 31);
        
        // Para fechas de nacimiento o similares
        if (metadata.columnName.toLowerCase().contains("nacimiento") || 
            metadata.columnName.toLowerCase().contains("birth")) {
            fechaMaxima = hoy.minusYears(1); // No futuro para fechas de nacimiento
        }
        
        // Para fechas de inicio de proyecto
        if (metadata.columnName.toLowerCase().contains("inicio") || 
            metadata.columnName.toLowerCase().contains("start")) {
            fechaMinima = hoy.minusYears(10); // No muy en el pasado
        }
        
        return fecha.isAfter(fechaMinima) && fecha.isBefore(fechaMaxima);
    }
    
    /**
     * Construye tooltip con información de validación
     */
    private String construirTooltip(CConexion.ColumnMetadata metadata) {
        StringBuilder tooltip = new StringBuilder("<html>");
        
        tooltip.append("<b>").append(metadata.columnName).append("</b><br>");
        tooltip.append("Tipo: ").append(metadata.dataType);
        
        if (metadata.maxLength != null) {
            tooltip.append("<br>Longitud máxima: ").append(metadata.maxLength);
        }
        
        if (metadata.numericPrecision != null) {
            tooltip.append("<br>Precisión: ").append(metadata.numericPrecision);
            if (metadata.numericScale != null) {
                tooltip.append(", Escala: ").append(metadata.numericScale);
            }
        }
        
        if (!metadata.isNullable) {
            tooltip.append("<br><font color='red'>Campo obligatorio (*)</font>");
        }
        
        if (metadata.isPrimaryKey) {
            tooltip.append("<br><font color='blue'>Clave primaria</font>");
        }
        
        if (metadata.isAutoIncrement) {
            tooltip.append("<br><font color='green'>Auto-incremento</font>");
        }
        
        tooltip.append("</html>");
        return tooltip.toString();
    }
    
    /**
     * Valida todos los campos según sus metadatos
     */
    public ValidationResult validarTodosLosCampos(boolean esCreacion) {
        ValidationResult resultado = new ValidationResult();
        
        for (CConexion.ColumnMetadata metadata : metadatosTabla) {
            String valor = getValorCampo(metadata.columnName);
            ValidationResult validacionCampo = validarCampo(metadata, valor, esCreacion);
            
            if (!validacionCampo.esValido) {
                resultado.esValido = false;
                resultado.errores.addAll(validacionCampo.errores);
            }
        }
        
        return resultado;
    }
    
    /**
     * Valida un campo específico
     */
    private ValidationResult validarCampo(CConexion.ColumnMetadata metadata, String valor, boolean esCreacion) {
        ValidationResult resultado = new ValidationResult();
        
        // Validar campo obligatorio
        if (!metadata.isNullable && (valor == null || valor.trim().isEmpty() || valor.equals("(Auto)"))) {
            if (!metadata.isAutoIncrement) {
                resultado.agregar("El campo '" + metadata.columnName + "' es obligatorio");
                return resultado;
            }
        }
        
        // Si está vacío y es nullable, es válido
        if (valor == null || valor.trim().isEmpty() || valor.equals("(Auto)")) {
            return resultado; // Válido
        }
        
        // Validar longitud máxima
        if (metadata.maxLength != null && valor.length() > metadata.maxLength) {
            resultado.agregar("El campo '" + metadata.columnName + "' no puede exceder " + 
                            metadata.maxLength + " caracteres");
        }
        
        // Validaciones específicas por tipo
        validarSegunTipo(metadata, valor, resultado);
        
        // Validar duplicados para claves únicas (si no es auto-increment)
        if (!metadata.isAutoIncrement && esCreacion
                && (metadata.isPrimaryKey || metadata.columnKey.equals("UNI"))) {

            // Skip validation for ingeniero_proyecto foreign keys
            if (tablaActual != null && tablaActual.equalsIgnoreCase("ingeniero_proyecto")
                    && (metadata.columnName.equalsIgnoreCase("IDIng")
                    || metadata.columnName.equalsIgnoreCase("IDProy"))) {
                // No validar duplicados para estas columnas en ingeniero_proyecto
            } else {
                validarDuplicados(metadata, valor, resultado, null);
            }
        }
        
        return resultado;
    }
    
    /**
     * Validaciones específicas según el tipo de dato
     */
    private void validarSegunTipo(CConexion.ColumnMetadata metadata, String valor, ValidationResult resultado) {
        String tipo = metadata.dataType.toLowerCase();
        
        // Validaciones numéricas
        if (tipo.contains("int") || tipo.contains("decimal") || tipo.contains("double") || 
            tipo.contains("float") || tipo.contains("numeric")) {
            validarNumerico(metadata, valor, resultado);
        }
        
        // Validaciones de fecha
        if (tipo.contains("date") || tipo.contains("timestamp") || tipo.contains("datetime")) {
            validarFecha(metadata, valor, resultado);
        }
        
        // Validaciones de email
        if (metadata.columnName.toLowerCase().contains("email") || 
            metadata.columnName.toLowerCase().contains("mail")) {
            if (!PATTERN_EMAIL.matcher(valor).matches()) {
                resultado.agregar("Formato de email inválido en '" + metadata.columnName + "'");
            }
        }
        
        // Validaciones de teléfono
        if (metadata.columnName.toLowerCase().contains("telefono") || 
            metadata.columnName.toLowerCase().contains("phone") ||
            metadata.columnName.toLowerCase().contains("tel")) {
            if (!PATTERN_TELEFONO.matcher(valor).matches()) {
                resultado.agregar("Formato de teléfono inválido en '" + metadata.columnName + "'");
            }
        }
    }
    
    /**
     * Valida campos numéricos
     */
    private void validarNumerico(CConexion.ColumnMetadata metadata, String valor, ValidationResult resultado) {
        try {
            if (metadata.dataType.toLowerCase().contains("int")) {
                long numero = Long.parseLong(valor);
                
                // Validar rangos según el tipo
                if (metadata.dataType.toLowerCase().contains("tinyint")) {
                    if (numero < -128 || numero > 127) {
                        resultado.agregar("Valor fuera de rango para TINYINT (-128 a 127)");
                    }
                } else if (metadata.dataType.toLowerCase().contains("smallint")) {
                    if (numero < -32768 || numero > 32767) {
                        resultado.agregar("Valor fuera de rango para SMALLINT (-32768 a 32767)");
                    }
                } else if (metadata.dataType.toLowerCase().contains("mediumint")) {
                    if (numero < -8388608 || numero > 8388607) {
                        resultado.agregar("Valor fuera de rango para MEDIUMINT");
                    }
                }
            } else {
                // Decimal/Float
                double numero = Double.parseDouble(valor);
                
                if (metadata.numericPrecision != null && metadata.numericScale != null) {
                    String[] partes = valor.split("\\.");
                    int enteros = partes[0].replace("-", "").length();
                    int decimales = partes.length > 1 ? partes[1].length() : 0;
                    
                    if (enteros > (metadata.numericPrecision - metadata.numericScale)) {
                        resultado.agregar("Demasiados dígitos enteros en '" + metadata.columnName + "'");
                    }
                    if (decimales > metadata.numericScale) {
                        resultado.agregar("Demasiados decimales en '" + metadata.columnName + "'");
                    }
                }
            }
        } catch (NumberFormatException e) {
            resultado.agregar("Formato numérico inválido en '" + metadata.columnName + "'");
        }
    }
    
    /**
     * Valida campos de fecha
     */
    private void validarFecha(CConexion.ColumnMetadata metadata, String valor, ValidationResult resultado) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate fecha = LocalDate.parse(valor, formatter);
            
            if (!validarRangoFecha(fecha, metadata)) {
                resultado.agregar("Fecha fuera del rango válido en '" + metadata.columnName + "'");
            }
        } catch (DateTimeParseException e) {
            resultado.agregar("Formato de fecha inválido en '" + metadata.columnName + "' (usar dd-MM-yyyy)");
        }
    }
    
    /**
     * Valida duplicados
     */
    private void validarDuplicados(CConexion.ColumnMetadata metadata, String valor, 
                                  ValidationResult resultado, String idExcluir) {
        CConexion conexion = new CConexion();
        if (conexion.existeValorEnColumna(tablaActual, metadata.columnName, valor, idExcluir)) {
            resultado.agregar("El valor '" + valor + "' ya existe en '" + metadata.columnName + "'");
        }
    }
    
    // Métodos existentes (getValorCampo, setValorCampo, etc.) - mantener igual
    public String getValorCampo(String nombreColumna) {
        JComponent componente = camposPorColumna.get(nombreColumna);
        if (componente == null) return "";
        
        if (componente instanceof JTextField) {
            return ((JTextField) componente).getText();
        } else if (componente instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) componente;
            JTextArea textArea = (JTextArea) scroll.getViewport().getView();
            return textArea.getText();
        } else if (componente instanceof JPanel) {
            JPanel panel = (JPanel) componente;
            Component[] components = panel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    return ((JTextField) comp).getText();
                }
            }
        }
        return "";
    }
    
    public void setValorCampo(String nombreColumna, String valor) {
        JComponent componente = camposPorColumna.get(nombreColumna);
        if (componente == null || valor == null) return;
        
        if (componente instanceof JTextField) {
            ((JTextField) componente).setText(valor);
        } else if (componente instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) componente;
            JTextArea textArea = (JTextArea) scroll.getViewport().getView();
            textArea.setText(valor);
        } else if (componente instanceof JPanel) {
            JPanel panel = (JPanel) componente;
            Component[] components = panel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    ((JTextField) comp).setText(valor);
                    
                    DatePicker datePicker = datePickersPorColumna.get(nombreColumna);
                    if (datePicker != null && !valor.trim().isEmpty()) {
                        try {
                            LocalDate fecha = null;
                            if (valor.contains("-")) {
                                if (valor.length() == 10) {
                                    if (valor.charAt(4) == '-') {
                                        fecha = LocalDate.parse(valor, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                                    } else {
                                        fecha = LocalDate.parse(valor, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                    }
                                }
                            }
                            if (fecha != null) {
                                datePicker.setSelectedDate(fecha);
                            }
                        } catch (Exception e) {
                            System.err.println("Error al parsear fecha: " + valor);
                        }
                    }
                    break;
                }
            }
        }
    }
    
    public String[] obtenerTodosLosValores() {
        List<String> valores = new ArrayList<>();
        for (String columna : nombresColumnas) {
            valores.add(getValorCampo(columna));
        }
        return valores.toArray(new String[0]);
    }
    
    public void establecerTodosLosValores(String[] valores) {
        if (valores == null) return;
        
        for (int i = 0; i < nombresColumnas.size() && i < valores.length; i++) {
            if (valores[i] != null) {
                setValorCampo(nombresColumnas.get(i), valores[i]);
            }
        }
    }
    
    public void limpiarTodosLosCampos() {
        for (String columna : nombresColumnas) {
            CConexion.ColumnMetadata metadata = metadatosPorColumna.get(columna);
            if (metadata != null && metadata.isAutoIncrement) {
                setValorCampo(columna, "(Auto)");
            } else {
                setValorCampo(columna, "");
            }
            
            DatePicker datePicker = datePickersPorColumna.get(columna);
            if (datePicker != null) {
                datePicker.clearSelectedDate();
            }
        }
    }
    
    public void habilitarCampos(boolean habilitar, boolean incluyeId) {
        for (int i = 0; i < nombresColumnas.size(); i++) {
            String columna = nombresColumnas.get(i);
            JComponent componente = camposPorColumna.get(columna);
            CConexion.ColumnMetadata metadata = metadatosPorColumna.get(columna);
            
            if (componente != null && metadata != null) {
                // No habilitar campos auto-increment
                if (metadata.isAutoIncrement) {
                    habilitarComponente(componente, false);
                } else if (metadata.isPrimaryKey && !incluyeId) {
                    habilitarComponente(componente, false);
                } else {
                    habilitarComponente(componente, habilitar);
                }
            }
        }
    }
    
    private void habilitarComponente(JComponent componente, boolean habilitar) {
        if (componente instanceof JTextField) {
            componente.setEnabled(habilitar);
        } else if (componente instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) componente;
            JTextArea textArea = (JTextArea) scroll.getViewport().getView();
            textArea.setEnabled(habilitar);
        } else if (componente instanceof JPanel) {
            JPanel panel = (JPanel) componente;
            Component[] components = panel.getComponents();
            for (Component comp : components) {
                comp.setEnabled(habilitar);
            }
        }
    }
    
    private void limpiarCampos() {
        if (panelCampos != null) {
            panelCampos.removeAll();
        }
        camposPorColumna.clear();
        datePickersPorColumna.clear();
        metadatosPorColumna.clear();
        nombresColumnas.clear();
        metadatosTabla.clear();
    }
    
    public String getTablaActual() {
        return tablaActual;
    }
    
    public List<String> getNombresColumnas() {
        return new ArrayList<>(nombresColumnas);
    }
    
    public List<CConexion.ColumnMetadata> getMetadatosTabla() {
        return new ArrayList<>(metadatosTabla);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 155, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
