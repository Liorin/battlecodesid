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

    public enum RobotState {START, BLOCK_WAIT, FOLLOW_ARCHON, BLOCK_SEARCH_START, FLUX_SEARCH, FLUX_GATHER, FLUX_CAPTURE, BLOCK_RETURN, BLOCK_SEARCH, BLOCK_GATHER, FIND_ENERGON}


    private final RobotController myRC;
    private RobotState state, prevState;
    private int archonNumber = 1;
    private int workerCount = 0;
    private int waiting = 0;
    
    private boolean hasBlocks = false;

    /*Location and info about currently exploited flux deposit*/
    public FluxDeposit currentFluxDeposit;
    public MapLocation currentFluxDepositLoc;
    public MapLocation archonLocation[]; 
    
    /*Shows the direction of stairs to the flux depo*/
    public Direction stairs = null;
    
    /*variables needed by the workers*/
    private MapLocation currentTarget = null;
    
    
    public RobotPlayer(RobotController myRC) {
        this.myRC = myRC;
        this.state = RobotState.START;
    }
    
    private void refillAdjacent() {
        try {
            
            for (Direction d : Direction.values()) {
                if (d != Direction.NONE && d != Direction.OMNI) {
                    MapLocation loc = myRC.getLocation().add(d);
                    Robot r = myRC.senseGroundRobotAtLocation(loc);
                    if (r != null) {
                        myRC.transferEnergon(1, loc, RobotLevel.ON_GROUND);
                        myRC.yield();
                    }
                }
            }
        } catch (Exception e) {}
        
    }

    private int dirToInt(Direction d) {
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

    private Direction intToDir(int i) {
        switch (i) {
            case 1: return Direction.EAST;
            case 2: return Direction.NORTH;
            case 3: return Direction.WEST;
            case 4: return Direction.SOUTH;
        }
        return Direction.NONE;
    }

    private MapLocation addToLoc(MapLocation loc, Direction dir, int count) {
        if (count <= 0) {
            return loc;
        }
        MapLocation tempLoc = loc;
        for (int j = 0; j < count; j++) {
            tempLoc = tempLoc.add(dir);
        }
        return tempLoc;
    }
    
    private boolean isPassable(MapLocation loc) {
        try {
            TerrainTile t = myRC.senseTerrainTile(loc);
            if (t.getType() == TerrainTile.TerrainType.LAND ) {
                return true;
            }
        } catch (Exception e) {return false;}
        return false;
    }
    
    private Direction findStairs(MapLocation start) {
        Direction dir = Direction.EAST; /*na wschodzie musi byc jakaś cywilizacja*/
        int count = 0;
        int free_fields;
        MapLocation tempLoc;
        while (count < 4) {
            free_fields = 0;
            tempLoc = start.add(dir);
            while ( isPassable(tempLoc)) {
                free_fields++;
                tempLoc = tempLoc.add(dir);
                if (free_fields >= 5) {
                    return dir;
                }
            }
            count++;
            dir = dir.rotateRight().rotateRight();
        }
        
        return null;
        
    }
    

    private boolean move(Direction dir) {
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

    

    public void findFluxDeposit() {
    /*This method finds uncaptured flux source*/
        FluxDeposit depositArray[];
        FluxDepositInfo depositInfo = null;
        Direction dir;
        MapLocation tempTarget;
        int i = 0;

        while(true) {
            
            depositArray = myRC.senseNearbyFluxDeposits();
            if (depositArray.length != 0) {
                for ( i = 0; i < depositArray.length; i++) {
                    try {
                        depositInfo = myRC.senseFluxDepositInfo(depositArray[i]);
                        if ( (myRC.senseAirRobotAtLocation(depositInfo.location) == null) &&
                            (depositInfo.team != myRC.getTeam() ) ) {
                            currentFluxDepositLoc = depositInfo.location;
                            currentFluxDeposit = depositArray[i];
                            state = RobotState.FLUX_CAPTURE;
                            
                            return;
                        }
                    }
                    catch (Exception e) {}

                }
            }
            dir = myRC.senseDirectionToUnownedFluxDeposit();
            tempTarget = addToLoc(myRC.getLocation(), dir, 1);
            travelToLocation(tempTarget);


        }
    }

    public void travelToLocation(MapLocation target) {
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

    public boolean sendMessage(String type, MapLocation locs[], int ints[])
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
   

    public boolean spawnWorker() {
        if (myRC.hasActionSet()) {
            myRC.yield();
        }
        
        while (myRC.getCurrentAction() != ActionType.IDLE) {
            myRC.yield();
        }
        while (myRC.getEnergonLevel() <= 15) {
            myRC.yield();
        }
        try {
            myRC.spawn(RobotType.WORKER);
            myRC.yield();
            for (int i = 0; i < 15; i++) {
                try {
                    myRC.transferEnergon(1, myRC.getLocation().add(myRC.getDirection()), RobotLevel.ON_GROUND);
                } catch (Exception e) {}
                myRC.yield();
            }
            
            int ints[] = new int[2];
            MapLocation locations[] = new MapLocation[2];
            ints[0] = archonNumber;
            ints[1] = dirToInt(stairs);
            locations[0]= myRC.getLocation().add(myRC.getDirection());
            locations[1] = currentFluxDepositLoc;
            if (!sendMessage("W", locations, ints) ) {
                return false;
            }

        } catch (Exception e) {return false;}
        this.workerCount++;
        return true;

    }

    private boolean isFreeAndPassable(MapLocation target) {
        try {
            if (isPassable(target) && (myRC.senseGroundRobotAtLocation(target) == null)) {
                return true;
            }
        } catch (Exception e) {}
        return false;

    }

    public void runArchon() {
        while (true) {
            Message newMesg[] = myRC.getAllMessages();
            for (int i = 0; i < newMesg.length; i++) {
                Message mes = newMesg[i];
                if ((mes.strings[0]).equals("m")) {
                    if (mes.locations[0].equals(currentFluxDepositLoc) && mes.ints[0] != archonNumber) {
                        
                            currentFluxDeposit = null;
                            currentFluxDepositLoc = null;
                            state = RobotState.FLUX_SEARCH;
                        
                        
                    }
                }

                /*if ((mes.strings[0]).equals("E")) {
                    travelToLocation(mes.locations[0].subtract(myRC.getDirection()));
                    for (int j = 0; j < 15; j++) {
                        try {
                            myRC.transferEnergon(1, mes.locations[0], RobotLevel.ON_GROUND);
                        } catch (Exception e) {}
                    }
                }*/

                if (mes.strings[0].equals("A") && mes.ints[0] == archonNumber) {
                    waiting--;
                }
            }

            refillAdjacent();
            switch (state) {
                case START:
                    archonLocation = myRC.senseAlliedArchons();
                    int i;
                    for (i = 0; i < archonLocation.length ; i++) {
                        /*here, we determine the number of our archon
                         in order to do so, we get locations of all archons,
                         and figure how many archons locations
                         precede current archon in natural order*/
                        if (
                                (archonLocation[i].getX() < myRC.getLocation().getX()) ||
                                (
                                    (archonLocation[i].getX() == myRC.getLocation().getX()) &&
                                    (archonLocation[i].getY() < myRC.getLocation().getY())
                                )
                           )
                        {
                            this.archonNumber++;
                        }
                    }
                    state = RobotState.FLUX_SEARCH;
                    break;
                case FLUX_SEARCH:
                    findFluxDeposit();
                   
                    break;

                case BLOCK_SEARCH_START:
                    /*spawn 4 workers*/
                    
                    Direction dir = myRC.getDirection();
                    for (int m = 0; m < 4; m++) {
                        int count = 0;
                        myRC.yield();
                        while (!isFreeAndPassable(myRC.getLocation().add(dir))) {
                            dir = dir.rotateRight();
                            try {
                                myRC.yield();
                                myRC.setDirection(dir);
                            } catch (Exception e ) {}
                            myRC.yield();
                            count++;
                            if (count >= 8) {
                                break;
                            }
                            
                        }
                        spawnWorker();
                        
                    }
                    waiting = workerCount;
                    this.state = RobotState.BLOCK_SEARCH;
                    break;
                case BLOCK_SEARCH:
                   
                    /*find nearby blocks, workers follow*/
                    MapLocation arr[] =  myRC.senseNearbyBlocks();
                    if (arr.length == 0) {
                        break;
                    }
                    try {
                    if (myRC.senseFluxDepositInfo(currentFluxDeposit).roundsAvailableAtCurrentHeight == 0) {
                        this.state = RobotState.FLUX_SEARCH;
                    }
                    } catch (Exception e) {}
                    /*currentTarget = arr[0];
                    int is[] = new int[1];
                    is[0] = archonNumber;
                    sendMessage("F", arr, is);
                    this.state = RobotState.BLOCK_WAIT;
                    */

                    break;
                case BLOCK_WAIT:
                    break;
                case FLUX_CAPTURE:
                    travelToLocation(currentFluxDepositLoc);
                    myRC.yield();
                    try{
                        if ( (myRC.senseFluxDepositInfo(currentFluxDeposit).team == myRC.getTeam() )
                            && (myRC.getLocation().equals(currentFluxDepositLoc)) ) {
                                Message m = new Message();
                                m.locations = new MapLocation[1];
                                m.locations[0] = currentFluxDepositLoc;
                                m.strings = new String[1];
                                m.strings[0] = "m";
                                m.ints = new int[1];
                                m.ints[0] = archonNumber;
                                myRC.yield();
                                myRC.broadcast(m);
                                myRC.yield();
                                myRC.yield();
                                state = RobotState.BLOCK_SEARCH_START;
                                stairs = findStairs(currentFluxDepositLoc);
                               
                            } else {break;}
                    } catch (Exception e) {}
                   
                    
                    break;
                    

                case FLUX_GATHER:
                    travelToLocation(currentFluxDepositLoc);
                    break;
                }
            myRC.yield();
        }
    }

    

    private MapLocation targetNearby(MapLocation target) {
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

    private void travelNearLocation(MapLocation target) {
        MapLocation tmp = targetNearby(target);
        travelToLocation(tmp);
    }

    public boolean energonLow() {
        if (myRC.getEnergonLevel() <= 2.0 ) {
            return true;
        }
        return false;
    }


    public boolean isForbiddenToLoad(MapLocation from) {
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
    
    public void runWorker() {
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
                        this.state = RobotState.BLOCK_GATHER;
                        
                    }
                }

                if (m.strings[0].equals("F") && (m.ints[0] == archonNumber)) {
                    this.state = RobotState.FOLLOW_ARCHON;
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
                            this.state = RobotState.BLOCK_RETURN;
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
                            continue;
                        }
                    
                        travelToLocation(neigh);

                        while (myRC.isMovementActive()) {
                            myRC.yield();
                        }
                        try {
                            myRC.unloadBlockToLocation(unloadTarget);
                            hasBlocks = false;
                            this.state = RobotState.BLOCK_GATHER;
                            break;
                        } catch (GameActionException e) {}
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


    public void run() {
        switch(myRC.getRobotType()) {
            case ARCHON :            
                runArchon();
                break;
            case WORKER: 
                runWorker();
                break;
        }

    }


}
