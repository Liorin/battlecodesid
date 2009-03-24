/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package team111;

import battlecode.common.*;
import battlecode.common.GameConstants;
import java.util.*;
/**
 *
 * @author liorin
 */


public class RobotPlayer implements Runnable {

    private AbstractRobot r;
    public RobotPlayer(RobotController myRC) {
        switch (myRC.getRobotType()) {
            case ARCHON:
                r = new Archon(myRC);
                break;
            case WORKER:
                r = new Worker(myRC);
                break;
            case SOLDIER:
                r = new Soldier(myRC);
                break;
        }
    }

    public void run() {
        r.run();
    }


}
