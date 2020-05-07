package com.example.a5.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a5.R;
import com.google.android.material.snackbar.Snackbar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RankFragment extends Fragment {

    Spinner criteria;
    private String[] arr = {"Age","Employment","Gender","Pre existing Conditions","Overall"};
    int[] responseData;
    GraphView graph;
    private TextView explainer;
    private Button weekView;
    private Button hourView;
    private int curr_pos = 0;
    //private double

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.rank_view, container, false);
        graph =  (GraphView) root.findViewById(R.id.graph);
        graph.setVisibility(View.VISIBLE);
        final TextView textView = root.findViewById(R.id.section_label);
        criteria = root.findViewById(R.id.ranks);
        explainer = root.findViewById(R.id.explainer_label);
        ArrayAdapter rankAdapter = new ArrayAdapter(this.getContext(),android.R.layout.simple_spinner_item,arr);
        rankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        criteria.setAdapter(rankAdapter);
        criteria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Snackbar.make(view, arr[position], Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                curr_pos = position;
                createGraph(-1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        weekView = root.findViewById(R.id.wk_view);
        weekView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGraph(0);
            }


        });
        hourView = root.findViewById(R.id.hr_view);
        hourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createGraph(-1);
            }
        });


        sendToServer();

        return root;
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
                    //count = false;
                    //continue;
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

    public void setResponse(int[] res){
        responseData = res;
        if(responseData != null)explainer.setText("The mean social distance score for "+arr[0]+" was "+(responseData[0]+"").substring(0,2) );
        Log.d("woof",res+"");
    }

    public void createGraph(int start){
        boolean initSer2 = false;
        ArrayList<Double> loc_scores = new ArrayList<Double>();
        ArrayList<Double> blu_scores = new ArrayList<Double>();
        LineGraphSeries<DataPoint> series2;
        getPreviousScores(loc_scores,blu_scores);
        if(start == -1){
            start = Math.max(0,loc_scores.size()-12);
            Log.d("woof","start "+start);
        }

        double mean = 0;
        if(responseData != null){
            if(curr_pos == 4)mean = 69;
            else mean = responseData[curr_pos];
            explainer.setText("The mean social distance score for "+arr[curr_pos]+" was "+(mean+"").substring(0,2) );
            if(curr_pos == 3 || curr_pos == 4){
                explainer.setTextSize(20);
            }else {
                explainer.setTextSize(25);
            }
        }

        graph.removeAllSeries();
        if(responseData != null && responseData.length > curr_pos){
            series2 =  new LineGraphSeries<DataPoint>(new DataPoint[] {
                    new DataPoint(start, responseData[curr_pos]),
                    new DataPoint(loc_scores.size(),responseData[curr_pos])

            });
            series2.setColor(Color.RED);
            graph.addSeries(series2);

        }
        try {

            DataPoint[] values = new DataPoint[loc_scores.size()-start];
            int cout = 0;
            for(int i=start;i<loc_scores.size();i++){
                double score = (loc_scores.get(i)+blu_scores.get(i))/2;
                DataPoint d = new DataPoint(i,score);
                values[cout] = d;
                cout++;
            }
            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(values);
            graph.addSeries(series);


        } catch (IllegalArgumentException e) {
        }

    }

    public void sendToServer(){
        File directory = getContext().getFilesDir();
        File file = new File(directory,"data.csv");
        ArrayList<Integer> meow = new ArrayList<Integer>();
        regServ();
        if(file.exists()){
            Log.d("woof", "exists lul");
        }else {
            Log.d("woof", "F");
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
        final int[] woof;
        try{

            RequestQueue queue = Volley.newRequestQueue(this.getContext());
            String url2 = "https://a5-server.herokuapp.com/getScore";
            StringRequest postRequest2 = new StringRequest(Request.Method.POST, url2,
                    new Response.Listener<String>()
                    {
                        @Override
                        public void onResponse(String response) {
                            // response
                            Log.d("Response", response);
                            JSONArray jsonArray = null;
                            try {
                                jsonArray = new JSONArray(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String[] strArr = new String[jsonArray.length()];

                            for (int i = 0; i < jsonArray.length(); i++) {
                                try {
                                    strArr[i] = jsonArray.getString(i);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            int[] iArr = new int[strArr.length];
                            Log.d("woof", Arrays.toString(strArr)+"");
                            for(int i=0;i< strArr.length;i++){
                                if (strArr[i].equals("null"))iArr[i] = 69;
                                iArr[i] = (int) Double.parseDouble(strArr[i]);
                            }
                            setResponse(iArr);
                            //System.out.println(Arrays.toString(strArr));
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
    public void regServ(){
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
                    params.put("score", 69+"");

                    return params;
                }
            };
            queue.add(postRequest);


        }catch (Exception e){
            Log.d("woof","EROROROROOROR"+e);
        }
    }

}
