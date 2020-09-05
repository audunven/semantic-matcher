package evaluation.combination.oaei;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import alignmentcombination.AlignmentConflictResolution;
import alignmentcombination.ProfileWeight;
import alignmentcombination.ProfileWeightSubsumption;
import equivalencematching.GraphEquivalenceMatcherSigmoid;
import equivalencematching.LexicalEquivalenceMatcherSigmoid;
import equivalencematching.WordEmbeddingMatcherSigmoid;
import evaluation.general.ComputeSyntacticEvaluationScores;
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.ConceptScopeMismatch;
import mismatchdetection.DomainMismatch;
import mismatchdetection.StructureMismatch;
import rita.wordnet.jwnl.JWNLException;
import ontologyprofiling.OntologyProfiler;
import subsumptionmatching.CompoundMatcherSigmoid;
import subsumptionmatching.ContextSubsumptionMatcherSigmoid;
import subsumptionmatching.LexicalSubsumptionMatcherSigmoid;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

/**
 * Evaluates the alignment combination method Profile Weight in the OAEI2011 dataset.
 * The Profile Weight imposes a confidence weight to each relation that is based on the scores obtained in the ontology profiling process. 
 * This class experiments with matcher selection, that is, if a score from the ontology profiling is below a defined threshold, the associated matcher is excluded from
 * the matcher ensemble. For now this is done manually by simply commenting out those matchers in the dataset that have too low ontology profile scores.
 * The input alignments are created by the individual matchers and the output from the main method is
 * a set of alignment files (RDF/XML) at different confidence thresholds (0.0-1.0) and evaluation scores printed to Excel files.
 * @author audunvennesland
 *
 */
public class EvaluateProfileWeightCombinationOAEI_matcher_selection {

	final static String DATASET = "OAEI2011";

	//these parameters are used for the sigmoid weight configuration
	final static int SLOPE = 3;
	final static double RANGEMIN = 0.5;
	final static double RANGEMAX = 0.7;
	static Date date = Calendar.getInstance().getTime();

	public static void main(String[] args) throws OWLOntologyCreationException, JWNLException, IOException, AlignmentException, URISyntaxException {

		String ontos = "303304";
		File ontoFile1 = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/ONTOLOGIES/" + ontos + "/" + ontos + "-" + ontos.substring(0, 3) + ".rdf");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/ONTOLOGIES/" + ontos + "/" + ontos + "-" + ontos.substring(3, ontos.length()) + ".rdf");
		String vectorFile = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/EMBEDDINGS/wikipedia_embeddings.txt";
		
		String referenceAlignmentEQ ="./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/REFALIGN/" + ontos + "/" + ontos.substring(0, 3) + "-" + ontos.substring(3, ontos.length()) + "-EQUIVALENCE.rdf";
		String referenceAlignmentSUB = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/REFALIGN/" + ontos + "/" + ontos.substring(0, 3) + "-" + ontos.substring(3, ontos.length()) + "-SUBSUMPTION.rdf";
		String referenceAlignmentEQAndSUB ="./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/REFALIGN/" + ontos + "/" + ontos.substring(0, 3) + "-" + ontos.substring(3, ontos.length()) + "-EQ_SUB.rdf";
		String mismatchStorePath = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/ALIGNMENTS/" + ontos + "/MISMATCHES/MISMATCHES_WITH_MATCHER_SELECTION";
		
		AlignmentParser aparser = new AlignmentParser(0);
		URIAlignment refalign_EQ = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentEQ)));
		URIAlignment refalign_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentSUB)));
		
		PrintWriter writer = null;
		AlignmentVisitor renderer = null;
		URIAlignment refalign = null;
		
		URIAlignment eqOnly = null;
		URIAlignment subOnly = null;

		refalign = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentEQAndSUB)));
		

		//compute profile scores
		System.err.println("Computing Profiling Scores");
		Map<String, Double> ontologyProfilingScores = OntologyProfiler.computeOntologyProfileScores(ontoFile1, ontoFile2, vectorFile);

		//compute EQ alignments
		ArrayList<URIAlignment> eqAlignments = computeEQAlignments(ontoFile1, ontoFile2, ontologyProfilingScores, vectorFile);

		//combine EQ alignments into a final EQ alignment
		URIAlignment combinedEQAlignment = combineEQAlignments(eqAlignments);

		//remove mismatches from combined EQ alignment
		URIAlignment combinedEQAlignmentWithoutMismatches = removeMismatches(combinedEQAlignment, referenceAlignmentEQ, mismatchStorePath, ontoFile1, ontoFile2);
		
		//evaluate EQ only
		Evaluator.evaluateSingleAlignment("Evaluation Profile Weight EQ " + ontos.substring(0, 3) + ontos.substring(3, ontos.length()), combinedEQAlignmentWithoutMismatches, referenceAlignmentEQ);

		//compute SUB alignments
		ArrayList<URIAlignment> subAlignments = computeSUBAlignments(ontoFile1, ontoFile2, ontologyProfilingScores, vectorFile);

		//combine SUB alignments into a final SUB alignment
		URIAlignment combinedSUBAlignment = combineSUBAlignments(subAlignments);
		
		//evaluate SUB only (after having resolved any conflicts)
		URIAlignment nonConflictedSUBAlignment = AlignmentConflictResolution.resolveAlignmentConflict(combinedSUBAlignment);
		Evaluator.evaluateSingleAlignment("Evaluation Profile Weight SUB " + ontos.substring(0, 3) + ontos.substring(3, ontos.length()), nonConflictedSUBAlignment, referenceAlignmentSUB);

		//merge final EQ and final SUB alignment
		URIAlignment mergedEQAndSubAlignment = mergeEQAndSubAlignments(combinedEQAlignmentWithoutMismatches, combinedSUBAlignment);

		//resolve conflicts in merged alignment
		URIAlignment nonConflictedMergedAlignment = AlignmentConflictResolution.resolveAlignmentConflict(mergedEQAndSubAlignment);
		
//		double precision = 0;
//		double recall = 0;
//		double fMeasure = 0;
//		PRecEvaluator eval = null;
//		Properties p = new Properties();
		Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();

		double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
		
		//isolate the equivalence relations and evaluate the equivalence alignment only
		eqOnly = AlignmentOperations.extractEquivalenceRelations(nonConflictedMergedAlignment);

		Map<String, EvaluationScore> eqEvaluationMap = new TreeMap<String, EvaluationScore>();

		for (double conf : confidence) {
//			EvaluationScore evalScore = new EvaluationScore();
			eqOnly.cut(conf);
//			eval = new PRecEvaluator(refalign_EQ, eqOnly);
//			eval.eval(p);
//			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//			evalScore.setPrecision(precision);
//			evalScore.setRecall(recall);
//			evalScore.setfMeasure(fMeasure);
			EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(eqOnly, refalign_EQ);
			//put the evalation score according to each confidence value in the map
			eqEvaluationMap.put(String.valueOf(conf), evalScore);			
		}
		
		Evaluator.evaluateSingleMatcherThresholds(eqEvaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos+"/PROFILEWEIGHT_MATCHER_SELECTION/PROFILEWEIGHT_EQ_ONLY_"+ontos+"_"+date);


		//isolate the subsumption relations and evaluate the subsumption alignment only
		subOnly = AlignmentOperations.extractSubsumptionRelations(nonConflictedMergedAlignment);

		Map<String, EvaluationScore> subEvaluationMap = new TreeMap<String, EvaluationScore>();

		for (double conf : confidence) {
//		EvaluationScore evalScore = new EvaluationScore();
			subOnly.cut(conf);
//			eval = new PRecEvaluator(refalign_SUB, subOnly);
//			eval.eval(p);
//			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//			evalScore.setPrecision(precision);
//			evalScore.setRecall(recall);
//			evalScore.setfMeasure(fMeasure);
			EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(subOnly, refalign_SUB);
			//put the evalation score according to each confidence value in the map
			subEvaluationMap.put(String.valueOf(conf), evalScore);			
		}

		Evaluator.evaluateSingleMatcherThresholds(subEvaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos+"/PROFILEWEIGHT_MATCHER_SELECTION/PROFILEWEIGHT_SUB_ONLY_"+ontos+"_"+date);

		System.err.println("\nThe merged EQ and SUB alignment contains " + nonConflictedMergedAlignment.nbCells() + " relations");

		//store the merged alignment
		File outputAlignment = null;


		for (double conf : confidence) {

//			EvaluationScore evalScore = new EvaluationScore();
			nonConflictedMergedAlignment.cut(conf);
//			eval = new PRecEvaluator(refalign, nonConflictedMergedAlignment);
//			eval.eval(p);
//			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//			evalScore.setPrecision(precision);
//			evalScore.setRecall(recall);
//			evalScore.setfMeasure(fMeasure);
			EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(nonConflictedMergedAlignment, refalign);
			//put the evalation score according to each confidence value in the map
			evaluationMap.put(String.valueOf(conf), evalScore);			
			outputAlignment = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos+"/PROFILEWEIGHT_MATCHER_SELECTION/PROFILEWEIGHT_MERGED_SIGMOID_"+DATASET+"_"+ontos+"_"+conf+".rdf");
			writer = new PrintWriter(
					new BufferedWriter(
							new FileWriter(outputAlignment)), true); 
			renderer = new RDFRendererVisitor(writer);

			nonConflictedMergedAlignment.render(renderer);
			writer.flush();
			writer.close();
			//print evaluation results to console
			Evaluator.evaluateSingleAlignment("Cut Threshold " + conf, nonConflictedMergedAlignment, referenceAlignmentEQAndSUB);
		}

		Evaluator.evaluateSingleMatcherThresholds(evaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos+"/PROFILEWEIGHT_MATCHER_SELECTION/PROFILEWEIGHT_"+ontos+"_"+date);

		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		renderer = new RDFRendererVisitor(writer);

		nonConflictedMergedAlignment.render(renderer);

		writer.flush();
		writer.close();


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
	 */
	private static ArrayList<URIAlignment> computeEQAlignments(File ontoFile1, File ontoFile2, Map<String, Double> ontologyProfilingScores, String vectorFile) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

		ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();

		System.err.println("Computing WEM alignment");
		URIAlignment WEMAlignment = WordEmbeddingMatcherSigmoid.returnWEMAlignment(ontoFile1, ontoFile2, vectorFile, ontologyProfilingScores.get("cc"), SLOPE, RANGEMIN, RANGEMAX);	
		eqAlignments.add(WEMAlignment);

//		System.err.println("Computing DEM alignment");
//		URIAlignment DEMAlignment = DefinitionEquivalenceMatcherSigmoid.returnDEMAlignment(ontoFile1, ontoFile2, vectorFile, ontologyProfilingScores.get("cc"), SLOPE, RANGEMIN, RANGEMAX);
//		eqAlignments.add(DEMAlignment);

//		System.err.println("Computing GEM alignment");
//		System.out.println("ontoFile1: " + ontoFile1.getAbsolutePath());
//		System.out.println("ontoFile2: " + ontoFile2.getAbsolutePath());

		URIAlignment GEMAlignment = GraphEquivalenceMatcherSigmoid.returnGEMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("sp"), SLOPE, RANGEMIN, RANGEMAX);	
		eqAlignments.add(GEMAlignment);

//		System.err.println("Computing PEM alignment");
//		URIAlignment PEMAlignment = PropertyEquivalenceMatcherSigmoid.returnPEMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("pf"), SLOPE, RANGEMIN, RANGEMAX);
//		eqAlignments.add(PEMAlignment);

		System.err.println("Computing LEM alignment");
		URIAlignment LEMAlignment = LexicalEquivalenceMatcherSigmoid.returnLEMAlignment(ontoFile1, ontoFile2, (ontologyProfilingScores.get("lc") * ontologyProfilingScores.get("sr")), SLOPE, RANGEMIN, RANGEMAX);
		eqAlignments.add(LEMAlignment);

		System.out.println("The arraylist eqAlignments contains " + eqAlignments.size() + " alignments");		


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

		System.err.println("\nThe combined EQ alignment contains " + combinedEQAlignment.nbCells() + " relations");
		System.err.println("These relations are: ");

		for (Cell c : combinedEQAlignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

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
	 */
	private static ArrayList<URIAlignment> computeSUBAlignments(File ontoFile1, File ontoFile2, Map<String, Double> ontologyProfilingScores, String vectorFile) throws OWLOntologyCreationException, AlignmentException {

		ArrayList<URIAlignment> subAlignments = new ArrayList<URIAlignment>();

		System.err.println("Computing CM alignment");
		URIAlignment CMAlignment = CompoundMatcherSigmoid.returnCMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("cf"), SLOPE, RANGEMIN, RANGEMAX);		
		subAlignments.add(CMAlignment);

		System.err.println("Computing CSM alignment");
		URIAlignment CSMAlignment = ContextSubsumptionMatcherSigmoid.returnCSMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("sp"), SLOPE, RANGEMIN, RANGEMAX);		
		subAlignments.add(CSMAlignment);

//		System.err.println("Computing DSM alignment");
//		URIAlignment DSMAlignment = DefinitionSubsumptionMatcherSigmoid.returnDSMAlignment(ontoFile1, ontoFile2, ontologyProfilingScores.get("dc"), SLOPE, RANGEMIN, RANGEMAX);		
//		subAlignments.add(DSMAlignment);

		System.err.println("Computing LSM alignment");
		URIAlignment LSMAlignment = LexicalSubsumptionMatcherSigmoid.returnLSMAlignment(ontoFile1, ontoFile2, (ontologyProfilingScores.get("lc") * ontologyProfilingScores.get("hr")), SLOPE, RANGEMIN, RANGEMAX);		
		subAlignments.add(LSMAlignment);

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

		System.err.println("\nThe combined SUB alignment contains " + combinedSUBAlignment.nbCells() + " relations");
		System.err.println("These relations are: ");

		for (Cell c : combinedSUBAlignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " " + c.getObject2AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}


		return combinedSUBAlignment;

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
	private static URIAlignment removeMismatches (URIAlignment combinedEQAlignment, String referenceAlignmentEQ, String mismatchStorePath, File ontoFile1, File ontoFile2) throws AlignmentException, OWLOntologyCreationException, JWNLException, URISyntaxException, IOException {

		//store the merged alignment
		File initialAlignment = new File(mismatchStorePath + "/initialAlignment.rdf");
		File conceptScopeMismatchAlignment = new File(mismatchStorePath + "/conceptScopeMismatch.rdf");
		File structureMismatchAlignment = new File(mismatchStorePath + "/structureMismatch.rdf");
		File domainMismatchAlignment = new File(mismatchStorePath + "/domainMismatch.rdf");
		PrintWriter writer = null;
		AlignmentVisitor renderer = null;

		//evaluate initial alignment
		Evaluator.evaluateSingleAlignment(combinedEQAlignment, referenceAlignmentEQ);
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(initialAlignment)), true); 
		renderer = new RDFRendererVisitor(writer);

		combinedEQAlignment.render(renderer);

		writer.flush();
		writer.close();

		URIAlignment conceptScopeMismatchDetection = ConceptScopeMismatch.detectConceptScopeMismatch(combinedEQAlignment);
		System.out.println("Concept Scope Mismatch Detection removed " + ( combinedEQAlignment.nbCells() - conceptScopeMismatchDetection.nbCells() ) + " relations");
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(conceptScopeMismatchAlignment)), true); 
		renderer = new RDFRendererVisitor(writer);

		conceptScopeMismatchDetection.render(renderer);

		writer.flush();
		writer.close();

		//evaluate concept scope mismatch detection
		Evaluator.evaluateSingleAlignment("Concept Scope Mismatch Detection", conceptScopeMismatchDetection, referenceAlignmentEQ);

		URIAlignment structureMismatchDetection = StructureMismatch.detectStructureMismatches(conceptScopeMismatchDetection, ontoFile1, ontoFile2);
		System.out.println("Structure Mismatch Detection removed " + ( conceptScopeMismatchDetection.nbCells() - structureMismatchDetection.nbCells() ) + " relations");
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(structureMismatchAlignment)), true); 
		renderer = new RDFRendererVisitor(writer);

		structureMismatchDetection.render(renderer);

		writer.flush();
		writer.close();

		//evaluate structure mismatch detection
		Evaluator.evaluateSingleAlignment("Structure Mismatch Detection", structureMismatchDetection, referenceAlignmentEQ);

		URIAlignment domainMismatchDetection = DomainMismatch.filterAlignment(structureMismatchDetection);
		System.out.println("Domain Mismatch Detection removed " + ( structureMismatchDetection.nbCells() - domainMismatchDetection.nbCells() ) + " relations");
		
		writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(domainMismatchAlignment)), true); 
		renderer = new RDFRendererVisitor(writer);

		domainMismatchDetection.render(renderer);

		writer.flush();
		writer.close();

		//evaluate domain mismatch detection
		Evaluator.evaluateSingleAlignment("Domain Mismatch Detection", domainMismatchDetection, referenceAlignmentEQ);

		return domainMismatchDetection;
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
