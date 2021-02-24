package fullObservability;

import wumpus.Agent;
import wumpus.World;

import java.util.*;


public class SearchAI extends Agent {
    public static class State {
        public int positionX;
        public int positionY;
        public boolean gold;
        public int direction; // The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
        public int gscore;
        public int tscore;
        public boolean arrow;
        public boolean wumpus;

        public State(int positionX, int positionY, boolean gold_retrieved, int direction, int gscore, int tscore, boolean arrow, boolean wumpus) {
            this.positionX = positionX;
            this.positionY = positionY;
            this.gold = gold_retrieved;
            this.direction = direction;
            this.gscore = gscore;
            this.tscore = tscore;
            this.arrow = arrow;
            this.wumpus = wumpus;
        }
    }

    private ListIterator<Action> planIterator;

    // Implements the method to calculate which element should
    // be removed from the priority list. Compares the scores
    // of the states and removes the one with the smallest
    public static Comparator<State> stateComparator = new Comparator<>() {
        @Override
        public int compare(State state1, State state2) {
            return state1.tscore - state2.tscore;
        }
    };


    public SearchAI(World.Tile[][] board) {


        Scanner in = new Scanner(System.in);

        // Data structure for action plan to be returned
        LinkedList<Action> plan;
        plan = new LinkedList<Action>();




        // Figure out if the gold is reachable
        LinkedList<Integer> goldPosition;
        goldPosition = new LinkedList<Integer>();
        getGoldPosition(board, goldPosition);
        //System.out.println(goldPosition);

        boolean reachable = isGoldReachable(board);
        // If the gold is not reachable, climb out
        if (!reachable) {
            plan.add(Action.CLIMB);
            planIterator = plan.listIterator();
            return;
        }

        // Create initial state and update its score
        //System.out.println("Creating initial state");
        State initialState, newState, goalState, currentState;
        initialState = new State(0, 0, false, 0, 0, Integer.MAX_VALUE, true, true);


        plan = searchPath(initialState, goldPosition, board, true, 0, 0, null);

        planIterator = plan.listIterator();
    }

    public static LinkedList<Action> searchPath(State initialState, LinkedList<Integer> goldPosition, World.Tile[][] board,
                                         boolean fullyObservable, int maxRow, int maxCol, ArrayList<String> safeTiles) {

        State newState, currentState;
        LinkedList<Action> plan;
        plan = new LinkedList<Action>();

        PriorityQueue<State> frontier = new PriorityQueue<>(stateComparator);
        Map<State, State> parent = new HashMap<>();
        Map<State, Integer> score = new HashMap<>();
        Map<State, Action> action = new HashMap<>();
        // Connect string to state
        Map<String, State> strState = new HashMap<>();


        initialState.tscore = initialState.gscore + calculateManhattan(initialState, goldPosition, fullyObservable);


        score.put(initialState, initialState.tscore);
        action.put(initialState, null);
        parent.put(initialState, null);

        frontier.add(initialState);

        String strCode = getStringFromState(initialState);
        strState.put(strCode, initialState);


        //System.out.println("Initial state score:" + score.get(initialState));


        //System.out.println("Getting to the main loop");
        while(!frontier.isEmpty()) {
            //System.out.println("Priority queue size:" + frontier.size());
            //System.out.println("Score: "+ frontier.remove().score);


            // The goal state: in position 0,0 while having the gold
            if ((frontier.peek().positionX == 0 && frontier.peek().positionY == 0 && frontier.peek().gold && fullyObservable) ||
                (frontier.peek().positionX == goldPosition.get(0) && frontier.peek().positionY == goldPosition.get(1) && !fullyObservable)){
                //System.out.println("Found gold");
                currentState = frontier.remove();
                plan = getPath(currentState, parent, action, fullyObservable);
                //System.out.println("Final plan:");
                //for(Action act : plan) System.out.print(act + " ");
                //System.out.println();

                break;
            }

            currentState = frontier.remove();
            if (!strState.containsKey(getStringFromState(currentState))) {
                //System.out.println("Continue reached");
                continue;
            }

            //System.out.println("Priority queue size:" + frontier.size());
            //printState(currentState, "Current state ");
            for (Action act: Action.values()) {
                //System.out.println(act);
                newState = getNextState(currentState, act, board, goldPosition, fullyObservable, maxRow, maxCol, safeTiles);
                printState(newState, "The new state: ");
                if (newState.tscore > 1000) {
                    continue;
                }
                // Case where state has not been reached before
                String newStateStr = getStringFromState(newState);
                if (!strState.containsKey(newStateStr)) {
                    frontier.add(newState);
                    parent.put(newState, currentState);
                    action.put(newState, act);
                    score.put(newState, newState.tscore);

                    strCode = getStringFromState(newState);
                    strState.put(strCode, newState);
                } else {
                    // Case where state has been reached and the score is better
                    State tmpState = strState.get(strCode);
                    if (newState.tscore < score.get(tmpState) ) {
                        frontier.add(newState);
                        parent.put(newState, currentState);
                        action.put(newState, act);
                        score.put(newState, newState.tscore);
                        strState.put(strCode, newState);
                    }
                }
                //System.out.println(act);
                //printState(newState, "New state     ");


            }

        }

        //System.out.println("Score of potential next move: " + frontier.peek().tscore);
        // This must be the last instruction.
        if (frontier.isEmpty()) {

            System.out.println("Frontier was empty");
            //System.out.println("Score of the last move: " + frontier.peek().tscore);
            if(fullyObservable) {
                plan.add(Action.CLIMB);
            }
        }

        return plan;
    }


    public static State getNextState(State prevState, Action action, World.Tile[][] board, LinkedList<Integer> goldPosition,
                                     boolean fullyObservable, int maxRow, int maxCol, ArrayList<String> safeTiles) {

        int colDimension, rowDimension = 0;
        if(fullyObservable) {
            colDimension = board.length;
            rowDimension = board[0].length;
        } else {
            colDimension = maxCol;
            rowDimension = maxRow;
        }


        State currentState = new State(prevState.positionX, prevState.positionY, prevState.gold, prevState.direction,
                prevState.gscore, Integer.MAX_VALUE, prevState.arrow, prevState.wumpus);


        switch ( action )
        {
            case TURN_LEFT:
                if (--currentState.direction < 0) currentState.direction = 3;
                ++currentState.gscore;
                break;

            case TURN_RIGHT:
                if (++currentState.direction > 3) currentState.direction = 0;
                ++currentState.gscore;
                break;

            case FORWARD:
                if ( currentState.direction == 0 && currentState.positionX+1 < colDimension )
                    ++currentState.positionX;
                else if ( currentState.direction == 1 && currentState.positionY-1 >= 0 )
                    --currentState.positionY;
                else if ( currentState.direction == 2 && currentState.positionX-1 >= 0 )
                    --currentState.positionX;
                else if ( currentState.direction == 3 && currentState.positionY+1 < rowDimension )
                    ++currentState.positionY;

                ++currentState.gscore;

                // Check if the new position is pit or wumpus
                if(fullyObservable) {
                    if (board[currentState.positionX][currentState.positionY].getPit() || (board[currentState.positionX][currentState.positionY].getWumpus()
                            && currentState.wumpus)) {
                        currentState.gscore += 1000;
                    }
                } else {
                    if(!safeTiles.contains("" + currentState.positionX + currentState.positionY)) {
                        currentState.gscore += 1000;
                    }
                }
                break;

            case SHOOT:
                if(fullyObservable) {
                    if (currentState.arrow) {
                        currentState.arrow = false;
                        currentState.gscore += 11;
                        if (currentState.direction == 0) {
                            for (int x = currentState.positionX; x < colDimension; ++x)
                                if (board[x][currentState.positionY].getWumpus()) {
                                    currentState.wumpus = false;
                                }
                        } else if (currentState.direction == 1) {
                            for (int y = currentState.positionY; y >= 0; --y)
                                if (board[currentState.positionX][y].getWumpus()) {
                                    currentState.wumpus = false;
                                }
                        } else if (currentState.direction == 2) {
                            for (int x = currentState.positionX; x >= 0; --x)
                                if (board[x][currentState.positionY].getWumpus()) {
                                    currentState.wumpus = false;
                                }
                        } else if (currentState.direction == 3) {
                            for (int y = currentState.positionY; y < rowDimension; ++y)
                                if (board[currentState.positionX][y].getWumpus()) {
                                    currentState.wumpus = false;
                                }
                        }
                    } else {
                        ++currentState.gscore;
                    }
                } else {
                    currentState.gscore += 11;
                }
                break;

            case GRAB:
                if(fullyObservable) {
                    if (board[currentState.positionX][currentState.positionY].getGold()) {
                        currentState.gold = true;
                    }
                    ++currentState.gscore;
                } else {
                    currentState.gscore += 1000;
                }
                break;

            case CLIMB:
                if(fullyObservable) {
                    if (currentState.positionX == 0 && currentState.positionY == 0) {
                        if (currentState.gold)
                            currentState.gscore -= 1000;
                    }
                    ++currentState.gscore;
                } else {
                    currentState.gscore += 1000;
                }
                break;
        }

        // Example of creating new state for one of the actions
        /*if (action == Action.FORWARD) {
            if (currentState.direction == 'r') {
                // TODO: Calculate cost for new state: cost of parent + cost of action + manhattan (to gold and back)
                // Score 15 is just an example below
                //currentState = new State(0, prevState.positionY + 1, false, 'r', 15, );
            }

        }*/

        // Add Manhattan distance to score
        currentState.tscore = currentState.gscore + calculateManhattan(currentState, goldPosition, fullyObservable);

        return currentState;

    }

    // Reverse the path to have the actions in the correct order
    public static LinkedList getPath(State currentState, Map<State, State> parent, Map<State, Action> action, boolean fullyObservable) {
       /* Scanner in = new Scanner(System.in);
        System.out.print ( "Please input: " );
        String userInput = in.next();*/

        //System.out.println("Path");
        LinkedList<Action> reversePath;
        reversePath = new LinkedList<>();

        if(fullyObservable) {
            reversePath.add(Action.CLIMB);
        }
        State tmpState = currentState;
        //printState(tmpState, "Path recon");

        while(parent.get(tmpState) != null) {

            reversePath.addFirst(action.get(tmpState));
            tmpState = parent.get(tmpState);
        }
        return reversePath;
    }


    // Returns if the gold is reachable
    // TODO: Are there more cases where we should climb out?
    public boolean isGoldReachable(World.Tile[][] board){

        for(int i=0; i<board.length; i++) {
            for(int j=0; j<board[0].length; j++) {
                if(board[i][j].getGold()) {
                    // Case where gold is in pit
                    if(board[i][j].getPit()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void getGoldPosition(World.Tile[][] board, LinkedList<Integer> goldPosition) {
        for(int i=0; i<board.length; i++) {
            for(int j=0; j<board[0].length; j++) {
                if(board[i][j].getGold()) {
                    goldPosition.add(i);
                    goldPosition.add(j);
                    break;
                }
            }
        }

    }

    // Calculate Manhattan distance from current position to gold and back to 0,0
    public static int calculateManhattan(State state, LinkedList<Integer> goldPosition, boolean fullyObservable) {

        int distance;
        if(fullyObservable) {
            if (!state.gold) {
                int toGold = Math.abs(state.positionX - goldPosition.get(0)) + Math.abs(state.positionY - goldPosition.get(1));
                int fromGold = Math.abs(0 - goldPosition.get(0)) + Math.abs(0 - goldPosition.get(1));
                distance = toGold + fromGold;
            } else {
                distance = Math.abs(state.positionX - 0) + Math.abs(state.positionY - 0);
            }
        } else {
            distance = Math.abs(state.positionX - goldPosition.get(0)) + Math.abs(state.positionY - goldPosition.get(1));
        }


        return distance;
    }

    public static void printState(State state, String init) {
        System.out.println(init + "\t" + state.positionX + ",\t" + state.positionY + "\tgold: " + state.gold +
                "\tdir " + state.direction + "\tgscore " + state.gscore +  "\ttscore " + state.tscore +
                "\tarrow " + state.arrow + "\twumpus " + state.wumpus);
    }

    public static String getStringFromState(State state) {
        String stateCode, g, a, w;

        if (!state.gold) {
            g = "0";
        } else {
            g = "1";
        }
        if (!state.arrow) {
            a = "0";
        } else {
            a = "1";
        }
        if (!state.wumpus) {
            w = "0";
        } else {
            w = "1";
        }

        stateCode = Integer.toString(state.positionX) + Integer.toString(state.positionY) + g + state.direction + a + w;
        //System.out.println(stateCode);

        return stateCode;

    }

    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        return planIterator.next();
    }

}
