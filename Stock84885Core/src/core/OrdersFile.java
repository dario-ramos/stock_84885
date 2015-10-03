/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 *
 * @author dario
 */
public class OrdersFile implements IOrders{

    private final String _filePath;
    private final String _lockFilePath;
    private static final String NEWLINE = System.getProperty("line.separator");

    public OrdersFile( String filePath ){
        _filePath = filePath;
        _lockFilePath = filePath + "_lock";
    }

    @Override
    public void setState(Order order, Order.EOrderState state)
            throws IOException{
        Path path = Paths.get( _lockFilePath );
        try (FileChannel fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )){
            FileLock lock = fileChannel.lock();
            doSetState( order, state );
            lock.release();
            fileChannel.close();
        }
    }

    private String orderToFileLine( Order order, Order.EOrderState state ){
        return order.getID() + "|" +
               order.CustomerName + "|" +
               order.ProductType + "|" +
               order.Count + "|" +
               state.name() +
               NEWLINE;
    }

    private void doSetState(Order order, Order.EOrderState state)
            throws FileNotFoundException, IOException{
        //Save the file content to the String "input"
        BufferedReader file = new BufferedReader(new FileReader(_filePath));
        String line;
        String input = "";
        while ((line = file.readLine()) != null){
            input += line + NEWLINE;
        }
        file.close();
        //Replace state
        String orderId = order.getID();
        int iOrderId = input.indexOf( orderId );
        if( iOrderId <= -1 ){
            input += orderToFileLine( order, state );
        }else{
            int iEndOfLine = input.indexOf( NEWLINE, iOrderId );
            String lineToReplace = input.substring(iOrderId, iEndOfLine);
            input = input.replace(
                lineToReplace, orderToFileLine( order, state )
            );
        }
       //Write the new String with the replaced line OVER the same file
        FileOutputStream fileOut = new FileOutputStream(_filePath);
        fileOut.write(input.getBytes());
        fileOut.close();
    }

}
