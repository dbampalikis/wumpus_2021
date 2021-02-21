package partialObservability;
import org.tweetyproject.logics.pl.reasoner.*;
import org.tweetyproject.logics.pl.sat.*;
import org.tweetyproject.logics.pl.syntax.*;
import org.tweetyproject.logics.pl.parser.*;
import wumpus.Agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

	boolean DEBUG = true;

	public PlFormula MakeQuestion(String Letter, String XY) {
		return new Negation(new Proposition(Letter + XY));
	}

	public boolean Ask(PlBeliefSet bs, PlFormula question) {

		// Solver setup.
		AbstractPlReasoner r = new SatReasoner();
		SatSolver.setDefaultSolver(new Sat4jSolver());

		// True if the second formula is entailed be the first formula.
		boolean answer = r.query(bs, question);
		if (DEBUG) System.out.println("Is " + question + " true? " + answer);

		return answer;
	}

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

			ArrayList<String> safeTiles = new ArrayList<String>();
			ArrayList<String> visitedTiles = new ArrayList<String>();
			PlBeliefSet bs = new PlBeliefSet();
			PlParser plParser = new PlParser();
			PlFormula question;
			boolean answer;
			String[] Letters;
			String[] Coordinates;
			int X, Y;


			// Default knowledge ---------------------------------------------------------------------------------------
			safeTiles.add("11");
			bs.add((PlFormula) new Negation(new Proposition ("P11")));


			// [1,1] ---------------------------------------------------------------------------------------------------
			X = 1; Y = 1;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");
			visitedTiles.add("" + X + Y);


			Proposition b = new Proposition("B" + X + Y);
			if (breeze) {
				bs.add(b);
			} else {
				bs.add((PlFormula) b.complement());
			}

			// Figure out here that we have 2 tiles to inspect: P12, P21.
			// Then add <=> to the KB.
			// Then check each tile with questions.

			bs.add(plParser.parseFormula("B11 <=> (P12 || P21)")); // Do be generated automatically.
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


			/*

			!!!!!!!!!!!! We should figure out here that 31 has a pit !!!!!!!!!!!!
			 Most probably we just need to ask both questions: Pxy and !Pxy.
			 Whichever returns `true` should be added to the KB.
			 It means that the if-statement `if (answer)` needs to be changed.

			*/

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

}