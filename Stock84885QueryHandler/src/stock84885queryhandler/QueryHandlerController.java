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

    private Channel _queriesResultsChannel;
    private final ILogger _logger;
    private final IOrders _orders;
    private final String _hostName;
    private final String _name;
    private final String _queriesExchangeName;
    private final String _queriesResultsExchangeName;

    public QueryHandlerController( int id, Configuration config,
                                   IOrders orders, ILogger logger ){
        _logger = logger;
        _orders = orders;
        _hostName = config.getProperty(Configuration.QUERY_HANDLER_HOSTNAME);
        _name = "QueryHandler" + id;
        _queriesExchangeName = config.getProperty(
            Configuration.QUERIES_EXCHANGE_NAME
        );
        _queriesResultsExchangeName = config.getProperty(
            Configuration.QUERIES_RESULTS_EXCHANGE_NAME
        );
    }

    public void run() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(_hostName);
        final Connection connection = factory.newConnection();
        final Channel queriesChannel = connection.createChannel();
        _queriesResultsChannel = connection.createChannel();
        queriesChannel.queueDeclare(
            _queriesExchangeName,
            true, //Passive declaration
            false, //Non-durable queue
            false, //Non-exclusive queue
            null //No arguments
        );
        _queriesResultsChannel.exchangeDeclare(
            _queriesResultsExchangeName, "direct"
        );
        _logger.trace( _name + " waiting for queries..." );
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
                _logger.error( ex.toString() );
            } finally {
                queriesChannel.basicAck(envelope.getDeliveryTag(), false);
            }
          }
        };
        queriesChannel.basicConsume( _queriesExchangeName, false, consumer);
    }

    private void processQuery( String userName ) throws IOException{
        List<Order> orders = _orders.getOrdersByUserName( userName );
        String result = "";
        if( !orders.isEmpty() ){
            for( int i=0; i<orders.size()-1; i++ ){
                result += orders.get(i).toString() + FileSystemUtils.NEWLINE;
            }
            result += orders.get( orders.size() - 1 ).toString();
        }
        //Send query result to user
        _logger.trace( _name + " sending query result " + result + " to " +
                       userName );
        _queriesResultsChannel.basicPublish(
            _queriesResultsExchangeName,
            userName,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            result.getBytes()
        );
    }

}
