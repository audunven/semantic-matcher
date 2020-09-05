package evaluation.competition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.SemPRecEvaluator;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;

public class EvaluateCompetitionSemPrecRec {
	
	public static void main(String[] args) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
		
		//which matcher is being evaluated?
		String matcher = "LOGMAP";
		
		/* IF OAEI */
		//which ontologies?
		String onto1 = "303";
		String onto2 = "304";
		
		
		//evaluate single alignment file for semantic precision and recall
		String evaluatedAlignmentFile = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/OAEI2011/"+onto1+onto2+"/"+matcher+"/input/LogMap_"+onto1+""+onto2+".rdf";
		String referenceAlignmentFile = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/OAEI2011/"+onto1+onto2+"/REFALIGN/"+onto1+"-"+onto2+"-EQ_SUB.rdf";
		
		evaluateSemPrecRecFile (evaluatedAlignmentFile, referenceAlignmentFile);	
		
		//extract alignment files for each confidence threshold 0.1-1.0
		String alignmentFolder = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/OAEI2011/"+onto1+onto2+"/"+matcher+"/output";
		String ontoFile1 = "/Users/audunvennesland/ontologies/OAEI2011/"+onto1+onto2+"/"+onto1+onto2+"-"+onto1+".rdf";
		String ontoFile2 = "/Users/audunvennesland/ontologies/OAEI2011/"+onto1+onto2+"/"+onto1+onto2+"-"+onto2+".rdf";
		
		extractAllConfidenceThresholds(matcher, evaluatedAlignmentFile, alignmentFolder, ontoFile1, ontoFile2);
		
		//evaluate all extracted alignments using semantic precision and recall and produce evaluation sheet
		String resultsOutput = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/OAEI2011/"+onto1+onto2+"/"+matcher+"/evaluationresults/evaluation_results_sem_prec_rec_"+matcher+"_"+onto1+onto2;
		
		evaluateSemPrecRecFolder (alignmentFolder, referenceAlignmentFile, resultsOutput);
		
		System.out.println("Results written to " + resultsOutput);

		/* IF ATM OR CROSS-DOMAIN 
		//which ontologies?
		String onto1 = "ATMONTO";
		String onto2 = "AIRM";
		
		//evaluate single alignment file for semantic precision and recall
		String evaluatedAlignmentFile = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/"+onto1+"-"+onto2+"/"+matcher+"/input/LOGMAP_ATM_NOPROPS.rdf";
		String referenceAlignmentFile = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/"+onto1+"-"+onto2+"/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQ-SUB.rdf";
		
		evaluateSemPrecRecFile (evaluatedAlignmentFile, referenceAlignmentFile);		
		
		//extract alignment files for each confidence threshold 0.1-1.0
		String alignmentFolder = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/"+onto1+"-"+onto2+"/"+matcher+"/output";
		String ontoFile1 = "/Users/audunvennesland/ontologies/CD/bibframe.rdf";
		String ontoFile2 = "/Users/audunvennesland/ontologies/CD/schema-org.owl";
		
		extractAllConfidenceThresholds(matcher, evaluatedAlignmentFile, alignmentFolder, ontoFile1, ontoFile2);

		//evaluate all extracted alignments using semantic precision and recall and produce evaluation sheet
		String resultsOutput = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/"+onto1+"-"+onto2+"/"+matcher+"/evaluationresults/evaluation_results_sem_prec_rec_"+matcher+"_"+onto1+"-"+onto2;		

		evaluateSemPrecRecFolder (alignmentFolder, referenceAlignmentFile, resultsOutput);
		 */
		
	}
	
	public static void evaluateSemPrecRecFile (String alignmentFilePath, String referenceAlignmentFilePath) throws AlignmentException, URISyntaxException {
		
		AlignmentParser refAlignParser = new AlignmentParser(0);
		AlignmentParser evaluatedAlignParser = new AlignmentParser(0);
		
		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFilePath)));
		URIAlignment evaluatedAlignment = (URIAlignment) evaluatedAlignParser.parse(new URI(StringUtilities.convertToFileURL(alignmentFilePath)));
		
		Properties p = new Properties();
		SemPRecEvaluator eval = new SemPRecEvaluator(referenceAlignment, evaluatedAlignment);
		
		
		eval.eval(p);

		System.out.println("------------------------------");
		System.out.println("Evaluator scores for " + alignmentFilePath.substring(alignmentFilePath.lastIndexOf("/")+1));
		System.out.println("------------------------------");
		System.out.println("F-measure: " + eval.getResults().getProperty("fmeasure").toString());
		System.out.println("Precision: " + eval.getResults().getProperty("precision").toString());
		System.out.println("Recall: " + eval.getResults().getProperty("recall").toString());

		
	}
	
	public static void evaluateSemPrecRecFolder (String folderPath, String referenceAlignmentFile, String resultsOutput) throws AlignmentException, URISyntaxException, IOException {
		
		File folder = new File(folderPath);

		File[] filesInDir = folder.listFiles();
		
		Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
		
		double conf = 0;
		
		URIAlignment evaluatedAlignment = null;	
		
		AlignmentParser refAlignParser = new AlignmentParser(0);
		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFile)));
		
		Properties p = null;
				
		for (int i = 0; i < filesInDir.length; i++) {
			
			conf = Double.valueOf(filesInDir[i].getName().substring(filesInDir[i].getName().lastIndexOf("_")+1, filesInDir[i].getName().lastIndexOf(".")));
			
			AlignmentParser parser = new AlignmentParser(0);
			evaluatedAlignment = (URIAlignment) parser.parse(new URI(StringUtilities.convertToFileURL(filesInDir[i].getPath())));
			
			EvaluationScore evalScore = new EvaluationScore();
			evaluationMap.put(String.valueOf(conf), evalScore);	
			
			p = new Properties();
			
			SemPRecEvaluator eval = new SemPRecEvaluator(referenceAlignment, evaluatedAlignment);
			
			eval.eval(p);
			
			evalScore.setPrecision(Double.valueOf(eval.getResults().getProperty("precision")));
			evalScore.setRecall(Double.valueOf(eval.getResults().getProperty("recall")));
			evalScore.setfMeasure(Double.valueOf(eval.getResults().getProperty("fmeasure")));
			
			

		}
		
		System.out.println("evaluationMap contains " + evaluationMap.size() + " entries");
		
		Evaluator.evaluateSingleMatcherThresholds(evaluationMap, resultsOutput);
		
		for (Entry<String, EvaluationScore> e : evaluationMap.entrySet()) {
			System.out.println("\nConfidence: " + e.getKey());
			System.out.println("Precision: " + e.getValue().getPrecision());
			System.out.println("Recall: " + e.getValue().getRecall());
			System.out.println("F-measure: " + e.getValue().getfMeasure());
		}
		
		
	}
	
	public static void extractAllConfidenceThresholds (String matcher, String alignmentPath, String storePath, String ontoFile1, String ontoFile2) throws AlignmentException, IOException, OWLOntologyCreationException {
				
		AlignmentParser parser = new AlignmentParser();		
		URIAlignment alignment = (URIAlignment) parser.parse(new File(alignmentPath).toURI().toString());
		
		double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
				
		AlignmentVisitor renderer = null; 
		File outputAlignment = null;
		PrintWriter writer = null;
		String alignmentFileName = null;
		BasicAlignment cutAlignment = null;
		BasicAlignment clonedAlignment = null;
		URIAlignment tempAlignment = null;
				
		for (double conf : confidence) {
			cutAlignment = (BasicAlignment)(alignment.clone());
			cutAlignment.toURIAlignment();
			cutAlignment.cut(conf);
			
			alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
					"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "_" + matcher + "_"+ conf + ".rdf";
			
			outputAlignment = new File(alignmentFileName);
			
			writer = new PrintWriter(
					new BufferedWriter(
							new FileWriter(outputAlignment)), true); 
			renderer = new RDFRendererVisitor(writer);
			
			clonedAlignment = (BasicAlignment)(cutAlignment.clone());			
			
			tempAlignment = clonedAlignment.toURIAlignment();
						
			tempAlignment.render(renderer);
			
			writer.flush();
			writer.close();
			

		}
	}
	
	

}
