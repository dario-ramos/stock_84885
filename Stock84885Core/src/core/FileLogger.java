/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import static core.FileSystemUtils.NEWLINE;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Uses a system-wide lock mechanism
 * @author dario
 */
public class FileLogger implements ILogger {

    private final String _filePath;

    public FileLogger( String logFilePath ) throws FileNotFoundException{
        _filePath = logFilePath;
    }

    @Override
    public void error(String msg) throws IOException {
        trace( "ERROR: " + msg );
    }

    @Override
    public void trace(String msg) throws IOException {
        msg = DateUtils.getTimeStamp() + ": " + msg + NEWLINE;
        Path path = Paths.get( _filePath );
        try (FileChannel fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )) {
            long pos = 0;
            if( fileChannel.size() == 0 ){
                pos = 0;
            }else{
                pos = fileChannel.size() - 1;
            }
            fileChannel.position(pos);
            FileLock lock = fileChannel.lock();
            fileChannel.write( ByteBuffer.wrap(msg.getBytes("UTF-8")) );
            lock.release();
            fileChannel.close();
        }
    }


}
