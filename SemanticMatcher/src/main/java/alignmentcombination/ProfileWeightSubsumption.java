package alignmentcombination;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.AlignmentOperations;
import utilities.Relation;
import utilities.RelationComparator;
import utilities.RelationComparatorConfidence;

public class ProfileWeightSubsumption {

	
	//used for evaluation of sigmoid parameters
		public static URIAlignment computeProfileWeightingSubsumption(ArrayList<URIAlignment> inputAlignments) throws AlignmentException, IOException, URISyntaxException {
			Set<URIAlignment> alignmentSet = new HashSet<URIAlignment>();
			URIAlignment highestCellAlignment = new URIAlignment();
			URIAlignment profileWeightingSubsumptionAlignment = new URIAlignment();						

			//for each alignment produced by an individual matcher
			for (URIAlignment a : inputAlignments) {
								
				highestCellAlignment.init( a.getOntology1URI(), a.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );	

				highestCellAlignment = getHighestCells(a);
				
				//remove relations that have 0 confidence
				AlignmentOperations.removeZeroConfidenceRelations(highestCellAlignment);
				alignmentSet.add(highestCellAlignment);

			}
			
			URI onto1URI = null;
			URI onto2URI = null;
			
			for (URIAlignment a : alignmentSet) {
				onto1URI = a.getOntology1URI();
				onto2URI = a.getOntology2URI();
				for (Cell c : a) {
					profileWeightingSubsumptionAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation(), c.getStrength());
				}
			}
						
			profileWeightingSubsumptionAlignment.init(onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

			return profileWeightingSubsumptionAlignment;
		}

	
	public static URIAlignment computeProfileWeightingSubsumption(String folderName) throws AlignmentException, IOException {
		System.out.println("\nStarting the Profile Weight method");
		Set<URIAlignment> alignmentSet = new HashSet<URIAlignment>();
		URIAlignment inputAlignment = new URIAlignment();
		URIAlignment highestCellAlignment = new URIAlignment();
		URIAlignment profileWeightingSubsumptionAlignment = new URIAlignment();
		AlignmentParser parser = new AlignmentParser();

		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();

		int counter = 0;
		for (int i = 0; i < filesInDir.length; i++) {
			
			//only the 0.0 cut threshold files should be considered
			if (filesInDir[i].getName().endsWith("0.0.rdf")) {

				parser = new AlignmentParser();
				inputAlignment = (URIAlignment)parser.parse(filesInDir[i].toURI().toString());

				highestCellAlignment.init( inputAlignment.getOntology1URI(), inputAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );		
				
				highestCellAlignment = getHighestCells(inputAlignment);

				//remove relations that have 0 confidence				
				AlignmentOperations.removeZeroConfidenceRelations(highestCellAlignment);
				alignmentSet.add(highestCellAlignment);

			}
			
			counter++;

		}
		
		URI onto1URI = null;
		URI onto2URI = null;
		
		for (URIAlignment a : alignmentSet) {
			
			if (a.getOntology1URI() != null && a.getOntology2URI() != null) {
				onto1URI = a.getOntology1URI();
				onto2URI = a.getOntology2URI();
			}
			
			for (Cell c : a) {
				profileWeightingSubsumptionAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation(), c.getStrength());
			}
		}
		
		profileWeightingSubsumptionAlignment.init( inputAlignment.getOntology1URI(), inputAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );

		return profileWeightingSubsumptionAlignment;
	}


	/**
	 * Extracts the "highest cells" from an initial alignment. The "highest cells" are those relations that in a similarity matrix has the highest confidence values both row-wise and column-wise
	 * @param alignmentFile
	 * @return
	 * @throws AlignmentException
	   Feb 1, 2019
	 * @throws IOException 
	 */
	public static URIAlignment getHighestCells(URIAlignment inputAlignment) throws AlignmentException, IOException {

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URIAlignment highestCellsAlignment = new URIAlignment();
		highestCellsAlignment.init( inputAlignment.getOntology1URI(), inputAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );
		
		//create similarity matrix
		Relation[][] similarityMatrix = createSimMatrix(inputAlignment);
		
		ArrayList<Relation> rowMax = getRowMax(similarityMatrix);

		ArrayList<Relation> colMax = getColMax(similarityMatrix);
		
		//remove the relations with zero confidence, they are not needed and slows the process significantly 
		removeZeroConfidenceRelations(rowMax);
		removeZeroConfidenceRelations(colMax);

		for (Relation r : rowMax) {
			for (Relation c : colMax) {

				if (r.getConcept1().equals(c.getConcept1()) && 
						r.getConcept2().equals(c.getConcept2()) && 
						r.getRelationType().equals(c.getRelationType()) && 
						r.getConfidence() == c.getConfidence()) {
					highestCellsAlignment.addAlignCell(r.getId(), URI.create(r.getConcept1().replaceAll("[<|>]", "")), URI.create(r.getConcept2().replaceAll("[<|>]", "")), r.getRelationType(), r.getConfidence());
				}
			}
		}
		
		return highestCellsAlignment;

	}
	
	private static void removeZeroConfidenceRelations(ArrayList<Relation> relationsList) {

		Iterator<Relation> itr = relationsList.iterator();
		
		while (itr.hasNext()) {
			
			Relation r = itr.next();
			
			if (r.getConfidence() == 0.0) {
				itr.remove();
			}
			
		}
		
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
		//		File outputFile = new File("./files/Harmony.txt");
		//		
		//		PrintWriter writer = new PrintWriter(
		//				new BufferedWriter(
		//						new FileWriter(outputFile)), true); 
		//		
		//		for (int i = 0; i < simMatrix.length; i++) {
		//		    for (int j = 0; j < simMatrix[i].length; j++) {
		//		        writer.print(simMatrix[i][j] + " ");
		//		    }
		//		    writer.println();
		//		}
		//		
		//		writer.flush();
		//		writer.close();

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

		//each row
		for (int row = 0; row < matrix.length; row++) {
			ArrayList<Relation> allRels = new ArrayList<Relation>();
			ArrayList<Relation> rowMaxPerCol = new ArrayList<Relation>();	

			//each cell in row
			for (int col = 0; col < matrix[row].length; col++) {
				
				allRels.add(matrix[row][col]);

			} 
			
			//get the max relations
			rowMaxPerCol = getMaxRelations(allRels);
			
			rowMaxes.addAll(rowMaxPerCol);

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
		
		
		//each column
		for (int col = 0; col < matrix[0].length; col++) {
			ArrayList<Relation> allRels = new ArrayList<Relation>();
			ArrayList<Relation> colMaxPerRow = new ArrayList<Relation>();			
			
			//each cell (row) in column
			for (int row = 0; row < matrix.length; row++) {

				//add all relations from the matrix in a map
				allRels.add(matrix[row][col]);
				
			}
						
			//get the max relations
			colMaxPerRow = getMaxRelations(allRels);

			colMaxes.addAll(colMaxPerRow);
		}


		return colMaxes;

	}
	
	private static ArrayList<Relation> getMaxRelations (ArrayList<Relation> allRelations) {

		ArrayList<Relation> maxRelations = new ArrayList<Relation>();
		
		Collections.sort(allRelations, new RelationComparatorConfidence());
		
		double max = allRelations.get(0).getConfidence();
		
		for (int i = 0; i < allRelations.size(); i++) {
			
			if (allRelations.get(i).getConfidence() == max) {
				maxRelations.add(allRelations.get(i));
			}
		}		

		return maxRelations;

	}
	


	/**
	 * Helper-method that counts the number of distinct source objects from an alignment
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
	 * Helper-method that counts the number of distinct target objects from an alignment
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
