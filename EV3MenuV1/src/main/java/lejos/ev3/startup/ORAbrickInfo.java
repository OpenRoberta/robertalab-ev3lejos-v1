package lejos.ev3.startup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import lejos.hardware.Sounds;
import lejos.hardware.ev3.LocalEV3;
import lejos.utility.Delay;

public class ORAbrickInfo implements HttpHandler {

    private static boolean reg = false;

    private static final String ISRUNNING = "isrunning";

    private final GraphicStartup menu;

    public ORAbrickInfo(GraphicStartup menu) {
        this.menu = menu;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder builder = new StringBuilder();
        String str;
        while ( (str = br.readLine()) != null ) {
            builder.append(str);
        }
        JSONObject content = new JSONObject(builder.toString());
        JSONObject response = new JSONObject();

        switch ( content.getString(ORApushCmd.KEY_CMD) ) {
            case ISRUNNING:
                response.put(ISRUNNING, new Boolean(ORAlauncher.isRunning()).toString());
                break;
            case ORApushCmd.CMD_REGISTER:
                reg = true;
                response = getBrickInfos();
                break;
            case ORApushCmd.CMD_REPEAT:
                if ( reg ) {
                    this.menu.setUSBconnection(true);
                    reg = false;
                }
                response = getBrickInfos();
                break;
            case ORApushCmd.CMD_UPDATE:
                LocalEV3.get().getAudio().systemSound(Sounds.ASCENDING);
                response.put("restart", "now");
                exchange.sendResponseHeaders(200, response.toString().getBytes().length);
                exchange.getResponseBody().write(response.toString().getBytes());
                exchange.close();
                Delay.msDelay(1000);
                GraphicStartup.restartMenu();
                return;
            case ORApushCmd.CMD_ABORT:
                this.menu.setUSBconnection(false);
                response.put("abort", "disconnect");
                break;
            default:
                break;
        }
        exchange.sendResponseHeaders(200, response.toString().getBytes().length);
        exchange.getResponseBody().write(response.toString().getBytes());
        exchange.close();
    }

    private JSONObject getBrickInfos() {
        JSONObject response = new JSONObject();
        response.put(ORApushCmd.KEY_FIRMWARENAME, "lejos");
        response.put(ORApushCmd.KEY_FIRMWAREVERSION, GraphicStartup.getLejosVersion());
        response.put(ORApushCmd.KEY_MENUVERSION, GraphicStartup.getORAmenuVersion());
        response.put(ORApushCmd.KEY_RUNTIMEVERSION, GraphicStartup.getRuntimeVersion());
        response.put(ORApushCmd.KEY_BRICKNAME, GraphicStartup.getBrickName());
        response.put(ORApushCmd.KEY_BATTERY, GraphicStartup.getBatteryStatus());
        response.put(ORApushCmd.KEY_NEPOEXITVALUE, ORAlauncher.getNepoExitValue());
        response.put(ORApushCmd.KEY_MACADDR, "usb");
        return response;
    }
}
