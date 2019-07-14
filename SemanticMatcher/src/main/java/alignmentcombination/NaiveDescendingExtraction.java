package alignmentcombination;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

public class NaiveDescendingExtraction {

	//test method
	public static void main(String[] args) throws AlignmentException, URISyntaxException {

		String alignmentFile = "./files/BACKUP/TestAverageAggregation1.rdf";
		AlignmentParser aparser = new AlignmentParser(0);

		URIAlignment alignment = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(alignmentFile)));

		System.out.println("Printing original alignment");
		for (Cell c : alignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getStrength());
		}

		URIAlignment ndeAlignment = extractOneToOneRelations(alignment);
		
		alignment.normalise();

		System.out.println("\nPrinting ndeAlignment");
		for (Cell c : alignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getStrength());
		}

	}


	public static URIAlignment extractOneToOneRelations (URIAlignment alignment) throws AlignmentException {

		URIAlignment extractedAlignment = new URIAlignment();

		List<Cell> cellsList = AlignmentOperations.sortAlignment(alignment);

		List<Cell> extractionList = new LinkedList<Cell>();

		Iterator<Cell> itr = cellsList.iterator();
		Set<Cell> removed = new HashSet<Cell>();
		int counter = 0;

		while (itr.hasNext()) {
			//System.out.println("Running NDE on cell " + counter + " of a total of " + cellsList.size() + " cells");
			counter++;
			Cell thisCell = itr.next();

			if (!removed.contains(thisCell)) {
				extractionList.add(thisCell);
				itr.remove();
				
			}

			List<Cell> alternatives = getAlternatives(cellsList, thisCell);

			for (Cell alt: alternatives) {			
				if (!removed.contains(alt)) {
					removed.add(alt);
				}
			}

		}
		counter++;

		for (Cell c : extractionList) {
			extractedAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
		}

		extractedAlignment.init(alignment.getOntology1URI(), alignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );

		return extractedAlignment;


	}


	/**
	 * Retrieves a list of cells that have EITHER the same object1 or same object2 as the cell c
	 * @param cellsList
	 * @param c
	 * @return
	   May 2, 2019
	 */
	public static List<Cell> getAlternatives (List<Cell> cellsList, Cell c) {

		List<Cell> sameObject1 = new LinkedList<Cell>();
		List<Cell> sameObject2 = new LinkedList<Cell>();

		for (Cell thisCell : cellsList) {
			if (thisCell.getObject1().equals(c.getObject1())) {
				sameObject1.add(thisCell);
			}
		}

		for (Cell thisCell : cellsList) {
			if (thisCell.getObject2().equals(c.getObject2())) {
				sameObject2.add(thisCell);
			}
		}

		List<Cell> alternatives = new LinkedList<Cell>();
		alternatives.addAll(sameObject1);
		alternatives.addAll(sameObject2);

		return alternatives;

	}

}



