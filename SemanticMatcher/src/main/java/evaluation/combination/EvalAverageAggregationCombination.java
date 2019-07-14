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
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import org.semanticweb.owl.align.Cell;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.MismatchDetection;
import net.didion.jwnl.JWNLException;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

public class EvalAverageAggregationCombination {

	//ATMONTO-AIRM || BIBFRAME-SCHEMAORG
	final static String DATASET = "BIBFRAME-SCHEMAORG";

	static File SOURCE_ONTO = null;
	static File TARGET_ONTO = null;
	static String REFERENCE_ALIGNMENT_EQ = null;
	static String REFERENCE_ALIGNMENT_SUB = null;
	static String REFERENCE_ALIGNMENT_EQ_AND_SUB = null;
	static Date date = Calendar.getInstance().getTime();

	public static void main(String[] args) throws AlignmentException, URISyntaxException, OWLOntologyCreationException, JWNLException, IOException {

		if (DATASET.equalsIgnoreCase("ATMONTO-AIRM")) {
			SOURCE_ONTO = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
			TARGET_ONTO = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
			REFERENCE_ALIGNMENT_EQ = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";
			REFERENCE_ALIGNMENT_SUB = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-SUBSUMPTION.rdf";
			REFERENCE_ALIGNMENT_EQ_AND_SUB = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQ-SUB.rdf";

		} else if (DATASET.equalsIgnoreCase("BIBFRAME-SCHEMAORG")) {
			SOURCE_ONTO = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
			TARGET_ONTO = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
			REFERENCE_ALIGNMENT_EQ = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";
			REFERENCE_ALIGNMENT_SUB = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUBSUMPTION.rdf";
			REFERENCE_ALIGNMENT_EQ_AND_SUB = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQ-SUB.rdf";
		}


		//folder holding all individual EQ and SUB alignments at threshold 0.0
		String EQ_folder = "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/AVERAGE/MERGED_NOWEIGHT/EQ";
		String SUB_folder = "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/AVERAGE/MERGED_NOWEIGHT/SUB";

		//put all EQ alignments in the EQ_folder into an ArrayList after enforcing 1-1 relations and removing mismatches
		AlignmentParser aparser = new AlignmentParser(0);
		URIAlignment refalign_EQ_AND_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(REFERENCE_ALIGNMENT_EQ_AND_SUB)));

		URIAlignment refalign_EQ = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(REFERENCE_ALIGNMENT_EQ)));
		URIAlignment refalign_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(REFERENCE_ALIGNMENT_SUB)));

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
			noMismatchAlignment = MismatchDetection.removeMismatches(ndaAlignment, SOURCE_ONTO, TARGET_ONTO);

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
		double precision = 0;
		double recall = 0;
		double fMeasure = 0;
		PRecEvaluator eval = null;
		Properties p = new Properties();


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
			EvaluationScore evalScore = new EvaluationScore();
			eqOnly.cut(conf);
			eval = new PRecEvaluator(refalign_EQ, eqOnly);
			eval.eval(p);
			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
			evalScore.setPrecision(precision);
			evalScore.setRecall(recall);
			evalScore.setfMeasure(fMeasure);
			//put the evalation score according to each confidence value in the map
			eqEvaluationMap.put(String.valueOf(conf), evalScore);			
		}

		Evaluator.evaluateSingleMatcherThresholds(eqEvaluationMap, "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/AVERAGE/AVERAGE_AGG_EQ_ONLY_"+date);


		//isolate the subsumption relations and evaluate the subsumption alignment only
		URIAlignment subOnly = AlignmentOperations.extractSubsumptionRelations(nonConflictedMergedAlignment);

		Map<String, EvaluationScore> subEvaluationMap = new TreeMap<String, EvaluationScore>();

		for (double conf : confidence) {
			EvaluationScore evalScore = new EvaluationScore();
			subOnly.cut(conf);
			eval = new PRecEvaluator(refalign_SUB, subOnly);
			eval.eval(p);
			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
			evalScore.setPrecision(precision);
			evalScore.setRecall(recall);
			evalScore.setfMeasure(fMeasure);
			//put the evalation score according to each confidence value in the map
			subEvaluationMap.put(String.valueOf(conf), evalScore);			
		}

		Evaluator.evaluateSingleMatcherThresholds(subEvaluationMap, "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/AVERAGE/AVERAGE_AGG_SUB_ONLY_"+date);

		//store the merged alignment
		File outputAlignment = null;

		PrintWriter writer = null;
		AlignmentVisitor renderer = null;

		Map<String, EvaluationScore> eq_and_sub_evaluationMap = new TreeMap<String, EvaluationScore>();


		for (double conf : confidence) {
			EvaluationScore evalScore = new EvaluationScore();
			nonConflictedMergedAlignment.cut(conf);
			eval = new PRecEvaluator(refalign_EQ_AND_SUB, nonConflictedMergedAlignment);
			eval.eval(p);
			precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
			recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
			fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
			evalScore.setPrecision(precision);
			evalScore.setRecall(recall);
			evalScore.setfMeasure(fMeasure);
			//put the evalation score according to each confidence value in the map
			eq_and_sub_evaluationMap.put(String.valueOf(conf), evalScore);			
			outputAlignment = new File("./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/AVERAGE/MERGED_NOWEIGHT/AverageAggregation"+DATASET+"_"+conf+".rdf");
			writer = new PrintWriter(
					new BufferedWriter(
							new FileWriter(outputAlignment)), true); 
			renderer = new RDFRendererVisitor(writer);

			nonConflictedMergedAlignment.render(renderer);
			writer.flush();
			writer.close();

		}

		Evaluator.evaluateSingleMatcherThresholds(eq_and_sub_evaluationMap, "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/AVERAGE/AVERAGE_"+date);




	}

}
