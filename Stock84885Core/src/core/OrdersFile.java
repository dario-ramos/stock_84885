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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 *
 * @author dario
 */
public class OrdersFile implements IOrders{

    private final String _filePath;
    private final String _lockFilePath;

    public OrdersFile( String filePath ){
        _filePath = filePath;
        _lockFilePath = filePath + "_lock";
    }

    @Override
    public void create(Order order, Order.EOrderState initialState)
            throws IOException {
        Path path = Paths.get( _lockFilePath );
        try (FileChannel fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )){
            FileLock lock = fileChannel.lock();
            //Save the file content to the String "input"
            BufferedReader file = new BufferedReader(new FileReader(_filePath));
            String line;
            String input = "";
            while ((line = file.readLine()) != null){
                input += line + FileSystemUtils.NEWLINE;
            }
            file.close();
            //Add new order
            order.setID( UUID.randomUUID().toString() );
            input += orderToFileLine( order, initialState );
            //Write the new String with the replaced line OVER the same file
            FileOutputStream fileOut = new FileOutputStream(_filePath);
            fileOut.write(input.getBytes());
            fileOut.close();
            lock.release();
            fileChannel.close();
        }
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
    
    @Override
    public void setState(String orderID, Order.EOrderState state)
            throws IOException {
        Path path = Paths.get( _lockFilePath );
        try (FileChannel fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )){
            FileLock lock = fileChannel.lock();
            doSetState( orderID, state );
            lock.release();
            fileChannel.close();
        }
    }

    private String orderToFileLine( Order order, Order.EOrderState state ){
        return order.getID() + "|" +
               order.CustomerName + "|" +
               order.ProductType + "|" +
               order.Count + "|" +
               state.name();
    }

    private void doSetState(Order order, Order.EOrderState state)
            throws FileNotFoundException, IOException{
        //Save the file content to the String "input"
        BufferedReader file = new BufferedReader(new FileReader(_filePath));
        String line;
        String input = "";
        while ((line = file.readLine()) != null){
            input += line + FileSystemUtils.NEWLINE;
        }
        file.close();
        //Replace state
        String orderId = order.getID();
        int iOrderId = input.indexOf( orderId );
        if( iOrderId <= -1 ){
            throw new InvalidObjectException( "Order not found: " + orderId );
        }
        int iEndOfLine = input.indexOf(
            FileSystemUtils.NEWLINE, iOrderId
        );
        String lineToReplace = input.substring(iOrderId, iEndOfLine);
        input = input.replace(
            lineToReplace, orderToFileLine( order, state )
        );
        //Write the new String with the replaced line OVER the same file
        FileOutputStream fileOut = new FileOutputStream(_filePath);
        fileOut.write(input.getBytes());
        fileOut.close();
    }
    
    private void doSetState(String orderID, Order.EOrderState state)
            throws FileNotFoundException, IOException{
        //Save the file content to the String "input"
        BufferedReader file = new BufferedReader(new FileReader(_filePath));
        String line;
        String input = "";
        while ((line = file.readLine()) != null){
            input += line + FileSystemUtils.NEWLINE;
        }
        file.close();
        //Replace state
        int iOrderID = input.indexOf( orderID );
        if( iOrderID <= -1 ){
            throw new InvalidObjectException( "Order not found: " + orderID );
        }
        int iEndOfLine = input.indexOf(
            FileSystemUtils.NEWLINE, iOrderID
        );
        int iBegginingOfState = input.lastIndexOf("|", iEndOfLine) + 1;
        String lineToReplace = input.substring(iBegginingOfState, iEndOfLine);
        input = input.replace(
            lineToReplace, state.name()
        );
       //Write the new String with the replaced line OVER the same file
        FileOutputStream fileOut = new FileOutputStream(_filePath);
        fileOut.write(input.getBytes());
        fileOut.close();
    }

}
