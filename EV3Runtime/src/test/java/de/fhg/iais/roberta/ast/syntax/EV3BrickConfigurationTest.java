package de.fhg.iais.roberta.ast.syntax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.ActorType;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.EV3Configuration;
import de.fhg.iais.roberta.components.Sensor;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.mode.action.DriveDirection;
import de.fhg.iais.roberta.mode.action.MotorSide;
import de.fhg.iais.roberta.mode.action.ev3.ActorPort;
import de.fhg.iais.roberta.mode.sensor.ev3.SensorPort;

public class EV3BrickConfigurationTest {
    private static final String expectedBrickConfigurationGenerator = //
        "robotev3test{"//
            + "sensorport{1:ultrasonic;4:color;}"//
            + "actorport{A:largemotor,regulated,forward,left;B:middlemotor,regulated,forward,right;}}";

    @Test
    public void testBuilder() {
        Configuration.Builder builder = new EV3Configuration.Builder();
        builder.addActor(ActorPort.A, new Actor(ActorType.LARGE, true, DriveDirection.FOREWARD, MotorSide.LEFT));
        builder.addActor(ActorPort.B, new Actor(ActorType.MEDIUM, true, DriveDirection.FOREWARD, MotorSide.RIGHT));
        builder.addSensor(SensorPort.S1, new Sensor(SensorType.ULTRASONIC));
        builder.addSensor(SensorPort.S4, new Sensor(SensorType.COLOR));
        Configuration conf = builder.build();

        assertEquals(ActorType.LARGE, conf.getActorOnPort(ActorPort.A).getName());
        assertEquals(ActorType.MEDIUM, conf.getActorOnPort(ActorPort.B).getName());
        assertNull(conf.getActorOnPort(ActorPort.C));
        assertEquals(SensorType.ULTRASONIC, conf.getSensorOnPort(SensorPort.S1).getType());
        assertEquals(SensorType.COLOR, conf.getSensorOnPort(SensorPort.S4).getType());
        assertNull(conf.getSensorOnPort(SensorPort.S2));

        assertEquals(EV3BrickConfigurationTest.expectedBrickConfigurationGenerator, conf.generateText("test").replaceAll("\\s+", ""));
    }

    @Test
    public void testBuilderFluent() {
        Configuration conf =
            new EV3Configuration.Builder()
                .addActor(ActorPort.A, new Actor(ActorType.LARGE, true, DriveDirection.FOREWARD, MotorSide.LEFT))
                .addActor(ActorPort.B, new Actor(ActorType.MEDIUM, true, DriveDirection.FOREWARD, MotorSide.RIGHT))
                .addSensor(SensorPort.S1, new Sensor(SensorType.ULTRASONIC))
                .addSensor(SensorPort.S4, new Sensor(SensorType.COLOR))
                .build();

        assertEquals(ActorType.LARGE, conf.getActorOnPort(ActorPort.A).getName());
        assertEquals(ActorType.MEDIUM, conf.getActorOnPort(ActorPort.B).getName());
        assertNull(conf.getActorOnPort(ActorPort.C));
        assertEquals(SensorType.ULTRASONIC, conf.getSensorOnPort(SensorPort.S1).getType());
        assertEquals(SensorType.COLOR, conf.getSensorOnPort(SensorPort.S4).getType());
        assertNull(conf.getSensorOnPort(SensorPort.S2));

        assertEquals(EV3BrickConfigurationTest.expectedBrickConfigurationGenerator, conf.generateText("test").replaceAll("\\s+", ""));
    }

    @Test
    public void testText() {
        Configuration.Builder builder = new EV3Configuration.Builder();
        builder.setTrackWidth(4.0).setWheelDiameter(-3.1428);
        builder.addActor(ActorPort.A, new Actor(ActorType.LARGE, true, DriveDirection.FOREWARD, MotorSide.LEFT));
        builder.addActor(ActorPort.B, new Actor(ActorType.MEDIUM, false, DriveDirection.FOREWARD, MotorSide.RIGHT));
        builder.addSensor(SensorPort.S1, new Sensor(SensorType.ULTRASONIC));
        builder.addSensor(SensorPort.S4, new Sensor(SensorType.COLOR));
        Configuration conf = builder.build();

        String expected =
            "" //
                + "robot ev3 test {\n"
                + "  size {\n"
                + "    wheel diameter -3.1 cm;\n"
                + "    track width    4.0 cm;\n"
                + "  }\n"
                + "  sensor port {\n"
                + "    1: ultrasonic;\n"
                + "    4: color;\n"
                + "  }\n"
                + "  actor port {\n"
                + "    A: large motor, regulated, forward, left;\n"
                + "    B: middle motor, unregulated, forward, right;\n"
                + "  }\n"
                + "}";

        assertEquals(expected.replaceAll("\\s+", ""), conf.generateText("test").replaceAll("\\s+", ""));
    }

    //    @Test
    //    public void testVisitorBuilder() {
    //        EV3BrickConfiguration.Builder builder = new EV3BrickConfiguration.Builder();
    //        builder.visiting("regulated", "large", "left", "off");
    //        builder.visitingActorPort("A");
    //        builder.visiting("regulated", "middle", "right", "off");
    //        builder.visitingActorPort("B");
    //        builder.visiting("ultrasonic");
    //        builder.visitingSensorPort("1");
    //        builder.visiting("Farbe");
    //        builder.visitingSensorPort("4");
    //        EV3BrickConfiguration conf = builder.build();
    //
    //        assertEquals(HardwareComponentType.EV3LargeRegulatedMotor, conf.getActorOnPort(ActorPort.A).getComponentType());
    //        assertEquals(HardwareComponentType.EV3MediumRegulatedMotor, conf.getActorOnPort(ActorPort.B).getComponentType());
    //        assertEquals(HardwareComponentType.EV3UltrasonicSensor, conf.getSensorOnPort(SensorPort.S1).getComponentType());
    //        assertEquals(HardwareComponentType.EV3ColorSensor, conf.getSensorOnPort(SensorPort.S4).getComponentType());
    //
    //        assertEquals(this.expectedBrickConfigurationGenerator, conf.generateRegenerate().replaceAll("\\s+", ""));
    //    }
    //
    //    @Test(expected = DbcException.class)
    //    public void testVisitorExc1() {
    //        EV3BrickConfiguration.Builder builder = new EV3BrickConfiguration.Builder();
    //        builder.visiting("regulated", "latsch");
    //        builder.visitingActorPort("A");
    //    }
    //
    //    @Test(expected = DbcException.class)
    //    public void testVisitorExc2() {
    //        EV3BrickConfiguration.Builder builder = new EV3BrickConfiguration.Builder();
    //        builder.visiting("regulated", "large");
    //        builder.visitingActorPort("X");
    //    }
}
