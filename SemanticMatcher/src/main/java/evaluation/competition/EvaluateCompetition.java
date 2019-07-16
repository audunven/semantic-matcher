package evaluation.competition;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.semanticweb.owl.align.AlignmentException;

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
		
		//ATMONTO-AIRM || BIBFRAME-SCHEMAORG
		String dataset = "BIBFRAME-SCHEMAORG";
		
		/* IF OAEI 2011 */
//		String onto1 = "303";
//		String onto2 = "304";
//		String referenceAlignment = "./files/_PHD_EVALUATION/"+dataset+"/REFALIGN/"+onto1+onto2+"/"+onto1+"-"+onto2+"-EQ_SUB.rdf";
//		String alignmentFolder = "./files/_PHD_EVALUATION/"+dataset+"/ALIGNMENTS/"+onto1+onto2+"/EVALUATION_COMPETITION/SUBSUMPTION_EQUIVALENCE";
		
		String referenceAlignment = "./files/_PHD_EVALUATION/"+dataset+"/REFALIGN/ReferenceAlignment-"+dataset+"-EQ-SUB.rdf";
		
		String alignmentFolder = "./files/_PHD_EVALUATION/"+dataset+"/ALIGNMENTS/COMPETITION/EVALUATION_COMPETITION/SUBSUMPTION_EQUIVALENCE";
		
		File folder = new File(alignmentFolder);

		File[] filesInDir = folder.listFiles();

		AlignmentParser parser = new AlignmentParser();
		URIAlignment refalign = (URIAlignment) parser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));

		//Evaluate at different confidence thresholds
		double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
		double precision = 0;
		double recall = 0;
		double fMeasure = 0;
		PRecEvaluator eval = null;
		Properties p = new Properties();
		Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
		
		URIAlignment thisAlignment = null;
		String URI = null;

		for (int i = 0; i < filesInDir.length; i++) {
			
			URI = StringUtilities.convertToFileURL(alignmentFolder) + "/" + StringUtilities.stripPath(filesInDir[i].toString());
			thisAlignment = (URIAlignment) parser.parse(new URI(URI));

			for (double conf : confidence) {
				EvaluationScore evalScore = new EvaluationScore();
				thisAlignment.cut(conf);
				eval = new PRecEvaluator(refalign, thisAlignment);
				eval.eval(p);
				precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
				recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
				fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());
				evalScore.setPrecision(precision);
				evalScore.setRecall(recall);
				evalScore.setfMeasure(fMeasure);
				//put the evalation score according to each confidence value in the map
				evaluationMap.put(String.valueOf(conf), evalScore);		

			}
			
			Evaluator.evaluateSingleMatcherThresholds(evaluationMap, alignmentFolder+"_"+StringUtilities.stripPath(filesInDir[i].toString()));

		}

	}

}
