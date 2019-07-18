package utilities;

import java.util.HashSet;
import java.util.Set;

public class Jaccard {
	
	
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
	
	public static void main(String[] args) {
		Set<String> set1 = new HashSet<String>();
		Set<String> set2 = new HashSet<String>();
		
		set1.add("links");
		set1.add("flight");
		set1.add("actual");
		set1.add("aircraft");
		set1.add("used");
		
		set2.add("aircraft");
		set2.add("enabling");
		set2.add("flight");
		
		double jaccard = jaccardSetSim(set1, set2);
		
		System.out.println("The jaccard is " + jaccard);
		
	}

}
