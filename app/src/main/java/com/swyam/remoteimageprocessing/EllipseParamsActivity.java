package com.swyam.remoteimageprocessing;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class EllipseParamsActivity extends AppCompatActivity {

    Context context;
    CoordinatorLayout coordinator1;
    LinearLayout settings_image_ellipse;
    boolean efile;

    ImageView imageView;
    Drawable drawable;
    Rect imageBounds;
    //original height and width of the bitmap
    float OHeight;
    float OWidth;
    float scaledHeight;
    float scaledWidth;
    float heightRatio;
    float widthRatio;
    Paint paint,paint2;
    int have_base_px=0;
    int have_base_py=0;


    SeekBar seekBar_rx;
    SeekBar seekBar_ry;
    Switch switch_circle;
    Switch switch_adjust_rx;
    Switch switch_adjust_ry;
    Switch switch_have_base;
    Button btn_save;
    Bitmap image_bm;
    TextView text_view_rx;
    TextView text_view_ry;

    String filename;
    double rx;
    double ry;
    double cx;
    double cy;
    double deg;
    int type_param;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_ellipse_params);

        // Initialize variables
        context = getBaseContext();
        coordinator1 = (CoordinatorLayout) findViewById(R.id.coordinatorep);
        imageView = (ImageView) findViewById(R.id.image_view_capture);
        seekBar_rx = (SeekBar) findViewById(R.id.seekbar_rx);
        seekBar_ry = (SeekBar) findViewById(R.id.seekbar_ry);
        switch_circle = (Switch) findViewById(R.id.switch_circle);
        switch_adjust_rx = (Switch) findViewById(R.id.switch_adjust_rx);
        switch_adjust_ry = (Switch) findViewById(R.id.switch_adjust_ry);
        switch_have_base = (Switch) findViewById(R.id.switch_have_base);

        btn_save = (Button) findViewById(R.id.btn_save_params);
        text_view_rx = (TextView) findViewById(R.id.text_view_rx);
        text_view_ry = (TextView) findViewById(R.id.text_view_ry);
        settings_image_ellipse = (LinearLayout) findViewById(R.id.settings_image_ellipse);
        settings_image_ellipse.setVisibility(View.VISIBLE);

        // get data intent
        Intent intent = getIntent();
        filename = intent.getStringExtra(UtilsView.CURRENT_FILENAME_VAR);
        rx = intent.getDoubleExtra(UtilsView.CURRENT_PARAM1,50);
        ry = intent.getDoubleExtra(UtilsView.CURRENT_PARAM2,50);
        if(rx<=0){
            rx=50;
        }

        if(ry<=0){
            ry=50;
        }
        type_param = intent.getIntExtra(UtilsView.CURRENT_TYPE_PARAMS,2);
        switch_have_base.setChecked(type_param==3);
        set_configurations_to_have_base(type_param==3);

        efile=!(filename==null);

        if(efile){
            image_bm = UtilsView.getBitmap(filename,context);
            imageView.setImageBitmap(image_bm);
            load_image(); // we load the file on imageview
        }else{
            showSnack(getString(R.string.standard_file_not_found),Snackbar.LENGTH_LONG,Color.RED);
        }

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double param1 = (type_param==2)?rx:have_base_px;
                double param2 = (type_param==2)?ry:have_base_py;
                Intent intent = new Intent(EllipseParamsActivity.this, NewProcessActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,filename);
                intent.putExtra(UtilsView.CURRENT_ALGORITHM_VAR,1);
                intent.putExtra(UtilsView.CURRENT_TYPE_PARAMS,type_param);
                intent.putExtra(UtilsView.CURRENT_PARAM1,param1);
                intent.putExtra(UtilsView.CURRENT_PARAM2,param2);

                startActivity(intent);

            }
        });


        seekBar_rx.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getBaseContext(),"progress srx: "+srx.getProgress(),Toast.LENGTH_SHORT).show();
                rx = seekBar_rx.getProgress();
                if(switch_circle.isChecked()){
                    ry = rx;
                    seekBar_ry.setProgress((int) rx);
                }


                draw_initial_ellipse();
            }
        });


        seekBar_ry.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getBaseContext(),"progress sry: "+sry.getProgress(),Toast.LENGTH_SHORT).show();
                ry = seekBar_ry.getProgress();
                if(switch_circle.isChecked()){
                    rx = ry;
                    seekBar_rx.setProgress((int) ry);
                }


                draw_initial_ellipse();
            }
        });

        imageView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeDown() {
                //Toast.makeText(EllipseParamsActivity.this, "Down", Toast.LENGTH_SHORT).show();
                settings_image_ellipse.animate()
                        .translationY(settings_image_ellipse.getHeight());
            }

            @Override
            public void onSwipeLeft(float length, float x) {

                if(type_param==2){
                    if(cx>x){ // aumentar radio x
                        rx+=length;
                        rx = (rx<OWidth)?rx:OWidth-50;
                    }else if(cx<x){  // reducir radio x
                        rx-=length;
                        rx = (rx>=50)?rx:50;
                    }

                    seekBar_rx.setProgress((int)rx);


                    draw_initial_ellipse();
                }
            }

            @Override
            public void onSwipeUp() {
                //Toast.makeText(EllipseParamsActivity.this, "Up", Toast.LENGTH_SHORT).show();
                settings_image_ellipse.animate()
                        .translationY(0);
            }

            @Override
            public void onSwipeRight(float length, float x) {
                if(type_param==2){
                    if(cx>x){ // reducir
                        rx-=length;
                        rx = (rx>=50)?rx:50;
                    }else if(cx<x){ // aumentar
                        rx+=length;
                        rx = (rx<OWidth)?rx:OWidth-50;
                    }
                    seekBar_rx.setProgress((int)rx);
                    draw_initial_ellipse();
                }
            }

            @Override
            public void onTouchE(MotionEvent e){

                if(type_param==2 && (switch_adjust_ry.isChecked() || switch_adjust_rx.isChecked())){
                    float difx,dify;
                    difx = (float) Math.abs(cx-correct_coord_x(e.getX()))/2;
                    dify = (float) Math.abs(cy-correct_coord_y(e.getY()))/2;
                    boolean adjusted =false;
                    if(difx>dify){
                        if(switch_adjust_rx.isChecked() && difx>50 && difx<OWidth){
                            rx = (int)difx;
                            seekBar_rx.setProgress((int)rx);
                            adjusted=true;
                        }
                    }else if(dify>difx){
                        if(switch_adjust_ry.isChecked() && dify>50 && dify<OHeight){
                            ry = (int)dify;
                            seekBar_ry.setProgress((int)ry);
                            adjusted=true;
                        }
                    }

                    if(adjusted){
                        draw_initial_ellipse();
                    }
                }

            }

            @Override
            public void onDoubleTouchE(MotionEvent e){
                if(efile){
                    adjust_params(e.getX(), e.getY());
                }
            }
        });

        switch_adjust_rx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch_circle.setChecked(false);
            }
        });

        switch_adjust_ry.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch_circle.setChecked(false);
            }
        });


        switch_circle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                rx = Math.max(rx,ry);
                ry = rx;
                draw_initial_ellipse();
            }
        });

        switch_have_base.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    Toast.makeText(context, getString(R.string.params_enclose_base), Toast.LENGTH_LONG).show();
                    type_param=3;
                }else{
                    type_param=2;
                }
                set_configurations_to_have_base(isChecked);
                draw_initial_ellipse();
            }
        });

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

    public void load_image(){
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint2.setColor(Color.RED);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(20);

    }

    public void draw_initial_ellipse(){

        try{
            Bitmap tempBitmap = Bitmap.createBitmap((int)OWidth, (int)OHeight, Bitmap.Config.RGB_565);
            Canvas tempCanvas = new Canvas(tempBitmap);
            tempCanvas.drawBitmap(image_bm, 0, 0, null);
            //RectF oval1 = new RectF(cx-rx,cy-ry,cx+(rx*2),cy+(ry*2));
            if(type_param==3){
                tempCanvas.drawLine((float)cx,0.0f,(float)cx,(OHeight-1),paint);
                tempCanvas.drawLine(0.0f,(float)cy,(OWidth-1),(float)cy,paint);
                tempCanvas.drawLine((float)cx,0.0f,(float)cx,(float)cy,paint2);
                tempCanvas.drawLine(0.0f,(float)cy,(float)(cx),(float)cy,paint2);

            }else{
                RectF oval1 = new RectF((int)(cx-rx) ,(int)(cy-ry), (int)(cx+rx),(int)(cy+ry));
                Log.i("radios","rx: "+rx+", ry: "+ry);
                Log.i("radios2","rx2: "+correct_coord_x((float)rx)+", ry2: "+correct_coord_y((float)ry));

                Log.i("centros","cx: "+cx+", cy: "+cy);

                tempCanvas.drawOval(oval1, paint);
            }

            setTextRadious();

            imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void adjust_params(float scaled_cx, float scaled_cy){
        float scaledImageOffsetX = (scaled_cx - imageBounds.left);
        float scaledImageOffsetY = (scaled_cy - imageBounds.top);
        cx = (scaledImageOffsetX * widthRatio);
        cy = (scaledImageOffsetY * heightRatio);
        draw_initial_ellipse();
        if(type_param==3){
            have_base_px = (int) cx;
            have_base_py = (int) cy;
        }
    }

    public float correct_coord_x(float x){
        return widthRatio*(x-imageBounds.left);
    }

    public float correct_coord_y(float y){
        return heightRatio*(y-imageBounds.top);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        drawable = imageView.getDrawable();

        imageBounds = drawable.getBounds();

        OHeight = drawable.getIntrinsicHeight();
        OWidth = drawable.getIntrinsicWidth();

        /** values for seekbars **/
        seekBar_rx.setMax((int)OWidth/3);
        seekBar_ry.setMax((int)OHeight/3);

        scaledHeight = imageBounds.height();
        scaledWidth = imageBounds.width();

        cx = (int) OWidth/2;
        cy = (int) OHeight/2;

        seekBar_rx.setProgress((int) rx);
        seekBar_ry.setProgress((int) ry);

        heightRatio = OHeight / scaledHeight;
        widthRatio = OWidth / scaledWidth;

        Log.i("elipse","original -> h: "+OHeight+", w: "+OWidth);
        Log.i("elipse","scaled -> h: "+scaledHeight+", w: "+scaledWidth);
        Log.i("elipse","elipse center -> cx: "+cx+", cy:"+cy);
        Log.i("elipse","ratio -> h: "+heightRatio+", w:"+widthRatio);

        draw_initial_ellipse();
    }

    public void setTextRadious(){
        text_view_rx.setText(getString(R.string.standard_radius_x)+": "+rx);
        text_view_ry.setText(getString(R.string.standard_radius_y)+": "+ry);
    }


    public void set_configurations_to_have_base(boolean v){
        switch_adjust_rx.setEnabled(!v);
        switch_adjust_ry.setEnabled(!v);
        switch_circle.setEnabled(!v);
        seekBar_rx.setEnabled(!v);
        seekBar_ry.setEnabled(!v);
    }


}
