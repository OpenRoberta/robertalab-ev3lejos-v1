package de.fhg.iais.roberta.mode.sensor;

import de.fhg.iais.roberta.inter.mode.sensor.IColorSensorMode;

public enum ColorSensorMode implements IColorSensorMode {
    COLOUR( "ColorID" ), RED( "Red" ), RGB( "RGB" ), AMBIENTLIGHT( "Ambient" );

    private final String[] values;

    private ColorSensorMode(String... values) {
        this.values = values;
    }

    /**
     * @return name that Lejos is using for this mode
     */
    public String getLejosModeName() {
        return this.values[0];
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}