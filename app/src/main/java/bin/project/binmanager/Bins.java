package bin.project.binmanager;

/**
 * Created by Mustafa on 29-01-2018.
 */

//pushing on git
public class Bins {
    double lat, lng;
    long fill_level;


    public Bins() {
    }

    public Bins(double lat, double lng, long fill_level) {
        this.lat = lat;
        this.lng = lng;
        this.fill_level = fill_level;

    }
}
