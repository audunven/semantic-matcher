package evaluation.profileweight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

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
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import mismatchdetection.ConceptScopeMismatch;
import mismatchdetection.DomainMismatch;
import mismatchdetection.StructureMismatch;
import net.didion.jwnl.JWNLException;
import ontologyprofiling.OntologyProfiler;
import subsumptionmatching.CompoundMatcher;
import subsumptionmatching.ContextSubsumptionMatcher;
import subsumptionmatching.DefinitionSubsumptionMatcher;
import subsumptionmatching.LexicalSubsumptionMatcher;
import utilities.AlignmentOperations;

public class EvaluateMergedAlignmentBasicWeighting {
	
	static String onto1 = "303";
	static String onto2 = "304";
	static String relationType ="EQ_SUB";

	static File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/" + onto1+onto2 + "/" + onto1+onto2 + "-" + onto1 + ".rdf");
	static File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/" + onto1+onto2 + "/" + onto1+onto2 + "-" + onto2 + ".rdf");
	static String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_trained.txt";
	static String referenceAlignmentEQAndSUB ="./files/_PHD_EVALUATION/OAEI2011/REFALIGN/" + onto1+onto2 + "/" + onto1 + "-" + onto2 + "-" +relationType+".rdf";
	
//	static File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-301.rdf");
//	static File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-302.rdf");
//
//	static String referenceAlignmentEQAndSUB = "./files/_PHD_EVALUATION/OAEI2011/REFALIGN/301302/301-302-EQ_SUB.rdf";
//	static String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_trained.txt";


	public static void main(String[] args) throws OWLOntologyCreationException, JWNLException, IOException, AlignmentException, URISyntaxException {


		//compute profile scores
		System.err.println("Computing Profiling Scores");
		Map<String, Double> ontologyProfilingScores = OntologyProfiler.computeOntologyProfileScores(ontoFile1, ontoFile2, vectorFile);

		//compute EQ alignments
		ArrayList<URIAlignment> eqAlignments = computeEQAlignments(ontoFile1, ontoFile2, ontologyProfilingScores, vectorFile);

		//combine EQ alignments into a final EQ alignment
		URIAlignment combinedEQAlignment = combineEQAlignments(eqAlignments);

		//remove mismatches from combined EQ alignment
		URIAlignment combinedEQAlignmentWithoutMismatches = removeMismatches(combinedEQAlignment);

		//compute SUB alignments
		ArrayList<URIAlignment> subAlignments = computeSUBAlignments(ontoFile1, ontoFile2, ontologyProfilingScores, vectorFile);

		//combine SUB alignments into a final SUB alignment
		URIAlignment combinedSUBAlignment = combineSUBAlignments(subAlignments);

		//merge final EQ and final SUB alignment
		URIAlignment mergedEQAndSubAlignment = mergeEQAndSubAlignments(combinedEQAlignmentWithoutMismatches, combinedSUBAlignment);

		//resolve conflicts in merged alignment
		URIAlignment nonConflictedMergedAlignment = AlignmentConflictResolution.resolveAlignmentConflict(mergedEQAndSubAlignment);

		System.err.println("\nThe merged EQ and SUB alignment contains " + nonConflictedMergedAlignment.nbCells() + " relations");

		//store the merged alignment
		File outputAlignment = new File("./files/_PHD_EVALUATION/MATCHERTESTING/EvalMergedAlignment/EvalMergedAlignmentNoWeighting_" + onto1+onto2 +".rdf");

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		nonConflictedMergedAlignment.render(renderer);

		writer.flush();
		writer.close();

		//evaluate merged final EQ-SUB alignment
		System.err.println("Evaluating the merged EQ and SUB alignment:");
		Evaluator.evaluateSingleAlignment(nonConflictedMergedAlignment, referenceAlignmentEQAndSUB);

	}

	private static ArrayList<URIAlignment> computeEQAlignments(File ontoFile1, File ontoFile2, Map<String, Double> ontologyProfilingScores, String vectorFile) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

		ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();

		System.err.println("Computing WEM alignment");
		URIAlignment WEMAlignment = WordEmbeddingMatcher.returnWEMAlignment(ontoFile1, ontoFile2, vectorFile, ontologyProfilingScores.get("cc"));	
		eqAlignments.add(WEMAlignment);

		System.err.println("Computing DEM alignment");
		URIAlignment DEMAlignment = DefinitionEquivalenceMatcher.returnDEMAlignment(ontoFile1, ontoFile2, vectorFile, ontologyProfilingScores.get("cc"));
		eqAlignments.add(DEMAlignment);

		System.err.println("Computing GEM alignment");
		URIAlignment GEMAlignment = GraphEquivalenceMatcher.returnGEMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("sp"));	
		eqAlignments.add(GEMAlignment);

		System.err.println("Computing PEM alignment");
		URIAlignment PEMAlignment = PropertyEquivalenceMatcher.returnPEMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("pf"));
		eqAlignments.add(PEMAlignment);

		System.err.println("Computing LEM alignment");
		URIAlignment LEMAlignment = LexicalEquivalenceMatcher.returnLEMAlignment(ontoFile1, ontoFile2, (ontologyProfilingScores.get("lc") * ontologyProfilingScores.get("sr")));
		eqAlignments.add(LEMAlignment);

		System.out.println("The arraylist eqAlignments contains " + eqAlignments.size() + " alignments");		


		return eqAlignments;

	}

	private static URIAlignment combineEQAlignments (ArrayList<URIAlignment> inputAlignments) throws AlignmentException, IOException, URISyntaxException {

		URIAlignment combinedEQAlignment = ProfileWeight.computeProfileWeightingEquivalence(inputAlignments);

		System.err.println("\nThe combined EQ alignment contains " + combinedEQAlignment.nbCells() + " relations");
		System.err.println("These relations are: ");

		for (Cell c : combinedEQAlignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		return combinedEQAlignment;

	}

	private static ArrayList<URIAlignment> computeSUBAlignments(File ontoFile1, File ontoFile2, Map<String, Double> ontologyProfilingScores, String vectorFile) throws OWLOntologyCreationException, AlignmentException {

		ArrayList<URIAlignment> subAlignments = new ArrayList<URIAlignment>();

		System.err.println("Computing CM alignment");
		URIAlignment CMAlignment = CompoundMatcher.returnCMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("cf"));		
		subAlignments.add(CMAlignment);

		System.err.println("Computing CSM alignment");
		URIAlignment CSMAlignment = ContextSubsumptionMatcher.returnCSMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("sp"));		
		subAlignments.add(CSMAlignment);

		System.err.println("Computing DSM alignment");
		URIAlignment DSMAlignment = DefinitionSubsumptionMatcher.returnDSMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("dc"));		
		subAlignments.add(DSMAlignment);

		System.err.println("Computing LSM alignment");
		URIAlignment LSMAlignment = LexicalSubsumptionMatcher.returnLSMAlignment(ontoFile1, ontoFile2, (ontologyProfilingScores.get("lc") * ontologyProfilingScores.get("hr")));		
		subAlignments.add(LSMAlignment);

		return subAlignments;

	}

	private static URIAlignment combineSUBAlignments (ArrayList<URIAlignment> inputAlignments) throws AlignmentException, IOException, URISyntaxException {

		URIAlignment combinedSUBAlignment = ProfileWeightSubsumption.computeProfileWeightingSubsumption(inputAlignments);

		System.err.println("\nThe combined SUB alignment contains " + combinedSUBAlignment.nbCells() + " relations");
		System.err.println("These relations are: ");

		for (Cell c : combinedSUBAlignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}


		return combinedSUBAlignment;

	}

	private static URIAlignment removeMismatches (URIAlignment combinedEQAlignment) throws AlignmentException, OWLOntologyCreationException, FileNotFoundException, JWNLException {

		URIAlignment conceptScopeMismatchDetection = ConceptScopeMismatch.detectConceptScopeMismatch(combinedEQAlignment);
		URIAlignment structureMismatchDetection = StructureMismatch.detectStructureMismatches(conceptScopeMismatchDetection, ontoFile1, ontoFile2);
		URIAlignment domainMismatchDetection = DomainMismatch.filterAlignment(structureMismatchDetection);

		return domainMismatchDetection;
	}

	private static URIAlignment mergeEQAndSubAlignments (URIAlignment eqAlignment, URIAlignment subAlignment) throws AlignmentException {

		URIAlignment mergedEQAndSubAlignment = AlignmentOperations.combineEQAndSUBAlignments(eqAlignment, subAlignment);

		return mergedEQAndSubAlignment;

	}

}
