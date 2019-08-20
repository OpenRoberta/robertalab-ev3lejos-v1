package de.fhg.iais.roberta.mode.sensor.ev3;

import de.fhg.iais.roberta.inter.mode.sensor.IHiTecColorSensorV2Mode;

public enum HiTecColorSensorV2Mode implements IHiTecColorSensorV2Mode {
    COLOUR("ColorID"), RGB("RGBA"), LIGHT("RGBA"), AMBIENTLIGHT("RGBAPassive");

    private final String[] values;

    private HiTecColorSensorV2Mode(String... values) {
        this.values = values;
    }

    @Override public String[] getValues() {
        return values;
    }
}
