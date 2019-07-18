package utilities;

import java.util.LinkedList;
import java.util.List;

/**
 * @author audunvennesland
 * 26. okt. 2017 
 */
public class MathUtils {

	public static double weightedSigmoid(int slope, double x, double weight) {
		return (1/( 1 + Math.pow(Math.E,(-slope*(x-weight)))));
	}
	
	public static double weightedEuzenatSigmoid(double x, double weight) {
		return (1/( 1 + Math.pow(Math.E,(-12*(x-(1-(weight/2)))))));
	}
	
	public static double sigmoidRiMom(double x) {
	    return (1/( 1 + Math.pow(Math.E,(-5*(x-0.5)))));
	  }
	
	public static double sigmoidEuzenat(double x) {
	    return (1/( 1 + Math.pow(Math.E,(-12*(x-0.5)))));
	  }
	
	public static double sigmoid(double x) {
	    return (1/( 1 + Math.pow(Math.E,(-1*x))));
	  }
	
	public static double computeInformationContent(int subConcepts, int totalConcepts) {
		return 1-((Math.log((double)subConcepts + 1)) / Math.log((double)totalConcepts));
	}
	
	
	/**
	 * Rounds a double to a specified number of digits after the decimal point
	 * @param value the double to be rounded
	 * @param places number of digits after decimal point
	 * @return rounded double
	 */
	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}
	
	
	
public static void main(String[] args) {
		
		int slope = 12;
		
		List<Double> confidenceList = new LinkedList<Double>();
		
		confidenceList.add(0.1);
		confidenceList.add(0.2);
		confidenceList.add(0.3);
		confidenceList.add(0.4);
		confidenceList.add(0.5);
		confidenceList.add(0.6);
		confidenceList.add(0.7);
		confidenceList.add(0.8);
		confidenceList.add(0.9);
		confidenceList.add(1.0);
		
		List<Double> profileWeightList = new LinkedList<Double>();
		
		profileWeightList.add(0.51);
		profileWeightList.add(0.52);
		profileWeightList.add(0.53);
		profileWeightList.add(0.54);
		profileWeightList.add(0.55);
		profileWeightList.add(0.56);
		profileWeightList.add(0.57);
		profileWeightList.add(0.58);
		profileWeightList.add(0.59);
		profileWeightList.add(0.6);
		
		
		double[][] matrix = new double[confidenceList.size()][profileWeightList.size()];
		
		for (int row = 0; row < confidenceList.size(); row++) {
			
			for (int col = 0; col < profileWeightList.size(); col++) {
				
				matrix[row][col] = round(weightedSigmoid(slope, confidenceList.get(row), profileWeightList.get(col)), 2);
			}
		}
		
		System.out.println("Printing matrix:");
		
		for (int i = 0; i < matrix.length; i++) {
		    for (int j = 0; j < matrix[i].length; j++) {
		        System.out.print(matrix[i][j] + " ");
		    }
		    System.out.println();
		}
		
		
	}

}
