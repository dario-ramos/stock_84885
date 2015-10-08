/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.SerializationUtils;

/**
 *
 * @author dario
 */
public class Order{

    public enum EOrderState{
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

    public String CustomerName;
    public EProductType ProductType;
    private String _id;
    public int Count;
    public EOrderState State;
    //private static long serialVersionUID = -2031969733962027974L;
    private static final String FIELD_SEPARATOR = "|";

    public Order(){
        _id = "";
    }

    //Had to do this because serialize broke down after adding a field
    //It gave "local class incompatible" error
    public byte[] serialize(){
        String s = CustomerName + FIELD_SEPARATOR +
                   ProductType.name() + FIELD_SEPARATOR +
                   _id + FIELD_SEPARATOR +
                   Integer.toString(Count) + FIELD_SEPARATOR +
                   State.name();
        return s.getBytes();
    }

    public String getID(){
        return _id;
    }

    public void setID( String id ){
        _id = id;
    }

    @Override
    public String toString(){
        return "Customer: " + CustomerName +
               ", Prod: " + ProductType +
               ", Count: " + Count; 
    }

    public static Order deserialize( byte[] bytes )
            throws UnsupportedEncodingException, InvalidObjectException{
        String s = new String( bytes, "UTF-8" );
        String[] fields = s.split( FIELD_SEPARATOR );
        if( fields.length != 5 ){
            throw new InvalidObjectException( "Bad order: " + s );
        }
        Order o = new Order();
        o.CustomerName = fields[0];
        o.ProductType = EProductType.valueOf(fields[1]);
        o._id = fields[2];
        o.Count = Integer.parseInt( fields[3] );
        o.State = EOrderState.valueOf(fields[4]);
        return o;
    }
}
