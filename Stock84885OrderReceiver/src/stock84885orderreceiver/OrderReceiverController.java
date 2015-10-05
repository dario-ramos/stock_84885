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

    private Channel _resultsChannel;
    private Channel _shippingChannel;
    private final ILogger _logger;
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
                                    Configuration config, ILogger logger ){
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
        _logger = logger;
        _stock = stock;
        _orders = orders;
    }

    public void run() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(_hostName);
        final Connection connection = factory.newConnection();
        final Channel ordersChannel = connection.createChannel();
        _resultsChannel = connection.createChannel();
        _shippingChannel = connection.createChannel();
        ordersChannel.queueDeclare(
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
        _logger.trace( _name + " waiting for orders" );
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
                _logger.error( ex.toString() );
            } finally {
                ordersChannel.basicAck(envelope.getDeliveryTag(), false);
            }
          }
        };
        ordersChannel.basicConsume( _ordersExchangeName, false, consumer);
    }

    private void processOrder(Order order)
            throws InterruptedException, IOException, TimeoutException {
        _logger.trace( _name + " received order: " + order.toString() );
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

    private void sendOrderResultToCustomer( String customerName,
                                            String result )
            throws IOException, TimeoutException{
        _logger.trace( _name + " sending order result " + result + " to " +
                       customerName );
        _resultsChannel.basicPublish(
            _resultsExchangeName,
            customerName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            result.getBytes()
        );
    }

    private void sendOrderToShipping( Order order ) throws IOException{
        _logger.trace(
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
