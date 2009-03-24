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

public class Worker extends AbstractRobot {
    private enum WorkerState {
        BLOCK_GATHER,
        FOLLOW_ARCHON,
        BLOCK_RETURN,
        FIND_ENERGON,
        START
    }
    private WorkerState state , prevState;

    private boolean hasBlocks = false;
    public Worker(RobotController rc) {
        super(rc);
        this.state = WorkerState.START;
    }
    
    protected boolean isForbiddenToLoad(MapLocation from) {
        if (from == null) {
            return true;
        }
        for (int i = 0 ; i < 6; i++) {
            if (addToLoc(currentFluxDepositLoc, stairs, i).equals(from) ){
                return true;

            }
        }


        return false;
    }

    public boolean canLoadBlock(MapLocation from, MapLocation target) {
        if (from == null || target == null) {
            return false;
        }

        if (from.equals(target)) {
            return false;
        }

        for (int i = 0 ; i < 6; i++) {
            if (addToLoc(currentFluxDepositLoc, stairs, i).equals(target) ){
                return false;

            }
        }

        if (!from.isAdjacentTo(target)) {
            return false;
        }
        
        /*from location must be passable for ground robots*/
        
        if (!myRC.canSenseSquare(from)) {
            return false;
        }
        if (!myRC.senseTerrainTile(from).isTraversableAtHeight(RobotLevel.ON_GROUND)) {
            return false;
        }
        
        try {
        if (Math.abs(myRC.senseHeightOfLocation(target) - myRC.senseHeightOfLocation(from)) >= GameConstants.WORKER_MAX_HEIGHT_DELTA) {
            return false;
        } }
        catch (Exception e) {return false;}

        return true;
    }

    public boolean canUnLoadBlock(MapLocation from, MapLocation target) {
        if (from == null || target == null) {
            return false;
        }

        if (from.equals(target)) {
            return false;
        }



        if (!from.isAdjacentTo(target)) {
            return false;
        }

        /*from location must be passable for ground robots*/

        if (!myRC.canSenseSquare(from)) {
            return false;
        }
        if (!myRC.senseTerrainTile(from).isTraversableAtHeight(RobotLevel.ON_GROUND)) {
            return false;
        }

        try {
        if (Math.abs(myRC.senseHeightOfLocation(target) - myRC.senseHeightOfLocation(from)) >= GameConstants.WORKER_MAX_HEIGHT_DELTA) {
            return false;
        } }
        catch (Exception e) {return false;}

        return true;
    }
    private MapLocation findLocationToUnload(MapLocation to) {
        for (Direction d : Direction.values()) {
            if (d != Direction.OMNI && d != Direction.NONE &&  
                     !to.add(d).equals(currentFluxDepositLoc)) {
                MapLocation loc, neigh;
                loc = to;
                neigh = loc.add(d);
                if (canUnLoadBlock(neigh, loc)) {
                    return neigh;
                }
            }
        }
        return null;                            
    }
    
    private MapLocation findLocationToLoad(MapLocation to) {
        return findLocationToUnload(to);
    }

    public void run() {
        Message m;
        MapLocation l = null;

        while (true)  {
            /*if (energonLow() && (this.state != RobotState.FIND_ENERGON)) {

                this.prevState = this.state;
                this.state = RobotState.FIND_ENERGON;
            }
            if ((this.state == RobotState.FIND_ENERGON) && (!energonLow())) {
                this.state = this.prevState;
            }*/
            m = myRC.getNextMessage();
            if (m != null) {
                if (m.strings[0].equals("W")) {
                    if (m.locations[0].equals(myRC.getLocation())) {
                        /*This message is for that robot*/
                        this.archonNumber = m.ints[0];
                        this.stairs = intToDir(m.ints[1]);
                        this.currentFluxDepositLoc = m.locations[1];
                        this.state = WorkerState.BLOCK_GATHER;

                    }
                }

                if (m.strings[0].equals("F") && (m.ints[0] == archonNumber)) {
                    this.state = WorkerState.FOLLOW_ARCHON;
                    currentTarget = m.locations[0];
                }


            }
            switch (state) {
                case START:
                    myRC.yield();
                    break;
                case BLOCK_GATHER:
                    MapLocation blocks[] = myRC.senseNearbyBlocks();
                    MapLocation temporaryTarget, loc = null;

                    for (int j = 0; j < blocks.length; j++) {

                        if (!isForbiddenToLoad(blocks[j])) {
                            travelToLocation(blocks[j]);
                            loc = blocks[j];
                            for (Direction d : Direction.values()) {
                                if (d != Direction.OMNI && d != Direction.NONE) {

                                    MapLocation neigh;
                                    neigh = loc.add(d);
                                    if (canLoadBlock(neigh, loc)) {

                                        travelToLocation(neigh);
                                        myRC.yield();
                                        try {
                                            while(myRC.isMovementActive()) {
                                                myRC.yield();
                                            }
                                            myRC.loadBlockFromLocation(loc);
                                            hasBlocks = true;
                                            break;
                                        } catch (Exception e) {}
                                    }
                                }
                            }
                        if (hasBlocks) {
                            this.state = WorkerState.BLOCK_RETURN;
                            break;
                        }
                            
                            break;
                        }
                    }

                    if (loc == null) {
                        /*TODO find some blocks*/
                        while (isForbiddenToLoad(myRC.getLocation())) {
                            move(stairs.rotateRight());
                        }
                        break;
                    }



                    /*at least one location with blocks nearby*/


                    break;
                case BLOCK_RETURN:
                  /*  travelToLocation(currentFluxDepositLoc);*/

                    travelToLocation(currentFluxDepositLoc);


                    MapLocation unloadTarget = currentFluxDepositLoc;
                    while (true) {
                        while (!myRC.canSenseSquare(unloadTarget)) {
                            move(myRC.getLocation().directionTo(unloadTarget));
                        }
                        MapLocation neigh = findLocationToUnload(unloadTarget);
                        if (neigh == null) {
                            unloadTarget = unloadTarget.add(stairs);
                            
                        } else {

                            travelToLocation(neigh);

                            while (myRC.isMovementActive()) {
                                myRC.yield();
                            }
                            try {
                                while (myRC.senseGroundRobotAtLocation(unloadTarget) != null) {
                                    myRC.yield();
                                }

                                myRC.unloadBlockToLocation(unloadTarget);
                                hasBlocks = false;
                                this.state = WorkerState.BLOCK_GATHER;
                                break;
                            } catch (GameActionException e) {}
                        }
                    }
                    break;
                case FOLLOW_ARCHON:
                    MapLocation tempTarget = targetNearby(currentTarget);
                    if (tempTarget == null) {
                        break;
                    }

                    travelToLocation(tempTarget);
                    if (myRC.getLocation().equals(tempTarget)) {
                        int tab_i[] = new int[1];
                        tab_i[0] = archonNumber;
                        sendMessage("A", null, tab_i);
                    }
                    break;

                case FIND_ENERGON:
                    /*very simple, if there are archons nearby, ask them
                     if not, travel to current Flux deposit*/
                    if (myRC.senseNearbyAirRobots().length > 0) {
                        m = new Message();
                        m.strings = new String[1];
                        m.locations = new MapLocation[1];
                        m.strings[0]="E";
                        m.locations[0] = myRC.getLocation();

                        if (myRC.hasActionSet()) {
                            myRC.yield();
                        }
                        try {
                            myRC.broadcast(m);
                        } catch (Exception e) {}
                    } else {
                        myRC.yield();
                    }
                    if (!energonLow()) {
                        this.state = this.prevState;
                        break;
                    }


            }
            myRC.yield();
        }
    }
}
