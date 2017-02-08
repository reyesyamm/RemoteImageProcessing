package com.swyam.remoteimageprocessing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

public class SendDataActivity extends AppCompatActivity {

    Context context;
    CoordinatorLayout coordinatorsd;
    LinearLayout linear_center;
    String filename;
    int algorithm;
    int type_param;
    double param1;
    double param2;
    SharedPreferences settings_app;
    boolean ready_and_configured =true;

    ImageView image_view_preview_result;
    TextView text_view_filename,text_view_params, tv_status, tv_current_activity;
    ProgressBar progressBar;

    String server_ip;
    int server_port;
    int my_port;
    int limit_wait_miliseconds;
    boolean work_done = false;

    int w,h;

    String[] possible_fails_phase1 = new String[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_send_data);

        possible_fails_phase1[0] =getString(R.string.send_cant_read);
        possible_fails_phase1[1] =getString(R.string.send_server_timeout);
        possible_fails_phase1[2] =getString(R.string.standard_file_not_found);
        possible_fails_phase1[3] =getString(R.string.send_exception_output);
        possible_fails_phase1[4] =getString(R.string.send_fail_send);

        // receive data
        Intent intent = getIntent();
        filename = intent.getStringExtra(UtilsView.CURRENT_FILENAME_VAR);
        algorithm = intent.getIntExtra(UtilsView.CURRENT_ALGORITHM_VAR,-1);
        type_param = intent.getIntExtra(UtilsView.CURRENT_TYPE_PARAMS,0);
        param1 = intent.getDoubleExtra(UtilsView.CURRENT_PARAM1,0);
        param2 = intent.getDoubleExtra(UtilsView.CURRENT_PARAM2,0);

        // Initialize variables
        context = getBaseContext();
        coordinatorsd = (CoordinatorLayout) findViewById(R.id.coordinatorsd);
        image_view_preview_result = (ImageView) findViewById(R.id.image_view_preview_result);
        text_view_filename = (TextView) findViewById(R.id.text_view_filename);
        text_view_params = (TextView) findViewById(R.id.text_view_params);
        tv_status = (TextView) findViewById(R.id.text_view_status);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tv_current_activity = (TextView)findViewById(R.id.text_view_current_activity);
        linear_center = (LinearLayout) findViewById(R.id.linear_center);



        // load text for each textview

        text_view_filename.setText(filename);
        String params ="";
        switch (type_param){
            case 1: // Hough Transform
                params=getString(R.string.standard_th_name)+"\n"+getString(R.string.standard_threshold)+": "+param1+"\n"+getString(R.string.standard_max_lines)+": "+param2;
                break;
            case 2:
                params =getString(R.string.standard_ce_name)+"\n"+getString(R.string.standard_radius_x)+": "+param1+"\n"+getString(R.string.standard_radius_y)+": "+param2;
                break;
            case 3:
                params = getString(R.string.standard_ce_name)+"\n"+getString(R.string.standard_crop_x)+": "+param1+"\n"+getString(R.string.standard_crop_y)+": "+param2;
                break;
        }

        text_view_params.setText(params);
        tv_status.setText(getString(R.string.send_process_activity));



        // getting settings client-server
        settings_app = UtilsView.getSettingsApp(context);
        server_ip = settings_app.getString(UtilsView.SP_IP_SERVER,"");
        server_port = settings_app.getInt(UtilsView.SP_PORT_SERVER,0);
        my_port = settings_app.getInt(UtilsView.SP_MY_PORT,0);
        limit_wait_miliseconds = settings_app.getInt(UtilsView.SP_LIMIT,60);

        // configure progressbar
        progressBar.setMax(limit_wait_miliseconds); // we get limit time wait | predeterminated is 60 seconds
        progressBar.setProgress(0);

        // adjust to miliseconds the limit
        limit_wait_miliseconds*=1000; // 60 seconds => 60000 miliseconds (what socket.setSoTimeout(..) wait)


        if(server_ip.equals("") || server_port<=0 || my_port<=0){
            ready_and_configured= false;
            showSnack(getString(R.string.standard_settings_not_found),Snackbar.LENGTH_LONG,Color.RED);
            tv_status.setText(getString(R.string.standard_error_action));
        }else{
            send_data_to_server();
        }
        // handle click events on buttons

    }

    @Override
    public void onBackPressed() {
        if(work_done){
            super.onBackPressed();
        }else{
            showSnack(getString(R.string.send_task_running),Snackbar.LENGTH_SHORT,Color.BLACK);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        // Load thumbnail for display while ...
        w = image_view_preview_result.getWidth();
        h = image_view_preview_result.getHeight();
        String abs_filename = UtilsView.getAbsolutePathName(filename,context);
        Bitmap bm_thumb = UtilsView.getScaledBitmap(abs_filename,w,h);
        if(bm_thumb!=null){
            image_view_preview_result.setImageBitmap(bm_thumb);
        }else{
            image_view_preview_result.setImageResource(R.drawable.ic_menu_gallery);
        }
    }

    public void onPostSendedData(){
        //showSnack("The capture and dates have been sent!",Snackbar.LENGTH_LONG,Color.GREEN);
        //work_done = true;
        String p1 = my_port+"";
        String folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        String limit_miliseconds = limit_wait_miliseconds+"";

        AsyncTaskReceive receive = new AsyncTaskReceive();
        receive.execute(p1,folder,limit_miliseconds);

    }

    public void final_step(String filename_out){

        String current_path = UtilsView.getAbsolutePathName(filename_out,context);
        Bitmap bm_out = UtilsView.getScaledBitmap(current_path,w,h);
        if(bm_out!=null){
            image_view_preview_result.setImageBitmap(bm_out);
            Drawable drawable = image_view_preview_result.getDrawable();
            Rect bounds = drawable.getBounds();
            linear_center.animate().translationY(bounds.height()-linear_center.getHeight());

            tv_current_activity.setText(getString(R.string.send_file_received)+": '"+filename_out+"'");
            progressBar.setProgress(100);
            tv_status.setText(getString(R.string.standard_process_finished));
            text_view_params.setText("");
            showSnack(getString(R.string.send_file_saved),Snackbar.LENGTH_LONG,Color.GREEN);

            // Finally we add this returnment value to gallery scann

            try{
                String fstr = UtilsView.getAbsolutePathName(filename_out,context);
                File f = new File(fstr);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, filename_out);
                values.put(MediaStore.Images.Media.DESCRIPTION, "Result on Remote Action");
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
                values.put(MediaStore.Images.ImageColumns.BUCKET_ID, f.toString().toLowerCase(Locale.US).hashCode());
                values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, f.getName().toLowerCase(Locale.US));
                values.put("_data", f.getAbsolutePath());
                ContentResolver cr = getContentResolver();
                cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }catch(Exception ex){
                Log.e("result","Cannot be added the result to mediastore");
            }

        }else{
            showSnack(getString(R.string.standard_file_not_found).toUpperCase(),Snackbar.LENGTH_LONG,Color.RED);
        }
        work_done=true;
    }



    private void showSnack(String message, int duration, int color){
        try{
            Snackbar snack = Snackbar.make(coordinatorsd,message,duration);
            View sbView = snack.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(Color.WHITE);
            sbView.setBackgroundColor(color);
            textView.setBackgroundColor(color);
            snack.show();
        }catch(Exception ex){
            Log.e("snackbar",ex.getMessage().toString());
            Toast.makeText(context, message, (duration==Snackbar.LENGTH_LONG)?Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show();
        }
    }

    public void send_data_to_server(){
        Log.i("send_task","Trying to send");
        if(!work_done){
            AsyncTaskSend send_task = new AsyncTaskSend();
            String p1,p2,p3,p4,p5,p6,p7,p8,p9;
            p1 = filename;
            p2 = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            p3 = String.valueOf(type_param);
            p4 = String.valueOf(param1);
            p5 = String.valueOf(param2);
            p6 = String.valueOf(algorithm);
            p7 = server_ip;
            p8 = String.valueOf(server_port);
            p9 = String.valueOf(my_port);
            send_task.execute(p1,p2,p3,p4,p5,p6,p7,p8,p9);
        }

    }

    private void onPostFailSendData(int response_status){
        showSnack(possible_fails_phase1[response_status-2],Snackbar.LENGTH_LONG,Color.RED);
        work_done = true;
    }

    private class AsyncTaskSend extends AsyncTask<String, String, Integer>{

        @Override
        protected Integer doInBackground(String... params) { // 1 -> OK SUCCESS
            String filename = params[0];
            String folder_dir = params[1];
            int type_param = Integer.parseInt(params[2]);
            double param1 = Double.parseDouble(params[3]);
            double param2 = Double.parseDouble(params[4]);
            int algorithm = Integer.parseInt(params[5]);
            String server_ip = params[6];
            int server_port = Integer.parseInt(params[7]);
            int myport = Integer.parseInt(params[8]);
            int data_sent;

            File file = new File(folder_dir,filename);
            if(!file.exists()){
                return 2; // 2 -> CANT READ FILE
            }

            long file_length = file.length();

            Socket socket=null;
            try {
                publishProgress(getString(R.string.send_connecting_server),"30");
                //socket = new Socket(server_ip,server_port);
                socket = new Socket();
                socket.connect(new InetSocketAddress(server_ip,server_port),30000);
            } catch (IOException e) {
                e.printStackTrace();
                return 3; // 3 -> SERVER TIMEOUT CONECTION
            }

            InputStream in=null;
            try {
                in = new FileInputStream(file); // the file that we will send
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return 4; // FILE NOT FOUND
            }

            OutputStream out=null;
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return 5; // OUTPUTSTREAM EXCEPTION
            }


            DataOutputStream dos = new DataOutputStream(out);


            try {
                publishProgress(getString(R.string.send_sending_data),"50");
                dos.writeInt(filename.length()); // 0 -> length filename
                dos.writeBytes(filename); // 1 -> filename
                dos.writeInt(type_param); // 2 -> type_param
                dos.writeInt(algorithm); // 3 -> algorithm
                dos.writeDouble(param1); // 4 -> param1
                dos.writeDouble(param2); // 5 -> param2
                dos.writeInt(myport);    // 6 -> myport

                publishProgress(getString(R.string.send_data_sent),"100");


                byte[] buffer = new byte[8192]; // recomended
                int th = (int) (file_length/8192);
                int aum=0;
                int count;
                int progressv;
                publishProgress(getString(R.string.send_sending_capture),"0");
                while((count=in.read(buffer))>0){
                    out.write(buffer,0,count);
                    aum+=count;
                    progressv = (int) (100*aum/file_length);
                    publishProgress(getString(R.string.send_sending_capture),progressv+"");
                }
                publishProgress(getString(R.string.send_connection_closed),"100");

                data_sent = 1;
            } catch (IOException e) {
                e.printStackTrace();
                data_sent= 6;  // FAIL SENDING DATA
            }

            try{
                dos.close();
                out.close();
                in.close();
                socket.close();
            }catch(Exception ex){

            }
            return data_sent;
        }

        @Override
        protected void onProgressUpdate(String... progress){
            tv_current_activity.setText(progress[0]);
            progressBar.setProgress(Integer.parseInt(progress[1]));
        }

        @Override
        protected  void onPostExecute(Integer response_status){
            if(response_status==1){
                onPostSendedData();
                tv_current_activity.setText(getString(R.string.send_data_sent_success));
                progressBar.setProgress(100);
            }else{
                onPostFailSendData(response_status);
                tv_current_activity.setText(getString(R.string.send_error_sending_data));
                progressBar.setProgressDrawable(getDrawable(R.drawable.progressbarcolorred));
            }
        }
    }

    private class AsyncTaskReceive extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {

            int myport = Integer.parseInt(params[0]);
            String folder_dir = params[1];
            int limit_miliseconds = Integer.parseInt(params[2]);
            String name_out_file=null;
            try {
                publishProgress(getString(R.string.send_starting_listen),"0");
                ServerSocket socket = new ServerSocket(myport);
                socket.setSoTimeout(limit_miliseconds);
                publishProgress(getString(R.string.send_listening_in)+" "+myport+" "+getString(R.string.send_port),"10");
                Socket socketS = socket.accept();
                InputStream in = socketS.getInputStream();
                DataInputStream dis = new DataInputStream(in);
                OutputStream out=null;
                int status = dis.readInt();
                if(status==1){
                    int length_namefile = dis.readInt();
                    byte[] byte_name_file = new byte[length_namefile];
                    dis.read(byte_name_file);
                    name_out_file = new String(byte_name_file,"UTF-8");

                    File file_out = new File(folder_dir,name_out_file);

                    publishProgress(getString(R.string.send_file_output)+": '"+name_out_file+"'","20");
                    long file_length = dis.readLong();
                    byte[] buffer = new byte[8192];
                    int th = (int) (file_length/8192);
                    int count;
                    int aum=0;
                    out = new FileOutputStream(file_out);
                    int progressv;
                    while ((count = in.read(buffer)) > 0) {
                        out.write(buffer, 0, count);
                        aum+=count;
                        progressv = (int) (100*aum/file_length);
                        publishProgress(getString(R.string.send_receiving_image),progressv+"");
                    }
                }

                dis.close();
                in.close();
                if(out!=null)
                    out.close();
                socketS.close();



                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }


            return name_out_file;

        }

        @Override
        protected void onProgressUpdate(String... progress){
            tv_current_activity.setText(progress[0]);
            progressBar.setProgress(Integer.parseInt(progress[1]));
        }

        @Override
        protected  void onPostExecute(String filename){
            if(filename!=null){
                tv_current_activity.setText(getString(R.string.send_image_received_success));
                final_step(filename);
            }else{
                tv_current_activity.setText(getString(R.string.send_error_receiving));
                progressBar.setProgressDrawable(getDrawable(R.drawable.progressbarcolorred));
                work_done=true;
            }
            progressBar.setProgress(100);
        }
    }


}
