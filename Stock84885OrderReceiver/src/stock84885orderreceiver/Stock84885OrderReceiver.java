/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885orderreceiver;

import core.Configuration;
import core.FileLogger;
import core.FileSystemUtils;
import core.ILogger;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author dario
 */
public class Stock84885OrderReceiver {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ILogger logger = null;
        try{
            if( args.length != 1 ){
                System.err.println( "Invalid parameters. "
                                    + "Usage: Stock84885OrderReceiver <id>" );
                return;
            }
            int id = Integer.parseInt(args[0]);
            Configuration config = new Configuration();
            logger = new FileLogger( FileSystemUtils.getCurrentDir() +
                                     File.separator +
                                     "log.txt" );
            OrderReceiverController orderReceiver =
                new OrderReceiverController(id, config, logger);
            orderReceiver.run();
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
