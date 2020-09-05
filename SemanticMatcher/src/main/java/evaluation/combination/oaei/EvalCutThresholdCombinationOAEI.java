package evaluation.combination.oaei;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import alignmentcombination.AlignmentConflictResolution;
import alignmentcombination.NaiveDescendingExtraction;
import evaluation.general.ComputeSyntacticEvaluationScores;
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import mismatchdetection.MismatchDetection;
import rita.wordnet.jwnl.JWNLException;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

/**
 * Evaluates the alignment combination method Cut Threshold in the OAEI2011 dataset.
 * The input alignments created by the individual matchers reside in predefined folders and the output from the main method is
 * a set of alignment files (RDF/XML) at different confidence thresholds (0.0-1.0) and evaluation scores printed to Excel files.
 * @author audunvennesland
 *
 */
public class EvalCutThresholdCombinationOAEI {
	
	final static String DATASET = "OAEI2011";	
	static File source_onto = null;
	static File target_onto = null;
	static String reference_alignment_eq = null;
	static String reference_alignment_sub = null;
	static String reference_alignment_eq_and_sub = null;
	static String eq_folder = null;
	static String sub_folder = null;
	static String[] ontos = new String[] {"301302", "301303", "301304", "302303", "302304", "303304"};
	static Date date = Calendar.getInstance().getTime();

	public static void main(String[] args) throws AlignmentException, URISyntaxException, OWLOntologyCreationException, JWNLException, IOException {


		AlignmentParser aparser = null;
		URIAlignment refalign_EQ_AND_SUB = null;
		URIAlignment refalign_EQ = null;
		URIAlignment refalign_SUB = null;
		
		URIAlignment eq_alignments = null;
		URIAlignment nda_eq_alignment = null;
		URIAlignment noMismatchEQAlignment = null;
		URIAlignment sub_alignment = null;
		URIAlignment mergedEQAndSubAlignment = null;
		URIAlignment nonConflictedMergedAlignment = null;
		File outputAlignment = null;
		
		URIAlignment eqOnly = null;
		URIAlignment subOnly = null;

		for (int i = 0; i < ontos.length; i++) {

			source_onto = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/ONTOLOGIES/" + ontos[i] + "/" + ontos[i] + "-" + ontos[i].substring(0, 3) + ".rdf");
			target_onto = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/ONTOLOGIES/" + ontos[i] + "/" + ontos[i] + "-" + ontos[i].substring(3, ontos[i].length()) + ".rdf");
			reference_alignment_eq_and_sub ="./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/REFALIGN/" + ontos[i] + "/" + ontos[i].substring(0, 3) + "-" + ontos[i].substring(3, ontos[i].length()) + "-EQ_SUB.rdf";
			reference_alignment_eq ="./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/REFALIGN/" + ontos[i] + "/" + ontos[i].substring(0, 3) + "-" + ontos[i].substring(3, ontos[i].length()) + "-EQUIVALENCE.rdf";
			reference_alignment_sub ="./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/REFALIGN/" + ontos[i] + "/" + ontos[i].substring(0, 3) + "-" + ontos[i].substring(3, ontos[i].length()) + "-SUBSUMPTION.rdf";			eq_folder = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/CUT_THRESHOLD/MERGED_NOWEIGHT/EQ";
			sub_folder = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/CUT_THRESHOLD/MERGED_NOWEIGHT/SUB";

			aparser = new AlignmentParser(0);
			refalign_EQ_AND_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_eq_and_sub)));
			refalign_EQ = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_eq)));
			refalign_SUB = (URIAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(reference_alignment_sub)));

			//combine EQ alignments into a single alignment
			eq_alignments = combineAlignments(eq_folder);

			//enforce 1-1 eq relations using naive descending extraction
			nda_eq_alignment = NaiveDescendingExtraction.extractOneToOneRelations(eq_alignments);

			//filter out potential mismatches
			noMismatchEQAlignment = MismatchDetection.removeMismatches(nda_eq_alignment, source_onto, target_onto);
			
			double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};

//			double precision = 0;
//			double recall = 0;
//			double fMeasure = 0;
//			PRecEvaluator eval = null;
//			Properties p = new Properties();
			
			
			//combine all SUB alignments into a single alignment
			sub_alignment = combineAlignments(sub_folder);
			

			//merge the "merged" EQ alignment and SUB alignment 
			mergedEQAndSubAlignment = AlignmentOperations.combineEQAndSUBAlignments(noMismatchEQAlignment, sub_alignment);

			//resolve potential conflicts in the merged EQ and SUB alignment
			nonConflictedMergedAlignment = AlignmentConflictResolution.resolveAlignmentConflict(mergedEQAndSubAlignment);
			
			
			
			//isolate the equivalence relations and evaluate the equivalence alignment only
			eqOnly = AlignmentOperations.extractEquivalenceRelations(nonConflictedMergedAlignment);

			Map<String, EvaluationScore> eqEvaluationMap = new TreeMap<String, EvaluationScore>();

			for (double conf : confidence) {
				//EvaluationScore evalScore = new EvaluationScore();
				eqOnly.cut(conf);
//				eval = new PRecEvaluator(refalign_EQ, eqOnly);
//				eval.eval(p);
//				precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//				recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//				fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//				evalScore.setPrecision(precision);
//				evalScore.setRecall(recall);
//				evalScore.setfMeasure(fMeasure);
				EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(eqOnly, refalign_EQ);
				//put the evalation score according to each confidence value in the map
				eqEvaluationMap.put(String.valueOf(conf), evalScore);			
			}

			Evaluator.evaluateSingleMatcherThresholds(eqEvaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/CUT_THRESHOLD/CUT_THRESHOLD_EQ_ONLY_"+ontos[i]+"_"+date);


			//isolate the subsumption relations and evaluate the subsumption alignment only
			subOnly = AlignmentOperations.extractSubsumptionRelations(nonConflictedMergedAlignment);

			Map<String, EvaluationScore> subEvaluationMap = new TreeMap<String, EvaluationScore>();

			for (double conf : confidence) {
				//EvaluationScore evalScore = new EvaluationScore();
				subOnly.cut(conf);
//				eval = new PRecEvaluator(refalign_SUB, subOnly);
//				eval.eval(p);
//				precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//				recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//				fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//				evalScore.setPrecision(precision);
//				evalScore.setRecall(recall);
//				evalScore.setfMeasure(fMeasure);
				EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(subOnly, refalign_SUB);
				//put the evalation score according to each confidence value in the map
				subEvaluationMap.put(String.valueOf(conf), evalScore);			
			}

			Evaluator.evaluateSingleMatcherThresholds(subEvaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/CUT_THRESHOLD/CUT_THRESHOLD_SUB_ONLY_"+ontos[i]+"_"+date);
			
			

			//evaluate the merged eq and sub alignment and write the alignment files to disk
			PrintWriter writer = null;
			AlignmentVisitor renderer = null;

			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();

			for (double conf : confidence) {
				//EvaluationScore evalScore = new EvaluationScore();
				nonConflictedMergedAlignment.cut(conf);
//				eval = new PRecEvaluator(refalign_EQ_AND_SUB, nonConflictedMergedAlignment);
//				eval.eval(p);
//				precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
//				recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
//				fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
//				evalScore.setPrecision(precision);
//				evalScore.setRecall(recall);
//				evalScore.setfMeasure(fMeasure);
				EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(nonConflictedMergedAlignment, refalign_EQ_AND_SUB);
				//put the evalation score according to each confidence value in the map
				evaluationMap.put(String.valueOf(conf), evalScore);		

				outputAlignment = new File("./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/CUT_THRESHOLD/MERGED_NOWEIGHT/CutThreshold"+DATASET+"_"+ontos[i]+"_"+conf+".rdf");

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);

				nonConflictedMergedAlignment.render(renderer);
				writer.flush();
				writer.close();

			}

			Evaluator.evaluateSingleMatcherThresholds(evaluationMap, "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+DATASET+"/ALIGNMENTS/"+ontos[i]+"/CUT_THRESHOLD/CUT_THRESHOLD_"+ontos[i]+"_"+date);

		}

	}

	/**
	 * Combines (by union) all individual alignments in a folder into a combined alignment
	 * @param inputAlignments
	 * @return
	 * @throws AlignmentException
	 * @throws URISyntaxException
	   May 3, 2019
	 */
	private static URIAlignment combineAlignments (String alignmentFolder) throws AlignmentException, URISyntaxException {

		ArrayList<Alignment> inputAlignments = new ArrayList<Alignment>();
		URIAlignment combinedAlignment = new URIAlignment();

		AlignmentParser aparser = new AlignmentParser(0);

		File folder = new File(alignmentFolder);
		File[] filesInDir = folder.listFiles();
		URIAlignment thisAlignment = null;
		String URI = null;

		for (int i = 0; i < filesInDir.length; i++) {

			URI = StringUtilities.convertToFileURL(alignmentFolder) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			thisAlignment = (URIAlignment) aparser.parse(new URI(URI));
			inputAlignments.add(thisAlignment);
		}

		URI onto1URI = null;
		URI onto2URI = null;

		for (Alignment a : inputAlignments) {

			onto1URI = a.getOntology1URI();
			onto2URI = a.getOntology2URI();

			for (Cell c : a) {

				combinedAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation(), c.getStrength());
			}
		}

		combinedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		return combinedAlignment;

	}

}
