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


    
    private enum SoldierState {
        ATTACK,
        GUARD,
        MOVE,
        START,
        PATROL,
       
    }
    
    final double MAX_SOLDIER_DISTANCE = 5.0; 
    private SoldierState state, prevState;
    private MapLocation target = null;

    public Soldier(RobotController rc) {
        super(rc);
        this.state = SoldierState.START;
    }

    public void getMessage() {
        Message m = myRC.getNextMessage();
        if (m != null) {
            String strs[] = m.strings;
            int ints[] = m.ints;
            MapLocation locs[] = m.locations;
            if (strs[0].equals("ATTACK")) {
                this.state = SoldierState.ATTACK;
                target = locs[1];
                return;
            }

            if (strs[0].equals("Patrol") && locs[0].equals(myRC.getLocation())) {
                currentFluxDepositLoc = locs[1];
                archonNumber = ints[0];
                this.state = SoldierState.PATROL;
            }
        }
    }

    protected void attackFront() {
        Direction straight, left, right;
        straight = myRC.getDirection();
        left = straight.rotateLeft();
        right = straight.rotateRight();
        MapLocation aTarget;
        Robot r;
        aTarget = myRC.getLocation().add(straight);
        try {
            if (( r = myRC.senseAirRobotAtLocation(aTarget)) != null )  {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    myRC.attackAir(aTarget);
                }
            }
            if ((r = myRC.senseGroundRobotAtLocation(aTarget)) != null) {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    myRC.attackGround(aTarget);
                }
            }
            aTarget = myRC.getLocation().add(left);
            if (( r = myRC.senseAirRobotAtLocation(aTarget)) != null )  {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    myRC.attackAir(aTarget);
                }
            }
            if ((r = myRC.senseGroundRobotAtLocation(aTarget)) != null) {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    myRC.attackGround(aTarget);
                }
            }

            aTarget = myRC.getLocation().add(right);
            
            if (( r = myRC.senseAirRobotAtLocation(aTarget)) != null )  {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    myRC.attackAir(aTarget);
                }
            }
            if ((r = myRC.senseGroundRobotAtLocation(aTarget)) != null) {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    myRC.attackGround(aTarget);
                }
            }
        } catch (Exception e) {}
    }

    protected void refillAdjacent() {

    }

    public void run() {
        while (true) {
            getMessage();
           
           
            switch(state) {
                
                    
                case START:
                    
                    break;
                case ATTACK:

                    setDirection(myRC.getLocation().directionTo(target));
                    while (myRC.isAttackActive()) {
                        myRC.yield();
                    }
                    try {
                        myRC.attackAir(myRC.getLocation().add(myRC.getDirection()));
                    } catch (Exception e) {}
                   
                    attackNearby();
                    move(myRC.getLocation().directionTo(target));
                    

                    break;
                case PATROL:
                    
                    if ( senseEnemies()) {
                        sendMessage("Alarm", null, null);
                        travelNear(currentFluxDepositLoc);
                    } else {
                        if (myRC.getLocation().distanceSquaredTo(currentFluxDepositLoc) < MAX_SOLDIER_DISTANCE) {
                            travelNear(currentFluxDepositLoc);
                        } else {
                            travelTo(randomTarget(3, myRC.getLocation()));
                            
                        }
                    }
                    break;
                case GUARD:
                    
                    break;

            }
            myRC.yield();
        }
    }

}
