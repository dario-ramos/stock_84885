/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author dario
 */
public class DateUtils {

    public static String getTimeStamp(){
        Date date= new Date();
        return new Timestamp(date.getTime()).toString();
    }

}
