/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885customer;

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
import core.Order;
import core.Order.EOrderState;
import core.Order.EProductType;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dario
 */
public class CustomerController {

    private Channel _deliveryChannel;
    private Channel _resultChannel;
    private Consumer _deliveryConsumer;
    private Consumer _resultConsumer;
    private final ILogger _logger;
    private final int _id;
    private final int _maxOrderGenerationDelay;
    private final int _maxProductCount;
    private final String _hostName;
    private final String _name;
    private final String _deliveryExchangeName;
    private final String _ordersExchangeName;
    private final String _resultsExchangeName;
    private String _deliveryConsumerTag;
    private String _deliveryQueueName;
    private String _resultQueueName;

    public CustomerController( int id, 
                               Configuration config, ILogger logger ){
        _id = id;
        _ordersExchangeName = config.getProperty(
            Configuration.ORDERS_EXCHANGE_NAME
        );
        _resultsExchangeName = config.getProperty(
            Configuration.RESULTS_EXCHANGE_NAME
        );
        _deliveryExchangeName = config.getProperty(
            Configuration.DELIVERY_EXCHANGE_NAME
        );
        _maxOrderGenerationDelay = Integer.parseInt(
            config.getProperty( Configuration.MAX_ORDER_GENERATION_DELAY )
        );
        _maxProductCount = Integer.parseInt(
            config.getProperty( Configuration.MAX_ORDER_PRODUCT_COUNT )
        );
        _hostName = config.getProperty(
            Configuration.CUSTOMER_HOSTNAME
        );
        _name = "Customer-" + _id;
        _logger = logger;
    }

    public void run()
            throws IOException, TimeoutException, InterruptedException{
        initResultChannel();
        initDeliveryChannel();
        sendOrder();
        _resultChannel.basicConsume( _resultQueueName, false, _resultConsumer);
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

    private void handleOrderResult( String orderResult )
            throws IOException, TimeoutException{
        _logger.trace( _name + " received order result: " + orderResult );
        if( orderResult.equals(EOrderState.REJECTED.name()) ){
            _deliveryChannel.basicCancel( _deliveryConsumerTag );
            _deliveryChannel.close();
            return;
        }
        _deliveryConsumerTag = _deliveryChannel.basicConsume(
            _deliveryQueueName, false, _deliveryConsumer
        );
    }

    private void initResultChannel() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(_hostName);
        final Connection connection = factory.newConnection();
        _resultChannel = connection.createChannel();
        _resultChannel.exchangeDeclare( _resultsExchangeName, "direct" );
        _resultQueueName = _resultChannel.queueDeclare().getQueue();
        _resultChannel.queueBind(
                _resultQueueName, _resultsExchangeName, _name
        );
        _resultConsumer = new DefaultConsumer(_resultChannel){
          @Override
          public void handleDelivery(String consumerTag,
                                     Envelope envelope,
                                     AMQP.BasicProperties properties,
                                     byte[] body) throws IOException {
            try {
                handleOrderResult( new String( body, "UTF-8" ) );
                _resultChannel.close();
            } catch (Exception ex) {
                _logger.error( ex.toString() );
            }
            connection.close();
          }
        };
    }

    private void initDeliveryChannel() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(_hostName);
        final Connection connection = factory.newConnection();
        _deliveryChannel = connection.createChannel();
        _deliveryChannel.exchangeDeclare( _deliveryExchangeName, "direct" );
        _deliveryQueueName = _deliveryChannel.queueDeclare().getQueue();
        _deliveryChannel.queueBind(
                _deliveryQueueName, _deliveryExchangeName, _name
        );
        _deliveryConsumer = new DefaultConsumer(_deliveryChannel){
          @Override
          public void handleDelivery(String consumerTag,
                                     Envelope envelope,
                                     AMQP.BasicProperties properties,
                                     byte[] body) throws IOException {
            String orderID = new String( body, "UTF-8" );
            _logger.trace( _name + " received order " + orderID + "!" );
            try {
                _deliveryChannel.close();
            } catch (TimeoutException ex) {
                _logger.error( ex.toString() );
            }
            connection.close();
          }
        };
    }

    private void sendOrder() throws IOException,
                                    TimeoutException, InterruptedException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(_hostName);
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

}
