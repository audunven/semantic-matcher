package wordembedding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import fr.inrialpes.exmo.ontosim.vector.CosineVM;
import utilities.Cosine;
import utilities.MathUtils;
import utilities.StringUtilities;

/**
 * @author audunvennesland
 * 6. okt. 2017 
 */
public class VectorConcept {

	String conceptURI;
	String conceptLabel;
	ArrayList<Double> labelVectors = new ArrayList<Double>();
	ArrayList<Double> commentVectors = new ArrayList<Double>();
	ArrayList<Double> globalVectors = new ArrayList<Double>();

	/**
	 * @param conceptURI the URI of the concept (e.g. http;//oaei.ontologymatching.org/2010/benchmarks/301/onto.rdf#Manual)
	 * @param conceptLabel the label associated with a concept (e.g. Manual)
	 * @param labelVectors a set of vectors associated with the label
	 * @param commentVectors a set of vectors associated with comments defining the concept
	 * @param globalVectors vectors as average of the label vectors and comment vectors
	 */
	public VectorConcept(String conceptURI, String conceptLabel, ArrayList<Double> labelVectors, ArrayList<Double> commentVectors,
			ArrayList<Double> globalVectors) {
		super();
		this.conceptURI = conceptURI;
		this.conceptLabel = conceptLabel;
		this.labelVectors = labelVectors;
		this.commentVectors = commentVectors;
		this.globalVectors = globalVectors;
	}
	

	public VectorConcept() {}

	private String getConceptURI() {
		return conceptURI;
	}

	public String getConceptLabel() {
		return conceptLabel;
	}

	/**
	 * @return the labelVectors
	 */
	public ArrayList<Double> getLabelVectors() {
		return labelVectors;
	}

	/**
	 * @param labelVectors the labelVectors to set
	 */
	private void setLabelVectors(ArrayList<Double> labelVectors) {
		this.labelVectors = labelVectors;
	}

	/**
	 * @return the commentVectors
	 */
	public ArrayList<Double> getCommentVectors() {
		return commentVectors;
	}

	/**
	 * @param commentVectors the commentVectors to set
	 */
	private void setCommentVectors(ArrayList<Double> commentVectors) {
		this.commentVectors = commentVectors;
	}

	/**
	 * @return the globalVectors
	 */
	public ArrayList<Double> getGlobalVectors() {
		return globalVectors;
	}

	/**
	 * @param globalVectors the globalVectors to set
	 */
	private void setGlobalVectors(ArrayList<Double> globalVectors) {
		this.globalVectors = globalVectors;
	}

	/**
	 * @param conceptName the conceptName to set
	 */
	private void setConceptURI(String conceptURI) {
		this.conceptURI = conceptURI;
	}

	/**
	 * @param conceptName the conceptName to set
	 */
	private void setConceptLabel(String conceptLabel) {
		this.conceptLabel = conceptLabel;
	}


/**
 * Iterates through each line in a vector file, checks which value is covered by that line, and adds the appropriate variable to the VectorConcept object. 
 * Finally, a set of VectorConcepts is used for holding VectorConcepts from a given file
 * @param vectorFile a vector file consists of a concept and its description (URI, label, comment tokens) and associated vectors
 * @return a set of VectorConcepts
 * @throws FileNotFoundException
 */
	public static Set<VectorConcept> populate(File vectorFile) throws FileNotFoundException {

		Set<VectorConcept> vcSet = new HashSet<VectorConcept>();

		String conceptURI = null;
		String conceptLabel = null;
		ArrayList<Double> labelVectors = null;
		ArrayList<Double> commentVectors = null;
		ArrayList<Double> globalVectors = null;

		VectorConcept vc = new VectorConcept();
		Scanner sc = new Scanner(vectorFile);

		//iterates through each line in a vector file, checks which value is covered by that line, and adds the appropriate variable to the VectorConcept object. Finally, a set of VectorConcepts is used for holding VectorConcepts 
		//from a given file
		while (sc.hasNextLine()) {

			String line = sc.nextLine();

			if (!line.isEmpty()) {

				String[] strings = line.split(";");

				if (strings[0].equals("conceptUri")) {
					conceptURI = strings[1];
					vc.setConceptURI(conceptURI);


				} if (strings[0].equals("label")) {
					conceptLabel = strings[1].trim();
					vc.setConceptLabel(conceptLabel);


				} if (strings[0].equals("label vector")) {
					labelVectors = new ArrayList<Double>();
					String lv = strings[1];
					String[] vectors = lv.split(" ");
					for (int i = 0; i < vectors.length; i++) {
						if (!vectors[i].isEmpty()) {
							labelVectors.add(Double.valueOf(vectors[i]));
						}
					}
					vc.setLabelVectors(labelVectors);


				} if (strings[0].equals("comment vector")) {
					commentVectors = new ArrayList<Double>();
					
					
					if (strings[1].equals("no vectors for these comment tokens")) {
					String lv = strings[1];
					String[] vectors = lv.split(" ");
					for (int i = 0; i < vectors.length; i++) {
						if (!vectors[i].isEmpty()) {
							commentVectors.add(Double.valueOf(vectors[i]));
						}
					}

					vc.setCommentVectors(commentVectors);
					} else {
						vc.setCommentVectors(null);
					}


				} if (strings[0].equals("global vector")) {
					globalVectors = new ArrayList<Double>();
					String lv = strings[1];
					String[] vectors = lv.split(" ");
					for (int i = 0; i < vectors.length; i++) {
						if (!vectors[i].isEmpty()) {
							globalVectors.add(Double.valueOf(vectors[i]));
						}
					}

					vc.setGlobalVectors(globalVectors);


					vcSet.add(vc);

					//instantiate a new VectorConcept object to hold next iterations
					vc = new VectorConcept();

				}
			}
		}

		sc.close();

		return vcSet;
	}
	
	/**
	 * Sorts a Map on values where key is a pair of concepts being matched and value is cosine sim
	 * @param cosineMap
	 * @return
	 */
	private static Map<String, Double> sortByValue(Map<String, Double> cosineMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Double>> list =
                new LinkedList<Map.Entry<String, Double>>(cosineMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        
        Collections.reverse(list);

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
	
	public static void printLabelAndGlobalSim(File vectorFileDir, double threshold) throws FileNotFoundException {
		
		//using the cosine measure from OntoSim
		CosineVM cosine = new CosineVM();
		File[] filesInDir = vectorFileDir.listFiles();
		
		for (int i = 0; i < filesInDir.length; i++) {
			for (int j = i+1; j < filesInDir.length; j++) {
				if (filesInDir[i].isFile() && filesInDir[j].isFile() && i != j) {
					System.out.println("\n*****Computing cosine for " + StringUtilities.stripPath(filesInDir[i].toString()) + " and " + StringUtilities.stripPath(filesInDir[j].toString()) + " *****");
					
					Set<VectorConcept> vc1 = populate(filesInDir[i]);
					Set<VectorConcept> vc2 = populate(filesInDir[j]);
					
					System.out.println("Number of vector concepts for " + StringUtilities.stripPath(filesInDir[i].toString()) + " " + vc1.size());
					System.out.println("Number of vector concepts for " + StringUtilities.stripPath(filesInDir[j].toString()) + " " + vc2.size());
					
					Map<String, Double> rankedLabelMap = new HashMap<String, Double>();
					Map<String, Double> rankedGlobalMap = new HashMap<String, Double>();
					
					//label vectors sim
					System.out.println("\n---Label Vectors Similarity > " + threshold + " ---");
					for (VectorConcept v1 : vc1) {
						for (VectorConcept v2 : vc2) {
							double[] a1 = ArrayUtils.toPrimitive(v1.getLabelVectors().toArray((new Double[v1.getLabelVectors().size()])));
							double[] a4 = ArrayUtils.toPrimitive(v2.getLabelVectors().toArray((new Double[v2.getLabelVectors().size()])));
							double c = cosine.getSim(a1, a4);
							if (c > threshold) {
								String pair = v1.getConceptLabel() + " -" + v2.getConceptLabel();
								rankedLabelMap.put(pair, MathUtils.round(c, 6));
							}
						}
					}
					
					Map<String, Double> sortedLabelMap = sortByValue(rankedLabelMap);
					for (Entry<String, Double> e : sortedLabelMap.entrySet()) {
						System.out.println(e.getKey() + ": " + e.getValue());
					}
					
					//global vectors sim
					System.out.println("\n---Global Vectors Similarity > " + threshold + " ---");
					for (VectorConcept v1 : vc1) {
						for (VectorConcept v2 : vc2) {
							double[] a1 = ArrayUtils.toPrimitive(v1.getGlobalVectors().toArray((new Double[v1.getGlobalVectors().size()])));
							double[] a4 = ArrayUtils.toPrimitive(v2.getGlobalVectors().toArray((new Double[v2.getGlobalVectors().size()])));
							double c = cosine.getSim(a1, a4);
							if (c > threshold) {
								String pair = v1.getConceptLabel() + " -" + v2.getConceptLabel();
								rankedGlobalMap.put(pair, MathUtils.round(c, 6));
							}
						}
					}
					
					Map<String, Double> sortedGlobalMap = sortByValue(rankedGlobalMap);
					for (Entry<String, Double> e : sortedGlobalMap.entrySet()) {
						System.out.println(e.getKey() + ": " + e.getValue());
					}
					
				}
			}
		}
		
	}

	
public static void printGlobalSim(File vectorFileDir, double threshold) throws FileNotFoundException {
		
		//using the cosine measure from OntoSim
		CosineVM cosine = new CosineVM();
		File[] filesInDir = vectorFileDir.listFiles();
		
		for (int i = 0; i < filesInDir.length; i++) {
			for (int j = i+1; j < filesInDir.length; j++) {
				if (filesInDir[i].isFile() && filesInDir[j].isFile() && i != j) {
					System.out.println("\n*****Computing cosine for " + StringUtilities.stripPath(filesInDir[i].toString()) + " and " + StringUtilities.stripPath(filesInDir[j].toString()) + " *****");
					
					Set<VectorConcept> vc1 = populate(filesInDir[i]);
					Set<VectorConcept> vc2 = populate(filesInDir[j]);
					
					System.out.println("Number of vector concepts for " + StringUtilities.stripPath(filesInDir[i].toString()) + " " + vc1.size());
					System.out.println("Number of vector concepts for " + StringUtilities.stripPath(filesInDir[j].toString()) + " " + vc2.size());
					
					Map<String, Double> rankedMap = new HashMap<String, Double>();
					
					System.out.println("\n---Global Vectors Similarity > " + threshold + " ---");
					for (VectorConcept v1 : vc1) {
						for (VectorConcept v2 : vc2) {
							double[] a1 = ArrayUtils.toPrimitive(v1.getGlobalVectors().toArray((new Double[v1.getGlobalVectors().size()])));
							double[] a4 = ArrayUtils.toPrimitive(v2.getGlobalVectors().toArray((new Double[v2.getGlobalVectors().size()])));
							double c = cosine.getSim(a1, a4);
							if (c > threshold) {

								String pair = v1.getConceptLabel() + " -" + v2.getConceptLabel();
								rankedMap.put(pair, MathUtils.round(c, 6));
							}

						}
					}
					
					Map<String, Double> sortedMap = sortByValue(rankedMap);
					for (Entry<String, Double> e : sortedMap.entrySet()) {
						System.out.println(e.getKey() + ": " + e.getValue());
					}
					
				}
			}
		}		
	}
	

	//printing cosine sim based on label vectors and global vectors
	public static void main(String[] args) throws FileNotFoundException {
		
		final File vectorFileDir = new File("./files/wordembedding/vector-files-single-ontology");
		double threshold = 0.6;
		
		VectorConcept vc = new VectorConcept();
		
		Set<VectorConcept> vectorConceptSet301 = populate(new File("./files/wordembedding/vector-files/vectorOutput301302-301.txt"));
		Set<VectorConcept> vectorConceptSet302 = populate(new File("./files/wordembedding/vector-files/vectorOutput301302-302.txt"));
		
		System.out.println("There are " + vectorConceptSet301.size() + " vector Concepts");
		System.out.println("There are " + vectorConceptSet302.size() + " vector Concepts");
		
		ArrayList<Double> lv = new ArrayList<Double>();
		
		System.out.println("Printing labels and vectors in 301");
		for (VectorConcept vc301 : vectorConceptSet301) {
			System.out.println(vc301.getConceptLabel());
			lv = vc301.getLabelVectors();
			for (Double d : lv) {
			
			System.out.println(d);
			}
		}
		
		
		
		
		
		//printLabelAndGlobalSim(vectorFileDir, threshold);


	}
}
