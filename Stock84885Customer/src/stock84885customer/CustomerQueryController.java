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
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author dario
 */
public class CustomerQueryController {

    private Channel queryChannel;
    private Channel queryResultChannel;
    private Connection connection;
    private Consumer queryResultsConsumer;
    private final ConnectionFactory connFactory;
    private final ILogger logger;
    private final String hostName;
    private final String name;
    private final String queryExchangeName;
    private final String queryResultsExchangeName;
    private String queryResultsQueueName;

    public CustomerQueryController( int id, Configuration config,
                                    ILogger logger ){
        connFactory = new ConnectionFactory();
        this.logger = logger;
        hostName = config.getProperty(
            Configuration.CUSTOMER_QUERY_HOSTNAME
        );
        name = "Customer-" + id;
        queryExchangeName = config.getProperty(
            Configuration.QUERIES_EXCHANGE_NAME
        );
        queryResultsExchangeName = config.getProperty(
            Configuration.QUERIES_RESULTS_EXCHANGE_NAME
        );
    }

    public void run() throws IOException, TimeoutException{
        try{
            connFactory.setHost( hostName );
            connection = connFactory.newConnection();
            initQueryChannel();
            initQueryResultChannel();
            sendQuery();
            queryResultChannel.basicConsume(
                queryResultsQueueName, false, queryResultsConsumer
            );
        }catch( IOException | TimeoutException e ){
            releaseNetworkResources();
            throw e;
        }
    }

    private void initQueryChannel() throws IOException{
        queryChannel = connection.createChannel();
        queryChannel.queueDeclare(
                queryExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null    //No arguments
        );
    }

    private void initQueryResultChannel() throws IOException{
        queryResultChannel = connection.createChannel();
        queryResultChannel.exchangeDeclare(
            queryResultsExchangeName, "direct"
        );
        queryResultsQueueName = queryResultChannel.queueDeclare().getQueue();
        queryResultChannel.queueBind(
            queryResultsQueueName, queryResultsExchangeName, name
        );
        queryResultsConsumer = new DefaultConsumer(queryResultChannel){
          @Override
          public void handleDelivery(String consumerTag,
                                     Envelope envelope,
                                     AMQP.BasicProperties properties,
                                     byte[] body) throws IOException {
            try {
                String result = new String( body, "UTF-8" );
                logger.trace( name + " received query result: " + result );
                releaseNetworkResources();
            } catch (Exception ex) {
                logger.error( ex.toString() );
            }
          }
        };
    }

    private void releaseNetworkResources() throws IOException,
                                                  TimeoutException{
        if( queryChannel != null ){
            queryChannel.close();
            queryChannel = null;
        }
        if( queryResultChannel != null ){
            queryResultChannel.close();
            queryResultChannel = null;
        }
        if( connection != null ){
            connection.close();
            connection = null;
        }
    }

    private void sendQuery() throws IOException{
        queryChannel.basicPublish(
            "",
            queryExchangeName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            name.getBytes()
        );
    }


}
