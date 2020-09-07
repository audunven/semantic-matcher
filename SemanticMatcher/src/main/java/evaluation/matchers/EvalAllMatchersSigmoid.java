package evaluation.matchers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import alignmentcombination.NaiveDescendingExtraction;
import equivalencematching.DefinitionEquivalenceMatcherSigmoid;
import equivalencematching.GraphEquivalenceMatcherSigmoid;
import equivalencematching.LexicalEquivalenceMatcherSigmoid;
import equivalencematching.PropertyEquivalenceMatcherSigmoid;
import equivalencematching.WordEmbeddingMatcherSigmoid;
import evaluation.general.EvaluationScore;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import graph.Graph;
import rita.wordnet.jwnl.JWNLException;
import ontologyprofiling.OntologyProfiler;
import subsumptionmatching.CompoundMatcher;
import subsumptionmatching.ContextSubsumptionMatcher;
import subsumptionmatching.DefinitionSubsumptionMatcherSigmoid;
import subsumptionmatching.LexicalSubsumptionMatcher;
import utilities.StringUtilities;

/**
 * Runs a complete evaluation of either equivalence or subsumption matchers using the scores from the ontology profiling as weights on the initial confidence values produced by the matchers.
 * A sigmoid function is used to calculate the final confidence values.
 * @author audunvennesland
 *
 */
public class EvalAllMatchersSigmoid {

	//ATMONTO-AIRM || BIBFRAME-SCHEMAORG || OAEI2011
	final static String DATASET = "ATMONTO-AIRM";

	//EQUIVALENCE || SUBSUMPTION
	final static String RELATIONTYPE = "SUBSUMPTION";

	//WEIGHT || NOWEIGHT || SIGMOID
	final static String WEIGHTTYPE = "SIGMOID";
	static boolean weighted;
	
	//PARAMETERS FOR THE SIGMOID WEIGHT CALCULATION
	static int slope = 3;
	static double rangeMin = 0.5;
	static double rangeMax = 0.7;

	//IF OAEI ONLY
	static String onto1 = "303";
	static String onto2 = "304";

	static File ontoFile1 = null;
	static File ontoFile2 = null;
	static String wiki_vectorFile_normal = null;

	//final static String PREFIX = "file:";

	static String storePath = null;
	static String evalPath = null;

	static String referenceAlignment = null;

	static String alignmentFileName = null;


	public static void main(String[] args) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException, JWNLException {

		if (DATASET.equalsIgnoreCase("ATMONTO-AIRM")) {
			ontoFile1 = new File("./files/_PHD_EVALUATION/"+DATASET+"/ONTOLOGIES/ATMOntoCoreMerged.owl");
			ontoFile2 = new File("./files/_PHD_EVALUATION/"+DATASET+"/ONTOLOGIES/airm-mono.owl");
			wiki_vectorFile_normal = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_embeddings.txt";
			referenceAlignment = "./files/_PHD_EVALUATION/"+DATASET+"/REFALIGN/ReferenceAlignment-"+DATASET+"-" + RELATIONTYPE + ".rdf";

			storePath = "./files/_PHD_EVALUATION/"+DATASET+"/ALIGNMENTS/INDIVIDUAL_ALIGNMENTS/"+ RELATIONTYPE + "_" +RELATIONTYPE;
			evalPath = storePath + "/EXCEL";

		} else if (DATASET.equalsIgnoreCase("BIBFRAME-SCHEMAORG")) {

			ontoFile1 = new File("./files/_PHD_EVALUATION/"+DATASET+"/ONTOLOGIES/bibframe.rdf");
			ontoFile2 = new File("./files/_PHD_EVALUATION/"+DATASET+"/ONTOLOGIES/schema-org.owl");
			wiki_vectorFile_normal = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";
			referenceAlignment = "./files/_PHD_EVALUATION/"+DATASET+"/REFALIGN/ReferenceAlignment-"+DATASET+"-" + RELATIONTYPE + ".rdf";

			storePath = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/INDIVIDUAL_ALIGNMENTS/"+ RELATIONTYPE + "_" +WEIGHTTYPE;
			evalPath = storePath + "/EXCEL";
					
		} else if (DATASET.equalsIgnoreCase("OAEI2011")) {

			ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/" + onto1+onto2 + "/" + onto1+onto2 + "-" + onto1 + ".rdf");
			ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/" + onto1+onto2 + "/" + onto1+onto2 + "-" + onto2 + ".rdf");
			wiki_vectorFile_normal = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_trained.txt";
			referenceAlignment ="./files/_PHD_EVALUATION/OAEI2011/REFALIGN/" + onto1+onto2 + "/" + onto1 + "-" + onto2 + "-" +RELATIONTYPE+".rdf";

			storePath = "./files/_PHD_EVALUATION/OAEI2011/ALIGNMENTS/" + onto1+onto2+ "/INDIVIDUAL_ALIGNMENTS/"+ RELATIONTYPE + "_" +WEIGHTTYPE;
			evalPath = storePath + "/EXCEL";
		}


		//if storepath is not an empty folder, delete the files in it before running experiments
		System.out.println("StorePath is " + storePath);
		File storePathFolder = new File(storePath);

		for (File file: storePathFolder.listFiles()) 
			if (!file.isDirectory()) 
				file.delete();
		

		if (RELATIONTYPE.equals("EQUIVALENCE") && WEIGHTTYPE.equals("SIGMOID")) {

			weighted = true;

			//retrieve ontology profiling scores in Map
			System.out.println("...creating ontology profiles...");
			Map<String, Double> ontologyProfilingScores = OntologyProfiler.computeOntologyProfileScores(ontoFile1, ontoFile2, wiki_vectorFile_normal);
			System.out.println("...ontology profiles created...");

			System.out.println("The ontology profile scores are: ");
			for (Entry<String, Double> e : ontologyProfilingScores.entrySet()) {
				System.out.println("Profiling metric: " + e.getKey() + ", Score: " + e.getValue());
			}

			System.out.println("\nRunning Word Embedding Equivalence Matcher (WEM)");
			runWordEmbeddingEquivalenceMatcher(ontologyProfilingScores.get("cc"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Property Matcher (PM)");
			runPropertyEquivalenceMatcher(ontologyProfilingScores.get("pf"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Graph Matcher (GM)");
			runGraphEquivalenceMatcher(ontologyProfilingScores.get("sp"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Lexical Matcher (LM)");
			runLexicalEquivalenceMatcher(ontologyProfilingScores.get("lcw"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Definitions Equivalence Matcher (DEM)");
			runDefinitionEquivalenceMatcher(ontologyProfilingScores.get("cc"), weighted, slope, rangeMin, rangeMax);
		}

		else if (RELATIONTYPE.equals("SUBSUMPTION") && WEIGHTTYPE.equalsIgnoreCase("SIGMOID")) {

			weighted = true;

			//retrieve ontology profiling scores in Map
			System.out.println("...creating ontology profiles...");
			Map<String, Double> ontologyProfilingScores = OntologyProfiler.computeOntologyProfileScores(ontoFile1, ontoFile2, wiki_vectorFile_normal);
			System.out.println("...ontology profiles created...");

			System.out.println("The ontology profile scores are: ");
			for (Entry<String, Double> e : ontologyProfilingScores.entrySet()) {
				System.out.println("Profiling metric: " + e.getKey() + ", Score: " + e.getValue());
			}

			System.out.println("\nRunning Compound Matcher (CM)");
			runCompoundMatcher(ontologyProfilingScores.get("cf"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Context Subsumption Matcher (CSM)");
			runContextSubsumptionMatcher(ontologyProfilingScores.get("sp"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Lexical Subsumption Matcher (LSM)");
			runLexicalSubsumptionMatcher(ontologyProfilingScores.get("lcw"), weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Definitions Subsumption Matcher (DSM)");
			runDefinitionsSubsumptionMatcher(ontologyProfilingScores.get("dc"), weighted, slope, rangeMin, rangeMax);						
		}

		if (RELATIONTYPE.equals("EQUIVALENCE") && WEIGHTTYPE.equalsIgnoreCase("NOWEIGHT")) {

			weighted = false;

			System.out.println("\nRunning Word Embedding Equivalence Matcher (WEM)");
			runWordEmbeddingEquivalenceMatcher(1.0, weighted, slope, rangeMin, rangeMax);
			
			System.out.println("\nRunning Definitions Equivalence Matcher (DEM)");
			runDefinitionEquivalenceMatcher(1.0, weighted, slope, rangeMin, rangeMax);	

			System.out.println("\nRunning Property Matcher (PM)");
			runPropertyEquivalenceMatcher(1.0, weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Graph Matcher (GM)");
			runGraphEquivalenceMatcher(1.0, weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Lexical Matcher (LM)");
			runLexicalEquivalenceMatcher(1.0, weighted, slope, rangeMin, rangeMax);


		}  else if (RELATIONTYPE.equals("SUBSUMPTION") && WEIGHTTYPE.equalsIgnoreCase("NOWEIGHT")) {

			weighted = false;

			System.out.println("\nRunning Compound Matcher (CM)");
			runCompoundMatcher(1.0, weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Context Subsumption Matcher (LSM)");
			runContextSubsumptionMatcher(1.0, weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Lexical Subsumption Matcher (LSM)");
			runLexicalSubsumptionMatcher(1.0, weighted, slope, rangeMin, rangeMax);

			System.out.println("\nRunning Definitions Subsumption Matcher (DSM)");
			runDefinitionsSubsumptionMatcher(1.0, weighted, slope, rangeMin, rangeMax);
		}
			
	}
	

		/**
		 * Runs the WordEmbeddingMatcher (WEM) and produces alignments at thresholds 0.1-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runWordEmbeddingEquivalenceMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
			
			System.out.println("\nRunning the WEM matcher");

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));

			AlignmentProcess a = new WordEmbeddingMatcherSigmoid(onto1, onto2, wiki_vectorFile_normal, profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());

			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
			
			BasicAlignment clonedAlignment = null;
			URIAlignment tempAlignment = null;
			URIAlignment one2oneAlignment = null;
			BasicAlignment cartesianProductAlignment = null;
			
			//this is just for properly placing the output alignments and evaluation scores in appropriate folders when they are stored
			if (weighted == true) {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-WordEmbeddingMatcher_WEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();
				
				
				//cut thresholds and enforce 1-1 relations
				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-WordEmbeddingMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					//using the alignment holding only 1-1 relations as basis for the evaluation
					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
										
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);
					
					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("WEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/WordEmbeddingMatcher_WEIGHT" + confidences[i] + ".txt");

				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/WordEmbeddingMatcher_WEIGHT");


			} else {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-WordEmbeddingMatcher_NOWEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();
				

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-WordEmbeddingMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					//using the alignment holding only 1-1 relations as basis for the evaluation
					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);
					
					System.out.println("one2oneAlignment at confidence " + confidences[i] + "contains " + one2oneAlignment.nbCells() + " cells");

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("WEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/WordEmbeddingMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/WordEmbeddingMatcher_NOWEIGHT");

			}

		}


		/**
		 * Runs the DefinitionEquivalenceMatcher (DEM) and produces alignments at thresholds 0.1-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runDefinitionEquivalenceMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
						
			System.out.println("\nRunning the DEM matcher");

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));

			AlignmentProcess a = new DefinitionEquivalenceMatcherSigmoid(onto1, onto2, wiki_vectorFile_normal, profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
			
			BasicAlignment clonedAlignment = null;
			URIAlignment tempAlignment = null;
			URIAlignment one2oneAlignment = null;


			if (weighted == true) {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-DefinitionEquivalenceMatcher_WEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();
				

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-DefinitionEquivalenceMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("DEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/DefinitionEquivalenceMatcher_WEIGHT" + confidences[i] + ".txt");

				}

				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/DefinitionEquivalenceMatcher_WEIGHT");

			} else {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-DefinitionEquivalenceMatcher_NOWEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-DefinitionEquivalenceMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("DEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/DefinitionEquivalenceMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}

				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/DefinitionEquivalenceMatcher_NOWEIGHT");
			}
			
			

		}

		/**
		 * Runs the PropertyEquivalenceMatcher (PEM) and produces alignments at thresholds 0.1-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runPropertyEquivalenceMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
			System.out.println("\nRunning the PEM matcher");

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));

			
			AlignmentProcess a = new PropertyEquivalenceMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
			
			BasicAlignment clonedAlignment = null;
			URIAlignment tempAlignment = null;
			URIAlignment one2oneAlignment = null;


			if (weighted == true) {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-PropertyMatcher_WEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-PropertyMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("PEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/PropertyMatcher_WEIGHT" + confidences[i] + ".txt");

				}

				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/PropertyMatcher_WEIGHT");

			} else {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-PropertyMatcher_NOWEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-PropertyMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("PEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/PropertyMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/PropertyMatcher_NOWEIGHT");

			}

		}


		/**
		 * Runs the GraphEquivalenceMatcher (GEM) and produces alignments at thresholds 0.1-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runGraphEquivalenceMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
			
			System.out.println("\nRunning the GEM matcher");


			OWLOntologyManager manager = null;
			OWLOntology onto1 = null;
			OWLOntology onto2 = null;

			Timestamp timestamp = new Timestamp(System.currentTimeMillis());


			//create new graphs
			manager = OWLManager.createOWLOntologyManager();
			onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);


			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));

			AlignmentProcess a = new GraphEquivalenceMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);

			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();

			BasicAlignment clonedAlignment = null;
			URIAlignment tempAlignment = null;
			URIAlignment one2oneAlignment = null;

			if (weighted == true) {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the ProfileWeight approach
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-GraphMatcher_WEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();
				

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-GraphMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("GEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/GraphMatcher_WEIGHT" + confidences[i] + ".txt");

				}

				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/GraphMatcher_WEIGHT");
				
			} else {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-GraphMatcher_NOWEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();
				

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-GraphMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("GEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/GraphMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}

				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/GraphMatcher_NOWEIGHT");
			}

		}


		/**
		 * Runs the LexicalEquivalenceMatcher (LEM) and produces alignments at thresholds 0.1-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runLexicalEquivalenceMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
			System.out.println("\nRunning the LEM matcher");
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));
		
			AlignmentProcess a = new LexicalEquivalenceMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();
			
			BasicAlignment clonedAlignment = null;
			URIAlignment tempAlignment = null;
			URIAlignment one2oneAlignment = null;


			if (weighted == true) {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-LexicalEquivalenceMatcher_WEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-LexicalEquivalenceMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( clonedAlignment.getOntology1URI(), clonedAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("LEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/LexicalEquivalenceMatcher_WEIGHT" + confidences[i] + ".txt");

				}

				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/LexicalEquivalenceMatcher_WEIGHT");

			} else {
				
				//store the cardesian product alignment (basically all possible relations) since this is used for the HADAPT and ProfileWeight approaches
				BasicAlignment cartesianProductAlignment = (BasicAlignment)(a.clone());
				alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
						"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-LexicalEquivalenceMatcher_NOWEIGHT"+0.0+".rdf";
				
				outputAlignment = new File(alignmentFileName);

				writer = new PrintWriter(
						new BufferedWriter(
								new FileWriter(outputAlignment)), true); 
				renderer = new RDFRendererVisitor(writer);
				cartesianProductAlignment.render(renderer);
				writer.flush();
				writer.close();
				

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-LexicalEquivalenceMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.toURIAlignment();
					//no normalisation needed as long as the naive descending extraction is applied further down
					//evaluatedAlignment.normalise();
					evaluatedAlignment.cut(confidences[i]);
					
					clonedAlignment = (BasicAlignment)(evaluatedAlignment.clone());
					tempAlignment = clonedAlignment.toURIAlignment();
					tempAlignment.init( clonedAlignment.getOntology1URI(), clonedAlignment.getOntology2URI(), A5AlgebraRelation.class, BasicConfidence.class );
					one2oneAlignment = NaiveDescendingExtraction.extractOneToOneRelations(tempAlignment);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, one2oneAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					one2oneAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("LEM with confidence " + confidences[i], one2oneAlignment, referenceAlignment, evalPath+"/LexicalEquivalenceMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/LexicalEquivalenceMatcher_NOWEIGHT");

			}

		}


		/**
		 * Runs the CompoundMatcher (CM) and produces alignments at thresholds 0.1-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runCompoundMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException {
			System.out.println("\nRunning the CM matcher");

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));
		
			AlignmentProcess a = new CompoundMatcher(profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();


			if (weighted == true) {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-CompoundMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);
					
					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("CM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/CompoundMatcher_WEIGHT" + confidences[i] + ".txt");

				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/CompoundMatcher_WEIGHT");


			} else {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-CompoundMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("CM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/CompoundMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/CompoundMatcher_NOWEIGHT");

			}

		}

		/**
		 * Runs the ContextSubsumptionMatcher (CSM) and produces alignments at thresholds 0.0-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runContextSubsumptionMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
			System.out.println("\nRunning the CSM matcher");
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));
		
			AlignmentProcess a = new ContextSubsumptionMatcher(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();


			if (weighted == true) {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-ContextSubsumptionMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("CSM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/ContextSubsumptionMatcher_WEIGHT" + confidences[i] + ".txt");

				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/ContextSubsumptionMatcher_WEIGHT");


			} else {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-ContextSubsumptionMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("CSM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/ContextSubsumptionMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/ContextSubsumptionMatcher_NOWEIGHT");

			}

		}


		/**
		 * Runs the LexicalSubsumptionMatcher (LSM) and produces alignments at thresholds 0.0-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runLexicalSubsumptionMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException {
			System.out.println("\nRunning the LSM matcher");
			
			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));
		
			AlignmentProcess a = new LexicalSubsumptionMatcher(profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();


			if (weighted == true) {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-LexicalSubsumptionMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("LSM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/LexicalSubsumptionMatcher_WEIGHT" + confidences[i] + ".txt");

				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/LexicalSubsumptionMatcher_WEIGHT");


			} else {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-LexicalSubsumptionMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("LSM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/LexicalSubsumptionMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/LexicalSubsumptionMatcher_NOWEIGHT");

			}

		}

		/**
		 * Runs the DefinitionsSubsumptionMatcher (DSM) and produces alignments at thresholds 0.0-1.0 along with an evaluation summary in Excel.
		 * @param profileScore the score obtained from the ontology profiling process
		 * @param weighted weighted whether or not a weight should be applied.
		 * @param slope a slope parameter used in the sigmoid function
		 * @param rangeMin the min value of the confidence transformation
		 * @param rangeMax the max value of the confidence transformation
		 * @throws AlignmentException
		 * @throws URISyntaxException
		 * @throws IOException
		 * @throws OWLOntologyCreationException
		   Jul 16, 2019
		 */
		private static void runDefinitionsSubsumptionMatcher(double profileScore, boolean weighted, int slope, double rangeMin, double rangeMax) throws AlignmentException, URISyntaxException, IOException, OWLOntologyCreationException {
			System.out.println("\nRunning the DSM matcher");
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
			OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

			AlignmentParser aparser = new AlignmentParser(0);
			Alignment refalign = aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));
		
			AlignmentProcess a = new DefinitionSubsumptionMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
			a.init(ontoFile1.toURI(), ontoFile2.toURI());
			Properties params = new Properties();
			params.setProperty("", "");
			a.align((Alignment)null, params);	
			AlignmentVisitor renderer = null; 
			File outputAlignment = null;
			BasicAlignment evaluatedAlignment = null;
			PrintWriter writer = null;

			String alignmentFileName = null;

			double[] confidences = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
			double precision = 0;
			double recall = 0;
			double fMeasure = 0;
			PRecEvaluator eval = null;
			Properties p = new Properties();
			Map<String, EvaluationScore> evaluationMap = new TreeMap<String, EvaluationScore>();


			if (weighted == true) {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-DefinitionSubsumptionMatcher_WEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("DSM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/DefinitionSubsumptionMatcher_WEIGHT" + confidences[i] + ".txt");

				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/DefinitionSubsumptionMatcher_WEIGHT");


			} else {

				for (int i = 0; i < confidences.length; i++) {
					EvaluationScore evalScore = new EvaluationScore();

					alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
							"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "-DefinitionSubsumptionMatcher_NOWEIGHT"+confidences[i]+".rdf";

					outputAlignment = new File(alignmentFileName);


					writer = new PrintWriter(
							new BufferedWriter(
									new FileWriter(outputAlignment)), true); 
					renderer = new RDFRendererVisitor(writer);

					evaluatedAlignment = (BasicAlignment)(a.clone());
					evaluatedAlignment.cut(confidences[i]);

					//perform the evaluation here...				
					eval = new PRecEvaluator(refalign, evaluatedAlignment);

					eval.eval(p);

					precision = Double.valueOf(eval.getResults().getProperty("precision").toString());
					recall = Double.valueOf(eval.getResults().getProperty("recall").toString());
					fMeasure = Double.valueOf(eval.getResults().getProperty("fmeasure").toString());

					evalScore.setPrecision(precision);
					evalScore.setRecall(recall);
					evalScore.setfMeasure(fMeasure);
					evaluationMap.put(String.valueOf(confidences[i]), evalScore);

					evaluatedAlignment.render(renderer);

					writer.flush();
					writer.close();
					
					Evaluator.evaluateSingleAlignment("DSM with confidence " + confidences[i], evaluatedAlignment, referenceAlignment, evalPath+"/DefinitionSubsumptionMatcher_NOWEIGHT" + confidences[i] + ".txt");
				}
				
				Evaluator.evaluateSingleMatcherThresholds(evaluationMap, evalPath+"/DefinitionSubsumptionMatcher_NOWEIGHT");

			}

		}

		private static void registerShutdownHook(final GraphDatabaseService db)
		{
			Runtime.getRuntime().addShutdownHook( new Thread()
			{
				@Override
				public void run()
				{
					db.shutdown();

				}
			} );
		}
	}


