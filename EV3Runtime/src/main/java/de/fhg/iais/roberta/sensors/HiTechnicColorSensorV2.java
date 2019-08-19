package de.fhg.iais.roberta.sensors;

import lejos.hardware.port.I2CPort;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.HiTechnicColorSensor;
import lejos.hardware.sensor.SensorMode;

public class HiTechnicColorSensorV2 extends HiTechnicColorSensor {

    private static final int HT_COLOR_SENSOR_V2_MODE_REGISTER = 0x41;

    public enum PowerMainsFrequency {
        FREQUENCY_50Hz((byte) 0x35), FREQUENCY_60Hz((byte) 0x36);

        final byte value;

        private PowerMainsFrequency(byte value) {
            this.value = value;
        }
    }

    private enum Mode {
        DEFAULT((byte) 0), PASSIVE((byte) 1), RAW((byte) 3);

        final byte value;

        private Mode(byte value) {
            this.value = value;
        }
    }

    private Mode currentMode = Mode.DEFAULT;

    public HiTechnicColorSensorV2(I2CPort port) {
        super(port);
    }

    public HiTechnicColorSensorV2(Port port) {
        super(port);
    }

    @Override
    protected void init() {
        setModes(new SensorMode[] {
            new ColorIDMode(),
            new RGBAMode(),
            new RGBA2ByteColor("RGBARaw", Mode.RAW),
            new RGBA2ByteColor("RGBAPassive", Mode.PASSIVE)
        });
    }

    public class ColorIDMode implements SensorMode {
        @Override public int sampleSize() {
            return 1;
        }

        @Override public void fetchSample(float[] sample, int offset) {
            setModeIfNeeded(Mode.DEFAULT);
            sample[offset] = (float) getColorID();
        }

        @Override public String getName() {
            return "ColorID";
        }
    }

    public class RGBAMode implements SensorMode {

        private static final int SAMPLE_SIZE = 4;

        private byte[] buffer = new byte[SAMPLE_SIZE];

        @Override public int sampleSize() {
            return SAMPLE_SIZE;
        }

        @Override public void fetchSample(float[] sample, int offset) {
            setModeIfNeeded(Mode.DEFAULT);
            getData(0x43, buffer, SAMPLE_SIZE);
            for ( int i = 0; i < SAMPLE_SIZE; i++ ) {
                sample[offset + i] = ((float) (0xFF & buffer[i])) / 256f;
            }
        }

        @Override public String getName() {
            return "RGBA";
        }
    }

    public class RGBA2ByteColor implements SensorMode {

        private static final int SAMPLE_SIZE = 4;

        private byte[] buffer = new byte[SAMPLE_SIZE * 2];

        private final String name;
        private final Mode mode;

        RGBA2ByteColor(String name, Mode mode) {
            this.name = name;
            this.mode = mode;
        }

        @Override public int sampleSize() {
            return SAMPLE_SIZE;
        }

        @Override public void fetchSample(float[] sample, int offset) {
            setModeIfNeeded(mode);
            getData(0x43, buffer, SAMPLE_SIZE);
            for ( int i = 0; i < SAMPLE_SIZE; i++ ) {
                byte first = buffer[i * 2];
                byte second = buffer[(i * 2) + 1];
                int temp = ((0xFF & first) << 8) | (0xFF & second);
                sample[offset + i] = ((float) temp) / 256f;
            }
        }

        @Override public String getName() {
            return name;
        }
    }


    private void setModeIfNeeded(Mode mode) {
        if (currentMode != mode) {
            currentMode = mode;
            writeSensorMode(mode.value);
        }
    }

    public void setPowerMainsFrequency(PowerMainsFrequency frequency) {
        writeSensorMode(frequency.value);
    }

    private void writeSensorMode(byte mode) {
        sendData(HT_COLOR_SENSOR_V2_MODE_REGISTER, mode);
    }
}
