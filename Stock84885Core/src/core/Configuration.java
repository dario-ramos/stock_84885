/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Expects config file to be in the same dir as calling jar
 * @author dario
 */
public class Configuration {
    private static final String CONFIG_FILE_NAME = "config.properties";
    public static final String CUSTOMER_HOSTNAME = "customer_hostname";
    public static final String DELIVERY_EXCHANGE_NAME =
        "delivery_exchange_name";
    public static final String MAX_ORDER_DELIVERY_DELAY = 
        "max_order_delivery_delay";
    public static final String MAX_ORDER_GENERATION_DELAY =
        "max_order_generation_delay";
    public static final String MAX_ORDER_PRODUCT_COUNT =
        "max_order_product_count";
    public static final String ORDER_RECEIVER_HOSTNAME =
        "order_receiver_hostname";
    public static final String ORDERS_EXCHANGE_NAME = "orders_exchange_name";
    public static final String RESULTS_EXCHANGE_NAME = "results_exchange_name";
    public static final String SHIPPING_EXCHANGE_NAME =
        "shipping_exchange_name";
    public static final String SHIPPING_HOSTNAME = "shipping_hostname";
    private Properties _props = null;

    public Configuration() throws FileNotFoundException, IOException,
                                  UnsupportedEncodingException,
                                  URISyntaxException{
        _props = new Properties();
        String configFilePath = FileSystemUtils.getCurrentDir() +
                                File.separator + 
                                CONFIG_FILE_NAME;
        try (InputStream input =
             new FileInputStream(configFilePath)) {
            _props.load(input);
        }
    }
    
    public List<String> getMultivaluedProperty( String key ){
        String s = _props.getProperty(key);
        return Arrays.asList( s.split(",") );
    }

    public String getProperty( String key ){
        return _props.getProperty(key);
    }

}
