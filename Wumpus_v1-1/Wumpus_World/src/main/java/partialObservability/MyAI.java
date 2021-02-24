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
		System.out.println(r.query(bs, b)); // true
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
	public LinkedList<Action> plan = new LinkedList<Action>();
	LinkedList<Action> tmpPlan = new LinkedList<Action>(); // Action plan to be returned by SearchAI.
	public static ArrayList<String> safeTiles = new ArrayList<String>(); // TODO: Do we need public static?
	ArrayList<String> visitedTiles = new ArrayList<String>();
	int numCol = -1;      // Real width of the world.
	int numRow = -1;      // Real height of the world.
	int maxCol = 10;       // Running width of the world.
	int maxRow = 10;       // Running height of the world.
	boolean DEBUG = true;
	PriorityQueue<Position> frontier = new PriorityQueue<>(positionComparator);
	PlBeliefSet bs = new PlBeliefSet();

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
				System.out.println("After BUMP: currentState.positionX = " + currentState.positionX);
				System.out.println("After BUMP: currentState.positionY = " + currentState.positionY);
				System.out.println("After BUMP: maxCol = " + maxCol);
				System.out.println("After BUMP: maxRow = " + maxRow);
			}


			if (DEBUG) System.out.println("=============== New Round ===============");
			if (DEBUG) SearchAI.printState(currentState, "Current state");
			if (DEBUG) System.out.println("Safe tiles:" + safeTiles);

			System.out.println("*** currentState.positionX = " + currentState.positionX);
			System.out.println("*** currentState.positionY = " + currentState.positionY);
			System.out.println("*** maxCol = " + maxCol);
			System.out.println("*** maxRow = " + maxRow);

			PlParser plParser = new PlParser();
			PlFormula question;
			boolean answer;
			String[] symbols;
			ArrayList<String> neighbors;


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
				String s = "W00";
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						if (!(i == 0 && j == 0)) {
							s = s + "||W" + i + j;
						}
					}
				}
				PlFormula wumpusDisjunction = plParser.parseFormula(s);
				bs.add(wumpusDisjunction);
				// There is at most one wumpus: disjunction of !Wxy pairs.
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						String s1 = "" + i + j;

						for (int n = 0; n < 4; n++) {
							for (int m = 0; m < 4; m++) {
								String s2 = "" + n + m;
								if (!s1.equals(s2)) {
									bs.add(plParser.parseFormula("!W" + s1 + " || !W" + s2));
								}
							}
						}
					}
				}
			}


			// Update visited tiles.
			if(!visitedTiles.contains("" + currentState.positionX + currentState.positionY)) {
				visitedTiles.add("" + currentState.positionX + currentState.positionY);
			}
			if (DEBUG) System.out.println("\n[" + currentState.positionX + "," + currentState.positionY + "]");

			System.out.println("*** currentState.positionX = " + currentState.positionX);
			System.out.println("*** currentState.positionY = " + currentState.positionY);

			// Breeze
			Proposition p = new Proposition("B" + currentState.positionX + currentState.positionY);
			if (breeze) {
				System.out.println("!!! BREEZE");
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}
			bs.add(plParser.parseFormula(createDoubleImplication("B", currentState)));
			if (DEBUG) System.out.println("BS:" + bs);


			// Stench
			p = new Proposition("S" + currentState.positionX + currentState.positionY);
			if (stench) {
				System.out.println("!!! STENCH");
				bs.add(p);
			} else {
				bs.add((PlFormula) p.complement());
			}
			bs.add(plParser.parseFormula(createDoubleImplication("S", currentState)));
			// if (DEBUG) System.out.println("BS:" + bs);


			// Scream
			// TODO: Replace Wxy with !Wxy because it is dead now.
			if (scream) {
				System.out.println("!!! SCREAM");
				// Remove Wxy.
				// Add !Wxy.
			}




			// ----------------------------------------------------------------------------------
			// safe <- {[x,y]: ASK(KB, OK) = true}
			// ----------------------------------------------------------------------------------

			// Questions - answers.
			symbols = new String[]{"P", "W", "!P", "!W"};
			neighbors = getNeighbors(currentState);

			for (String neighbor : neighbors) {
				for (String symbol : symbols) {

					answer = Ask(bs, symbol+neighbor);
					if (answer) {
						if (symbol.equals("W")) {
							System.out.println("!!! WUMPUS IS IN " + neighbor);
							// TODO: Reconsider Wumpus' neighbours for safety.
							// Create a global variable wumpusNeighbors - initially empty.
							// In the code above replace with :
							// neighbors = getNeighbors(currentState) + wumpusNeighbors;
							// After we find wumpus - add its neighbors to wumpusNeighbors
							// so that those tiles are reconsidered for safety.

//							SearchAI.State wumpusState = new SearchAI.State(Integer.parseInt(neighbor.substring(0,1)), Integer.parseInt(neighbor.substring(1,2)), false, 0, 0, Integer.MAX_VALUE, true, true);
//							ArrayList<String> wumpusNeighbors = getNeighbors(wumpusState);
//							System.out.println("!!! WUMPUS NEIGHBOURS: " + wumpusNeighbors);

							// TODO: Mark all non-Wumpus tiles as !Wxy. - probably unnecessary.



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
						if (DEBUG) System.out.println("Tile " + neighbor + " is safe");
					}
				}
			}



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
							tmpPlan = SearchAI.searchPath(currentState, goalPosition, null, false, maxRow, maxCol, safeTiles);
							if (DEBUG) System.out.println("For tile " + tile + " the plan is " + tmpPlan);

							if (tmpPlan.size() > 0) {
								Position currentPosition = new Position(tile, tmpPlan.size(), tmpPlan);
								frontier.add(currentPosition);
								if (DEBUG) System.out.println("Plan added");
							} else {
								if (DEBUG) System.out.println("Plan NOT added");
							}


						}
					}
				} /*else if (currentState.arrow) { // Case where the plan is to kill wumpus
					wumpusStr = getWumpusPosition();
					// TODO: check which tiles have the same row number as the wumpus
					// TODO: check which tiles have the same column as the wumpus
					// Get plan cost for each of them
					// Select one with smallest and add Action.SHOOT

				}*/ else {
					System.out.println("CRUSHING HERE???????????");
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


			if (DEBUG) System.out.println("Visited tiles: " + visitedTiles);
			if (DEBUG) System.out.println("Latest KB: " + bs);
			if (DEBUG) System.out.println("Latest safe tiles: " + safeTiles);



		} catch (IOException e) {
			e.printStackTrace();
		}

		if(!frontier.isEmpty()) {
			plan = frontier.remove().plan;
			if (DEBUG) System.out.println("Plan: " + plan);
			frontier.clear();
		}
		Action nextAction = plan.pop();

		currentState = SearchAI.getNextState(currentState, nextAction,null, fakeGoal, false, maxRow, maxCol, safeTiles);
		if (DEBUG) SearchAI.printState(currentState, "The new state");

		// Return a safe move with the lowest cost.
		return nextAction;

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


		if (DEBUG) System.out.println("Implication: " + implication);
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
		if(state.positionX < maxRow) {
			neighbors.add("" + (state.positionX+1) + state.positionY);
		}
		if(state.positionX < maxCol) {
			neighbors.add("" + (state.positionX) + (state.positionY+1));
		}

		return neighbors;
	}

}