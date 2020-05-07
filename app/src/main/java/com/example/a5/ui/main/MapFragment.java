package com.example.a5.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import com.google.android.gms.location.*;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import com.example.a5.R;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.provider.SettingsSlicesContract.KEY_LOCATION;

public class MapFragment extends Fragment implements
        OnMapReadyCallback, LocationListener {
    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    LocationManager locationManager;
    private Button prev_loc_btn;
    private PlacesClient mPlacesClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean mLocationPermissionGranted;
    private Location prevLocation;
    private Location currLocation;
    private Location homeLocation = null;
    private static final int M_MAX_ENTRIES = 5;
    private long exitTime;
    private boolean outMode = false;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private List[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private ArrayList<Location> previousLocations;
    private TextView bt;

    private Button setLocation;
    MapView mView;

    /*
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private PlacesClient mPlacesClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private CameraPosition mCameraPosition;
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private List[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;
    private LatLng markerLatLng;
    private String markerSnippet;
    private String markerPlaceName;
    private int btDevicesCount;
    */
    private int btn_pos = 0;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

//        if (savedInstanceState != null) {
//            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
//            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
//        }


        View root = inflater.inflate(R.layout.map_view, container, false);
        //mView = (MapView) root;
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if(!checkPermissions())requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE,RECORD_AUDIO}, 1);
        if (ActivityCompat.checkSelfPermission(root.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(root.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  null;// and ask for run time permission
        }
        bt = root.findViewById(R.id.bt);
        bt.setText("");
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,this);
        final Location currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        Places.initialize(root.getContext(), getString(R.string.google_maps_key));
        mPlacesClient = Places.createClient(root.getContext());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(root.getContext());
        final TextView textView = root.findViewById(R.id.section_label);
        if(mapFragment == null){
            Log.d ("woof","meow");
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            mapFragment = new SupportMapFragment();
            ft.replace(R.id.map,mapFragment).commit();
        }
        //Places.initialize(root.getContext(), getString(R.string.google_maps_key));
        currLocation = currentLocation;
        mapFragment.getMapAsync(this);

        setLocation = root.findViewById(R.id.set_location);
        previousLocations = new ArrayList<Location>();
        getPreviousLocationList();
        prev_loc_btn = root.findViewById(R.id.prev_loc);
        prev_loc_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(previousLocations.size() == 0){
                    Snackbar.make(v, "You havent gone out.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }
                //if(btn_pos == 0)
                Log.d("woof",btn_pos+"butpos");
                Random r = new Random();
                int ran = r.nextInt(2);
                //if()bt.setText("There were " +(2+ran)+" bluetooth devices here");
                LatLng loc = new LatLng(previousLocations.get(btn_pos).getLatitude(),previousLocations.get(btn_pos).getLongitude());
                mMap.addMarker(new MarkerOptions().position(loc).title("Location "+(btn_pos+1)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                btn_pos++;
                if(btn_pos == previousLocations.size())btn_pos = 0;

            }
        });
        setLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Print Location
                //Save to a csv
                homeLocation = currentLocation;
                Snackbar.make(v, "Set Location "+currentLocation.getLatitude() +" "+ currentLocation.getLongitude(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                try {
                    FileOutputStream f =  v.getContext().openFileOutput("mylocation.csv", Context.MODE_PRIVATE);
                    //FileOutputStream f =  new FileOutputStream(fu);
                    String re = currentLocation.getLatitude() +"\n"+ currentLocation.getLongitude();
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
            }
        });
        return  root;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }
    private boolean checkPermissions(){
        int write_res = this.getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int audio_res = this.getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        int loc_res = this.getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        return write_res == PackageManager.PERMISSION_GRANTED && audio_res == PackageManager.PERMISSION_GRANTED
                && loc_res == PackageManager.PERMISSION_GRANTED;
    }

    private void showCurrentPlace() {
        if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            // Use fields to define the data types to return.
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS,
                    Place.Field.LAT_LNG);

            // Use the builder to create a FindCurrentPlaceRequest.
            FindCurrentPlaceRequest request =
                    FindCurrentPlaceRequest.newInstance(placeFields);

            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final Task<FindCurrentPlaceResponse> placeResult =
                    mPlacesClient.findCurrentPlace(request);
            placeResult.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FindCurrentPlaceResponse likelyPlaces = task.getResult();

                        // Set the count, handling cases where less than 5 entries are returned.
                        int count;
                        if (likelyPlaces.getPlaceLikelihoods().size() < M_MAX_ENTRIES) {
                            count = likelyPlaces.getPlaceLikelihoods().size();
                        } else {
                            count = M_MAX_ENTRIES;
                        }

                        int i = 0;
                        mLikelyPlaceNames = new String[count];
                        mLikelyPlaceAddresses = new String[count];
                        mLikelyPlaceAttributions = new List[count];
                        mLikelyPlaceLatLngs = new LatLng[count];

                        for (PlaceLikelihood placeLikelihood : likelyPlaces.getPlaceLikelihoods()) {
                            // Build a list of likely places to show the user.
                            mLikelyPlaceNames[i] = placeLikelihood.getPlace().getName();
                            mLikelyPlaceAddresses[i] = placeLikelihood.getPlace().getAddress();
                            mLikelyPlaceAttributions[i] = placeLikelihood.getPlace()
                                    .getAttributions();
                            mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                            i++;
                            if (i > (count - 1)) {
                                break;
                            }
                        }

                        // Show a dialog offering the user the list of likely places, and add a
                        // marker at the selected place.
                    } else {
                        Log.e("woof", "Exception: %s", task.getException());
                    }
                }
            });
        } else {
            // The user has not granted permission.
            Log.i("woof", "The user did not grant location permission.");

            // Add a default marker, because the user hasn't selected a place.
//            mMap.addMarker(new MarkerOptions()
//                    .title(getString(R.string.default_info_title))
//                    .position(mDefaultLocation)
//                    .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            //getLocationPermission();
        }

    }

@Override
public void onMapReady(GoogleMap map) {
    mMap = map;


    // TEST
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

    // Use a custom info window adapter to handle multiple lines of text in the
    // info window contents.
//    mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
//
//        @Override
//        // Return null here, so that getInfoContents() is called next.
//        public View getInfoWindow(Marker arg0) {
//            return null;
//        }
//
//        @Override
//        public View getInfoContents(Marker marker) {
//            // Inflate the layouts for the info window, title and snippet.
//
//            View infoWindow = getLayoutInflater().inflate(mView.findViewById(R.id.map), false);
//
//            TextView title = infoWindow.findViewById(R.id.title);
//            title.setText(marker.getTitle());
//
//            TextView snippet = infoWindow.findViewById(R.id.snippet);
//            snippet.setText(marker.getSnippet());
//
//            return infoWindow;
//        }
//    });

    // Prompt the user for permission.

    // Turn on the My Location layer and the related control on the map.
    updateLocationUI();

    // Get the current location of the device and set the position of the map.
    getDeviceLocation();
}
//    }
private void updateLocationUI() {
    if (mMap == null) {
        return;
    }
    try {
        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
            getLocationPermission();
        }
    } catch (SecurityException e)  {
        Log.e("Exception: %s", e.getMessage());
    }
}
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this.getActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if(mLastKnownLocation == null)return;
                            if(currLocation == null)return;
                            LatLng currL = new LatLng(currLocation.getLatitude(),currLocation.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(currL).title("My area"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        } else {
                            //Log.d(TAG, "Current location is null. Using defaults.");
                            //Log.e(TAG, "Exception: %s", task.getException());
                            LatLng currL = new LatLng(currLocation.getLatitude(),currLocation.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(currL).title("My area"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currL, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void getPreviousLocationList(){
        try {
            File directory = this.getContext().getFilesDir();
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
                Location l = new Location("");
                l.setLatitude(Double.parseDouble(spl[0]));
                l.setLongitude(Double.parseDouble(spl[1]));
                previousLocations.add(l);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Location woof = new Location("");
        woof.setLatitude(1.1);
        woof.setLongitude(1.1);
        //previousLocations.add(woof);
        //previousLocations.add(currLocation);
    }

//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        if (mMap != null) {
//            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
//            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
//            super.onSaveInstanceState(outState);
//        }
//    }
    @Override
    public void onLocationChanged(Location location) {
        //txtLat = (TextView) findViewById(R.id.textview1);
        //txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        prevLocation = currLocation;
        currLocation = location;
        //Log.d("woof",outMode+"");
        Log.d("woof",location+"");
        if(homeLocation != null){
            Log.d("woof",location.distanceTo(homeLocation)+"");
        }
        if(homeLocation != null && !outMode){
            Toast.makeText(this.getContext(),""+location.distanceTo(homeLocation),Toast.LENGTH_SHORT);
        }
        int thresh = 40;
        if(homeLocation != null && location.distanceTo(homeLocation) >= thresh && !outMode){
            Snackbar.make(this.getView(), "Recording via bluetooth "+location.distanceTo(homeLocation), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            Calendar rightNow = Calendar.getInstance();
            exitTime = rightNow.getTimeInMillis();
            outMode = true;
            //create new intent ofc
        }else{
            if(outMode && location.distanceTo(homeLocation) < thresh){
                Calendar rightNow = Calendar.getInstance();
                long entranceTime = rightNow.getTimeInMillis();
                long diff = entranceTime - exitTime;
                Snackbar.make(this.getView(), "You were out for "+diff+" ms", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                outMode = false;
            }

        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }


}
