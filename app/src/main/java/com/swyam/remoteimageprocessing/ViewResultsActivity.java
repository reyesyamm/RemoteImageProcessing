package com.swyam.remoteimageprocessing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.io.File;
import java.util.HashMap;

public class ViewResultsActivity extends AppCompatActivity {
    Context context;

    ImageSwitcher imageSwitcher;
    int previous_activity;
    TextView tv_current_image, tv_dates_algorithm_image;
    ImageButton btn_next_image,btn_previous_image;
    FloatingActionButton fab;

    String filename;
    String[] list_algorithm = new String[3];
    String[] list_pref_file_algorithm = new String[]{"","TH_","CE_"};
    boolean exists_file;
    int current_algorithm_index;
    boolean elements_shown=true;
    HashMap<Integer,Drawable> images;
    int targetW,targetH;
    boolean exists_current_file = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_view_results);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        list_algorithm[0] = getString(R.string.main_capture);
        list_algorithm[1] = getString(R.string.standard_th_name);
        list_algorithm[2] = getString(R.string.standard_ce_name);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        imageSwitcher = (ImageSwitcher) findViewById(R.id.image_view_preview_result);
        tv_current_image = (TextView) findViewById(R.id.tv_current_image);
        btn_next_image = (ImageButton) findViewById(R.id.btn_next_image);
        btn_previous_image = (ImageButton) findViewById(R.id.btn_previous_image);
        tv_dates_algorithm_image = (TextView) findViewById(R.id.tv_dates_algorithm_image);
        context = getBaseContext();
        images = new HashMap<>();

        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView myView = new ImageView(getApplicationContext());
                myView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                myView.setLayoutParams(new
                        ImageSwitcher.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT,
                        Toolbar.LayoutParams.MATCH_PARENT));
                return myView;
            }
        });

        Intent intent = getIntent();
        filename = intent.getStringExtra(UtilsView.CURRENT_FILENAME_VAR);
        previous_activity = intent.getIntExtra("previous_activity",0);





        btn_next_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               move_nex();
            }
        });

        btn_previous_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move_previous();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(exists_current_file){
                    String str_file_to_share = list_pref_file_algorithm[current_algorithm_index]+filename;
                    String abs_path_file_to_share = UtilsView.getAbsolutePathName(str_file_to_share, context);
                    Uri uri = Uri.parse("file://"+abs_path_file_to_share);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    share.setType("image/*");
                    share.putExtra(Intent.EXTRA_STREAM, uri);

                    Intent new_intent = Intent.createChooser(share, getString(R.string.view_share_via));
                    new_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(new_intent);
                }
            }
        });



        imageSwitcher.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeLeft(float length, float x) {
                move_nex();
            }

            @Override
            public void onSwipeRight(float length, float x) {
                move_previous();
            }

            @Override
            public void onTouchE(MotionEvent e){
                animation_display_elements();
            }
        });
    }


    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        targetW = imageSwitcher.getWidth();
        targetH = imageSwitcher.getHeight();

        String abs_path_name = UtilsView.getAbsolutePathName(filename,context);
        if(abs_path_name.length()>0){
            Bitmap bm_original_scaled = UtilsView.getScaledBitmap(abs_path_name,targetW,targetH);
            Drawable drawable1 = new BitmapDrawable(getResources(),bm_original_scaled);

            exists_file=true;
            current_algorithm_index=0;
            images.put(0,drawable1);

            for(int i=1;i<list_pref_file_algorithm.length;i++){
                String tmp_abs_file = UtilsView.getAbsolutePathName(list_pref_file_algorithm[i]+filename,context);
                Log.d("file_out_name",list_pref_file_algorithm[i]+filename);
                if(tmp_abs_file.length()>0){
                    Bitmap bm_current_algorithm_scaled = UtilsView.getScaledBitmap(tmp_abs_file,targetW,targetH);
                    Drawable draw_temp = new BitmapDrawable(getResources(),bm_current_algorithm_scaled);
                    images.put(i,draw_temp);
                }else{
                    images.put(i,null);
                }
            }

            change_image();
        }else{
            Toast.makeText(context,getString(R.string.standard_file_not_found),Toast.LENGTH_LONG).show();
            exists_file=false;
        }

        if(!exists_file){
            btn_previous_image.setEnabled(false);
            btn_next_image.setEnabled(false);
            fab.setEnabled(false);
        }
    }


    public void move_nex(){

        imageSwitcher.setInAnimation(this, R.anim.slide_in_left);
        imageSwitcher.setOutAnimation(this, R.anim.slide_out_left);

        if(current_algorithm_index==list_algorithm.length-1){
            current_algorithm_index=0;
        }else{
            current_algorithm_index++;
        }
        change_image();
    }

    public void move_previous(){
        imageSwitcher.setInAnimation(this, R.anim.slide_in_right);
        imageSwitcher.setOutAnimation(this, R.anim.slide_out_right);

        if(current_algorithm_index==0){
            current_algorithm_index = list_algorithm.length-1;
        }else{
            current_algorithm_index--;
        }

        change_image();
    }

    public void change_image(){
        if(images.get(current_algorithm_index)!=null){
            imageSwitcher.setImageDrawable(images.get(current_algorithm_index));
            tv_dates_algorithm_image.setText(list_pref_file_algorithm[current_algorithm_index]+filename);
            tv_dates_algorithm_image.setTextColor(Color.GREEN);
            exists_current_file = true;
        }else{
            imageSwitcher.setImageDrawable(images.get(0));
            tv_dates_algorithm_image.setText(getString(R.string.view_algorithm_not_executed));
            tv_dates_algorithm_image.setTextColor(Color.RED);
            exists_current_file = false;
        }
        tv_current_image.setText(list_algorithm[current_algorithm_index]);
    }


    public void animation_display_elements(){
        if(elements_shown){
            btn_next_image.animate().translationX(100);
            btn_previous_image.animate().translationX(-100);
            fab.animate().translationY(100);
        }else{
            btn_next_image.animate().translationX(0);
            btn_previous_image.animate().translationX(0);
            fab.animate().translationY(0);
        }

        elements_shown=!elements_shown;
    }

    @Override
    public void onBackPressed(){
        //super.onBackPressed();
        Intent intent = null;
        if(previous_activity==1){ // from gallery
            intent = new Intent(ViewResultsActivity.this, MainActivity.class);
        }else{
            intent = new Intent(ViewResultsActivity.this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }





}
