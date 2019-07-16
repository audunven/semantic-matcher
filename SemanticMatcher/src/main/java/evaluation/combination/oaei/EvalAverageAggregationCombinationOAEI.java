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
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import alignmentcombination.AlignmentConflictResolution;
import alignmentcombination.AverageAggregation;
import alignmentcombination.NaiveDescendingExtraction;
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.MismatchDetection;
import net.didion.jwnl.JWNLException;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

/**
 * Evaluates the alignment combination method Average Aggregation in the OAEI 2011 dataset.
 * The input alignments created by the individual matchers reside in predefined folders and the output from the main method is
 * a set of alignment files (RDF/XML) at different confidence thresholds (0.0-1.0) and evaluation scores printed to Excel files.
 * @author audunvennesland
 *
 */
public class EvalAverageAggregationCombinationOAEI {
	

	final static String DATASET = "OAEI2011";
	
	static File source_onto = null;
	static File target_onto = null;
	static String reference_alignment_eq = null;
	static String reference_alignment_sub = null;
	static String reference_alignment_eq_and_sub = null;
	static Date date = Calendar.getInstance().getTime();

	public static void main(String[] args) throws AlignmentException, URISyntaxException, OWLOntologyCreationException, JWNLException, IOException {

		String EQ_folder = null;
		String SUB_folder = null;
		File sourceOntologyFile = null;
		File targetOntologyFile = null;

		String[] ontos = new String[] {"301302", "301303", "301304", "302303", "302304", "303304"};
		
		AlignmentParser aparser = null;
		URIAlignment refalign_EQ_AND_SUB = null;
		URIAlignment refalign_EQ = null;
		URIAlignment refalign_SUB = null;
		URIAlignment mergedEQAndSubAlignment = null;
		URIAlignment nonConflictedMergedAlignment = null;
		URIAlignment eqOnly = null;
		URIAlignment subOnly = null;
		File outputAlignment = null;
		File folder = null;
		File[] filesInDir = null;

		for (int i = 0; i < ontos.length; i++) {

			sourceOntologyFile = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/" + ontos[i] + "/" + ontos[i] + "-" + ontos[i].substring(0, 3) + ".rdf");
			targetOntologyFile = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/" + ontos[i] + "/" + ontos[i] + "-" + ontos[i].substring(3, ontos[i].length()) + ".rdf");
			reference_alignment_eq_and_sub ="./files/_PHD_EVALUATION/OAEI2011/REFALIGN/" + ontos[i] + "/" + ontos[i].substring(0, 3) + "-" + ontos[i].substring(3, ontos[i].length()) + "-EQ_SUB.rdf";
			reference_alignment_eq ="./files/_PHD_EVALUATION/OAEI2011/REFALIGN/" + ontos[i] + "/" + ontos[i].substring(0, 3) + "-" + ontos[i].substring(3, ontos[i].length()) + "-EQUIVALENCE.rdf";
			reference_alignment_sub ="./files/_PHD_EVALUATION/OAEI2011/REFALIGN/" + ontos[i] + "/" + ontos[i].substring(0, 3) + "-" + ontos[i].substring(3, ontos[i].length()) + "-SUBSUMPTION.rdf";
			
			EQ_folder = "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/AVERAGE/MERGED_NOWEIGHT/EQ";
			SUB_folder = "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/AVERAGE/MERGED_NOWEIGHT/SUB";
			aparser = new AlignmentParser(0);
			refalign_EQ_AND_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_eq_and_sub)));
			refalign_EQ = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_eq)));
			refalign_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_sub)));
			
			//put all EQ alignments in the EQ_folder into an ArrayList after enforcing 1-1 relations and removing mismatches
			folder = new File(EQ_folder);
			filesInDir = folder.listFiles();
			URIAlignment thisAlignment = null;

			String URI = null;

			ArrayList<URIAlignment> eqAlignments = new ArrayList<URIAlignment>();

			URIAlignment ndaAlignment = new URIAlignment();
			URIAlignment noMIsmatchAlignment = new URIAlignment();

			for (int j = 0; j < filesInDir.length; j++) {
				URI = StringUtilities.convertToFileURL(EQ_folder) + "/" + StringUtilities.stripPath(filesInDir[j].toString());
				System.out.println("Parsing file " + URI);
				thisAlignment = (URIAlignment) aparser.parse(new URI(URI));

				//enforce 1-1 relations
				ndaAlignment = NaiveDescendingExtraction.extractOneToOneRelations(thisAlignment);

				//remove mismatches
				noMIsmatchAlignment = MismatchDetection.removeMismatches(ndaAlignment, sourceOntologyFile, targetOntologyFile);

				eqAlignments.add(noMIsmatchAlignment);

			}
			
			//put all SUB alignments in the SUB_folder into an ArrayList
			folder = new File(SUB_folder);
			filesInDir = folder.listFiles();
			ArrayList<URIAlignment> subAlignments = new ArrayList<URIAlignment>();

			for (int k = 0; k < filesInDir.length; k++) {
				URI = StringUtilities.convertToFileURL(SUB_folder) + "/" + StringUtilities.stripPath(filesInDir[k].toString());
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
			mergedEQAndSubAlignment = AlignmentOperations.combineEQAndSUBAlignments(averageAggEQAlignment, averageAggSUBAlignment);

			//resolve potential conflicts in the merged EQ and SUB alignment
			nonConflictedMergedAlignment = AlignmentConflictResolution.resolveAlignmentConflict(mergedEQAndSubAlignment);
			
			//isolate the equivalence relations and evaluate the equivalence alignment only
			eqOnly = AlignmentOperations.extractEquivalenceRelations(nonConflictedMergedAlignment);

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

			Evaluator.evaluateSingleMatcherThresholds(eqEvaluationMap, "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/AVERAGE/AVERAGE_EQ_ONLY_"+ontos[i]+"_"+date);


			//isolate the subsumption relations and evaluate the subsumption alignment only
			subOnly = AlignmentOperations.extractSubsumptionRelations(nonConflictedMergedAlignment);

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

			Evaluator.evaluateSingleMatcherThresholds(subEvaluationMap, "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/AVERAGE/AVERAGE_SUB_ONLY_"+ontos[i]+"_"+date);
			
			PrintWriter writer = null;
			AlignmentVisitor renderer = null;
			
			
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
			
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
				evaluationMap.put(String.valueOf(conf), evalScore);			
				outputAlignment = new File("./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/AVERAGE/MERGED_NOWEIGHT/AverageAggregation"+DATASET+"_"+ontos[i]+"_"+conf+".rdf");
				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);

				nonConflictedMergedAlignment.render(renderer);
				writer.flush();
				writer.close();

			}
			
			Evaluator.evaluateSingleMatcherThresholds(evaluationMap, "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/AVERAGE/AVERAGE_"+ontos[i]+"_"+date);

		}
		}

}
