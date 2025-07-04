import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TarjetaRemotaImpl extends UnicastRemoteObject implements TarjetaRemota {
    private static final long serialVersionUID = 1L;
    private Map<String, Tarjeta> tarjetas;
    private Random random;

    public TarjetaRemotaImpl() throws RemoteException {
        super(0);
        tarjetas = new HashMap<>();
        random = new Random();
    }
    
    // Generar numero de tarjeta unico
    private String generarNumeroTarjeta() {
        String numero;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));}
            numero = sb.toString();
        } while (tarjetas.containsKey(numero));
        return numero;
    }

    @Override
    public synchronized String crearTarjeta(String nombreTitular, int pin, double limiteCredito) throws RemoteException {
        String numero = generarNumeroTarjeta();
        Tarjeta nuevaTarjeta = new Tarjeta(numero, nombreTitular, pin, limiteCredito);
        tarjetas.put(numero, nuevaTarjeta);
        return numero;
    }

    @Override
    public synchronized double consultarSaldo(String numeroTarjeta, int pin) throws RemoteException {
        Tarjeta tarjeta = tarjetas.get(numeroTarjeta);
        if (tarjeta == null || !tarjeta.validarPin(pin)) {
            throw new RemoteException("Tarjeta no encontrada o PIN incorrecto."); }
        return tarjeta.getSaldoDisponible();
    }

    @Override
    public synchronized boolean realizarCompra(String numeroTarjeta, int pin, double monto) throws RemoteException {
        Tarjeta tarjeta = tarjetas.get(numeroTarjeta);
        if (tarjeta == null || !tarjeta.validarPin(pin)) {
            throw new RemoteException("Tarjeta no encontrada o PIN incorrecto.");}
        return tarjeta.realizarCompra(monto);
    }

    @Override
    public synchronized boolean recargarSaldo(String numeroTarjeta, int pin, double monto) throws RemoteException {
        Tarjeta tarjeta = tarjetas.get(numeroTarjeta);
        if (tarjeta == null || !tarjeta.validarPin(pin)) {
            throw new RemoteException("Tarjeta no encontrada o PIN incorrecto."); }
        return tarjeta.recargarSaldo(monto);
    }

    @Override
    public synchronized List<Double> verHistorial(String numeroTarjeta, int pin) throws RemoteException {
        Tarjeta tarjeta = tarjetas.get(numeroTarjeta);
        if (tarjeta == null || !tarjeta.validarPin(pin)) {
            throw new RemoteException("Tarjeta no encontrada o PIN incorrecto."); }
        return tarjeta.getHistorialCompras();
    }

    @Override
    public synchronized String verTarjeta(String numeroTarjeta, int pin) throws RemoteException {
        Tarjeta tarjeta = tarjetas.get(numeroTarjeta);
        if (tarjeta == null || !tarjeta.validarPin(pin)) {
            throw new RemoteException("Tarjeta no encontrada o PIN incorrecto.");}
        return tarjeta.toString();
    }
}
