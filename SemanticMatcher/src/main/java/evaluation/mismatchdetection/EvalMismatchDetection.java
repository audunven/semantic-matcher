package evaluation.mismatchdetection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import de.unima.alcomox.exceptions.AlcomoException;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.ConceptScopeMismatch;
import mismatchdetection.DomainMismatch;
import mismatchdetection.IncoherenceDetection;
import mismatchdetection.StructureMismatch;
import net.didion.jwnl.JWNLException;
import utilities.StringUtilities;

public class EvalMismatchDetection {

	public static void main(String[] args) throws AlignmentException, URISyntaxException, AlcomoException, OWLOntologyCreationException, JWNLException, IOException {


		//import all alignments in folder
		String folderPath = "./files/KEOD18/datasets_refined/d6/combination/equivalence";
		String evalPath = "./files/KEOD18/datasets_refined/d6/MismatchDetection";
		String referenceAlignmentPath = "./files/KEOD18/datasets_refined/d6/refalign/ref-align_aixm-obstacle-airm-mono-Equivalence.rdf";

		//ontologies involved
		String onto1File = "./files/KEOD18/datasets_refined/d6/ontologies/aixm_obstacle.owl";
		String onto2File = "./files/KEOD18/datasets_refined/d6/ontologies/airm-mono.owl";

		//get the reference alignment
		AlignmentParser aparser = new AlignmentParser(0);
		Alignment referenceAlignment = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentPath)));


		File folder = new File(folderPath);
		File[] filesInDir = folder.listFiles();

		//URIAlignment alcomoMismatchAlignment = new URIAlignment();
		URIAlignment conceptScopeMismatchAlignment = new URIAlignment();
		URIAlignment structureMismatchAlignment = new URIAlignment();
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

			//Run concept scope mismatch detection
			conceptScopeMismatchAlignment = ConceptScopeMismatch.detectConceptScopeMismatch(thisAlignment);
			Evaluator.evaluateSingleAlignment("Concept Scope Mismatch", conceptScopeMismatchAlignment, referenceAlignmentPath, evalPath + "ConceptScopeMismatch_" + filesInDir[i].getName()  + ".txt");
			int conceptScopeNumCells = conceptScopeMismatchAlignment.nbCells();

			//print alert to screen if some relations are removed
			if (conceptScopeNumCells != numInitialCells) {
				System.err.println("The Concept Scope Mismatch Detection identified a mismatch in alignment " + filesInDir[i].getName());
			}

			//Run categorisation mismatch detection
			structureMismatchAlignment = StructureMismatch.detectStructureMismatches(conceptScopeMismatchAlignment, new File(onto1File), new File(onto2File));
			Evaluator.evaluateSingleAlignment("Structure Mismatch", structureMismatchAlignment, referenceAlignmentPath, evalPath + "StructureMismatch_" + filesInDir[i].getName() + ".txt");
			int structureMismatchNumCells = structureMismatchAlignment.nbCells();

			//print alert to screen if some relations are removed
			if (structureMismatchNumCells < conceptScopeNumCells) {
				System.err.println("The Structure Mismatch Detection identified a mismatch in alignment " + filesInDir[i].getName());
			}

			//Run WNDomains mismatch detection
			domainsMismatchAlignment = DomainMismatch.filterAlignment(structureMismatchAlignment);
			Evaluator.evaluateSingleAlignment("Domains Mismatch", domainsMismatchAlignment, referenceAlignmentPath, evalPath + "DomainMismatch_" + filesInDir[i].getName() + ".txt");
			int domainMismatchNumCells = domainsMismatchAlignment.nbCells();

			//print alert to screen if some relations are removed
			if (domainMismatchNumCells < structureMismatchNumCells) {
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
