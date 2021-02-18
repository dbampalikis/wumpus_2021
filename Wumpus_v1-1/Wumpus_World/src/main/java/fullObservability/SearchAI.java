package fullObservability;

import wumpus.Agent;
import wumpus.World;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Comparator;


public class SearchAI extends Agent {
    public class State {
        int positionX;
        int positionY;
        boolean gold;
        char direction;
        int score;
        State parent;
        Action action;

        public State(int positionX, int positionY, boolean gold_retrieved, char direction, int score, State parent, Action action) {
            this.positionX = positionX;
            this.positionY = positionY;
            this.gold = gold_retrieved;
            this.direction = direction;
            this.score = score;
            this.parent = parent;
            this.action = action;
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
                return state1.score - state2.score;
            }
        };

        LinkedList<Action> plan;
        plan = new LinkedList<Action>();


        // Figure out if the gold is reachable
        boolean reachable = isGoldReachable(board);
        // If the gold is not reachable, climb out
        if (!reachable) {
            plan.add(Action.CLIMB);
        }

        PriorityQueue<State> frontier = new PriorityQueue<>(stateComparator);
        List<State> explored;

        State initialState, newState, goalState;
        initialState = new State(0, 0, false, 'r', 16, null, null);
        goalState = new State(0, 0, true, 'r', 10, null, null);

        newState = getNextStates(initialState, Action.FORWARD);
        System.out.println("PositionX: " + newState.positionX + " positionY: " + newState.positionY);


        frontier.add(initialState);
        frontier.add(newState);
        frontier.add(goalState);
        //frontier.add(test2);

        while(!frontier.isEmpty()) {
            //System.out.println("Score: "+ frontier.remove().score);


            // The goal state: in position 0,0 while having the gold
            if (frontier.peek().positionX == 0 && frontier.peek().positionY == 0 && frontier.peek().gold) {
                System.out.println("Found gold");
                break;
            }
            current_state = frontier.remove();
            frontier.add(getNextStates(current_state));
            // Add the expanded state to the hash for closed
            // and store parent and action taken
            closed_set.add(current_state);

        }

        // Backtrack on the hashmap in order to get the path


        // This must be the last instruction.
        planIterator = plan.listIterator();
    }

    public State getNextStates(State currentState, Action action) {

        State newState = new State(-1, -1, false, 'n', Integer.MAX_VALUE, null , null);

        // TODO: Use the loop to iterate through all possible actions
        // TODO: Probably need if statements for non possible actions
        // eg climbing in a tile except for 0,0
        for (Action act: Action.values()) {
            System.out.println(act);
        }

        // TODO: Check if the state already exists and then if it has better score than the stored one
        // TODO: If not, remove state, otherwise update hashmap


        // Example of creating new state for one of the actions
        if (action == Action.FORWARD) {
            if (currentState.direction == 'r') {
                // TODO: Calculate cost for new state: cost of parent + cost of action + manhattan (to gold and back)
                // Score 15 is just an example below
                newState = new State(0, currentState.positionY + 1, false, 'r', 15, currentState, action);
            }

        }


        return newState;

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


    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        return planIterator.next();
    }

}
