package de.fhg.iais.roberta.runtime.ev3.deprecated;

import java.util.LinkedHashSet;
import java.util.Set;

import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.ActorType;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.Sensor;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.runtime.ev3.Hal;
import de.fhg.iais.roberta.shared.action.ActorPort;
import de.fhg.iais.roberta.shared.action.DriveDirection;
import de.fhg.iais.roberta.shared.action.MotorSide;
import de.fhg.iais.roberta.shared.action.ShowPicture;
import de.fhg.iais.roberta.shared.sensor.BrickKey;
import de.fhg.iais.roberta.shared.sensor.SensorPort;

public class NEPOprog {
    private static final boolean TRUE = true;
    private static Configuration brickConfiguration;

    private final Set<UsedSensor> usedSensors = new LinkedHashSet<UsedSensor>();

    private final Hal hal = new Hal(brickConfiguration, this.usedSensors);

    public static void main(String[] args) {
        try {
            brickConfiguration =
                new Configuration.Builder()
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

        this.hal.drawPicture(ShowPicture.OLDGLASSES, 0, 0);
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
