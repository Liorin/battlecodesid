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
import java.util.Random;

public abstract class AbstractRobot {
    protected RobotController myRC;
    protected int  archonNumber = 1;
    protected FluxDeposit currentFluxDeposit;
    protected MapLocation currentFluxDepositLoc;
    protected Direction stairs = null;
    protected MapLocation currentTarget = null;
   

    
    protected Team myTeam, oppTeam;
    protected Random generator = new Random();

    public static double ENERGON_MIN_LEVEL = 0.1;

    protected Team oppositeTeam(Team t) {
        if (t == Team.A) {
            return Team.B;
        }
        return Team.A;
    }


    AbstractRobot(RobotController rc) {
        this.myRC = rc;
        myTeam = myRC.getTeam();
        oppTeam = oppositeTeam(myTeam);
    }

    protected boolean setDirection(Direction dir) {
        if (dir == Direction.OMNI || dir == Direction.NONE) {
            return false;
        }
        if (myRC.hasActionSet()) {
            myRC.yield();
        }
        try {
            myRC.setDirection(dir);
        } catch (Exception e) {return false; }
        return true;
    }
    
    protected double getEnergonLevel(RobotInfo inf) {
        return (inf.energonLevel/ inf.maxEnergon);
    }

    protected void attackNearby() {
        /*Attack nearby robot, if any*/
        /*Attack weakest flying robot first, if no flying enemies nearby, 
         attack weakest enemy on the ground */
        Robot robs[];
        RobotInfo rob = null;
        double min = 1.0;
        if (myRC.isAttackActive() || myRC.hasActionSet()) {
            return;
        }
        try {
            if ((robs = myRC.senseNearbyAirRobots()).length > 0) {
                for (Robot r: robs) {
                    RobotInfo rInfo = myRC.senseRobotInfo(r);
                    if (rInfo.team != myTeam) {
                        if (myRC.canAttackSquare(rInfo.location) && getEnergonLevel(rInfo) <= min) {
                            rob = rInfo;
                            min = getEnergonLevel(rInfo);
                            return;
                        }
                    }
                }
            }
            if (rob != null) {
                
                myRC.attackAir(rob.location);
                return;
            }

            if ( (rob == null) && (robs = myRC.senseNearbyGroundRobots()).length > 0) {
                for (Robot r: robs) {
                    RobotInfo rInfo = myRC.senseRobotInfo(r);
                    if (rInfo.team != myTeam) {
                        if (myRC.canAttackSquare(rInfo.location) && getEnergonLevel(rInfo) <= min) {
                            rob = rInfo;
                            min = getEnergonLevel(rInfo);
                            return;
                        }
                    }
                }
            }
            if (rob != null) {
                
                myRC.attackGround(rob.location);
                return;
            }
            
        } catch (Exception e) {}
    }

    protected int dirToInt(Direction d) {
        if (d == null) {
            return 0;
        }

        switch (d) {
            case EAST: return 1;
            case NORTH: return 2;
            case WEST: return 3;
            case SOUTH: return 4;
            case NORTH_EAST: return 8;
            case NORTH_WEST: return 7;
            case SOUTH_EAST: return 6;
            case SOUTH_WEST: return 5;
        }

        return 0;
    }

    protected Direction intToDir(int i) {
        switch (i) {
            case 5: return Direction.SOUTH_WEST;
            case 6: return Direction.SOUTH_EAST;
            case 7: return Direction.NORTH_WEST;
            case 8: return Direction.NORTH_EAST;
            case 1: return Direction.EAST;
            case 2: return Direction.NORTH;
            case 3: return Direction.WEST;
            case 4: return Direction.SOUTH;
            default: return Direction.NONE;

        }
        
    }

    protected MapLocation addToLoc(MapLocation loc, Direction dir, int count) {
        if (count <= 0) {
            return loc;
        }
        MapLocation tempLoc = loc;
        for (int j = 0; j < count; j++) {
            tempLoc = tempLoc.add(dir);
        }
        return tempLoc;
    }

    protected boolean isPassable(MapLocation loc) {
        try {
            TerrainTile t = myRC.senseTerrainTile(loc);
            if (t.getType() == TerrainTile.TerrainType.LAND ) {
                return true;
            }
        } catch (Exception e) {return false;}
        return false;
    }

    protected boolean isFreeAndPassable(MapLocation target) {
        try {
            if (isPassable(target) && (myRC.senseGroundRobotAtLocation(target) == null)) {
                return true;
            }
        } catch (Exception e) {}
        return false;

    }

    protected MapLocation targetNearby(MapLocation target) {
        /*robot needs to be in sensor's range of target to find suitable location*/
        if (isFreeAndPassable(target)) {
           return target;

        }
        for (Direction d : Direction.values()) {
            if (d != Direction.NONE && d != Direction.OMNI) {
                if (isFreeAndPassable(target.add(d))) {

                    return (target.add(d));
                }
            }
        }

        return null;
    }

    protected boolean move(Direction dir) {
        
        try {
            while (myRC.hasActionSet() || myRC.isMovementActive()) {
                myRC.yield();
            }
            if (myRC.getDirection() != dir) {
                myRC.setDirection(dir);
                myRC.yield();
            }
            myRC.moveForward();
            myRC.yield();
        } catch (Exception e) { return false;}

        return true;
    }

    protected void travelToLocation(MapLocation target) {
        /*Right now, it's just the basic pathfinding(AKA just go straight)*/
        /**/
        Direction dir;
        MapLocation current;
        int count;
        int blocked = 0;
        if (target == null)
            return;
        while (!myRC.getLocation().equals(target)) {
            current = myRC.getLocation();
            count = 0;
            dir = current.directionTo(target);
            try {
                if (myRC.hasActionSet()){
                    myRC.yield();
                }
                myRC.setDirection(dir);
                if (myRC.canSenseSquare(target)) {
                    /*we need to check if someone isn't occupyuing our target location*/
                    switch (myRC.getRobotType()) {
                        case ARCHON:
                            if (myRC.senseAirRobotAtLocation(target) != null) {
                                blocked++;
                            } else {
                                blocked = 0;
                            }
                            break;
                        case WORKER:
                        case SOLDIER:
                            if (myRC.senseGroundRobotAtLocation(target) != null) {
                                blocked++;
                            } else {
                                blocked = 0;
                            }
                            break;
                    }
                    if (blocked >= 3) {
                        return;
                    }
                } else {
                    /*we can't ssense our target*/
                    current = addToLoc(myRC.getLocation(), myRC.getLocation().directionTo(target), 4);
                    current = targetNearby(current);
                    travelToLocation(current);
                }
            } catch (Exception e) {}
            while ((!myRC.canMove(dir)) && (count < 8)) {
                dir = dir.rotateRight();
                count++;
            }
            if (!move(dir)) {
                return;
            }
            myRC.yield();
        }
    }

    protected void travelNearLocation(MapLocation target) {
        MapLocation tmp = targetNearby(target);
        if (tmp == null) {
            tmp = target;
        }
        travelToLocation(tmp);
    }

    protected boolean energonLow() {
        return ( (myRC.getEnergonLevel() / myRC.getMaxEnergonLevel()) < ENERGON_MIN_LEVEL);
    }

    protected boolean sendMessage(String type, MapLocation locs[], int ints[])
    {
        Message m = new Message();
        m.strings = new String[1];
        m.strings[0] = type;
        m.locations = locs;
        m.ints = ints;
        if (myRC.hasActionSet()) {
            myRC.yield();
        }
        try {
            myRC.broadcast(m);
        } catch (Exception e) {return false;}
        return true;
    }

  

    protected boolean pathExists(MapLocation from, MapLocation to) {
        if (!myRC.canSenseSquare(from) || !myRC.canSenseSquare(to)) {
            return false;
        }

        


        return true;
    }

    protected boolean senseEnemies() {
        Robot robots[] = myRC.senseNearbyAirRobots();
        for (Robot r: robots) {
            try {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    return true;
                }
            } catch (Exception e) {}
        }
        robots = myRC.senseNearbyGroundRobots();

        for (Robot r: robots) {
            try {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    return true;
                }
            } catch (Exception e) {}
        }

        return false;
    }

    protected MapLocation randomTarget(int length, MapLocation from){
        Direction d = intToDir(generator.nextInt(8) + 1);
        return addToLoc(from, d, length);
    }

    protected boolean avoidObstacle(MapLocation obstacleTarget, MapLocation firstObstacle){
        int obstacleSteps = 0;

        Direction obstDir;
        MapLocation lastObstacle = firstObstacle;
        while (!myRC.getLocation().equals(obstacleTarget)) {
            if (obstacleSteps > 20) {
                return false;
            }
            obstDir = myRC.getLocation().directionTo(lastObstacle);
            while (!myRC.canMove(obstDir)) {
                lastObstacle = myRC.getLocation().add(obstDir);
                obstDir = obstDir.rotateRight();
            }
            move(obstDir);

            obstacleSteps++;
        }
        return true;

    }

    protected boolean isNear(MapLocation target) {
        if (target == null) {
            return false;
        }
        return (myRC.getLocation().distanceSquaredTo(target) < 3.0);
    }

    protected boolean travelTo(MapLocation target) {
        Direction dir = myRC.getLocation().directionTo(target);
        
        long stepsNum = 50;
        

        while (!myRC.getLocation().equals(target) ) {
            if (stepsNum < 0) {
                return false;
            }
            if (!myRC.canMove(dir)) {
                        /*Przeszkoda na drodze*/
                        
                        /*avoidObstacle(addToLoc(myRC.getLocation(), dir, 2), myRC.getLocation().add(dir));*/
            } else { move(dir); }
            stepsNum--;
        }
        return true;
    }

    protected boolean travelNear(MapLocation target) {
        Direction dir = myRC.getLocation().directionTo(target);

        long stepsNum = Math.round(myRC.getLocation().distanceSquaredTo(target)*2.0);

        while (!isNear(target) ) {
            if (stepsNum < 0) {
                return false;
            }
            if (!move(dir)) {
                        /*Przeszkoda na drodze*/
                        avoidObstacle(addToLoc(myRC.getLocation(), dir, 2), myRC.getLocation().add(dir));
                    }
            stepsNum--;
        }
        return true;
    }

    protected abstract void getMessage();
    protected abstract void refillAdjacent();
    
    

   
    public abstract void run();
}
