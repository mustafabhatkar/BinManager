package bin.project.binmanager;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    double[][] latLon = {{19.026756, 73.055807},{19.027111, 73.057295},{19.025488, 73.054763},{19.026606, 73.055233},{19.026264, 73.056633}};
    Marker marker;
    FirebaseDatabase database;
    DatabaseReference myRef;
    String TAG = MapsActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Bins").child("fill_level");

    }

    @Override
    public void onMapReady(final GoogleMap map) {

        map.addMarker(new MarkerOptions()
                .position(new LatLng(19.026738, 73.055215)));

        for (int i = 0;i<latLon.length;i++){
           marker =  createMarker(map, latLon[i][0], latLon[i][1], R.drawable.ic_bin_normal);
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(19.026738, 73.055215), 18));

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Long fillLevel = dataSnapshot.getValue(Long.class);
                Log.d(TAG, "Value is: " + fillLevel);
                marker.remove();
                if (fillLevel<30){
                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(19.026264, 73.056633))
                                    .anchor(0.5f, 0.5f)
                                    .title("Bin level")
                                    .snippet("90%")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_full)));
                }else {
                    map.addMarker(new MarkerOptions()
                                    .position(new LatLng(19.026264, 73.056633))
                                    .anchor(0.5f, 0.5f)
                                    .title("Bin level")
                                    .snippet("40%")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_normal)));
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    protected Marker createMarker(GoogleMap map, double latitude, double longitude, int iconResID) {

        return map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f)
                .title("Bin level")
                .snippet("40")
                .icon(BitmapDescriptorFactory.fromResource(iconResID)));
    }

    }


