package com.none.non.openhd.newStuff;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.ArrayRes;
import android.support.annotation.ColorInt;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.none.non.openhd.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Abstract setting
 */

@SuppressWarnings("WeakerAccess")
public class ASetting implements AdapterView.OnItemSelectedListener,TextWatcher {
    private static final String NOT_LOADED="Setting not loaded";
    private static final String NOT_IN_SYNC="Not in sync";
    private final Context context;
    public final String KEY;
    //Text view holding key
    private final TextView textView;
    //There are 2 input methods:
    //EditText or Spinner
    //one of them is always null while the other one is active
    //(that's why there are 2 constructors)
    private final Spinner spinner;
    private final ArrayAdapter<String> adapter; //null when not spinner
    final List<String> VALID_DROPDOWN_VALUES;
    private final EditText editText;
    public final TableRow tableRow;
    private final UserChangedText userChangedText=new UserChangedText() {
        @Override
        public void onTextChanged(String newText) {
            System.out.println("user changed text"+KEY+newText);
            inputViewSetColor(Color.RED);
            updatedByUser=true;
        }
    };
    private boolean updatedByUser=false;
    private boolean errornousValue=false;

    public ASetting(final String key, final Context c){
        context=c;
        this.KEY =key;
        textView=new TextView(c);
        textView.setText(KEY);
        editText=new EditText(c);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        spinner=null;
        adapter=null;
        VALID_DROPDOWN_VALUES=null;
        tableRow=new TableRow(c);
        tableRow.addView(textView);
        tableRow.addView(editText);
        editText.addTextChangedListener(this);
    }

    public ASetting(final String key,final Context c,@ArrayRes int textArrayResId){
        context=c;
        this.KEY =key;
        textView=new TextView(c);
        textView.setText(KEY);
        spinner=new Spinner(c);
        VALID_DROPDOWN_VALUES= Collections.unmodifiableList(Arrays.asList(c.getResources().getStringArray(textArrayResId)));
        adapter=new ArrayAdapter<String>(c,android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(new ArrayList<String>(VALID_DROPDOWN_VALUES));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        editText=null;
        tableRow=new TableRow(c);
        tableRow.addView(textView);
        tableRow.addView(spinner);
        spinner.setOnItemSelectedListener(this);
    }


    public boolean hasBeenUpdatedByUser(){
        return updatedByUser;
    }

    public String getCurrentValue(){
        if(spinner!=null){
            return spinner.getItemAtPosition(spinner.getSelectedItemPosition()).toString();
        }
        return editText.getText().toString();
    }
    public void reset(){
        updatedByUser=false;
        inputViewSetColor(Color.RED);
        inputViewUpdateText(NOT_LOADED);
        inputViewSetEnabled(false);
        errornousValue=false;
    }

    public void processMessageGET_OK(final boolean ground,final String value,final boolean syncGroundOnly){
        //System.out.println("Process get_ok "+KEY+" "+ground+" "+value);
        //System.out.println("Curr"+getCurrentValue());
        //In this case we don't wait for the value from the air pi
        if(errornousValue){
            return;
        }
        if(ground){
            if(syncGroundOnly){
                inputViewUpdateText(value);
                inputViewSetColor(Color.GREEN);
                inputViewSetEnabled(true);
            }else{
                inputViewUpdateText(value);
                inputViewSetColor(Color.YELLOW);
                inputViewSetEnabled(false);
            }
        }else{
            //message from air pi (comes always after ground pi)
            if(value.equals(getCurrentValue())){
                //air and ground are in sync
                inputViewSetEnabled(true);
                inputViewSetColor(Color.GREEN);
            }else{
                //ask the user if he would like to use the air or ground value
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(false);
                final String valueGround=getCurrentValue();
                final String message="Air and ground are not in sync. Which value do you want to keep ?\n"+
                        KEY+"=\n"+
                        "GROUND: "+valueGround+"\n" +
                        "AIR: "+value+"\n";
                builder.setMessage(message);
                builder.setPositiveButton("Ground", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        updatedByUser=true;
                        inputViewUpdateText(valueGround);
                        inputViewSetEnabled(true);
                        inputViewSetColor(Color.GREEN);

                    }
                });
                builder.setNegativeButton("AIR", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updatedByUser=true;
                        inputViewUpdateText(value);
                        inputViewSetEnabled(true);
                        inputViewSetColor(Color.GREEN);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                inputViewUpdateText(NOT_IN_SYNC);
                inputViewSetEnabled(false);
                //Toast.makeText(context,"Not in sync"+getCurrentValue()+" | "+value,Toast.LENGTH_LONG).show();
            }
        }
    }

    public void processMessageCHANGE_OK(final boolean ground,final String value,final boolean syncGroundOnly){
        if(errornousValue){
            return;
        }
        if(ground){
            if(syncGroundOnly){
                inputViewUpdateText(value);
                inputViewSetColor(Color.GREEN);
                inputViewSetEnabled(true);
            }else{
                inputViewUpdateText(value);
                inputViewSetColor(Color.YELLOW);
                inputViewSetEnabled(false);
            }
        }else{
            if(value.equals(getCurrentValue())){
                //air and ground are in sync
                inputViewSetEnabled(true);
                inputViewSetColor(Color.GREEN);
            }else{
                inputViewUpdateText(NOT_IN_SYNC);
                inputViewSetEnabled(false);
                Toast.makeText(context,"Not in sync"+getCurrentValue()+" | "+value,Toast.LENGTH_LONG).show();
            }
        }
    }

    private void inputViewSetEnabled(final boolean enable){
        if(spinner!=null){
            spinner.setEnabled(enable);
        }else{
            editText.setEnabled(enable);
        }
    }

    private void inputViewSetColor(@ColorInt int color){
        if(spinner!=null){
            spinner.setBackgroundColor(color);
        }else{
            editText.setTextColor(color);
        }
    }

    //update input view without notifying the watchers
    private void inputViewUpdateText(final String value){
        //when the input view is an edit text, we just update its content
        //else, we have to find the right index from the drop down menu
        if(editText!=null){
            //The callback should only listen for user modifications
            editText.removeTextChangedListener(this);
            editText.setText(value);
            editText.addTextChangedListener(this);
        }else{
            spinner.setOnItemSelectedListener(null);
            if(value.equals(NOT_IN_SYNC)|| value.equals(NOT_LOADED)){
                adapter.clear();
                adapter.add(value);
                spinner.setSelection(0,false);
            }else{
                adapter.clear();
                adapter.addAll(new ArrayList<String>(VALID_DROPDOWN_VALUES));
                int spinnerPosition=adapter.getPosition(value);
                if(spinnerPosition!=-1){
                    spinner.setSelection(spinnerPosition,false);//animate==false important else the listener gets notified ! (WTF android !!)
                }else{
                    Toast.makeText(context,"Invalid value: "+KEY+"="+value,Toast.LENGTH_LONG).show();
                    errornousValue=true;
                    adapter.clear();
                    adapter.add("INVALID"+value);
                    spinner.setAdapter(adapter);
                    spinner.setSelection(0,false);
                }
            }
            spinner.setOnItemSelectedListener(this);
        }
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        final String newText=s.toString();
        System.out.println("Text changed");
        userChangedText.onTextChanged(newText);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final String newText=parent.getItemAtPosition(position).toString();
        userChangedText.onTextChanged(newText);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }



}
