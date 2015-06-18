package trialbycombat.com.whengreen;

import android.text.format.Time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by IS96266 on 03.06.2015.
 */
public class Light implements Serializable {
    private UUID lightID;
    private String lightName;
    private String lightLocation;
    private ArrayList<String> lightActivityTimes;


    public String getLightName() {
        if(lightName==null)
            return "";
        return lightName;
    }

    public void setLightName(String lightName) {
        this.lightName = lightName;
    }

    public String getLightLocation() {
        if(lightLocation==null)
            return "";
        return lightLocation;
    }

    public void setLightLocation(String lightLocation) {
        this.lightLocation = lightLocation;
    }

    public ArrayList<String> getLightActivityTimes() {
        if(lightLocation==null)
            return new ArrayList<>();
        return lightActivityTimes;
    }

    public void setLightActivityTimes(ArrayList<String> lightActivityTimes) {
        this.lightActivityTimes = lightActivityTimes;
    }

    public UUID getLightID() {
        return lightID;
    }

    public void setLightID(UUID lightID) {
        this.lightID = lightID;
    }

    @Override
    public String toString() {
        return lightName;
    }
}
