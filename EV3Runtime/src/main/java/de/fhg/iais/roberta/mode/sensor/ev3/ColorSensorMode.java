package de.fhg.iais.roberta.mode.sensor.ev3;

import de.fhg.iais.roberta.inter.mode.sensor.IColorSensorMode;

public enum ColorSensorMode implements IColorSensorMode {
    COLOUR( "ColorID" ), RED( "Red" ), RGB( "RGB" ), AMBIENTLIGHT( "Ambient" );

    private final String[] values;

    private ColorSensorMode(String... values) {

        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}