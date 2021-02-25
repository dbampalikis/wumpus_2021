package partialObservability;
import fullObservability.SearchAI;
import org.tweetyproject.logics.pl.reasoner.*;
import org.tweetyproject.logics.pl.sat.*;
import org.tweetyproject.logics.pl.syntax.*;
import org.tweetyproject.logics.pl.parser.*;
import wumpus.Agent;

import java.io.IOException;
import java.util.*;

public class MyAI extends Agent
{

	public MyAI ( )
	{

	}

	public static int tscore;
	public static class Position {
		String tile;
		public int tscore;
		LinkedList<Action> plan;

		public Position(String tile, int tscore, LinkedList<Action> plan) {
			this.tile = tile;
			this.plan = plan;
			this.tscore = tscore;
		}
	}
	LinkedList<Integer> fakeGoal = new LinkedList<Integer>(){{ add(0); add(0); }}; // Goals passed to SearchAI.
	String wumpusPosition = ""; // Updated with the actual XY position of the Wumpus once becomes known.
	public LinkedList<Action> plan = new LinkedList<Action>(); // Action plan to be executed.
	LinkedList<Action> tmpPlan = new LinkedList<Action>(); // Intermediate action plan to be returned by SearchAI.
	public ArrayList<String> safeTiles = new ArrayList<String>(); // Tiles marked as safe (i.e., !Pxy && !Wxy)
	public ArrayList<String> wumpusTiles = new ArrayList<String>();
	ArrayList<String> visitedTiles = new ArrayList<String>(); // Unique visited tiles.
	PriorityQueue<Position> frontier = new PriorityQueue<>(positionComparator); // Container for alternative plans.
	PlBeliefSet bs = new PlBeliefSet(); // Knowledge base.
	public SearchAI.State currentState = new SearchAI.State(0, 0, false, 0, 0, Integer.MAX_VALUE, true, true); // Current state of the agent.

	int maxCol = 10;            // Initial guess about the X-dimension of the world.
	int maxRow = 10;            // Initial guess about the Y-dimension of the world.

	boolean checkAll = false;   // Do a check of all tiles for safety? (i.e., !Pxy && !Wxy)
	boolean checkedAll = false; // Was the check already implemented?
	boolean killWumpus = false; // Is agent on a mission to kill the Wumpus?

	boolean DEBUG = false;  // Print debug messages?
	ArrayList<String> visitedTilesWithDuplicates = new ArrayList<String>(); // For debug - visited tiles as-is.


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

			PlParser plParser = new PlParser(); // For parsing formulas.
			PlFormula question;                 // Question - Is this formula entailed by the KB?
			boolean answer;                     // Answer   - Is this formula entailed by the KB?
			String[] symbols;                   // String to formulate the `question`.
			ArrayList<String> neighbors = new ArrayList<String>(); // Neigboring tiles of the agent.


			// Bump
			if (bump) {
				// Update X or Y dimension.
				switch(currentState.direction)
				// The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
				{
					case 0:
						// Correct the current X position.
						currentState.positionX  = currentState.positionX - 1;
						// Update X dimension of the world.
						maxCol = currentState.positionX + 1;
						break;
					case 3:
						// Correct the current Y position.
						currentState.positionY  = currentState.positionY - 1;
						// Update Y dimension of the world.
						maxRow = currentState.positionY + 1;
						break;
				}

				if (DEBUG) System.out.println("After BUMP: X = " + currentState.positionX +
						", Y = " + currentState.positionY +
						", maxCol = " + maxCol +
						", maxRow = " + maxRow);
			}


			if (DEBUG)  System.out.println("=============== [" +
					currentState.positionX + "," + currentState.positionY +
					"] ===============");
			if (DEBUG) SearchAI.printState(currentState, "Current state");
			if (DEBUG) System.out.println("Safe tiles in the beginning of the turn: " + safeTiles);
			if (DEBUG) System.out.println("Dimensions in the beginning of the turn: maxCol = " + maxCol + ", maxRow = " + maxRow);



			// ----------------------------------------------------------------------------------
			// Add the initial knowledge to the KB.
			// ----------------------------------------------------------------------------------

			if(!safeTiles.contains("00")) {
				safeTiles.add("00");
				// !P00 and !W00 are always true.
				bs.add((PlFormula) new Negation(new Proposition ("P00")));
				bs.add((PlFormula) new Negation(new Proposition ("W00")));
				// There is at least one wumpus: disjunction of all Wxy.
				bs.add(generateWumpusDisjunction());
				// There is at most one wumpus: disjunction of !Wxy pairs.
				bs.addAll(generateBsWithPairsOfWumpusDisjunctions());
			}


			// Update visited tiles.
			if(!visitedTiles.contains("" + currentState.positionX + currentState.positionY)) {
				visitedTiles.add("" + currentState.positionX + currentState.positionY);
			}
			visitedTilesWithDuplicates.add("" + currentState.positionX + currentState.positionY);


			// Breeze
			Proposition p = new Proposition("B" + currentState.positionX + currentState.positionY);
			if (breeze) {
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}
			bs.add(plParser.parseFormula(createDoubleImplication("B", currentState)));

			// Stench
			if(currentState.wumpus) {
				p = new Proposition("S" + currentState.positionX + currentState.positionY);
				if (stench) {
					bs.add(p);
				} else {
					bs.add((PlFormula) p.complement());
				}

				bs.add(plParser.parseFormula(createDoubleImplication("S", currentState)));
			}

			// Scream - replace all Wumpus-related knowledge with !Wxy for all tiles.
			if (scream) {
				if (DEBUG) System.out.println("Wumpus is dead. Starting KB cleanup...");
				currentState.wumpus = false;
				safeTiles.add(wumpusPosition);
				bs.remove(new Proposition("W" + wumpusPosition));

				// There is at least one wumpus: disjunction of all Wxy.
				bs.remove(generateWumpusDisjunction());
				// There is at most one wumpus: disjunction of !Wxy pairs.
				bs.removeAll(generateBsWithPairsOfWumpusDisjunctions());

				// Remove all the rest.
				ArrayList<PlFormula> remove = new ArrayList<>();
				for (PlFormula element : bs) {
					if(element.toString().contains("W")) {
						remove.add(element);
					}
				}
				for (PlFormula el : remove) {
					bs.remove(el);
				}

				// Add !Wxy for all tiles.
				for (int i = 0; i < maxCol; i++) {
					for (int j = 0; j < maxRow; j++) {
						bs.add(plParser.parseFormula("!W" + i + j));
					}
				}
				if (DEBUG) System.out.println("Clean KB after killing the Wumpus: " + bs);
			}



			// ----------------------------------------------------------------------------------
			// Update KB and safe tiles.
			// ----------------------------------------------------------------------------------

			// Questions - answers.
			symbols = new String[]{"P", "W", "!P", "!W"};

			if (checkAll) {
				// After finding the Wumpus - consider all tiles.
				if (DEBUG) System.out.println("Checking all tiles for safety...");
				for (int i = 0; i < maxCol; i++) {
					for (int j = 0; j < maxRow; j++) {
						neighbors.add("" + i + j);
						checkAll = false;
						checkedAll = true;
					}
				}
			} else {
				// Consider only the neighboring tiles.
				neighbors = getNeighbors(currentState);
			}

			// Check if formulas are entailed by the KB.
			for (String neighbor : neighbors) {
				for (String symbol : symbols) {

					answer = Ask(bs, symbol+neighbor);
					if (answer) {
						if (symbol.equals("W")) {
							if (DEBUG) System.out.println("!!! FOUND THE WUMPUS IN " + neighbor);
							// Registed the position of the Wumpus.
							wumpusPosition = neighbor;
							// In next iteration - reconsider all tiles for safety.
							if (!checkedAll) checkAll = true;
						}
						// Add this to the KB.
						bs.add(plParser.parseFormula(symbol+neighbor));
					}
				}

				// Check if a tile is safe.
				answer = Ask(bs, "!P"+neighbor+"&&!W"+neighbor);
				if (answer) {
					// Add to safe tiles.
					if(!safeTiles.contains(neighbor)) {
						safeTiles.add(neighbor);
						if (DEBUG)  System.out.println("Tile " + neighbor + " is safe.");
					}
				}
			}



			// ----------------------------------------------------------------------------------
			// Plans and actions
			// ----------------------------------------------------------------------------------

			// Add to the plan: turn around to face the Wumpus and shoot.
			if (killWumpus && plan.size() == 0) {
				// Plan to turn around.
				faceWumpus(currentState, plan);
				plan.add(Action.SHOOT);
				if (DEBUG) System.out.println("Wumpus killing plan: " + plan);
				currentState.arrow = false;
				killWumpus = false;
			}

			// Remove tiles outside of the world from the safe tiles.
			removeOutsideTiles(safeTiles);
			if (DEBUG) System.out.println("Safe tiles after remove: " + safeTiles + "\n");

			// Create a plan to grab the gold and climb out.
			if(glitter) {
				plan.clear();

				tmpPlan = SearchAI.searchPath(currentState, fakeGoal, null, false, maxRow, maxCol, safeTiles);
				tmpPlan.addFirst(Action.GRAB);
				tmpPlan.add(Action.CLIMB);
				Position currentPosition = new Position(""+fakeGoal.get(0)+fakeGoal.get(1), tmpPlan.size(), tmpPlan);
				frontier.add(currentPosition);

			} else if(plan.size() == 0) {
				// Plan is empty.
				if(safeTiles.size() != visitedTiles.size()) {
					// There are tiles to explore.
					for (String tile : safeTiles) {
						// If the tile has not been visited
						if (!visitedTiles.contains(tile)) {
							tscore = Integer.MAX_VALUE;
							LinkedList<Integer> goalPosition = new LinkedList<>();
							goalPosition.add(Integer.parseInt(tile.substring(0, 1)));
							goalPosition.add(Integer.parseInt(tile.substring(1, 2)));
							if (DEBUG) SearchAI.printState(currentState, "Before search: ");
							if (DEBUG) System.out.println("Goal position in search: " + goalPosition);
							// if (DEBUG) System.out.println("Max dimensions: " + maxRow + maxCol);
							tmpPlan = SearchAI.searchPath(currentState, goalPosition, null, false, maxRow, maxCol, safeTiles);
							if (DEBUG) System.out.println("For tile " + tile + " the plan is " + tmpPlan);

							if (tmpPlan.size() > 0) {
								Position currentPosition = new Position(tile, tmpPlan.size(), tmpPlan);
								frontier.add(currentPosition);
								if (DEBUG) System.out.println("Plan added\n");
							} else {
								if (DEBUG) System.out.println("Plan is empty, so it is NOT added\n");
							}
						}
					}
				} else if (currentState.arrow && !wumpusPosition.equals("") && Ask(bs, "!P" + wumpusPosition)) {
					// Kill the Wumpus! (Agent has an arrow  + Wumpus' location is known + Ther eis no pit there)

					if (DEBUG) System.out.println("Decided to kill the Wumpus.");
					wumpusKillingTiles(wumpusTiles, currentState);
					for (String tile : wumpusTiles) {
						tscore = Integer.MAX_VALUE;
						LinkedList<Integer> goalPosition = new LinkedList<>();
						goalPosition.add(Integer.parseInt(tile.substring(0, 1)));
						goalPosition.add(Integer.parseInt(tile.substring(1, 2)));
						tmpPlan = SearchAI.searchPath(currentState, goalPosition, null, false, maxRow, maxCol, safeTiles);
						if (DEBUG)  System.out.println("For tile " + tile + " the plan is " + tmpPlan);
						killWumpus = true;

						Position currentPosition = new Position(tile, tmpPlan.size(), tmpPlan);
						frontier.add(currentPosition);
						if (DEBUG)  System.out.println("Plan added");
					}

				} else {
					if (DEBUG) System.out.println("Decided to go home.");
					plan.clear();

					tmpPlan = SearchAI.searchPath(currentState, fakeGoal, null, false, maxRow, maxCol, safeTiles);
					tmpPlan.add(Action.CLIMB);
					Position currentPosition = new Position(""+fakeGoal.get(0)+fakeGoal.get(1), tmpPlan.size(), tmpPlan);
					frontier.add(currentPosition);
				}
			}


			// if (DEBUG)  System.out.println("Latest KB: " + bs);
			if (DEBUG)  System.out.println("Path: " + visitedTilesWithDuplicates);
			if (DEBUG)  System.out.println("Unique visited tiles: " + visitedTiles);
			if (DEBUG)  System.out.println("Unique safe tiles: " + safeTiles);


		} catch (IOException e) {
			e.printStackTrace();
		}

		// Pick the cheapest plan from the frontier.
		if(!frontier.isEmpty()) {
			plan = frontier.remove().plan;
			if (DEBUG)  System.out.println("\nA NEW plan: " + plan);
			frontier.clear();
		}


		// Pick the next action from the current plan.
		if (DEBUG) System.out.println("The remaining plan: " + plan + "\n");
		Action nextAction = plan.pop();

		// Updat the current state.
		currentState = SearchAI.getNextState(currentState, nextAction,null, fakeGoal, false, maxRow, maxCol, safeTiles);
		// if (DEBUG) SearchAI.printState(currentState, "Expected new state");

		// Return a safe move with the lowest cost.
		return nextAction;

	}



	// The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
	private void faceWumpus(SearchAI.State currentState, LinkedList<Action> tmpPlan) {
		if(currentState.positionX == Integer.parseInt(wumpusPosition.substring(0,1))) {
			// Shoot wumpus from the top
			if(currentState.positionY > Integer.parseInt(wumpusPosition.substring(1,2))) {
				if(currentState.direction == 0) {
					tmpPlan.add(Action.TURN_RIGHT);
				} else if(currentState.direction == 2) {
					tmpPlan.add(Action.TURN_LEFT);
				} else if(currentState.direction == 3) {
					tmpPlan.add(Action.TURN_LEFT);
					tmpPlan.add(Action.TURN_LEFT);
				}
			} else {
				// Shoot wumpus from below
				if(currentState.direction == 0) {
					tmpPlan.add(Action.TURN_LEFT);
				} else if(currentState.direction == 2) {
					tmpPlan.add(Action.TURN_RIGHT);
				} else if(currentState.direction == 1) {
					tmpPlan.add(Action.TURN_LEFT);
					tmpPlan.add(Action.TURN_LEFT);
				}
			}
		} else if ((currentState.positionY == Integer.parseInt(wumpusPosition.substring(1,2)))) {
			// Shoot wumpus from it's right (should face to the left)
			if(currentState.positionX > Integer.parseInt(wumpusPosition.substring(0,1))) {
				if(currentState.direction == 0) {
					tmpPlan.add(Action.TURN_LEFT);
					tmpPlan.add(Action.TURN_LEFT);
				} else if(currentState.direction == 1) {
					tmpPlan.add(Action.TURN_RIGHT);
				} else if(currentState.direction == 3) {
					tmpPlan.add(Action.TURN_LEFT);
				}
			} else {
				// Shoot wumpus from it's left (should face to the right)
				if(currentState.direction == 2) {
					tmpPlan.add(Action.TURN_LEFT);
					tmpPlan.add(Action.TURN_LEFT);
				} else if(currentState.direction == 1) {
					tmpPlan.add(Action.TURN_LEFT);
				} else if(currentState.direction == 3) {
					tmpPlan.add(Action.TURN_RIGHT);
				}
			}
		}
	}

	// Get tiles that the wumpus can be shoot from.
	private void wumpusKillingTiles(ArrayList<String> wumpusTiles, SearchAI.State currentState) {



		for(int i=0; i<safeTiles.size(); i++) {
			if(Integer.parseInt(safeTiles.get(i).substring(0, 1)) == Integer.parseInt(wumpusPosition.substring(0,1))
				|| Integer.parseInt(safeTiles.get(i).substring(1, 2)) == Integer.parseInt(wumpusPosition.substring(1,2))) {

				if (!(currentState.positionX == Integer.parseInt(safeTiles.get(i).substring(0, 1))
						&& currentState.positionY == Integer.parseInt(safeTiles.get(i).substring(1, 2)))) {
					wumpusTiles.add(safeTiles.get(i));
				}


			}
		}

	}

	// Remove tiles that are outside the known world.
	private void removeOutsideTiles(ArrayList<String> safeTiles) {

		ArrayList<Integer> toRemove = new ArrayList<>();
		if (DEBUG) System.out.println("Safe tiles before remove: " + safeTiles);
		for(int i=0; i<safeTiles.size(); i++) {
			if(Integer.parseInt(safeTiles.get(i).substring(0, 1)) >= maxCol || Integer.parseInt(safeTiles.get(i).substring(1, 2)) >= maxRow) {
				toRemove.add(i);
			}
		}
		Collections.sort(toRemove, Collections.reverseOrder());
		// if (DEBUG) System.out.println("Indices of tiles to remove: " + toRemove);

		for(int element : toRemove) {
			// if (DEBUG) System.out.println("Removing: " + element);
			safeTiles.remove(element);
		}
	}

	// Create double implications about Breeze-Pit and Stench-Wumpus.
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
		return implication;
	}

	// Get neighboring tiles for the current position.
	public ArrayList<String> getNeighbors(SearchAI.State state) {

		ArrayList<String> neighbors = new ArrayList<>();

		if(state.positionX > 0) {
			neighbors.add("" + (state.positionX-1) + state.positionY);
		}
		if(state.positionY > 0) {
			neighbors.add("" + (state.positionX) + (state.positionY-1));
		}
		if(state.positionX < maxCol) {
			neighbors.add("" + (state.positionX+1) + state.positionY);
		}
		if(state.positionY < maxRow) {
			neighbors.add("" + (state.positionX) + (state.positionY+1));
		}

		return neighbors;
	}

	// Does KB entail a given formula?
	public boolean Ask(PlBeliefSet bs, String symbol) {

		// Solver setup.
		AbstractPlReasoner r = new SatReasoner();
		SatSolver.setDefaultSolver(new Sat4jSolver());

		PlParser plParser = new PlParser();
		boolean answer = false;

		try {
			PlFormula f = plParser.parseFormula(symbol);
			answer = r.query(bs, f);
			// if (DEBUG)  System.out.println("Is " + f + " true? " + answer);
		} catch (IOException e) {
			System.out.println(e.getStackTrace());
		}

		return answer;
	}

	// There is AT LEAST ONE Wumpus.
	PlFormula generateWumpusDisjunction() {
		String s = "W00";
		for (int i = 0; i < maxCol; i++) {
			for (int j = 0; j < maxRow; j++) {
				if (!(i == 0 && j == 0)) {
					s = s + "||W" + i + j;
				}
			}
		}
		PlParser plParser = new PlParser();
		PlFormula wumpusDisjunction = null;
		try {
			wumpusDisjunction = plParser.parseFormula(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wumpusDisjunction;
	}

	// There is AT MOST ONE Wumpus.
	PlBeliefSet generateBsWithPairsOfWumpusDisjunctions() {
		PlBeliefSet tmpBs = new PlBeliefSet();
		PlParser plParser = new PlParser();

		for (int i = 0; i < maxCol; i++) {
			for (int j = 0; j < maxRow; j++) {
				String s1 = "" + i + j;

				for (int n = 0; n < maxCol; n++) {
					for (int m = 0; m < maxRow; m++) {
						String s2 = "" + n + m;
						if (!s1.equals(s2)) {
							try {
								tmpBs.add(plParser.parseFormula("!W" + s1 + " || !W" + s2));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}

		return tmpBs;
	}

    // Comparator for the plan priority queue.
	public static Comparator<Position> positionComparator = new Comparator<>() {
		@Override
		public int compare(Position position1, Position position2) {
			return position1.tscore - position2.tscore;
		}
	};

}