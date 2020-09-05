package alignmentcombination;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;

public class MajorityVote {
	
	//test method
		public static void main(String[] args) throws AlignmentException, URISyntaxException {
			
			String folderName = "./files/_PHD_EVALUATION/OAEI2011/ALIGNMENTS/301302/INDIVIDUAL_ALIGNMENTS/EQUIVALENCE_NOWEIGHT";
			Map<Double, URIAlignment> majorityVoteAlignmentMap = getMajorityVotes(folderName);
			
			System.out.println("Printing majorityvotes");
			for (Entry<Double, URIAlignment> e : majorityVoteAlignmentMap.entrySet()) {
				System.out.println("\nConfidence: " + e.getKey());
				//System.out.println("Size of alignment: " + e.getValue().nbCells());
				for (Cell c : e.getValue()) {
					System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getStrength());
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
	public static Map<Double, URIAlignment> getMajorityVotes(String folderName) throws AlignmentException, URISyntaxException {
		Map<Double, URIAlignment> majorityVoteAlignmentMap = new HashMap<Double, URIAlignment>();

		AlignmentParser aparser = new AlignmentParser(0);

		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();
		URIAlignment thisAlignment = null;

		String URI = null;		

		double[] conf = {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};

		for (double d : conf) {
			
			ArrayList<URIAlignment> inputAlignments = new ArrayList<URIAlignment>();

			for (int i = 0; i < filesInDir.length; i++) {

				//get all files from folder having the same confidence
				if (filesInDir[i].getName().endsWith(d + ".rdf")) {

					URI = StringUtilities.convertToFileURL(folderName) + "/" + StringUtilities.stripPath(filesInDir[i].toString());

					thisAlignment = (URIAlignment) aparser.parse(new URI(URI));		
										
					inputAlignments.add(thisAlignment);		
					//System.err.println("Adding " + filesInDir[i].getName() + " to inputAlignments");

				}
		
			}
			
			//System.out.println("inputAlignments contain " + inputAlignments.size() + " alignments");
			majorityVoteAlignmentMap.put(d, majorityVote(inputAlignments));
			
		}
		
		return majorityVoteAlignmentMap;
	}
	
	/**
	 * Creates an alignment that includes correspondences that are computed by n-x matchers (e.g. 3 of 4 matchers)
	 * @param inputAlignments A list of all alignments produced by the matchers involved
	 * @return an alignment that includes the "voted" set of correspondences 
	 * @throws AlignmentException
	 */
	public static URIAlignment majorityVote(ArrayList<URIAlignment> inputAlignments) throws AlignmentException, URISyntaxException {

		URIAlignment simpleVoteAlignment = new URIAlignment();

		ArrayList<Cell> allCells = new ArrayList<Cell>();
		
		URI onto1URI = null;
		URI onto2URI = null;

		// get all cells in all alignments and put them in a set
		for (Alignment a : inputAlignments) {
			
			onto1URI = a.getOntology1URI();
			onto2URI = a.getOntology2URI();
			
			for (Cell c : a) {
				allCells.add(c);
			}
		}
				
		simpleVoteAlignment.init(onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		int numAlignments = inputAlignments.size();

		ArrayList<Cell> todel = new ArrayList<Cell>();
		ArrayList<Cell> toKeep = new ArrayList<Cell>();

		for (Cell currentCell : allCells) {
			
			if (!todel.contains(currentCell)) {
				
				// get all cells that has the same object1 as c1
				ArrayList<Cell> sameObj1 = new ArrayList<Cell>();				
				for (Cell c : allCells) {
					if (c.getObject1().equals(currentCell.getObject1())) {
						sameObj1.add(c);
					}
				}
							
				if (sameObj1.size() > 1) {
					
					// placeholder for cells that contains the same object1 and
					// object 2 as c1 AND that has the same relation type as currentCell
					ArrayList<Cell> toCheck = new ArrayList<Cell>();

					Object o2 = currentCell.getObject2();
					Relation rCurrent = currentCell.getRelation();

					//checking if the cells in sameObj1 also have the same object 2 as "currentCell", AND that their relation type is the same -> if so add the cells to "toCheck"
					for (Cell c2 : sameObj1) {
						if (o2.equals(c2.getObject2()) && rCurrent.equals(c2.getRelation())) {
							toCheck.add(c2);
						}

					}
										
					//if the number of cells in toCheck (those that have the same object1 and object 2 as currentCell) is represented by numAlignments-1 (e.g.3 of 4 alignments)
					if (toCheck.size() >= (numAlignments - 2)) {

						for (Cell c : toCheck) {

							//checking that c (this cell) in fact is not currentCell
							if (c != currentCell) {
								toKeep.add(c);
								todel.add(currentCell);
								
							}
						}
					}

				} else {
					
				}
			}
		}
		

		for (Cell c : toKeep) {
			simpleVoteAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), StringUtilities.validateRelationType(c.getRelation().getRelation()),
					c.getStrength());
		}
		
		
		return simpleVoteAlignment;
	}

}
