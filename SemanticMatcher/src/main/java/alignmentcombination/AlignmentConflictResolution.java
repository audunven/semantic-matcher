package alignmentcombination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

public class AlignmentConflictResolution {

	//test method
	public static void main(String[] args) throws AlignmentException, URISyntaxException, IOException {


		String alignmentFile = "./files/_PHD_EVALUATION/MATCHERTESTING/Test_ConflictResolution.rdf";
		AlignmentParser aparser = new AlignmentParser(0);

		URIAlignment alignment = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(alignmentFile)));

		System.out.println("Printing original alignment");
		for (Cell c : alignment) {
			System.out.println(c.getId() + " " + c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getStrength());
		}

		URIAlignment noConflictAlignment = resolveAlignmentConflict(alignment);

		alignment.normalise();

		System.out.println("\nPrinting ndeAlignment");
		for (Cell c : noConflictAlignment) {
			System.out.println(c.getId() + " " + c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getStrength());
		}

		//store the conflict resolved alignment
		File outputAlignment = new File("./files/_PHD_EVALUATION/MATCHERTESTING/EvalMergedAlignment/Output_Test_ConflictResolution.rdf");

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		noConflictAlignment.render(renderer);

		writer.flush();
		writer.close();

	}


	public static URIAlignment resolveAlignmentConflict (URIAlignment alignment) throws AlignmentException {

		URIAlignment extractedAlignment = new URIAlignment();

		List<Cell> cellsList = AlignmentOperations.sortAlignment(alignment);

		List<Cell> extractionList = new LinkedList<Cell>();

		Iterator<Cell> itr = cellsList.iterator();

		Set<Cell> removed = new HashSet<Cell>();

		while (itr.hasNext()) {

			Cell thisCell = itr.next();

			//System.out.println("\nthisCell is " + thisCell.getId());

			if (!removed.contains(thisCell)) {

				List<Cell> alternatives = getSameObjectsAlternatives(cellsList, thisCell);

				for (Cell alt : alternatives) {

					if (alt.getStrength() == thisCell.getStrength()) {

						//System.out.println("...." + thisCell.getId() + " and " + alt.getId() + " contain the same objects and the same confidence");

						//check the profile scores and add the cell with the highest profile score to the extractionList
						//TODO: include the profile score along with the matcher´s identifier in the matching algorithm, e.g. WordEmbeddingMatcher123_0.80_ so that we can easily extract the profile score (0.80). 
						//ideally the Alignment Format could be extended to allow for a separate tag for this...
						double object1ProfileScore = Double.parseDouble(getProfileScore(thisCell.getId()));
						double object2ProfileScore = Double.parseDouble(getProfileScore(alt.getId()));

						//TODO: improve this functionality, in principle there might be more than two cells having the same objects and the same confidence
						if (object1ProfileScore > object2ProfileScore) {
							//System.out.println("......Adding " + thisCell.getId() + " to extractionList");
							extractionList.add(thisCell);
							removed.add(alt);
						} else {
							//System.out.println("......Adding " + alt.getId() + " to extractionList");
							extractionList.add(alt);
							removed.add(thisCell);

						}

					}

					if (!removed.contains(alt)) {
						removed.add(alt);
					}

				}

				if (!removed.contains(thisCell)) {
					extractionList.add(thisCell);
					//System.out.println("Adding " + thisCell.getId() + " to extractionList");
				}
				//removed.add(thisCell);

			}

			itr.remove();

		}

		for (Cell c : extractionList) {
			extractedAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
		}

		extractedAlignment.init(alignment.getOntology1URI(), alignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );

		return extractedAlignment;


	}


	/**
	 * Retrieves a list of cells that have BOTH the same object1 or same object2 as the cell c
	 * @param cellsList
	 * @param c
	 * @return
	   May 2, 2019
	 */
	public static List<Cell> getSameObjectsAlternatives (List<Cell> cellsList, Cell c) {

		List<Cell> sameObjects = new LinkedList<Cell>();


		for (Cell thisCell : cellsList) {
			if (!thisCell.equals(c) && thisCell.getObject1().equals(c.getObject1()) && thisCell.getObject2().equals(c.getObject2())) {
				sameObjects.add(thisCell);
			}
		}

		List<Cell> alternatives = new LinkedList<Cell>();
		alternatives.addAll(sameObjects);

		return alternatives;

	}

	private static String getProfileScore (String id) {

		String [] splitted = id.split("_");

		String profileScore = splitted[splitted.length-1];

		return profileScore;

	}

}



