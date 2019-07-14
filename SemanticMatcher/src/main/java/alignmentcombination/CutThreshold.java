package alignmentcombination;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;

public class CutThreshold {

	//test method
	public static void main(String[] args) throws AlignmentException, URISyntaxException, IOException {

		//folder with non-weighted alignments
		String folderName = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/INDIVIDUAL_ALIGNMENTS/SUBSUMPTION_SIGMOID";
		
		//produce excel file with thresholds

		Map<Double, URIAlignment> aggregatedAlignments = cutThresholdAlignment(folderName);

		for (Entry<Double, URIAlignment> e : aggregatedAlignments.entrySet()) {
			System.out.println("\nConfidence: " + e.getKey());
			for (Cell c : e.getValue()) {

				System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " "  + c.getRelation().getRelation() + " " + c.getStrength());
			}
		}

	}




	public static Map<Double, URIAlignment> cutThresholdAlignment (String folderName) throws AlignmentException, URISyntaxException {

		Map<Double, URIAlignment> cutThresholdAlignmentMap = new HashMap<Double, URIAlignment>();


		AlignmentParser aparser = new AlignmentParser(0);

		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();
		URIAlignment thisAlignment = null;

		String URI = null;


		double[] conf = {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};

		for (double d : conf) {

			ArrayList<Alignment> inputAlignments = new ArrayList<Alignment>();

			for (int i = 0; i < filesInDir.length; i++) {

				if (filesInDir[i].getName().endsWith(d + ".rdf")) {

					URI = StringUtilities.convertToFileURL(folderName) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
					thisAlignment = (URIAlignment) aparser.parse(new URI(URI));
					inputAlignments.add(thisAlignment);
					//System.err.println("Adding " + filesInDir[i].getName() + " to inputAlignments");

				}
			}

			cutThresholdAlignmentMap.put(d, combineAlignments(inputAlignments));

		}

		return cutThresholdAlignmentMap;

	}


	public static URIAlignment combineAlignments (ArrayList<Alignment> inputAlignments) throws AlignmentException, URISyntaxException {

		URIAlignment combinedAlignment = new URIAlignment();

		URI onto1URI = null;
		URI onto2URI = null;

		for (Alignment a : inputAlignments) {

			onto1URI = a.getOntology1URI();
			onto2URI = a.getOntology2URI();

			for (Cell c : a) {

				combinedAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation(), c.getStrength());
			}
		}

		combinedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		return combinedAlignment;

	}

}
