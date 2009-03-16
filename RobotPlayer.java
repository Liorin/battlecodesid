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


class PathElement {
    public MapLocation loc;
    public MapLocation previous;
    public double cost;
    
    public PathElement(MapLocation m, double c, MapLocation prev) {
        this.loc = m;
        this.cost = c;
        this.previous = prev;
    }

    public boolean equals(PathElement p){
        if (p == null) {
            return false;
        }
        if (p == this) {
            return true;
        }
        if ((this.cost == p.cost) && (this.loc.equals(p.loc)) ) {
            return true;
        }
        return false;
    }
}


class MapComparator implements Comparator<PathElement> {
    private MapLocation target;

    public MapComparator (MapLocation t ) {
        this.target = t;
    }

    public int estimate(MapLocation m1) {
        return m1.distanceSquaredTo(this.target);
    }

    public int compare(PathElement m1, PathElement m2) {
        /*we assume that there are no nulls*/
        if (m1.equals(m2)) {
            return 0;
        }

        double c1,c2;
        c1 = m1.cost;
        c2 = m2.cost;
        if ((c1 +  this.estimate(m1.loc)) > (c2 + this.estimate(m2.loc) ) ) {
            return 1;
        }
        if ((c1 +  this.estimate(m1.loc)) < (c2 + this.estimate(m2.loc) ) ) {
            return -1;
        }

        return 0;
    }
}

public class RobotPlayer implements Runnable {

    public enum RobotState {START, FLUX_SEARCH, FLUX_GATHER, FLUX_CAPTURE, BLOCK_RETURN, BLOCK_SEARCH, BLOCK_GATHER, FIND_ENERGON}


    private final RobotController myRC;
    private RobotState state, prevState;
    private int archonNumber = 1;
    private boolean hasBlocks = false;

    public FluxDeposit currentFluxDeposit;
    public MapLocation currentFluxDepositLoc;
    public MapLocation archonLocation[]; 
    public MapLocation forbidden;
    public RobotPlayer(RobotController myRC) {
        this.myRC = myRC;
        this.state = RobotState.START;
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

    private int findLocation(List<PathElement> l, MapLocation m) {
        int ret = 0;
        Iterator<PathElement> iter = l.iterator();
        while(iter.hasNext()) {
            PathElement p = iter.next();
            if (p.loc.equals(m)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

    private LinkedList<PathElement> localPathFind(MapLocation start, MapLocation end) {
        if (start == null || end == null || start.equals(end)) {
            return null;
        }

        Comparator<PathElement> comp = new MapComparator(end);
        PathElement startElement = new PathElement(start, 0, null);
        PathElement p;
        PriorityQueue<PathElement> queue = new PriorityQueue<PathElement>(100, comp);
        List<PathElement> visited = new LinkedList<PathElement>();
        queue.offer(startElement);
        while (true) {
            if (queue.isEmpty()) {
                return null;
            }
            p = queue.poll();
            visited.add(p); 
            if (p.loc.equals(end)) {
                /* if we found the target, break from the loop*/
                break;
            }
            /*if not, add neighbours to priority queue*/
            for (Direction d : Direction.values()) {
                if (d != Direction.OMNI && d != Direction.NONE) {
                    TerrainTile tile;
                    MapLocation loc = p.loc;
                    MapLocation newLoc = p.loc.add(d);
                    if (!myRC.canSenseSquare(newLoc)) {
                        continue;
                    }
                   
                    tile = myRC.senseTerrainTile(newLoc);
                    boolean robotsPresent = false;
                    
                    try {
                        switch (myRC.getRobotType()) {
                            case ARCHON:
                                robotsPresent = !(myRC.senseAirRobotAtLocation(newLoc) == null);
                                break;
                            case WORKER:
                                robotsPresent = !(myRC.senseGroundRobotAtLocation(newLoc) == null);
                               
                                break;
                        }
                    } catch (Exception e) {}
                    if  ( (findLocation(visited, p.loc) < 0) &&
                            (tile != null) && 
                            (tile.isTraversableAtHeight(myRC.getRobot().getRobotLevel())) &&
                            (!robotsPresent) ) {
                        queue.offer(new PathElement(newLoc, p.cost+loc.distanceSquaredTo(newLoc) ,loc));
                    }
                }
            }                  
        }
        /*we are outside the loop, it means that we found the path
         so, now we need to get actual set of MapLocations from the visited set*/
        LinkedList<PathElement> ret = new LinkedList<PathElement>();
        System.out.print("Found path!\n");


        /*Using PathElement previous field, we search backwards trough visited, and create list of PathElements
         indicating path from start to end, that is passable by our robot
         (PathElement with loc = start is not included on returned list */
        while (!p.loc.equals(start)) {
            ret.addFirst(p);
            int index = findLocation(visited, p.previous);
            if (index < 0) {
                return null;
            }
            p = visited.remove(index);
        }
       
        return ret;
    }

    public void findFluxDeposit() {
    /*This method finds uncaptured flux source*/
        FluxDeposit depositArray[];
        FluxDepositInfo depositInfo = null;
        Direction dir;
        int i = 0;

        while(true) {
            
            depositArray = myRC.senseNearbyFluxDeposits();
            if (depositArray.length != 0) {
                for ( i = 0; i < depositArray.length; i++) {
                    try {
                        depositInfo = myRC.senseFluxDepositInfo(depositArray[i]);
                        if ( (myRC.senseGroundRobotAtLocation(depositInfo.location) == null) &&
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
           if (dir != Direction.NONE) {
                if (!move(dir)) {
                    return;
                }
           }
        


        }
    }

    public void travelToLocation(MapLocation target) {
        /*Right now, it's just the basic pathfinding(AKA just go straight)*/
        Direction dir;
        MapLocation current;
        int count;
        if (target == null)
            return;
        while (!myRC.getLocation().equals(target)) {
            current = myRC.getLocation();
            count = 0;
            dir = current.directionTo(target);
           
            while ((!myRC.canMove(dir)) && (count <4)) {
                dir.rotateRight();
                count++;
            }
            if (!move(dir)) {
                return;
            }
            myRC.yield();
        }
    }


    public boolean sendBlockMessage() {
        /*This assumes that worker was just spawned on top of flux deposit*/
        Message m = new Message();
        m.strings = new String[1];
        m.locations = new MapLocation[1];
        m.strings[0] = "B";
        m.locations[0] = currentFluxDepositLoc;
        myRC.yield();
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
        try {
            myRC.spawn(RobotType.WORKER);
            myRC.yield();
            for (int i = 0; i < 15; i++) {
                myRC.transferEnergon(1, myRC.getLocation().add(myRC.getDirection()), RobotLevel.ON_GROUND);
                myRC.yield();
            }
            Message me = new Message();
            me.ints = new int[1];
            me.locations = new MapLocation[2];
            me.strings = new String[1];
            me.ints[0] = archonNumber;
            me.strings[0] = "W";
            me.locations[0]= myRC.getLocation().add(myRC.getDirection());
            me.locations[1] = currentFluxDepositLoc;
            if (myRC.hasActionSet()) {
                myRC.yield();
            }
            myRC.broadcast(me);
        } catch (Exception e) {return false;}
        return true;

    }

    public void runArchon() {
        while (true) {
            Message newMesg[] = myRC.getAllMessages();
            for (int i = 0; i < newMesg.length; i++) {
                Message mes = newMesg[i];
                if ((mes.strings[0]).equals("m")) {
                    if (mes.locations[0].equals(currentFluxDepositLoc)) {
                        currentFluxDeposit = null;
                        currentFluxDepositLoc = null;
                        state = RobotState.FLUX_SEARCH;
                        
                    }
                }

                if ((mes.strings[0]).equals("E")) {
                    travelToLocation(mes.locations[0].subtract(myRC.getDirection()));
                    for (int j = 0; j < 15; j++) {
                        try {
                            myRC.transferEnergon(1, mes.locations[0], RobotLevel.ON_GROUND);
                        } catch (Exception e) {}
                    }
                }
            }

            Robot r[] = myRC.senseNearbyGroundRobots();
            for (int j =0; j < r.length; j++) {
                try {
                if (myRC.senseRobotInfo(r[j]).location.isAdjacentTo(myRC.getLocation())) {
                    myRC.transferEnergon(1,myRC.senseRobotInfo(r[j]).location , RobotLevel.ON_GROUND);
                }
                } catch (Exception e) {}
            }
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
                case FLUX_SEARCH:
                    findFluxDeposit();
                   
                    break;

                case BLOCK_SEARCH:
                    if (!myRC.getLocation().equals(currentFluxDepositLoc)) {
                        travelToLocation(currentFluxDepositLoc);
                    }
                    try {
                        FluxDepositInfo inf = myRC.senseFluxDepositInfo(currentFluxDeposit);
                        if (inf.roundsAvailableAtCurrentHeight == 0) {
                            this.state = RobotState.FLUX_SEARCH;
                        }
                    } catch (Exception e) {}
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
                                myRC.yield();
                                myRC.broadcast(m);
                                state = RobotState.BLOCK_SEARCH;
                            } else {break;}
                    } catch (Exception e) {}
                   
                    spawnWorker();
                    break;
                    

                case FLUX_GATHER:
                    travelToLocation(currentFluxDepositLoc);
                    break;
                }
            myRC.yield();
        }
    }
    
    public boolean workerGoTo(MapLocation target) {
         if (target == null) {
             return false;
         }
         
       
         
         while (!myRC.getLocation().equals(target)) {  
/*First, we figure furthest square (tempTarget) in direction to real target, that is traversable by our robot*/
            MapLocation tempTarget = myRC.getLocation();
            Direction dir = tempTarget.directionTo(target);
            while (myRC.canSenseSquare(tempTarget) &&
                    (!tempTarget.equals(target)) &&
                    myRC.senseTerrainTile(tempTarget).isTraversableAtHeight(myRC.getRobot().getRobotLevel())) {
                tempTarget = tempTarget.add(dir);
            }
            if (!tempTarget.equals(target)) {
                tempTarget = tempTarget.subtract(dir);
            }
            

            List<PathElement> path = localPathFind(myRC.getLocation(), tempTarget);
            if (path == null) {
                return false;
            }
            for (PathElement p : path) {
                if (!move(myRC.getLocation().directionTo(p.loc)) ) {
                    return false;
                }
            }
         }
            
            
            
            return true;
    }

    public boolean energonLow() {
        if (myRC.getEnergonLevel() <= 2.0 ) {
            return true;
        }
        return false;
    }


    public boolean canLoadBlock(MapLocation from, MapLocation target) {
        if (from == null || target == null || from.equals(target)) {
            return false;
        }

        if (!from.isAdjacentTo(target)) {
            return false;
        }
        try {
        if (Math.abs(myRC.senseHeightOfLocation(target) - myRC.senseHeightOfLocation(from)) > GameConstants.WORKER_MAX_HEIGHT_DELTA) {
            return false;
        } }
        catch (Exception e) {return false;}

        return true;
    }
    
    private MapLocation findLocationToUnload(MapLocation to) {
        for (Direction d : Direction.values()) {
            if (d != Direction.OMNI && d != Direction.NONE &&  
                     !to.add(d).equals(currentFluxDeposit)) {
                MapLocation loc, neigh;
                loc = to;
                neigh = loc.add(d);
                if (canLoadBlock(neigh, loc)) {
                    return neigh;
                }
            }
        }
        return null;                            
    }

    public void runWorker() {
        Message m;
        MapLocation l = null;

        while (true)  {
            if (energonLow() && (this.state != RobotState.FIND_ENERGON)) {

                this.prevState = this.state;
                this.state = RobotState.FIND_ENERGON;
            }
            if ((this.state == RobotState.FIND_ENERGON) && (!energonLow())) {
                this.state = this.prevState;
            }
            m = myRC.getNextMessage();
            if (m != null) {
                if (m.strings[0].equals("W")) {
                    if (m.locations[0].equals(myRC.getLocation())) {
                        /*This message is for that robot*/
                        this.archonNumber = m.ints[0];
                        this.currentFluxDepositLoc = m.locations[1];
                        this.state = RobotState.BLOCK_GATHER;
                        System.out.printf("My archon num %d", archonNumber);
                    }
                }


            }
            switch (state) {
                case START:
                   
                    break;
                case BLOCK_GATHER:
                    MapLocation blocks[] = myRC.senseNearbyBlocks();
                   
                    for (int j = 0; j < blocks.length; j++) {
                        for (Direction d : Direction.values()) {
                            if (d != Direction.OMNI && d != Direction.NONE &&
                                    !blocks[j].equals(currentFluxDepositLoc)) {
                                MapLocation loc, neigh;
                                loc = blocks[j];
                                neigh = loc.add(d);
                                if (canLoadBlock(neigh, loc)) {
                                    
                                    travelToLocation(neigh);
                                    myRC.yield();
                                    try {
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

                    }
                    /*at least one location with blocks nearby*/

                    
                    break;
                case BLOCK_RETURN:
                    MapLocation neigh = findLocationToUnload(currentFluxDepositLoc);
                    if (neigh == null) { break; }
                    
                    travelToLocation(neigh);
                    myRC.yield();
                    try {
                        myRC.unloadBlockToLocation(currentFluxDepositLoc);
                        hasBlocks = false;
                        this.state = RobotState.BLOCK_GATHER;
                        break;
                    } catch (Exception e) {}
                                
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
                        workerGoTo(currentFluxDepositLoc);
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
