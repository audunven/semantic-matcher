package utilities;

import java.util.HashSet;
import java.util.Set;

public class Jaccard {
	
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
		
		//System.out.println("The union is " + union);
		//System.out.println("The intersection is " + intersection);
		
		double jaccardSetSim = (double) intersection / (double) union;
		
		return jaccardSetSim;
	}
	
	public static void main(String[] args) {
		Set<String> set1 = new HashSet<String>();
		Set<String> set2 = new HashSet<String>();
		
//		set1.add("audi");
//		set1.add("vw");
//		set1.add("seat");
//		set1.add("toyota");
//		set1.add("mitsubishi");
//		
//		set2.add("audi");
//		set2.add("vw");
//		set2.add("seat");
//		set2.add("jaguar");
//		set2.add("rollsroyce");
//		set2.add("landrover");
		
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
