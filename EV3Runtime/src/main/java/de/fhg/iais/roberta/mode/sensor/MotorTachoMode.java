package de.fhg.iais.roberta.mode.sensor;

public enum MotorTachoMode implements IMotorTachoMode {
    ROTATION(), DEGREE(), RESET(), DISTANCE();

    private final String[] values;

    private MotorTachoMode(String... values) {
        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}