package com.swyam.remoteimageprocessing;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    EditText ip,port,myport,limit;
    Button btn_save;
    Context context;
    CoordinatorLayout coordinator1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
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
                finish();
            }
        });

        context = getBaseContext();
        ip = (EditText)findViewById(R.id.edit_text_ip_server);
        port = (EditText)findViewById(R.id.edit_text_port_server);
        myport = (EditText)findViewById(R.id.edit_text_my_port);
        limit = (EditText)findViewById(R.id.edit_text_limit_wait_answer);
        btn_save = (Button)findViewById(R.id.btn_save_settings);
        coordinator1 = (CoordinatorLayout) findViewById(R.id.coordinator1);
        load_predeterminated();


        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str_ip = ip.getText().toString();
                int str_port = UtilsView.to_int(port.getText().toString());
                int str_myport = UtilsView.to_int(myport.getText().toString());
                int  str_limit = UtilsView.to_int(limit.getText().toString());
                if(validIp(str_ip) && str_port>0 && str_myport>0 && str_limit>0){
                    UtilsView.saveSettings(str_ip,str_port,str_myport, str_limit,context);
                    showSnack(getString(R.string.standard_saved_params),Snackbar.LENGTH_LONG,Color.GREEN);
                }else{
                    showSnack(getString(R.string.standard_error_action),Snackbar.LENGTH_LONG,Color.RED);
                }
            }
        });

    }


    public void load_predeterminated(){
        SharedPreferences settings = UtilsView.getSettingsApp(context);
        ip.setText(settings.getString(UtilsView.SP_IP_SERVER,""));
        port.setText( String.valueOf( settings.getInt(UtilsView.SP_PORT_SERVER,0)));
        myport.setText( String.valueOf( settings.getInt(UtilsView.SP_MY_PORT,0)));
        limit.setText( String.valueOf( settings.getInt(UtilsView.SP_LIMIT,0)));
    }


    public boolean validIp(String str_ip){
        String[] octs = str_ip.split("\\.");
        if(octs.length!=4)
            return false;

        int[] io=null;
        try{
            io=new int[]{
                    Integer.parseInt(octs[0]),
                    Integer.parseInt(octs[1]),
                    Integer.parseInt(octs[2]),
                    Integer.parseInt(octs[3]),
            };
        }catch (Exception ex){
            return false;
        }

        return (io[0]>=1 && io[0]<256) && (io[1]>=0 && io[1]<256) && (io[2]>=0 && io[2]<256) && (io[3]>=0 && io[3]<256);
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


}
