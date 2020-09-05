package evaluation.competition;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.semanticweb.owl.align.AlignmentException;

import evaluation.general.ComputeSyntacticEvaluationScores;
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;

/**
 * Evaluates the alignments produced by BLOOMS (Wikipedia and WordNet version), STROMA and S-Match against the reference alignments in the ATM, Cross-domain and OAEI 2011 datasets.
 * @author audunvennesland
 *
 */
public class EvaluateCompetition {

	public static void main(String[] args) throws AlignmentException, URISyntaxException, IOException {		
		
		//ATMONTO-AIRM || BIBFRAME-SCHEMAORG || OAEI2011
		String dataset = "OAEI2011";
		
		/* IF OAEI 2011 */
		String onto1 = "303";
		String onto2 = "304";
		String referenceAlignment = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+dataset+"/REFALIGN/"+onto1+onto2+"/"+onto1+"-"+onto2+"-EQ_SUB.rdf";
		String alignmentFolder = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+dataset+"/ALIGNMENTS/"+onto1+onto2+"/COMPETITION/EQ_SUB";
						          //files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/OAEI2011/ALIGNMENTS/301302/COMPETITION/EQ_SUB
		
//		String referenceAlignment = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+dataset+"/REFALIGN/ReferenceAlignment-"+dataset+"-EQ-SUB.rdf";
//		
//		String alignmentFolder = "./files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/"+dataset+"/ALIGNMENTS/COMPETITION/EVALUATION_COMPETITION/SUBSUMPTION_EQUIVALENCE";
		
		//						   files/_PHD_EVALUATION/_EVALUATION_SYNPRECREC/ATMONTO-AIRM/COMPETITION/EVALUATION_COMPETITION/SUBSUMPTION_EQUIVALENCE
		
		File folder = new File(alignmentFolder);

		File[] filesInDir = folder.listFiles();

		AlignmentParser parser = new AlignmentParser();
		URIAlignment refalign = (URIAlignment) parser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));

		//Evaluate at different confidence thresholds
		double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};


		Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
		
		URIAlignment thisAlignment = null;
		String URI = null;

		for (int i = 0; i < filesInDir.length; i++) {
			
			URI = StringUtilities.convertToFileURL(alignmentFolder) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			thisAlignment = (URIAlignment) parser.parse(new URI(URI));

			for (double conf : confidence) {
				//EvaluationScore evalScore = new EvaluationScore();
				thisAlignment.cut(conf);
				EvaluationScore evalScore = ComputeSyntacticEvaluationScores.getSyntacticEvaluationScore(thisAlignment, refalign);

				//put the evalation score according to each confidence value in the map
				evaluationMap.put(String.valueOf(conf), evalScore);		
				
				//print the results to screen
				System.out.println("------------------------------");
				System.out.println("Evaluator scores for " + filesInDir[i].toString() + " with confidence " + conf);
				System.out.println("------------------------------");
				System.out.println("F-measure: " + evalScore.getfMeasure());
				System.out.println("Precision: " + evalScore.getPrecision());
				System.out.println("Recall: " + evalScore.getRecall());



			}
			
			Evaluator.evaluateSingleMatcherThresholds(evaluationMap, alignmentFolder+"_"+StringUtilities.stripPath(filesInDir[i].toString()));

		}

	}

}
