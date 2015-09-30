/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.IOException;

/**
 *
 * @author dario
 */
public interface ILogger {
    public void error( String msg ) throws IOException;
    public void trace( String msg ) throws IOException;
}
