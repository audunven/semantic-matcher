package utilities;

import java.util.HashSet;
import java.util.Set;


public class SimilarityMetrics {
	
	public static void main(String[] args) {
		double confidence = 0.7;
		
		Set<String> set1 = new HashSet<String>();
		Set<String> set2 = new HashSet<String>();
		String concept1 = "author";
		String concept2 = "writer";
		
		set1.add("writer");
		set1.add("poet");
		set1.add("correspondent");
		set1.add("wordsmith");
		
		set2.add("author");
		set2.add("poet");
		set2.add("correspondent");
		set2.add("scrawler");
		set2.add("alphabetiser");
		
		System.out.println("Original Jaccard sim is " + jaccardSetSim(set1, set2));
		
		System.out.println("\nRefined Jaccard sim is " + jaccardSetSimEqualConcepts(concept1, concept2, set1, set2));
		
		System.out.println("Refined Jaccard sim using ISub is " + jaccardSetSimISubEqualConcepts(confidence, concept1, concept2, set1, set2));
		
		System.out.println("\nDice similarity is : " + SorensenDiceSim(confidence, set1, set2));
	}
	
	
	/**
	 * jaccardSetSim = [number of common elements] / [total num elements] - [number of common elements]
	 * @param s1
	 * @param s2
	 * @return
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
		
		System.out.println("The union is " + union);
		System.out.println("The intersection is " + intersection);
		
		double jaccardSetSim = (double) intersection / (double) union;
		
		return jaccardSetSim;
	}
	
	public static double jaccardSetSimEqualConcepts (String concept1, String concept2, Set<String> set1, Set<String> set2) {
		
		int intersection = 0;
		int refinedIntersection = 0;
		int refinedUnion = 0;
		
		for (String s1 : set1) {
			for (String s2 : set2) {
				if (s1.equals(s2)) {
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
		

		System.out.println("The refined intersection is " + refinedIntersection);
		System.out.println("The refined union is " + refinedUnion);
		
		double jaccardSetSim = (double) refinedIntersection / (double) refinedUnion;
		
		return jaccardSetSim;
	}
	
	public static double jaccardSetSimISub (Set<String> set1, Set<String> set2) {

		ISub isubMatcher = new ISub();

		int intersection = 0;
		double isubScore = 0;

		for (String s1 : set1) {
			for (String s2 : set2) {
				//using ISub to compute a similarity score
				isubScore = isubMatcher.score(s1,s2);
				System.out.println("The ISubScore between " + s1 + " and " + s2 + " is " + isubScore);
				if (isubScore > 0.7) {
					intersection += 1;
				}
			}
		}

		int union = (set1.size() + set2.size()) - intersection;

		double jaccardSetSim = (double) intersection / (double) union;

		return jaccardSetSim;
	}
						 
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
				//System.out.println("The ISubScore between " + s1 + " and " + s2 + " is " + isubScore);
				if (isubScore > confidence) {
					intersection += 1;
				}
			}
		}
		
		int union = (set1.size() + set2.size()) - intersection;
		
		if (set1.contains(concept2.toLowerCase()) && set2.contains(concept1.toLowerCase())) {
			refinedIntersection = intersection +2;
			refinedUnion = union - 2;
			//System.out.println("\n Increasing intersection by 2 for concepts (" + concept1 + ") and " + concept2);
		} else if (set1.contains(concept2.toLowerCase()) || set2.contains(concept1.toLowerCase())) {
			refinedIntersection = intersection +1;
			refinedUnion = union - 1;
			//System.out.println("Increasing intersection by 1");
		} else {
			refinedIntersection = intersection;
			refinedUnion = union;
		}
		
		//System.out.println("Intersection is: "+ intersection);
		//System.out.println("Refined Intersection is: "+ intersection);

		

		double jaccardSetSim = (double) refinedIntersection / (double) refinedUnion;
		
		return jaccardSetSim;
	}
	
	public static double SorensenDiceSim (double confidence, Set<String> set1, Set<String> set2) {
		
		ISub isubMatcher = new ISub();
		int intersection = 0;
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
		
		double sorensenDiceSim = 2 * ((double) intersection) / (double) union;
		
		return sorensenDiceSim;
		
		
	}
	
	


}
