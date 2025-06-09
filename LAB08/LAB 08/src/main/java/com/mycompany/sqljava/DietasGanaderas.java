/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.sqljava;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;

/**
 *
 * @author wbbtester
 */
public class DietasGanaderas {

    public static void main(String[] args) {
        /*
        CConexion objetoConexion = new CConexion();
        objetoConexion.estableceConexion();
        */
        try {
            FlatArcDarkOrangeIJTheme.setup();
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }
        InterfazDietas form = new InterfazDietas();
        form.setVisible(true);
        
    }
}

