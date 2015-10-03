/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import core.Order.EOrderState;
import java.io.IOException;

/**
 *
 * @author dario
 */
public interface IOrders {
    public void setState( Order order, EOrderState state ) throws IOException;
}
