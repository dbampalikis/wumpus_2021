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

		PlBeliefSet bs = new PlBeliefSet();
		PlParser plParser = new PlParser();

		// This loop adds propositions to the knowledge base
		// about that there is no Wumpus or Pits outside of the map
		// This will help the reasoner when reasoning near the borders
		// of the map.
		// This loop should only be run once and that is when the game start
		for (int i = 0; i < 10; i++) {
			String no_wumpus_outside_map = "Wz" + Integer.toString(i);
			String no_pit_outside_map = "Pz" + Integer.toString(i);
			bs.add((PlFormula) new Negation(new Proposition (no_wumpus_outside_map)));
			bs.add((PlFormula) new Negation(new Proposition (no_pit_outside_map)));
			no_wumpus_outside_map = "W" + Integer.toString(i) + "z";
			no_pit_outside_map = "P" + Integer.toString(i) + "z";
			bs.add((PlFormula) new Negation(new Proposition (no_wumpus_outside_map)));
			bs.add((PlFormula) new Negation(new Proposition (no_pit_outside_map)));
		};



		/*
		When we are in a new square we can use the code below
		to express what we experience (stench, breeze, glitter)
		as string propositions that Tweety can read.
		E.g: If we enter square 2,3 and we experience breeze,
		no stench and no glitter, the propositions
		"B23", "!S23", "!G23" are created inside the strings
		new_breeze_proposition, new_stench_proposition and
		new_glitter_proposition. These strings can be passed
		to the Tweety knowledge base.
		*/

		/* Create new strings */
		String new_breeze_proposition;
		String new_stench_proposition;
		String new_glitter_proposition;

		/* Collect position,
		* switch which line is
		*  muted below after importing
		*  the state class */

		// String position_string = Integer.toString(state.positionX) + Integer.toString(state.positionY);
		String position_string = Integer.toString(0) + Integer.toString(5);

		// We temporarerly set breeze and stench to TRUE to test if we can build a
		// logical statement:
		breeze = true;
		stench = true;

		/* Create string propositions */
		if (breeze) {
			new_breeze_proposition = "B" + position_string;
		} else {
			new_breeze_proposition = "!B" + position_string;
		};
		bs.add((PlFormula) new Proposition (new_breeze_proposition));

		if (stench) {
			new_stench_proposition = "S" + position_string;
		} else {
			new_stench_proposition = "!S" + position_string;
		};
		bs.add((PlFormula) new Proposition (new_stench_proposition));

		if (glitter) {
			new_glitter_proposition = "G" + position_string;
		} else {
			new_glitter_proposition = "!G" + position_string;
		};
		bs.add((PlFormula) new Proposition (new_glitter_proposition));

		// Construct a new LOGICAL FORMULA BASED ON NEW POSITION
		// WE ARE ON POSITION XY, stored in the string position_string
		// if the String new_breeze_proposition contains: "B13"

		// We want to express the following relationship, but given
		// position XY instead of position 13
		// "B13 <=> (P13 || P10 || P03 || P23)"

		// Assuming that we have not been on this position before:
		// Create new logical sentences if we experience breeze or stench
		// These sentences are then added to our knowledge base
		if (breeze | stench) {

			int i = Integer.parseInt(position_string);
			int x_coordinate = Integer.parseInt(position_string.substring(0,1));
			int y_coordinate = Integer.parseInt(position_string.substring(1));
			int x1 = x_coordinate - 1;
			int x2 = x_coordinate + 1;
			int y1 = y_coordinate - 1;
			int y2 = y_coordinate + 1;
			String X1 = Integer.toString(x1);
			String X2 = Integer.toString(x2);
			String Y1 = Integer.toString(y1);
			String Y2 = Integer.toString(y2);
			if (x1 < 0) {
				X1 = "z";

			};
			if (y1 < 0) {
				Y1 = "z";
			};

			if (breeze) {
				String P1 = "P" + x_coordinate + Y1;
				String P2 = "P" + x_coordinate + Y2;
				String P3 = "P" + X1 + y_coordinate;
				String P4 = "P" + X2 + y_coordinate;
				String new_breeze_sentence = new_breeze_proposition + " <=> ( " + P1 + " || " + P2 + " || " + P3 + " || " + P4 + " )";
				bs.add((PlFormula) new Proposition (new_breeze_sentence));
			};

			if (stench) {
				String W1 = "W" + x_coordinate + Y1;
				String W2 = "W" + x_coordinate + Y2;
				String W3 = "W" + X1 + y_coordinate;
				String W4 = "W" + X2 + y_coordinate;
				String new_stench_sentence = new_stench_proposition + " <=> ( " + W1 + " || " + W2 + " || " + W3 + " || " + W4 + " )";
				bs.add((PlFormula) new Proposition (new_stench_sentence));
			};
		}






		/* I have not changed anything below this point /Johan */














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

		//AbstractPlReasoner r = new SimplePlReasoner();
		//Proposition question = new  Proposition("P12");
		//System.out.println("Is P12 true? " + r.query(bs, (PlFormula) question));
		//System.out.println("Is !P12 true? " + r.query(bs, (PlFormula) question.complement()));




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
