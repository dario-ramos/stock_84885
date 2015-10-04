/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stock84885stockadmin;

import core.Configuration;
import core.FileSystemUtils;
import core.ILogger;
import core.IStock;
import core.Order;
import core.Order.EProductType;
import core.StockFile;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

/**
 *
 * @author dario
 */
public class StockAdminController {

    private IStock _stock;
    private ILogger _logger;
    private final int _maxProductCount;
    private final String _name;

    public StockAdminController( int id, Configuration config,
                                 IStock stock, ILogger logger ){
        _stock = stock;
        _logger = logger;
        _maxProductCount = Integer.parseInt(
            config.getProperty(Configuration.MAX_ORDER_PRODUCT_COUNT)
        );
        _name = "StockAdmin-" + id;
    }

    public void run() throws URISyntaxException, IOException{
        //Choose a random product type and a random count
        EProductType type = EProductType.randomProductType();
        Random random = new Random();
        random.setSeed( System.nanoTime() );
        int count = 1 + random.nextInt(_maxProductCount);
        //Now decide whether to increment or decrement
        boolean increment = (random.nextInt() % 2 == 0);
        if( increment ){
            _stock.increment(type, count);
        }else{
            _stock.decrement(type, count);
        }
    }

}
