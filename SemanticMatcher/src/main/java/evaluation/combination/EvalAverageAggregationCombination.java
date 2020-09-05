package evaluation.combination;

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
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import alignmentcombination.AlignmentConflictResolution;
import alignmentcombination.AverageAggregation;
import alignmentcombination.NaiveDescendingExtraction;
import evaluation.general.ComputeSyntacticEvaluationScores;
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.MismatchDetection;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

import rita.wordnet.jwnl.JWNLException;

/**
 * Evaluates the alignment combination method Average Aggregation in the ATM and Cross-domain datasets.
 * The input alignments created by the individual matchers reside in predefined folders and the output from the main method is
 * a set of alignment files (RDF/XML) at different confidence thresholds (0.0-1.0) and evaluation scores printed to Excel files.
 * @author audunvennesland
 *
 */
public class EvalAverageAggregationCombination {

	//ATMONTO-AIRM || BIBFRAME-SCHEMAORG
	final static String DATASET = "BIBFRAME-SCHEMAORG";

	static File source_onto = null;
	static File target_onto = null;
	static String reference_alignment_eq = null;
	static String reference_alignment_sub = null;
	static String reference_alignment_eq_AND_SUB = null;
	static Date date = Calendar.getInstance().getTime();

	public static void main(String[] args) throws AlignmentException, URISyntaxException, OWLOntologyCreationException, JWNLException, IOException {

		if (DATASET.equalsIgnoreCase("ATMONTO-AIRM")) {
			source_onto = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
			target_onto = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
			reference_alignment_eq = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";
			reference_alignment_sub = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-SUBSUMPTION.rdf";
			reference_alignment_eq_AND_SUB = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQ-SUB.rdf";

		} else if (DATASET.equalsIgnoreCase("BIBFRAME-SCHEMAORG")) {
			source_onto = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
			target_onto = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
			reference_alignment_eq = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";
			reference_alignment_sub = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUBSUMPTION.rdf";
			reference_alignment_eq_AND_SUB = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQ-SUB.rdf";
		}


		//folder holding all individual EQ and SUB alignments at threshold 0.0
		String EQ_folder = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/AVERAGE/MERGED_NOWEIGHT/EQ";
		String SUB_folder = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/AVERAGE/MERGED_NOWEIGHT/SUB";

		//put all EQ alignments in the EQ_folder into an ArrayList after enforcing 1-1 relations and removing mismatches
		AlignmentParser aparser = new AlignmentParser(0);
		URIAlignment refalign_EQ_AND_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_eq_AND_SUB)));

		URIAlignment refalign_EQ = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_eq)));
		URIAlignment refalign_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_sub)));

		File folder = new File(EQ_folder);
		File[] filesInDir = folder.listFiles();
		URIAlignment thisAlignment = null;

		String URI = null;

		ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();

		URIAlignment ndaAlignment = new URIAlignment();
		URIAlignment noMismatchAlignment = new URIAlignment();

		for (int i = 0; i < filesInDir.length; i++) {
			URI = StringUtilities.convertToFileURL(EQ_folder) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			thisAlignment = (URIAlignment) aparser.parse(new URI(URI));

			//enforce 1-1 relations
			ndaAlignment = NaiveDescendingExtraction.extractOneToOneRelations(thisAlignment);

			//remove mismatches
			noMismatchAlignment = MismatchDetection.removeMismatches(ndaAlignment, source_onto, target_onto);

			eqAlignments.add(noMismatchAlignment);

		}

		//put all SUB alignments in the SUB_folder into an ArrayList
		folder = new File(SUB_folder);
		filesInDir = folder.listFiles();
		ArrayList<URIAlignment> subAlignments = new ArrayList<URIAlignment>();

		for (int i = 0; i < filesInDir.length; i++) {
			URI = StringUtilities.convertToFileURL(SUB_folder) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			thisAlignment = (URIAlignment) aparser.parse(new URI(URI));
			subAlignments.add(thisAlignment);

		}

		double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
//		double precision = 0;
//		double recall = 0;
//		double fMeasure = 0;
//		PRecEvaluator eval = null;
//		Properties p = new Properties();


		URIAlignment averageAggEQAlignment = AverageAggregation.getAverageAggregatedAlignment(eqAlignments);				
		URIAlignment averageAggSUBAlignment = AverageAggregation.getAverageAggregatedAlignment(subAlignments);


		//merge the "merged" EQ alignment and SUB alignment 
		URIAlignment mergedEQAndSubAlignment = AlignmentOperations.combineEQAndSUBAlignments(averageAggEQAlignment, averageAggSUBAlignment);

		//resolve potential conflicts in the merged EQ and SUB alignment
		URIAlignment nonConflictedMergedAlignment = AlignmentConflictResolution.resolveAlignmentConflict(mergedEQAndSubAlignment);

		//isolate the equivalence relations and evaluate the equivalence alignment only
		URIAlignment eqOnly = AlignmentOperations.extractEquivalenceRelations(nonConflictedMergedAlignment);

		Map<String, EvaluationScore> eqEvaluationMap = new TreeMap<String, EvaluationScore>();

		for (double conf : confidence) {
			//EvaluationScore evalScore = new EvaluationScore();
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

		Evaluator.evaluateSingleMatcherThresholds(eqEvaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/AVERAGE/AVERAGE_AGG_EQ_ONLY_"+date);


		//isolate the subsumption relations and evaluate the subsumption alignment only
		URIAlignment subOnly = AlignmentOperations.extractSubsumptionRelations(nonConflictedMergedAlignment);

		Map<String, EvaluationScore> subEvaluationMap = new TreeMap<String, EvaluationScore>();

		for (double conf : confidence) {
			//EvaluationScore evalScore = new EvaluationScore();
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

		Evaluator.evaluateSingleMatcherThresholds(subEvaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/AVERAGE/AVERAGE_AGG_SUB_ONLY_"+date);

		//store the merged alignment
		File outputAlignment = null;

		PrintWriter writer = null;
		AlignmentVisitor renderer = null;

		Map<String, EvaluationScore> eq_and_sub_evaluationMap = new TreeMap<String, EvaluationScore>();


		for (double conf : confidence) {
			//EvaluationScore evalScore = new EvaluationScore();
			nonConflictedMergedAlignment.cut(conf);
//			eval = new PRecEvaluator(refalign_EQ_AND_SUB, nonConflictedMergedAlignment);
//			eval.eval(p);
//			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//			evalScore.setPrecision(precision);
//			evalScore.setRecall(recall);
//			evalScore.setfMeasure(fMeasure);
			EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(nonConflictedMergedAlignment, refalign_EQ_AND_SUB);
			//put the evalation score according to each confidence value in the map
			eq_and_sub_evaluationMap.put(String.valueOf(conf), evalScore);			
			outputAlignment = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/AVERAGE/MERGED_NOWEIGHT/AverageAggregation"+DATASET+"_"+conf+".rdf");
			writer = new PrintWriter(
					new BufferedWriter(
							new FileWriter(outputAlignment)), true); 
			renderer = new RDFRendererVisitor(writer);

			nonConflictedMergedAlignment.render(renderer);
			writer.flush();
			writer.close();

		}

		Evaluator.evaluateSingleMatcherThresholds(eq_and_sub_evaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/AVERAGE/AVERAGE_"+date);




	}

}
