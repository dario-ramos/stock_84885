/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885customer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import core.Configuration;
import core.ILogger;
import core.Order;
import core.Order.EProductType;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dario
 */
public class CustomerController {

    private final ILogger _logger;
    private final int _id;
    private final int _maxOrderGenerationDelay;
    private final int _maxProductCount;
    private final String _name;
    private final String _ordersExchangeName;

    public CustomerController( int id, Configuration config, ILogger logger ){
        _id = id;
        _ordersExchangeName = config.getProperty(
            Configuration.ORDERS_EXCHANGE_NAME
        );
        _maxOrderGenerationDelay = Integer.parseInt(
            config.getProperty( Configuration.MAX_ORDER_GENERATION_DELAY )
        );
        _maxProductCount = Integer.parseInt(
            config.getProperty( Configuration.MAX_ORDER_PRODUCT_COUNT )
        );
        _name = "Customer-" + _id;
        _logger = logger;
    }

    public void run()
            throws IOException, TimeoutException, InterruptedException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(
                _ordersExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null    //No arguments
        );
        Order order = generateOrder();
        channel.basicPublish( "",
                              _ordersExchangeName,
                              MessageProperties.PERSISTENT_TEXT_PLAIN,
                              order.serialize());
        _logger.trace( _name + " sent order " + order.toString() );
        channel.close();
        connection.close();
    }

    private Order generateOrder() throws IOException, InterruptedException{
        _logger.trace( _name + " generating order..." );
        Order order = new Order();
        order.CustomerName = _name;
        order.ProductType = EProductType.randomProductType();
        Random random = new Random();
        random.setSeed( System.nanoTime() );
        order.Count = 1 + random.nextInt(_maxProductCount);
        int delay = random.nextInt( _maxOrderGenerationDelay );
        Thread.sleep( delay );
        return order;
    }

}
