package partialObservability;
import fullObservability.SearchAI;
import org.tweetyproject.logics.pl.reasoner.*;
import org.tweetyproject.logics.pl.sat.*;
import org.tweetyproject.logics.pl.syntax.*;
import org.tweetyproject.logics.pl.parser.*;
import wumpus.Agent;
import wumpus.World;

import java.io.IOException;
import java.util.*;

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
		if (DEBUG) System.out.println(r.query(bs, b)); // true
	}
*/

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
	LinkedList<Integer> fakeGoal = new LinkedList<Integer>(){{ add(0); add(0); }};
	String wumpusPosition = "";
	public LinkedList<Action> plan = new LinkedList<Action>();
	LinkedList<Action> tmpPlan = new LinkedList<Action>(); // Action plan to be returned by SearchAI.
	public static ArrayList<String> safeTiles = new ArrayList<String>(); // TODO: Do we need public static?
	public static ArrayList<String> wumpusTiles = new ArrayList<String>();
	ArrayList<String> visitedTiles = new ArrayList<String>();
	ArrayList<String> visitedTilesWithDuplicates = new ArrayList<String>();
	int numCol = -1;      // Real width of the world.
	int numRow = -1;      // Real height of the world.
	int maxCol = 10;       // Running width of the world.
	int maxRow = 10;       // Running height of the world.
	boolean DEBUG = true;
	PriorityQueue<Position> frontier = new PriorityQueue<>(positionComparator);
	PlBeliefSet bs = new PlBeliefSet();
	boolean checkAll = false;
	boolean checkedAll = false;
	boolean killWumpus = false;

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

	PlFormula generateWumpusDisjunction() {
		String s = "W00";
		// TODO: Replace 4 with 10.
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

	PlBeliefSet generateBsWithPairsOfWumpusDisjunctions() {
		PlBeliefSet tmpBs = new PlBeliefSet();
		PlParser plParser = new PlParser();

		// TODO: Replace 4 with 10.
		for (int i = 0; i < maxCol; i++) {
			for (int j = 0; j < maxRow; j++) {
				String s1 = "" + i + j;

				// TODO: Replace 4 with 10.
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


	public static Comparator<Position> positionComparator = new Comparator<>() {
		@Override
		public int compare(Position position1, Position position2) {
			return position1.tscore - position2.tscore;
		}
	};

	public SearchAI.State currentState = new SearchAI.State(0, 0, false, 0, 0, Integer.MAX_VALUE, true, true);
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

			// if (DEBUG) System.out.println();

			// Bump
			if (bump) {
				// Update the dimension - global maximum.
				switch(currentState.direction)
				// The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
				{
					case 0:
						currentState.positionX  = currentState.positionX - 1;
						maxCol = currentState.positionX + 1;
						break;
					case 3:
						currentState.positionY  = currentState.positionY - 1;
						maxRow = currentState.positionY + 1;
						break;
				}



				if (DEBUG) System.out.println("After BUMP: currentState.positionX = " + currentState.positionX);
				if (DEBUG) System.out.println("After BUMP: currentState.positionY = " + currentState.positionY);
				if (DEBUG) System.out.println("After BUMP: maxCol = " + maxCol);
				if (DEBUG) System.out.println("After BUMP: maxRow = " + maxRow);
			}


			if (DEBUG)  System.out.println("=============== New Round ===============");
			if (DEBUG) SearchAI.printState(currentState, "Current state");
			if (DEBUG)  System.out.println("Safe tiles:" + safeTiles);

			if (DEBUG) System.out.println("*** currentState.positionX = " + currentState.positionX);
			if (DEBUG) System.out.println("*** currentState.positionY = " + currentState.positionY);
			if (DEBUG) System.out.println("*** maxCol = " + maxCol);
			if (DEBUG) System.out.println("*** maxRow = " + maxRow);

			PlParser plParser = new PlParser();
			PlFormula question;
			boolean answer;
			String[] symbols;
			// ArrayList<String> neighbors;
			ArrayList<String> neighbors = new ArrayList<String>();


			// ----------------------------------------------------------------------------------
			// TELL(KB, MAKE-PERCEPT-SENTENCE(percept, t))
			// ----------------------------------------------------------------------------------

			// Define initial state
			if(!safeTiles.contains("00")) {
				safeTiles.add("00");
				// !P00 and !W00 are always true.
				bs.add((PlFormula) new Negation(new Proposition ("P00")));
				bs.add((PlFormula) new Negation(new Proposition ("W00")));
				// There is at leat one wumpus: disjunction of all Wxy.
				bs.add(generateWumpusDisjunction());
				// There is at most one wumpus: disjunction of !Wxy pairs.
				bs.addAll(generateBsWithPairsOfWumpusDisjunctions());
			}


			// Update visited tiles.
			if(!visitedTiles.contains("" + currentState.positionX + currentState.positionY)) {
				visitedTiles.add("" + currentState.positionX + currentState.positionY);
			}
			visitedTilesWithDuplicates.add("" + currentState.positionX + currentState.positionY);
			if (DEBUG)  System.out.println("\n[" + currentState.positionX + "," + currentState.positionY + "]");


			// Breeze
			Proposition p = new Proposition("B" + currentState.positionX + currentState.positionY);
			if (breeze) {
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}
			bs.add(plParser.parseFormula(createDoubleImplication("B", currentState)));
			//if (DEBUG) System.out.println("BS:" + bs);


			// Stench
			if(currentState.wumpus) {
				p = new Proposition("S" + currentState.positionX + currentState.positionY);
				if (stench) {
					bs.add(p);
				} else {
					bs.add((PlFormula) p.complement());
				}

				bs.add(plParser.parseFormula(createDoubleImplication("S", currentState)));
				// if (DEBUG)  System.out.println("BS:" + bs);
			}

			// Scream - Remove the wumpus from the knowledge base
			if (scream) {
				if (DEBUG) System.out.println("Cleanup is starting");
				//wumpusPosition = "";
				currentState.wumpus = false;
				safeTiles.add(wumpusPosition);
				bs.remove(new Proposition("W" + wumpusPosition));

				// There is at least one wumpus: disjunction of all Wxy.
				bs.remove(generateWumpusDisjunction());
				// There is at most one wumpus: disjunction of !Wxy pairs.
				bs.removeAll(generateBsWithPairsOfWumpusDisjunctions());


				ArrayList<PlFormula> remove = new ArrayList<>();
				if (DEBUG) System.out.println("Cleanup is starting2");
				for (PlFormula element : bs) {
					//if (DEBUG) System.out.println("Element: " + element);
					if(element.toString().contains("W")) {
						remove.add(element);

					}
				}
				for (PlFormula el : remove) {
					//if (DEBUG) System.out.println("Element to remove: " + el);
					bs.remove(el);
				}

				// Add negations for wumpus in every tile
				for (int i = 0; i < maxCol; i++) {
					for (int j = 0; j < maxRow; j++) {
						bs.add(plParser.parseFormula("!W" + i + j));
					}
				}
				if (DEBUG) System.out.println("Knowledge base after wumpus cleaning");
				//if (DEBUG) System.out.println(bs);

			}




			// ----------------------------------------------------------------------------------
			// safe <- {[x,y]: ASK(KB, OK) = true}
			// ----------------------------------------------------------------------------------

			// Questions - answers.
			symbols = new String[]{"P", "W", "!P", "!W"};


			if (checkAll) {
				// After finding wumpus - reconsider all tiles for safety.
				// if (DEBUG) System.out.println("in wumpusKnown");
				if (DEBUG) System.out.println("!!!!!! Checking all tiles for safety !!!!!!");
				// TODO: Replace 4 with 10.
				for (int i = 0; i < maxCol; i++) {
					for (int j = 0; j < maxRow; j++) {
						neighbors.add("" + i + j);
						checkAll = false;
						checkedAll = true;
					}
				}
			} else {
				// if (DEBUG) System.out.println("in NOT wumpusKnown");
				neighbors = getNeighbors(currentState);
			}

			for (String neighbor : neighbors) {
				for (String symbol : symbols) {

					answer = Ask(bs, symbol+neighbor);
					if (answer) {
						if (symbol.equals("W")) {
							if (DEBUG) System.out.println("!!! WUMPUS IS IN " + neighbor);
							// In next iteration - reconsider all tiles for safety.
							wumpusPosition = neighbor;
							if (!checkedAll) checkAll = true;
						}
						// Add this to the KB.
						bs.add(plParser.parseFormula(symbol+neighbor));
					}
				}
				answer = Ask(bs, "!P"+neighbor+"&&!W"+neighbor);
				if (answer) {
					// Add to safe tiles.
					if(!safeTiles.contains(neighbor)) {
						safeTiles.add(neighbor);
						if (DEBUG)  System.out.println("Tile " + neighbor + " is safe");
					}
				}
			}

			if (killWumpus && plan.size() == 0) {

				faceWumpus(currentState, plan);

				plan.add(Action.SHOOT);
				if (DEBUG) System.out.println("Wumpus killing: " + plan);
				currentState.arrow = false;
				killWumpus = false;
			}

			// Remove tiles outside of the world from the safe tiles.
			removeOutsideTiles(safeTiles);
			if (DEBUG) System.out.println("Safe tiles after remove: " + safeTiles);
			// ----------------------------------------------------------------------------------
			// if ASK(KB, Glitter) = true
			//     then plan ← [Grab] + PLAN-ROUTE(current,{[1,1]}, safe) + [Climb]
			// ----------------------------------------------------------------------------------

			//
			if(glitter) {
				plan.clear();

				tmpPlan = SearchAI.searchPath(currentState, fakeGoal, null, false, maxRow, maxCol, safeTiles);
				tmpPlan.addFirst(Action.GRAB);
				tmpPlan.add(Action.CLIMB);
				Position currentPosition = new Position(""+fakeGoal.get(0)+fakeGoal.get(1), tmpPlan.size(), tmpPlan);
				frontier.add(currentPosition);

			} else if(plan.size() == 0) {
				if(safeTiles.size() != visitedTiles.size()) {
					for (String tile : safeTiles) {
						// If the tile has not been visited
						if (!visitedTiles.contains(tile)) {
							tscore = Integer.MAX_VALUE;
							LinkedList<Integer> goalPosition = new LinkedList<>();
							goalPosition.add(Integer.parseInt(tile.substring(0, 1)));
							goalPosition.add(Integer.parseInt(tile.substring(1, 2)));
							SearchAI.printState(currentState, "Before search: ");
							if (DEBUG) System.out.println("Goal position in search: " + goalPosition);
							if (DEBUG) System.out.println("Max dimensions: " + maxRow + maxCol);
							tmpPlan = SearchAI.searchPath(currentState, goalPosition, null, false, maxRow, maxCol, safeTiles);
							if (DEBUG)  System.out.println("For tile " + tile + " the plan is " + tmpPlan);

							if (tmpPlan.size() > 0) {
								Position currentPosition = new Position(tile, tmpPlan.size(), tmpPlan);
								frontier.add(currentPosition);
								if (DEBUG)  System.out.println("Plan added");
							} else {
								if (DEBUG)  System.out.println("Plan NOT added");
							}


						}
					}
				} else if (currentState.arrow && !wumpusPosition.equals("") && Ask(bs, "!P" + wumpusPosition)) { // Case where the plan is to kill wumpus
//					if() {
//						if() {
							// TODO: Kill wumpus: Create list with tiles that have
							// the same row or column as wumpusPosition among the safe tiles
							// TODO:
							if (DEBUG) System.out.println("Decided to kill wumpus");
							wumpusKillingTiles(wumpusTiles);
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
				//			}
				//		}
					}
					// TODO: check which tiles have the same row number as the wumpus
					// TODO: check which tiles have the same column as the wumpus
					// Get plan cost for each of them
					// Select one with smallest and add Action.SHOOT

				} else {
					plan.clear();

					tmpPlan = SearchAI.searchPath(currentState, fakeGoal, null, false, maxRow, maxCol, safeTiles);
					tmpPlan.add(Action.CLIMB);
					Position currentPosition = new Position(""+fakeGoal.get(0)+fakeGoal.get(1), tmpPlan.size(), tmpPlan);
					frontier.add(currentPosition);
				}
			}



			// ----------------------------------------------------------------------------------
			// if plan is empty then ...
			// ----------------------------------------------------------------------------------
			/*if (plan.size() > 0) {
				// Plan is NOT empty - continue implementing the plan.
				return plan.pop();
			} else {
				// Plan is empty.
				if (!safeTiles.isEmpty()) {
					// There are tiles to explore.
					// TODO: SearchAI
				} else if (WeKnowWhereTheWumpusIs && WeWantToKillIt) {
					// There are no tiles to explore and we want to kill the wumpus.
					// TODO: Kill the wumpus
				} else {
					// Nothing to explore and we don't intend to kill the wumpus => climb out
					// TODO: SearchAI - plan climbing out.
				}
			}*/



			// ----------------------------------------------------------------------------------
			// if plan is empty and ASK(KB, HaveArrowt) = true then
			//     possible wumpus ← {[x, y] : ASK(KB,¬ Wx,y) = false}
			//     plan ← PLAN-SHOT(current, possible wumpus, safe)
			// ----------------------------------------------------------------------------------


			//if (DEBUG)  System.out.println("Latest KB: " + bs);
			if (DEBUG)  System.out.println("Path: " + visitedTilesWithDuplicates);
			if (DEBUG)  System.out.println("Unique visited tiles: " + visitedTiles);
			if (DEBUG)  System.out.println("Unique safe tiles: " + safeTiles);



		} catch (IOException e) {
			e.printStackTrace();
		}

		if(!frontier.isEmpty()) {
			plan = frontier.remove().plan;
			if (DEBUG)  System.out.println("Plan: " + plan);
			frontier.clear();
		}
		/*if(plan.size() == 0) {
			tmpPlan.clear();
			SearchAI.printState(currentState, "Final state: ");

			frontier.clear();
			fakeGoal.clear();
			fakeGoal.add(0);
			fakeGoal.add(0);
			tmpPlan = SearchAI.searchPath(currentState, fakeGoal, null, false, maxRow, maxCol, safeTiles);
			tmpPlan.add(Action.CLIMB);
			Position currentPosition = new Position(""+fakeGoal.get(0)+fakeGoal.get(1), tmpPlan.size(), tmpPlan);
			frontier.add(currentPosition);
			plan.clear();
			plan.addAll(tmpPlan);
			//plan.add(Action.CLIMB);
			//tmpPlan = SearchAI.searchPath(currentState, fakeGoal, null, false, maxRow, maxCol, safeTiles);
		}*/
		Action nextAction = plan.pop();


		currentState = SearchAI.getNextState(currentState, nextAction,null, fakeGoal, false, maxRow, maxCol, safeTiles);
		if (DEBUG) SearchAI.printState(currentState, "The new state");

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

	// Get tiles that the wumpus can be shot from
	private void wumpusKillingTiles(ArrayList<String> wumpusTiles) {

		for(int i=0; i<safeTiles.size(); i++) {
			if(Integer.parseInt(safeTiles.get(i).substring(0, 1)) == Integer.parseInt(wumpusPosition.substring(0,1))
				|| Integer.parseInt(safeTiles.get(i).substring(1, 2)) == Integer.parseInt(wumpusPosition.substring(1,2))) {
				wumpusTiles.add(safeTiles.get(i));
			}
		}

	}

	private void removeOutsideTiles(ArrayList<String> safeTiles) {
		if (DEBUG) System.out.println("About to remove tiles");

		ArrayList<Integer> toRemove = new ArrayList<>();
		if (DEBUG) System.out.println("Safe tiles in remove: " + safeTiles);
		for(int i=0; i<safeTiles.size(); i++) {
			if(Integer.parseInt(safeTiles.get(i).substring(0, 1)) >= maxCol || Integer.parseInt(safeTiles.get(i).substring(1, 2)) >= maxRow) {
				toRemove.add(i);
			}
		}
		Collections.sort(toRemove, Collections.reverseOrder());
		if (DEBUG) System.out.println(toRemove);

		for(int element : toRemove) {
			if (DEBUG) System.out.println(element);
			safeTiles.remove(element);
		}
		if (DEBUG) System.out.println("Removed tiles");
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


		// if (DEBUG)  System.out.println("Implication: " + implication);
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
		if(state.positionX < maxCol) {
			neighbors.add("" + (state.positionX+1) + state.positionY);
		}
		if(state.positionY < maxRow) {
			neighbors.add("" + (state.positionX) + (state.positionY+1));
		}

		return neighbors;
	}

}