/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.SerializationUtils;

/**
 *
 * @author dario
 */
public class Order implements Serializable{

    public static final String ORDER_REJECTED = "REJECTED";
    public static final String ORDER_APPROVED = "APPROVED";

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
    public int Count;

    public byte[] serialize(){
        return SerializationUtils.serialize(this);
    }

    @Override
    public String toString(){
        return "Customer: " + CustomerName +
               ", Prod: " + ProductType +
               ", Count: " + Count; 
    }

    public static Order deserialize( byte[] bytes ){
        return (Order) SerializationUtils.deserialize( bytes );
    }
}
