/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sqljava;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author marro
 */
public class ValidationResult {
    public boolean esValido;
    public List<String> errores;
    
    public ValidationResult() {
        this.esValido = true;
        this.errores = new ArrayList<>();
    }
    
    public void agregar(String error) {
        this.errores.add(error);
        this.esValido = false;
    }
    
    public String obtenerMensajeCompleto() {
        if (esValido) {
            return "Validación exitosa";
        }
        
        StringBuilder mensaje = new StringBuilder("Errores de validación:\n");
        for (int i = 0; i < errores.size(); i++) {
            mensaje.append("• ").append(errores.get(i));
            if (i < errores.size() - 1) {
                mensaje.append("\n");
            }
        }
        return mensaje.toString();
    }
}