/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import core.Order.EOrderState;
import core.Order.EProductType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author dario
 */
public class OrdersFile implements IOrders{

    private final String filePathBase;
    private static final String FIELD_SEPARATOR = "|";

    public OrdersFile( String filePathBase ){
        this.filePathBase = filePathBase;
    }

    @Override
    public List<Order> getOrdersByUserName(String userName)
            throws IOException {
        List<Order> orders = new ArrayList<Order>();
        String prefixes = "0123456789abcdef";
        for( int i = 0; i<prefixes.length(); i++ ){
            String filePath = filePathBase + prefixes.charAt(i) + ".txt";
            String lockFilePath = filePath + "_lock";
            getOrdersByUserName( orders, lockFilePath, filePath, userName );
        }
        return orders;
    }

    @Override
    public void create(Order order, Order.EOrderState initialState)
            throws IOException {
        order.setID( UUID.randomUUID().toString() );
        String filePath = getFilePath( order );
        String lockFilePath = getLockFilePath( order );
        Path path = Paths.get( lockFilePath );
        FileLock lock = null;
        FileChannel fileChannel = null;
        try{
            fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND );
            lock = fileChannel.lock();
            //Save the file content to the String "input"
            BufferedReader file = new BufferedReader(new FileReader(filePath));
            String line;
            String input = "";
            while ((line = file.readLine()) != null){
                input += line + FileSystemUtils.NEWLINE;
            }
            file.close();
            //Add new order
            order.setState( initialState );
            input += orderToFileLine( order );
            //Write the new String with the replaced line OVER the same file
            try( FileOutputStream fileOut = new FileOutputStream(filePath) ){
                fileOut.write(input.getBytes());
            }
        }finally{
            if( lock != null ){
                lock.release();
            }
            if( fileChannel != null ){
                fileChannel.close();
            }
        }
    }

    @Override
    public void setState(Order order, Order.EOrderState state)
            throws IOException{
        String lockFilePath = getLockFilePath(order);
        Path path = Paths.get( lockFilePath );
        FileLock lock = null;
        FileChannel fileChannel = null;
        try{
            fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND );
            lock = fileChannel.lock();
            doSetState( order, state );
        }finally{
            if( lock != null ){
                lock.release();
            }
            if( fileChannel != null ){
                fileChannel.close();
            }
        }
    }
    
    @Override
    public void setState(String orderID, Order.EOrderState state)
            throws IOException {
        String lockFilePath = getLockFilePath( orderID );
        Path path = Paths.get( lockFilePath );
        FileLock lock = null;
        FileChannel fileChannel = null;
        try{
            fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND );
            lock = fileChannel.lock();
            doSetState( orderID, state );
        }finally{
            if( lock != null ){
                lock.release();
            }
            if( fileChannel != null ){
                fileChannel.close();
            }
        }
    }

    private Order fileLineToOrder( String line ) throws InvalidObjectException{
        String[] fields = line.split( FIELD_SEPARATOR );
        if( fields.length != 5 ){
            throw new InvalidObjectException( "Invalid order entry: " + line );
        }
        Order order = new Order();
        order.setID(fields[0]);
        order.setCustomerName( fields[1] );
        order.setProductType( EProductType.valueOf( fields[2] ) );
        order.setCount( Integer.parseInt( fields[3] ) );
        order.setState( EOrderState.valueOf( fields[4] ) );
        return order;
    }

    private String getFilePath( Order order ){
        return getFilePath( order.getID() );
    }

    private String getFilePath( String orderID ){
        return filePathBase + orderID.charAt(0) + ".txt";
    }

    private String getLockFilePath( Order order ){
        return getFilePath(order) + "_lock";
    }

    private String getLockFilePath( String orderID ){
        return getFilePath(orderID) + "_lock";
    }

    private String orderToFileLine( Order order ){
        return order.getID() + FIELD_SEPARATOR +
               order.getCustomerName() + FIELD_SEPARATOR +
               order.getProductType() + FIELD_SEPARATOR +
               order.getCount() + FIELD_SEPARATOR +
               order.getState().name();
    }

    private void doSetState(Order order, Order.EOrderState state)
            throws FileNotFoundException, IOException{
        String filePath = getFilePath(order);
        //Save the file content to the String "input"
        String input = "";
        try( BufferedReader file = new BufferedReader(
                new FileReader(filePath)) ){
            String line;
            while ((line = file.readLine()) != null){
                input += line + FileSystemUtils.NEWLINE;
            }
        }
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
        order.setState( state );
        input = input.replace(
            lineToReplace, orderToFileLine( order )
        );
        //Write the new String with the replaced line OVER the same file
        try( FileOutputStream fileOut = new FileOutputStream(filePath) ){
            fileOut.write(input.getBytes());
        }
    }

    private void doSetState(String orderID, Order.EOrderState state)
            throws FileNotFoundException, IOException{
        String filePath = getFilePath( orderID );
        //Save the file content to the String "input"
        String input = "";
        try( BufferedReader file = new BufferedReader(
                new FileReader(filePath)) ){
            String line;
            while ((line = file.readLine()) != null){
                input += line + FileSystemUtils.NEWLINE;
            }
        }
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
        try( FileOutputStream fileOut = new FileOutputStream(filePath) ){
            fileOut.write(input.getBytes());
        }
    }

    private void getOrdersByUserName( List<Order> orders,
                                      String lockFilePath, String filePath,
                                      String userName )
            throws IOException{
        Path path = Paths.get( lockFilePath );
        FileLock lock = null;
        FileChannel fileChannel = null;
        try{
            fileChannel = FileChannel.open(
              path, StandardOpenOption.WRITE, StandardOpenOption.APPEND );
            lock = fileChannel.lock();
            //Save the file content to the String "input"
            BufferedReader file = new BufferedReader(new FileReader(filePath));
            String line;
            String input = "";
            while ((line = file.readLine()) != null){
                if( !line.startsWith(userName + FIELD_SEPARATOR) ){
                    continue;
                }
                orders.add( fileLineToOrder(line) );
            }
            file.close();
        }finally{
            if( lock != null ){
                lock.release();
            }
            if( fileChannel != null ){
                fileChannel.close();
            }
        }
    }

}
