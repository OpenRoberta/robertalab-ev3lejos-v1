package de.fhg.iais.roberta.mode.sensor.ev3;

import de.fhg.iais.roberta.inter.mode.sensor.IPixySensorMode;

public enum PixySensorMode implements IPixySensorMode {
    ID( "ID" ), X( "X" ), Y( "Y" ), W( "W" ), H( "H" );

    private final String[] values;

    private PixySensorMode(String... values) {

        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}