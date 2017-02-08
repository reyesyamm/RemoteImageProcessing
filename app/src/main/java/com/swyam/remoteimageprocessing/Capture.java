package com.swyam.remoteimageprocessing;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * Created by Reyes Yam on 03/02/2017.
 */
public class Capture {
    Bitmap thumb_path;
    String name_file;
    String capture_date;
    int executed_algorithm;

    public Capture(Bitmap thumb_path, String name_file, String capture_date, int capt_v){
        this.thumb_path = thumb_path;
        this.name_file = name_file;
        this.capture_date = capture_date;
        this.executed_algorithm = capt_v;
    }

}
