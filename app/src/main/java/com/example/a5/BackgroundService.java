package com.example.a5;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a5.MainActivity;
import com.example.a5.R;
import com.google.android.material.snackbar.Snackbar;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import static com.example.a5.App.CHANNEL_ID;
import org.tensorflow.lite.Interpreter;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundService extends Service implements LocationListener {

    LocationManager locationManager;
    ArrayList<Double> setLocation;
    private Location homeLocation = null;
    private long exitTime;
    private Location prevLocation;
    private Location currLocation;
    private MediaRecorder mediaRecorder;
    private boolean outMode = false;
    private ArrayList<String> mNewDevicesArrayAdapter;
    private ArrayList<Location> hotspots;

    private BluetoothAdapter mBtAdapter;
    private double location_score = 100;
    private File recordAudioFile;
    private double bluetooth_score = 100;
    private boolean scanning = false;
    private long loc_store = 0;
    private Interpreter tflite;
    private static final int REQUEST_RECORD_AUDIO = 13;
    private Classifier classifier;



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                scanning = true;
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if (device.getName() != null) {
                        Log.d("woof", device.getName());
                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        bluetooth_score-=5;
                        updateScore();
                        Log.d("woof","bluetooth score"+bluetooth_score);
                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                scanning = false;
                //setTitle(R.string.select_device);
                bluetooth_score -= (mNewDevicesArrayAdapter.size());
                updateScore();
                Log.d("woof","bluetoothHH score"+bluetooth_score);
                Log.d("woof","locationHH socre"+location_score);
                if (mNewDevicesArrayAdapter.size() >= 2) {
                    bluetooth_score -= (20 * mNewDevicesArrayAdapter.size());
                    updateScore();

                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    Activity#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for Activity#requestPermissions for more details.
                        return;
                    }
                    //hotspots.add(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                    locationList();
                }
                Log.d("woof",mNewDevicesArrayAdapter.size()+"");
            }
        }
    };



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Log.d("woof","STARTED");
        File directory = getFilesDir();
        File file = new File(directory,"mylocation.csv");
        setLocation = new ArrayList<Double>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            boolean count = true;

            while ((line = br.readLine()) != null) {
                if(count){
//                    count = false;
//                    continue;
                }
                setLocation.add(Double.parseDouble(line));

            }
            homeLocation = new Location("home");
            homeLocation.setLatitude(setLocation.get(0));
            homeLocation.setLongitude(setLocation.get(1));
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recordAudio();

        classifier = new Classifier(getAssets());

        mNewDevicesArrayAdapter = new ArrayList<String>();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                //your code
                Log.d("woof","send to server");
                sendToServer();
                Random r = new Random();
                ArrayList<Double> loc_scores = new ArrayList<Double>();
                ArrayList<Double> blu_scores = new ArrayList<Double>();
                getPreviousScores(loc_scores,blu_scores);
                //get Scores
                if(outMode)location_score-=r.nextInt(50);
                Log.d("woof","LOCATION_SCORE"+location_score);
                loc_scores.add(location_score);
                blu_scores.add(bluetooth_score);
                String re = "";
                for(int i=0;i<loc_scores.size();i++){
                    re+=(loc_scores.get(i)+" "+blu_scores.get(i) + "\n");
                }

                try {
                    //Write Score to hourly.csv
                    //File fu = new File("scores.csv");
                    FileOutputStream f =  openFileOutput("scores.csv", Context.MODE_PRIVATE);
                    //FileOutputStream f =  new FileOutputStream(fu);
                    f.write(re.getBytes());
                    f.close();
                    String[] files = fileList();
                    Log.d("woof", Arrays.toString(files));

                    File directory = getFilesDir();
                    File file = new File(directory,"scores.csv");

                    //Uri path = Uri.fromFile(file);
                    if(file.exists()){
                        Log.d("woof", "exists lul");
                    }else {
                        Log.d("woof", "F");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                location_score = 100;
                bluetooth_score = 100;
                updateScore();


                handler.postDelayed(this,3600000);
            }
        },3600000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("woof","sssjkfdjfdjfdjkdfjkfjkfdjkfjkjfdjgfdjkgfdjkgfjkgjkfjks");
        String input = "BARK BARK";//intent.getStringExtra("inputExtra");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background service A5")
                .setContentText(input)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        //do heavy work on a background thread
        //stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    //Writes score to file
    public void updateScore(){
        String toWrite = location_score + " " + bluetooth_score;
        try{
            FileOutputStream f =  openFileOutput("curr_score.csv", Context.MODE_PRIVATE);
            //FileOutputStream f =  new FileOutputStream(fu);
            f.write(toWrite.getBytes());
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //Log.d("woof","mmmmmmmmmm");
        prevLocation = currLocation;
        currLocation = location;
        if(setLocation.size() == 2){

            int thresh = 40;

            if(homeLocation != null && location.distanceTo(homeLocation) >= thresh && !outMode){
                Calendar rightNow = Calendar.getInstance();
                exitTime = rightNow.getTimeInMillis();
                outMode = true;
            }else{
                if(outMode && location.distanceTo(homeLocation) < thresh){
                    Calendar rightNow = Calendar.getInstance();
                    long entranceTime = rightNow.getTimeInMillis();
                    long diff = entranceTime - exitTime;
                    location_score-=((double) diff/60000.0);
                    updateScore();
                    outMode = false;

                }else if (outMode){
                    doDiscovery();
                    location_score-=0.5;
                    updateScore();
                }

            }
        }
        return;
    }


    public void locationList(){
        long time= System.currentTimeMillis();

        if(time - loc_store < 60000)return;
        loc_store = time;
        ArrayList<String> loc_list = new ArrayList<String>();

        String toWrite = "";
        try {
            File directory = getFilesDir();
            File file = new File(directory,"loc_list.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            boolean count = true;

            while ((line = br.readLine()) != null) {
                if(count){
//                    count = false;
//                    continue;
                }
                String[] spl = line.split(" ");
                toWrite+=(line+"\n");
                //Location l = new Location("");
                //l.setLatitude(Double.parseDouble(spl[0]));
                //l.setLongitude(Double.parseDouble(spl[1]));
                //loc_list.add(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//
        //for()
        toWrite+=(currLocation.getLatitude()+" "+currLocation.getLongitude()+"\n");
        try {
            //Write Score to hourly.csv
            //File fu = new File("scores.csv");
            FileOutputStream f =  openFileOutput("loc_list.csv", Context.MODE_PRIVATE);
            //FileOutputStream f =  new FileOutputStream(fu);
            f.write(toWrite.getBytes());
            f.close();
            String[] files = fileList();
            //Log.d("woof", Arrays.toString(files));

            File directory = getFilesDir();
            File file = new File(directory,"loc_list.csv");

            //Uri path = Uri.fromFile(file);
            if(file.exists()){
                Log.d("woof", "exists lul");
            }else {
                Log.d("woof", "F");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    private void doDiscovery() {
        mBtAdapter.startDiscovery();
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void getPreviousScores(ArrayList<Double> loc_scores,ArrayList<Double> blu_scores){
        try {
            File directory = getFilesDir();
            File file = new File(directory,"scores.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            boolean count = true;

            while ((line = br.readLine()) != null) {
                if(count){
//                    count = false;
//                    continue;
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

    private void setupmediarecorder(){
        try {
            recordAudioFile = File.createTempFile("record",".wav");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD);
            mediaRecorder.setAudioSamplingRate(40050);
            mediaRecorder.setAudioEncodingBitRate(500000);
//            try {
//                FileOutputStream f =  openFileOutput("loc_list.csv", Context.MODE_PRIVATE);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
            mediaRecorder.setOutputFile(recordAudioFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private boolean checkPermissions(){
        int write_res = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int audio_res = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        return write_res == PackageManager.PERMISSION_GRANTED && audio_res == PackageManager.PERMISSION_GRANTED;
    }
    public void recordAudio(){
        if(!checkPermissions()){
            Log.d("woof","get Permission bruh");
            return;
        }
        setupmediarecorder();
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    mediaRecorder.release();
                    Log.d("woof",recordAudioFile.getAbsolutePath());
                    Log.d("woof","edfdf");
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    BufferedInputStream in = null;
                    try {
                        in = new BufferedInputStream(new FileInputStream(recordAudioFile.getAbsolutePath()));
                        int read;
                        byte[] buff = new byte[1024];
                        while ((read = in.read(buff)) > 0)
                        {
                            out.write(buff, 0, read);
                        }
                        out.flush();
                        byte[] audioBytes = out.toByteArray();
                        Log.d("woof",audioBytes.length+"");
                        MFCC m = new MFCC();
                       float[] spectrogram =  m.process(convertArr(audioBytes));
                       double[][] sp = m.melSpectrogram(convertArr(audioBytes));//m.dctMfcc(convertArr(audioBytes));
                       conductInference(sp);
                       Log.d("woof","created spectogram "+spectrogram.length);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

            }, 1000);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public double[] convertArr(byte[] bArr){
        double[] mArr = new double[Math.min(22050,bArr.length)];
        for(int i =0;i<mArr.length;i++){
            mArr[i] = (double)bArr[i];
        }
        return mArr;
    }
    public double[] convertArrd(float[] bArr){
        double[] mArr = new double[bArr.length];
        for(int i =0;i<mArr.length;i++){
            mArr[i] = (double)bArr[i];
        }
        return mArr;
    }

    public void conductInference(double[][] spec){
        float[][][][] inp = new float[1][128][44][1];
        for(int i= 0;i<128;i++){
            for(int j=0;j<44;j++){
                if(i >= spec.length)return;
                if(j >= spec[0].length)return;
                inp[0][i][j][0] = (float) spec[i][j];
            }
        }
        int res = classifier.doInferenceWof(inp);
        Log.d("woof",res+" classification result");

    }


    public void sendToServer(){
        File directory = getFilesDir();
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
        //Log.d("woof",meow+"");
        if(meow.size() ==0)return;
        final ArrayList<Integer> postDataARR = new ArrayList<Integer>(meow);
        try{

            RequestQueue queue = Volley.newRequestQueue(this);
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
                    params.put("score", (2*location_score+bluetooth_score)/3+"");


                    return params;
                }
            };
            queue.add(postRequest);

        }catch (Exception e){
            Log.d("woof","EROROROROOROR"+e);
        }
    }
}
