package com.example.a5.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.a5.MainActivity;
import com.example.a5.R;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class RegistrationFragment extends Fragment  {

    private Spinner employment;
    private Spinner age;
    private Spinner gender;
    private Spinner prex;
    private Button submit;

    private int pos1,pos2,pos3,pos4 = 0;

    private String[] emplpyArr = {"Student","Service","Manufacturing","Office Based","Remote","Neet"};
    private String[] ageArr = {"under 18","18-35","35-50","50-65","65+"};
    private String[] genderArr = {"male","female"};
    private String[] prexArr = {"No conditions","Diabetes","Asthma","Cardiovascular disease","Cancer","Other"};




    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        final View root = inflater.inflate(R.layout.reg_view, container, false);

        employment = (Spinner) root.findViewById(R.id.empSpin);
        age = (Spinner) root.findViewById(R.id.ageSpin);
        gender = (Spinner) root.findViewById(R.id.genderSpin);
        prex = (Spinner) root.findViewById(R.id.prexSpin);
        submit = (Button) root.findViewById(R.id.Submit);


        ArrayAdapter empAdapter = new ArrayAdapter(this.getContext(),android.R.layout.simple_spinner_item,emplpyArr);
        empAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter ageAdapter = new ArrayAdapter(this.getContext(),android.R.layout.simple_spinner_item,ageArr);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter genderAdapter = new ArrayAdapter(this.getContext(),android.R.layout.simple_spinner_item,genderArr);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter prexAdapter = new ArrayAdapter(this.getContext(),android.R.layout.simple_spinner_item,prexArr);
        prexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        employment.setAdapter(empAdapter);
        age.setAdapter(ageAdapter);
        gender.setAdapter(genderAdapter);
        prex.setAdapter(prexAdapter);

        employment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pos1 = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        age.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pos2 = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        gender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pos3 = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        prex.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pos4 = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getContext().checkSelfPermission(WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d("woof","REEEEEE");
                    // Permission is not granted
                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE}, 1);
                }
                Snackbar.make(v, "Submitted "+emplpyArr[pos1]+" "+ageArr[pos2]+" "+genderArr[pos3]+" "+prexArr[pos4], Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();


                String re = "";
                //We store the information to a file
                try {
                    Random r = new Random();

                    re = genString() +"\n" + pos1+"\n"+pos2+"\n"+pos3+"\n"+pos4;
                    //File fu = new File("data.csv");
                    FileOutputStream f =  v.getContext().openFileOutput("data.csv", Context.MODE_PRIVATE);
                    //FileOutputStream f =  new FileOutputStream(fu);
                    f.write(re.getBytes());
                    f.close();
                    String[] files = getContext().fileList();
                    Log.d("woof", Arrays.toString(files));

                    File directory = getContext().getFilesDir();
                    File file = new File(directory,"data.csv");

                    //Uri path = Uri.fromFile(file);
                    if(file.exists()){
                        Log.d("woof", "exists lul");
                    }else {
                        Log.d("woof", "F");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent a = new Intent(v.getContext(), MainActivity.class);
                startActivity(a);

            }
        });

        return root;

    }
    public String genString() {
        byte[] array = new byte[10]; // length is bounded by 7
        new Random().nextBytes(array);
        String generatedString = new String(array, Charset.forName("UTF-8"));
        return generatedString;
    }

}
