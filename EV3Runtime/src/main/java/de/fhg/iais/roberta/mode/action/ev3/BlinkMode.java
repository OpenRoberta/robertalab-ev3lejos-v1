package de.fhg.iais.roberta.mode.action.ev3;

import de.fhg.iais.roberta.inter.mode.action.IBlinkMode;

public enum BlinkMode implements IBlinkMode {
    ON(), FLASH(), DOUBLE_FLASH();

    private final String[] values;

    private BlinkMode(String... values) {
        this.values = values;
    }

    @Override
    public String[] getValues() {
        return values;
    }

}