package partialObservability;
import fullObservability.SearchAI;
import org.tweetyproject.logics.pl.reasoner.*;
import org.tweetyproject.logics.pl.syntax.*;
import org.tweetyproject.logics.pl.parser.*;
import wumpus.Agent;

import java.io.IOException;

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

/*	public class State {
		int positionX;
		int positionY;
		boolean gold;
		int direction; // The direction the agent is facing: 0 - right, 1 - down, 2 - left, 3 - up
		int gscore;
		int tscore;
		boolean arrow;
		boolean wumpus;

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
*/
	public Action getAction
	(
		boolean stench,
		boolean breeze,
		boolean glitter,
		boolean bump,
		boolean scream
	)
	{
/*		// Define the initial state.
		State initialState;
		initialState = new State(0, 0, false, 0, 0, Integer.MAX_VALUE, true, true);
		initialState.tscore = initialState.gscore + 0; // No Manhattan for now.

		// KB: add the things we know.
		String[] axioms = {"P00", "W00", "G00"};
		PlBeliefSet bs = new PlBeliefSet();
		for (String axiom : axioms) {
			Proposition p = new Proposition(axiom);
			bs.add((PlFormula) p.complement());
		}
		System.out.println("know: " + bs);

		// KB: add the things we perceived.

		// Stench

		Proposition s = new Proposition("S00");
		Proposition w1 = new Proposition("W10");
		Proposition w2 = new Proposition("W01");
		if (stench) {
			bs.add((PlFormula) s); // stench in [0,0]
			bs.add((PlFormula) w1.combineWithOr(w2)); // wumpus in either [0,1] or [1,0]
		} else {
			bs.add((PlFormula) s.complement()); // // no stench in [0,0]
			bs.add((PlFormula) w1.complement().combineWithAnd(w2.complement())); // no wumpus in both [0,1] and [1,0]
		}

		Proposition b = new Proposition("B00");
		Proposition p1 = new Proposition("P10");
		Proposition p2 = new Proposition("P01");
		if (stench) {
			bs.add((PlFormula) b); // breeze in [0,0]
			bs.add((PlFormula) p1.combineWithOr(w2)); // pit in either [0,1] or [1,0]
		} else {
			bs.add((PlFormula) b.complement()); // // no breeze in [0,0]
			bs.add((PlFormula) p1.complement().combineWithAnd(p2.complement())); // no pit in both [0,1] and [1,0]
		}


		System.out.println("know and feel: " + bs);

		System.out.println("Percepts before the move: " + stench + " " + breeze + " " + glitter + " " + bump + " " + scream);
		System.out.println("Map after the move:");


*/

		PlBeliefSet bs = new PlBeliefSet();

		// in [0,0]
		Proposition p = new Proposition("B00");
		bs.add((PlFormula) p.complement());

		PlParser plParser = new PlParser();
		try {
			PlFormula f = plParser.parseFormula("!P00 && !P10 && !P01");
			bs.add(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// move right - in [1,0]
		p = new Proposition("B01");
		bs.add((PlFormula) p);

		try {
			PlFormula f = plParser.parseFormula("P20 || P11 || (P20 && P11)");
			bs.add(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// go back and move up - in [0,1]
		p = new Proposition("B10");
		bs.add((PlFormula) p.complement()); // no breeze in [0,1]

		try {
			PlFormula f = plParser.parseFormula("!P20 && !P11");
			bs.add(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(bs);

		// this should be enough to figure out that
		// there is a pit in [2,0] - p4
		// there is no pit in [1,1] - p5

		AbstractPlReasoner r = new SatReasoner();

		/*
		p = new Proposition("P11");
		System.out.println(r.query(bs, p)); // true
		*/


		// Figure out safe moves.

		// Return a safe move with the lowest cost.
		return Action.FORWARD;

	}

}