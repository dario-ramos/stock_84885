/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import core.Order.EOrderState;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author dario
 */
public interface IOrders {
    public List<Order> getOrdersByUserName( String userName )
        throws IOException;
    public void create( Order order, EOrderState initialState )
        throws IOException;
    public void setState( Order order, EOrderState state )
        throws IOException;
    public void setState( String orderID, EOrderState state )
        throws IOException;
}
