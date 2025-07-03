import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Servidor {
    public static void main(String[] args) {
        try {
            TarjetaRemotaImpl tarjetaService = new TarjetaRemotaImpl();
            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("RMI Registry iniciado en el puerto 1099.");
            } catch (RemoteException e) {
                System.out.println("RMI Registry ya estaba en ejecución.");
            }
            Naming.rebind("rmi://localhost/TarjetaService", tarjetaService);
            System.out.println("Servidor listo. Objeto 'TarjetaService' registrado.");
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
