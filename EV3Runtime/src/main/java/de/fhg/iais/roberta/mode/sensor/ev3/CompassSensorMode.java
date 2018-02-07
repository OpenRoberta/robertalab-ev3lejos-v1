package de.fhg.iais.roberta.mode.sensor.ev3;

import de.fhg.iais.roberta.inter.mode.sensor.ICompassSensorMode;

public enum CompassSensorMode implements ICompassSensorMode {
    ANGLE( "Angle" ), COMPASS( "Compass" ), CALIBRATE();

    private final String[] values;

    private CompassSensorMode(String... values) {
        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }
}
