/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885stockadmin;

import core.Configuration;
import core.ILogger;
import core.IStock;
import core.Order.EProductType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

/**
 *
 * @author dario
 */
public class StockAdminController {

    private final IStock stock;
    private final ILogger logger;
    private final int maxProductCount;
    private final String name;

    public StockAdminController( int id, Configuration config,
                                 IStock stock, ILogger logger ){
        this.stock = stock;
        this.logger = logger;
        maxProductCount = Integer.parseInt(
            config.getProperty(Configuration.MAX_ORDER_PRODUCT_COUNT)
        );
        name = "StockAdmin-" + id;
    }

    public void run() throws URISyntaxException, IOException{
        //Choose a random product type and a random count
        EProductType type = EProductType.randomProductType();
        Random random = new Random();
        random.setSeed( System.nanoTime() );
        int count = 1 + random.nextInt(maxProductCount);
        //Now decide whether to increment or decrement
        boolean increment = (random.nextInt() % 2 == 0);
        if( increment ){
            logger.trace(name + " will increment " + type.name() + " by " + count
            );
            stock.increment(type, count);
        }else{
            logger.trace(name + " will decrement " + type.name() + " by " + count
            );
            stock.decrement(type, count);
        }
    }

}
