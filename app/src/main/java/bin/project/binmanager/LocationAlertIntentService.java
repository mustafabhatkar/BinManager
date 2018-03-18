package bin.project.binmanager;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * Created by Mustafa on 17-03-2018.
 */

public class LocationAlertIntentService extends IntentService {
    private static final String IDENTIFIER = "LocationAlertIS";


    public LocationAlertIntentService() {
        super(IDENTIFIER);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String binNo = "";

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        Log.d(TAG, "onHandleIntent: geofencingEvent "+ geofencingEvent);

        binNo = intent.getStringExtra("binNo");
        Log.d(TAG, "onHandleIntent: binNo "+intent.getStringExtra("binNo"));
        if (geofencingEvent.hasError()) {
            Log.e(IDENTIFIER, "" + getErrorString(geofencingEvent.getErrorCode()));
            return;
        }

        Log.i(IDENTIFIER, geofencingEvent.toString());
         int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Intent toAct = new Intent(this, MapsActivity.class);
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String transitionDetails = getGeofenceTransitionInfo(
                    triggeringGeofences);

            String transitionType = getTransitionString(geofenceTransition, binNo);
            notifyLocationAlert(transitionType, transitionDetails);
            sendLocationBroadcast();
        }
    }

    private String getGeofenceTransitionInfo(List<Geofence> triggeringGeofences) {
        ArrayList<String> locationNames = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            locationNames.add(getLocationName(geofence.getRequestId()));
        }
        String triggeringLocationsString = TextUtils.join(", ", locationNames);

        return triggeringLocationsString;
    }

    private String getLocationName(String key) {
        String[] strs = key.split("-");

        String locationName = null;
        if (strs != null && strs.length == 2) {
            double lat = Double.parseDouble(strs[0]);
            double lng = Double.parseDouble(strs[1]);

            locationName = getLocationNameGeocoder(lat, lng);
        }
        if (locationName != null) {
            return locationName;
        } else {
            return key;
        }
    }

    private String getLocationNameGeocoder(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
        } catch (Exception ioException) {
            Log.e("", "Error in getting location name for the location");
        }

        if (addresses == null || addresses.size() == 0) {
            Log.d("", "no location name");
            return null;
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressInfo = new ArrayList<>();
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressInfo.add(address.getAddressLine(i));
            }

            return TextUtils.join(System.getProperty("line.separator"), addressInfo);
        }
    }

    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "geofence too many_geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "geofence too many pending_intents";
            default:
                return "geofence error";
        }
    }

    private String getTransitionString(int transitionType, String binNo) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:

                return "bin "+ binNo;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Exit bin "+ binNo;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "bin "+ binNo;
            default:
                return "location transition";
        }
    }

    private void notifyLocationAlert(String locTransitionType, String locationDetails) {
        Intent intent = new Intent("bina");
        intent.putExtra("s","sd");
        //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //PendingIntent pendingIntent = PendingIntent.getActivity(this,1,intent, PendingIntent.FLAG_UPDATE_CURRENT);


        String CHANNEL_ID = "Zoftino";
         NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(locTransitionType)
                        .setContentText("Click to mark bin for pickup")
                        .setContentIntent(pendingIntent);
                ;

        builder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, builder.build());
    }
    private PendingIntent getGeofencePendingIntent(){
        Intent intent = new Intent(LocationAlertIntentService.this, MapsActivity.class);
        intent.putExtra("s","sd");
        return PendingIntent.getActivity(this,1,intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private void sendLocationBroadcast(){

        Intent intent = new Intent("bina");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }


}
