package com.example.mobileappproject;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class locationActivity extends FragmentActivity implements OnMapReadyCallback {

    FirebaseFirestore db;
    GoogleMap mMap;
    AutocompleteSupportFragment autocomplete;
    SearchView searchView;

    int fcount = 0;
    int count = 0;
    double[] latitudes = new double[20];
    double[] longitudes = new double[20];

    String[] names = new String[20];
    String[] chosen = new String[20];
    String[] userIDs = new String[20];

    ArrayList<String> chosenUserIDS = new ArrayList<>();

    String search;
    Button nextPage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        count = 0;
        fcount = 0;

        Arrays.fill(latitudes, 0);
        Arrays.fill(longitudes, 0);
        Arrays.fill(names, null);
        Arrays.fill(chosen, null);

        db = FirebaseFirestore.getInstance();

        db.collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());

                                latitudes[count] = (double) document.getData().get("latitude");
                                longitudes[count] = (double) document.getData().get("longitude");
                                names[count] = document.getData().get("name").toString();
                                userIDs[count] = document.getId();
                                count++;
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

        searchView = findViewById(R.id.idSearchView);

        nextPage = (Button) findViewById(R.id.nextPage);

        double radius = (getIntent().getDoubleExtra("keyRadius",0)) * 1000;

        String eName = getIntent().getStringExtra("keyName");
        String dTime = getIntent().getStringExtra("keyDateTime");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                String location = searchView.getQuery().toString();

                List<Address> addressList = null;

                if (location != null || location.equals("")) {
                    Geocoder geocoder = new Geocoder(locationActivity.this);

                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Address address = addressList.get(0);

                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                    search = address.getAddressLine(0);

                    mMap.addMarker(new MarkerOptions().position(latLng).title(location));

                    for (int i = 0; i < count; i++){
                        LatLng friend = new LatLng(latitudes[i], longitudes[i]);
                        distanceBetween(latLng, friend, radius, i);
                        mMap.addMarker(new MarkerOptions().position(friend).title(names[i]));
                    }

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                    mMap.addCircle(new CircleOptions().center(latLng).radius(radius).strokeWidth(0f).fillColor(0x550000FF));

                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }

        });

        mapFragment.getMapAsync(this);

        nextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent submitPage = new Intent(locationActivity.this, submitActivity.class);
                submitPage.putExtra("KeyNAME", eName);
                submitPage.putExtra("keyDATE", dTime);
                submitPage.putExtra("keySearch", search);
                submitPage.putExtra("keyFriend", fcount);
                submitPage.putExtra("keyChosen", chosen);
                submitPage.putExtra("keyLength", (count-1));
                submitPage.putStringArrayListExtra("selectedUserIDs", chosenUserIDS);
                startActivity(submitPage);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public void distanceBetween(LatLng search, LatLng friend, double radius, int position){

        Location searchLoc = new Location("pointA");
        searchLoc.setLatitude(search.latitude);
        searchLoc.setLongitude(search.longitude);

        Location friendLoc = new Location("pointB");
        friendLoc.setLatitude(friend.latitude);
        friendLoc.setLongitude(friend.longitude);

        double distance = searchLoc.distanceTo(friendLoc);

        if (distance < radius){
            chosen[fcount] = names[position];
            chosenUserIDS.add(userIDs[position]);
            fcount++;
        }
    }
}