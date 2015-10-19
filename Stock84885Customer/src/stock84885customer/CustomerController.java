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

    private Channel deliveryChannel;
    private Channel resultChannel;
    private Channel ordersChannel;
    private Connection connection;
    private ConnectionFactory connFactory;
    private Consumer deliveryConsumer;
    private Consumer resultConsumer;
    private final ILogger logger;
    private final int id;
    private final int maxOrderGenerationDelay;
    private final int maxProductCount;
    private final String hostName;
    private final String name;
    private final String deliveryExchangeName;
    private final String ordersExchangeName;
    private final String resultsExchangeName;
    private String deliveryQueueName;
    private String resultQueueName;

    public CustomerController( int id, 
                               Configuration config, ILogger logger ){
        this.id = id;
        ordersExchangeName = config.getProperty(
            Configuration.ORDERS_EXCHANGE_NAME
        );
        resultsExchangeName = config.getProperty(
            Configuration.RESULTS_EXCHANGE_NAME
        );
        deliveryExchangeName = config.getProperty(
            Configuration.DELIVERY_EXCHANGE_NAME
        );
        maxOrderGenerationDelay = Integer.parseInt(
            config.getProperty( Configuration.MAX_ORDER_GENERATION_DELAY )
        );
        maxProductCount = Integer.parseInt(
            config.getProperty( Configuration.MAX_ORDER_PRODUCT_COUNT )
        );
        hostName = config.getProperty(
            Configuration.CUSTOMER_HOSTNAME
        );
        name = "Customer-" + this.id;
        this.logger = logger;
    }

    public void run()
            throws IOException, TimeoutException, InterruptedException{
        try{
            connFactory = new ConnectionFactory();
            connFactory.setHost(hostName);
            connection = connFactory.newConnection();
            initResultChannel();
            initDeliveryChannel();
            sendOrder();
            resultChannel.basicConsume(resultQueueName, false, resultConsumer
            );
        }catch( IOException | TimeoutException e ){
            releaseNetworkResources();
            throw e;
        }
    }

    private Order generateOrder() throws IOException, InterruptedException{
        logger.trace(name + " generating order..." );
        Order order = new Order();
        order.setCustomerName(name );
        order.setProductType( EProductType.randomProductType() );
        Random random = new Random();
        random.setSeed( System.nanoTime() );
        order.setCount(1 + random.nextInt(maxProductCount) );
        order.setState( EOrderState.UNDEFINED );
        int delay = random.nextInt(maxOrderGenerationDelay );
        Thread.sleep( delay );
        return order;
    }

    private void handleOrderResult( String orderResult )
            throws IOException, TimeoutException{
        logger.trace(name + " received order result: " + orderResult );
        if( orderResult.equals(EOrderState.REJECTED.name()) ){
            releaseNetworkResources();
            return;
        }
        deliveryChannel.basicConsume(
            deliveryQueueName, false, deliveryConsumer
        );
    }

    private void initResultChannel() throws IOException, TimeoutException{
        resultChannel = connection.createChannel();
        resultChannel.exchangeDeclare(resultsExchangeName, "direct" );
        resultQueueName = resultChannel.queueDeclare().getQueue();
        resultChannel.queueBind(resultQueueName, resultsExchangeName, name
        );
        resultConsumer = new DefaultConsumer(resultChannel){
          @Override
          public void handleDelivery(String consumerTag,
                                     Envelope envelope,
                                     AMQP.BasicProperties properties,
                                     byte[] body) throws IOException {
            try {
                handleOrderResult( new String( body, "UTF-8" ) );
            } catch (Exception ex) {
                logger.error( ex.toString() );
            }
          }
        };
    }

    private void initDeliveryChannel() throws IOException, TimeoutException{
        deliveryChannel = connection.createChannel();
        deliveryChannel.exchangeDeclare(deliveryExchangeName, "direct" );
        deliveryQueueName = deliveryChannel.queueDeclare().getQueue();
        deliveryChannel.queueBind(deliveryQueueName, deliveryExchangeName, name
        );
        deliveryConsumer = new DefaultConsumer(deliveryChannel){
          @Override
          public void handleDelivery(String consumerTag,
                                     Envelope envelope,
                                     AMQP.BasicProperties properties,
                                     byte[] body) throws IOException {
            String orderID = new String( body, "UTF-8" );
            logger.trace(name + " received order " + orderID + "!" );
            try {
                releaseNetworkResources();
            } catch (TimeoutException ex) {
                logger.error( ex.toString() );
            }
          }
        };
    }

    private void releaseNetworkResources() throws IOException,
                                                  TimeoutException{
        if( ordersChannel != null ){
            ordersChannel.close();
            ordersChannel = null;
        }
        if( resultChannel != null ){
            resultChannel.close();
            resultChannel = null;
        }
        if( deliveryChannel != null ){
            deliveryChannel.close();
            deliveryChannel = null;
        }
        if( connection != null ){
            connection.close();
            connection = null;
        }
    }

    private void sendOrder() throws InterruptedException, IOException,
                                    TimeoutException{
        ordersChannel = connection.createChannel();
        ordersChannel.queueDeclare(ordersExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null    //No arguments
        );
        Order order = generateOrder();
        ordersChannel.basicPublish("",
                              ordersExchangeName,
                              MessageProperties.PERSISTENT_TEXT_PLAIN,
                              order.serialize());
        logger.trace(name + " sent order " + order.toString() );
    }

}
