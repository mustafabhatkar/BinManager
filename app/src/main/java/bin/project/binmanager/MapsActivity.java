package bin.project.binmanager;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

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
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private double[][] latLon = new double[100][2];
    //{{19.026756, 73.055807}, {19.027111, 73.057295}, {19.025488, 73.054763}, {19.026606, 73.055233}, {19.026264, 73.056633}};
    private Marker marker;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private String TAG = MapsActivity.class.getSimpleName();
    private double lat, lng;
    private long fill_level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Bins");

    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.directionsApiKey))
                .queryRateLimit(3)
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build();
        return geoApiContext;
    }


    @Override
    public void onMapReady(final GoogleMap map) {
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int i = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Bins bins = snapshot.getValue(Bins.class);
                    lat = bins.lat;
                    lng = bins.lng;
                    fill_level = bins.fill_level;
                    marker = createMarker(map, lat, lng, R.drawable.ic_bin_normal, fill_level);
                    latLon[i][0] = lat;
                    latLon[i][1] = lng;
                    i++;
                }
                for (int j = 0; j < dataSnapshot.getChildrenCount(); j++) {
                    addDistanceOfBinsToDb(dataSnapshot.getChildrenCount(), latLon[j][0], latLon[j][1], j);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(19.026738, 73.055215), 18));
    }

    protected Marker createMarker(GoogleMap map, double latitude, double longitude, int iconResID, long fill_level) {

        return map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f)
                .title("Bin level")
                .snippet(String.valueOf(fill_level))
                .icon(BitmapDescriptorFactory.fromResource(iconResID)));
    }


    private void addDistanceOfBinsToDb(long noOFbins, double lat, double lng, int currentBin) {
        DateTime now = new DateTime();
        try {

            for (int i = 1; i <= noOFbins; i++) {
                if (lat != latLon[i - 1][0]) {
                    DirectionsResult result = DirectionsApi.newRequest(getGeoContext())
                            .mode(TravelMode.DRIVING)
                            .origin(new com.google.maps.model.LatLng(lat, lng))
                            .destination(new com.google.maps.model.LatLng(latLon[i - 1][0], latLon[i - 1][1]))
                            .departureTime(now)
                            .await();
                    int binNumber = i;
                    myRef.child(String.valueOf(currentBin + 1))
                            .child("distance from " + binNumber)
                            .setValue(result.routes[0].legs[0].distance.humanReadable);
                }
            }

//            Log.d(TAG, "Time :" + result.routes[0].legs[0].duration.humanReadable + " Distance :"
//                    + result.routes[0].legs[0].distance.humanReadable);

        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


