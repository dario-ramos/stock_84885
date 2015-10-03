/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Use a file lock to access a text file (couldn't figure out how to
 * lock using a text file)
 * @author dario
 */
public class StockFile implements IStock {

    private final String _filePath;
    private final String _lockFilePath;

    public StockFile( String filePath ){
        _filePath = filePath;
        _lockFilePath = filePath + "_lock";
    }

    @Override
    public boolean available(Order.EProductType type, int count)
            throws IOException {
        Path path = Paths.get( _lockFilePath );
        boolean result = false;
        try (FileChannel fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )){
            FileLock lock = fileChannel.lock();
            result = readAvailability( type, count );
            lock.release();
            fileChannel.close();
        }
        return result;
    }

    private boolean readAvailability(Order.EProductType type, int count)
            throws IOException{
        Path filePath = Paths.get( _filePath );
        List<String> lines = Files.readAllLines(filePath);
        for( int i=0; i<lines.size(); i++ ){
            String line = lines.get(i);
            String[] prodAndStock = line.split("=");
            if( prodAndStock.length != 2 ){
                throw new InvalidObjectException(
                        "Bad stock entry : " + line );
            }
            Order.EProductType prodType = Order.EProductType.valueOf(
                prodAndStock[0].trim()
            );
            if( prodType != type ){
                continue;
            }
            int stock = Integer.parseInt(prodAndStock[1].trim());
            return count <= stock;
        }
        return false;
    }

}
