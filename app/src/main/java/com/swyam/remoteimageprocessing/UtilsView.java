package com.swyam.remoteimageprocessing;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Reyes Yam on 03/02/2017.
 */
public class UtilsView {

    public static final String ID_SP_SETTINGS ="UserConfig";
    public static final String SP_IP_SERVER = "ip_server";
    public static final String SP_PORT_SERVER = "port_server";
    public static final String SP_MY_PORT ="my_port";
    public static final String SP_LIMIT = "limit_wait";

    public static final String CURRENT_FILENAME_VAR = "CURRENT_FILENAME";
    public static final String CURRENT_ALGORITHM_VAR = "CURRENT_ALGORITHM";
    public static final String CURRENT_PARAM1 = "CURRENT_PARAM_1";
    public static final String CURRENT_PARAM2 = "CURRENT_PARAM_2";
    public static final String CURRENT_TYPE_PARAMS ="CURRENT_TYPE_PARAM";

    public static final int THUMBNAIL_SIZE=480;



    // some colors
    public static int GREEN_50 = Color.rgb(0,128,0);




    public static Bitmap getThumbBitmap(String file_name_TEMP, Context context){
        String file_name = file_name_TEMP;
        Bitmap bm = null;
        if(file_name.substring(0,5).equals("THUMB")){
            File fl = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),file_name);
            if(fl.exists()){
                bm = BitmapFactory.decodeFile(fl.getAbsolutePath());
            }
        }
        return bm;
    }

    public static Bitmap getBitmap(String file_name_TEMP, Context context){
        String file_name = file_name_TEMP;
        Bitmap bm = null;
        File fl = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),file_name);
        if(fl.exists()){
            bm = BitmapFactory.decodeFile(fl.getAbsolutePath());
        }
        return bm;
    }

    public static Bitmap generateThumbnailBitmap(String filename_PATH){
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename_PATH, bounds);
        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
            return null;
        }
        int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
                : bounds.outWidth;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = originalSize / THUMBNAIL_SIZE;
        return BitmapFactory.decodeFile(filename_PATH, opts);
    }

    public static boolean WriteBitmapThumb(Bitmap bm, String filename, Context context){
        boolean returnment = true;
        String str_filename_thumb = "THUMB"+filename;

        try{

            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File filename_thumb = new File(storageDir,str_filename_thumb);
           // Log.i("filename",filename_thumb.getAbsolutePath());

            FileOutputStream out = null;

            try {
                out = new FileOutputStream(filename_thumb);
                bm.compress(Bitmap.CompressFormat.JPEG, 90, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (Exception e) {
                e.printStackTrace();
                returnment = false;
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }catch(Exception ex){
            returnment = false;
            Log.e("namefile",str_filename_thumb);
        }

        return returnment;
    }

    public static String getAbsolutePathName(String filename,Context context){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File fl = new File(storageDir,filename);
        if(fl.exists()){
            return fl.getAbsolutePath();
        }
        return "";
    }



    public static boolean server_ip_configured(Context context){
        SharedPreferences sett = getSettingsApp(context);
        String ip_server = sett.getString("ip_server","").toString();
        int port_server = sett.getInt("port_server",0);
        int my_port = sett.getInt("my_port",0);

        return (!ip_server.equals("") && port_server>0 && my_port>0);
    }

    public static SharedPreferences getSettingsApp(Context context, String str_id){
        SharedPreferences sett = context.getSharedPreferences(str_id,0);
        return sett;
    }


    public static SharedPreferences getSettingsApp(Context context){
        SharedPreferences sett = context.getSharedPreferences(ID_SP_SETTINGS,0);
        return sett;
    }

    public static void saveSettings(String ip, int port, int myport, int limit,Context context){
        SharedPreferences settings = context.getSharedPreferences(ID_SP_SETTINGS,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SP_IP_SERVER,ip);
        editor.putInt(SP_PORT_SERVER,port);
        editor.putInt(SP_MY_PORT,myport);
        editor.putInt(SP_LIMIT,limit);
        editor.commit();

    }

    public static int to_int(String str){
        int ret=0;
        try{
            ret = Integer.parseInt(str);
        }catch (Exception ex){

        }

        return ret;
    }

    public static boolean deleteCapture(String str_filename,Context context){
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File imgFile = new File(storageDir,str_filename);
        if(imgFile.exists() && imgFile.delete()){
            File imgFilethumb = new File(storageDir,"THUMB"+str_filename);
            if(imgFilethumb.exists()){
                imgFilethumb.delete();
            }

            File imgFilethumb2 = new File(storageDir,"TH_"+str_filename);
            if(imgFilethumb2.exists()){
                imgFilethumb2.delete();
            }

            File imgFilethumb3 = new File(storageDir,"CE_"+str_filename);
            if(imgFilethumb3.exists()){
                imgFilethumb3.delete();
            }


            return true;
        }

        return false;
    }

    public static String crop2square(String filename, Context context, int x1, int y1, int length, boolean overwrite){
        String file_out_name=null;
        Bitmap bm = getBitmap(filename,context);
        if(bm!=null){
            Bitmap cropped = Bitmap.createBitmap(bm,x1,y1,length,length);
            String new_filename;
            if(overwrite){
                deleteCapture(filename,context);
                new_filename = filename;
            }else{
                new_filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                new_filename = "RIP_JPEG_SQ_" + new_filename + ".jpg";
            }

            if(writeBitmapCropped(cropped,new_filename,context)){
                file_out_name = new_filename;
            }
        }

        return file_out_name;
    }

    public static boolean writeBitmapCropped(Bitmap bm, String filename, Context context){
        boolean returnment = true;

        try{

            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File filename_thumb = new File(storageDir,filename);

            FileOutputStream out = null;

            try {
                out = new FileOutputStream(filename_thumb);
                bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
                returnment = false;
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }catch(Exception ex){
            returnment = false;
            Log.e("namefile",filename);
        }

        return returnment;
    }

    public static int AlgorithmsExecutedInt(File fl){
        int nexec=0;
        String abs_path_file = fl.getAbsolutePath();
        String storage_dir = abs_path_file.substring(0,abs_path_file.lastIndexOf('/')+1);
        String filename_th = "TH_"+fl.getName();
        String filename_ce = "CE_"+fl.getName();
        File fth = new File(storage_dir,filename_th);
        File fce = new File(storage_dir,filename_ce);

        if(fth.exists())
            nexec++;
        if(fce.exists())
            nexec++;

        return nexec;
    }


    public static Bitmap getScaledBitmap(String mCurrentPhotoPath, int targetW, int targetH) {
        // Get the dimensions of the View
        //int targetW = imageSwitcher.getWidth();
        //int targetH = imageSwitcher.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        return bitmap;
    }


}
