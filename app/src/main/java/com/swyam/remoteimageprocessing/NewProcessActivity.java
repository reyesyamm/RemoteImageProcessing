package com.swyam.remoteimageprocessing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewProcessActivity extends AppCompatActivity {

    CoordinatorLayout coordinator1;
    Context context;
    List<String> list_options =  new ArrayList<String>();
    List<String> list_algoritms =  new ArrayList<String>();
    Button btn_start_camera, btn_chage_params, btn_send_to_server;
    Spinner sp_list_options, sp_list_algorithms;
    TextView text_preview_params, text_file_name;
    ImageView preview;
    Switch switch_have_base;

    // GLOBAL VARIABLES FOR CONFIGURATIONS
    String file_name;
    String file_abs_path="";
    int algorithm_selected;
    double param1,param2;
    int type_param; // 1 -> normal TH params, 2 -> Rx and Ry params, 3 -> have a base on image (param1,param2) play a role of coordinates for cut image

    // for the camera picutre
    static final int REQUEST_TAKE_PHOTO  = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_process);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                back_to_main_activity();
            }
        });

        // Inicialize variables
        context = getBaseContext();
        coordinator1 = (CoordinatorLayout) findViewById(R.id.coordinatornp);
        btn_start_camera = (Button) findViewById(R.id.btn_start_camera);
        btn_chage_params = (Button) findViewById(R.id.btn_change_params);
        btn_send_to_server = (Button) findViewById(R.id.btn_send_to_server);
        sp_list_options = (Spinner) findViewById(R.id.spinner_list_options_image);
        sp_list_algorithms = (Spinner) findViewById(R.id.spinner_list_algorithms);
        text_preview_params = (TextView) findViewById(R.id.text_view_label_params);
        text_file_name = (TextView) findViewById(R.id.text_view_file_name);
        preview = (ImageView) findViewById(R.id.image_view_preview_thumb);
        switch_have_base = (Switch) findViewById(R.id.switch_have_base);
        // Load options availables on every spinner
        load_data_options_spinners();

        // load possible values configurations
        load_values();

        // dispatch click events on buttons
        btn_start_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        ex.printStackTrace();
                    }

                    if (photoFile != null) {
                        file_abs_path = photoFile.getAbsolutePath();
                        Uri photoURI = FileProvider.getUriForFile(getBaseContext(), "com.swyam.remoteimageprocessing.fileprovider", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }else{
                        file_name ="";
                        showSnack(getString(R.string.process_error_creating_image),Snackbar.LENGTH_LONG,Color.BLACK);
                    }

                }
            }
        });

        btn_chage_params.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater li = LayoutInflater.from(context);
                View promptsView = li.inflate(R.layout.prompt_params, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(NewProcessActivity.this);
                alertDialogBuilder.setView(promptsView);

                final TextView text_title = (TextView) promptsView.findViewById(R.id.text_view_algorithm_name);
                final TextView text_param1 = (TextView) promptsView.findViewById(R.id.text_view_param1_caption);
                final TextView text_param2 = (TextView) promptsView.findViewById(R.id.text_view_param2_caption);
                final EditText edit_param1 = (EditText) promptsView.findViewById(R.id.edit_text_param1_value);
                final EditText edit_param2 = (EditText) promptsView.findViewById(R.id.edit_text_param2_value);

                text_title.setText(sp_list_algorithms.getSelectedItem().toString());
                String str_p1 = algorithm_selected==0?(getString(R.string.standard_threshold) + " "):( getString(R.string.standard_radius_x)+" ");
                String str_p2 = algorithm_selected==0?(getString(R.string.standard_max_lines)+" "):( getString(R.string.standard_radius_y)+" ");
                if(type_param==3){
                    str_p1 = getString(R.string.standard_crop_x)+" ";
                    str_p2 = getString(R.string.standard_crop_y)+" ";
                }
                text_param1.setText(str_p1);
                text_param2.setText(str_p2);
                edit_param1.setText(param1+"");
                edit_param2.setText(param2+"");

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.standard_save),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        // get user input and set it to result
                                        double out_p1 = Double.parseDouble(edit_param1.getText().toString());
                                        double out_p2 = Double.parseDouble(edit_param2.getText().toString());
                                        if(out_p1>=0 && out_p2>=0){
                                            param1 = out_p1;
                                            param2 = out_p2;
                                            set_text_params_preview();
                                            showSnack(getString(R.string.process_params_updated),Snackbar.LENGTH_SHORT,Color.BLACK);
                                        }else{
                                            showSnack(getString(R.string.process_invalid_values),Snackbar.LENGTH_SHORT,Color.BLUE);
                                        }
                                    }
                                })
                        .setNegativeButton(getString(R.string.standard_no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();

            }
        });

        btn_send_to_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UtilsView.server_ip_configured(context)){
                    String str_alg = (algorithm_selected==0)?getString(R.string.standard_th_name):getString(R.string.standard_ce_name);
                    String str_type_params = (type_param==1)?getString(R.string.process_thres_maxlines):((type_param==3)?getString(R.string.process_cropxy):getString(R.string.process_radiusxy));
                    str_type_params = getString(R.string.process_with)+" "+str_type_params+" "+getString(R.string.standard_saved_params);
                    String str_params = "("+param1+","+param2+")";


                    String message=(getString(R.string.standard_algorithm)+": "+str_alg+"\n"+str_type_params+"\n"+str_params);
                    new AlertDialog.Builder(NewProcessActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.process_send_confirm))
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.standard_yes), new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(NewProcessActivity.this, SendDataActivity.class);
                                    intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,file_name);
                                    intent.putExtra(UtilsView.CURRENT_TYPE_PARAMS,type_param);
                                    intent.putExtra(UtilsView.CURRENT_ALGORITHM_VAR,algorithm_selected);
                                    intent.putExtra(UtilsView.CURRENT_PARAM1,param1);
                                    intent.putExtra(UtilsView.CURRENT_PARAM2,param2);
                                    startActivity(intent);
                                }

                            })
                            .setNegativeButton(getString(R.string.standard_no), null)
                            .show();


                }else{
                    showSnack(getString(R.string.standard_settings_not_found),Snackbar.LENGTH_LONG, Color.RED);
                }
            }
        });

        // spinner selections
        sp_list_algorithms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()  {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                algorithm_selected = position;
                if(position==1){
                    switch_have_base.setEnabled(true);
                    type_param = (switch_have_base.isChecked())?3:2;

                }else{
                    switch_have_base.setEnabled(false);
                    type_param=1;
                }
                set_text_params_preview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sp_list_options.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 1: // adjust elipse params
                        if(file_name.length()>0 && algorithm_selected==1){
                            Intent intent = new Intent(NewProcessActivity.this, EllipseParamsActivity.class);
                            intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,file_name);
                            intent.putExtra(UtilsView.CURRENT_TYPE_PARAMS,type_param);
                            intent.putExtra(UtilsView.CURRENT_PARAM1,param1);
                            intent.putExtra(UtilsView.CURRENT_PARAM2,param2);
                            startActivity(intent);
                        }else{
                            showSnack(getString(R.string.process_just_available_ce),Snackbar.LENGTH_LONG,Color.BLUE);
                        }
                        break;
                    case 2: // crop image
                        if(file_name.length()>0){
                            Intent intent = new Intent(NewProcessActivity.this, CropSquareActivity.class);
                            intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,file_name);
                            intent.putExtra(UtilsView.CURRENT_TYPE_PARAMS,type_param);
                            intent.putExtra(UtilsView.CURRENT_ALGORITHM_VAR,algorithm_selected);
                            startActivity(intent);
                        }else{
                            showSnack(getString(R.string.process_no_capture_selected),Snackbar.LENGTH_LONG,Color.BLUE);
                        }
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sp_list_options.setSelection(0);
            }
        });

        switch_have_base.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    type_param=3;
                }else{
                    type_param=2;
                }
                set_text_params_preview();
            }
        });

    }

    @Override
    public void onBackPressed() {
        back_to_main_activity();
    }

    public void back_to_main_activity(){
        Intent intent = new Intent(NewProcessActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void showSnack(String message, int duration, int color){
        try{
            Snackbar snack = Snackbar.make(coordinator1,message,duration);
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

    private void load_data_options_spinners(){
        list_options.add(getString(R.string.process_selecte_one));
        list_options.add(getString(R.string.process_get_ellipse_params));
        list_options.add(getString(R.string.process_crop_image_2_square));

        list_algoritms.add(getString(R.string.standard_th_name));
        list_algoritms.add(getString(R.string.standard_ce_name));

        ArrayAdapter<String> adapter_lo = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list_options);
        adapter_lo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_list_options.setAdapter(adapter_lo);

        ArrayAdapter<String> adapter_la = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list_algoritms);
        adapter_la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_list_algorithms.setAdapter(adapter_la);

    }

    private void load_values(){
        Intent this_intent = getIntent();
        String filename = this_intent.getStringExtra(UtilsView.CURRENT_FILENAME_VAR);
        int algorithm = this_intent.getIntExtra(UtilsView.CURRENT_ALGORITHM_VAR,0);
        double param1 = this_intent.getDoubleExtra(UtilsView.CURRENT_PARAM1,0.0);
        double param2 = this_intent.getDoubleExtra(UtilsView.CURRENT_PARAM2,0.0);
        this.type_param = this_intent.getIntExtra(UtilsView.CURRENT_TYPE_PARAMS,1); // default is TH (1)
        this.param1 = param1;
        this.param2 = param2;
        if(filename!=null &&  filename.length()>0 && algorithm>=0){
            Bitmap bm = UtilsView.getThumbBitmap("THUMB"+filename,context);
            if(bm==null){
                bm = UtilsView.generateThumbnailBitmap(getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/"+filename);
                UtilsView.WriteBitmapThumb(bm,filename,context);
            }

            preview.setImageBitmap(bm);

            file_abs_path = UtilsView.getAbsolutePathName(filename,context);
            text_file_name.setText(filename);
            if(algorithm>=0 && algorithm<list_algoritms.size())
                sp_list_algorithms.setSelection(algorithm);
            else
                algorithm=0;

            this.algorithm_selected = algorithm;
            this.file_name = filename;

        }else{
            this.algorithm_selected=0;
            this.file_name="";
        }

        switch_have_base.setEnabled(type_param==2 || type_param==3);
        switch_have_base.setChecked(type_param==3);


        set_text_params_preview();
    }

    private void set_text_params_preview(){
        String str_prev_params="";
        if(algorithm_selected==0){
            str_prev_params=getString(R.string.standard_threshold)+": "+param1+"\n"+getString(R.string.standard_max_lines)+": "+param2;
        }else{
            if(type_param==2)
                str_prev_params=getString(R.string.standard_radius_x)+": "+param1+"\n"+getString(R.string.standard_radius_y)+": "+param2;
            else
                str_prev_params=getString(R.string.standard_crop_x)+": "+param1+"\n"+getString(R.string.standard_crop_y)+": "+param2;
        }
        text_preview_params.setText(str_prev_params);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String imageFileName = "RIP_JPEG_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir,imageFileName);

        this.file_name = image.getName();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            add_to_galery();
            text_file_name.setText(getString(R.string.standard_wait_moment));

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Bitmap bm=null;
                    try{
                /*
                * First lets generate a thumbnail
                * */

                        BitmapFactory.Options bounds = new BitmapFactory.Options();
                        bounds.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(file_abs_path, bounds);
                        if ((bounds.outWidth == -1) || (bounds.outHeight == -1)) {
                            bm = null;
                        }else{
                            int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
                                    : bounds.outWidth;
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = originalSize / UtilsView.THUMBNAIL_SIZE;
                            bm = BitmapFactory.decodeFile(file_abs_path, opts);
                            //preview.setImageBitmap(bm);
                        }

                        /**
                         * Second - save the Thumbnail
                         */

                        if(bm!=null){

                            String str_filename_thumb = "THUMB"+file_name;
                            try{

                                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                                File filename_thumb = new File(storageDir,str_filename_thumb);
                                FileOutputStream out = null;

                                try {
                                    out = new FileOutputStream(filename_thumb);
                                    bm.compress(Bitmap.CompressFormat.JPEG, 90, out); // bmp is your Bitmap instance
                                    // PNG is a lossless format, the compression factor (100) is ignored
                                } catch (Exception e) {
                                    e.printStackTrace();
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
                                Log.e("namefile",str_filename_thumb);
                            }
                        }

                    }catch (Exception ex){
                        showSnack(getString(R.string.process_fail_thumb_creation), Snackbar.LENGTH_SHORT,Color.RED);
                    }


                    update_preview_new_process(bm);

                }
            }, 2000);
        }
    }

    private void add_to_galery() {
        /*Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(file_abs_path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);*/
        try{
            File f = new File(file_abs_path);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, file_name);
            values.put(MediaStore.Images.Media.DESCRIPTION, "No description");
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
            values.put(MediaStore.Images.ImageColumns.BUCKET_ID, f.toString().toLowerCase(Locale.US).hashCode());
            values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, f.getName().toLowerCase(Locale.US));
            values.put("_data", f.getAbsolutePath());

            ContentResolver cr = getContentResolver();
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }catch(Exception ex){
            Toast.makeText(context, getString(R.string.standard_fail_add_media_store), Toast.LENGTH_SHORT).show();
        }
    }

    public void update_preview_new_process(Bitmap bm){
        if(bm!=null){
            preview.setImageBitmap(bm);
            text_file_name.setText(file_name);
        }
    }


}
