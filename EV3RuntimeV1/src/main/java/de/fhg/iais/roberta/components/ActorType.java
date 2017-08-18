package de.fhg.iais.roberta.components;

import java.util.Arrays;
import java.util.Locale;

import de.fhg.iais.roberta.mode.action.DriveDirection;
import de.fhg.iais.roberta.util.dbc.DbcException;

public enum ActorType {
    MEDIUM( "robBrick_motor_middle" ), LARGE( "robBrick_motor_big" ), REGULATED();

    private final String[] values;

    private ActorType(String... values) {
        this.values = Arrays.copyOf(values, values.length);
        Arrays.sort(this.values);
    }

    public String blocklyName() {
        return this.values[0];
    }

    /**
     * Get direction from {@link DriveDirection} from string parameter. It is possible for one direction to have multiple string mappings.
     * Throws exception if the direction does not exists.
     *
     * @param name of the direction
     * @return name of the direction from the enum {@link DriveDirection}
     */
    public static ActorType get(String s) {
        if ( s == null || s.isEmpty() ) {
            throw new DbcException("Invalid Actor Type: " + s);
        }
        String sUpper = s.trim().toUpperCase(Locale.GERMAN);
        for ( ActorType sp : ActorType.values() ) {
            if ( sp.toString().equals(sUpper) ) {
                return sp;
            }
            for ( String value : sp.values ) {
                if ( s.trim().equals(value) ) {
                    return sp;
                }
            }
        }
        throw new DbcException("Invalid Actor type: " + s);
    }

}
