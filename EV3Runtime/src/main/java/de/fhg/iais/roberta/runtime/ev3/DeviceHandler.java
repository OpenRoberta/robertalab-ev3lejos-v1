package de.fhg.iais.roberta.runtime.ev3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.Sensor;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.mode.action.ev3.ActorPort;
import de.fhg.iais.roberta.mode.sensor.ev3.SensorPort;
import de.fhg.iais.roberta.util.dbc.DbcException;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.hardware.motor.UnregulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.BaseSensor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3GyroSensor;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.HiTechnicCompass;
import lejos.hardware.sensor.NXTSoundSensor;
import lejos.robotics.EncoderMotor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;

/**
 * This class instantiates all sensors (sensor modes) and actors used in blockly program.
 */
public class DeviceHandler {
    private final Set<UsedSensor> usedSensors;
    private final Map<SensorPort, SampleProviderBean[]> lejosSensors = new HashMap<>();
    private EV3GyroSensor gyroSensor = null;
    private HiTechnicCompass hiTechnicCompass = null;

    private final Map<ActorPort, RegulatedMotor> lejosRegulatedMotors = new HashMap<>();
    private final Map<ActorPort, EncoderMotor> lejosUnregulatedMotors = new HashMap<>();

    private final TextLCD lcd = LocalEV3.get().getTextLCD();

    /**
     * Construct new initialization for actors and sensors on the brick. Client must provide
     * brick configuration ({@link Configuration}) and used sensors in program.
     *
     * @param brickConfiguration for the particular brick
     * @param usedSensors in the blockly program
     */
    public DeviceHandler(Configuration brickConfiguration, Set<UsedSensor> usedSensors) {
        this.usedSensors = usedSensors;
        createDevices(brickConfiguration);
    }

    /**
     * Get regulated motor connected on port ({@link ActorPort})
     *
     * @exception DbcException if regulated motor does not exist
     * @param actorPort on which the motor is connected
     * @return regulate motor
     */
    public RegulatedMotor getRegulatedMotor(ActorPort actorPort) {
        RegulatedMotor motor = this.lejosRegulatedMotors.get(actorPort);
        if ( motor == null ) {
            throw new DbcException("No regulated motor on port " + actorPort + "!");
        }
        return motor;
    }

    /**
     * Get unregulated motor connected on port ({@link ActorPort})
     *
     * @exception DbcException if unregulated motor does not exist
     * @param actorPort on which the motor is connected
     * @return unregulated motor
     */
    public EncoderMotor getUnregulatedMotor(ActorPort actorPort) {
        EncoderMotor motor = this.lejosUnregulatedMotors.get(actorPort);
        if ( motor == null ) {
            throw new DbcException("No unregulated motor on port " + actorPort + "!");
        }
        return motor;
    }

    /**
     * @exception DbcException if the sensor is not connected
     * @return the gyroSensor
     */
    public EV3GyroSensor getGyroSensor() {
        if ( this.gyroSensor == null ) {
            throw new DbcException("No Gyro Sensor Connected!");
        }
        return this.gyroSensor;
    }

    public HiTechnicCompass getHiTechnicCompass() {
        return this.hiTechnicCompass;
    }

    /**
     * Sample provider of given mode connected on given port. <br>
     * <br>
     * Throws an exception if the sensor mode is not available on the given port.
     *
     * @param sensorPort of the sensors
     * @param sensorMode in which sensor operates
     * @return sample provider
     */
    public SampleProvider getProvider(SensorPort sensorPort, String sensorMode) {
        SampleProviderBean[] sampleProviders = this.lejosSensors.get(sensorPort);
        if ( sampleProviders == null ) {
            throw new DbcException("Sensor mode " + sensorMode + " not avaliable on port " + sensorPort);
        }
        return findProviderByMode(sampleProviders, sensorMode);
    }

    private void createDevices(Configuration brickConfiguration) {
        initMotor(ActorPort.A, brickConfiguration.getActorOnPort(ActorPort.A), lejos.hardware.port.MotorPort.A);
        initMotor(ActorPort.B, brickConfiguration.getActorOnPort(ActorPort.B), lejos.hardware.port.MotorPort.B);
        initMotor(ActorPort.C, brickConfiguration.getActorOnPort(ActorPort.C), lejos.hardware.port.MotorPort.C);
        initMotor(ActorPort.D, brickConfiguration.getActorOnPort(ActorPort.D), lejos.hardware.port.MotorPort.D);

        initSensor(SensorPort.S1, brickConfiguration.getSensorOnPort(SensorPort.S1), lejos.hardware.port.SensorPort.S1);
        initSensor(SensorPort.S2, brickConfiguration.getSensorOnPort(SensorPort.S2), lejos.hardware.port.SensorPort.S2);
        initSensor(SensorPort.S3, brickConfiguration.getSensorOnPort(SensorPort.S3), lejos.hardware.port.SensorPort.S3);
        initSensor(SensorPort.S4, brickConfiguration.getSensorOnPort(SensorPort.S4), lejos.hardware.port.SensorPort.S4);
        this.lcd.clear();
    }

    private void initMotor(ActorPort actorPort, Actor actorType, Port hardwarePort) {
        if ( actorType != null ) {
            if ( actorType.isRegulated() ) {
                initRegulatedMotor(actorPort, actorType, hardwarePort);
            } else {
                initUnregulatedMotor(actorPort, hardwarePort);
            }
        }
    }

    private void initUnregulatedMotor(ActorPort actorPort, Port hardwarePort) {
        UnregulatedMotor nxtMotor = new UnregulatedMotor(hardwarePort);
        nxtMotor.resetTachoCount();
        this.lejosUnregulatedMotors.put(actorPort, nxtMotor);
    }

    private void initRegulatedMotor(ActorPort actorPort, Actor actorType, Port hardwarePort) {
        this.lcd.clear();
        // Hal.formatInfoMessage("Initializing motor on port " + actorPort, this.lcd);
        switch ( actorType.getName() ) {
            case LARGE:
                this.lejosRegulatedMotors.put(actorPort, new EV3LargeRegulatedMotor(hardwarePort));
                break;
            case MEDIUM:
                this.lejosRegulatedMotors.put(actorPort, new EV3MediumRegulatedMotor(hardwarePort));
                break;
            case REGULATED:
                this.lejosRegulatedMotors.put(actorPort, new NXTRegulatedMotor(hardwarePort));
                break;
            default:
                throw new DbcException("Actor type " + actorType.getName() + " does not exists!");
        }
    }

    private boolean isUsed(Sensor sensor) {
        for ( UsedSensor usedSensor : this.usedSensors ) {
            if ( usedSensor.getSensorType().equals(sensor.getType()) ) {
                return true;
            }
        }
        return false;
    }

    private void initSensor(SensorPort sensorPort, Sensor sensorType, Port hardwarePort) {
        if ( sensorType != null && isUsed(sensorType) ) {
            this.lcd.clear();
            // Hal.formatInfoMessage("Initializing " + sensorType.getComponentType().getShortName() + " on port " + sensorPort + " ...", this.lcd);
            switch ( sensorType.getType() ) {
                case COLOR:
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(new EV3ColorSensor(hardwarePort)));
                    break;
                case INFRARED:
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(new EV3IRSensor(hardwarePort)));
                    break;
                case GYRO:
                    this.gyroSensor = new EV3GyroSensor(hardwarePort);
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(this.gyroSensor));
                    break;
                case TOUCH:
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(new EV3TouchSensor(hardwarePort)));
                    break;
                case ULTRASONIC:
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(new EV3UltrasonicSensor(hardwarePort)));
                    break;
                case COMPASS_HI_TEC:
                    this.hiTechnicCompass = new HiTechnicCompass(hardwarePort);
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(this.hiTechnicCompass));
                    break;
                case SOUND:
                    this.lejosSensors.put(sensorPort, sensorSampleProviders(new NXTSoundSensor(hardwarePort)));
                    break;
                default:
                    throw new DbcException("Sensor type " + sensorType.getType() + " does not exists!");
            }
        }
    }

    private SampleProvider findProviderByMode(SampleProviderBean[] sampleProviders, String sensorMode) {
        for ( SampleProviderBean bean : sampleProviders ) {
            if ( bean.getModeName().equals(sensorMode) ) {
                return bean.getSampleProvider();
            }
        }
        throw new DbcException(sensorMode + " sample provider does not exists!");
    }

    private SampleProviderBean[] sensorSampleProviders(BaseSensor sensor) {
        SampleProviderBean[] sampleProvider = new SampleProviderBean[sensor.getAvailableModes().size()];
        int i = 0;
        for ( String modeName : sensor.getAvailableModes() ) {
            SampleProviderBean providerBean = new SampleProviderBean();
            providerBean.setModeName(modeName);
            providerBean.setSampleProvider(sensor.getMode(modeName));
            sampleProvider[i] = providerBean;
            i++;
        }
        return sampleProvider;
    }
}
