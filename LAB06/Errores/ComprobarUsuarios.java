/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package errores;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 *
 * @author User
 */
@WebService(serviceName = "ComprobarUsuario")
public class ComprobarUsuarios {
    
    // Credenciales predefinidas (en un caso real deberían estar en base de datos)
    private static final String USUARIO_VALIDO = "admin";
    private static final String CONTRASENIA_VALIDA = "123456";
    
    @WebMethod(operationName = "Comprobar")
    public boolean Comprobar(@WebParam(name = "usuario") String user1, 
                           @WebParam(name = "contrasenia") String contra) {
        try {
            // Verificar que los parámetros no sean null
            if (user1 == null || contra == null) {
                return false;
            }
            
            // Comparar usando .equals() para strings
            if (USUARIO_VALIDO.equals(user1) && CONTRASENIA_VALIDA.equals(contra)) {
                return true;
            } else {
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error en la validación: " + e.getMessage());
            return false;
        }
    }
}