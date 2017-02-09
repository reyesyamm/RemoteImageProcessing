package com.swyam.remoteimageprocessing;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
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
    String filename;
    int algorithm;
    int type_param;
    double param1;
    double param2;
    SharedPreferences settings_app;
    boolean ready_and_configured =true;

    ImageView image_view_preview_result;
    TextView text_view_filename,text_view_params;

    String server_ip;
    int server_port;
    int my_port;
    int limit_wait_seconds;
    boolean work_done = false;
    AsyncTaskSend send_task = null;
    AsyncTaskReceive receive_task = null;

    int w,h;


    ProgressDialog progressDialog;

    String[] possible_fails_phase1 = new String[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_send_data);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        h = displaymetrics.heightPixels;
        w = displaymetrics.widthPixels;


        possible_fails_phase1[0] =getString(R.string.send_cant_read);
        possible_fails_phase1[1] =getString(R.string.send_server_timeout);
        possible_fails_phase1[2] =getString(R.string.standard_file_not_found);
        possible_fails_phase1[3] =getString(R.string.send_exception_output);
        possible_fails_phase1[4] =getString(R.string.send_fail_send);
        possible_fails_phase1[5] = getString(R.string.standard_action_canceled);
        possible_fails_phase1[6] = getString(R.string.standard_unknown_error);

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




        progressDialog = new ProgressDialog(this);

        progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("   ");
        progressDialog.setMessage("  ");
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.setProgressDrawable(getResources().getDrawable(R.drawable.progressbarcolor));
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL,getString(R.string.standard_text_cancel),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!work_done){
                    try{
                        send_task.cancel(true);
                    }catch(Exception ex){

                    }

                    try{
                        receive_task.cancel(true);
                    }catch(Exception ex){

                    }
                    work_done=true;
                }else{
                    progressDialog.dismiss();
                }
            }
        });


        // load text for each textview

        text_view_filename.setText(filename);
        String params ="";
        switch (type_param){
            case 1: // Hough Transform
                params=getString(R.string.standard_th_name)+", "+getString(R.string.standard_threshold)+": "+param1+", "+getString(R.string.standard_max_lines)+": "+param2;
                break;
            case 2:
                params =getString(R.string.standard_ce_name)+", "+getString(R.string.standard_radius_x)+": "+param1+", "+getString(R.string.standard_radius_y)+": "+param2;
                break;
            case 3:
                params = getString(R.string.standard_ce_name)+", "+getString(R.string.standard_crop_x)+": "+param1+", "+getString(R.string.standard_crop_y)+": "+param2;
                break;
        }

        text_view_params.setText(params);

        // getting settings client-server
        settings_app = UtilsView.getSettingsApp(context);
        server_ip = settings_app.getString(UtilsView.SP_IP_SERVER,"");
        server_port = settings_app.getInt(UtilsView.SP_PORT_SERVER,0);
        my_port = settings_app.getInt(UtilsView.SP_MY_PORT,0);
        limit_wait_seconds = settings_app.getInt(UtilsView.SP_LIMIT,60);

        // adjust to miliseconds the limit



        if(!work_done){
            //w = image_view_preview_result.getWidth();
            //h = image_view_preview_result.getHeight();
            String abs_filename = UtilsView.getAbsolutePathName(filename,context);
            Bitmap bm_thumb = UtilsView.getScaledBitmap(abs_filename,w,h);
            if(bm_thumb!=null){
                image_view_preview_result.setImageBitmap(bm_thumb);
            }else{
                image_view_preview_result.setImageResource(R.drawable.ic_menu_gallery);
            }


            if(server_ip.equals("") || server_port<=0 || my_port<=0){
                ready_and_configured= false;
                showSnack(getString(R.string.standard_settings_not_found),Snackbar.LENGTH_LONG,Color.RED);
            }else{
                Log.i("sending","executing sending");
                send_data_to_server();
            }
        }


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
        // Load thumbnail for display while ...
        super.onWindowFocusChanged(hasWindowFocus);

    }

    public void onPostSendedData(){
        String p1 = my_port+"";
        String folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        String limit_miliseconds = limit_wait_seconds+"";
        receive_task = new AsyncTaskReceive();
        receive_task.execute(p1,folder,limit_miliseconds);

    }

    public void final_step(String filename_out){

        String current_path = UtilsView.getAbsolutePathName(filename_out,context);
        Bitmap bm_out = UtilsView.getScaledBitmap(current_path,w,h);
        if(bm_out!=null){

            image_view_preview_result.setImageBitmap(bm_out);
            Drawable drawable = image_view_preview_result.getDrawable();
            Rect bounds = drawable.getBounds();

            text_view_params.setText("");
            showSnack(getString(R.string.send_file_saved),Snackbar.LENGTH_LONG,UtilsView.GREEN_50);

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

            progressDialog.dismiss();
        }else{
            progressDialog.setTitle(getString(R.string.standard_unknown_error));
            progressDialog.setMessage(getString(R.string.standard_file_not_found));
            progressDialog.setProgress(100);
            progressDialog.setProgressDrawable(getResources().getDrawable(R.drawable.progressbarcolorred));

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
            send_task = new AsyncTaskSend();
            String p1,p2,p3,p4,p5,p6,p7,p8,p9,p10;
            p1 = filename;
            p2 = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            p3 = String.valueOf(type_param);
            p4 = String.valueOf(param1);
            p5 = String.valueOf(param2);
            p6 = String.valueOf(algorithm);
            p7 = server_ip;
            p8 = String.valueOf(server_port);
            p9 = String.valueOf(my_port);
            p10 = limit_wait_seconds+"";
            send_task.execute(p1,p2,p3,p4,p5,p6,p7,p8,p9,p10);
        }

    }

    private void onPostFailSendData(int response_status){
        showSnack(possible_fails_phase1[response_status-1],Snackbar.LENGTH_LONG,Color.RED);
        work_done = true;
    }

    private class AsyncTaskSend extends AsyncTask<String, String, Integer>{

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

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
            int limit_wait = Integer.parseInt(params[9]);
            int data_sent;

            File file = new File(folder_dir,filename);
            if(!file.exists()){
                return 2; // 2 -> CANT READ FILE
            }

            long file_length = file.length();

            Socket socket=null;

            int limit_try =15;
            publishProgress(getString(R.string.send_connecting_server),getString(R.string.send_try_connection_server)+" 0/"+limit_try,"0");
            int current_try=0;
            int percent;
            boolean flag_connected = false;
            while(!isCancelled() && limit_try>current_try && !flag_connected){
                socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(server_ip,server_port),2000);
                    flag_connected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    current_try++;
                    percent = (current_try*100/limit_try);
                    publishProgress(getString(R.string.send_connecting_server),
                            getString(R.string.send_try_connection_server)+" "+current_try+"/"+limit_try,
                            percent+"");
                }
            }


            if(isCancelled()){
                return 6; // action canceled
            }else if(limit_try<=current_try){
                return 2; // server timeout
            }else if(socket==null){
                return 7; //  unknown error
            }

            // if not, les continue

            publishProgress(getString(R.string.send_sending_data),getString(R.string.send_sending_parameters),"0");

            InputStream in=null;
            try {
                in = new FileInputStream(file); // the file that we will send
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return 3; // FILE NOT FOUND
            }

            OutputStream out=null;
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    in.close();
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return 4; // OUTPUTSTREAM EXCEPTION
            }


            DataOutputStream dos = new DataOutputStream(out);


            try {
                publishProgress(getString(R.string.send_data_sent), getString(R.string.send_data_sent),  "50");
                dos.writeInt(filename.length()); // 0 -> length filename
                dos.writeBytes(filename); // 1 -> filename
                dos.writeInt(type_param); // 2 -> type_param
                dos.writeInt(algorithm); // 3 -> algorithm
                dos.writeDouble(param1); // 4 -> param1
                dos.writeDouble(param2); // 5 -> param2
                dos.writeInt(myport);    // 6 -> myport
                dos.writeInt(limit_wait); // 7 -> limit that the client will be listening for an answer

                publishProgress(getString(R.string.send_data_sent), getString(R.string.send_data_sent),  "100");


                byte[] buffer = new byte[8192]; // recomended
                int th = (int) (file_length/8192);
                int aum=0;
                int count;
                int progressv;
                publishProgress(getString(R.string.send_sending_capture),  getString(R.string.send_sending_capture_percent),   "0");
                while((count=in.read(buffer))>0 && !isCancelled()){
                    out.write(buffer,0,count);
                    aum+=count;
                    progressv = (int) (100*aum/file_length);
                    publishProgress(getString(R.string.send_sending_capture),    getString(R.string.send_sending_capture_percent),   progressv+"");
                }
                publishProgress(getString(R.string.send_data_sent_success),  getString(R.string.send_data_sent_success),  "100");
                out.flush();

                if(isCancelled()){
                    data_sent = 6;
                }else{
                    data_sent = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                data_sent= 5;  // FAIL SENDING DATA
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
            String title = progress[0];
            String description = progress[1];
            int progress_percent = Integer.parseInt(progress[2]);
            progressDialog.setTitle(title);
            progressDialog.setMessage(description);
            progressDialog.setProgress(progress_percent);
        }

        @Override
        protected  void onPostExecute(Integer response_status){
            if(response_status==0){
                onPostSendedData();
            }else{
                progressDialog.dismiss();
                onPostFailSendData(response_status);

            }
        }
    }

    private class AsyncTaskReceive extends AsyncTask<String,String,String>{

        @Override
        protected void onPreExecute() {
            progressDialog.setTitle(getString(R.string.send_starting_listen));
            progressDialog.setMessage(getString(R.string.send_listening_in)+" "+my_port);
            progressDialog.setProgress(0);
        }

        @Override
        protected String doInBackground(String... params) {

            int myport = Integer.parseInt(params[0]);
            String folder_dir = params[1];
            int limit_seconds = Integer.parseInt(params[2]);

            int try_listen = limit_seconds/1;
            int current_listen_time=0;
            boolean connected = false;
            ServerSocket socket = null;
            try{
                socket = new ServerSocket(myport);
                socket.setSoTimeout(1000);
            }catch(Exception ex){
                return "1"; // 1 -> current port is used for some other application
            }

            Socket socketS=null;
            int progress_list = 0;
            while(try_listen>current_listen_time && !isCancelled() && !connected){

                try {
                    socketS = socket.accept();
                    connected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    current_listen_time++;
                    progress_list = current_listen_time*100/try_listen;
                    publishProgress(getString(R.string.send_starting_listen),
                            (getString(R.string.send_listening_in)+" "+myport+". "+
                                    getString(R.string.send_remaining_time)
                                    +" "+(limit_seconds- current_listen_time)+" "+getString(R.string.sett_seconds))
                            ,progress_list+"");
                }

            }


            if(try_listen<=current_listen_time){
                try {
                    socket.close();
                }catch (Exception ex){

                }
                return "2"; // 2 -> nothing come from server
            }else if(isCancelled()){
                try {
                    socket.close();
                    socketS.close();
                }catch (Exception ex){

                }
                return "3"; // action canceled by user
            }

            if(!connected){
                return "2"; // same (nothing come...)
            }

            publishProgress(getString(R.string.send_receiving_image),
                    getString(R.string.send_receiving_image),
                    "0");
            InputStream in = null;
            DataInputStream dis = null;
            OutputStream out = null;

            try {
                in = socketS.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socketS.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }


                return "4"; // error trying to get inputstream

            }

            dis = new DataInputStream(in);
            String returnment_str="6"; // default 6 means that server doesnt do well his job but response to this client
            try {
                int status = dis.readInt();
                if(status==1){
                    int length_namefile = dis.readInt();
                    byte[] byte_name_file = new byte[length_namefile];
                    dis.read(byte_name_file);
                    returnment_str = new String(byte_name_file,"UTF-8");
                    File file_out = new File(folder_dir,returnment_str);
                    publishProgress(getString(R.string.send_file_output)+": "+returnment_str+"",
                            getString(R.string.send_receiving_image),
                            "0");


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
                        publishProgress(getString(R.string.send_file_output)+": "+returnment_str+"",
                                getString(R.string.send_receiving_image),
                                progressv+"");
                    }

                    dis.close();
                    in.close();
                    if(out!=null)
                        out.close();
                    socketS.close();
                    socket.close();

                    returnment_str = "0"+returnment_str;

                }

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socketS.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                return "5"; // error reading data from server
            }

            return returnment_str;

        }

        @Override
        protected void onProgressUpdate(String... progress){
            String title = progress[0];
            String description = progress[1];
            int progress_percent = Integer.parseInt(progress[2]);
            progressDialog.setTitle(title);
            progressDialog.setMessage(description);
            progressDialog.setProgress(progress_percent);
        }

        @Override
        protected  void onPostExecute(String filename){

            int status = Integer.parseInt(filename.charAt(0)+"");
            if(status==0){
                Log.i("namefileout",filename.substring(1, filename.length()));
                final_step(filename.substring(1, filename.length()));

            }else{
                String last_message_error="";
                switch (status){
                    case 1:
                        last_message_error = getString(R.string.send_error_rec_1);
                        break;
                    case 2:
                        last_message_error = getString(R.string.send_error_rec_2);
                        break;
                    case 3:
                        last_message_error = getString(R.string.send_error_rec_3);
                        break;
                    case 4:
                        last_message_error = getString(R.string.send_error_rec_4);
                        break;
                    case 5:
                        last_message_error = getString(R.string.send_error_rec_5);
                        break;
                    case 6:
                        last_message_error = getString(R.string.send_error_rec_6);
                        break;
                    default:
                        last_message_error = getString(R.string.send_error_receiving);

                }
                progressDialog.setProgress(100);
                progressDialog.setTitle(getString(R.string.send_error_receiving));
                progressDialog.setMessage(last_message_error);
                work_done=true;
            }
        }
    }


}
