package wordembedding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import utilities.StringUtilities;
import utilities.MathUtils;

/**
 * Extracts VectorConcepts by retrieving vectors from a Word Embedding file according to concepts in an ontology.
 * @author audunvennesland
 * 21. sep. 2017 
 */
public class VectorExtractor {

	/**
	 * An OWLOntologyManager manages a set of ontologies. It is the main point
	 * for creating, loading and accessing ontologies.
	 */
	static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	/**
	 * The OWLReasonerFactory represents a reasoner creation point.
	 */
	static OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();

	static OWLDataFactory factory = manager.getOWLDataFactory();

	//private static DecimalFormat df6 = new DecimalFormat(".######");

	public static String getConceptURI(OWLClass cls) {
		String conceptURI = cls.getIRI().toString();

		return conceptURI;
	}

	/**
	 * Returns the label (i.e. the concept name without prefix) from an OWL class
	 * @param cls the input OWL class
	 * @return label (string) without prefix
	 */
	public static String getLabel (OWLClass cls) {
		String label = cls.getIRI().getFragment().toString().toLowerCase();

		return label;
	}

	/**
	 * Returns a set of string tokens from the RDFS comment associated with an OWL class
	 * @param onto The ontology holding the OWL class
	 * @param cls The OWL class
	 * @return A string representing the set of tokens from a comment without stopwords
	 * @throws IOException
	 */
	public static String getComment (OWLOntology onto, OWLClass cls) throws IOException {

		String comment = null;
		String commentWOStopWords = null;

		for(OWLAnnotation a : cls.getAnnotations(onto, factory.getRDFSComment())) {
			OWLAnnotationValue value = a.getValue();
			if(value instanceof OWLLiteral) {
				comment = ((OWLLiteral) value).getLiteral().toString();
				commentWOStopWords = StringUtilities.removeStopWords(comment);
			}
		}

		return commentWOStopWords;

	}

	/**
	 * Returns a boolean stating if two input strings are equal
	 * @param input An input string from the vector file checked for equality
	 * @param cls An input string representing the class label checked for equality
	 * @return A boolean stating if two strings are equal
	 */
	public static boolean stringMatch(String input, String cls) {
		boolean match = false;

		if (input.equals(cls)) {
			match = true;
		} else {
			match = false;
		}


		return match;
	}

	/**
	 * Returns a boolean stating whether a term is considered a compound term
	 * (e.g. ElectronicBook)
	 * 
	 * @param a
	 *            the input string tested for being compound or not
	 * @return boolean stating whether the input string is a compound or not
	 */
	public static boolean isCompound(String a) {
		boolean test = false;

		String[] compounds = a.split("(?<=.)(?=\\p{Lu})");

		if (compounds.length > 1) {
			test = true;
		}

		return test;
	}

	

	/**
	 * Checks whether a string contains only letters 
	 * @param name The string checked for containing only letters
	 * @return A boolean stating if a string contains only letters
	 */
	public static boolean isAlpha(String name) {
		char[] chars = name.toCharArray();

		for (char c : chars) {
			if(!Character.isLetter(c)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Averages a set of vectors
	 * @param inputVectors ArrayList holding a set of input vectors
	 * @return an average of all input vectors
	 */
	public static double averageVectors (ArrayList<Double> inputVectors) {

		int num = inputVectors.size();

		double sum = 0;

		for (Double d : inputVectors) {
			sum+=d;
		}

		double averageVectors = sum/num;

		return averageVectors;
	}



	/**
	 * Takes a file of words and corresponding vectors and creates a Map where the word in each line is key and the vectors are values (as ArrayList<Double>)
	 * @param vectorFile A file holding a word and corresponding vectors on each line
	 * @return A Map<String, ArrayList<Double>> where the key is a word and the value is a list of corresponding vectors
	 * @throws FileNotFoundException
	 */
	public static Map<String, ArrayList<Double>> createVectorMap (File vectorFile) throws FileNotFoundException {

		Map<String, ArrayList<Double>> vectorMap = new HashMap<String, ArrayList<Double>>();

		Scanner sc = new Scanner(vectorFile);

		//read the file holding the vectors and extract the concept word (first word in each line) as key and the vectors as ArrayList<Double> as value in a Map
		while (sc.hasNextLine()) {

			String line = sc.nextLine();
			String[] strings = line.split(" ");

			//get the word, not the vectors
			String word1 = strings[0];

			//get the vectors and put them in an array list
			ArrayList<Double> vec = new ArrayList<Double>();
			for (int i = 1; i < strings.length; i++) {
				vec.add(Double.valueOf(strings[i]));
			}
			//put the word and associated vectors in the vectormap
			vectorMap.put(word1, vec);

		}
		sc.close();

		return vectorMap;
	}
	
	/**
	 * Returns a "global vector", that is an average of a label vector and a comment vector
	 * @param labelVector The average vector for an OWL class´ label
	 * @param commentVector The average vector for all (string) tokens in the OWL class´ RDFS comment
	 * @return a set of vectors averaged between label vectors and comment vectors
	 * @throws IOException 
	 */
	public static double[] getGlobalVector(String label, String def, Map<String, ArrayList<Double>> vectorMap) throws IOException {

		ArrayList<Double> labelVectors = getLabelVector(label, vectorMap);
		
		ArrayList<Double> commentVectors = getCommentVector(def, vectorMap);

		ArrayList<Double> globalVectors = new ArrayList<Double>();

		ArrayList<Double> globalVector = new ArrayList<Double>();
		
		double[] vectors = new double[300];


		//a fixed dimension of vectors is 300
		int numVectors = 300;

		//if there also are comment vectors, we average the label vector and the comment vector (already averaged between all token vectors for each comment) into a global vector
		//TODO: Simplify this computation of averages
		if (labelVectors != null && !labelVectors.isEmpty() && commentVectors!= null && !commentVectors.isEmpty()) {

			double average = 0;
			for (int i = 0; i < numVectors; i++) {
				if (labelVectors.size() < 1 && commentVectors.size() < 1) {
					return null;
				} else if (labelVectors.size() < 1 && commentVectors.size() > 0) {
					average = commentVectors.get(i);
				} else if (labelVectors.size() > 0 && commentVectors.size() < 1) { 
					average = labelVectors.get(i);
				} else {

					if (labelVectors.get(i) == 0.0) {
						average = commentVectors.get(i);
					} else if (commentVectors.get(i) == 0.0) {
						average = labelVectors.get(i);
					} else {

						average = (labelVectors.get(i) + commentVectors.get(i)) / 2;
					}
				}
				globalVectors.add(average);

			}

		} else {
			
			globalVector = labelVectors;
		}

		//round the vector value to 6 decimals
		for (double d : globalVectors) {
			globalVector.add(MathUtils.round(d, 6));
		}

		
		if (globalVector != null && !globalVector.isEmpty()) {
		
		for (int i = 0; i < vectors.length; i++) {
			vectors[i] = globalVector.get(i);
		}
		
		return vectors;
		
		} else {
			return null;
		}

	}
	
	/**
	 * Checks if the vectorMap contains the label of an OWL class as key and if so the vectors of the label are returned. 
	 * @param cls An input OWL class
	 * @param vectorMap The Map holding words and corresponding vectors
	 * @return a set of vectors (as a string) associated with the label
	 */
	public static ArrayList<Double> getLabelVector(String label, Map<String, ArrayList<Double>> vectorMap) {


		ArrayList<ArrayList<Double>> avgLabelVectors = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> labelVector = new ArrayList<Double>();
		ArrayList<Double> localVectors = new ArrayList<Double>();

		//if the class name is not a compound, turn it into lowercase, 
		if (!StringUtilities.isCompoundWord(label)) {

			String lcLabel = label.toLowerCase();

			//if the class name is in the vectormap, get its vectors
			if (vectorMap.containsKey(lcLabel)) {
				labelVector = vectorMap.get(lcLabel);

			} else {

				labelVector = null;
			}


			//if the class name is a compound, split the compounds, and if the vectormap contains ANY of the compounds, extract the vectors from 
			//the compound parts and average them in order to return the vector for the compound class name
		} else if (StringUtilities.isCompoundWord(label)) {
			

			//get the compounds and check if any of them are in the vector file
			String[] compounds = label.split("(?<=.)(?=\\p{Lu})");


			for (int i = 0; i < compounds.length; i++) {
				
				if (vectorMap.containsKey(compounds[i].toLowerCase())) {
					
					localVectors = vectorMap.get(compounds[i].toLowerCase());
					
					avgLabelVectors.add(localVectors);


				} else {
					
					labelVector = null;
				}
			}
			
			//averages all vector arraylists
			labelVector = getAVGVectors(avgLabelVectors, 300);

		}

		return labelVector;


	}
	
	/**
	 * Returns the average vector of all tokens represented in the RDFS comment for an OWL class
	 * @param onto The ontology holding the OWL class
	 * @param cls The OWL class
	 * @param vectorMap The map of vectors from en input vector file
	 * @return An average vector for all (string) tokens in an RDFS comment
	 * @throws IOException
	 */
	public static ArrayList<Double> getCommentVector(String comment, Map<String, ArrayList<Double>> vectorMap) throws IOException {
		
		
		ArrayList<ArrayList<Double>> avgCommentVectors = new ArrayList<ArrayList<Double>>();

		ArrayList<Double> commentVector = new ArrayList<Double>();

		ArrayList<Double> commentVectors = new ArrayList<Double>();

		if (comment != null && !comment.isEmpty()) {

			//create tokens from comment
			ArrayList<String> tokens = StringUtilities.tokenize(comment, true);
			

			if (containedInVectorMap(tokens, vectorMap)) {
			//put all tokens that have an associated vector in the vectorMap in allCommentVectors along with the associated vector
			for (String s : tokens) {
				
				if (vectorMap.containsKey(s)) {

					commentVectors = vectorMap.get(s);
					
					avgCommentVectors.add(commentVectors);

				}
			} 
			
			//create average vector representing all token vectors in each comment
			//averages all vector arraylists
			commentVector = getAVGVectors(avgCommentVectors, 300);
			
			} 

			else {
				commentVector = null;
			}
			
		} else {
			commentVector = null;
		}
		

		return commentVector;

	}
	
	private static double[] getAVGVectorsToArray(ArrayList<ArrayList<Double>> a_input, int numVectors) {

		ArrayList<Double> avgList = new ArrayList<Double>();

		double[] avgArray = new double[300];

		double[] temp = new double[numVectors];


		for (ArrayList<Double> singleArrayList : a_input) {
			for (int i = 0; i < temp.length; i++) {
				temp[i] += singleArrayList.get(i);
			}
		}

		for (int i = 0; i < temp.length; i++) {
			avgList.add(temp[i]/(double) a_input.size());
		}

		for (int i = 0; i < avgArray.length; i++) {
			avgArray[i] = avgList.get(i);
		}


		return avgArray;
	}

	
	
	private static ArrayList<Double> getAVGVectors(ArrayList<ArrayList<Double>> a_input, int numVectors) {

		ArrayList<Double> avgList = new ArrayList<Double>();
		
		double[] temp = new double[numVectors];
		
		
		for (ArrayList<Double> singleArrayList : a_input) {
			for (int i = 0; i < temp.length; i++) {
				temp[i] += singleArrayList.get(i);
			}
		}
		
		for (int i = 0; i < temp.length; i++) {
			avgList.add(temp[i]/(double) a_input.size());
		}
		
		
		
		return avgList;
	}
	
	private static boolean containedInVectorMap (ArrayList<String> tokens, Map<String, ArrayList<Double>> vectorMap) {
		
		boolean contains = false;
		
		for (String s : tokens) {
			if (vectorMap.containsKey(s)) {
				contains = true;
			}
		}
		
		return contains;
		
	}

	
	/**
	 * Checks if the vectorMap contains the label of an OWL class as key and if so the vectors of the label are returned. 
	 * @param cls An input OWL class
	 * @param vectorMap The Map holding words and corresponding vectors
	 * @return a set of vectors (as a string) associated with the label
	 */
	public static ArrayList<Double> getLemmatizedLabelVector(String lemmatizedConceptName, Map<String, ArrayList<Double>> vectorMap) {

		StringBuffer sb = new StringBuffer();

		ArrayList<Double> labelVectors = new ArrayList<Double>();
		Map<String, ArrayList<Double>> compoundVectors = new HashMap<String, ArrayList<Double>>();
		ArrayList<Double> labelVector = new ArrayList<Double>();
//		String labelVector = null;
		String label = lemmatizedConceptName;

		//if the class name is not a compound, turn it into lowercase, 
		if (!isCompound(label)) {

			String lcLabel = label.toLowerCase();

			//if the class name is in the vectormap, get its vectors
			if (vectorMap.containsKey(lcLabel)) {
				labelVectors = vectorMap.get(lcLabel);

//				for (double d : labelVectors) {
//					sb.append(Double.toString(d) + " ");
//
//				}

			} else {

				labelVectors = null;
			}

			labelVector = labelVectors;

			//if the class name is a compound, split the compounds, and if the vectormap contains ANY of the compounds, extract the vectors from 
			//the compound parts and average them in order to return the vector for the compound class name
		} else if (isCompound(label)) {

			//get the compounds and check if any of them are in the vector file
			String[] compounds = label.split("(?<=.)(?=\\p{Lu})");


			for (int i = 0; i < compounds.length; i++) {
				if (vectorMap.containsKey(compounds[i].toLowerCase())) {
					labelVectors = vectorMap.get(compounds[i].toLowerCase());

					compoundVectors.put(compounds[i].toLowerCase(), labelVectors);


				} else {
					labelVectors = null;
				}
			}

			//we need to create average scores for each vector dimension (i.e. the rows of a vector matrix)
			ArrayList<Double> avgs = new ArrayList<Double>();

			//get the number (dimension) of vectors for each entry (should be 300)
			int numVectors = 300;
			ArrayList<Double> temp = new ArrayList<Double>();

			//creating a temporary arraylist<double> that will be used to compute average vectors for each vector
			for (int i = 0; i < numVectors; i++) {


				for (Entry<String, ArrayList<Double>> e : compoundVectors.entrySet()) {

					ArrayList<Double> a = e.getValue();

					temp.add(a.get(i));

				}


				double avg = 0;

				//number of entries to create an average from
				int entries = temp.size();

				//for each vector (d) in the temporary arraylist
				for (double d : temp) {
					avg += d;

				}

				double newAvg = avg/entries;

				//ensure that vectors are not 0.0 or NaN
				if (newAvg != 0.0 && !Double.isNaN(newAvg)) {
					avgs.add(newAvg);

				}

			}

//			for (double d : avgs) {
//				sb.append(Double.toString(MathUtils.round(d, 6)) + " ");
//
//			}

			labelVector = avgs;

		}

		//the label vector is averaged across all compound parts that are in the vectormap
		return labelVector;


	}


	}
