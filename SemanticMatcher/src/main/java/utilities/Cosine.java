package utilities;

import java.util.ArrayList;

/**
 * Various implementations of cosine similarity
 * @author audunvennesland
 * 29. sep. 2017 
 */
public class Cosine {
	
	/**
	 * Computes the cosine similarity between two vectors represented by arrays.
	 * @param vectorA an array representing the first vector
	 * @param vectorB an array representing the second vector
	 * @return the cosine similarity between two vectors
	   Jul 17, 2019
	 */
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {

	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    double tempSim = 0.0;
	    double finalSim = 0.0;
	    
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    tempSim = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	    
	    if (tempSim < 0) {
	    	finalSim = 0;
	    } else if (tempSim > 1.0) {
	    	finalSim = 1.0;
	    } else {
	    	finalSim = tempSim;
	    }
	    
	    return finalSim;
	}
	
	/**
	 * Computes the cosine similarity between two vectors represented by arraylists.
	 * @param vectorAList an arraylist representing the first vector
	 * @param vectorBList an arraylist representing the second vector
	 * @return the cosine similarity between two vectors
	   Jul 17, 2019
	 */
	public static double cosineSimilarity(ArrayList<Double> vectorAList, ArrayList<Double> vectorBList) {
		
		double[] vectorA = new double[vectorAList.size()];
		double[] vectorB = new double[vectorBList.size()];
		
		for (int i = 0; i < vectorA.length; i++) {
			vectorA[i] = vectorAList.get(i);
		}
		
		for (int i = 0; i < vectorB.length; i++) {
			vectorB[i] = vectorBList.get(i);
		}
		
		double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    double tempSim = 0.0;
	    double finalSim = 0.0;
	    
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    tempSim = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	    
	    if (tempSim < 0) {
	    	finalSim = 0;
	    } else if (tempSim > 1.0) {
	    	finalSim = 1.0;
	    } else {
	    	finalSim = tempSim;
	    }
	    
	    return finalSim;
		
	}

}
