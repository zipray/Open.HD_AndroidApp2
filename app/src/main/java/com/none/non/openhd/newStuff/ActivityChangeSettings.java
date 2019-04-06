package com.none.non.openhd.newStuff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.none.non.openhd.R;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActivityChangeSettings extends AppCompatActivity implements TCPClient.ProcessMessage {

    //ArrayList<AbstractSetting> mSelectedSyncSettings;

    Button bRefresh;
    Button bApply;
    Button bPing;
    Switch sSyncGroundOnly;
    TableLayout tableLayout;
    Context context;
    ArrayList<ArrayList<AbstractSetting>> ALL_SYNCHRONIZED_SETTINGS;
    ArrayList<AbstractSetting> mSelectedSyncSettings;

    private final TCPClient client=new TCPClient(this,this);
    private AtomicBoolean connectionEstablished=new AtomicBoolean(false);

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.activity_change_settings);
        //Note: call OPENHD_SETTINGS_1 after setContentView !
        ALL_SYNCHRONIZED_SETTINGS=new ArrayList<>();
        ALL_SYNCHRONIZED_SETTINGS.add(SettingsFactory.OPENHD_SETTINGS_1(this));
        ALL_SYNCHRONIZED_SETTINGS.add(SettingsFactory.OPENHD_SETTINGS_2(this));
        ALL_SYNCHRONIZED_SETTINGS.add(SettingsFactory.OPENHD_OSD_Settings(this));
        for(final ArrayList<AbstractSetting> list:ALL_SYNCHRONIZED_SETTINGS){
            for(final AbstractSetting setting:list){
                setting.getKeyView().setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(checkConnectedAndMessageUser()){
                            client.sendMessage(Message.BuildMessageGET(sSyncGroundOnly.isChecked(),setting.KEY));
                        }
                        return false;
                    }
                });
            }
        }
        mSelectedSyncSettings =ALL_SYNCHRONIZED_SETTINGS.get(0);
        tableLayout=findViewById(R.id.tableLayout);
        //Populate the layout with all synchronized settings values
        for(final AbstractSetting setting: mSelectedSyncSettings){
            tableLayout.addView(setting.tableRow);
            //Disable the edit text - only as soon as it is initialized with its default currentValue (confirmed by both ground and air) we enable it
            setting.reset();
        }
        TabLayout tabLayout=findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tableLayout.removeAllViews();
                mSelectedSyncSettings =ALL_SYNCHRONIZED_SETTINGS.get(tab.getPosition());
                //Populate the layout with all synchronized settings values
                for(final AbstractSetting setting: mSelectedSyncSettings){
                    tableLayout.addView(setting.tableRow);
                    //Disable the edit text - only as soon as it is initialized with its default currentValue (confirmed by both ground and air) we enable it
                    setting.reset();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        bRefresh=findViewById(R.id.buttonRefresh);
        bApply=findViewById(R.id.buttonApply);
        sSyncGroundOnly=findViewById(R.id.switchOnlySyncGround);
        sSyncGroundOnly.setChecked(true);
        bPing=findViewById(R.id.buttonPing);
        bPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkConnectedAndMessageUser()){
                    client.sendMessage(Message.BuildMessageHELLO(sSyncGroundOnly.isChecked()));
                }
            }
        });
        bRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkConnectedAndMessageUser()){
                    //Disable all views
                    //send GET message for all synchronized settings
                    for(final AbstractSetting setting: mSelectedSyncSettings){
                        setting.reset();
                        client.sendMessage(Message.BuildMessageGET(sSyncGroundOnly.isChecked(),setting.KEY));
                    }
                }
            }
        });
        bApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkConnectedAndMessageUser()){
                    return;
                }
                final ArrayList<AbstractSetting> modifiedSettings=new ArrayList<>();
                for(final AbstractSetting setting: mSelectedSyncSettings){
                    if(setting.hasBeenUpdatedByUser()){
                        modifiedSettings.add(setting);
                    }
                }
                if(modifiedSettings.isEmpty()){
                    Toast.makeText(context,"Change settings first",Toast.LENGTH_SHORT).show();
                }else{
                    StringBuilder messageToUser= new StringBuilder("Do you want to change these values:\n");
                    for(final AbstractSetting setting: modifiedSettings){
                        messageToUser.append(setting.KEY).append(" ").append(setting.getCurrentValue()).append("\n");
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(true);
                    builder.setMessage(messageToUser.toString());
                    builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for(final AbstractSetting setting:modifiedSettings){
                                client.sendMessage(Message.BuildMessageCHANGE(sSyncGroundOnly.isChecked(),setting.KEY,setting.getCurrentValue()));
                                //setting.reset();
                            }
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }


    private boolean checkConnectedAndMessageUser(){
        if(!connectionEstablished.get()){
            Toast.makeText(context, "Please connect your device first", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        client.start(context);
    }


    @Override
    protected void onPause(){
        super.onPause();
        client.stop();
        client.joinSafe();
    }




    private void processMessageGET_OK(final boolean ground,final String key,final String value){
        //find the matching setting and call its processing function
        for(final AbstractSetting setting: mSelectedSyncSettings){
            if(key.equals(setting.KEY)){
                setting.processMessageGET_OK(ground,value,sSyncGroundOnly.isChecked());
                break;
            }
        }
    }

    private void processMessageCHANGE_OK(final boolean ground,final String key,final String value){
        //find the matching setting and call its processing function
        for(final AbstractSetting setting: mSelectedSyncSettings){
            if(key.equals(setting.KEY)){
                setting.processMessageCHANGE_OK(ground,value,sSyncGroundOnly.isChecked());
                break;
            }
        }
    }

    @Override
    public void processMessage(final String messageData) {
        System.out.println("Received from server:"+ messageData);
        final Message message=new Message(messageData);
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (message.cmd) {
                    case "HELLO":{
                        client.sendMessage(Message.BuildMessageHELLO_OK());
                        break;
                    }
                    case "HELLO_OK":{
                        Toast.makeText(context,message.src+" "+message.cmd,Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case "GET_OK": {
                        processMessageGET_OK(message.ground(),message.dataKey,message.dataValue);
                        break;
                    }
                    case "CHANGE_OK": {
                        processMessageCHANGE_OK(message.ground(),message.dataKey,message.dataValue);
                        break;
                    }
                    default:
                        System.out.println("Unknown command"+message.toString());
                        break;
                }
            }
        });
    }


    @Override
    public void OnConnectionEstablished() {
        connectionEstablished.set(true);
        System.out.println("Connection established");
    }

    @Override
    public void OnConnectionClosed(){
        System.out.println("Connection closed");
        connectionEstablished.set(false);
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(final AbstractSetting setting: mSelectedSyncSettings){
                    setting.reset();
                }
            }
        });
    }


}