package de.fhg.iais.roberta.mode.sensor.ev3;

import de.fhg.iais.roberta.inter.mode.sensor.IBrickKey;

public enum BrickKey implements IBrickKey {
    ENTER(), UP(), DOWN(), LEFT(), RIGHT(), ESCAPE(), ANY();

    private final String[] values;

    private BrickKey(String... values) {
        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}