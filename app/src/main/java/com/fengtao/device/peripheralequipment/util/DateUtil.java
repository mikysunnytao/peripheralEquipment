package com.fengtao.device.peripheralequipment.util;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    //获取当前时间字符串
    public static String getCurrDateStr(){
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
        return format.format(date);
    }
}
