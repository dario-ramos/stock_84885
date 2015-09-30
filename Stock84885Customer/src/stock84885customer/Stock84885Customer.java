/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885customer;

import core.Configuration;
import core.FileLogger;
import core.ILogger;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class Stock84885Customer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        ILogger logger = null;
        try{
            if( args.length != 1 ){
                System.err.println( "Invalid parameters. "
                                    + "Usage: Stock84885Customer <id>" );
            }
            int id = Integer.parseInt(args[0]);
            Configuration config = new Configuration();
            logger = new FileLogger("log.txt");
            CustomerController customer = new CustomerController(
                id,
                config,
                logger
            );
            customer.run();
        }catch( Exception ex ){
            if( logger != null ){
                try {
                    logger.error( ex.toString() );
                } catch (IOException ex1) {
                    System.err.println( ex1 );
                }
            }else{
                System.err.println( ex );
            }
        }
    }
    
}
