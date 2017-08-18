package lejos.ev3.startup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import lejos.hardware.Sounds;
import lejos.hardware.ev3.LocalEV3;

/**
 * Download all required library files and menu for Open Roberta lab to the brick.<br>
 *
 * @author dpyka
 */
public class ORAupdater {

    private final String serverBaseIP;
    private final File libDir = new File("/home/roberta/lib");
    private final File menuDir = new File("/home/root/lejos/bin/utils");

    private boolean update_error = false;

    /**
     * Creates an object for updating the brick.
     *
     * @param serverBaseIP The server's base IP like 192.168.56.1:1999.
     */
    public ORAupdater(String serverBaseIP) {
        this.serverBaseIP = serverBaseIP;
    }

    /**
     * Download all files required for Open Roberta Lab.
     */
    public void update() {
        this.update_error = false;
        getRuntime();
        getJsonLib();
        getWebSocketLib();
        getEV3Menu();
        if ( this.update_error == false ) {
            LocalEV3.get().getAudio().systemSound(Sounds.ASCENDING);
            ORAhandler.setRestarted(false);
            ORAhandler.setRegistered(false);
            ORAhandler.setConnectionError(false);
        } else {
            LocalEV3.get().getAudio().systemSound(Sounds.BUZZ);
        }
    }

    /**
     * Download the generated jar from OpenRobertaRuntime.
     */
    private void getRuntime() {
        URL runtimeURL = null;
        try {
            runtimeURL = new URL("http://" + this.serverBaseIP + "/rest/update/runtime");
        } catch ( MalformedURLException e ) {
            // ok
        }
        downloadFile(runtimeURL, this.libDir);
    }

    /**
     * Download the generated jar from OpenRobertaShared.
     * The OpenRobertaShared and OpenRobertaRuntime projects are merged into one project,
     * and the **OpenRobertaShared.jar** is not needed any more.
     */
    @Deprecated
    private void getShared() {
        URL sharedURL = null;
        try {
            sharedURL = new URL("http://" + this.serverBaseIP + "/rest/update/shared");
        } catch ( MalformedURLException e ) {
            // ok
        }
        downloadFile(sharedURL, this.libDir);
    }

    /**
     * Download the JSON library for brick server communication.
     */
    private void getJsonLib() {
        URL jsonURL = null;
        try {
            jsonURL = new URL("http://" + this.serverBaseIP + "/rest/update/jsonlib");
        } catch ( MalformedURLException e ) {
            // ok
        }
        downloadFile(jsonURL, this.libDir);
    }

    /**
     * Download the Java-WebSocket library for brick server communication.
     */
    private void getWebSocketLib() {
        URL jsonURL = null;
        try {
            jsonURL = new URL("http://" + this.serverBaseIP + "/rest/update/websocketlib");
        } catch ( MalformedURLException e ) {
            // ok
        }
        downloadFile(jsonURL, this.libDir);
    }

    /**
     * Download the EV3Menu. Restart is needed to launch the "new" one.
     */
    private void getEV3Menu() {
        URL menuURL = null;
        try {
            menuURL = new URL("http://" + this.serverBaseIP + "/rest/update/ev3menu");
        } catch ( MalformedURLException e ) {
            // ok
        }
        downloadFile(menuURL, this.menuDir);
    }

    /**
     * Opens http connection to server. "POST" as request method. Input, output
     * set to "true".
     * no readTimeOut, connection will be hold forever or until data was send or until force disconnect
     *
     * @param url
     *        the robertalab server url or ip+port
     * @return httpURLConnection http connection object to the server
     * @throws IOException
     *         opening a connection failed
     */
    private HttpURLConnection openConnection(URL serverURL) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) serverURL.openConnection();
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(false);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setReadTimeout(30000);
        return httpURLConnection;
    }

    /**
     * Download a file from a specific REST service.
     *
     * @param url
     * @param directory
     */
    private void downloadFile(URL url, File directory) {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            HttpURLConnection httpURLConnection = openConnection(url);
            is = httpURLConnection.getInputStream();
            byte[] buffer = new byte[4096];
            int n;

            String raw = httpURLConnection.getHeaderField("Content-Disposition");
            String fileName = "";
            if ( raw != null && raw.indexOf("=") != -1 ) {
                fileName = raw.substring(raw.indexOf("=") + 1);
                fos = new FileOutputStream(new File(directory, fileName));
                while ( (n = is.read(buffer)) != -1 ) {
                    fos.write(buffer, 0, n);
                }
            }
        } catch ( IOException e ) {
            System.out.println("Error while updating!");
            this.update_error = true;
        } finally {
            try {

                if ( is != null ) {
                    is.close();
                }
                if ( fos != null ) {
                    fos.close();
                }
            } catch ( IOException e ) {
                // ok
            }
        }
    }
}
