package ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import alignmentcombination.AlignmentConflictResolution;
import alignmentcombination.ProfileWeight;
import alignmentcombination.ProfileWeightSubsumption;
import equivalencematching.DefinitionEquivalenceMatcher;
import equivalencematching.GraphEquivalenceMatcher;
import equivalencematching.LexicalEquivalenceMatcher;
import equivalencematching.PropertyEquivalenceMatcher;
import equivalencematching.WordEmbeddingMatcher;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import mismatchdetection.ConceptScopeMismatch;
import mismatchdetection.DomainMismatch;
import ontologyprofiling.OntologyProfiler;
import rita.wordnet.jwnl.JWNLException;
import subsumptionmatching.CompoundMatcher;
import subsumptionmatching.ContextSubsumptionMatcher;
import subsumptionmatching.DefinitionSubsumptionMatcher;
import subsumptionmatching.LexicalSubsumptionMatcher;
import utilities.AlignmentOperations;

public class SemanticMatcherBasicWeight {
	
	
	static File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
	static File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
	static String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_embeddings.txt";
	static String mismatchStorePath = "./files/_PHD_EVALUATION/ATMONTO-AIRM/MISMATCHES";
	static String finalAlignmentStorePath = "./files/_PHD_EVALUATION/ATMONTO-AIRM/FINAL_ALIGNMENT/";
	
	public static void main(String[] args) throws OWLOntologyCreationException, IOException, AlignmentException, URISyntaxException, JWNLException {
				
	long startTimeMatchingProcess = System.currentTimeMillis();
	
	/* profile input ontologies */
	
	long startTimeOntologyProfiling = System.currentTimeMillis();
	System.out.print("Computing ontology profiles");
	Map<String, Double> ontologyProfilingScores = OntologyProfiler.computeOntologyProfileScores(ontoFile1, ontoFile2, vectorFile);
	long endTimeOntologyProfiling = System.currentTimeMillis();
	System.out.print("..." + (endTimeOntologyProfiling - startTimeOntologyProfiling) / 1000 + " seconds.\n");
	
	//print profiling scores
	for (Entry<String, Double> e : ontologyProfilingScores.entrySet()) {
		System.out.println(e.getKey() + ": " + e.getValue());
	}

	/* run matcher ensemble EQ */
	ArrayList<URIAlignment> eqAlignments = computeEQAlignments(ontoFile1, ontoFile2, ontologyProfilingScores, vectorFile);

	
	/* combine using ProfileWeight EQ */
	URIAlignment combinedEQAlignment = combineEQAlignments(eqAlignments);
	URIAlignment combinedEQAlignmentWithoutMismatches = removeMismatches(combinedEQAlignment, mismatchStorePath);
	
	//store the EQ alignment
	File outputAlignment = new File(finalAlignmentStorePath + "EQAlignment.rdf");

	PrintWriter writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(outputAlignment)), true); 
	AlignmentVisitor renderer = new RDFRendererVisitor(writer);

	combinedEQAlignmentWithoutMismatches.render(renderer);

	writer.flush();
	writer.close();
	
	/* run matcher ensemble SUB */
	ArrayList<URIAlignment> subAlignments = computeSUBAlignments(ontoFile1, ontoFile2, ontologyProfilingScores);
		
	/* combine using ProfileWeight SUB */
	URIAlignment combinedSUBAlignment = combineSUBAlignments(subAlignments);
	URIAlignment nonConflictedSUBAlignment = AlignmentConflictResolution.resolveAlignmentConflict(combinedSUBAlignment);
	
	
	//TEST: PRINT RELS IN SUB ALIGNMENT
	for (Cell c : nonConflictedSUBAlignment) {
		System.out.println(c.getId() + " : " + c.getObject1AsURI().getFragment() + " : " + c.getObject2AsURI().getFragment() + " : " + c.getRelation().getRelation() + " : " + c.getStrength());
	}
	
	//store the SUB alignment
	outputAlignment = new File(finalAlignmentStorePath + "SUBAlignment.rdf");

	writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(outputAlignment)), true); 
	renderer = new RDFRendererVisitor(writer);

	combinedEQAlignmentWithoutMismatches.render(renderer);

	writer.flush();
	writer.close();
	
	/* merge ProfileWeight EQ and ProfileWeight SUB alignments into a final alignment */
	URIAlignment mergedEQAndSubAlignment = mergeEQAndSubAlignments(combinedEQAlignmentWithoutMismatches, nonConflictedSUBAlignment);
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
	private static ArrayList<URIAlignment> computeEQAlignments(File ontoFile1, File ontoFile2, Map<String, Double> ontologyProfilingScores, String vectorFile) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

		ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();
		
		//get the profiling scores
		double cc = ontologyProfilingScores.get("cc");
		double sp = ontologyProfilingScores.get("sp");
		double pf = ontologyProfilingScores.get("pf");
		double lc = ontologyProfilingScores.get("lc");
		double dc = ontologyProfilingScores.get("dc");

		if (cc >= 0.5) {
		System.out.print("Computing WEM alignment");
		long startTimeWEM = System.currentTimeMillis();
		URIAlignment WEMAlignment = WordEmbeddingMatcher.returnWEMAlignment(ontoFile1, ontoFile2, vectorFile, ontologyProfilingScores.get("cc"));	
		eqAlignments.add(WEMAlignment);
		long endTimeWEM = System.currentTimeMillis();
		System.out.print("..." + (endTimeWEM - startTimeWEM)  / 1000 + " seconds.\n");
		}

		if (dc >= 0.5) {
		System.out.print("Computing DEM alignment");
		long startTimeDEM = System.currentTimeMillis();
		URIAlignment DEMAlignment = DefinitionEquivalenceMatcher.returnDEMAlignment(ontoFile1, ontoFile2, vectorFile, ontologyProfilingScores.get("cc"));
		eqAlignments.add(DEMAlignment);
		long endTimeDEM = System.currentTimeMillis();
		System.out.print("..." + (endTimeDEM - startTimeDEM)  / 1000 + " seconds.\n");
		}
		
//		if (sp >= 0.5) {
//		System.out.print("Computing GEM alignment");
//		long startTimeGEM = System.currentTimeMillis();
//		URIAlignment GEMAlignment = GraphEquivalenceMatcher.returnGEMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("sp"));	
//		eqAlignments.add(GEMAlignment);
//		long endTimeGEM = System.currentTimeMillis();
//		System.out.print("..." + (endTimeGEM - startTimeGEM)  / 1000 + " seconds.\n");
//		}

		if (pf >= 0.5) {
		System.out.print("Computing PEM alignment");
		long startTimePEM = System.currentTimeMillis();
		URIAlignment PEMAlignment = PropertyEquivalenceMatcher.returnPEMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("pf"));
		eqAlignments.add(PEMAlignment);
		long endTimePEM = System.currentTimeMillis();
		System.out.print("..." + (endTimePEM - startTimePEM)  / 1000 + " seconds.\n");
		}
		
		if (lc >= 0.5) {
		System.out.print("Computing LEM alignment");
		long startTimeLEM = System.currentTimeMillis();
		URIAlignment LEMAlignment = LexicalEquivalenceMatcher.returnLEMAlignment(ontoFile1, ontoFile2, (ontologyProfilingScores.get("lc") * ontologyProfilingScores.get("sr")));
		eqAlignments.add(LEMAlignment);
		long endTimeLEM = System.currentTimeMillis();
		System.out.print("..." + (endTimeLEM - startTimeLEM)  / 1000 + " seconds.\n");
		}


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
	 */
	private static ArrayList<URIAlignment> computeSUBAlignments(File ontoFile1, File ontoFile2, Map<String, Double> ontologyProfilingScores) throws OWLOntologyCreationException, AlignmentException {

		//get the profiling scores
		double sp = ontologyProfilingScores.get("sp");
		double lc = ontologyProfilingScores.get("lc");
		double cf = ontologyProfilingScores.get("cf");
		double dc = ontologyProfilingScores.get("dc");
		
		ArrayList<URIAlignment> subAlignments = new ArrayList<URIAlignment>();

		if (cf >= 0.5) {
		System.out.print("Computing CM alignment");
		long startTimeCM = System.currentTimeMillis();
		URIAlignment CMAlignment = CompoundMatcher.returnCMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("cf"));		
		subAlignments.add(CMAlignment);
		long endTimeCM = System.currentTimeMillis();
		System.out.print("..." + (endTimeCM - startTimeCM)  / 1000 + " seconds.\n");
		}

		if (sp >= 0.5) {
		System.out.print("Computing CSM alignment");
		long startTimeCSM = System.currentTimeMillis();
		URIAlignment CSMAlignment = ContextSubsumptionMatcher.returnCSMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("sp"));		
		subAlignments.add(CSMAlignment);
		long endTimeCSM = System.currentTimeMillis();
		System.out.print("..." + (endTimeCSM - startTimeCSM)  / 1000 + " seconds.\n");
		}
		
		if (dc >= 0.5) {
		System.out.print("Computing DSM alignment");
		long startTimeDSM = System.currentTimeMillis();
		URIAlignment DSMAlignment = DefinitionSubsumptionMatcher.returnDSMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("dc"));		
		subAlignments.add(DSMAlignment);
		long endTimeDSM = System.currentTimeMillis();
		System.out.print("..." + (endTimeDSM - startTimeDSM)  / 1000 + " seconds.\n");
		}
		
		if (lc >= 0.5) {
		
		System.out.print("Computing LSM alignment");
		long startTimeLSM = System.currentTimeMillis();
		URIAlignment LSMAlignment = LexicalSubsumptionMatcher.returnLSMAlignment(ontoFile1, ontoFile2, (ontologyProfilingScores.get("lc") * ontologyProfilingScores.get("hr")));		
		subAlignments.add(LSMAlignment);
		long endTimeLSM = System.currentTimeMillis();
		System.out.print("..." + (endTimeLSM - startTimeLSM)  / 1000 + " seconds.\n");
		}
		
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

/**
 * Filters out relations representing mismatches on the basis of a set of mismatch detection strategies.
 * @param combinedEQAlignment the input alignment from which mismatches are filtered out.
 * @param mismatchStorePath a folder where the filtered alignments from the included mismatch detection strategies are stored.
 * @return an URIAlignment without mismatch relations.
 * @throws AlignmentException
 * @throws OWLOntologyCreationException
 * @throws JWNLException
 * @throws URISyntaxException
 * @throws IOException
   Jul 15, 2019
 */
private static URIAlignment removeMismatches (URIAlignment combinedEQAlignment, String mismatchStorePath) throws AlignmentException, OWLOntologyCreationException, JWNLException, URISyntaxException, IOException {

	//store the merged alignment
	File initialAlignment = new File(mismatchStorePath + "/initialAlignment.rdf");
	File conceptScopeMismatchAlignment = new File(mismatchStorePath + "/conceptScopeMismatch.rdf");
	File domainMismatchAlignment = new File(mismatchStorePath + "/domainMismatch.rdf");
	PrintWriter writer = null;
	AlignmentVisitor renderer = null;
	
	writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(initialAlignment)), true); 
	renderer = new RDFRendererVisitor(writer);

	combinedEQAlignment.render(renderer);

	writer.flush();
	writer.close();

	URIAlignment conceptScopeMismatchDetection = ConceptScopeMismatch.detectConceptScopeMismatch(combinedEQAlignment);
	//System.out.println("Concept Scope Mismatch Detection removed " + ( combinedEQAlignment.nbCells() - conceptScopeMismatchDetection.nbCells() ) + " relations");
	
	writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(conceptScopeMismatchAlignment)), true); 
	renderer = new RDFRendererVisitor(writer);

	conceptScopeMismatchDetection.render(renderer);

	writer.flush();
	writer.close();

	URIAlignment domainMismatchDetection = DomainMismatch.filterAlignment(conceptScopeMismatchDetection);
	//System.out.println("Domain Mismatch Detection removed " + ( conceptScopeMismatchDetection.nbCells() - domainMismatchDetection.nbCells() ) + " relations");
	
	writer = new PrintWriter(
			new BufferedWriter(
					new FileWriter(domainMismatchAlignment)), true); 
	renderer = new RDFRendererVisitor(writer);

	domainMismatchDetection.render(renderer);

	writer.flush();
	writer.close();


	return domainMismatchDetection;
}

}
