/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import core.Order.EProductType;
import java.io.IOException;

/**
 *
 * @author dario
 */
public interface IStock {
    public boolean available( EProductType type, int count )
            throws IOException;
}
