/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import static core.FileSystemUtils.NEWLINE;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author dario
 */
public class ConsoleLogger implements ILogger {

    private String _lockFilePath;

    public ConsoleLogger( String lockFilePath ){
        _lockFilePath = lockFilePath;
    }

    @Override
    public void error(String msg) throws IOException {
        trace( "ERROR: " + msg );
    }

    @Override
    public void trace(String msg) throws IOException {
        msg = DateUtils.getTimeStamp() + ": " + msg + NEWLINE;
        Path path = Paths.get( _lockFilePath );
        try (FileChannel fileChannel = FileChannel.open(
                path, StandardOpenOption.WRITE, StandardOpenOption.APPEND )) {
            FileLock lock = fileChannel.lock();
            System.out.println( msg );
            lock.release();
            fileChannel.close();
        }
    }

}
