/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885queryhandler;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import core.Configuration;
import core.FileSystemUtils;
import core.ILogger;
import core.IOrders;
import core.Order;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dario
 */
public class QueryHandlerController {

    private Channel queriesChannel;
    private Channel queriesResultsChannel;
    private ConnectionFactory connFactory;
    private Connection connection;
    private final ILogger logger;
    private final IOrders orders;
    private final String hostName;
    private final String name;
    private final String queriesExchangeName;
    private final String queriesResultsExchangeName;

    public QueryHandlerController( int id, Configuration config,
                                   IOrders orders, ILogger logger ){
        this.logger = logger;
        this.orders = orders;
        hostName = config.getProperty(Configuration.QUERY_HANDLER_HOSTNAME);
        name = "QueryHandler" + id;
        queriesExchangeName = config.getProperty(
            Configuration.QUERIES_EXCHANGE_NAME
        );
        queriesResultsExchangeName = config.getProperty(
            Configuration.QUERIES_RESULTS_EXCHANGE_NAME
        );
    }

    public void run() throws IOException, TimeoutException{
        try{
            connFactory = new ConnectionFactory();
            connFactory.setHost(hostName);
            connection = connFactory.newConnection();
            queriesChannel = connection.createChannel();
            queriesResultsChannel = connection.createChannel();
            queriesChannel.queueDeclare(queriesExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null //No arguments
            );
            queriesResultsChannel.exchangeDeclare(queriesResultsExchangeName, "direct"
            );
            logger.trace(name + " waiting for queries..." );
            queriesChannel.basicQos(1);
            final Consumer consumer = new DefaultConsumer(queriesChannel) {
              @Override
              public void handleDelivery(String consumerTag,
                                         Envelope envelope,
                                         AMQP.BasicProperties properties,
                                         byte[] body) throws IOException {
                String userName = new String( body, "UTF-8" );
                try {
                    processQuery( userName );
                } catch (Exception ex) {
                    logger.error( ex.toString() );
                } finally {
                    queriesChannel.basicAck(envelope.getDeliveryTag(), false);
                }
              }
            };
            queriesChannel.basicConsume(queriesExchangeName, false, consumer
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

    private void processQuery( String userName ) throws IOException{
        List<Order> orders = this.orders.getOrdersByUserName( userName );
        String result = "";
        if( !orders.isEmpty() ){
            for( int i=0; i<orders.size()-1; i++ ){
                result += orders.get(i).toString() + FileSystemUtils.NEWLINE;
            }
            result += orders.get( orders.size() - 1 ).toString();
        }
        //Send query result to user
        logger.trace(name + " sending query result " + result + " to " +
                       userName );
        queriesResultsChannel.basicPublish(queriesResultsExchangeName,
            userName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            result.getBytes()
        );
    }

    private void releaseNetworkResources() throws IOException,
                                           TimeoutException{
        if( queriesChannel != null ){
            queriesChannel.close();
            queriesChannel = null;
        }
        if( queriesResultsChannel != null ){
            queriesResultsChannel.close();
            queriesResultsChannel = null;
        }
        if( connection != null ){
            connection.close();
            connection = null;
        }
    }

}
