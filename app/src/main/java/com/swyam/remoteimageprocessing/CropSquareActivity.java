package com.swyam.remoteimageprocessing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CropSquareActivity extends AppCompatActivity {

    Context context;
    CoordinatorLayout coordinatorcs;
    LinearLayout linear_almost_finish;
    Button btn_finish_cropping;
    TextView text_view_status_cropping;
    ProgressBar progressBar;

    ImageView image_view_preview_result;
    Switch switch_overwrite_original_capture;
    Button btn_save_square_crop;
    SeekBar seekbar_dim_square;

    String filename;
    int algorithm;
    int type_param;
    double param1;
    double param2;
    String filename_out;


    float OWidth,OHeight, SWidth, SHeight,RatioWitdh, RatioHeigth;
    float x1,y1,x2,y2;
    Paint paint1,paint2;
    Bitmap image_bm;
    Drawable drawable;
    Rect imageBounds;
    float max_size_square, lenght_sq;

    int TRANSLATE_PRED_LENGTH = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_crop_square);

        // Initialize
        context = getBaseContext();
        coordinatorcs = (CoordinatorLayout) findViewById(R.id.coordinatorcs);
        linear_almost_finish = (LinearLayout) findViewById(R.id.linear_almost_finish);
        btn_finish_cropping = (Button) findViewById(R.id.btn_finish_cropping);
        text_view_status_cropping = (TextView) findViewById(R.id.text_view_status_cropping);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);



        image_view_preview_result = (ImageView) findViewById(R.id.image_view_preview_result);
        switch_overwrite_original_capture = (Switch) findViewById(R.id.switch_overwrite_original_capture);
        btn_save_square_crop = (Button) findViewById(R.id.btn_save_square_crop);
        seekbar_dim_square = (SeekBar) findViewById(R.id.seekbar_dim_square);

        // get params
        Intent intent = getIntent();
        filename = intent.getStringExtra(UtilsView.CURRENT_FILENAME_VAR);
        algorithm = intent.getIntExtra(UtilsView.CURRENT_ALGORITHM_VAR,0); // default TH
        type_param = intent.getIntExtra(UtilsView.CURRENT_TYPE_PARAMS,1);  // default TH
        param1 = intent.getDoubleExtra(UtilsView.CURRENT_PARAM1,0);
        param1 = intent.getDoubleExtra(UtilsView.CURRENT_PARAM2,0);

        if(filename!=null){
            image_bm = UtilsView.getBitmap(filename,context);
            image_view_preview_result.setImageBitmap(image_bm);
            paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint1.setColor(Color.BLUE);
            paint1.setStyle(Paint.Style.STROKE);
            paint1.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
            paint1.setStrokeWidth(20);

            paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint2.setColor(Color.BLACK);
            paint2.setStyle(Paint.Style.FILL);
            paint2.setStrokeWidth(1);
            paint2.setAlpha(127);
        }



        image_view_preview_result.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeDown() {
                translate_swipe_direction(2);
            }

            @Override
            public void onSwipeLeft(float length, float x) {
                translate_swipe_direction(3);
            }

            @Override
            public void onSwipeUp() {
                translate_swipe_direction(0);
            }

            @Override
            public void onSwipeRight(float length, float x) {
                translate_swipe_direction(1);
            }

            @Override
            public void onTouchE(MotionEvent e){
                translate_square(e.getX(),e.getY(),false);
            }

            @Override
            public void onDoubleTouchE(MotionEvent e){

            }
        });

        seekbar_dim_square.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int new_dim=seekbar_dim_square.getProgress();
                change_dim_square(new_dim);
            }
        });

        btn_save_square_crop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                btn_save_square_crop.setEnabled(false);
                seekbar_dim_square.setEnabled(false);
                linear_almost_finish.animate()
                        .translationX(0);

                seekbar_dim_square.setVisibility(View.GONE);
                btn_save_square_crop.setVisibility(View.GONE);
                switch_overwrite_original_capture.setVisibility(View.GONE);

                setProgress(0);

                text_view_status_cropping.setText(getString(R.string.crop_cropping));


                String as_filename=filename;
                String as_path_dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
                String as_x = String.valueOf((int)x1);
                String as_y = String.valueOf((int)y1);
                String as_length = String.valueOf((int)lenght_sq);
                String as_overwrite = String.valueOf(switch_overwrite_original_capture.isChecked());

                AsyncTaskCrop cropp_background_task = new AsyncTaskCrop();
                cropp_background_task.execute(as_filename, as_path_dir, as_x, as_y, as_length, as_overwrite);

            }
        });

        btn_finish_cropping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(CropSquareActivity.this, NewProcessActivity.class);
                if(filename_out!=null){
                    intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,filename_out);
                }else{
                    intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,filename);
                }

                intent.putExtra(UtilsView.CURRENT_ALGORITHM_VAR,algorithm);
                intent.putExtra(UtilsView.CURRENT_TYPE_PARAMS,type_param);
                intent.putExtra(UtilsView.CURRENT_PARAM1,param1);
                intent.putExtra(UtilsView.CURRENT_PARAM2,param2);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
         // Once the activity is displayed correctly we proceed to get correspondent values and more
        super.onWindowFocusChanged(hasWindowFocus);
        drawable = image_view_preview_result.getDrawable();

        imageBounds = drawable.getBounds();

        OHeight = drawable.getIntrinsicHeight();
        OWidth = drawable.getIntrinsicWidth();
        linear_almost_finish.animate()
                .translationX(OWidth);

        btn_finish_cropping.setEnabled(false);
        btn_finish_cropping.setText(getString(R.string.crop_cropping));
        text_view_status_cropping.setText(getString(R.string.crop_on_edit_mode));

        max_size_square = Math.min(OHeight,OWidth)-1;

        SHeight = imageBounds.height();
        SWidth = imageBounds.width();

        RatioHeigth = OHeight / SHeight;
        RatioWitdh = OWidth / SWidth;

        // Initial values for coordinates to crop
        lenght_sq = (int)(max_size_square/2);

        // top left corner
        x1 = 0;
        y1 = 0;

        // bottom right corner
        x2 = lenght_sq;
        y2 = lenght_sq;



        seekbar_dim_square.setMax((int)max_size_square);
        seekbar_dim_square.setProgress((int)lenght_sq);
        draw_rectangle_contour();
    }

    public void draw_rectangle_contour(){
        try{
            Bitmap tempBitmap = Bitmap.createBitmap((int)OWidth, (int)OHeight, Bitmap.Config.RGB_565);
            Canvas tempCanvas = new Canvas(tempBitmap);
            tempCanvas.drawBitmap(image_bm, 0, 0, null);

            tempCanvas.drawRect(x1,y1,x2,y2,paint1); // we draw the rect that enclose the square segment to crop on the image
            // we set some level of opacity to the other part
            tempCanvas.drawRect(0,0,OWidth-1,y1+1,paint2);
            tempCanvas.drawRect(0,y1,x1,OHeight-1,paint2);
            tempCanvas.drawRect(x1,y2,OWidth-1,OHeight-1,paint2);
            tempCanvas.drawRect(x2,y1,OWidth-1,y2,paint2);
            image_view_preview_result.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void translate_square(float iv_x, float iv_y, boolean real_coords){

        float x= iv_x;
        float y = iv_y;
        if(!real_coords){
            x = RatioWitdh*(iv_x-imageBounds.left);
            y = RatioHeigth*(iv_y-imageBounds.top);
        }

        float temp_x1 = x - length_square()/2;
        float temp_x2 = temp_x1 + length_square();
        float temp_y1 = y - length_square()/2;
        float temp_y2 = temp_y1 + length_square();

        adjust_coordinates(temp_x1,temp_y1, temp_x2,temp_y2);

        draw_rectangle_contour();
    }

    public void adjust_coordinates(float temp_x1, float temp_y1, float temp_x2, float temp_y2){
        float l = length_square();
        if(temp_x1>=0 && temp_y1>=0){
            if(temp_x2<OWidth && temp_y2<OHeight){
                x1 = temp_x1;
                x2 = temp_x2;
                y1 = temp_y1;
                y2 = temp_y2;
                //Toast.makeText(context, "normal",Toast.LENGTH_SHORT).show();
            }else{
                if(temp_x2>=OWidth){
                    x1 = OWidth - 1 - l;  // case 4 included
                    x2 = OWidth -1;
                    if(temp_y2>=OHeight){ // case 5
                        y1 = OHeight - 1 -l;
                        y2 = OHeight-1;
                        //Toast.makeText(context, "case 5",Toast.LENGTH_SHORT).show();
                    }else{
                        y1=temp_y1;
                        y2=temp_y2;
                        //Toast.makeText(context, "case 4",Toast.LENGTH_SHORT).show();
                    }
                }else if(temp_y2>=OHeight){ // case 6
                    y1 = OHeight-1-l;
                    y2 = OHeight-1;
                    x1 = temp_x1;
                    x2 = temp_x2;
                    //Toast.makeText(context, "case 6",Toast.LENGTH_SHORT).show();
                }
            }
        }else{
            if(temp_x1<0 && temp_y1<0){ // case 1
                x1 = 0;
                y1 = 0;
                x2 = l;
                y2 = l;
                //Toast.makeText(context, "case 1",Toast.LENGTH_SHORT).show();
            }else if(temp_x1<0){
                x1 = 0; // case 8 included
                x2 = l;
                if(y2>=OHeight){ // case 7
                    y1 = OHeight-1-l;
                    y2 = OHeight-1;
                    //Toast.makeText(context, "case 7",Toast.LENGTH_SHORT).show();
                }else{
                    y1 = temp_y1;
                    y2 = temp_y2;
                    //Toast.makeText(context, "case 8",Toast.LENGTH_SHORT).show();
                }
            }else if(temp_y1<0){
                y1 = 0; // case 2 included
                y2 = l;
                if(temp_x2>=OWidth){ // case3
                    x1 = OWidth-1-l;
                    x2 = OWidth-1;
                    //Toast.makeText(context, "case 3",Toast.LENGTH_SHORT).show();
                }else{
                    x1 = temp_x1;
                    x2 = temp_x2;
                    //Toast.makeText(context, "case 2",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public float length_square(){
        return lenght_sq;
    }

    public void change_dim_square(int dim){
        float cx = x2 - length_square()/2;
        float cy = y2 - length_square()/2;
        lenght_sq = (dim>50)?dim:50;
        translate_square(cx,cy,true);
    }

    public void translate_swipe_direction(int direction){
        // 0 -> up
        // 1 -> right
        // 2 -> down
        // 3 -> left
        float cx = x2 - length_square()/2;
        float cy = y2 - length_square()/2;

        int aug_x = (direction%2)*TRANSLATE_PRED_LENGTH*((direction==1)?1:-1);
        int aug_y = ((1+direction)%2)*TRANSLATE_PRED_LENGTH*((direction==2)?1:-1);

        cx +=aug_x;
        cy +=aug_y;

        translate_square(cx,cy,true);

    }

    // Class for async file cropping


    private class AsyncTaskCrop extends AsyncTask<String,Integer,String>{



        @Override
        protected String doInBackground(String... params) {
            String out_filename=null;
            String filename = params[0];
            String path_dir = params[1];
            int x = Integer.parseInt(params[2]);
            int y = Integer.parseInt(params[3]);
            int length = Integer.parseInt(params[4]);
            boolean overwrite = Boolean.parseBoolean(params[5]);
            publishProgress(10);
            File file = new File(path_dir,filename);
            Bitmap bm=null;
            if(file.exists()){
                bm = BitmapFactory.decodeFile(file.getAbsolutePath());
            }

            if(bm!=null){

                Bitmap cropped = Bitmap.createBitmap(bm,x,y,length,length);
                publishProgress(20);
                String new_filename;
                if(overwrite){
                    file.delete();
                    File thumb = new File(path_dir,"THUMB"+filename);
                    if(thumb.exists())
                        thumb.delete();

                    new_filename = filename;
                }else{
                    new_filename = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    new_filename = "RIP_JPEG_SQ_" + new_filename + ".jpg";
                }

                publishProgress(50);
                File cropped_new_file = new File(path_dir,new_filename);
                FileOutputStream out = null;

                try {
                    out = new FileOutputStream(cropped_new_file);
                    cropped.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (Exception e) {
                    e.printStackTrace();
                    new_filename = null;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                out_filename = new_filename;
            }


            return out_filename;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setProgress(progress[0]);
        }


        @Override
        protected void onPostExecute(String filename){
            if(filename!=null){
                text_view_status_cropping.setText(getString(R.string.crop_square_captured));
                progressBar.setProgress(100);
                filename_out = filename;
                btn_finish_cropping.setBackgroundResource(R.drawable.custom_button);
            }else{
                text_view_status_cropping.setText(getString(R.string.standard_error_action));
                filename_out = null;
            }
            btn_finish_cropping.setEnabled(true);
            btn_finish_cropping.setText(getString(R.string.crop_finalize_and_return));

        }
    }


}
