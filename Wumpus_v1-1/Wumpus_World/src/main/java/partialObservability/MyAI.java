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
			String Letter, XY;
			int X, Y;


			// Default knowledge ---------------------------------------------------------------------------------------
			XY = "11";
			safeTiles.add(XY);
			visitedTiles.add(XY);
			bs.add((PlFormula) new Negation(new Proposition ("P11"))); // r1


			// [1,1] ---------------------------------------------------------------------------------------------------
			X = 1; Y = 1;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");

			bs.add((PlFormula) new Negation(new Proposition ("B" + X + Y))); // r4
			bs.add(plParser.parseFormula("B11 <=> (P12 || P21)")); // r2
			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// TODO: Q1
			Letter = "P";
			XY = "12";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}

			// TODO: Q2
			Letter = "P";
			XY = "21";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}


			// [2,1] ---------------------------------------------------------------------------------------------------
			X = 2; Y = 1;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");

			bs.add((PlFormula) new Proposition("B" + X + Y)); // r5
			bs.add(plParser.parseFormula("B21 <=> (P11 || P22 || P31)")); // ??????????

			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// TODO: Q3
			Letter = "P";
			XY = "22";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}

			// TODO: Q4
			Letter = "P";
			XY = "31";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}


			// [1,2] ---------------------------------------------------------------------------------------------------
			X = 1; Y = 2;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");

			bs.add((PlFormula) new Negation(new Proposition ("B" + X + Y))); // r11
			bs.add(plParser.parseFormula("B12 <=> (P11 || P22 || P13)")); // r12
			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// TODO: Q5
			Letter = "P";
			XY = "22";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}

			// TODO: Q6
			Letter = "P";
			XY = "13";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}

///*

			// [2,2] ---------------------------------------------------------------------------------------------------
			X = 2; Y = 2;
			if (DEBUG) System.out.println("\n[" + X + "," + Y + "]");

			bs.add((PlFormula) new Negation(new Proposition ("B" + X + Y)));
			bs.add(plParser.parseFormula("B22 <=> (P12 || P23 || P32 || P21)"));
			if (DEBUG) System.out.println("KB in [" + X + "," + Y + "]: " + bs);

			// TODO: Q6
			Letter = "P";
			XY = "23";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}

			// TODO: Q7
			Letter = "P";
			XY = "32";

			question = MakeQuestion(Letter, XY);
			answer = Ask(bs, question);
			if (answer) {
				// Add this to the KB.
				bs.add(question);

				// Add to safe tiles.
				safeTiles.add(XY);
				if (DEBUG) System.out.println("Tile " + XY + " is safe");
			}

 //*/


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