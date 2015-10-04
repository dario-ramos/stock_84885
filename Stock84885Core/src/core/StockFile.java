/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Use a file lock to access a text file (couldn't figure out how to
 * lock using a text file)
 * @author dario
 */
public class StockFile implements IStock {

    private final int _maxStock;
    private final String _filePath;
    private final String _lockFilePath;

    public StockFile( String filePath, int maxStock ){
        _filePath = filePath;
        _lockFilePath = filePath + "_lock";
        _maxStock = maxStock;
    }

    @Override
    public boolean decrement(Order.EProductType type, int count)
            throws IOException {
        return changeStock( type, -count );
    }

    @Override
    public boolean increment(Order.EProductType type, int count)
            throws IOException {
        return changeStock( type, count );
    }
    
    private boolean changeStock( Order.EProductType type, int count )
            throws IOException{
        Path path = Paths.get( _lockFilePath );
        boolean result = false;
        try ( FileChannel fileChannel = FileChannel.open(
              path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )){
            FileLock lock = fileChannel.lock();
            result = doChangeStock( type, count );
            lock.release();
            fileChannel.close();
        }
        return result;
    }

    private boolean doChangeStock(Order.EProductType type, int delta)
            throws IOException{
        //Save the file content to the String "input"
        String input = readFileIntoString();
        //Look for product type
        int iType = input.indexOf( type.name() );
        if( iType <= -1 ){
            return false;
        }
        int iEndOfLine = input.indexOf(FileSystemUtils.NEWLINE, iType);
        String lineToReplace = input.substring(iType, iEndOfLine);
        String[] typeAndStock = lineToReplace.split( "=" );
        if( typeAndStock.length != 2 ){
            throw new InvalidObjectException(
                "Bad stock file entry: " + lineToReplace
            );
        }
        int stock = Integer.parseInt( typeAndStock[1].trim() );
        if( stock + delta < 0 || stock + delta > _maxStock ){
            return false;
        }
        int iStock = input.indexOf( "=", iType ) + 1;
        String toReplace = input.substring(iStock, iEndOfLine);
        input = input.replace(toReplace,
            " " + Integer.toString(stock + delta)
        );
        //Write the new String with the replaced line OVER the same file
        FileOutputStream fileOut = new FileOutputStream(_filePath);
        fileOut.write(input.getBytes());
        fileOut.close();
        return true;
    }

    private String readFileIntoString()
            throws FileNotFoundException, IOException{
        BufferedReader file = new BufferedReader(new FileReader(_filePath));
        String line;
        String input = "";
        while ((line = file.readLine()) != null){
            input += line + FileSystemUtils.NEWLINE;
        }
        file.close();
        return input;
    }

}
