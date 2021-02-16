package fullObservability;





import wumpus.Agent;
import wumpus.World;

import java.util.LinkedList;
import java.util.ListIterator;


// Java program to print the elements of
// a 2 D array or matrix
import java.io.*;





public class SearchAI extends Agent {
    private ListIterator<Action> planIterator;


    public static void print2D(int mat[][]) {
        // Loop through all rows
        for (int i = 0; i < mat.length; i++)

            // Loop through all elements of current row
            for (int j = 0; j < mat[i].length; j++)
                System.out.print(mat[i][j] + " ");
    }

    public int boolToInt(boolean b) {
        return b ? 1 : -1;
    }

    public SearchAI(World.Tile[][] board) {



        /* The world is board[coloumn][row] with initial position (bottom left) being board[0][0] */

       LinkedList<Action> plan;

        System.out.println("The size of this board is: " + board.length + " x " + board[0].length);

        /* Create 2d arrays in the same size as the map */
        int pitArray[][] = new int[board.length][board[0].length];
        int wumpusArray[][] = new int[board.length][board[0].length];
        int goldArray[][] = new int[board.length][board[0].length];

        /* A loop to get map information in a nice format */
        for (int i = 0; i<board.length; i++)
        {
            for (int j = 0; j < board[0].length; j++)
            {
                pitArray[i][j] = boolToInt(board[i][j].getPit());
                wumpusArray[i][j] = boolToInt(board[i][j].getPit());
                goldArray[i][j] = boolToInt(board[i][j].getPit());
            }
        }



        // Remove the code below //
         plan = new LinkedList<Action>();
         for (int i = 0; i<8; i++)
             plan.add(Agent.Action.FORWARD);
        plan.add(Action.TURN_LEFT);
        plan.add(Action.TURN_LEFT);
        for (int i = 10; i<18; i++)
            plan.add(Action.FORWARD);
        plan.add(Action.CLIMB);

        // This must be the last instruction.
        planIterator = plan.listIterator();
    }

    @Override
    public Agent.Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        /*
        System.out.println("Glitter: " + glitter);
        System.out.println("Stench: " + stench);
        System.out.println("Breeze: " + breeze);
        System.out.println(" "); */
        return planIterator.next();


    }

}
