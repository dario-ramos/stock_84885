/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885shipping;

import core.Configuration;
import core.ConsoleLogger;
import core.FileLogger;
import core.FileSystemUtils;
import core.ILogger;
import core.IOrders;
import core.OrdersFile;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class Stock84885Shipping {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ILogger logger = null;
        try{
            if( args.length != 1 ){
                System.err.println( "Invalid parameters. "
                                    + "Usage: Stock84885Shipping <id>" );
                return;
            }
            int id = Integer.parseInt(args[0]);
            String currDirPrefix = FileSystemUtils.getCurrentDir() +
                                   File.separator;
            Configuration config = new Configuration();
            IOrders orders = new OrdersFile( currDirPrefix + "orders_" );
            logger = new ConsoleLogger( currDirPrefix + "sh_console_lock.txt" );
            ShippingController shipping = new ShippingController(
                id, orders, config, logger
            );
            shipping.run();
        }catch( Exception ex ){
            if( logger != null ){
                try {
                    logger.error( ex.toString() );
                } catch (IOException ex1) {
                    System.err.println( ex1 );
                    ex1.printStackTrace(System.err);
                }
            }else{
                System.err.println( ex );
                ex.printStackTrace(System.err);
            }
        }
    }

}
