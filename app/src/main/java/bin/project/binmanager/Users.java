package bin.project.binmanager;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by ameym on 31-01-2018.
 */

@IgnoreExtraProperties
public class Users {

    public String display_name;
    public String email;
    public Double lat;
    public Double lng;

    public Users() {
     }

    public Users(String display_name, String email) {
        this.display_name = display_name;
        this.email = email;
    }
    public Users(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;


    }

}