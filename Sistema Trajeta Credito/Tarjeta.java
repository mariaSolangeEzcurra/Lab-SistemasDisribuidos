import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Tarjeta implements Serializable {
    private static final long serialVersionUID = 1L;
    private String numero;
    private String nombreTitular;
    private int pin;
    private double limiteCredito;
    private double saldoDisponible;
    private List<Double> historialCompras;
    
    // Constructor
    public Tarjeta(String numero, String nombreTitular, int pin, double limiteCredito) {
        if (numero == null || !numero.matches("\\d{16}")) {
            throw new IllegalArgumentException("El número de la tarjeta debe tener exactamente 16 digitos.");}
        if (nombreTitular == null || nombreTitular.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del titular no puede estar vacio."); }
        if (pin < 1000 || pin > 9999) {
            throw new IllegalArgumentException("El PIN debe tener 4 digitos numericos."); }
        if (limiteCredito <= 0) {
            throw new IllegalArgumentException("El limite de credito debe ser mayor a cero."); }
        this.numero = numero;
        this.nombreTitular = nombreTitular;
        this.pin = pin;
        this.limiteCredito = limiteCredito;
        this.saldoDisponible = limiteCredito;
        this.historialCompras = new ArrayList<>();
    }

    // Getters
    public String getNumero() { return numero; }
    public String getNombreTitular() { return nombreTitular; }
    public int getPin() { return pin; }
    public double getLimiteCredito() { return limiteCredito; }
    public double getSaldoDisponible() { return saldoDisponible; }
    public List<Double> getHistorialCompras() { return historialCompras; }

    // Metodos de una tarjeta
    public boolean validarPin(int pinIngresado) {
        return this.pin == pinIngresado;
    }
    public boolean realizarCompra(double monto) {
        if (monto <= 0) {
            System.out.println("Monto invalido.");
            return false;}
        if (monto > saldoDisponible) {
            System.out.println("Saldo insuficiente.");
            return false; }
        saldoDisponible -= monto;
        historialCompras.add(monto);
        return true;
    }
    public boolean recargarSaldo(double monto) {
        if (monto <= 0) {
            System.out.println("Monto invalido.");
            return false;}
        if ((saldoDisponible + monto) > limiteCredito) {
            System.out.println("No se puede exceder el limite de credito.");
            return false;}
        saldoDisponible += monto;
        return true;
    }
    @Override
    public String toString() {
        return "Tarjeta{" +
                "Titular='" + nombreTitular + '\'' +
                ", Número='" + numero + '\'' +
                ", Limite=" + limiteCredito +
                ", Saldo Disponible=" + saldoDisponible +
                '}';
    }
}