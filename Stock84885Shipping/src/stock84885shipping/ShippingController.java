/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885shipping;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import core.Configuration;
import core.ILogger;
import core.IOrders;
import core.Order;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dario
 */
public class ShippingController {

    private Channel deliveryChannel;
    private Channel shippingChannel;
    private ConnectionFactory connFactory;
    private Connection connection;
    private final ILogger logger;
    private final int maxOrderDeliveryDelay;
    private final IOrders orders;
    private final String deliveryExchangeName;
    private final String shippingExchangeName;
    private final String hostName;
    private final String name;

    public ShippingController( int id, IOrders orders,
                               Configuration config, ILogger logger ){
        deliveryExchangeName = config.getProperty(
            Configuration.DELIVERY_EXCHANGE_NAME
        );
        shippingExchangeName = config.getProperty(
            Configuration.SHIPPING_EXCHANGE_NAME
        );
        hostName = config.getProperty( Configuration.SHIPPING_HOSTNAME );
        name = "Shipping-" + id;
        this.logger = logger;
        this.orders = orders;
        maxOrderDeliveryDelay = Integer.parseInt( config.getProperty(
            Configuration.MAX_ORDER_DELIVERY_DELAY
        ) );
    }

    public void run() throws URISyntaxException,
                             IOException, TimeoutException{
        try{
            connFactory = new ConnectionFactory();
            connFactory.setHost(hostName);
            connection = connFactory.newConnection();
            shippingChannel = connection.createChannel();
            deliveryChannel = connection.createChannel();
            shippingChannel.queueDeclare(shippingExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null //No arguments
            );
            deliveryChannel.exchangeDeclare(deliveryExchangeName, "direct");
            logger.trace(name + " waiting for orders" );
            shippingChannel.basicQos(1);
            final Consumer consumer = new DefaultConsumer(shippingChannel) {
              @Override
              public void handleDelivery(String consumerTag,
                                         Envelope envelope,
                                         AMQP.BasicProperties properties,
                                         byte[] body) throws IOException {
                  Order order = Order.deserialize(body);
                  logger.trace(name + " received order " + order.getID() );
                  try {
                      deliverOrder(order);
                  } catch (Exception ex) {
                      logger.error( ex.toString() );
                  }
              }
            };
            shippingChannel.basicConsume(shippingExchangeName, false, consumer
            );
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        releaseNetworkResources();
                    } catch (IOException | TimeoutException ex) {
                        try {
                            logger.error( ex.toString() );
                        } catch (IOException ex1) {
                            System.err.println( ex1.toString() );
                        }
                    }
                }
             });
        }catch( IOException | TimeoutException e ){
            releaseNetworkResources();
            throw e;
        }
    }

    private void deliverOrder( Order order )
            throws InterruptedException, IOException{
        Random random = new Random();
        random.setSeed( System.nanoTime() );
        int delay = random.nextInt(maxOrderDeliveryDelay );
        Thread.sleep( delay );
        deliveryChannel.basicPublish(deliveryExchangeName,
            order.getCustomerName(),
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            order.getID().getBytes()
        );
        orders.setState(order, Order.EOrderState.DELIVERED);
    }

    private void releaseNetworkResources() throws IOException,
                                           TimeoutException{
        if( deliveryChannel != null ){
            deliveryChannel.close();
            deliveryChannel = null;
        }
        if( shippingChannel != null ){
            shippingChannel.close();
            shippingChannel = null;
        }
        if( connection != null ){
            connection.close();
            connection = null;
        }
    }

}
