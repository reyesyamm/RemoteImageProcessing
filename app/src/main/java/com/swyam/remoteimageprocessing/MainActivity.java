package com.swyam.remoteimageprocessing;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int MY_PERMISSIONS_CAMERA = 0;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    DrawerLayout drawer;
    LinearLayoutManager llm;
    Context context;
    RecyclerView rv;
    RVAdapter adapter;
    FloatingActionButton fab;
    Button btn_new_capture;
    TextView tv_number_captures;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /* Initialize all variables */
        rv = (RecyclerView)findViewById(R.id.my_rv);
        context = getBaseContext();
        fab = (FloatingActionButton) findViewById(R.id.fab);
        llm = new LinearLayoutManager(context);
        btn_new_capture = (Button) findViewById(R.id.btn_new_capture);
        tv_number_captures = (TextView) findViewById(R.id.text_view_number_captures);

        // manager for recycler view
        rv.setLayoutManager(llm);


        /* Camera and Write Permission Request */
        locationpermission();

        /* Load dates on recyclerview  */
        File my_dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File[] my_dir_files = null;
        try{
            my_dir_files = my_dir.listFiles();
        }catch(Exception ex){
            showSnack(getString(R.string.main_imposible_read_capture_directory),Snackbar.LENGTH_LONG,Color.RED);
        }

        fill_recycler(my_dir_files);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        btn_new_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!UtilsView.server_ip_configured(context)){
                    Snackbar.make(drawer, getString(R.string.standard_settings_not_found), Snackbar.LENGTH_SHORT).setAction(getString(R.string.standard_configure), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            start_settings_activity();
                        }
                    }).show();
                }else{
                    Intent intent = new Intent(MainActivity.this, NewProcessActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            showSnack("Will import from gallery",Snackbar.LENGTH_SHORT,Color.BLACK);
        } else if (id == R.id.nav_gallery) {
            showSnack("Will show al results runned",Snackbar.LENGTH_SHORT,Color.BLACK);
        } else if (id == R.id.nav_manage) {
            start_settings_activity();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void fill_recycler(File[] files){
        int number_captures=0;
        if(files.length>0) {
            List<Capture> list = new ArrayList<>();
            //DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy hh:mm a");
            String file_name_tmp;
            String file_date_tmp;
            for (int i = 0; i < files.length; i++) {
                file_name_tmp = files[i].getName();
                if (file_name_tmp.substring(0, 3).equals("RIP")) {
                    number_captures++;
                    file_date_tmp = dateFormat.format(files[i].lastModified());
                    int exec_num = UtilsView.AlgorithmsExecutedInt(files[i]);
                    list.add(new Capture(UtilsView.getThumbBitmap("THUMB" + file_name_tmp,context), file_name_tmp, file_date_tmp,exec_num));
                }
            }
            adapter = new RVAdapter(context,list);
            rv.setAdapter(adapter);

            adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(Capture capture) {
                    showPopUp(capture);
                }

                @Override
                public void onLongItemClick(Capture capture, int position){
                    showDialogConfirmDelete(capture.name_file,position);
                }
            });
        }

        tv_number_captures.setText(number_captures+"");
    }


    private void locationpermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_CAMERA);
            }
        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique

            return;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)     {
        switch (requestCode) {
            case MY_PERMISSIONS_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnack(getString(R.string.standard_camera_permission_granted),Snackbar.LENGTH_LONG,Color.GREEN);
                } else {
                    showSnack(getString(R.string.standard_camera_permission_deny),Snackbar.LENGTH_LONG,Color.GREEN);
                }
                return;
            }
        }
    }

    public void start_settings_activity(){
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void showSnack(String message, int duration, int color){
        try{
            Snackbar snack = Snackbar.make(drawer,message,duration);
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

    public void showPopUp(final Capture capture){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog_rv);
        dialog.setTitle(capture.name_file);

        ImageView image = (ImageView) dialog.findViewById(R.id.image_view_preview_thumb_dialog);
        if(capture.thumb_path!=null){
            image.setImageBitmap(capture.thumb_path);
        }else{
            image.setImageResource(R.drawable.ic_menu_gallery);
        }

        Button btn_process = (Button) dialog.findViewById(R.id.btn_select_for_process);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel_dialog);
        Button btn_view = (Button) dialog.findViewById(R.id.btn_view_related_images);

        btn_process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, NewProcessActivity.class);
                intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,capture.name_file);
                startActivity(intent);
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btn_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, ViewResultsActivity.class);
                intent.putExtra(UtilsView.CURRENT_FILENAME_VAR,capture.name_file);
                startActivity(intent);
            }
        });

        dialog.show();
    }

    public void showDialogConfirmDelete(final String capture_name, final int position){
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.standard_delete_confirmation))
                .setMessage(getString(R.string.standard_delete_question))
                .setPositiveButton(getString(R.string.standard_yes), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(UtilsView.deleteCapture(capture_name,context)){
                            adapter.removeAt(position);
                            showSnack(getString(R.string.main_capture_deleted),Snackbar.LENGTH_SHORT,Color.GREEN);
                        }else{
                            showSnack(getString(R.string.standard_error_action),Snackbar.LENGTH_SHORT,Color.RED);
                        }
                    }

                })
                .setNegativeButton(getString(R.string.standard_no), null)
                .show();
    }

}
