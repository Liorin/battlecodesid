/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package team111;

/**
 *
 * @author liorin
 */
import battlecode.common.*;

public class Soldier extends AbstractRobot {
    RobotState state;
    MapLocation target = null;
    
    private enum RobotState {
        ATTACK,
        START
    }

    public Soldier(RobotController rc) {
        super(rc);
        this.state = RobotState.START;
    }
    public void getMessage() {
        Message m = myRC.getNextMessage();
        if (m != null) {
            String strs[] = m.strings;
            int ints[] = m.ints;
            MapLocation locs[] = m.locations;
            if (strs[0].equals("Enemy")) {
                this.state = RobotState.ATTACK;
                target = locs[0];
            }
        }
    }
    public void run() {
        while (true) {
            getMessage();
            attackNearby();
            switch(state) {
                case START:
                    
                    break;
                case ATTACK:
                    setDirection(myRC.getLocation().directionTo(target));
                    

                    break;
            }
            myRC.yield();
        }
    }

}
