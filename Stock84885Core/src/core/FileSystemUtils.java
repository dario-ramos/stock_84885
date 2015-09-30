/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.File;
import java.net.URISyntaxException;

/**
 *
 * @author dario
 */
public class FileSystemUtils {

    public static String getCurrentDir() throws URISyntaxException{
        String dir = new File(
            Configuration.class.getProtectionDomain().getCodeSource().
                getLocation().toURI().getPath()
        ).getParent();
        return dir;
    }

}
