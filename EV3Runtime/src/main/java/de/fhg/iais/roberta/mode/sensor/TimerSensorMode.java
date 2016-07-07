package de.fhg.iais.roberta.mode.sensor;

public enum TimerSensorMode implements ITimerSensorMode {
    RESET, GET_SAMPLE;

    private final String[] values;

    private TimerSensorMode(String... values) {
        this.values = values;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

}