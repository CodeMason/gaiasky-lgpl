package gaia.cu9.ari.gaiaorbit.util.time;

import java.util.Date;

import com.badlogic.gdx.utils.TimeUtils;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;

public class RealTimeClock implements ITimeFrameProvider {
    private static final double SEC_TO_HOUR = 1d / 3600d;

    Date time;
    double dtHours;
    double lastUpdate = 0;

    public RealTimeClock() {
        time = new Date();
    }

    /**
     * The dt in hours
     */
    @Override
    public double getDt() {
        return SEC_TO_HOUR;
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public void update(double dt) {
        dtHours = dt * SEC_TO_HOUR;
        time.setTime(TimeUtils.millis());

        // Post event each 1/2 second
        lastUpdate += dt;
        if (lastUpdate > .5) {
            EventManager.instance.post(Events.TIME_CHANGE_INFO, time);
            lastUpdate = 0;
        }
    }

    @Override
    public double getWarpFactor() {
        return SEC_TO_HOUR;
    }

    @Override
    public boolean isFixedRateMode() {
        return false;
    }

    @Override
    public float getFixedRate() {
        return -1;
    }

}
