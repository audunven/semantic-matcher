package alignmentcombination;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;

public class AverageAggregation {

	//test method
	public static void main(String[] args) throws AlignmentException, URISyntaxException {

		String folderName = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/INDIVIDUAL_ALIGNMENTS/EQUIVALENCE_NOWEIGHT";
		
		Map<Double, URIAlignment> aggregatedAlignments = getAverageAggregatedAlignmentMap(folderName);
		
		for (Entry<Double, URIAlignment> e : aggregatedAlignments.entrySet()) {
			System.out.println("\nConfidence: " + e.getKey());
			for (Cell c : e.getValue()) {
				
				System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " "  + c.getRelation().getRelation() + " " + c.getStrength());
			}
		}
		
	}

	/**
	 * Returns an alignment that consists of a set of relations where the strength is averaged across all input alignments 
	 * @param folderName
	 * @return
	 * @throws AlignmentException
	 * @throws URISyntaxException
		   Mar 11, 2019
	 */
	public static Map<Double, URIAlignment> getAverageAggregatedAlignmentMap(String folderName) throws AlignmentException, URISyntaxException {
		Map<Double, URIAlignment> averageAggregatedAlignmentMap = new HashMap<Double, URIAlignment>();


		AlignmentParser aparser = new AlignmentParser(0);

		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();
		URIAlignment thisAlignment = null;

		String URI = null;


		double[] conf = {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};

		for (double d : conf) {
			
			//create an ArrayList of all alignments in folder
			ArrayList<URIAlignment> inputAlignments = new ArrayList<URIAlignment>();

			for (int i = 0; i < filesInDir.length; i++) {

				if (filesInDir[i].getName().endsWith(d + ".rdf")) {

					URI = StringUtilities.convertToFileURL(folderName) + "/" + StringUtilities.stripPath(filesInDir[i].toString());

					thisAlignment = (URIAlignment) aparser.parse(new URI(URI));

					inputAlignments.add(thisAlignment);
					
				}
			}
			
			System.out.println("Adding " + inputAlignments.size() + " alignments for confidence " + d + " to averageAggregatedAlignmentMap");
			averageAggregatedAlignmentMap.put(d, getAverageAggregatedAlignment(inputAlignments));

		}
		
		return averageAggregatedAlignmentMap;
	}
	
	/**
	 * Returns an alignment that consists of a set of relations where the strength is averaged across all input alignments 
	 * @param folderName
	 * @return
	 * @throws AlignmentException
	 * @throws URISyntaxException
		   Mar 11, 2019
	 */
	public static Map<Double, URIAlignment> getAverageAggregatedAlignmentMap(ArrayList<URIAlignment> alignments) throws AlignmentException, URISyntaxException {
		
		Map<Double, URIAlignment> averageAggregatedAlignmentMap = new HashMap<Double, URIAlignment>();

		double[] conf = {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};

		for (double d : conf) {
			
			averageAggregatedAlignmentMap.put(d, getAverageAggregatedAlignment(alignments));

		}
		
		return averageAggregatedAlignmentMap;
	}

	/**
	 * Returns an alignment that consists of a set of relations where the strength is averaged across all input alignments 
	 * @param folderName
	 * @return
	 * @throws AlignmentException
	 * @throws URISyntaxException
		   Mar 11, 2019
	 */
	public static URIAlignment getAverageAggregatedAlignment(ArrayList<URIAlignment> inputAlignments) throws AlignmentException, URISyntaxException {
		URIAlignment aggregatedAlignment = new URIAlignment();

		int numCellsWithSameRel = 0;

		ArrayList<Cell> allCells = new ArrayList<Cell>();
		
		URI onto1URI = null;
		URI onto2URI = null;

		// get all cells in all alignments and put them in a set
		for (Alignment a : inputAlignments) {
			// for all cells C in each input alignment
			
			onto1URI = a.getOntology1URI();
			onto2URI = a.getOntology2URI();
			
			for (Cell c : a) {
				allCells.add(c);
			}
		}

		int counter = 0;
		ArrayList<Cell> processed = new ArrayList<Cell>();
		ArrayList<Cell> toKeep = new ArrayList<Cell>();
		double thisStrength;
		double averageStrength;

		for (Cell currentCell : allCells) {			
			counter++;
			//System.out.println("Processing cell " + counter + " of " + allCellsSize);

			//get the strength of currentCell
			thisStrength = 0;
			averageStrength = 0;

			if (!processed.contains(currentCell)) {

				// get all cells that has the same object1 as currentCell
				ArrayList<Cell> sameObj1 = new ArrayList<Cell>();				
				for (Cell c : allCells) {
					if (c.getObject1().equals(currentCell.getObject1())) {
						sameObj1.add(c);
					}
				}

				//why bigger than 1 and not?
				if (sameObj1.size() > 1) {

					// placeholder for cells that contains the same object1 and
					// object 2 as currentCell AND that has the same relation type as currentCell
					ArrayList<Cell> sameObj2 = new ArrayList<Cell>();

					Object o2 = currentCell.getObject2();
					Relation rCurrent = currentCell.getRelation();

					//checking if the cells in sameObj1 also have the same object 2 as "currentCell", AND that their relation type is the same -> if so add the cells to "toCheck"
					for (Cell c2 : sameObj1) {
						if (o2.equals(c2.getObject2()) && rCurrent.equals(c2.getRelation())) {
							sameObj2.add(c2);
						}

					}

					//if toCheck is not null or an empty set
					if (sameObj2 != null && !sameObj2.isEmpty()) {

						//how many other cells have the same relation as currentCell?
						numCellsWithSameRel = sameObj2.size();
						for (Cell c : sameObj2) {
							thisStrength += c.getStrength();

							//checking that c (this cell) in fact is not currentCell
							if (c != currentCell) {
								toKeep.add(c);
								processed.add(currentCell);

							}
						}

						averageStrength = thisStrength / (double) numCellsWithSameRel;
						aggregatedAlignment.addAlignCell(currentCell.getId(), currentCell.getObject1(), currentCell.getObject2(), currentCell.getRelation(), averageStrength);
					}				

				} else {

					//there are no other relations that have the same objects and relation as currentCell, so no confidence values to average...
					aggregatedAlignment.addAlignCell(currentCell.getId(), currentCell.getObject1(), currentCell.getObject2(), currentCell.getRelation(), currentCell.getStrength());
				}
			}

		}
		counter++;
		
		aggregatedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		return aggregatedAlignment;
	}
	

	

}
