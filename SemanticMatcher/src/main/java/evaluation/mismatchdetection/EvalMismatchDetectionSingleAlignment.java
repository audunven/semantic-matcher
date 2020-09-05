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
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.ConceptScopeMismatch;
import mismatchdetection.DomainMismatch;
import mismatchdetection.StructureMismatch;
import rita.wordnet.jwnl.JWNLException;
import utilities.StringUtilities;

/**
 * Produces an evaluation summary of running the mismatch detection strategies Concept Scope Mismatch Detection, Structure Mismatch Detection and Domain Mismatch Detection on a single alignment.
 * @author audunvennesland
 *
 */
public class EvalMismatchDetectionSingleAlignment {

	public static void main(String[] args) throws AlignmentException, URISyntaxException, OWLOntologyCreationException, JWNLException, IOException {
		
		String folderPath = "./files/_PHD_EVALUATION/MISMATCH_DETECTION_EXAMPLE";
		String amlAlignment = "./files/_PHD_EVALUATION/MISMATCH_DETECTION_EXAMPLE/AML_CrossDomain_23062019.rdf";
		File alignmentFile = new File(amlAlignment);
		String referenceAlignmentPath = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";
		String mismatchStorePath = "./files/_PHD_EVALUATION/MISMATCH_DETECTION_EXAMPLE";
	
		
		//ontologies involved
		String onto1File = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf";
		String onto2File = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl";
		
		//get the reference alignment
		AlignmentParser aparser = new AlignmentParser(0);
		Alignment referenceAlignment = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentPath)));
		BasicAlignment initialAlignment = (BasicAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(amlAlignment)));

		//URIAlignment alcomoMismatchAlignment = new URIAlignment();
		URIAlignment conceptScopeMismatchAlignment = new URIAlignment();
		URIAlignment structureMismatchAlignment = new URIAlignment();
		URIAlignment domainsMismatchAlignment = new URIAlignment();
		
		//store the merged alignment
		File initialAlignmentFile = new File(mismatchStorePath + "/initialAlignment_crossdomain.rdf");
		File conceptScopeMismatchAlignmentFile = new File(mismatchStorePath + "/conceptScopeMismatch_crossdomain.rdf");
		File structureMismatchAlignmentFile = new File(mismatchStorePath + "/structureMismatch_crossdomain.rdf");
		File domainMismatchAlignmentFile = new File(mismatchStorePath + "/domainMismatch_crossdomain.rdf");
		
		
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = initialAlignment.getOntology1URI();
		URI onto2URI = initialAlignment.getOntology2URI();
		
		
		initialAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		int numInitialCells = 0;
		int numFinalCells = 0;
		
		PrintWriter writer = null;
		AlignmentVisitor renderer = null;
		String alignmentFileName = null;
		File outputAlignment = null;
		
			
		//initial alignment file
		String URI = StringUtilities.convertToFileURL(folderPath) + "/" + StringUtilities.stripPath(alignmentFile.toString());
		System.out.println("\nEvaluating file " + URI);
		initialAlignment = (URIAlignment) aparser.parse(new URI(URI));		
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(initialAlignmentFile)), true); 
		renderer = new RDFRendererVisitor(writer);

		initialAlignment.render(renderer);

		writer.flush();
		writer.close();
		
		numInitialCells = initialAlignment.nbCells();
		System.out.println("There are " + numInitialCells + " relations in the initial alignment");
			
		Evaluator.evaluateSingleAlignment(URI, initialAlignment, referenceAlignmentPath, mismatchStorePath + "ConceptScopeMismatch_" + alignmentFile.getName()  + "_initial.txt");
						
		//Run concept scope mismatch detection
		conceptScopeMismatchAlignment = ConceptScopeMismatch.detectConceptScopeMismatch(initialAlignment);
		Evaluator.evaluateSingleAlignment("Concept Scope Mismatch", conceptScopeMismatchAlignment, referenceAlignmentPath, mismatchStorePath + "ConceptScopeMismatch_" + alignmentFile.getName()  + ".txt");
		int conceptScopeNumCells = conceptScopeMismatchAlignment.nbCells();
		
		//print alert to screen if some relations are removed
		if (conceptScopeNumCells != numInitialCells) {
			System.err.println("The Concept Scope Mismatch Detection identified a mismatch in alignment " + alignmentFile.getName());
		}
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(conceptScopeMismatchAlignmentFile)), true); 
		renderer = new RDFRendererVisitor(writer);

		conceptScopeMismatchAlignment.render(renderer);

		writer.flush();
		writer.close();
		
		//Run categorisation mismatch detection
		structureMismatchAlignment = StructureMismatch.detectStructureMismatches(conceptScopeMismatchAlignment, new File(onto1File), new File(onto2File));
		Evaluator.evaluateSingleAlignment("Structure Mismatch", structureMismatchAlignment, referenceAlignmentPath, mismatchStorePath + "StructureMismatch_" + alignmentFile.getName() + ".txt");
		int structureMismatchNumCells = structureMismatchAlignment.nbCells();
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(structureMismatchAlignmentFile)), true); 
		renderer = new RDFRendererVisitor(writer);

		structureMismatchAlignment.render(renderer);

		writer.flush();
		writer.close();
		
		//print alert to screen if some relations are removed
		if (structureMismatchNumCells < conceptScopeNumCells) {
			System.err.println("The Structure Mismatch Detection identified a mismatch in alignment " + alignmentFile.getName());
		}
		
		
		
		//Run WNDomains mismatch detection
		domainsMismatchAlignment = DomainMismatch.filterAlignment(initialAlignment);
		Evaluator.evaluateSingleAlignment("Domains Mismatch", domainsMismatchAlignment, referenceAlignmentPath, mismatchStorePath + "DomainMismatch_" + alignmentFile.getName() + ".txt");
		int domainMismatchNumCells = domainsMismatchAlignment.nbCells();
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(domainMismatchAlignmentFile)), true); 
		renderer = new RDFRendererVisitor(writer);

		domainsMismatchAlignment.render(renderer);

		writer.flush();
		writer.close();
		
		//print alert to screen if some relations are removed
		if (domainMismatchNumCells < structureMismatchNumCells) {
			System.err.println("The Domains Mismatch Detection identified a mismatch in alignment " + alignmentFile.getName());
		}
		
		numFinalCells = domainsMismatchAlignment.nbCells();
		System.out.println("There are " + numFinalCells + " relations in the filtered alignment");
		
		//store the filtered alignment
		alignmentFileName = mismatchStorePath + "Filtered_" + alignmentFile.getName();

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


