package partialObservability;
import fullObservability.SearchAI;
import org.tweetyproject.logics.pl.reasoner.*;
import org.tweetyproject.logics.pl.sat.*;
import org.tweetyproject.logics.pl.syntax.*;
import org.tweetyproject.logics.pl.parser.*;
import wumpus.Agent;
import wumpus.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MyAI extends Agent
{

/*
	// slide 23
	public static void main(String[] args) throws IOException {
		Proposition p = new Proposition("p");
		Proposition q = new Proposition("q");
		Conjunction pAq = new Conjunction(p,q); // [[p, q]]
		Disjunction pIq = new Disjunction(p,q); // [[p], [q], [p, q]]
		// http://tweetyproject.org/api/1.17/net/sf/tweety/logics/pl/syntax/Implication.html
		Implication i = new Implication(p,q); // [[q], [p, q], []]
		// slide 24
		PlParser plParser = new PlParser();
		PlFormula f = plParser.parseFormula("!a && b"); // !a&&b
		PlFormula g = plParser.parseFormula("b || c"); // b||c
		PlFormula h = f.combineWithAnd(g).toDnf(); // (!a&&b&&b)||(!a&&b&&c)
		// slide 25
		PlBeliefSet bs = new PlBeliefSet(); // { }
		Proposition a = new Proposition("a");
		Proposition b = new Proposition("b");
		bs.add((PlFormula) a.complement().combineWithOr(b)); // { !a||b }
		bs.add(a); // { !a||b, a }
		AbstractPlReasoner r = new SatReasoner();
		System.out.println(r.query(bs, b)); // true
	}
*/

	public MyAI ( )
	{

	}

	public static class State {
		public int positionX;
		public int positionY;
		public boolean gold;
		public int direction; // The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
		public int gscore;
		public int tscore;
		public boolean arrow;
		public boolean wumpus;
		public int maxRow;
		public int maxCol;

		public State(int positionX, int positionY, boolean gold_retrieved, int direction, int gscore, int tscore, boolean arrow, boolean wumpus, int maxRow, int maxCol) {
			this.positionX = positionX;
			this.positionY = positionY;
			this.gold = gold_retrieved;
			this.direction = direction;
			this.gscore = gscore;
			this.tscore = tscore;
			this.arrow = arrow;
			this.wumpus = wumpus;
			this.maxRow = maxRow;
			this.maxCol = maxCol;
		}
	}



	boolean DEBUG = true;
	// Variables for the size of the board
	int numCol = -1;
	int numRow = -1;
	int maxCol = 1;
	int maxRow = 1;

	public PlFormula MakeQuestion(String Letter, String XY) {
		return new Negation(new Proposition(Letter + XY));
	}

	public boolean Ask(PlBeliefSet bs, String symbol) {

		// Solver setup.
		AbstractPlReasoner r = new SatReasoner();
		SatSolver.setDefaultSolver(new Sat4jSolver());

		PlParser plParser = new PlParser();
		boolean answer = false;

		try {
			PlFormula f = plParser.parseFormula(symbol);
			answer = r.query(bs, f);
			if (DEBUG) System.out.println("Is " + f + " true? " + answer);
		} catch (IOException e) {
			System.out.println(e.getStackTrace());
		}

		return answer;
	}

	public static ArrayList<String> safeTiles = new ArrayList<String>();

	public Action getAction
			(
					boolean stench,
					boolean breeze,
					boolean glitter,
					boolean bump,
					boolean scream
			)
	{

		try {


			ArrayList<String> visitedTiles = new ArrayList<String>();
			PlBeliefSet bs = new PlBeliefSet();
			PlParser plParser = new PlParser();
			PlFormula question;
			boolean answer;
			String[] symbols;
			ArrayList<String> neighbors;
			int X, Y;

			System.out.println("Safe tiles!!!:" + safeTiles);
			// Default knowledge ---------------------------------------------------------------------------------------
			safeTiles.add("00");
			bs.add((PlFormula) new Negation(new Proposition ("P00")));


			// [1,1] ---------------------------------------------------------------------------------------------------
			X = 0; Y = 0;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");
			visitedTiles.add("" + X + Y);

			// Define initial state
			SearchAI.State currentState;
			currentState = new SearchAI.State(0, 0, false, 0, 0, Integer.MAX_VALUE, true, true);


			// Check if the tile contains the gold
			if(glitter) {
				if(currentState.gold) {
					// TODO: Call search AI to find optimal path
					// TODO: Possibly store this to a global var and pick from that every time
				} else {
					return Action.GRAB;
				}
			}

			// Make-percept-sentence
			// Breeze
			Proposition p = new Proposition("B" + currentState.positionX + currentState.positionY);
			if (breeze) {
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}
			bs.add(plParser.parseFormula(createDoubleImplication("B", currentState)));
			System.out.println("BS:" + bs);

			// Stench
			p = new Proposition("S" + X + Y);
			if (stench) {
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}
			bs.add(plParser.parseFormula(createDoubleImplication("S", currentState)));
			System.out.println("BS:" + bs);

			// Scream
			p = new Proposition("C" + X + Y);
			if (scream) {
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}

			// Bump
			if (bump) {
				bs.add(p);
				// The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
				// If there is no bump and the current position is bigger than the size of the map
				switch(currentState.direction)
				{
					case 0:
						numCol = currentState.positionX;
						break;
					case 3:
						numRow = currentState.positionY;
						break;
				}
			} else {

				bs.add((PlFormula) p.complement());
			}


			// Line safe <- {[x,y]: ASK(KB, OK) = true}
			// Questions - answers.
			symbols = new String[]{"P", "W", "!P", "!W"};
			neighbors = getNeighbors(currentState);

			for (String neighbor : neighbors) {
				for (String symbol : symbols) {

					answer = Ask(bs, symbol+neighbor);
					if (answer) {
						// Add this to the KB.
						bs.add(plParser.parseFormula(symbol+neighbor));
					}
				}
				answer = Ask(bs, "!P"+neighbor+"&&!W"+neighbor);
				if (answer) {
					// Add to safe tiles.
					safeTiles.add(neighbor);
					if (DEBUG) System.out.println("Tile " + neighbor + " is safe");
				}
			}






/*

			// Figure out here that we have 2 tiles to inspect: P12, P21.
			// Then add <=> to the KB.
			// Then check each tile with questions.

			bs.add(plParser.parseFormula("B11 <=> (P12 || P21)")); // To be generated automatically.
			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// Questions - answers.
			Letters = new String[]{"P"};
			Coordinates = new String[]{"12", "21"};

			for (String l : Letters) {
				for (String c : Coordinates) {

					question = MakeQuestion(l, c);
					answer = Ask(bs, question);
					if (answer) {
						// Add this to the KB.
						bs.add(question);

						// Add to safe tiles.
						safeTiles.add(c);
						if (DEBUG) System.out.println("Tile " + c + " is safe");
					}
				}
			}



			// [2,1] ---------------------------------------------------------------------------------------------------
			X = 2; Y = 1;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");
			visitedTiles.add("" + X + Y);

			bs.add((PlFormula) new Proposition("B" + X + Y));
			bs.add(plParser.parseFormula("B21 <=> (P11 || P22 || P31)")); // Do be generated automatically.

			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// Questions - answers.
			Letters = new String[]{"P"};
			Coordinates = new String[]{"22", "31"};

			for (String l : Letters) {
				for (String c : Coordinates) {

					question = MakeQuestion(l, c);
					answer = Ask(bs, question);
					if (answer) {
						// Add this to the KB.
						bs.add(question);

						// Add to safe tiles.
						safeTiles.add(c);
						if (DEBUG) System.out.println("Tile " + c + " is safe");
					}
				}
			}


			// [1,2] ---------------------------------------------------------------------------------------------------
			X = 1; Y = 2;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");
			visitedTiles.add("" + X + Y);

			bs.add((PlFormula) new Negation(new Proposition ("B" + X + Y)));
			bs.add(plParser.parseFormula("B12 <=> (P11 || P22 || P13)")); // Do be generated automatically.

			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// Questions - answers.
			Letters = new String[]{"P"};
			Coordinates = new String[]{"22", "13", "31"};


			*/
/*
			!!!!!!!!!!!! We should figure out here that 31 has a pit !!!!!!!!!!!!
			 Most probably we just need to ask both questions: Pxy and !Pxy.
			 Whichever returns `true` should be added to the KB.
			 It means that the if-statement `if (answer)` needs to be changed.
			*//*


			for (String l : Letters) {
				for (String c : Coordinates) {

					question = MakeQuestion(l, c);
					answer = Ask(bs, question);
					if (answer) {
						// Add this to the KB.
						bs.add(question);

						// Add to safe tiles.
						safeTiles.add(c);
						if (DEBUG) System.out.println("Tile " + c + " is safe");
					}
				}
			}


			// [2,2] ---------------------------------------------------------------------------------------------------
			X = 2; Y = 2;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");
			visitedTiles.add("" + X + Y);

			bs.add((PlFormula) new Negation(new Proposition ("B" + X + Y)));
			bs.add(plParser.parseFormula("B22 <=> (P12 || P23 || P32 || P21)"));

			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// Questions - answers.
			Letters = new String[]{"P"};
			Coordinates = new String[]{"23", "32"};

			for (String l : Letters) {
				for (String c : Coordinates) {

					question = MakeQuestion(l, c);
					answer = Ask(bs, question);
					if (answer) {
						// Add this to the KB.
						bs.add(question);

						// Add to safe tiles.
						safeTiles.add(c);
						if (DEBUG) System.out.println("Tile " + c + " is safe");
					}
				}
			}

*/


			if (DEBUG) System.out.println("Visited tiles: " + visitedTiles);
			if (DEBUG) System.out.println("Latest KB: " + bs);
			if (DEBUG) System.out.println("Latest safe tiles: " + safeTiles);



		} catch (IOException e) {
			e.printStackTrace();
		}




		/*
		max_x = null
		max_y = null
		if (bump) {
			// save the dimension we know now.
		}
		 */




		// Return a safe move with the lowest cost.
		return Action.FORWARD;

	}


	public String createDoubleImplication(String source, SearchAI.State state) {
		String implication = "";
		ArrayList<String> neighbors = getNeighbors(state);
		String symbol = "";
		switch(source)
		{
			case "B":
				symbol = "P";
				break;
			case "S":
				symbol = "W";
				break;
		}

		implication = source + state.positionX + state.positionY + " <=> (" + symbol + neighbors.get(0);

		for(String neighbor : neighbors.subList(1, neighbors.size())) {
			implication = implication + " || " + symbol + neighbor;
		}
		implication = implication + ")";


		System.out.println("Implication: " + implication);
		return implication;
	}

	public ArrayList<String> getNeighbors(SearchAI.State state) {

		ArrayList<String> neighbors = new ArrayList<>();

		if(state.positionX > 0) {
			neighbors.add("" + (state.positionX-1) + state.positionY);
		}
		if(state.positionY > 0) {
			neighbors.add("" + (state.positionX) + (state.positionY-1));
		}
		if(numRow == -1 || state.positionX < numRow) {
			neighbors.add("" + (state.positionX+1) + state.positionY);
		}
		if(numCol == -1 || state.positionX < numCol) {
			neighbors.add("" + (state.positionX) + (state.positionY+1));
		}

		return neighbors;
	}

	public World.Tile[][] createBoard() {
		World.Tile[][] board = new World.Tile[numRow][numCol];


		for ( int r = 0; r < rowDimension; ++r ) {
			for (int c = 0; c < colDimension; ++c) {
				board[c][r] = new board.Tile();

			}
		}

		return
	}
}