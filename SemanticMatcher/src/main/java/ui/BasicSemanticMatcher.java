package ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import alignmentcombination.AlignmentConflictResolution;
import alignmentcombination.ProfileWeight;
import alignmentcombination.ProfileWeightSubsumption;
import equivalencematching.BasicEQMatcher;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import rita.wordnet.jwnl.JWNLException;
import subsumptionmatching.BasicSubsumptionMatcher;
import utilities.AlignmentOperations;

/**
 * A basic semantic matching prototype without any ontology profiling, mismatch detection and with only two matchers (basic EQ matcher and basic SUB matcher)
 * @author audunvennesland
 *
 */
public class BasicSemanticMatcher {
	
	
	static File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
	static File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
	static String finalAlignmentStorePath = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/FINAL_ALIGNMENT/";

	
	public static void main(String[] args) throws OWLOntologyCreationException, IOException, AlignmentException, URISyntaxException, JWNLException {
				
	long startTimeMatchingProcess = System.currentTimeMillis();
	

	/* run matcher ensemble EQ */
	ArrayList<URIAlignment> eqAlignments = computeEQAlignments(ontoFile1, ontoFile2);

	
	/* combine using ProfileWeight EQ */
	URIAlignment combinedEQAlignment = combineEQAlignments(eqAlignments);
	
	//store the EQ alignment
	File outputAlignment = new File(finalAlignmentStorePath + "EQAlignment.rdf");

	PrintWriter writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(outputAlignment)), true); 
	AlignmentVisitor renderer = new RDFRendererVisitor(writer);

	combinedEQAlignment.render(renderer);

	writer.flush();
	writer.close();
	
	/* run matcher ensemble SUB */
	ArrayList<URIAlignment> subAlignments = computeSUBAlignments(ontoFile1, ontoFile2);
		
	/* combine using ProfileWeight SUB */
	URIAlignment combinedSUBAlignment = combineSUBAlignments(subAlignments);
	URIAlignment nonConflictedSUBAlignment = AlignmentConflictResolution.resolveAlignmentConflict(combinedSUBAlignment);
	
	//store the SUB alignment
	outputAlignment = new File(finalAlignmentStorePath + "SUBAlignment.rdf");

	writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(outputAlignment)), true); 
	renderer = new RDFRendererVisitor(writer);

	nonConflictedSUBAlignment.render(renderer);

	writer.flush();
	writer.close();
	
	/* merge ProfileWeight EQ and ProfileWeight SUB alignments into a final alignment */
	URIAlignment mergedEQAndSubAlignment = mergeEQAndSubAlignments(combinedEQAlignment, nonConflictedSUBAlignment);
	URIAlignment nonConflictedMergedAlignment = AlignmentConflictResolution.resolveAlignmentConflict(mergedEQAndSubAlignment);
	
	//store the final alignment
	outputAlignment = new File(finalAlignmentStorePath + "finalAlignment.rdf");

	writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(outputAlignment)), true); 
	renderer = new RDFRendererVisitor(writer);

	nonConflictedMergedAlignment.render(renderer);

	writer.flush();
	writer.close();
	
	long endTimeMatchingProcess = System.currentTimeMillis();

	System.out.println("The semantic matching operation took " + (endTimeMatchingProcess - startTimeMatchingProcess)  / 1000 + " seconds.");
	
	}
	
	/**
	 * This method makes a call to the individual equivalence matchers which produce their alignments.
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology 
	 * @param ontologyProfilingScores a map holding scores from the ontology profiling process
	 * @param vectorFile a file holding terms and corresponding embedding vectors
	 * @return an ArrayList of URIAlignments produced by the individual equivalence matchers.
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	 * @throws URISyntaxException
	   Jul 15, 2019
	   TODO: Omit matchers if their corresponding profile score is below a threshold (e.g. 0.5)
	 */
	private static ArrayList<URIAlignment> computeEQAlignments(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

		ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();

		System.out.print("Computing Basic Equivalence Matcher Alignment");
		long startTimeBWM = System.currentTimeMillis();
		URIAlignment BEMAlignment = BasicEQMatcher.returnBasicEQMatcherAlignment(ontoFile1, ontoFile2);	
		eqAlignments.add(BEMAlignment);
		long endTimeBEM = System.currentTimeMillis();
		System.out.print("..." + (endTimeBEM - startTimeBWM)  / 1000 + " seconds.\n");

		return eqAlignments;

	}
	
	/**
	 * Combines individual equivalence alignments into a single alignment
	 * @param inputAlignments individual alignments produced by an emsemble of matchers
	 * @return a URIAlignment holding equivalence relations produced by an ensemble of matchers
	 * @throws AlignmentException
	 * @throws IOException
	 * @throws URISyntaxException
	   Jul 15, 2019
	 */
	private static URIAlignment combineEQAlignments (ArrayList<URIAlignment> inputAlignments) throws AlignmentException, IOException, URISyntaxException {

		URIAlignment combinedEQAlignment = ProfileWeight.computeProfileWeightingEquivalence(inputAlignments);

		return combinedEQAlignment;

	}

	
	/**
	 * This method makes a call to the individual subsumption matchers which produce their alignments.
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param ontologyProfilingScores a map holding scores from the ontology profiling process
	 * @return an ArrayList of URIAlignments produced by the individual subsumption matchers.
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Jul 15, 2019
	   TODO: Omit matchers if their corresponding profile score is below a threshold (e.g. 0.5)
	 * @throws IOException 
	 */
	private static ArrayList<URIAlignment> computeSUBAlignments(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException, AlignmentException, IOException {

		
		ArrayList<URIAlignment> subAlignments = new ArrayList<URIAlignment>();


		System.out.print("Computing Basic Subsumption Matcher Alignment");
		long startTimeBSM = System.currentTimeMillis();
		URIAlignment BSMAlignment = BasicSubsumptionMatcher.returnBasicSUBMatcherAlignment(ontoFile1, ontoFile2);		
		
		subAlignments.add(BSMAlignment);
		long endTimBSM = System.currentTimeMillis();
		System.out.print("..." + (endTimBSM - startTimeBSM)  / 1000 + " seconds.\n");
		
		
		return subAlignments;

	}

	/**
	 * Combines individual subsumption alignments into a single alignment
	 * @param inputAlignments individual alignments produced by an emsemble of matchers
	 * @return a URIAlignment holding subsumption relations produced by an ensemble of matchers
	 * @throws AlignmentException
	 * @throws IOException
	 * @throws URISyntaxException
	   Jul 15, 2019
	 */
	private static URIAlignment combineSUBAlignments (ArrayList<URIAlignment> inputAlignments) throws AlignmentException, IOException, URISyntaxException {

		URIAlignment combinedSUBAlignment = ProfileWeightSubsumption.computeProfileWeightingSubsumption(inputAlignments);

		return combinedSUBAlignment;

	}

/**
 * Merges an alignment holding equivalence relations with an alignment holding subsumption relations.
 * @param eqAlignment the input equivalence alignment
 * @param subAlignment the input subsumption alignment
 * @return a merged URIAlignment holding both equivalence and subsumption relations.
 * @throws AlignmentException
   Jul 15, 2019
 */
private static URIAlignment mergeEQAndSubAlignments (URIAlignment eqAlignment, URIAlignment subAlignment) throws AlignmentException {

	URIAlignment mergedEQAndSubAlignment = AlignmentOperations.combineEQAndSUBAlignments(eqAlignment, subAlignment);

	return mergedEQAndSubAlignment;

}



}
