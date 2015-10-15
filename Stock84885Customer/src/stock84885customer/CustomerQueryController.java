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

    private Channel _queryChannel;
    private Channel _queryResultChannel;
    private Connection _connection;
    private Consumer _queryResultsConsumer;
    private final ConnectionFactory _connFactory;
    private final ILogger _logger;
    private final String _hostName;
    private final String _name;
    private final String _queryExchangeName;
    private final String _queryResultsExchangeName;
    private String _queryResultsQueueName;

    public CustomerQueryController( int id, Configuration config,
                                    ILogger logger ){
        _connFactory = new ConnectionFactory();
        _logger = logger;
        _hostName = config.getProperty(
            Configuration.CUSTOMER_QUERY_HOSTNAME
        );
        _name = "Customer-" + id;
        _queryExchangeName = config.getProperty(
            Configuration.QUERIES_EXCHANGE_NAME
        );
        _queryResultsExchangeName = config.getProperty(
            Configuration.QUERIES_RESULTS_EXCHANGE_NAME
        );
    }

    public void run() throws IOException, TimeoutException{
        try{
            _connFactory.setHost( _hostName );
            _connection = _connFactory.newConnection();
            initQueryChannel();
            initQueryResultChannel();
            sendQuery();
            _queryResultChannel.basicConsume(
                _queryResultsQueueName, false, _queryResultsConsumer
            );
        }catch( IOException | TimeoutException e ){
            releaseNetworkResources();
            throw e;
        }
    }

    private void initQueryChannel() throws IOException{
        _queryChannel = _connection.createChannel();
        _queryChannel.queueDeclare(
                _queryExchangeName,
                true, //Passive declaration
                false, //Non-durable queue
                false, //Non-exclusive queue
                null    //No arguments
        );
    }

    private void initQueryResultChannel() throws IOException{
        _queryResultChannel = _connection.createChannel();
        _queryResultChannel.exchangeDeclare(
            _queryResultsExchangeName, "direct"
        );
        _queryResultsQueueName = _queryResultChannel.queueDeclare().getQueue();
        _queryResultChannel.queueBind(
            _queryResultsQueueName, _queryResultsExchangeName, _name
        );
        _queryResultsConsumer = new DefaultConsumer(_queryResultChannel){
          @Override
          public void handleDelivery(String consumerTag,
                                     Envelope envelope,
                                     AMQP.BasicProperties properties,
                                     byte[] body) throws IOException {
            try {
                String result = new String( body, "UTF-8" );
                _logger.trace( _name + " received query result: " + result );
                releaseNetworkResources();
            } catch (Exception ex) {
                _logger.error( ex.toString() );
            }
          }
        };
    }

    private void releaseNetworkResources() throws IOException,
                                                  TimeoutException{
        if( _queryChannel != null ){
            _queryChannel.close();
            _queryChannel = null;
        }
        if( _queryResultChannel != null ){
            _queryResultChannel.close();
            _queryResultChannel = null;
        }
        if( _connection != null ){
            _connection.close();
            _connection = null;
        }
    }

    private void sendQuery() throws IOException{
        _queryChannel.basicPublish(
            "",
            _queryExchangeName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            _name.getBytes()
        );
    }


}
