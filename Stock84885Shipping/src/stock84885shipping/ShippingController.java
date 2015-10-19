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

    private Channel _deliveryChannel;
    private Channel _shippingChannel;
    private ConnectionFactory _connFactory;
    private Connection _connection;
    private final ILogger _logger;
    private final int _maxOrderDeliveryDelay;
    private final IOrders _orders;
    private final String _deliveryExchangeName;
    private final String _shippingExchangeName;
    private final String _hostName;
    private final String _name;

    public ShippingController( int id, IOrders orders,
                               Configuration config, ILogger logger ){
        _deliveryExchangeName = config.getProperty(
            Configuration.DELIVERY_EXCHANGE_NAME
        );
        _shippingExchangeName = config.getProperty(
            Configuration.SHIPPING_EXCHANGE_NAME
        );
        _hostName = config.getProperty( Configuration.SHIPPING_HOSTNAME );
        _name = "Shipping-" + id;
        _logger = logger;
        _orders = orders;
        _maxOrderDeliveryDelay = Integer.parseInt( config.getProperty(
            Configuration.MAX_ORDER_DELIVERY_DELAY
        ) );
    }

    public void run() throws URISyntaxException,
                             IOException, TimeoutException{
        try{
            _connFactory = new ConnectionFactory();
            _connFactory.setHost(_hostName);
            _connection = _connFactory.newConnection();
            _shippingChannel = _connection.createChannel();
            _deliveryChannel = _connection.createChannel();
            _shippingChannel.queueDeclare(_shippingExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null //No arguments
            );
            _deliveryChannel.exchangeDeclare(_deliveryExchangeName, "direct");
            _logger.trace( _name + " waiting for orders" );
            _shippingChannel.basicQos(1);
            final Consumer consumer = new DefaultConsumer(_shippingChannel) {
              @Override
              public void handleDelivery(String consumerTag,
                                         Envelope envelope,
                                         AMQP.BasicProperties properties,
                                         byte[] body) throws IOException {
                  Order order = Order.deserialize(body);
                  _logger.trace( _name + " received order " + order.getID() );
                  try {
                      deliverOrder(order);
                  } catch (Exception ex) {
                      _logger.error( ex.toString() );
                  }
              }
            };
            _shippingChannel.basicConsume(
                _shippingExchangeName, false, consumer
            );
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        releaseNetworkResources();
                    } catch (IOException | TimeoutException ex) {
                        try {
                            _logger.error( ex.toString() );
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
        int delay = random.nextInt( _maxOrderDeliveryDelay );
        Thread.sleep( delay );
        _deliveryChannel.basicPublish(
            _deliveryExchangeName,
            order.getCustomerName(),
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            order.getID().getBytes()
        );
        _orders.setState(order, Order.EOrderState.DELIVERED);
    }

    private void releaseNetworkResources() throws IOException,
                                           TimeoutException{
        if( _deliveryChannel != null ){
            _deliveryChannel.close();
            _deliveryChannel = null;
        }
        if( _shippingChannel != null ){
            _shippingChannel.close();
            _shippingChannel = null;
        }
        if( _connection != null ){
            _connection.close();
            _connection = null;
        }
    }

}
