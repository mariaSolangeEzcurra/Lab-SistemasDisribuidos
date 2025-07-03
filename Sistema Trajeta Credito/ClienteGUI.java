import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.Naming;
import java.util.List;

public class ClienteGUI extends JFrame {
    private TarjetaRemota servicio;
    private JPanel panelContenido;
    private String numeroTarjeta;
    private int pin;

    public ClienteGUI() {
        try {
            servicio = (TarjetaRemota) Naming.lookup("rmi://localhost/Tarjeta");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error conectando con el servicio RMI: " + e.getMessage());
            System.exit(1);
        }

        setTitle("Bienvenido al sistema de tarjetas de Credito");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel izquierdo con botones
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(7, 1, 10, 10));

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
            int opcion = i + 1;
            boton.addActionListener(e -> manejarOpcion(opcion));
            panelBotones.add(boton);
        }

        // Panel de contenido
        panelContenido = new JPanel();
        panelContenido.setLayout(new BorderLayout());

        add(panelBotones, BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);
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
                JOptionPane.showMessageDialog(this, "Saliendo del sistema...");
                System.exit(0);
            }
        }
        panelContenido.revalidate();
        panelContenido.repaint();
    }
    private void mostrarFormularioCrearTarjeta() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));

        JTextField campoNombre = new JTextField();
        JTextField campoPin = new JTextField();
        JTextField campoLimite = new JTextField();

        panel.add(new JLabel("Nombre del titular:"));
        panel.add(campoNombre);
        panel.add(new JLabel("PIN (4 dígitos):"));
        panel.add(campoPin);
        panel.add(new JLabel("Límite de crédito:"));
        panel.add(campoLimite);

        JButton btnCrear = new JButton("Crear");
        btnCrear.addActionListener(e -> {
            try {
                String nombre = campoNombre.getText();
                int pin = Integer.parseInt(campoPin.getText());
                double limite = Double.parseDouble(campoLimite.getText());
                numeroTarjeta = servicio.crearTarjeta(nombre, pin, limite);
                this.pin = pin;

                JOptionPane.showMessageDialog(this, "Tarjeta creada. Número: " + numeroTarjeta);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel());
        panel.add(btnCrear);
        panelContenido.add(panel, BorderLayout.CENTER);
    }

    private boolean login() {
        JTextField campoNumero = new JTextField();
        JTextField campoPin = new JPasswordField();

        Object[] fields = {
            "Número de tarjeta:", campoNumero,
            "PIN:", campoPin
        };

        int resultado = JOptionPane.showConfirmDialog(this, fields, "Iniciar sesión", JOptionPane.OK_CANCEL_OPTION);
        if (resultado == JOptionPane.OK_OPTION) {
            try {
                numeroTarjeta = campoNumero.getText();
                pin = Integer.parseInt(campoPin.getText());
                servicio.consultarSaldo(numeroTarjeta, pin);
                return true;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Acceso denegado: " + e.getMessage());
            }
        }
        return false;
    }

    private void mostrarSaldo() {
        if (!login()) return;
        try {
            double saldo = servicio.consultarSaldo(numeroTarjeta, pin);
            panelContenido.add(new JLabel("Saldo disponible: $" + saldo), BorderLayout.CENTER);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void mostrarFormularioCompra() {
        if (!login()) return;
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField campoMonto = new JTextField();

        panel.add(new JLabel("Monto de compra:"));
        panel.add(campoMonto);

        JButton btnComprar = new JButton("Comprar");
        btnComprar.addActionListener(e -> {
            try {
                double monto = Double.parseDouble(campoMonto.getText());
                if (servicio.realizarCompra(numeroTarjeta, pin, monto)) {
                    JOptionPane.showMessageDialog(this, "Compra realizada.");
                } else {
                    JOptionPane.showMessageDialog(this, "Compra denegada.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel());
        panel.add(btnComprar);
        panelContenido.add(panel, BorderLayout.CENTER);
    }

    private void mostrarFormularioRecarga() {
        if (!login()) return;
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField campoMonto = new JTextField();

        panel.add(new JLabel("Monto a recargar:"));
        panel.add(campoMonto);

        JButton btnRecargar = new JButton("Recargar");
        btnRecargar.addActionListener(e -> {
            try {
                double monto = Double.parseDouble(campoMonto.getText());
                if (servicio.recargarSaldo(numeroTarjeta, pin, monto)) {
                    JOptionPane.showMessageDialog(this, "Recarga exitosa.");
                } else {
                    JOptionPane.showMessageDialog(this, "Error en la recarga.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        panel.add(new JLabel());
        panel.add(btnRecargar);
        panelContenido.add(panel, BorderLayout.CENTER);
    }

    private void mostrarHistorial() {
        if (!login()) return;
        try {
            List<Double> historial = servicio.verHistorial(numeroTarjeta, pin);
            JTextArea area = new JTextArea();
            area.setEditable(false);
            for (Double d : historial) {
                area.append("- $" + d + "\n");
            }
            panelContenido.add(new JScrollPane(area), BorderLayout.CENTER);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void mostrarDatosTarjeta() {
        if (!login()) return;
        try {
            String datos = servicio.verTarjeta(numeroTarjeta, pin);
            panelContenido.add(new JLabel("<html><pre>" + datos + "</pre></html>"), BorderLayout.CENTER);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteGUI cliente = new ClienteGUI();
            cliente.setVisible(true);
        });
    }
}