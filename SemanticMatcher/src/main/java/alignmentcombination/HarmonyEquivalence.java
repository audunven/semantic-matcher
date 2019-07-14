package alignmentcombination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import subsumptionmatching.CompoundMatcher;
import utilities.AlignmentOperations;
import utilities.Relation;
import utilities.RelationComparator;

public class HarmonyEquivalence {

	public static void main(String[] args) throws AlignmentException, IOException, OWLOntologyCreationException {
		
		File inputAlignmentFile = new File("./files/weGlobalAlignment.rdf");
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());
		
		//public static BasicAlignment getHarmonyAlignment(BasicAlignment inputAlignment) throws AlignmentException, IOException {
		BasicAlignment harmonyAlignment = getHarmonyAlignment(inputAlignment);
		

	}
	
	public static URIAlignment computeHarmonyAlignmentFromFolder(String folderName) throws AlignmentException, IOException {
		URIAlignment temp_harmonyAlignment = new URIAlignment();
		URIAlignment one2OneHarmonyAlignment = new URIAlignment();
		Map<URIAlignment, Double> hMap = new HashMap<URIAlignment, Double>();
		
		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();
		
		for (int i = 0; i < filesInDir.length; i++) {
			
			//only the 0.0 cut threshold files should be considered
			if (filesInDir[i].getName().endsWith("0.0.rdf")) {
			
			//get Harmony alignment and Harmony Value
			//System.out.println("Getting the harmony value from alignment " + filesInDir[i]);
			Map<URIAlignment, Double> localHMap = getHarmonyValue(filesInDir[i]);
			hMap.putAll(localHMap);
			}
			
		}
		
		//integrate all localHMap alignment cells after computing their Harmony value
		for (Entry<URIAlignment, Double> e : hMap.entrySet()) {
			double weight = e.getValue();
			
			URI onto1URI = e.getKey().getOntology1URI();
			URI onto2URI = e.getKey().getOntology2URI();
			
			//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
			temp_harmonyAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
			
			for (Cell c : e.getKey()) {
				temp_harmonyAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength()*weight);
			}
		}
		

		
		//remove relations with 0 confidence
		AlignmentOperations.removeZeroConfidenceRelations(temp_harmonyAlignment);
		URIAlignment harmonyAlignment = (URIAlignment) temp_harmonyAlignment.clone();
		
		//normalize the confidence of all cells between [0..1]
		//AlignmentOperations.normalizeConfidence(harmonyAlignment);
		//ensure 1-1 equivalence relations
		
		//nonConflictedPWEAlignment = NaiveDescendingExtraction.naiveDescendingExtraction(profileWeightingEquivalenceAlignment);
		one2OneHarmonyAlignment = NaiveDescendingExtraction.extractOneToOneRelations(harmonyAlignment);
		
		return one2OneHarmonyAlignment;
	}
	
//	public static URIAlignment computeHarmonyAlignment(String folderName) throws AlignmentException, IOException {
//		URIAlignment temp_harmonyAlignment = new URIAlignment();
//		Map<URIAlignment, Double> hMap = new HashMap<URIAlignment, Double>();
//		
//		File folder = new File(folderName);
//		File[] filesInDir = folder.listFiles();
//		
//		for (int i = 0; i < filesInDir.length; i++) {
//			
//			//get Harmony alignment and Harmony Value
//			//System.out.println("Getting the harmony value from alignment " + filesInDir[i]);
//			Map<URIAlignment, Double> localHMap = getHarmonyValue(filesInDir[i]);
//			hMap.putAll(localHMap);
//			
//		}
//		
//		//integrate all localHMap alignment cells after computing their Harmony value
//		for (Entry<URIAlignment, Double> e : hMap.entrySet()) {
//			double weight = e.getValue();
//			
//			URI onto1URI = e.getKey().getOntology1URI();
//			URI onto2URI = e.getKey().getOntology2URI();
//			
//			//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
//			temp_harmonyAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
//			
//			for (Cell c : e.getKey()) {
//				temp_harmonyAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength()*weight);
//			}
//		}
//		
//
//		
//		//remove relations with 0 confidence
//		AlignmentOperations.removeZeroConfidenceRelations(temp_harmonyAlignment);
//		URIAlignment harmonyAlignment = (URIAlignment) temp_harmonyAlignment.clone();
//		
//		//normalize the confidence of all cells between [0..1]
//		//AlignmentOperations.normalizeConfidence(harmonyAlignment);
//		
//		return harmonyAlignment;
//	}
	
	
	/**
	 * Computes the harmony relations from an initial alignment. The harmony relations are those relations that in a similarity matrix has the highest confidence values both row-wise and column-wise
	 * @param alignmentFile
	 * @return
	 * @throws AlignmentException
	   Feb 1, 2019
	 * @throws IOException 
	 */
	private static Map<URIAlignment, Double> getHarmonyValue(File alignmentFile) throws AlignmentException, IOException {
		
		Map<URIAlignment, Double> harmonyMap = new HashMap<URIAlignment, Double>();
		
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment)parser.parse(alignmentFile.toURI().toString());

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URIAlignment harmonyAlignment = new URIAlignment();
		harmonyAlignment.init( inputAlignment.getOntology1URI(), inputAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );
		
		//used for calculating the Harmony value
		int numInitialRelations = inputAlignment.nbCells();
		
		
		//based Harmony value on non-null relations only
//		int nonZeroRelations = 0;
//		for (Cell c : inputAlignment) {
//			if (c.getStrength() > 0) {
//				nonZeroRelations++;
//			}
//		}
		
		//System.out.println("\nFor alignment " + alignmentFile.getName() + " there are " + numInitialRelations + " total possible relations in the inputAlignment");
//		System.out.println("\nFor alignment " + alignmentFile.getName() + " there are " + nonZeroRelations + " non-zero relations");
		
		//create similarity matrix
		Relation[][] similarityMatrix = createSimMatrix(inputAlignment);
		
		//ArrayList<Relation> harmonyRelations = new ArrayList<Relation>();
		ArrayList<Relation> rowMax = getRowMax(similarityMatrix);
		ArrayList<Relation> colMax = getColMax(similarityMatrix);
		
		for (Relation r : rowMax) {
			for (Relation c : colMax) {
				if (r.getConcept1().equals(c.getConcept1()) && 
						r.getConcept2().equals(c.getConcept2()) && 
						r.getRelationType().equals(c.getRelationType()) && 
						r.getConfidence() == c.getConfidence()) {
					harmonyAlignment.addAlignCell(r.getId(), URI.create(r.getConcept1()), URI.create(r.getConcept2()), r.getRelationType(), r.getConfidence());
					//harmonyRelations.add(r);
				}
			}
		}
		
		int numFinalRelations = harmonyAlignment.nbCells();
		
		double harmonyValue = (double) numFinalRelations / (double) numInitialRelations;
		
		harmonyMap.put(harmonyAlignment, harmonyValue);
		
		//System.out.println("For alignment " + alignmentFile.getName() + " there are " + numFinalRelations + " harmony relations (best across rows and columns)");
		//System.out.println("The Harmony-value of alignment: " + alignmentFile.getName() + " is therefore " + harmonyValue);
		//System.out.println("Printing Harmony-relations for " + alignmentFile.getName());
		//for (Cell c : harmonyAlignment) {
		//	System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		//}
				
		return harmonyMap;
		
	}
	
	/**
	 * Computes the harmony relations from an initial alignment. The harmony relations are those relations that in a similarity matrix has the highest confidence values both row-wise and column-wise
	 * @param alignmentFile
	 * @return
	 * @throws AlignmentException
	   Feb 1, 2019
	 * @throws IOException 
	 */
//	public static URIAlignment getHarmonyAlignment(File alignmentFile) throws AlignmentException, IOException {
//		
//		AlignmentParser parser = new AlignmentParser();
//		BasicAlignment inputAlignment = (BasicAlignment)parser.parse(alignmentFile.toURI().toString());
//
//		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
//		URIAlignment harmonyAlignment = new URIAlignment();
//		harmonyAlignment.init( inputAlignment.getOntology1URI(), inputAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );
//		
//		
//		//create similarity matrix
//		Relation[][] similarityMatrix = createSimMatrix(inputAlignment);
//		
//		ArrayList<Relation> harmonyRelations = new ArrayList<Relation>();
//		ArrayList<Relation> rowMax = getRowMax(similarityMatrix);
//		ArrayList<Relation> colMax = getColMax(similarityMatrix);
//		
//		for (Relation r : rowMax) {
//			for (Relation c : colMax) {
//				if (r.getConcept1().equals(c.getConcept1()) && 
//						r.getConcept2().equals(c.getConcept2()) && 
//						r.getRelationType().equals(c.getRelationType()) && 
//						r.getConfidence() == c.getConfidence()) {
//					harmonyAlignment.addAlignCell(URI.create(r.getConcept1()), URI.create(r.getConcept2()), r.getRelationType(), r.getConfidence());
//					harmonyRelations.add(r);
//				}
//			}
//		}
//				
//		return harmonyAlignment;
//		
//	}
	
	/**
	 * Computes the harmony relations from an initial alignment. The harmony relations are those relations that in a similarity matrix has the highest confidence values both row-wise and column-wise
	 * @param alignmentFile
	 * @return
	 * @throws AlignmentException
	   Feb 1, 2019
	 * @throws IOException 
	 */
	public static BasicAlignment getHarmonyAlignment(BasicAlignment inputAlignment) throws AlignmentException, IOException {

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URIAlignment harmonyAlignment = new URIAlignment();
		harmonyAlignment.init( inputAlignment.getOntology1URI(), inputAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );

		//create similarity matrix
		Relation[][] similarityMatrix = createSimMatrix(inputAlignment);
		
		ArrayList<Relation> harmonyRelations = new ArrayList<Relation>();
		ArrayList<Relation> rowMax = getRowMax(similarityMatrix);
		ArrayList<Relation> colMax = getColMax(similarityMatrix);
		
//		System.out.println("\nrowMax size: " + rowMax.size());
//		System.out.println("Best in row are: ");
//		for (Relation r : rowMax) {
//			System.out.println(r.getConcept1() + " " + r.getConcept2() + " " + r.getConfidence());
//		}
//		
//		System.out.println("\ncolMax size: " + colMax.size());
//		System.out.println("Best in col are: ");
//		for (Relation r : colMax) {
//			System.out.println(r.getConcept1() + " " + r.getConcept2() + " " + r.getConfidence());
//		}

		
		for (Relation r : rowMax) {
			for (Relation c : colMax) {
			
				if (r.getConcept1().equals(c.getConcept1()) && 
						r.getConcept2().equals(c.getConcept2()) && 
						r.getRelationType().equals(c.getRelationType()) && 
						r.getConfidence() == c.getConfidence()) {
					//System.out.println(r.getConcept1() + " equals " + c.getConcept1() + ", " + r.getConcept2() + " equals " + c.getConcept2() + ", " + r.getRelationType() + ", " + c.getRelationType() + ", " + r.getConfidence() + " equals " + c.getConfidence() );
					harmonyAlignment.addAlignCell(r.getId(), URI.create(r.getConcept1().replaceAll("[<|>]", "")), URI.create(r.getConcept2().replaceAll("[<|>]", "")), r.getRelationType(), r.getConfidence());
					harmonyRelations.add(r);
				}
			}
		}
				
		return harmonyAlignment;
		
	}
	
	
	public URIAlignment combineHarmonyAlignments(String folderName) throws AlignmentException {
		URIAlignment combinedHarmonyAlignment = new URIAlignment();
		
		//combine all alignments into a single alignment
		combinedHarmonyAlignment = AlignmentOperations.combineAlignments(folderName);
		
		
		return combinedHarmonyAlignment;
	}
	

	/**
	 * Creates a matrix from an alignment where the sources and targets are alphabetically sorted to represent them properly in a similarity matrix
	 * @param a input alignment
	 * @return matrix (2D array) holding Relation objects (concept1, concept2, relation type, confidence)
	 * @throws AlignmentException
	   Jan 30, 2019
	 * @throws IOException 
	 */
	private static Relation[][] createSimMatrix (Alignment a) throws AlignmentException, IOException {

		ArrayList<Relation> relArray = new ArrayList<Relation>();
		Relation rel = null;

		for (Cell c : a) {
			relArray.add(rel = new Relation(c.getId(), c.getObject1().toString().replaceAll("[<|>]", ""), c.getObject2().toString().replaceAll("[<|>]", ""), c.getRelation().getRelation(), c.getStrength()));
		}

		Collections.sort(relArray, new RelationComparator());

		int numDistinctSources = getNumDistinctSources(relArray);
		int numDistinctTargets = getNumDistinctTargets(relArray);

		Relation[][] simMatrix = new Relation[numDistinctSources][numDistinctTargets];
		int temp = 0;
		for (int i = 0; i < numDistinctSources;i++) {
			for (int j = 0; j < numDistinctTargets; j++) {
				simMatrix[i][j] = relArray.get(temp);
				temp++;
			}
		}
		
		//print matrix
		File outputFile = new File("./files/Harmony.txt");
		
		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputFile)), true); 
		
		for (int i = 0; i < simMatrix.length; i++) {
		    for (int j = 0; j < simMatrix[i].length; j++) {
		        writer.print(simMatrix[i][j] + " ");
		    }
		    writer.println();
		}
		
		writer.flush();
		writer.close();

		return simMatrix;
	}

	/**
	 * Retrieves the relations that has the highest confidence value row-wise from a similarity matrix
	 * @param matrix a similarity matrix where source concepts of the relation is represented in rows and target concepts are represented in columns
	 * @return an ArrayList<Relation> with the relations having the highest confidence values row-wise
	   Feb 1, 2019
	 */
	private static ArrayList<Relation> getRowMax (Relation[][] matrix) {

		ArrayList<Relation> rowMaxes = new ArrayList<Relation>();
		Relation rel = new Relation();

		//each row
		for (int row = 0; row < matrix.length; row++) {
			//System.out.println("Row : " + row);
			double max = 0;

			//each cell in row
			for (int col = 1; col < matrix[row].length; col++) {
				//System.out.println("Column: " + col);
				
				//System.out.println("The confidence of " + matrix[row][col].getConcept1() + " " + matrix[row][col].getConcept2() + " is " + matrix[row][col].getConfidence());
				if (matrix[row][col].getConfidence() > max) {

					//System.out.println("The confidence of " + matrix[row][col].getConcept1() + " " + matrix[row][col].getConcept2() + " " + matrix[row][col].getConfidence() + " is higher than " + max);
					max = matrix[row][col].getConfidence();
					rel = new Relation(matrix[row][col].getId(), matrix[row][col].getConcept1(), matrix[row][col].getConcept2(), matrix[row][col].getRelationType(), matrix[row][col].getConfidence());	

				}
			} 
			
			//System.out.println("Adding " + rel.getConcept1() + " " + rel.getConcept2()+ " " + rel.getRelationType()+ " " + rel.getConfidence() + " to rowMaxes");
			if (!rowMaxes.contains(rel) && rel.getConcept1() != null && rel.getConcept2() != null)
			rowMaxes.add(rel);
		
		}

		return rowMaxes;
	}



	/**
	 * Retrieves the relations that has the highest confidence value column-wise from a similarity matrix
	 * @param matrix matrix a similarity matrix where source concepts of the relation is represented in rows and target concepts are represented in columns
	 * @return an ArrayList<Relation> with the relations having the highest confidence values column-wise
	   Feb 1, 2019
	 */
	private static ArrayList<Relation> getColMax (Relation[][] matrix) {

		ArrayList<Relation> colMaxes = new ArrayList<Relation>();
		Relation rel = new Relation();
		
		//each column
		for (int col = 0; col < matrix[0].length; col++) {
			//System.out.println("Column: " + col);
			double max = 0;
			
			//each cell (row) in column
			for (int row = 0; row < matrix.length; row++) {
				//System.out.println("Row: " + row);
				
				//System.out.println("The confidence of " + matrix[row][col].getConcept1() + " " + matrix[row][col].getConcept2() + " is " + matrix[row][col].getConfidence());
				if (matrix[row][col].getConfidence() > max) {
					
					//System.out.println("The confidence of " + matrix[row][col].getConcept1() + " " + matrix[row][col].getConcept2() + " " + matrix[row][col].getConfidence() + " is higher than " + max);
					max = matrix[row][col].getConfidence();
					rel = new Relation(matrix[row][col].getId(), matrix[row][col].getConcept1(), matrix[row][col].getConcept2(), matrix[row][col].getRelationType(), matrix[row][col].getConfidence());	
				}
				
			}
			
			//System.out.println("Adding " + rel.getConcept1() + " " + rel.getConcept2()+ " " + rel.getRelationType()+ " " + rel.getConfidence() + " to colMaxes");
			if (!colMaxes.contains(rel) && rel.getConcept1() != null && rel.getConcept2() != null) 
				colMaxes.add(rel);
		}
			
			return colMaxes;
			
	}



	/**
	 * Helper-method that counts the number of distinct source objects from an alignment (a matrix)
	 * @param cellArray
	 * @return
	 * @throws AlignmentException
	   Feb 1, 2019
	 */
	private static int getNumDistinctSources (ArrayList<Relation> cellArray) throws AlignmentException {

		Set<String> sources = new HashSet<String>();

		for (Relation r : cellArray) {
			sources.add(r.getConcept1Fragment());
		}
		int numDistinctSources = sources.size();

		return numDistinctSources;

	}

	/**
	 * Helper-method that counts the number of distinct target objects from an alignment (a matrix)
	 * @param cellArray
	 * @return
	 * @throws AlignmentException
	   Feb 1, 2019
	 */
	private static int getNumDistinctTargets (ArrayList<Relation> cellArray) throws AlignmentException {

		Set<String> targets = new HashSet<String>();

		for (Relation r : cellArray) {
			targets.add(r.getConcept2Fragment());
		}
		int numDistinctTargets = targets.size();

		return numDistinctTargets;

	}



}
