package utilities;

import java.util.Set;

/**
 * Different variants of the Jaccard set similarity
 * @author audunvennesland
 *
 */
public class SimilarityMetrics {

	/**
	 * Computes the Jaccard similarity between two sets of strings
	 * @param set1 the first set of strings
	 * @param set2 the second set of strings
	 * @return the jaccard similarity score (intersection over union)
	   Jul 17, 2019
	 */
	public static double jaccardSetSim (Set<String> set1, Set<String> set2) {


		int intersection = 0;

		for (String s1 : set1) {
			for (String s2 : set2) {
				if (s1.equals(s2)) {
					intersection += 1;
				}
			}
		}

		int union = (set1.size() + set2.size()) - intersection;

		double jaccardSetSim = (double) intersection / (double) union;

		return jaccardSetSim;
	}


	/**
	 * Computes a similarity score using a combination of Jaccard and ISUB
	 * @param confidence a threshold used to determine if two concepts are equal according to ISUB.
	 * @param concept1 represents an ontology concept
	 * @param concept2 represents a second ontology concept
	 * @param set1 represent a set of values associated with concept1 (e.g. set of properties associated with concept1)
	 * @param set2 represent a set of values associated with concept2 (e.g. set of properties associated with concept2)
	 * @return a similarity score computed using a combination of Jaccard and ISUB.
	   Jul 18, 2019
	 */
	public static double jaccardSetSimISubEqualConcepts (double confidence, String concept1, String concept2, Set<String> set1, Set<String> set2) {

		ISub isubMatcher = new ISub();

		int intersection = 0;
		int refinedIntersection = 0;
		int refinedUnion = 0;
		double isubScore = 0;

		for (String s1 : set1) {
			for (String s2 : set2) {
				//using ISub to compute a similarity score
				isubScore = isubMatcher.score(s1,s2);
				if (isubScore > confidence) {
					intersection += 1;
				}
			}
		}

		int union = (set1.size() + set2.size()) - intersection;

		if (set1.contains(concept2.toLowerCase()) && set2.contains(concept1.toLowerCase())) {
			refinedIntersection = intersection +2;
			refinedUnion = union - 2;
		} else if (set1.contains(concept2.toLowerCase()) || set2.contains(concept1.toLowerCase())) {
			refinedIntersection = intersection +1;
			refinedUnion = union - 1;
		} else {
			refinedIntersection = intersection;
			refinedUnion = union;
		}

		double jaccardSetSim = (double) refinedIntersection / (double) refinedUnion;

		return jaccardSetSim;
	}


}
