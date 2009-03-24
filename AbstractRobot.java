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

public abstract class AbstractRobot {
    protected RobotController myRC;
    protected int archonNumber = 1;
    protected FluxDeposit currentFluxDeposit;
    protected MapLocation currentFluxDepositLoc;
    protected Direction stairs = null;
    protected MapLocation currentTarget = null;
    protected Team myTeam;

    public static double ENERGON_MIN_LEVEL = 0.2;


    AbstractRobot(RobotController rc) {
        this.myRC = rc;
        myTeam = myRC.getTeam();
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

    protected void attackNearby() {
        Robot robs[];
        if (myRC.isAttackActive() || myRC.hasActionSet()) {
            return;
        }
        try {
            if ((robs = myRC.senseNearbyAirRobots()).length > 0) {
                for (Robot r: robs) {
                    RobotInfo rInfo = myRC.senseRobotInfo(r);
                    if (rInfo.team != myTeam) {
                        if (myRC.canAttackSquare(rInfo.location)) {
                            myRC.attackAir(rInfo.location);
                            return;
                        }
                    }
                }
            }

            if ((robs = myRC.senseNearbyGroundRobots()).length > 0) {
                for (Robot r: robs) {
                    RobotInfo rInfo = myRC.senseRobotInfo(r);
                    if (rInfo.team != myTeam) {
                        if (myRC.canAttackSquare(rInfo.location)) {
                            myRC.attackGround(rInfo.location);
                            return;
                        }
                    }
                }
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
        }

        return 0;
    }

    protected Direction intToDir(int i) {
        switch (i) {
            case 1: return Direction.EAST;
            case 2: return Direction.NORTH;
            case 3: return Direction.WEST;
            case 4: return Direction.SOUTH;
        }
        return Direction.NONE;
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
            while (myRC.getCurrentAction() != ActionType.IDLE || myRC.isMovementActive()) {
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
                if (myRC.canSenseSquare(target)) {
                    /*we need to check if someone isn't occupyuing our tart=get location*/
                    switch (myRC.getRobotType()) {
                        case ARCHON:
                            if (myRC.senseAirRobotAtLocation(target) != null) {
                                blocked++;
                            } else {
                                blocked--;
                            }
                            break;
                        case WORKER:
                            if (myRC.senseGroundRobotAtLocation(target) != null) {
                                blocked++;
                            } else {
                                blocked--;
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

    public abstract void run();
}
