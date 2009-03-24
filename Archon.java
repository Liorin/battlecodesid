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


public class Archon extends AbstractRobot {
    private enum RobotState {
        FLUX_SEARCH,
        FLUX_CAPTURE,
        START,
        BLOCK_SEARCH,
        BLOCK_SEARCH_START,
        FLUX_GATHER,
        BLOCK_WAIT

    }

    RobotState state;

    private MapLocation archonLocation[];
    private int workerCount = 0;
    private int waiting;

    public Archon(RobotController rc) {
        super(rc);
        this.state = RobotState.START;
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
    
     private void findFluxDeposit() {
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

    private boolean spawnWorker() {
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

    private boolean spawnSoldier() {
        if (myRC.hasActionSet()) {
            myRC.yield();
        }

        while (myRC.getCurrentAction() != ActionType.IDLE) {
            myRC.yield();
        }
        while (myRC.getEnergonLevel() <= RobotType.SOLDIER.spawnCost()) {
            myRC.yield();
        }
        try {
            myRC.spawn(RobotType.SOLDIER);
            myRC.yield();
            for (int i = 0; i < 15; i++) {
                try {
                    myRC.transferEnergon(1, myRC.getLocation().add(myRC.getDirection()), RobotLevel.ON_GROUND);
                } catch (Exception e) {}
                myRC.yield();
            }



        } catch (Exception e) {return false;}
        
        return true;

    }


    private void refillAdjacent() {
        try {

            for (Direction d : Direction.values()) {
                if (d != Direction.OMNI) {
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
    public boolean senseDanger() {
        Robot rs[];
        rs = myRC.senseNearbyGroundRobots();
        for (Robot r : rs) {
            try {
                if (myRC.senseRobotInfo(r).team != myTeam) {
                    return true;
                }

            } catch (Exception e) {}

        }

        return false;
    }

    public void run() {
        while (true) {
            Message newMesg[] = myRC.getAllMessages();
            for (int i = 0; i < newMesg.length; i++) {
                Message mes = newMesg[i];
                if ((mes.strings[0]).equals("m")) {
                    if (mes.locations[0].equals(currentFluxDepositLoc) && mes.ints[0] != archonNumber) {

                            currentFluxDeposit = null;
                            currentFluxDepositLoc = null;
                            /*Scatter the archons*/
                            Direction scatterDir = myRC.senseDirectionToUnownedFluxDeposit().opposite();
                            for (int k = 0; k < 10; k++) {
                                move(scatterDir);
                            }
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
            if (senseDanger() ) {
                
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
                        if ( (myRC.getLocation().equals(currentFluxDepositLoc)) ) {
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

}
