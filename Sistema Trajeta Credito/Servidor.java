import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Servidor {
    public static void main(String[] args) {
        try {
            // Instancia del objeto remoto
            TarjetaRemotaImpl tarjeta = new TarjetaRemotaImpl();
            // RMI Registry en el puerto 1099
            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("RMI Registry iniciado en el puerto 1099.");
            } catch (RemoteException e) {
                System.out.println("RMI Registry ya estaba en ejecución.");
            }
            // Registrar el objeto remoto 
            Naming.rebind("Tarjeta", tarjeta);
            System.out.println("Servidor listo. Objeto 'TarjetaService' registrado.");
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
