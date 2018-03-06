package bin.project.binmanager;

import java.io.Serializable;

/**
 * Created by Mustafa on 29-01-2018.
 */


public class Bins implements Serializable{
    public double lat, lng;
    public long fill_level;


    public Bins() {
    }

    public Bins(double lat, double lng, long fill_level) {
        this.lat = lat;
        this.lng = lng;
        this.fill_level = fill_level;

    }
}
