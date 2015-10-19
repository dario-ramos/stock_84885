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

/**
 *
 * @author dario
 */
public class OrderReceiverController {

    private Channel ordersChannel;
    private Channel resultsChannel;
    private Channel shippingChannel;
    private Connection connection;
    private ConnectionFactory connFactory;
    private final ILogger auditLogger;
    private final ILogger traceLogger;
    private final IOrders orders;
    private final IStock stock;
    private final String hostName;
    private final String name;
    private final String ordersExchangeName;
    private final String resultsExchangeName;
    private final String shippingExchangeName;

    public OrderReceiverController( int id,
                                    IStock stock, 
                                    IOrders orders,
                                    Configuration config, ILogger traceLogger,
                                    ILogger auditLogger ){
        ordersExchangeName = config.getProperty(
            Configuration.ORDERS_EXCHANGE_NAME
        );
        resultsExchangeName = config.getProperty(
            Configuration.RESULTS_EXCHANGE_NAME
        );
        shippingExchangeName = config.getProperty(
            Configuration.SHIPPING_EXCHANGE_NAME
        );
        hostName = config.getProperty(
            Configuration.ORDER_RECEIVER_HOSTNAME
        );
        name = "OrderReceiver-" + id;
        this.traceLogger = traceLogger;
        this.auditLogger = auditLogger;
        this.stock = stock;
        this.orders = orders;
    }

    public void run() throws IOException, TimeoutException{
        try{
            connFactory = new ConnectionFactory();
            connFactory.setHost(hostName);
            connection = connFactory.newConnection();
            ordersChannel = connection.createChannel();
            resultsChannel = connection.createChannel();
            shippingChannel = connection.createChannel();
            ordersChannel.queueDeclare(ordersExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null //No arguments
            );
            resultsChannel.exchangeDeclare(resultsExchangeName, "direct");
            shippingChannel.queueDeclare(shippingExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null    //No arguments
            );
            traceLogger.trace(name + " waiting for orders" );
            ordersChannel.basicQos(1);
            final Consumer consumer = new DefaultConsumer(ordersChannel) {
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    Order order = Order.deserialize( body );
                    try {
                        processOrder(order);
                    } catch (InterruptedException | TimeoutException ex) {
                        traceLogger.error( ex.toString() );
                    } finally {
                        ordersChannel.basicAck(
                            envelope.getDeliveryTag(), false
                        );
                    }
                }
            };
            ordersChannel.basicConsume(ordersExchangeName, false, consumer
            );
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        releaseNetworkResources();
                    } catch (IOException | TimeoutException ex) {
                        try {
                            traceLogger.error( ex.toString() );
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
        traceLogger.trace(name + " received order: " + order.toString() );
        auditLogger.trace(name + " received order: " + order.toString() );
        orders.create( order, EOrderState.RECEIVED );
        boolean available = stock.decrement(order.getProductType(),
                                             order.getCount() );
        if( !available ){
            orders.setState( order, EOrderState.REJECTED );
            sendOrderResultToCustomer( order.getCustomerName(),
                                       EOrderState.REJECTED.name());
            return;
        }
        orders.setState( order, Order.EOrderState.APPROVED );
        sendOrderResultToCustomer( order.getCustomerName(),
                                   EOrderState.APPROVED.name() );
        sendOrderToShipping( order );
    }

    private void releaseNetworkResources() throws IOException,
                                                  TimeoutException{
        if( ordersChannel != null ){
            ordersChannel.close();
            ordersChannel = null;
        }
        if( resultsChannel != null ){
            resultsChannel.close();
            resultsChannel = null;
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

    private void sendOrderResultToCustomer( String customerName,
                                            String result )
            throws IOException, TimeoutException{
        traceLogger.trace(name + " sending order result " + result + " to " +
                            customerName );
        resultsChannel.basicPublish(resultsExchangeName,
            customerName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            result.getBytes()
        );
    }

    private void sendOrderToShipping( Order order ) throws IOException{
        traceLogger.trace(name + " sending order " + order.getID() + " to Shipping"
        );
        shippingChannel.basicPublish("",
            shippingExchangeName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            order.serialize()
        );
    }

}
