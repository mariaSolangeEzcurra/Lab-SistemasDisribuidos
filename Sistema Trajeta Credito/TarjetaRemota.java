import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TarjetaRemota extends Remote {
    
    String crearTarjeta(String nombreTitular, int pin, double limiteCredito) throws RemoteException;
    double consultarSaldo(String numeroTarjeta, int pin) throws RemoteException;
    boolean realizarCompra(String numeroTarjeta, int pin, double monto) throws RemoteException;
    boolean recargarSaldo(String numeroTarjeta, int pin, double monto) throws RemoteException;
    List<Double> verHistorial(String numeroTarjeta, int pin) throws RemoteException;
    String verTarjeta(String numeroTarjeta, int pin) throws RemoteException;
}
