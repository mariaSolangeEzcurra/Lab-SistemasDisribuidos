import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.rmi.Naming;
import java.util.List;

public class ClienteGUI extends JFrame {
    private static final String RMI_URL = "rmi://localhost:5099/Tarjeta";
    private static final Dimension FIELD_SIZE = new Dimension(200, 25);
    private static final Font FONT_BOLD_16 = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BOLD_24 = new Font("Segoe UI", Font.BOLD, 24);

    private TarjetaRemota servicio;
    private JPanel panelContenido;

    public ClienteGUI() {
        inicializarServicio();
        inicializarVentana();
    }

    private void inicializarServicio() {
        try {
            servicio = (TarjetaRemota) Naming.lookup(RMI_URL);
        } catch (Exception e) {
            mostrarErrorFatal("Error conectando con el servicio RMI: " + e.getMessage());
        }
    }

    private void inicializarVentana() {
        setTitle("\uD83D\uDCB3 Sistema de Tarjetas de Crédito");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelBotones = crearPanelBotones();
        panelContenido = new JPanel(new BorderLayout());
        panelContenido.setBorder(new EmptyBorder(20, 20, 20, 20));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelBotones, panelContenido);
        splitPane.setDividerLocation(250);
        splitPane.setEnabled(false);

        add(splitPane);
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new GridLayout(7, 1, 10, 10));
        panel.setBorder(new EmptyBorder(20, 10, 20, 10));
        panel.setBackground(new Color(240, 240, 240));

        String[] opciones = {
            "1. Crear nueva tarjeta",
            "2. Consultar saldo",
            "3. Realizar compra",
            "4. Recargar saldo",
            "5. Ver historial de compras",
            "6. Ver datos de tarjeta",
            "7. Salir"
        };

        for (int i = 0; i < opciones.length; i++) {
            JButton boton = new JButton(opciones[i]);
            boton.setFocusPainted(false);
            final int opcion = i + 1;
            boton.addActionListener(e -> manejarOpcion(opcion));
            panel.add(boton);
        }

        return panel;
    }

    private void manejarOpcion(int opcion) {
        panelContenido.removeAll();

        switch (opcion) {
            case 1 -> mostrarFormularioCrearTarjeta();
            case 2 -> mostrarSaldo();
            case 3 -> mostrarFormularioCompra();
            case 4 -> mostrarFormularioRecarga();
            case 5 -> mostrarHistorial();
            case 6 -> mostrarDatosTarjeta();
            case 7 -> {
                JOptionPane.showMessageDialog(this, "Gracias por usar el sistema.");
                System.exit(0);
            }
        }

        panelContenido.revalidate();
        panelContenido.repaint();
    }

    private void mostrarFormularioCrearTarjeta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = crearGbc();

        JLabel lblNombre = new JLabel("Nombre del titular:");
        JTextField campoNombre = new JTextField();
        campoNombre.setPreferredSize(FIELD_SIZE);

        JLabel lblPin = new JLabel("PIN (4 dígitos):");
        JPasswordField campoPin = new JPasswordField();
        campoPin.setPreferredSize(FIELD_SIZE);

        JLabel lblLimite = new JLabel("Límite de crédito:");
        JTextField campoLimite = new JTextField();
        campoLimite.setPreferredSize(FIELD_SIZE);

        JButton btnCrear = new JButton("Crear tarjeta");
        btnCrear.setPreferredSize(new Dimension(150, 30));

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblNombre, gbc);
        gbc.gridx = 1;
        panel.add(campoNombre, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblPin, gbc);
        gbc.gridx = 1;
        panel.add(campoPin, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lblLimite, gbc);
        gbc.gridx = 1;
        panel.add(campoLimite, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnCrear, gbc);

        btnCrear.addActionListener(e -> {
            try {
                String nombre = campoNombre.getText().trim();
                String pinStr = new String(campoPin.getPassword()).trim();
                String limiteStr = campoLimite.getText().trim();

                if (nombre.isEmpty() || pinStr.isEmpty() || limiteStr.isEmpty()) {
                    mostrarError("Todos los campos son obligatorios.");
                    return;
                }

                if (!pinStr.matches("\\d{4}")) {
                    mostrarError("El PIN debe ser un número de 4 dígitos.");
                    return;
                }

                int pin = Integer.parseInt(pinStr);
                double limite = Double.parseDouble(limiteStr);

                if (limite <= 0) {
                    mostrarError("El límite de crédito debe ser mayor a cero.");
                    return;
                }

                String numeroTarjeta = servicio.crearTarjeta(nombre, pin, limite);
                copiarAlPortapapeles(numeroTarjeta);

                JTextField campoNumero = crearCampoNoEditable(numeroTarjeta);
                Object[] mensaje = {
                    "Tarjeta creada. El número ha sido copiado al portapapeles:",
                    campoNumero
                };

                JOptionPane.showMessageDialog(this, mensaje, "Tarjeta creada", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        panelContenido.add(panel, BorderLayout.CENTER);
    }

    private void mostrarSaldo() {
        realizarLogin((numero, pin) -> {
            try {
                double saldo = servicio.consultarSaldo(numero, pin);
                JLabel lbl = new JLabel("Saldo disponible: $" + saldo);
                lbl.setFont(FONT_BOLD_24);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                panelContenido.add(lbl, BorderLayout.CENTER);
            } catch (Exception e) {
                mostrarError("Error: " + e.getMessage());
            }
        });
    }

    private void mostrarFormularioCompra() {
        realizarLogin((numero, pin) -> {
            JPanel panel = crearFormularioMonto("Monto de compra:", "Realizar compra", monto -> {
                try {
                    boolean ok = servicio.realizarCompra(numero, pin, monto);
                    JOptionPane.showMessageDialog(this, ok ? "Compra realizada." : "Compra denegada.");
                } catch (Exception e) {
                    mostrarError("Error: " + e.getMessage());
                }
            });
            panelContenido.add(panel, BorderLayout.CENTER);
        });
    }

    private void mostrarFormularioRecarga() {
        realizarLogin((numero, pin) -> {
            JPanel panel = crearFormularioMonto("Monto a recargar:", "Recargar", monto -> {
                try {
                    boolean ok = servicio.recargarSaldo(numero, pin, monto);
                    JOptionPane.showMessageDialog(this, ok ? "Recarga exitosa." : "Error en la recarga.");
                } catch (Exception e) {
                    mostrarError("Error: " + e.getMessage());
                }
            });
            panelContenido.add(panel, BorderLayout.CENTER);
        });
    }

    private void mostrarHistorial() {
        realizarLogin((numero, pin) -> {
            try {
                List<Double> historial = servicio.verHistorial(numero, pin);
                JTextArea area = new JTextArea();
                area.setEditable(false);
                historial.forEach(d -> area.append("\uD83D\uDED2 $" + d + "\n"));
                panelContenido.add(new JScrollPane(area), BorderLayout.CENTER);
            } catch (Exception e) {
                mostrarError("Error: " + e.getMessage());
            }
        });
    }

    private void mostrarDatosTarjeta() {
        realizarLogin((numero, pin) -> {
            try {
                String datos = servicio.verTarjeta(numero, pin);
                JTextArea area = new JTextArea(datos);
                area.setEditable(false);
                panelContenido.add(new JScrollPane(area), BorderLayout.CENTER);
            } catch (Exception e) {
                mostrarError("Error: " + e.getMessage());
            }
        });
    }

    private void realizarLogin(LoginAccion accion) {
        JTextField campoNumero = new JTextField();
        JPasswordField campoPin = new JPasswordField();

        Object[] fields = {
            "Número de tarjeta:", campoNumero,
            "PIN:", campoPin
        };

        int resultado = JOptionPane.showConfirmDialog(this, fields, "Iniciar sesión", JOptionPane.OK_CANCEL_OPTION);
        if (resultado == JOptionPane.OK_OPTION) {
            try {
                String numero = campoNumero.getText().trim();
                int pin = Integer.parseInt(new String(campoPin.getPassword()).trim());
                servicio.consultarSaldo(numero, pin); // Validar credenciales
                accion.ejecutar(numero, pin);
            } catch (Exception e) {
                mostrarError("Acceso denegado: " + e.getMessage());
            }
        }
    }

    private GridBagConstraints crearGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        return gbc;
    }

    private JTextField crearCampoNoEditable(String texto) {
        JTextField campo = new JTextField(texto);
        campo.setEditable(false);
        campo.setBorder(null);
        campo.setFont(FONT_BOLD_16);
        return campo;
    }

    private void copiarAlPortapapeles(String texto) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(texto), null);
    }

    private JPanel crearFormularioMonto(String etiqueta, String textoBoton, MontoAccion accion) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = crearGbc();

        JLabel lblMonto = new JLabel(etiqueta);
        JTextField campoMonto = new JTextField(15);
        JButton btnAccion = new JButton(textoBoton);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblMonto, gbc);
        gbc.gridx = 1;
        panel.add(campoMonto, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(btnAccion, gbc);

        btnAccion.addActionListener(e -> {
            try {
                String texto = campoMonto.getText().trim();
                if (texto.isEmpty()) {
                    mostrarError("Debe ingresar un monto.");
                    return;
                }
                double monto = Double.parseDouble(texto);
                if (monto <= 0) {
                    mostrarError("El monto debe ser mayor a cero.");
                    return;
                }
                accion.ejecutar(monto);
            } catch (NumberFormatException nfe) {
                mostrarError("Monto inválido.");
            }
        });

        return panel;
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarErrorFatal(String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, "Error Fatal", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    @FunctionalInterface
    private interface MontoAccion {
        void ejecutar(double monto);
    }

    @FunctionalInterface
    private interface LoginAccion {
        void ejecutar(String numeroTarjeta, int pin);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("No se pudo aplicar Nimbus.");
        }

        SwingUtilities.invokeLater(() -> {
            ClienteGUI cliente = new ClienteGUI();
            cliente.setVisible(true);
        });
    }
}
