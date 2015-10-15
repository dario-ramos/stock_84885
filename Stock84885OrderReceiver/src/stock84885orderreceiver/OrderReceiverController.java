/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885orderreceiver;

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
import core.IStock;
import core.Order;
import core.Order.EOrderState;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class OrderReceiverController {

    private Channel _ordersChannel;
    private Channel _resultsChannel;
    private Channel _shippingChannel;
    private Connection _connection;
    private ConnectionFactory _connFactory;
    private final ILogger _auditLogger;
    private final ILogger _traceLogger;
    private final IOrders _orders;
    private final IStock _stock;
    private final String _hostName;
    private final String _name;
    private final String _ordersExchangeName;
    private final String _resultsExchangeName;
    private final String _shippingExchangeName;

    public OrderReceiverController( int id,
                                    IStock stock, 
                                    IOrders orders,
                                    Configuration config, ILogger traceLogger,
                                    ILogger auditLogger ){
        _ordersExchangeName = config.getProperty(
            Configuration.ORDERS_EXCHANGE_NAME
        );
        _resultsExchangeName = config.getProperty(
            Configuration.RESULTS_EXCHANGE_NAME
        );
        _shippingExchangeName = config.getProperty(
            Configuration.SHIPPING_EXCHANGE_NAME
        );
        _hostName = config.getProperty(
            Configuration.ORDER_RECEIVER_HOSTNAME
        );
        _name = "OrderReceiver-" + id;
        _traceLogger = traceLogger;
        _auditLogger = auditLogger;
        _stock = stock;
        _orders = orders;
    }

    public void run() throws IOException, TimeoutException{
        try{
            _connFactory = new ConnectionFactory();
            _connFactory.setHost(_hostName);
            _connection = _connFactory.newConnection();
            _ordersChannel = _connection.createChannel();
            _resultsChannel = _connection.createChannel();
            _shippingChannel = _connection.createChannel();
            _ordersChannel.queueDeclare(
                _ordersExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null //No arguments
            );
            _resultsChannel.exchangeDeclare(_resultsExchangeName, "direct");
            _shippingChannel.queueDeclare(
                _shippingExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null    //No arguments
            );
            _traceLogger.trace( _name + " waiting for orders" );
            _ordersChannel.basicQos(1);
            final Consumer consumer = new DefaultConsumer(_ordersChannel) {
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    Order order = Order.deserialize( body );
                    try {
                        processOrder(order);
                    } catch (InterruptedException | TimeoutException ex) {
                        _traceLogger.error( ex.toString() );
                    } finally {
                        _ordersChannel.basicAck(
                            envelope.getDeliveryTag(), false
                        );
                    }
                }
            };
            _ordersChannel.basicConsume(
                _ordersExchangeName, false, consumer
            );
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        releaseNetworkResources();
                    } catch (IOException | TimeoutException ex) {
                        try {
                            _traceLogger.error( ex.toString() );
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

    private void processOrder(Order order)
            throws InterruptedException, IOException, TimeoutException {
        _traceLogger.trace( _name + " received order: " + order.toString() );
        _auditLogger.trace( _name + " received order: " + order.toString() );
        _orders.create( order, EOrderState.RECEIVED );
        boolean available = _stock.decrement(order.ProductType, order.Count);
        if( !available ){
            _orders.setState( order, EOrderState.REJECTED );
            sendOrderResultToCustomer( order.CustomerName,
                                       EOrderState.REJECTED.name());
            return;
        }
        _orders.setState( order, Order.EOrderState.APPROVED );
        sendOrderResultToCustomer( order.CustomerName,
                                   EOrderState.APPROVED.name() );
        sendOrderToShipping( order );
    }

    private void releaseNetworkResources() throws IOException,
                                                  TimeoutException{
        if( _ordersChannel != null ){
            _ordersChannel.close();
            _ordersChannel = null;
        }
        if( _resultsChannel != null ){
            _ordersChannel.close();
            _ordersChannel = null;
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

    private void sendOrderResultToCustomer( String customerName,
                                            String result )
            throws IOException, TimeoutException{
        _traceLogger.trace( _name + " sending order result " + result + " to " +
                            customerName );
        _resultsChannel.basicPublish(
            _resultsExchangeName,
            customerName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            result.getBytes()
        );
    }

    private void sendOrderToShipping( Order order ) throws IOException{
        _traceLogger.trace(
            _name + " sending order " + order.getID() + " to Shipping"
        );
        _shippingChannel.basicPublish(
            "",
            _shippingExchangeName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            order.serialize()
        );
    }

}
