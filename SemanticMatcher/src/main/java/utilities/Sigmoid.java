package utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author audunvennesland
 * 26. okt. 2017 
 */
public class Sigmoid {
	
	public static void main(String[] args) {
		
		int slope = 1;
		
		double confidence = 1.0;
		double profileWeight = 0.9;
		double rangeMin = 0.5;
		double rangeMax = 0.7;		
		
		System.out.println("The transformed profile weight is " + transformProfileWeight(profileWeight, rangeMin, rangeMax));
		
		System.out.println("Confidence " + confidence + " is transformed to " + round(weightedSigmoid(slope, confidence, transformProfileWeight(profileWeight, rangeMin, rangeMax)), 2) + " using Sigmoid with slope " + slope + " and a profile weight of " + profileWeight);
				
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
		
		profileWeightList.add(0.50);
		profileWeightList.add(0.52);
		profileWeightList.add(0.54);
		profileWeightList.add(0.56);
		profileWeightList.add(0.58);
		profileWeightList.add(0.60);
		profileWeightList.add(0.62);
		profileWeightList.add(0.64);
		profileWeightList.add(0.66);
		profileWeightList.add(0.68);
		
		
		double[][] matrix = new double[confidenceList.size()][profileWeightList.size()];
		
		for (int row = 0; row < confidenceList.size(); row++) {
			
			for (int col = 0; col < profileWeightList.size(); col++) {
				
				matrix[row][col] = round(weightedSigmoid(slope, confidenceList.get(row), profileWeightList.get(col)), 2);
			}
		}
		
		System.out.println("Printing matrix:");
		
		for (int i = 0; i < matrix.length; i++) {
		    for (int j = 0; j < matrix[i].length; j++) {
		        System.out.print(matrix[i][j] + ",");
		    }
		    System.out.println();
		}
		
		
	}
	
	/** 
	 * Transforms a profile weight score between 0 and 1.0 to a given range (minRange-maxRange)
	 * @param profileWeight the profile weight value to be transformed
	 * @param rangeMin the minimum value of the target range
	 * @param rangeMax the maximum value of the target range
	 * @return a transformed profile weight value within the target range (minRange-maxRange)
	   Apr 2, 2019
	 */
	public static double transformProfileWeight (double profileWeight, double rangeMin, double rangeMax) {
		
		return ((1.0 - profileWeight) * (rangeMax - rangeMin) / (1.0 - 0.0)) + rangeMin;
		
	}
	

	/**
	 * Computes sigmoid using a slope parameter, a confidence value, and a profile weight (between 0.5-0.7)
	 * @param slope
	 * @param x
	 * @param weight
	 * @return
	   Apr 2, 2019
	 */
	public static double weightedSigmoid(int slope, double confidence, double profileWeight) {
		return (1/( 1 + Math.pow(Math.E,(-slope*(confidence-profileWeight)))));
	}
	
	/**
	 * Computes sigmoid using a slope parameter, a confidence value, and a profile weight (between 0.5-0.7)
	 * @param slope
	 * @param x
	 * @param weight
	 * @return
	   Apr 2, 2019
	 */
	public static double defaultWeightedSigmoid(int slope, double confidence) {
		return (1/( 1 + Math.pow(Math.E,(-slope*(confidence-0.5)))));
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

}
