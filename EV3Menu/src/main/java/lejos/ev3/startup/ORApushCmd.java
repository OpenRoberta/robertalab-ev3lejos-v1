package lejos.ev3.startup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONObject;

import lejos.hardware.Sounds;
import lejos.hardware.ev3.LocalEV3;
import lejos.utility.Delay;

/**
 * Class for brick <-> server communication based on json and key words "cmds".
 *
 * @author dpyka
 */
public class ORApushCmd implements Runnable {

    private URL pushServiceURL;
    private URLConnection urlConnection;

    private final ORAdownloader oraDownloader;
    private final ORAupdater oraUpdater;

    private final boolean TRUE = true;

    private int reconnectAttempts = 0;
    private final int maxAttempts = 3;

    private int nepoExitValue = 0;

    private final JSONObject brickData = new JSONObject();

    // brick data keywords
    public static final String KEY_BRICKNAME = "brickname";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_MACADDR = "macaddr";
    public static final String KEY_BATTERY = "battery";
    public static final String KEY_FIRMWARENAME = "firmwarename";
    public static final String KEY_FIRMWAREVERSION = "firmwareversion";
    public static final String KEY_MENUVERSION = "menuversion";
    public static final String KEY_RUNTIMEVERSION = "runtimeversion";
    public static final String KEY_CMD = "cmd";
    public static final String KEY_NEPOEXITVALUE = "nepoexitvalue";

    // brickdata + cmds send to server
    public static final String CMD_REGISTER = "register";
    public static final String CMD_PUSH = "push";

    // cmds receive from server
    public static final String CMD_REPEAT = "repeat";
    public static final String CMD_ABORT = "abort";
    public static final String CMD_UPDATE = "update";
    public static final String CMD_DOWNLOAD = "download";
    public static final String CMD_CONFIGURATION = "configuration";
    private String serverBaseIP;

    /**
     * Creates a new Open Roberta Lab "push" communication object. Additional
     * objects for downloading user programs, updating the brick and launching a
     * program are being created.
     *
     * @param serverBaseIP
     *        The base IP like 192.168.56.1:1999
     * @param ind
     *        title bar of the brick's screen
     */
    public ORApushCmd(String serverBaseIP, String token) {
        // add brick data pairs which will not change during runtime
        this.brickData.put(KEY_TOKEN, token);
        this.brickData.put(KEY_MACADDR, GraphicStartup.getWlanMACaddress());
        this.brickData.put(KEY_MENUVERSION, GraphicStartup.getORAmenuVersion());
        this.brickData.put(KEY_RUNTIMEVERSION, GraphicStartup.getRuntimeVersion());
        this.brickData.put(KEY_FIRMWARENAME, "ev3lejosv1");
        this.brickData.put(KEY_FIRMWAREVERSION, GraphicStartup.getLejosVersion());
        this.serverBaseIP = serverBaseIP;
        try {
            this.pushServiceURL = new URL("https://" + this.serverBaseIP + "/rest/pushcmd");
        } catch ( MalformedURLException e ) {
            // ok
        }

        this.oraDownloader = new ORAdownloader(serverBaseIP);
        this.oraUpdater = new ORAupdater(serverBaseIP);
    }

    /**
     * Expose http connection to allow the user to cancel the registration
     * process. Otherwise user has to wait until timeout occurs (5minutes). Http
     * connection will "hang" in another thread trying to read from inputstream.
     *
     * @return The http connection the brick uses to communicate with the server.
     */
    public URLConnection getURLConnection() {
        return this.urlConnection;
    }

    /**
     * Method which processes the brick server communication in a separate thread.
     * The brick reacts on specific commands from the server.
     */
    @Override
    public void run() {
        OutputStream os = null;
        BufferedReader br = null;

        while ( this.TRUE ) {
            try {
                this.brickData.put(KEY_BRICKNAME, GraphicStartup.getBrickName());
                this.brickData.put(KEY_BATTERY, GraphicStartup.getBatteryStatus());

                if ( ORAhandler.isRegistered() ) {
                    this.brickData.put(KEY_CMD, CMD_PUSH);
                    this.urlConnection = openConnection(15000);
                } else {
                    this.brickData.put(KEY_CMD, CMD_REGISTER);
                    this.urlConnection = openConnection(330000);
                }

                os = this.urlConnection.getOutputStream();
                os.write(this.brickData.toString().getBytes("UTF-8"));

                br = new BufferedReader(new InputStreamReader(this.urlConnection.getInputStream()));
                StringBuilder responseStrBuilder = new StringBuilder();
                String responseString;
                while ( (responseString = br.readLine()) != null ) {
                    responseStrBuilder.append(responseString);
                }
                JSONObject responseEntity = new JSONObject(responseStrBuilder.toString());

                String command = responseEntity.getString("cmd");
                switch ( command ) {
                    case CMD_REPEAT:
                        ORAhandler.setRegistered(true);
                        ORAhandler.setConnectionError(false);
                        ORAhandler.setTimeout(false);
                        this.brickData.put(KEY_NEPOEXITVALUE, 0);
                        break;
                    case CMD_ABORT:
                        // if brick is waiting for registration, server sends abort as timeout message
                        if ( ORAhandler.isRegistered() == false ) {
                            ORAhandler.setTimeout(true);
                        } else { // this should never happen
                            ORAhandler.setRegistered(false);
                            LocalEV3.get().getAudio().systemSound(Sounds.DESCENDING);
                        }
                        return;
                    case CMD_UPDATE:
                        this.oraUpdater.update();
                        GraphicStartup.restartMenu();
                        return;
                    case CMD_DOWNLOAD:
                        if ( GraphicStartup.getUserprogram() == null ) {
                            String programName = this.oraDownloader.downloadProgram(this.brickData);
                            this.nepoExitValue = ORAlauncher.runProgram(programName);
                            this.brickData.put(KEY_NEPOEXITVALUE, this.nepoExitValue);
                        }
                        break;
                    case CMD_CONFIGURATION:
                        break;
                    default:
                        break;
                }
                this.reconnectAttempts = 0;
            } catch ( SocketTimeoutException ste ) {
                if ( ORAhandler.isRegistered() == false ) {
                    ORAhandler.setTimeout(true);
                    System.out.println("2" + ste.getMessage());
                    break;
                } else {
                    this.reconnectAttempts++;
                    System.out.println(this.reconnectAttempts + "(timeout)");
                }
            } catch ( IOException ioe ) {
                System.out.println("3" + ioe.getMessage());
                if ( ORAhandler.isRegistered() == false ) {

                    if ( this.pushServiceURL.getProtocol().equals("https") ) {
                        try {
                            this.pushServiceURL = new URL("http://" + this.serverBaseIP + "/rest/pushcmd");
                        } catch ( MalformedURLException e ) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        ORAhandler.setConnectionError(true);
                        return;
                    }
                } else {
                    if ( this.reconnectAttempts >= this.maxAttempts ) {
                        GraphicStartup.menu.drawConnectionLost();
                        return;
                    } else {
                        this.reconnectAttempts++;
                        Delay.msDelay(1000);
                    }
                    System.out.println(this.reconnectAttempts + "(ioex)");
                }
            } finally {
                try {
                    if ( os != null ) {
                        os.close();
                    }
                    if ( br != null ) {
                        br.close();
                    }
                } catch ( IOException e ) {
                    // ok
                }
            }
        }
    }

    /**
     * Opens an http connection to server. "POST" as request method. Input, output
     * set to "true". 5 minutes readtimeout set. This connection is used for the
     * "push" service. The server will answer the request every few seconds.
     *
     * @return HttpURLConnection http connection object
     * @throws IOException
     *         Connection to server failed
     */
    private URLConnection openConnection(int readTimeOut) throws SocketTimeoutException, IOException {
        URLConnection urlConnection = this.pushServiceURL.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        //        httpURLConnection.setRequestMethod("POST");
        urlConnection.setReadTimeout(readTimeOut);
        urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");
        return urlConnection;
    }
}
