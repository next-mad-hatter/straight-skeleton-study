package at.tugraz.igi.algorithm;

import lombok.*;
import at.tugraz.igi.main.Controller;

/**
 * Things we publish as updates from our algorithm, implemented as swing worker,
 * to the controller.
 */
public class AlgoChunk {
    public String eventName;
    public boolean isEvent;
    public double loc_x;
    public double loc_y;

    public AlgoChunk(String eventName, boolean isEvent, double x, double y) {
        this.eventName = eventName;
        this.isEvent = isEvent;
        loc_x = x;
        loc_y = y;
    }
}
