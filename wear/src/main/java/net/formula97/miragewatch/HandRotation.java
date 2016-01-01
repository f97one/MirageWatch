package net.formula97.miragewatch;

import java.io.Serializable;

/**
 * Created by f97one on 16/01/01.
 */
public class HandRotation implements Serializable {

    private static final long serialVersionUID = 3646948579296545147L;

    private float hourHand;
    private float minuteHand;
    private float secondHand;

    public HandRotation() {
        hourHand = 0f;
        minuteHand = 0f;
        secondHand = 0f;
    }

    public float getHourHand() {
        return hourHand;
    }

    public void setHourHand(float hourHand) {
        this.hourHand = hourHand;
    }

    public float getMinuteHand() {
        return minuteHand;
    }

    public void setMinuteHand(float minuteHand) {
        this.minuteHand = minuteHand;
    }

    public float getSecondHand() {
        return secondHand;
    }

    public void setSecondHand(float secondHand) {
        this.secondHand = secondHand;
    }
}
