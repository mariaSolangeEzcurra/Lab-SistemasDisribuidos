import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {
        try {
            TarjetaRemota servicio = (TarjetaRemota) Naming.lookup("rmi://localhost/TarjetaService");
            Scanner scanner = new Scanner(System.in);
            String numeroTarjeta = null;
            int pin = 0;

            while (true) {
                System.out.println("\n---- MENÚ ----");
                System.out.println("1. Crear nueva tarjeta");
                System.out.println("2. Consultar saldo");
                System.out.println("3. Realizar compra");
                System.out.println("4. Recargar saldo");
                System.out.println("5. Ver historial de compras");
                System.out.println("6. Ver datos de tarjeta");
                System.out.println("7. Salir");
                System.out.print("Seleccione una opción: ");
                int opcion = scanner.nextInt();

                switch (opcion) {
                    case 1:
                        scanner.nextLine();
                        System.out.print("Nombre del titular: ");
                        String nombre = scanner.nextLine();
                        System.out.print("PIN (4 digitos): ");
                        pin = scanner.nextInt();
                        System.out.print("Limite de credito: ");
                        double limite = scanner.nextDouble();
                        numeroTarjeta = servicio.crearTarjeta(nombre, pin, limite);
                        System.out.println("Tarjeta creada exitosamente. Número: " + numeroTarjeta);
                        break;

                    case 2:
                        if (!validarLogin(scanner, servicio)) break;
                        double saldo = servicio.consultarSaldo(numeroTarjeta, pin);
                        System.out.println("Saldo disponible: $" + saldo);
                        break;

                    case 3:
                        if (!validarLogin(scanner, servicio)) break;
                        System.out.print("Monto de la compra: ");
                        double montoCompra = scanner.nextDouble();
                        if (servicio.realizarCompra(numeroTarjeta, pin, montoCompra)) {
                            System.out.println("Compra realizada con éxito.");
                        } else {
                            System.out.println("No se pudo realizar la compra.");
                        }
                        break;

                    case 4:
                        if (!validarLogin(scanner, servicio)) break;
                        System.out.print("Monto a recargar: ");
                        double recarga = scanner.nextDouble();
                        if (servicio.recargarSaldo(numeroTarjeta, pin, recarga)) {
                            System.out.println("Saldo recargado exitosamente.");
                        } else {
                            System.out.println("No se pudo recargar el saldo.");
                        }
                        break;

                    case 5:
                        if (!validarLogin(scanner, servicio)) break;
                        List<Double> historial = servicio.verHistorial(numeroTarjeta, pin);
                        System.out.println("Historial de compras:");
                        for (Double compra : historial) {
                            System.out.println("- $" + compra);
                        }
                        break;

                    case 6:
                        if (!validarLogin(scanner, servicio)) break;
                        String datos = servicio.verTarjeta(numeroTarjeta, pin);
                        System.out.println("Información de la tarjeta: " + datos);
                        break;

                    case 7:
                        System.out.println("Saliendo...");
                        scanner.close();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Opción invalida.");
                        break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean validarLogin(Scanner scanner, TarjetaRemota servicio) {
        if (servicio == null) return false;
        scanner.nextLine(); 
        System.out.print("Ingrese numero de tarjeta: ");
        String numero = scanner.nextLine();
        System.out.print("Ingrese PIN: ");
        int pinIngresado = scanner.nextInt();

        try {
            servicio.consultarSaldo(numero, pinIngresado);
            System.setProperty("tarjeta.numero", numero);
            System.setProperty("tarjeta.pin", String.valueOf(pinIngresado));
            return true;
        } catch (Exception e) {
            System.out.println("Acceso denegado: " + e.getMessage());
            return false;
        }
    }
    private static String getNumeroTarjeta() {
        return System.getProperty("tarjeta.numero");
    }
    private static int getPinTarjeta() {
        return Integer.parseInt(System.getProperty("tarjeta.pin"));
    }
}
