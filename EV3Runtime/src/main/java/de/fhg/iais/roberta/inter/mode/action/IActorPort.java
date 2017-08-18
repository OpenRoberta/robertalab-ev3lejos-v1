package de.fhg.iais.roberta.inter.mode.action;

import de.fhg.iais.roberta.inter.mode.general.IMode;

/**
 * The enumeration implementing this interface should contain all the actor ports available on the robot.
 *
 * @author kcvejoski
 */
public interface IActorPort extends IMode {

    /**
     * @return the name used in the Blockly XML representation.
     */
    public String getXmlName();
}
