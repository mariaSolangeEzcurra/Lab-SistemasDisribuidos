import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class Servidor {
    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            
            // Obtener referencia al registro RMI en el puerto 5099 (asumiendo que ya está corriendo)
            Registry registry = LocateRegistry.createRegistry(5099);

            // Crear instancia del objeto remoto (debe implementar la interfaz remota)
            TarjetaRemotaImpl tarjeta = new TarjetaRemotaImpl();

            // Registrar el objeto remoto con un nombre accesible
            registry.rebind("Tarjeta", tarjeta);

            System.out.println("Servidor RMI listo y esperando conexiones en el puerto 5099...");
        } catch (RemoteException e) {
            System.err.println("Error en el servidor RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
