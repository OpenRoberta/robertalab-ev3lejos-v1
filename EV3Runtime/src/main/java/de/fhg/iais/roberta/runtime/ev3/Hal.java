package de.fhg.iais.roberta.runtime.ev3;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.fhg.iais.roberta.components.Actor;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.SensorType;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.inter.mode.action.IActorPort;
import de.fhg.iais.roberta.inter.mode.general.IMode;
import de.fhg.iais.roberta.mode.action.DriveDirection;
import de.fhg.iais.roberta.mode.action.MotorMoveMode;
import de.fhg.iais.roberta.mode.action.MotorStopMode;
import de.fhg.iais.roberta.mode.action.TurnDirection;
import de.fhg.iais.roberta.mode.action.ev3.ActorPort;
import de.fhg.iais.roberta.mode.action.ev3.BlinkMode;
import de.fhg.iais.roberta.mode.action.ev3.BrickLedColor;
import de.fhg.iais.roberta.mode.action.ev3.ShowPicture;
import de.fhg.iais.roberta.mode.general.PickColor;
import de.fhg.iais.roberta.mode.sensor.ev3.BrickKey;
import de.fhg.iais.roberta.mode.sensor.ev3.ColorSensorMode;
import de.fhg.iais.roberta.mode.sensor.ev3.CompassSensorMode;
import de.fhg.iais.roberta.mode.sensor.ev3.GyroSensorMode;
import de.fhg.iais.roberta.mode.sensor.ev3.IRSeekerSensorMode;
import de.fhg.iais.roberta.mode.sensor.ev3.InfraredSensorMode;
import de.fhg.iais.roberta.mode.sensor.ev3.MotorTachoMode;
import de.fhg.iais.roberta.mode.sensor.ev3.SensorPort;
import de.fhg.iais.roberta.mode.sensor.ev3.SoundSensorMode;
import de.fhg.iais.roberta.mode.sensor.ev3.UltrasonicSensorMode;
import de.fhg.iais.roberta.runtime.Utils;
import de.fhg.iais.roberta.util.dbc.DbcException;
import lejos.hardware.ev3.EV3;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.Image;
import lejos.hardware.lcd.LCDOutputStream;
import lejos.hardware.lcd.TextLCD;
import lejos.internal.ev3.EV3IOPort;
import lejos.remote.nxt.NXTConnection;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.utility.Delay;
import lejos.utility.Stopwatch;

/**
 * Connection class between our generated code and the leJOS API
 *
 * @author dpyka
 */
public class Hal {

    //    private enum ConnectionType {
    //        NONE, WEBSOCKET, REST
    //    };

    private static final int NUMBER_OF_CHARACTERS_IN_ROW = 17;
    private static final int BLUETOOTH_TIMEOUT = 20;

    //    private static final String OPENROBERTAPROPERTIESFILE = "/home/roberta/openroberta.properties";

    private final Set<UsedSensor> usedSensors;
    private final DeviceHandler deviceHandler;

    private final EV3 brick;
    private final LCDOutputStream lcdos = new LCDOutputStream();

    private final Stopwatch[] timers = new Stopwatch[5];

    private final double wheelDiameter;
    private final double trackWidth;

    MovePilot mPilot = null;
    Chassis chassis = null;

    private final BluetoothCom blueCom = new BluetoothComImpl();

    //    private final Properties openrobertaProperties = null;
    //    private URI sensorloggingWebSocketURI = null;
    //    private URL sensorloggingRestURL = null;
    //    private final Thread serverLoggerThread = null;
    private Thread screenLoggerThread = null;
    private final Configuration brickConfiguration;

    //    private final String token = "";
    //    private final String serverAddress = "";
    //    private final boolean wifiLogging = false;
    //    private ConnectionType connectionType = ConnectionType.NONE;

    private String language = "de";

    /**
     * Setup the hardware components of the robot, which are used by the NEPO program.
     *
     * @param brickConfiguration
     * @param usedSensors
     */
    public Hal(Configuration brickConfiguration, Set<UsedSensor> usedSensors) {
        this.usedSensors = usedSensors;
        this.deviceHandler = new DeviceHandler(brickConfiguration, usedSensors);
        this.brickConfiguration = brickConfiguration;
        this.wheelDiameter = brickConfiguration.getWheelDiameterCM();
        this.trackWidth = brickConfiguration.getTrackWidthCM();

        this.brick = LocalEV3.get();
        this.brick.setDefault();

        for ( int i = 0; i < this.timers.length; i++ ) {
            this.timers[i] = new Stopwatch();
        }

        try {
            RegulatedMotor leftRegulatedMotor = this.deviceHandler.getRegulatedMotor((ActorPort) brickConfiguration.getLeftMotorPort());
            RegulatedMotor rightRegulatedMotor = this.deviceHandler.getRegulatedMotor((ActorPort) brickConfiguration.getRightMotorPort());
            boolean isLeftActorInverse =
                this.brickConfiguration.getActorOnPort(brickConfiguration.getLeftMotorPort()).getRotationDirection() == DriveDirection.BACKWARD;
            boolean isRightActorInverse =
                this.brickConfiguration.getActorOnPort(brickConfiguration.getRightMotorPort()).getRotationDirection() == DriveDirection.BACKWARD;
            Wheel leftWheel = WheeledChassis.modelWheel(leftRegulatedMotor, this.wheelDiameter).offset(this.trackWidth / 2.).invert(isLeftActorInverse);
            Wheel rightWeel = WheeledChassis.modelWheel(rightRegulatedMotor, this.wheelDiameter).offset(-this.trackWidth / 2.).invert(isRightActorInverse);
            this.chassis =
                new WheeledChassis(
                    new Wheel[] {
                        leftWheel,
                        rightWeel
                    },
                    WheeledChassis.TYPE_DIFFERENTIAL);
            this.mPilot = new MovePilot(this.chassis);

        } catch ( DbcException e ) {
            // do not instantiate because we do not need it (checked form code generation side)
        }

        //        try {
        //            this.openrobertaProperties = loadOpenRobertaProperties();
        //            this.serverAddress = this.openrobertaProperties.getProperty("lastaddress");
        //            this.token = this.openrobertaProperties.getProperty("lasttoken");
        //            this.wifiLogging = this.openrobertaProperties.getProperty("connection").equals("wifi") ? true : false;
        //            this.sensorloggingWebSocketURI = new URI("ws://" + this.serverAddress + "/ws/");
        //            this.sensorloggingRestURL = new URL("http://" + this.serverAddress + "/rest/sensorlogging");
        //        } catch ( Exception e ) {
        //            this.wifiLogging = false;
        //        }
    }

    /**
     * Load properties from /home/roberta/openroberta.properties .
     * These are shared informations between the menu (read and write) and the NEPO program (only read).
     *
     * @return All stored key value pairs from the file
     * @throws IOException Should never occur if the menu is working correctly.
     */
    //    private Properties loadOpenRobertaProperties() throws IOException {
    //        File f = new File(OPENROBERTAPROPERTIESFILE);
    //        Properties p = new Properties();
    //        p.load(new FileInputStream(f));
    //        return p;
    //    }

    //    private HttpURLConnection openConnection() throws SocketTimeoutException, IOException {
    //        HttpURLConnection httpURLConnection = (HttpURLConnection) this.sensorloggingRestURL.openConnection();
    //        httpURLConnection.setDoInput(true);
    //        httpURLConnection.setDoOutput(true);
    //        httpURLConnection.setRequestMethod("POST");
    //        httpURLConnection.setReadTimeout(5000);
    //        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");
    //        return httpURLConnection;
    //    }

    /**
     * Starts logging of the sensor data to the server and brick screen.
     */
    public void startLogging() {
        //        startServerLoggingThread();
        startScreenLoggingThread();
    }

    /**
     * <b>!!!Temporary solution for version 1.4!!!</b></br>
     * Ugly fix, we can not upload websocket library to the ev3 in version 1.4, therefor use special method without websocket reference.
     * Logging is only allowed, if the last connection is wifi set by the ev3 menu.
     */
    //    private void startServerLoggingThread() {
    //        if ( this.wifiLogging ) {
    //            File f = new File("/home/roberta/lib/Java-WebSocket.jar");
    //            if ( f.exists() ) {
    //                loggingWithWebSocket();
    //            } else {
    //                loggingWithoutWebSocket();
    //            }
    //        }
    //    }

    /**
     * <b>!!!Temporary solution for version 1.4!!!</b></br>
     * Start a new thread for logging with websocket reference.
     */
    //    private void loggingWithWebSocket() {
    //        final ClientWebSocket ws = new ClientWebSocket(this.sensorloggingWebSocketURI);
    //        ws.connect();
    //        this.connectionType = ConnectionType.WEBSOCKET;
    //
    //        this.serverLoggerThread = new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                while ( true ) {
    //                    switch ( Hal.this.connectionType ) {
    //                        case NONE:
    //                            return;
    //                        case WEBSOCKET:
    //                            try {
    //                                logToServerWS(ws);
    //                            } catch ( Exception programFinished ) {
    //                                return;
    //                            }
    //                            break;
    //                        case REST:
    //                            try {
    //                                logToServerRest();
    //                            } catch ( Exception programFinished ) {
    //                                return;
    //                            }
    //                            break;
    //                        default:
    //                            return;
    //                    }
    //                    Delay.msDelay(2000);
    //                }
    //            }
    //        });
    //        this.serverLoggerThread.setDaemon(true);
    //        this.serverLoggerThread.start();
    //    }

    /**
     * <b>!!!Temporary solution for version 1.4!!!</b></br>
     * Start a new thread for logging without websocket reference.
     */
    //    private void loggingWithoutWebSocket() {
    //        this.connectionType = ConnectionType.REST;
    //        this.serverLoggerThread = new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                while ( true ) {
    //                    switch ( Hal.this.connectionType ) {
    //                        case NONE:
    //                            return;
    //                        case REST:
    //                            try {
    //                                logToServerRest();
    //                            } catch ( Exception programFinished ) {
    //                                return;
    //                            }
    //                            break;
    //                        default:
    //                            return;
    //                    }
    //                    Delay.msDelay(2000);
    //                }
    //            }
    //        });
    //        this.serverLoggerThread.setDaemon(true);
    //        this.serverLoggerThread.start();
    //    }

    /**
     * Start a new Thread in the NEPO program to regularly display sensor information on the ev3 screen.
     */
    private void startScreenLoggingThread() {
        this.screenLoggerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                logToScreen();
            }
        });
        this.screenLoggerThread.setDaemon(true);
        this.screenLoggerThread.start();
    }

    /**
     * Send sensor values to Open Roberta Lab via websocket.
     * Fall back to REST if the websocket is not able to connect to the server at least once.
     */
    //    private void logToServerWS(ClientWebSocket ws) {
    //        // READYSTATE.CONNECTING not working/ unused
    //        System.out.println(ws.getReadyState());
    //        if ( ws.getReadyState() == READYSTATE.OPEN ) {
    //            sendJSONviaWebsocket(ws);
    //        } else if ( ws.getReadyState() == READYSTATE.CLOSED || ws.getReadyState() == READYSTATE.CLOSING ) {
    //            ws.close();
    //            this.connectionType = ConnectionType.REST;
    //            return;
    //        }
    //    }

    /**
     * Send sensor values to Open Roberta Lab via websocket.
     * Give up the logging if no connection is possible.
     */
    //    public void logToServerRest() {
    //        try {
    //            HttpURLConnection httpURLConnection = openConnection();
    //            sendJSONviaRest(httpURLConnection);
    //            System.out.println(httpURLConnection.getResponseCode());
    //            httpURLConnection.disconnect();
    //            this.connectionType = ConnectionType.REST;
    //        } catch ( Exception e ) {
    //            e.printStackTrace();
    //            this.wifiLogging = false;
    //            this.connectionType = ConnectionType.NONE;
    //        }
    //    }

    /**
     * Write sensor values as JSON object to the websocket.
     */
    //    private void sendJSONviaWebsocket(ClientWebSocket ws) {
    //        JSONObject ev3Values = new JSONObject();
    //        ev3Values.put("token", this.token);
    //        addSensorsValues(ev3Values);
    //        addActorsTacho(ev3Values);
    //        ws.send(ev3Values.toString());
    //    }

    /**
     * Write sensor values as JSON object to the websocket.
     *
     * @throws IOException
     */
    //    private void sendJSONviaRest(HttpURLConnection httpURLConnection) throws IOException {
    //        JSONObject ev3Values = new JSONObject();
    //        ev3Values.put("token", this.token);
    //        addSensorsValues(ev3Values);
    //        addActorsTacho(ev3Values);
    //        OutputStream os = httpURLConnection.getOutputStream();
    //        os.write(ev3Values.toString().getBytes("UTF-8"));
    //        os.close();
    //    }

    /**
     * Put motor tacho information into the JSON object which will be send to the server.
     *
     * @param ev3Values
     */
    //    private void addActorsTacho(JSONObject ev3Values) {
    //        for ( Entry<IActorPort, Actor> mapEntry : this.brickConfiguration.getActors().entrySet() ) {
    //            int hardwareId = mapEntry.getValue().hashCode();
    //            ActorPort port = (ActorPort) mapEntry.getKey();
    //            String partKey = port.name() + "-";
    //
    //            partKey += "LARGE_MOTOR".hashCode() == hardwareId ? "LARGE_MOTOR" : "MEDIUM_MOTOR";
    //
    //            if ( this.brickConfiguration.isMotorRegulated(port) ) {
    //                ev3Values.put(partKey + MotorTachoMode.DEGREE, getRegulatedMotorTachoValue(port, MotorTachoMode.DEGREE));
    //            } else {
    //                ev3Values.put(partKey + MotorTachoMode.DEGREE, getUnregulatedMotorTachoValue(port, MotorTachoMode.DEGREE));
    //            }
    //        }
    //    }

    /**
     * Put sensor information into the JSON object which will be send to the server.
     *
     * @param ev3Values
     */
    //    private void addSensorsValues(JSONObject ev3Values) {
    //        for ( UsedSensor sensor : this.usedSensors ) {
    //            SensorPort port = (SensorPort) sensor.getPort();
    //            IMode mode = sensor.getMode();
    //
    //            String methodName = getHalMethodName(mode, sensor.getSensorType());
    //
    //            Method method;
    //            String result = null;
    //            try {
    //                method = Hal.class.getMethod(methodName, SensorPort.class);
    //                result = String.valueOf(method.invoke(this, port));
    //            } catch ( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
    //                break;
    //            }
    //            ev3Values.put(port + "-" + mode.getClass().getSimpleName() + "-" + mode, result);
    //        }
    //    }

    private String getHalMethodName(IMode mode, SensorType sensorType) {
        switch ( mode.toString() ) {
            case "COLOUR":
                return "getColorSensorColour";
            case "RED":
                return "getColorSensorRed";
            case "RGB":
                return "getColorSensorRgb";
            case "AMBIENTLIGHT":
                return "getColorSensorAmbient";
            case "RATE":
                return "getGyroSensorRate";
            case "ANGLE":
                if ( sensorType == SensorType.GYRO ) {
                    return "getGyroSensorAngle";
                }
                return "getHiTecCompassAngle";
            case "RESET":
                return "resetGyroSensor";
            case "TOUCH":
                return "isPressed";
            case "DISTANCE":
                if ( sensorType == SensorType.ULTRASONIC ) {
                    return "getUltraSonicSensorDistance";
                }
                return "getInfraredSensorDistance";
            case "PRESENCE":
                return "getUltraSonicSensorPresence";
            case "SEEK":
                return "getInfraredSensorSeek";
            case "SOUND":
                return "getSoundLevel";
            case "COMPASS":
                return "getHiTecCompassCompass";
            case "CALIBRATE":
                return "hiTecCompassStartCalibration";
            case "MODULATED":
                return "getHiTecIRSeekerModulated";
            case "UNMODULATED":
                return "getHiTecIRSeekerUnmodulated";
            default:
                return null;
        }
    }

    /**
     * Display the port and the sensor values on the ev3 screen every two seconds.
     */
    public void logToScreen() {
        while ( !Thread.currentThread().isInterrupted() ) {
            for ( UsedSensor sensor : this.usedSensors ) {
                SensorPort port = (SensorPort) sensor.getPort();
                IMode mode = sensor.getMode();
                String methodName = getHalMethodName(mode, sensor.getSensorType());
                Method method;
                String result = "";
                try {
                    method = Hal.class.getMethod(methodName, SensorPort.class);
                    result = String.valueOf(method.invoke(this, port));
                } catch ( NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
                    break;
                }
                formatAndPrintToScreen(port, result);
            }
            for ( Entry<IActorPort, Actor> mapEntry : this.brickConfiguration.getActors().entrySet() ) {
                ActorPort port = (ActorPort) mapEntry.getKey();
                String line = "";
                try {
                    if ( this.brickConfiguration.isMotorRegulated(port) ) {
                        line = port.name() + " " + getRegulatedMotorTachoValue(port, MotorTachoMode.DEGREE) + "\n";
                        this.lcdos.write(line.getBytes());
                    } else {
                        line = port.name() + " " + getUnregulatedMotorTachoValue(port, MotorTachoMode.DEGREE) + "\n";
                        this.lcdos.write(line.getBytes());
                    }
                } catch ( IOException e ) {
                    // ok
                }
            }
            Delay.msDelay(2000);
        }
    }

    /**
     * TODO not nice, find better solution
     *
     * @param port
     * @param result
     */
    private void formatAndPrintToScreen(SensorPort port, String result) {
        try {
            if ( result.startsWith("[") ) {
                String tmp = result.substring(1, result.length() - 1);
                List<String> list = new ArrayList<>(Arrays.asList(tmp.split(", ")));
                this.lcdos.write((port + " ").getBytes());
                for ( String string : list ) {
                    this.lcdos.write((string + " ").getBytes());
                }
                this.lcdos.write("\n".getBytes());
            } else {
                this.lcdos.write((port + " " + result + "\n").getBytes());
            }
        } catch ( IOException e ) {
            return;
        }
    }

    /**
     * Properly close all resources at the end of the NEPO program: ev3 io ports, debugging threads, websocket and the lcd output stream.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void closeResources() throws InterruptedException, IOException {
        EV3IOPort.closeAll();
        this.lcdos.close();
        System.exit(0);
    }

    //    /**
    //     * Send a message to the server. This is similar to a system.out.println() command, so the user can see if his program reaches a specific point during
    //     * runtime.
    //     *
    //     * @param msg Message to send to the server.
    //     */
    //    public void infoMessage(String msg) {
    //        if ( this.ws != null ) {
    //            try {
    //                if ( this.ws.getReadyState() == READYSTATE.OPEN ) {
    //                    JSONObject debugMsg = new JSONObject();
    //                    debugMsg.put("token", this.token);
    //                    debugMsg.put("msg", msg);
    //                    this.ws.send(debugMsg.toString());
    //                } else if ( this.ws.getReadyState() == READYSTATE.NOT_YET_CONNECTED || this.ws.getReadyState() == READYSTATE.CLOSED ) {
    //                    this.ws.connect();
    //                }
    //            } catch ( JSONException e ) {
    //                // ok
    //            }
    //        }
    //    }

    public static void displayExceptionWaitForKeyPress(Exception e) {
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

    /**
     * Print formatted message on the robot display.
     *
     * @param message
     * @param lcd
     */
    public static void formatInfoMessage(String message, TextLCD lcd) {
        String messageToBePrinted = message;
        int displayRow = 3;
        while ( messageToBePrinted.length() != 0 && displayRow <= 5 ) {
            if ( messageToBePrinted.length() <= NUMBER_OF_CHARACTERS_IN_ROW ) {
                lcd.drawString(messageToBePrinted, 0, displayRow);
                return;
            } else {
                lcd.drawString(messageToBePrinted.substring(0, NUMBER_OF_CHARACTERS_IN_ROW), 0, displayRow);
            }
            displayRow++;
            messageToBePrinted = messageToBePrinted.substring(NUMBER_OF_CHARACTERS_IN_ROW);
        }
    }

    // --- Aktion Bewegung ---

    /**
     * Turn on regulated motor. <br>
     * <br>
     * Client must give correct motor port and percent of the power of the motor is used.
     * 0 is the motor is used with power 0 i.e. it is not working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.
     *
     * @param actorPort on which motor is connected
     * @param speedPercent of motor power
     */
    public void turnOnRegulatedMotor(ActorPort actorPort, float speedPercent) {
        setRegulatedMotorSpeed(actorPort, speedPercent);
        if ( speedPercent < 0 ) {
            this.deviceHandler.getRegulatedMotor(actorPort).backward();
        } else {
            this.deviceHandler.getRegulatedMotor(actorPort).forward();
        }
    }

    /**
     * Turn on unregulated motor. <br>
     * <br>
     * Client must give correct motor port and percent of the power of the motor is used.
     * 0 is the motor is used with power 0 i.e. it is not working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.
     *
     * @param actorPort on which motor is connected
     * @param speedPercent of motor power
     */
    public void turnOnUnregulatedMotor(ActorPort actorPort, float speedPercent) {
        setUnregulatedMotorSpeed(actorPort, speedPercent);
        this.deviceHandler.getUnregulatedMotor(actorPort).forward();
    }

    /**
     * Set the speed on regulated motor.<br>
     * <br>
     * Client must give correct motor port and percent of the power of the motor is used.
     * 0 is the motor is used with power 0 i.e. it is not working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.<br>
     * <br>
     * <b>Setting the motor seed is only possible when the motor is not running.</b>
     *
     * @param actorPort on which motor is connected
     * @param speedPercent of motor power
     */
    public void setRegulatedMotorSpeed(ActorPort actorPort, float speedPercent) {
        this.deviceHandler.getRegulatedMotor(actorPort).setSpeed(toDegPerSec(speedPercent));
    }

    /**
     * Set the speed on unregulated motor.<br>
     * <br>
     * Client must give correct motor port and percent of the power of the motor is used.
     * 0 is the motor is used with power 0 i.e. it is not working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.<br>
     * <br>
     * <b>Setting the motor seed is only possible when the motor is not running.</b>
     *
     * @param actorPort on which motor is connected
     * @param speedPercent of motor power
     */
    public void setUnregulatedMotorSpeed(ActorPort actorPort, float speedPercent) {
        speedPercent = speedPercent < -100 ? -100 : speedPercent;
        speedPercent = speedPercent > 100 ? 100 : speedPercent;
        this.deviceHandler.getUnregulatedMotor(actorPort).setPower((int) speedPercent);
    }

    /**
     * Turn on regulated motor in a given move mode.<br>
     * <br>
     * Client must give correct motor port and percent of the power of the motor is used.
     * 0 is the motor is used with power 0 i.e. it is not working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum. Client also must provide the mode of termination of the motor work
     * (see {@link MotorMoveMode} for all possible modes for the motor when to stop).
     *
     * @param actorPort on which the motor is connected
     * @param speedPercent of motor power
     * @param mode until the motor will work
     * @param value until the motor will work
     */
    public void rotateRegulatedMotor(ActorPort actorPort, float speedPercent, MotorMoveMode mode, float value) {
        this.deviceHandler.getRegulatedMotor(actorPort).setSpeed(toDegPerSec(speedPercent));
        value = value < 0 ? 0 : value;
        if ( speedPercent < 0 ) {
            value *= -1;
        }
        switch ( mode ) {
            case DEGREE:
                this.deviceHandler.getRegulatedMotor(actorPort).rotate((int) value);
                break;
            case ROTATIONS:
                this.deviceHandler.getRegulatedMotor(actorPort).rotate(rotationsToAngle(value));
                break;
            default:
                throw new DbcException("incorrect MotorMoveMode");
        }
    }

    /**
     * Turn on unregulated motor in a given move mode.<br>
     * <br>
     * Client must give correct motor port and percent of the power of the motor is used.
     * 0 is the motor is used with power 0 i.e. it is not working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum. Client also must provide the mode of termination of the motor work
     * (see {@link MotorMoveMode} for all possible modes for the motor when to stop).
     *
     * @param actorPort on which the motor is connected
     * @param speedPercent of motor power
     * @param mode until the motor will work
     * @param value until the motor will work
     */
    public void rotateUnregulatedMotor(ActorPort actorPort, float speedPercent, MotorMoveMode mode, float value) {
        value = value < 0 ? 0 : value;
        int zeroTachoCount = this.deviceHandler.getUnregulatedMotor(actorPort).getTachoCount();
        setUnregulatedMotorSpeed(actorPort, speedPercent);
        this.deviceHandler.getUnregulatedMotor(actorPort).forward();
        if ( speedPercent >= 0 ) {
            switch ( mode ) {
                case DEGREE:
                    value += zeroTachoCount;
                    break;
                case ROTATIONS:
                    value = zeroTachoCount + rotationsToAngle(value);
                    break;
                default:
                    throw new DbcException("incorrect MotorMoveMode");
            }
            while ( this.deviceHandler.getUnregulatedMotor(actorPort).getTachoCount() < value ) {
                // do nothing
            }
        } else {
            switch ( mode ) {
                case DEGREE:
                    value = zeroTachoCount - value;
                    break;
                case ROTATIONS:
                    value = zeroTachoCount - rotationsToAngle(value);
                    break;
                default:
                    throw new DbcException("incorrect MotorMoveMode");
            }
            while ( this.deviceHandler.getUnregulatedMotor(actorPort).getTachoCount() > value ) {
                // do nothing
            }
        }
        this.deviceHandler.getUnregulatedMotor(actorPort).stop();
    }

    /**
     * Get the currently set regulated motor speed. <br>
     * <br>
     * Client must provide valid actor port on which the motor is connected.
     *
     * @param actorPort on which the motor is connected
     * @return current speed value
     */
    public float getRegulatedMotorSpeed(ActorPort actorPort) {
        return toPercent(this.deviceHandler.getRegulatedMotor(actorPort).getRotationSpeed());
    }

    /**
     * Get the currently set unregulated motor speed. <br>
     * <br>
     * Client must provide valid actor port on which the motor is connected.
     *
     * @param actorPort on which the motor is connected
     * @return current speed value
     */
    public float getUnregulatedMotorSpeed(ActorPort actorPort) {
        return this.deviceHandler.getUnregulatedMotor(actorPort).getPower();
    }

    /**
     * Stop regulated mode on given port.<br>
     * <br>
     * Client must provide valid actor port on which the motor is connected and how the motor will stop (see {@link MotorStopMode})
     *
     * @param actorPort on which the motor is connected
     * @param stopMode of the motor
     */
    public void stopRegulatedMotor(ActorPort actorPort, MotorStopMode stopMode) {
        switch ( stopMode ) {
            case FLOAT:
                this.deviceHandler.getRegulatedMotor(actorPort).flt(true);
                //this.deviceHandler.getRegulatedMotor(actorPort).stop(true);
                break;
            case NONFLOAT:
                this.deviceHandler.getRegulatedMotor(actorPort).stop(true);
                break;
            default:
                throw new DbcException("Wrong MotorStopMode");
        }
    }

    /**
     * Stops unregulated motor if it is running.<br>
     * <br>
     * Client must provide valid actor port on which the motor is connected and mode in which the motor will brake (see {@link MotorStopMode}).
     *
     * @param actorPort on which the motor is connected
     * @param floating mode of motor stopping
     */
    public void stopUnregulatedMotor(ActorPort actorPort, MotorStopMode floating) {
        switch ( floating ) {
            case FLOAT:
                this.deviceHandler.getUnregulatedMotor(actorPort).flt();
                //this.deviceHandler.getUnregulatedMotor(actorPort).stop();
                break;
            case NONFLOAT:
                this.deviceHandler.getUnregulatedMotor(actorPort).stop();
                break;
            default:
                throw new DbcException("Wrong MotorStopMode");
        }
    }

    // --- END Aktion Bewegung ---
    // --- Aktion Fahren ---

    /**
     * Turn on motors in regulated drive.<br>
     * <br>
     * Client must provide correct actor ports of the left and the right motor, is the motor set in reverse mode (forwards is backwards in the case), direction
     * of rotations of the motor (see {@link DriveDirection}) and percent of the power of the motor is used. 0 is the motor is used with power 0 i.e. it is not
     * working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.
     *
     * @param direction of rotation of the motor (forward or backward)
     * @param speedPercent of motor power
     */
    public void regulatedDrive(DriveDirection direction, float speedPercent) {
        int direct = direction == DriveDirection.FOREWARD ? 1 : -1;
        speedPercent = (float) (this.mPilot.getMaxLinearSpeed() * speedPercent / 100.0);
        this.chassis.setVelocity(direct * speedPercent, 0);
    }

    /**
     * Turn on motors in regulated drive for a given distance.<br>
     * <br>
     * Client must provide correct actor ports of the left and the right motor, is the motor set in reverse mode (forwards is backwards in the case), direction
     * of rotations of the motor (see {@link DriveDirection}) and percent of the power of the motor is used. 0 is the motor is used with power 0 i.e. it is not
     * working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.
     *
     * @param direction of rotation of the motor (forward or backward)
     * @param speedPercent of motor power
     * @param distance that the robot should travel
     */
    public void driveDistance(DriveDirection direction, float speedPercent, float distance) {
        int direct = 1;
        if ( speedPercent < 0 ) {
            direct = -1;
            speedPercent = direct * speedPercent;
        }
        this.mPilot.setLinearSpeed(this.mPilot.getMaxLinearSpeed() * speedPercent / 100.0);
        switch ( direction ) {
            case FOREWARD:
                this.mPilot.travel(direct * distance);
                break;
            case BACKWARD:
                this.mPilot.travel(direct * -distance);
                break;
            default:
                throw new DbcException("incorrect DriveAction");
        }
    }

    public void driveInCurve(DriveDirection direction, float speedLeft, float speedRight) {
        speedLeft = (float) (this.chassis.getMaxLinearSpeed() * speedLeft / 100.0);
        speedRight = (float) (this.chassis.getMaxLinearSpeed() * speedRight / 100.0);
        int direct = direction == DriveDirection.FOREWARD ? 1 : -1;
        double radius = calculateRadius(speedLeft, speedRight);
        double Lspeed = calculateSpeedDriveInCurve(speedLeft, speedRight);
        double Aspeed = Lspeed / radius * 180.0 / Math.PI;
        this.chassis.setVelocity(direct * Lspeed, direct * Aspeed);
    }

    public void driveInCurve(DriveDirection direction, float speedLeft, float speedRight, float distance) {
        speedLeft = (float) (this.mPilot.getMaxLinearSpeed() * speedLeft / 100.0);
        speedRight = (float) (this.mPilot.getMaxLinearSpeed() * speedRight / 100.0);
        int direct = direction == DriveDirection.FOREWARD ? 1 : -1;
        double radius = calculateRadius(speedLeft, speedRight);
        double robotSpeed = calculateSpeedDriveInCurve(speedLeft, speedRight);
        double Lspeed = Math.abs(robotSpeed);
        double Aspeed = Lspeed / Math.abs(radius) * 180.0 / Math.PI;
        double angle = direct * Math.signum(robotSpeed) * 360.0 / (2 * Math.PI * Math.abs(radius)) * distance;
        if ( speedLeft == speedRight ) {
            this.mPilot.setLinearSpeed(Lspeed);
            this.mPilot.travel(direct * Math.signum(robotSpeed) * distance);
        } else if ( robotSpeed == 0 ) {
            this.mPilot.setAngularSpeed(speedLeft);
            this.mPilot.rotate(direct * 10000, false);
        } else {
            this.mPilot.setAngularSpeed(Aspeed);
            this.mPilot.setLinearSpeed(Lspeed);
            this.mPilot.arc(radius, angle, false);
        }
    }

    private float calculateRadius(float speedLeft, float speedRight) {
        float radius = (float) (this.trackWidth * (speedLeft + speedRight) / (2.0f * (speedRight - speedLeft)));
        return radius;
    }

    private float calculateSpeedDriveInCurve(float speedLeft, float speedRight) {
        return (speedLeft + speedRight) / 2.0f;
    }

    /**
     * Stop regulated drive motors.<br>
     * <br>
     * Client must provide correct ports of the left and right motor.
     */
    public void stopRegulatedDrive() {
        this.mPilot.stop();
    }

    /**
     * Turn on motors in regulated drive and turn the robot left or right.<br>
     * <br>
     * Client must provide correct actor ports of the left and the right motor, is the motor set in reverse mode (forwards is backwards in the case), turning
     * direction of the robot (see {@link TurnDirection}) and percent of the power of the motor is used. 0 is the motor is used with power 0 i.e. it is not
     * working and 100 if we want to use full power of the motor.
     * Values larger then 100 set the motor speed again to its maximum.
     *
     * @param direction in which the robot will turn (left or right)
     * @param speedPercent of motor power
     */
    public void rotateDirectionRegulated(TurnDirection direction, float speedPercent) {
        float speed = (float) (this.chassis.getMaxAngularSpeed() * speedPercent / 100.0);
        int direct = direction == TurnDirection.LEFT ? 1 : -1;
        this.chassis.setVelocity(0, direct * speed);
    }

    /**
     * Turn on motors in regulated drive and turn the robot by give angle.<br>
     * <br>
     * Client must provide correct actor ports of the left and the right motor, is the motor set in reverse mode (forwards is backwards in the case), turning
     * direction of the robot (see {@link TurnDirection}) and percent of the power of the motor is used. 0 is the motor is used with power 0 i.e. it is not
     * working and 100 if we want to use full power of the motor. Values larger then 100 set the motor speed again to its maximum. Client must also provide the
     * angle of the turn of the robot.
     *
     * @param direction in which the robot will turn (left or right)
     * @param speedPercent of motor power
     * @param angle of the turn
     */
    public void rotateDirectionAngle(TurnDirection direction, float speedPercent, float angle) {
        this.mPilot.setAngularSpeed(this.mPilot.getMaxAngularSpeed() * speedPercent / 100.);
        switch ( direction ) {
            case RIGHT:
                angle = angle * -1;
                this.mPilot.rotate(angle, false);
                break;
            case LEFT:
                this.mPilot.rotate(angle, false);
                break;
            default:
                throw new DbcException("incorrect TurnAction");
        }
    }

    // --- END Aktion Fahren ---
    // --- Aktion Anzeige ---

    /**
     * Draw text on the display of the brick.<br>
     * <br>
     * Client must provide the string that should be displayed and the location of the screen (<b>x</b> and <b>y</b> coordinate)
     *
     * @param text to be displayed
     * @param x coordinate of the display
     * @param y coordinate of the display
     */
    public void drawText(String text, float x, float y) {
        this.brick.getTextLCD().drawString(text, (int) x, (int) y);
    }

    /**
     * Draw picture on the display of the brick.<br>
     * <br>
     * Client must provide the picture that should be displayed and the location of the screen (<b>x</b> and <b>y</b> coordinate).
     * See {@link ShowPicture} for all possible pictures that can be displayed.
     *
     * @param picture to be displayed
     * @param x coordinate of the display
     * @param y coordinate of the display
     */
    public void drawPicture(String picture, float x, float y) {
        Image image = new Image(178, 128, Utils.stringToBytes8(picture));
        this.brick.getGraphicsLCD().drawImage(image, (int) x, (int) y, 0);
    }

    /**
     * Clear the display.
     */
    public void clearDisplay() {
        this.brick.getGraphicsLCD().clear();
    }

    // --- END Aktion Anzeige ---
    // -- Aktion Klang ---

    /**
     * Play tone on the brick.<br>
     * <br>
     * Client must provide the frequency of the tone and the duration of the tone.
     *
     * @param frequency of the tone
     * @param duration of the tone (in <i>sec</i>)
     */
    public void playTone(float frequency, float duration) {
        this.brick.getAudio().playTone((int) frequency, (int) duration);
    }

    /**
     * Play stored sound file.<br>
     * <br>
     * Client must provide the number of the stored file.
     *
     * @param systemSound number of the sound
     */
    public void playFile(float systemSound) {
        this.brick.getAudio().systemSound((int) systemSound);
    }

    /**
     * Set the volume level of the brick.
     *
     * @param volume value between 1 and 100
     */
    public void setVolume(float volume) {
        this.brick.getAudio().setVolume((int) volume);
    }

    /**
     * Get the master volume level of the brick.
     *
     * @return master volume level value
     */
    public float getVolume() {
        return this.brick.getAudio().getVolume();
    }

    /**
     * Sets the language for the sayText function
     *
     * @param language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Say the text.
     * Generates a .wav file with eSpeak and plays it.
     *
     * @param text text to be said
     * @throws IOException
     * @throws InterruptedException
     */
    public void sayText(String text) {
        this.sayText(text, 30, 50); //Default values of espeak
    }

    /**
     * Say the text with additional parameters.
     * Generates a .wav file with eSpeak and plays it.
     *
     * @param text text to be said
     * @param speed words per minute
     * @param pitch pitch of the voice
     * @throws IOException
     * @throws InterruptedException
     */
    public void sayText(String text, float speed, float pitch) {
        // Clamp values
        speed = Math.max(0, Math.min(100, speed));
        pitch = Math.max(0, Math.min(100, pitch));
        // Convert to espeak values
        speed = Math.round(speed * 2.5f + 100); // use range 100 - 350
        pitch = Math.round(pitch * 0.99f); // use range 0 - 99
        Runtime rt = Runtime.getRuntime();
        String[] cmd =
            new String[] {
                "speak",
                "-w",
                "text.wav",
                "-a",
                Integer.toString(200),
                "-p",
                Float.toString(pitch),
                "-s",
                Float.toString(speed),
                "-v",
                this.language + "+f1", // female voice
                text
            };
        Process pr;
        try {
            pr = rt.exec(cmd);
            pr.waitFor();
            // Only play audio if no errors were thrown
            this.brick.getAudio().playSample(new File("text.wav"));
        } catch ( InterruptedException e ) {
            // Ignore
        } catch ( IOException e1 ) {
            // Ignore
        }
    }

    // -- END Aktion Klang ---
    // --- Aktion Statusleuchte ---

    /**
     * Turn on led lights on the brick.<br>
     * <br>
     *
     * @param color of the light
     * @param blinkMode of the light
     */
    public void ledOn(BrickLedColor color, BlinkMode blinkMode) {
        switch ( color ) {
            case GREEN:
                handleBlinkMode(blinkMode, 0);
                break;
            case RED:
                handleBlinkMode(blinkMode, 1);
                break;
            case ORANGE:
                handleBlinkMode(blinkMode, 2);
                break;
        }
    }

    private void handleBlinkMode(BlinkMode blinkMode, int colorNum) {
        switch ( blinkMode ) {
            case ON:
                this.brick.getLED().setPattern(1 + colorNum);
                break;
            case FLASH:
                this.brick.getLED().setPattern(4 + colorNum);
                break;
            case DOUBLE_FLASH:
                this.brick.getLED().setPattern(7 + colorNum);
                break;
            default:
                throw new DbcException("incorrect blink mode");
        }
    }

    /**
     * Turn of the lights on the brick
     */
    public void ledOff() {
        this.brick.getLED().setPattern(0);
    }

    /**
     * needed as soon as we decide to have a led pattern while running a roberta program<br>
     * change to this pattern then
     */
    public void resetLED() {
        this.brick.getLED().setPattern(0);
    }

    // --- END Aktion Statusleuchte ---
    // --- Sensoren Berührungssensor ---

    /**
     * Check if the touch sensor is pressed.
     *
     * @param sensorPort on which the touch sensor is connected
     * @return true if the sensor is pressed
     */
    public synchronized boolean isPressed(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, "Touch");
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        if ( sample[0] == 1.0 ) {
            return true;
        } else {
            return false;
        }
    }

    // --- END Sensoren Berührungssensor ---
    // --- Sensoren Ultraschallsensor ---

    /**
     * Get if there is presence of other ultrasonic sensor.<br>
     * <br>
     *
     * @param sensorPort on which the ultrasonic sensor is connected
     * @return true if exists other ultrasonic sensor
     */
    public synchronized boolean getUltraSonicSensorPresence(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, UltrasonicSensorMode.PRESENCE.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        if ( sample[0] == 1.0 ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get sample from ultrasonic sensor set in <b>distance mode</b>.
     *
     * @param sensorPort on which the ultrasonic sensor is connected
     * @return value in <i>cm</i> of the distance of the ultrasonic sensor and some object
     */
    public synchronized float getUltraSonicSensorDistance(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, UltrasonicSensorMode.DISTANCE.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0); // ^= distance in cm
        float distance = Math.round(sample[0] * 100.0f);
        if ( distance <= 3 ) {
            return 0;
        } else if ( distance > 255 ) {
            return 255;
        }
        return distance;
    }

    // END Sensoren Ultraschallsensor ---
    // --- Sensoren Farbsensor ---

    /**
     * Get sample from a color sensor set in <b>ambient light mode</b>
     *
     * @param sensorPort on which the color sensor is connected
     * @return the value of the measurement of the sensor
     */
    public synchronized float getColorSensorAmbient(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, ColorSensorMode.AMBIENTLIGHT.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return Math.round(sample[0] * 100.0f); // * 100
    }

    /**
     * Get sample from a color sensor in <b>colour mode</b>.
     *
     * @param sensorPort on which the sensor is connected
     * @return color that is detected with the sensor (see {@link Pickcolor} for all colors that can be detected)
     */
    public synchronized PickColor getColorSensorColour(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, ColorSensorMode.COLOUR.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return PickColor.get(Math.round(sample[0]));
    }

    /**
     * Get sample from a color sensor in <b>red mode</b>.
     *
     * @param sensorPort on which the sensor is connected
     * @return the value of the measurement of the sensor
     */
    public synchronized float getColorSensorRed(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, ColorSensorMode.RED.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return Math.round(sample[0] * 100.0f); // * 100
    }

    /**
     * Get sample from a color sensor in <b>RGB mode</b>.
     *
     * @param sensorPort on which the sensor is connected
     * @return array of size three where in each element of the array is encode on color channel (RGB), values are between 0 and 255
     */
    public synchronized ArrayList<Float> getColorSensorRgb(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, ColorSensorMode.RGB.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        ArrayList<Float> result = new ArrayList<>();
        result.add((float) Math.round(sample[0] * 255.0f));
        result.add((float) Math.round(sample[1] * 255.0f));
        result.add((float) Math.round(sample[2] * 255.0f));
        return result;
    }

    // END Sensoren Farbsensor ---
    // --- Sensoren IRSensor ---

    /**
     * Get sample from infrared sensor set in <b>distance mode</b>.
     *
     * @param sensorPort on which the infrared sensor is connected
     * @return value in <i>cm</i> of the distance of the infrared sensor and some object
     */
    public synchronized float getInfraredSensorDistance(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, InfraredSensorMode.DISTANCE.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);

        return Math.round(sample[0]);
    }

    /**
     * Get sample from infrared sensor set in <b>seek mode</b>.
     *
     * @param sensorPort on which the infrared sensor is connected
     * @return array of size 7
     */
    public synchronized ArrayList<Float> getInfraredSensorSeek(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, InfraredSensorMode.SEEK.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        ArrayList<Float> result = new ArrayList<>();
        result.add((float) Math.round(sample[0]));
        result.add((float) Math.round(sample[1]));
        result.add((float) Math.round(sample[2]));
        result.add((float) Math.round(sample[3]));
        result.add((float) Math.round(sample[4]));
        result.add((float) Math.round(sample[5]));
        result.add((float) Math.round(sample[6]));
        result.add((float) Math.round(sample[7]));
        return result;
    }

    // END Sensoren IRSensor ---
    // --- Sensoren IRSeekerSensor ---

    /**
     * Get sample from HiTechnic IRSeeker V2 sensor set in <b>modulated mode</b>.
     *
     * @param sensorPort on which the HiTechnic IRSeeker V2 sensor is connected
     * @return value in <i>°</i> of the direction of an modulated infrared signal
     */
    public synchronized float getHiTecIRSeekerModulated(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, IRSeekerSensorMode.MODULATED.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);

        return sample[0];
    }

    /**
     * Get sample from HiTechnic IRSeeker V2 sensor set in <b>unmodulated mode</b>.
     *
     * @param sensorPort on which the HiTechnic IRSeeker V2 sensor is connected
     * @return value in <i>°</i> of the direction of an unmodulated infrared signal
     */
    public synchronized float getHiTecIRSeekerUnmodulated(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, IRSeekerSensorMode.UNMODULATED.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);

        return sample[0];
    }

    // END Sensoren IRSeekerSensor ---
    // --- Sensor Gyrosensor ---

    /**
     * Get sample from gyro sensor.<br>
     * <br>
     * Client must provide sensor port on which the sensor is connected and the mode in which sensor would working (see {@link GyroSensorMode})
     *
     * @param sensorPort on which the gyro sensor sensor is connected
     * @return angle of the measurment of the sensor
     */
    public synchronized float getGyroSensorAngle(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, GyroSensorMode.ANGLE.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return Math.round(sample[0]);
    }

    /**
     * Get sample from gyro sensor.<br>
     * <br>
     * Client must provide sensor port on which the sensor is connected and the mode in which sensor would working (see {@link GyroSensorMode})
     *
     * @param sensorPort on which the gyro sensor sensor is connected
     * @return rate of the measurment of the sensor
     */
    public synchronized float getGyroSensorRate(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, GyroSensorMode.RATE.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return Math.round(sample[0]);
    }

    /**
     * Reset the gyro sensor
     *
     * @param sensorPort on which the gyro sensor is connected
     */
    public synchronized void resetGyroSensor(SensorPort sensorPort) {
        this.deviceHandler.getGyroSensor().reset();
    }

    // END Sensoren Gyrosensor ---
    // --- Sensoren CompassSensor ---

    /**
     * Starts calibration for the compass. Must rotate <b>very</b> slowly, taking at least 20 seconds per rotation. Should make 1.5 to 2 full rotations. Must
     * call
     * {@link hiTecCompassStopCalibration}() when done.
     *
     * @param sensorPort on which the compass sensor is connected
     */
    public synchronized void hiTecCompassStartCalibration(SensorPort sensorPort) {
        this.deviceHandler.getHiTechnicCompass().startCalibration();
    }

    /**
     * Stops calibration for the compass sensor.
     *
     * @param sensorPort on which the compass sensor is connected
     */
    public synchronized void hiTecCompassStopCalibration(SensorPort sensorPort) {
        this.deviceHandler.getHiTechnicCompass().stopCalibration();
    }

    /**
     * Get sample from compass sensor.<br>
     * <br>
     * Client must provide sensor port on which the sensor is connected and the mode in which sensor would working (see {@link GyroSensorMode})
     *
     * @param sensorPort on which the compass sensor sensor is connected
     * @return the angle output of the sensor
     */
    public synchronized float getHiTecCompassAngle(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, CompassSensorMode.ANGLE.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return sample[0];
    }

    /**
     * Get sample from compass sensor.<br>
     * <br>
     * Client must provide sensor port on which the sensor is connected and the mode in which sensor would working (see {@link GyroSensorMode})
     *
     * @param sensorPort on which the compass sensor sensor is connected
     * @return the compass output of the sensor
     */
    public synchronized float getHiTecCompassCompass(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, CompassSensorMode.COMPASS.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);
        return sample[0];
    }

    // END Sensoren CompassSensor ---
    // --- Sensoren Zeitgeber ---

    /**
     * Get time from timer
     *
     * @param timerNumber from which we want the measurement
     * @return time that have elepsed
     */
    public float getTimerValue(int timerNumber) {
        return this.timers[timerNumber - 1].elapsed();
    }

    /**
     * Reset the timer to 0
     *
     * @param timerNumber that we want to reset
     */
    public void resetTimer(int timerNumber) {
        this.timers[timerNumber - 1].reset();
    }

    // END Sensoren Zeitgeber ---
    // --- Aktorsensor Drehsensor ---

    /**
     * Reset regulated motor tacho sensor
     *
     * @param actorPort on which the motor is connected
     */
    public void resetRegulatedMotorTacho(ActorPort actorPort) {
        this.deviceHandler.getRegulatedMotor(actorPort).resetTachoCount();
    }

    /**
     * Reset unregulated motor tacho sensor
     *
     * @param actorPort on which the motor is connected
     */
    public void resetUnregulatedMotorTacho(ActorPort actorPort) {
        this.deviceHandler.getUnregulatedMotor(actorPort).resetTachoCount();
    }

    /**
     * Get value of the tacho sensor of the regulated motor.<br>
     * <br>
     * Client must provide the motor port of the motor on which we want to make measurement and mode of the tacho sensor (see {@link MotorTachoMode}).
     *
     * @param actorPort on which the motor is connected
     * @param mode in which the sensor is working
     * @return value of the measurement (number of rotations or angle of rotation)
     */
    public synchronized float getRegulatedMotorTachoValue(ActorPort actorPort, MotorTachoMode mode) {
        switch ( mode ) {
            case DEGREE:
                return this.deviceHandler.getRegulatedMotor(actorPort).getTachoCount();
            case ROTATION:
            case DISTANCE:
                float rotations = Math.round(this.deviceHandler.getRegulatedMotor(actorPort).getTachoCount() / 360.0 * 100.0) / 100.0f;
                if ( mode == MotorTachoMode.ROTATION ) {
                    return rotations;
                } else {
                    return Math.round(Math.PI * this.wheelDiameter * rotations);
                }
            default:
                throw new DbcException("incorrect MotorTachoMode");
        }
    }

    /**
     * Get value of the tacho sensor of the unregulated motor.<br>
     * <br>
     * Client must provide the motor port of the motor on which we want to make measurement and mode of the tacho sensor (see {@link MotorTachoMode}).
     *
     * @param actorPort on which the motor is connected
     * @param mode in which the sensor is working
     * @return value of the measurement (number of rotations or angle of rotation)
     */
    public synchronized float getUnregulatedMotorTachoValue(ActorPort actorPort, MotorTachoMode mode) {
        switch ( mode ) {
            case DEGREE:
                return this.deviceHandler.getUnregulatedMotor(actorPort).getTachoCount();
            case ROTATION:
            case DISTANCE:
                float rotations = Math.round(this.deviceHandler.getUnregulatedMotor(actorPort).getTachoCount() / 360.0 * 100.0) / 100.0f;
                if ( mode == MotorTachoMode.ROTATION ) {
                    return rotations;
                } else {
                    return Math.round(Math.PI * this.wheelDiameter * rotations);
                }
            default:
                throw new DbcException("incorrect MotorTachoMode");
        }
    }

    // END Aktorsensor Drehsensor ---
    // --- Sensoren Steintasten ---

    /**
     * Check if given button on the brick is pressed
     *
     * @param key that is checked
     * @return true if the button is pressed
     */
    public boolean isPressed(BrickKey key) {
        switch ( key ) {
            case ANY:
                if ( this.brick.getKeys().readButtons() != 0 ) {
                    return true;
                } else {
                    return false;
                }
            case DOWN:
                return this.brick.getKey("Down").isDown();
            case ENTER:
                return this.brick.getKey("Enter").isDown();
            case ESCAPE:
                return this.brick.getKey("Escape").isDown();
            case LEFT:
                return this.brick.getKey("Left").isDown();
            case RIGHT:
                return this.brick.getKey("Right").isDown();
            case UP:
                return this.brick.getKey("Up").isDown();
            default:
                throw new DbcException("wrong button name??");
        }
    }

    // END Sensoren Steintasten ---

    // --- Sensoren Sound ---

    /**
     * Get sample from sound sensor.
     *
     * @param sensorPort on which the sound sensor is connected
     * @return value in <i>dB</i> of the sound level measured
     */
    public synchronized float getSoundLevel(SensorPort sensorPort) {
        SampleProvider sampleProvider = this.deviceHandler.getProvider(sensorPort, SoundSensorMode.SOUND.getValues()[0]);
        float[] sample = new float[sampleProvider.sampleSize()];
        sampleProvider.fetchSample(sample, 0);

        return Math.round(sample[0] * 100);
    }

    // END Sensoren Sound ---

    /**
     * Sleep the running thread.
     *
     * @param time in milliseconds
     */
    public void waitFor(float time) {
        Delay.msDelay((long) time);
    }

    /**
     * Establishes a connection to host via {@link BluetoothCom#establishConnectionTo(String, int)} with a timeout of {@link #BLUETOOTH_TIMEOUT}.
     *
     * @param host the host.
     */
    public NXTConnection establishConnectionTo(String host) {
        return this.blueCom.establishConnectionTo(host, BLUETOOTH_TIMEOUT);
    }

    /**
     * Awaits an incoming connection via {@link BluetoothCom#waitForConnection(int)} with a timeout of {@link #BLUETOOTH_TIMEOUT}.
     */
    public NXTConnection waitForConnection() {
        return this.blueCom.waitForConnection(BLUETOOTH_TIMEOUT);
    }

    /**
     * Reads a message from an established connection.
     *
     * @param bluetoothConnection
     * @return the message or "NO MESSAGE"
     */
    public String readMessage(NXTConnection bluetoothConnection) {
        String message = "NO MESSAGE";
        if ( bluetoothConnection != null ) {
            message = this.blueCom.readMessage(bluetoothConnection);
        }
        return message;
    }

    /**
     * Sends a message over an established connection or returns.
     *
     * @param message the message to be sent
     * @param bluetoothConnection
     */
    public void sendMessage(String message, NXTConnection bluetoothConnection) {
        if ( bluetoothConnection != null ) {
            this.blueCom.sendTo(bluetoothConnection, message);
        }
    }

    private int toDegPerSec(float speedPercent) {
        return (int) (720.0 / 100.0 * speedPercent);
    }

    private int toPercent(float degPerSec) {
        return (int) (degPerSec * 100.0 / 720.0);
    }

    private int rotationsToAngle(float rotations) {
        return (int) (rotations * 360.0);
    }

}
