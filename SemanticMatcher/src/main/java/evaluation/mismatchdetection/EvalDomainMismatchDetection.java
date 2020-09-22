package evaluation.mismatchdetection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.DomainMismatch;
import rita.wordnet.jwnl.JWNLException;
import utilities.StringUtilities;

/**
 * Produces an evaluation summary of the mismatch detection strategies Concept Scope Mismatch, Structure Mismatch Detection and Domain Mismatch Detection and stores their refined alignments.
 * @author audunvennesland
 *
 */
public class EvalDomainMismatchDetection {

	public static void main(String[] args) throws AlignmentException, URISyntaxException, OWLOntologyCreationException, JWNLException, IOException {


		//import all alignments in folder
		String folderPath = "./files/_PHD_EVALUATION/EVALUATION_DOMAIN_MISMATCH_DETECTION/SUB/CD";
		String evalPath = "./files";
		String referenceAlignmentPath = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUBSUMPTION.rdf";

		//ontologies involved
		String onto1File = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf";
		String onto2File = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl";

		//get the reference alignment
		AlignmentParser aparser = new AlignmentParser(0);
		Alignment referenceAlignment = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentPath)));


		File folder = new File(folderPath);
		File[] filesInDir = folder.listFiles();


		URIAlignment domainsMismatchAlignment = new URIAlignment();

		URIAlignment thisAlignment = null;

		int numInitialCells = 0;
		int numFinalCells = 0;

		PrintWriter writer = null;
		AlignmentVisitor renderer = null;
		String alignmentFileName = null;
		File outputAlignment = null;


		//for each alignment file in folder
		for (int i = 0; i < filesInDir.length; i++) {

			//initial alignment file
			String URI = StringUtilities.convertToFileURL(folderPath) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			System.out.println("\nEvaluating file " + URI);
			thisAlignment = (URIAlignment) aparser.parse(new URI(URI));		

			numInitialCells = thisAlignment.nbCells();
			System.out.println("There are " + numInitialCells + " relations in the initial alignment");

			Evaluator.evaluateSingleAlignment(URI, thisAlignment, referenceAlignmentPath, evalPath + "ConceptScopeMismatch_" + filesInDir[i].getName()  + "_initial.txt");


			//Run WNDomains mismatch detection
			domainsMismatchAlignment = DomainMismatch.filterAlignment(thisAlignment);
			Evaluator.evaluateSingleAlignment("Domains Mismatch", domainsMismatchAlignment, referenceAlignmentPath, evalPath + "DomainMismatch_" + filesInDir[i].getName() + ".txt");
			int domainMismatchNumCells = domainsMismatchAlignment.nbCells();

			//print alert to screen if some relations are removed
			if (domainMismatchNumCells < numInitialCells) {
				System.err.println("The Domains Mismatch Detection identified a mismatch in alignment " + filesInDir[i].getName());
			}

			numFinalCells = domainsMismatchAlignment.nbCells();
			System.out.println("There are " + numFinalCells + " relations in the filtered alignment");

			//store the filtered alignment
			alignmentFileName = evalPath + "Filtered_" + filesInDir[i].getName();

			outputAlignment = new File(alignmentFileName);

			writer = new PrintWriter(
					new BufferedWriter(
							new FileWriter(outputAlignment)), true); 
			renderer = new RDFRendererVisitor(writer);

			domainsMismatchAlignment.render(renderer);
			writer.flush();
			writer.close();

		}

	}

}
