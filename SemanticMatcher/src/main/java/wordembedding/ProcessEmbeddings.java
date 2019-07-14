package wordembedding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import fr.inrialpes.exmo.ontosim.vector.CosineVM;
import utilities.StringUtilities;


public class ProcessEmbeddings {

	public static void main(String[] args) throws IOException {
		String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/processedFile_Skybrary.txt";
		String output = "./files/_PHD_EVALUATION/EMBEDDINGS/Skybrary_vectors_lemmatized.txt";
		
		ArrayList<Double> vectors1 = new ArrayList<Double>();
		ArrayList<Double> vectors2 = new ArrayList<Double>();
		
		vectors1.add(0.3);
		vectors1.add(0.4);
		vectors1.add(0.5);
		vectors1.add(0.6);
		vectors1.add(0.7);
		
		vectors2.add(0.1);
		vectors2.add(0.2);
		vectors2.add(0.3);
		vectors2.add(0.4);
		vectors2.add(0.5);
		
		ArrayList<ArrayList<Double>> listOfArrayLists = new ArrayList<ArrayList<Double>>();
		listOfArrayLists.add(vectors1);
		listOfArrayLists.add(vectors2);
		
		//private static ArrayList<Double> getAVGVectors(ArrayList<ArrayList<Double>> a_input) {
		ArrayList<Double> avg = getAVGVectors(listOfArrayLists, 5);
		for (double d : avg) {
			System.out.println(d);
		}
		
				
		lemmatizeVectorFile(vectorFile, output);

	}
	
	public static File lemmatizeVectorFile (String vectorFile, String outputFile) throws IOException {
		File vectors = new File(vectorFile);
		
		File output= new File(outputFile);
		
		//String buffer to store contents of the file
		StringBuffer sb=new StringBuffer("");

		//create a map of the vectors
		Map<String, ArrayList<Double>> vectorMap = VectorExtractor.createVectorMap(vectors);

		Map<String, ArrayList<Double>> lemmatizedVectorMap = new HashMap<String, ArrayList<Double>>();

		Map<String, String> lemmas = new HashMap<String, String>();
		for (Entry<String, ArrayList<Double>> e : vectorMap.entrySet()) {
			lemmas.put(e.getKey(), StringUtilities.getLemma(e.getKey()));
		}

		for (Entry<String, String> e : lemmas.entrySet()) {

			//get all keys that are associated with the same lemma from the vectormap
			Set<String> keys = getKeysByValue(lemmas, e.getValue());

			ArrayList<ArrayList<Double>> allVectors = new ArrayList<ArrayList<Double>>();

			for (String key : keys) {
				if (e.getValue().equals("publish")) {
				System.out.println("Adding the vectors of key " + key + " to the vectorMap (lemma is " + e.getValue() + ")");
				}
				allVectors.add(vectorMap.get(key));
			}

			ArrayList<Double> avgVectors = getAVGVectors(allVectors, 300);
			lemmatizedVectorMap.put(e.getValue(), avgVectors);

			sb.append(e.getValue() + " " + arrayListToString(avgVectors) + "\n");

		}
		
		FileWriter fw=new FileWriter(output);
		//Write entire string buffer into the file
		fw.write(sb.toString());
		fw.close();

		return output;
	}
	
	private static String arrayListToString(ArrayList<Double> arrayList) {
		
		StringBuffer sb=new StringBuffer("");
		for (double d : arrayList) {
			sb.append(Double.toString(d) + " ");
		}
		
		return sb.toString();
		
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
	
	
	public static String getAVGVectors(Map<String, ArrayList<Double>> vectorMap, int numVectors) {

		ArrayList<Double> avgList = new ArrayList<Double>();
		
		double[] temp = new double[numVectors];
		
		for (Entry<String, ArrayList<Double>> e : vectorMap.entrySet()) {
			
			for (int i = 0; i < temp.length; i++) {
				temp[i] += e.getValue().get(i);
			}
			
		}
		
		for (int i = 0; i < temp.length; i++) {
			avgList.add(temp[i] / (double) vectorMap.size());
		}
				
		return arrayListToString(avgList);
		
	}


	private static double computeCosSim(ArrayList<Double> a1, ArrayList<Double> a2) {
		double sim = 0;

		double[] vec1 = ArrayUtils.toPrimitive(a1.toArray((new Double[a1.size()])));	
		double[] vec2 = ArrayUtils.toPrimitive(a2.toArray((new Double[a2.size()])));	

		//measure the cosine similarity between the vector dimensions of these two entities
		CosineVM cosine = new CosineVM();

		if (vec1 != null && vec2 != null) {

			sim = cosine.getSim(vec1, vec2);

		}

		return sim;
	}

	/**
	 * Removes vectors that do not represent words from a corpus. 
	 * @param corpus a set of words
	 * @param inputFile a vector file where each line represents a word and associated embedding vector
	 * @return a vector file that only contains words represented in the corpus
	 * @throws IOException
	   Feb 15, 2019
	 */
	public static File removeVectors(Set<String> corpus, String vectorFile) throws IOException {

		File processedFile = new File("./files/processedFile_Lemmatized_Wikipedia.txt");

		BufferedReader br=new BufferedReader(new FileReader(vectorFile));

		//String buffer to store contents of the file
		StringBuffer sb=new StringBuffer("");

		String line;
		String wordToCheck = null;

		while((line=br.readLine())!=null) {

			//get the first word of the line

			int i = line.indexOf(' ');
			wordToCheck = line.substring(0, i);
			if (corpus.contains(wordToCheck)) {
				sb.append(line+"\n");
			}

		}

		br.close();

		FileWriter fw=new FileWriter(processedFile);
		//Write entire string buffer into the file
		fw.write(sb.toString());
		fw.close();


		return processedFile;


	}

	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
		Set<T> keys = new HashSet<T>();
		for (Entry<T, E> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				keys.add(entry.getKey());
			}
		}
		return keys;
	}

}
