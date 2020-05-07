package com.example.a5.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a5.BackgroundService;
import com.example.a5.R;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class OverViewFragment extends Fragment {
    private ArrayList<String> mNewDevicesArrayAdapter;
    //private BluetoothAdapter mBtAdapter;
    private ArrayList<String> encounterTimes;
    private ArrayList<Location> encounterLocation;
    private Button serverBu;
    private TextView overall_score;
    private TextView location_score;
    private TextView bluetooth_score;
    private int[] textState = {0,0,0};



    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mNewDevicesArrayAdapter = new ArrayList<String>();
        final View root = inflater.inflate(R.layout.overview_view, container, false);
        final TextView textView = root.findViewById(R.id.section_label);
        //mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        //Log.d("woof", mBtAdapter+"");
        // Register for broadcasts when a device is discovered

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //this.getContext().registerReceiver(mReceiver, filter);
        ArrayList<Double> loc_scores = new ArrayList<Double>();
        ArrayList<Double> blu_scores = new ArrayList<Double>();
        getPreviousScores(loc_scores,blu_scores);
        overall_score = root.findViewById(R.id.over_sdScore);
        location_score = root.findViewById(R.id.person_sd_score);
        bluetooth_score = root.findViewById(R.id.land_sd_score);
        if(loc_scores.size() > 0){
            int amount = 3;
            //amount = Math.min(3, bluetooth_score.length());
            String blueScore = (blu_scores.get(blu_scores.size()-1)+"").substring(0,amount);
            String loc_Score = (loc_scores.get(loc_scores.size()-1)+"").substring(0,amount);
            //amount = Math.min(3, loc_Score.length());
            String overall_Score = (""+(2*loc_scores.get(loc_scores.size()-1)+blu_scores.get(blu_scores.size()-1))/3).substring(0,amount);
            //amount = Math.min(3, overall_score.length());
            bluetooth_score.setText(blueScore);
            location_score.setText(loc_Score);
            overall_score.setText(overall_Score);
            updateScoreLabels(Double.parseDouble(loc_Score),Double.parseDouble(blueScore));
        }


        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //this.getContext().registerReceiver(mReceiver, filter);
        //doDiscovery();

        serverBu = (Button)root.findViewById(R.id.send_server);
        serverBu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToServer();
            }
        });
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    File directory = root.getContext().getFilesDir();
                    File file = new File(directory,"curr_score.csv");
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    boolean count = true;

                    line = br.readLine();
                    br.close();
                    if(line !=null){
                        String[] ls = line.split(" ");
                        double l = Double.parseDouble(ls[0]);
                        double b = Double.parseDouble(ls[1]);
                        updateScoreLabels(l,b);

                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this,10000);
            }
        },10000);
        //handler.postDelayed
        return root;
    }

//    public void onDestroy() {
//        super.onDestroy();
//
//        // Make sure we're not doing discovery anymore
//        if (mBtAdapter != null) {
//            mBtAdapter.cancelDiscovery();
//        }
//
//        // Unregister broadcast listeners
//        //this.getContext().unregisterReceiver(mReceiver);
//    }

    public void sendToServer(){
        File directory = getContext().getFilesDir();
        File file = new File(directory,"data.csv");
        ArrayList<Integer> meow = new ArrayList<Integer>();
        if(file.exists()){
            Log.d("woof", "exists lul");
        }else {
            Log.d("woof", "F");
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            boolean count = true;

            while ((line = br.readLine()) != null) {
                if(count){
                    count = false;
                    continue;
                }
                meow.add(Integer.parseInt(line));
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        Log.d("woof",meow+"");
        if(meow.size() == 0){
            Snackbar.make(this.getView(),"Register first",Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            return;
        }
        JSONObject postData = new JSONObject();
        try {
            postData.put("age", meow.get(0)+"");
            postData.put("emp", meow.get(1) +"");
            postData.put("gender", meow.get(2) + "");
            postData.put("prex", meow.get(3) + "");
            postData.put("score", 69 + "");

        } catch (JSONException e) {
            e.printStackTrace();
        }


        final ArrayList<Integer> postDataARR = new ArrayList<Integer>(meow);
        try{

            RequestQueue queue = Volley.newRequestQueue(this.getContext());
            String url = "https://a5-server.herokuapp.com/registerScore";
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>()
                    {
                        @Override
                        public void onResponse(String response) {
                            // response
                            Log.d("Response", response);
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            error.printStackTrace();
                            //Log.d("Error.Response", );
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams()
                {
                    Map<String, String> params = new HashMap<String, String>();

                    params.put("age", postDataARR.get(0)+"");
                    params.put("emp", postDataARR.get(1)+"");
                    params.put("gender", postDataARR.get(2)+"");
                    params.put("prex", postDataARR.get(3)+"");
                    params.put("score", 100+"");

                    return params;
                }
            };
            queue.add(postRequest);

            String url2 = "https://a5-server.herokuapp.com/getScore";
            StringRequest postRequest2 = new StringRequest(Request.Method.POST, url2,
                    new Response.Listener<String>()
                    {
                        @Override
                        public void onResponse(String response) {
                            // response
                            Log.d("Response", response);
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            error.printStackTrace();
                            //Log.d("Error.Response", );
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams()
                {
                    Map<String, String> params = new HashMap<String, String>();

                    params.put("age", postDataARR.get(0)+"");
                    params.put("emp", postDataARR.get(1)+"");
                    params.put("gender", postDataARR.get(2)+"");
                    params.put("prex", postDataARR.get(3)+"");
                    params.put("score", 100+"");

                    return params;
                }
            };
            queue.add(postRequest2);



        }catch (Exception e){
            Log.d("woof","EROROROROOROR"+e);
        }
    }

    public void updateScoreLabels(double l,double b){
        String[] strDec = {"","",""};
        TextView[] w = {location_score,overall_score,bluetooth_score};
        int amount = 3;
        //amount = Math.min(3, bluetooth_score.length());
        DecimalFormat df = new DecimalFormat("#");
        if(b < 0)amount++;
        String blueScore = df.format(b);//(df.format(b)+"").substring(0,amount);

        amount--;
        if(l < 0)amount++;
        String loc_Score = df.format(l);//(df.format(l)+"").substring(0,amount);
        amount--;
        //amount = Math.min(3, loc_Score.length());
        if(((2*l+b)/3) < 0)amount++;
        String overall_Score = df.format((2*l+b)/3);//(df.format((2*l+b)/3)).substring(0,amount);
        //amount = Math.min(3, overall_score.length());

        bluetooth_score.setText(blueScore);
        location_score.setText(loc_Score);
        overall_score.setText(overall_Score);
        bluetooth_score.setTextColor(rgbVals(b));
        location_score.setTextColor(rgbVals(l));
        overall_score.setTextColor(rgbVals((2*l+b)/3));
        //location_score.setTextColor(Color.RED);
        //Log.d("woof",rgbVals(l)+"");



    }

    public int rgbVals(double score){
        //Log.d("ew",((score/100.0) - 0.5)+"");
        score = Math.max(0,score);
        double scorePr = score/100.0;
        int green = score > 50 ? (int)(((scorePr - 0.5)/0.6)*255) : 0;
        double r1 = (1.0 - scorePr)*255;
        int red = score > 50 ? (int) r1  : (int) (0.8-scorePr)*255;
        //Log.d("woof",red+" "+green +"colorlo");

        return getIntFromColor(red,green,0);

    }
    public int getIntFromColor(int Red, int Green, int Blue){
        Red = (Red << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
        Green = (Green << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
        Blue = Blue & 0x000000FF; //Mask out anything not blue.

        return 0xFF000000 | Red | Green | Blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }


    public void getPreviousScores(ArrayList<Double> loc_scores,ArrayList<Double> blu_scores){
        try {
            File directory = this.getContext().getFilesDir();
            File file = new File(directory,"scores.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            boolean count = true;

            while ((line = br.readLine()) != null) {
                if(count){
                    count = false;
                    continue;
                }
                String[] spl = line.split(" ");
                loc_scores.add(Double.parseDouble(spl[0]));
                blu_scores.add(Double.parseDouble(spl[1]));
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void doDiscovery() {
//        Log.d("woof", "doDiscovery()");
//
//        // If we're already discovering, stop it
////        if (mBtAdapter.isDiscovering()) {
////            mBtAdapter.cancelDiscovery();
////        }
//
//        // Request discover from BluetoothAdapter
//        mBtAdapter.startDiscovery();
//    }

}
