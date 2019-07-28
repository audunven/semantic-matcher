package utilities;

import java.util.Comparator;

/**
 * Implements Comparator in order to sort relations for a matrix representation.
 * @author audunvennesland
 * @see alignmentcombination.ProfileWeight
 * @see alignmentcombination.ProfileWeightSubsumption
 */
public class RelationComparator implements Comparator<Relation> {


	@Override
	public int compare(Relation rel1, Relation rel2) {
		int result = rel1.getConcept1Fragment().compareTo(rel2.getConcept1Fragment());
		if (result != 0) {
			return result;
		} else {
			return rel1.getConcept2Fragment().compareTo(rel2.getConcept2Fragment());
		}
	}

}


