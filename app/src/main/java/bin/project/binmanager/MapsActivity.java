package bin.project.binmanager;

import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
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
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private double[][] latLon = new double[100][2];
    //{{19.026756, 73.055807}, {19.027111, 73.057295}, {19.025488, 73.054763}, {19.026606, 73.055233}, {19.026264, 73.056633}};

    private long[] fillLevelArray = new long[100];

    private Marker marker;
    private Marker locationMarker;

    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;

    private DatabaseReference myRefBin;
    private DatabaseReference myRefUsers;

    private String TAG = MapsActivity.class.getSimpleName();
    private double lat, lng;
    private long fill_level;
    private String disp_name;
    private String email;

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private List<LatLng> waypoints = new ArrayList<>();

    private Toolbar toolbar;

    private AccountHeader headerResult;

    LocationManager locationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        toolbar = findViewById(R.id.map_toolbar);
        setSupportActionBar(toolbar);
        createDrawer();

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRefBin = database.getReference("Bins");
        myRefUsers = database.getReference("Users").child(firebaseAuth.getCurrentUser().getUid());
        Toast.makeText(this, "Logged in as " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();


        myRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Users users = dataSnapshot.getValue(Users.class);
                disp_name = users.display_name;
                email = users.email;
                headerResult.addProfiles(
                        new ProfileDrawerItem().withName(disp_name).withEmail(email)
                );

            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        //for toolbar transparency
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


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
        mMap = map;
        myRefBin.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int i = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Bins bins = snapshot.getValue(Bins.class);
                    lat = bins.lat;
                    lng = bins.lng;
                    fill_level = bins.fill_level;
                    if (fill_level >= 80) {
                        waypoints.add(new LatLng(lat, lng));
                    }
                    marker = createMarker(map, lat, lng, R.drawable.ic_bin_normal, fill_level);
                    latLon[i][0] = lat;
                    latLon[i][1] = lng;
                    i++;
                }
                /*
                for (int j = 0; j < dataSnapshot.getChildrenCount(); j++) {
                    addDistanceOfBinsToDb(dataSnapshot.getChildrenCount(), latLon[j][0], latLon[j][1], j);
                }
                */

                changeBinMarker(marker, map);
                getUserLocation();


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

   /*     if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }*/


    }

    private void changeBinMarker(final Marker marker, final GoogleMap map) {
        myRefBin.child("4").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Bins bins = dataSnapshot.getValue(Bins.class);
                long fill_level = bins.fill_level;
                marker.remove();
                if (fill_level >= 80) {
                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(19.027025, 73.057561))
                            .anchor(0.5f, 0.5f)
                            .title("Bin level")
                            .snippet(String.valueOf(fill_level))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_full)));
                } else {
                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(19.027025, 73.057561))
                            .anchor(0.5f, 0.5f)
                            .title("Bin level")
                            .snippet(String.valueOf(fill_level))
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
                    myRefBin.child(String.valueOf(currentBin + 1))
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

    private void createDrawer() {
        headerResult = new AccountHeaderBuilder()
                .withActivity(MapsActivity.this)
                .withHeaderBackground(R.color.primary_dark)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();


        //Now create your drawer and pass the AccountHeader.Result
        Drawer result = new DrawerBuilder()
                .withAccountHeader(headerResult)
                .withActivity(MapsActivity.this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new SecondaryDrawerItem().withName(R.string.dashboard),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_signout)
                ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        switch (position) {
                            case 1:
                                startActivity(new Intent(MapsActivity.this, Dashboard.class));
                                break;
                            case 2:
                                firebaseAuth.signOut();
                                Intent toLogin = new Intent(MapsActivity.this, LoginActivity.class);
                                startActivity(toLogin);
                        }

                        return false;
                    }
                }).build();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
    }

    private LatLng getUserLocation() {
        final LatLng[] latLng = new LatLng[1];
        myRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                double latitude = user.lat;
                double longitude = user.lng;
                latLng[0] = new LatLng(latitude, longitude);
                if (locationMarker != null)
                    locationMarker.remove();
                locationMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng[0]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user))
                        .title("You")

                );
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng[0], 15));
                showDirection(latLng[0]);


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return latLng[0];
/*        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                      if(locationMarker!= null)
                          locationMarker.remove();
                       locationMarker = mMap.addMarker(new MarkerOptions()
                               .position(latLng)
                               .title("You")

                       );
                     mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    myRefUsers.child("lat").setValue(latitude);
                    myRefUsers.child("lng").setValue(longitude);

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
        else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                    if(locationMarker!= null)
                        locationMarker.remove();
                    locationMarker = mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("You")

                    );
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    myRefUsers.child("lat").setValue(latitude);
                    myRefUsers.child("lng").setValue(longitude);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }*/
    }


    private void showDirection(LatLng userLocation) {
        GoogleDirection.withServerKey(getString(R.string.directionsApiKey))
                .from(userLocation)
                .and(waypoints)
                .to(waypoints.get(waypoints.size() - 1))
                .optimizeWaypoints(true)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            // Do something
                            Log.d(TAG, "onDirectionSuccess: success");
                            Route route = direction.getRouteList().get(0);
                            int legCount = route.getLegList().size();
                            for (int index = 0; index < legCount; index++) {
                                Leg leg = route.getLegList().get(index);
                                mMap.addMarker(new MarkerOptions().position(leg.getStartLocation().getCoordination()));
                                if (index == legCount - 1) {
                                    mMap.addMarker(new MarkerOptions().position(leg.getEndLocation().getCoordination()));
                                }
                                List<Step> stepList = leg.getStepList();
                                ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(MapsActivity.this, stepList, 5, Color.RED, 3, Color.BLUE);
                                for (PolylineOptions polylineOption : polylineOptionList) {
                                    mMap.addPolyline(polylineOption);
                                }
                            }
                            setCameraWithCoordinationBounds(route);

                        } else {
                            // Do something
                            Log.d(TAG, "onDirectionSuccess: fail ");
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                        Log.d(TAG, "onDirectionFailure: failed");
                    }
                });
    }

    @Override
    public void onBackPressed() {
        //exit on double click?
        //NO
    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }
}


