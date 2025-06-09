/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sqljava;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
/**
 *
 * @author marro
 */
public class NumericDocument extends PlainDocument {
    private CConexion.ColumnMetadata metadata;
    
    public NumericDocument(CConexion.ColumnMetadata metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
        if (str == null) return;
        
        String currentText = getText(0, getLength());
        String newText = currentText.substring(0, offset) + str + currentText.substring(offset);
        
        if (esNumeroValido(newText)) {
            super.insertString(offset, str, attr);
        }
    }
    
    private boolean esNumeroValido(String text) {
        if (text.isEmpty()) return true;
        if (text.equals("-")) return true; // Permitir signo negativo
        
        try {
            String tipo = metadata.dataType.toLowerCase();
            
            if (tipo.contains("int")) {
                Long.parseLong(text);
            } else if (tipo.contains("decimal") || tipo.contains("double") || tipo.contains("float")) {
                Double.parseDouble(text);
                
                // Validar precisión y escala
                if (metadata.numericPrecision != null && metadata.numericScale != null) {
                    String[] partes = text.split("\\.");
                    int enteros = partes[0].replace("-", "").length();
                    int decimales = partes.length > 1 ? partes[1].length() : 0;
                    
                    if (enteros > (metadata.numericPrecision - metadata.numericScale)) {
                        return false;
                    }
                    if (decimales > metadata.numericScale) {
                        return false;
                    }
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
