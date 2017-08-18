package de.fhg.iais.roberta.runtime.ev3.deprecated;

import java.util.LinkedHashSet;
import java.util.Set;

import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.ActorType;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.EV3Configuration;
import de.fhg.iais.roberta.components.Sensor;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.mode.action.DriveDirection;
import de.fhg.iais.roberta.mode.action.MotorSide;
import de.fhg.iais.roberta.mode.action.ev3.ActorPort;
import de.fhg.iais.roberta.mode.sensor.ev3.BrickKey;
import de.fhg.iais.roberta.mode.sensor.ev3.SensorPort;
import de.fhg.iais.roberta.runtime.ev3.Hal;

public class NEPOprog {
    private static final boolean TRUE = true;
    private static Configuration brickConfiguration;

    private final Set<UsedSensor> usedSensors = new LinkedHashSet<>();

    private final Hal hal = new Hal(brickConfiguration, this.usedSensors);

    public static void main(String[] args) {
        try {
            brickConfiguration =
                new EV3Configuration.Builder()
                    .setWheelDiameter(5.6)
                    .setTrackWidth(12.0)
                    .addActor(ActorPort.B, new Actor(ActorType.LARGE, true, DriveDirection.FOREWARD, MotorSide.RIGHT))
                    .addActor(ActorPort.C, new Actor(ActorType.LARGE, true, DriveDirection.FOREWARD, MotorSide.LEFT))
                    .addSensor(SensorPort.S1, new Sensor(SensorType.TOUCH))
                    .addSensor(SensorPort.S4, new Sensor(SensorType.ULTRASONIC))
                    .build();
            new NEPOprog().run();
        } catch ( Exception e ) {
            lejos.hardware.lcd.TextLCD lcd = lejos.hardware.ev3.LocalEV3.get().getTextLCD();
            lcd.clear();
            lcd.drawString("Error in the EV3", 0, 0);
            if ( e.getMessage() != null ) {
                lcd.drawString("Error message:", 0, 2);
                Hal.formatInfoMessage(e.getMessage(), lcd);
            }
            lcd.drawString("Press any key", 0, 7);
            lejos.hardware.Button.waitForAnyPress();
        }
    }

    public void run() throws Exception {

        this.hal.drawPicture("", 0, 0);
        if ( TRUE ) {
            while ( true ) {
                if ( this.hal.isPressed(BrickKey.ENTER) == true ) {
                    break;
                }
                this.hal.waitFor(15);
            }
        }
        this.hal.closeResources();
    }
}
