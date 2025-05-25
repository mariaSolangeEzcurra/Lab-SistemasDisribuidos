/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/WebService.java to edit this template
 */
package compra.productos;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author LENOVO
 */
@WebService(serviceName = "compraproductos")
public class compraproductos {

    /**
     * This is a sample web service operation
     */
    @WebMethod(operationName = "hello")
    public String hello(@WebParam(name = "name") String txt) {
        return "Hello " + txt + " !";
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "compraProductos")
public String compraProductos(
    @WebParam(name = "CantidadPan") int CantidadPan,
    @WebParam(name = "CantidadQueso") int CantidadQueso,
    @WebParam(name = "CantidadPlatanos") int CantidadPlatanos,
    @WebParam(name = "CantidadNaranjas") int CantidadNaranjas) {

    String mensaje = "";
    double total = 0.0;

    // Validación de cantidades
    if (CantidadPan < 0 || CantidadQueso < 0 || CantidadPlatanos < 0 || CantidadNaranjas < 0) {
        mensaje = "Lo siento, ingrese una cantidad positiva.";
    } else {
        mensaje += "\n";
        mensaje += "Pan: " + CantidadPan + " unidades --> SubTotal: $" + (CantidadPan * 0.50) + "\n";
        total += CantidadPan * 0.50;

        mensaje += "Queso: " + CantidadQueso + " unidades --> SubTotal: $" + (CantidadQueso * 2.50) + "\n";
        total += CantidadQueso * 2.50;

        mensaje += "Plátanos: " + CantidadPlatanos + " unidades --> SubTotal: $" + (CantidadPlatanos * 0.40) + "\n";
        total += CantidadPlatanos * 0.40;

        mensaje += "Naranjas: " + CantidadNaranjas + " unidades --> SubTotal: $" + (CantidadNaranjas * 0.60) + "\n";
        total += CantidadNaranjas * 0.60;

        mensaje += "\nTOTAL A PAGAR: $" + String.format("%.2f", total);
    }

    return mensaje;
}


}
