package fullObservability;

import wumpus.Agent;
import wumpus.World;

import java.util.*;


public class SearchAI extends Agent {
    public class State {
        int positionX;
        int positionY;
        boolean gold;
        int direction; // The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
        int gscore;
        int tscore;
        boolean arrow;
        boolean wumpus;
        //State parent;
        //Action action;

        public State(int positionX, int positionY, boolean gold_retrieved, int direction, int gscore, int tscore, boolean arrow, boolean wumpus) {
            this.positionX = positionX;
            this.positionY = positionY;
            this.gold = gold_retrieved;
            this.direction = direction;
            this.gscore = gscore;
            this.tscore = tscore;
            this.arrow = arrow;
            this.wumpus = wumpus;
            //this.parent = parent;
            //this.action = action;
        }
    }

    private ListIterator<Action> planIterator;



    public SearchAI(World.Tile[][] board) {

        // Implements the method to calculate which element should
        // be removed from the priority list. Compares the scores
        // of the states and removes the one with the smallest
        // TODO: Maybe we need the opposite?
        Comparator<State> stateComparator = new Comparator<>() {
            @Override
            public int compare(State state1, State state2) {
                return state1.tscore - state2.tscore;
            }
        };
        Scanner in = new Scanner(System.in);

        // Data structure for action plan to be returned
        LinkedList<Action> plan;
        plan = new LinkedList<Action>();

        PriorityQueue<State> frontier = new PriorityQueue<>(stateComparator);
        Map<State, State> parent = new HashMap<>();
        Map<State, Integer> score = new HashMap<>();
        Map<State, Action> action = new HashMap<>();
        List<State> explored;

        // Figure out if the gold is reachable
        LinkedList<Integer> goldPosition;
        goldPosition = new LinkedList<Integer>();
        getGoldPosition(board, goldPosition);
        System.out.println(goldPosition);

        boolean reachable = isGoldReachable(board);
        // If the gold is not reachable, climb out
        if (!reachable) {
            plan.add(Action.CLIMB);
        }

        // Create initial state and update its score
        System.out.println("Creating initial state");
        State initialState, newState, goalState, currentState;
        initialState = new State(0, 0, false, 0, 0, Integer.MAX_VALUE, true, true);
        initialState.tscore = initialState.gscore + calculateManhattan(initialState, goldPosition);
        score.put(initialState, initialState.tscore);
        action.put(initialState, null);
        parent.put(initialState, null);
        frontier.add(initialState);



        //System.out.println("Initial state score:" + score.get(initialState));

        goalState = new State(0, 0, true, 5, 10, 0, true, true);


        //newState = getNextStates(initialState, Action.FORWARD);
        //System.out.println("PositionX: " + newState.positionX + " positionY: " + newState.positionY);



        //parent.put(initialState, null);


        //frontier.add(newState);
        //frontier.add(goalState);
        //frontier.add(test2);

        System.out.println("Getting to the main loop");
        while(!frontier.isEmpty()) {
            //System.out.println("Score: "+ frontier.remove().score);


            // The goal state: in position 0,0 while having the gold
            if (frontier.peek().positionX == 0 && frontier.peek().positionY == 0 && frontier.peek().gold) {
                System.out.println("Found gold");
                break;
            }


            currentState = frontier.remove();
            printState(currentState, "Current state ");
            // TODO: Use the loop to iterate through all possible actions
            // TODO: Probably need if statements for non possible actions
            // eg climbing in a tile except for 0,0
            for (Action act: Action.values()) {
                //System.out.println(act);
                newState = getNextStates(currentState, act, board, goldPosition);
                printState(newState, "New state ");
                frontier.add(newState);
                parent.put(newState, currentState);
                action.put(newState, act);
                score.put(newState, newState.tscore);
                System.out.print ( "Please input: " );
                String userInput = in.next();
            }

            // Add the expanded state to the hash for closed
            // and store parent and action taken


        }

        // Backtrack on the hashmap in order to get the path


        // This must be the last instruction.
        planIterator = plan.listIterator();
    }

    public State getNextStates(State prevState, Action action, World.Tile[][] board, LinkedList<Integer> goldPosition) {


        int colDimension = board[0].length;
        int rowDimension = board[1].length;

        State currentState = new State(prevState.positionX, prevState.positionY, prevState.gold, prevState.direction,
                prevState.gscore, Integer.MAX_VALUE, prevState.arrow, prevState.wumpus);



        // TODO: Check if the state already exists and then if it has better score than the stored one
        // TODO: If not, remove state, otherwise update hashmap
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
                if ( board[currentState.positionX][currentState.positionY].getPit() || board[currentState.positionX][currentState.positionY].getWumpus() )
                {
                    currentState.gscore += 1000;
                }
                break;

            case SHOOT:
                if ( currentState.arrow )
                {
                    currentState.arrow = false;
                    currentState.gscore += 10;
                    if ( currentState.direction == 0 )
                    {
                        for ( int x = currentState.positionX; x < colDimension; ++x )
                            if ( board[x][currentState.positionY].getWumpus() )
                            {
                                currentState.wumpus = false;
                            }
                    }
                    else if ( currentState.direction == 1 )
                    {
                        for ( int y = currentState.positionY; y >= 0; --y )
                            if ( board[currentState.positionX][y].getWumpus() )
                            {
                                currentState.wumpus = false;
                            }
                    }
                    else if ( currentState.direction == 2 )
                    {
                        for ( int x = currentState.positionX; x >= 0; --x )
                            if ( board[x][currentState.positionY].getWumpus() )
                            {
                                currentState.wumpus = false;
                            }
                    }
                    else if ( currentState.direction == 3 )
                    {
                        for ( int y = currentState.positionY; y < rowDimension; ++y )
                            if ( board[currentState.positionX][y].getWumpus() )
                            {
                                currentState.wumpus = false;
                            }
                    }
                }
                break;

            case GRAB:
                if ( board[currentState.positionX][currentState.positionY].getGold() )
                {
                    currentState.gold = true;
                }
                ++currentState.gscore;
                break;

            case CLIMB:
                if ( currentState.positionX == 0 && currentState.positionY == 0 )
                {
                    if ( currentState.gold )
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
        currentState.tscore = currentState.gscore + calculateManhattan(currentState, goldPosition);

        return currentState;

    }

    // Returns if the gold is reachable
    // TODO: Are there more cases where we should climb out?
    public boolean isGoldReachable(World.Tile[][] board){

        for(int i=0; i<board[0].length; i++) {
            for(int j=0; j<board[1].length; j++) {
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

    public void getGoldPosition(World.Tile[][] board, LinkedList<Integer> goldPosition) {
        for(int i=0; i<board[0].length; i++) {
            for(int j=0; j<board[1].length; j++) {
                if(board[i][j].getGold()) {
                    goldPosition.add(i);
                    goldPosition.add(j);
                }

            }
        }
    }

    // Calculate Manhattan distance from current position to gold and back to 0,0
    public int calculateManhattan(State state, LinkedList<Integer> goldPosition) {

        int distance;
        if (!state.gold) {
            int toGold = Math.abs(state.positionX - goldPosition.get(0)) + Math.abs(state.positionY - goldPosition.get(1));
            int fromGold = Math.abs(0 - goldPosition.get(0)) + Math.abs(0 - goldPosition.get(1));
            distance = toGold + fromGold;
        } else {
            distance = Math.abs(state.positionX - 0) + Math.abs(state.positionY - 0);
        }


        return distance;
    }

    public void printState(State state, String init) {
        System.out.println(init + state.positionX + ", " + state.positionY + " gold: " + state.gold +
                " dir " + state.direction + " gscore " + state.gscore +  " tscore " + state.tscore +
                " arrow " + state.arrow + " wumpus " + state.wumpus);
    }


    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        return planIterator.next();
    }

}
