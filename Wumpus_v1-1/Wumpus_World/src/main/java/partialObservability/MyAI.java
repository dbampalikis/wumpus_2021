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

	public Action getAction
	(
		boolean stench,
		boolean breeze,
		boolean glitter,
		boolean bump,
		boolean scream
	)
	{
/*
		//Manually create formulas and belief base
		PlBeliefSet beliefSet = new PlBeliefSet();
		Proposition f1 = new Proposition("a");
		Negation f2 = new Negation(f1);
		Conjunction c = new Conjunction();
		c.add(f1, f2, new Proposition("b"));
		Implication i = new Implication(f2, new Proposition("c"));
		beliefSet.add(f1,f2,c,i);
		System.out.println(beliefSet + "\n");

		//Parse belief base from string
		PlParser parser = new PlParser();
		beliefSet = parser.parseBeliefBase("a || b || c \n !a || b \n !b || c \n !c || (!a && !b && !c && !d)");
		System.out.println(beliefSet);

		//Parse belief base from file
		beliefSet = parser.parseBeliefBaseFromFile("src/main/resources/examplebeliefbase.proplogic");
		System.out.println(beliefSet);

		//Parse list of belief bases from file
		List<PlBeliefSet> beliefSets = parser.parseListOfBeliefBasesFromFile("src/main/resources/examplebeliefbase_multiple.proplogic");
		System.out.println(beliefSets);

		//Parse list of belief bases using a custom delimiter
		beliefSets = parser.parseListOfBeliefBases("a || b \n a && !a ##### c => d", "#####");
		System.out.println(beliefSets);

		//Note that belief bases can have signatures larger than their formulas' signature
		PlSignature sig = beliefSet.getSignature();
		sig.add(new Proposition("f"));
		beliefSet.setSignature(sig);
		System.out.println(beliefSet);
		System.out.println("Minimal signature: " + beliefSet.getMinimalSignature());
		//...but not smaller (commented out line throws exception)
		sig.remove(new Proposition("a"));
		//beliefSet2.setSignature(sig);

		//Use simple inference reasoner
		SimplePlReasoner reasoner = new SimplePlReasoner();
		PlFormula query = new Negation(new Proposition("a"));

		System.out.println("beliefSet: " + beliefSet);
		System.out.println("query" + query);
		Boolean answer1 = reasoner.query(beliefSet, query);
		System.out.println("answer1" + answer1);

*/

		/*
	    // KB: add the things we know.
		String[] axioms = {"P00", "W00", "G00"};
		PlBeliefSet bs = new PlBeliefSet();
		for (String axiom : axioms) {
			Proposition p = new Proposition(axiom);
			bs.add((PlFormula) p.complement());
		}

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

		System.out.println("bs: " + bs);

		System.out.println("Percepts before the move: " + stench + " " + breeze + " " + glitter + " " + bump + " " + scream);
		System.out.println("Map after the move:");

*/


		PlBeliefSet bs = new PlBeliefSet();
		PlParser plParser = new PlParser();

		// [1,1]
		bs.add((PlFormula) new Negation(new Proposition ("P11"))); // r1
		bs.add((PlFormula) new Negation(new Proposition ("B11"))); // r4


		try {
			// PlFormula f = plParser.parseFormula("(B11 => (P12 || P21)) && ((P12 || P21) => B11)"); // r2
			PlFormula f = plParser.parseFormula("B11 <=> (P12 || P21)"); // r2
			bs.add(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Should be enough to derive !P12.
		System.out.println(bs);

		AbstractPlReasoner r = new SimplePlReasoner();
		Proposition question = new  Proposition("P12");
		System.out.println("Is P12 true? " + r.query(bs, (PlFormula) question));
		System.out.println("Is !P12 true? " + r.query(bs, (PlFormula) question.complement()));




/*
		// [2,1]
		bs.add((PlFormula) new Proposition("B21")); // r5

		try {
			PlFormula f = plParser.parseFormula("B21 <=> (P11 || P22 || P31)"); // r3
			bs.add(f);
		} catch (IOException e) {
			e.printStackTrace();
		}



		// [1,2]
		bs.add((PlFormula) new Proposition("!B12")); // r11

		try {
			PlFormula f = plParser.parseFormula("B12 <=> (P11 || P22 || P13)"); // r12
			bs.add(f);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Should be enough to derive P22.

*/


		// Figure out safe moves.

		// Return a safe move with the lowest cost.
		return Action.FORWARD;

	}

}