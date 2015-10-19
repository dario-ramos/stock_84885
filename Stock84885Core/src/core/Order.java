/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author dario
 */
public class Order{

    public enum EOrderState{
        UNDEFINED,
        RECEIVED,
        APPROVED,
        REJECTED,
        DELIVERED
    }

    public enum EProductType{
        TYPE_A,
        TYPE_B,
        TYPE_C,
        TYPE_D,
        TYPE_E,
        TYPE_F,
        TYPE_G;

        private static final List<EProductType> VALUES =
            Collections.unmodifiableList(Arrays.asList(values()));
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();

        public static EProductType randomProductType(){
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    private EOrderState state;
    private EProductType productType;
    private int count;
    private String customerName;
    private String _id;
    private static final String FIELD_SEPARATOR = "|";

    public Order(){
        _id = " ";
    }

    //Had to do this because serialize broke down after adding a field
    //It gave "local class incompatible" error
    public byte[] serialize(){
        String s = customerName + FIELD_SEPARATOR +
                   productType.name() + FIELD_SEPARATOR +
                   _id + FIELD_SEPARATOR +
                   Integer.toString(count) + FIELD_SEPARATOR +
                   state.name();
        return s.getBytes();
    }

    public EOrderState getState(){
        return state;
    }

    public EProductType getProductType(){
        return productType;
    }

    public int getCount(){
        return count;
    }

    public String getCustomerName(){
        return customerName;
    }

    public String getID(){
        return _id;
    }

    public void setCount( int count ){
        this.count = count;
    }

    public void setCustomerName( String customerName ){
        this.customerName = customerName;
    }

    public void setID( String id ){
        _id = id;
    }

    public void setProductType( EProductType productType ){
        this.productType = productType;
    }

    public void setState( EOrderState newVal ){
        state = newVal;
    }

    @Override
    public String toString(){
        return "Customer: " + customerName +
               ", Prod: " + productType +
               ", Count: " + count; 
    }

    public static Order deserialize( byte[] bytes )
            throws UnsupportedEncodingException, InvalidObjectException{
        String s = new String( bytes, "UTF-8" );
        String[] fields = s.split( FIELD_SEPARATOR );
        if( fields.length != 5 ){
            throw new InvalidObjectException( "Bad order: " + s );
        }
        Order o = new Order();
        o.customerName = fields[0];
        o.productType = EProductType.valueOf(fields[1]);
        o._id = fields[2];
        o.count = Integer.parseInt( fields[3] );
        o.state = EOrderState.valueOf(fields[4]);
        return o;
    }
}
