package bin.project.binmanager;

/**
 * Created by Mustafa on 01-02-2018.
 */

public class User {
    public String display_name;
    public double lat, lng;

    public User() {
    }

    public User(String display_name, double lat, double lng) {
        this.display_name = display_name;
        this.lat = lat;
        this.lng = lng;
    }
}
