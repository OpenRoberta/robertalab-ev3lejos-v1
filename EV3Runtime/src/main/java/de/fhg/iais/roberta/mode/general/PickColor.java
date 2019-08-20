package de.fhg.iais.roberta.mode.general;

import de.fhg.iais.roberta.inter.mode.general.IPickColor;
import de.fhg.iais.roberta.util.dbc.DbcException;

/**
 * All colors that are legal.
 */
public enum PickColor implements IPickColor {

    RED( 0, "#B30006" ),
    GREEN( 1, "#00642E" ),
    BLUE( 2, "#0057A6" ),
    YELLOW( 3, "#F7D117" ),
    MAGENTA( 4, "#000001" ),
    ORANGE( 5, "#000002" ),
    WHITE( 6, "#FFFFFF" ),
    BLACK( 7, "#000000" ),
    PINK( 8, "#000003" ),
    GRAY( 9, "#000004" ),
    LIGHT_GRAY( 10, "#000005" ),
    DARK_GRAY( 11, "#000006" ),
    CYAN( 12, "#000007" ),
    BROWN( 13, "#532115" ),
    NONE( -1, "#585858" );

    private final String[] values;
    private final int colorID;

    private PickColor(int colorID, String... values) {
        this.values = values;
        this.colorID = colorID;
    }

    @Override
    public int getColorID() {
        return this.colorID;
    }

    @Override
    public String getHex() {
        return this.values[0];
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

    public static PickColor get(int id) {
        for ( PickColor sp : PickColor.values() ) {
            if ( sp.colorID == id ) {
                return sp;
            }
        }
        throw new DbcException("Invalid color: " + id);
    }

    /**
     * Return the nearest PickColor that can be returned by the Lego Color sensor
     * @param id
     * @return
     */
    public static PickColor getByHiTecColorId(int id) {
        switch ( id ) {
            case 0:
                return BLACK;
            case 1:
            case 2:
                return RED;
            case 3:
                return BLUE;
            case 4:
                return GREEN;
            case 5:
            case 6:
            case 7:
                return YELLOW;
            case 8:
            case 9:
            case 10:
            case 11:
                return RED;
            case 12:
            case 13:
                return YELLOW;
            case 14:
            case 15:
            case 16:
                return RED;
            case 17:
                return WHITE;
            default:
                return NONE;
        }
    }
}
