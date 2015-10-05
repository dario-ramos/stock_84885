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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dario
 */
public class Stock84885Customer {

    static final String ORDER_CMD = "order";
    static final String QUERY_CMD = "query";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        ILogger logger = null;
        try{
            if( !argumentsAreValid( args ) ){
                return;
            }
            int id = Integer.parseInt(args[0]);
            Configuration config = new Configuration();
            logger = new FileLogger("log.txt");
            runCommand( args[1], id, config, logger );
        }catch( Exception ex ){
            if( logger != null ){
                try {
                    logger.error( ex.toString() );
                    logger.error( getStackTrace(ex) );
                } catch (IOException ex1) {
                    System.err.println( ex1 );
                }
            }else{
                System.err.println( ex );
            }
        }
    }

    private static boolean argumentsAreValid( String[] args ){
        if( args.length != 2 ){
            System.err.println( "Invalid parameters. " +
                                "Usage: Stock84885Customer <id> " +
                                "<" + ORDER_CMD + "/" + QUERY_CMD +">" );
            return false;
        }
        if( args[1] == null ){
            System.err.println( "Second argument cannot be null" );
            return false;
        }
        if( !args[1].equals(ORDER_CMD) && !args[1].equals(QUERY_CMD) ){
            System.err.println( "Invalid value for parameter 2." +
                                "Valid values: " + ORDER_CMD + ", " + 
                                QUERY_CMD );
            return false;
        }
        return true;
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static void runCommand( String cmd, int id,
                                    Configuration config, ILogger logger )
            throws IOException, TimeoutException, InterruptedException{
        if( cmd.equals(ORDER_CMD) ){
            CustomerController customer = new CustomerController(
                id,
                config,
                logger
            );
            customer.run();
        }else{
            CustomerQueryController queryController =
                new CustomerQueryController( id, config, logger );
            queryController.run();
        }
    }

}
