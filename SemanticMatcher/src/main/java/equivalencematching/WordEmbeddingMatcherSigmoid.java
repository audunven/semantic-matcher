package equivalencematching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import alignmentcombination.HarmonyEquivalence;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import utilities.Sigmoid;
import wordembedding.VectorExtractor;

/**
 * Matches concepts from two ontologies based on their global vectors (average of label vectors and comment vectors).
 * @author audunvennesland
 *
 */
public class WordEmbeddingMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;
	OWLOntology sourceOntology;
	OWLOntology targetOntology;
	String vectorFile;
	
	static Map<String, double[]> vectorMapSourceOntology = new HashMap<String, double[]>();
	static Map<String, double[]> vectorMapTargetOntology = new HashMap<String, double[]>();

	public WordEmbeddingMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, String vectorFile, double profileScore, int slope, double rangeMin, double rangeMax) {
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.sourceOntology = onto1;
		this.targetOntology = onto2;
		this.vectorFile = vectorFile;
	}
	
	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {
		
//		File ontoFile1 = new File("./files/SATest1.owl");
//		File ontoFile2 = new File("./files/SATest2.owl");
//		String referenceAlignment = "./files/ReferenceAlignmentSATest.rdf";
//		String vectorFile = "./files//_PHD_EVALUATION/EMBEDDINGS/wikipedia_trained.txt";

//		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
//		String referenceAlignment = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";
//		String vectorFile = "./files//_PHD_EVALUATION/EMBEDDINGS/wikipedia_trained.txt";

		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";
		String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_trained_ontology_tokens.txt";
		

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double testProfileScore = 0.84;
		int testSlope = 12;
		double testRangeMin = 0.5;
		double testRangeMax = 0.7;

		AlignmentProcess a = new WordEmbeddingMatcherSigmoid(sourceOntology, targetOntology, vectorFile, testProfileScore, testSlope, testRangeMin, testRangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment WordEmbeddingMatcherSigmoidAlignment = new BasicAlignment();

		WordEmbeddingMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		WordEmbeddingMatcherSigmoidAlignment.normalise();

		//evaluate the Harmony alignment
		BasicAlignment harmonyAlignment = HarmonyEquivalence.getHarmonyAlignment(WordEmbeddingMatcherSigmoidAlignment);
		System.out.println("The Harmony alignment contains " + harmonyAlignment.nbCells() + " cells");
		Evaluator.evaluateSingleAlignment(harmonyAlignment, referenceAlignment);

		System.out.println("Printing Harmony Alignment: ");
		for (Cell c : harmonyAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("\nThe alignment contains " + WordEmbeddingMatcherSigmoidAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherSigmoidAlignment, referenceAlignment);


		System.out.println("Evaluation with threshold 0.2:");
		WordEmbeddingMatcherSigmoidAlignment.cut(0.2);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherSigmoidAlignment, referenceAlignment);
		
		System.out.println("Printing relations at 0.2:");
		for (Cell c : WordEmbeddingMatcherSigmoidAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.4:");
		WordEmbeddingMatcherSigmoidAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherSigmoidAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		WordEmbeddingMatcherSigmoidAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherSigmoidAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.9:");
		WordEmbeddingMatcherSigmoidAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherSigmoidAlignment, referenceAlignment);


	}
	
	
	public static Map<String, double[]> createVectorMap (OWLOntology onto, String vectorFile) throws IOException {
		
		Map<String, double[]> vectors = new HashMap<String, double[]>();
		
		//create the vector map from the source vector file
		Map<String, ArrayList<Double>> vectorMap = VectorExtractor.createVectorMap (new File(vectorFile));
		ArrayList<Double> labelVector = new ArrayList<Double>();
		
		
		for (OWLClass cls : onto.getClassesInSignature()) {

			if (vectorMap.containsKey(cls.getIRI().getFragment().toLowerCase())) {				
				
				labelVector = VectorExtractor.getLabelVector(cls.getIRI().getFragment(), vectorMap);
				
				double[] labelVectorArray = new double[labelVector.size()];
				for (int i = 0; i < labelVectorArray.length; i++) {
					labelVectorArray[i] = labelVector.get(i);
				}
				vectors.put(cls.getIRI().getFragment().toLowerCase(), labelVectorArray);
			}	
		}

		return vectors;
		
		
	}
	
	public static URIAlignment returnWEMAlignment (File ontoFile1, File ontoFile2, String vectorFile, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {
		
		URIAlignment WEMAlignment = new URIAlignment();
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
		AlignmentProcess a = new WordEmbeddingMatcherSigmoid(onto1, onto2, vectorFile, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment WordEmbeddingMatcherSigmoidAlignment = new BasicAlignment();

		WordEmbeddingMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		WordEmbeddingMatcherSigmoidAlignment.normalise();
		
		WEMAlignment = WordEmbeddingMatcherSigmoidAlignment.toURIAlignment();
		
		WEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
		
		return WEMAlignment;
		
	}

	public void align(Alignment alignment, Properties param) throws AlignmentException {

		try {
			vectorMapSourceOntology = createVectorMap(sourceOntology, vectorFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			vectorMapTargetOntology = createVectorMap(targetOntology, vectorFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		double[] sourceVectors = null;
		double[] targetVectors = null;

		double cosineSim = 0;
		int idCounter = 0;

		
		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){

					idCounter++;
					
					String source = ontology1().getEntityName(sourceObject).toLowerCase();
					String target = ontology2().getEntityName(targetObject).toLowerCase();

					if (vectorMapSourceOntology.containsKey(source) && vectorMapTargetOntology.containsKey(target)) {
						
						sourceVectors = vectorMapSourceOntology.get(source);
						targetVectors = vectorMapTargetOntology.get(target);

						//ensure that both vectors have the same, correct size (not sure why they shouldn´t be...)
						if (sourceVectors.length == 300 && targetVectors.length == 300) {

							cosineSim = utilities.Cosine.cosineSimilarity(sourceVectors, targetVectors);
							
							//calculate the final confidence using a sigmoid function
							addAlignCell("WordEmbeddingMatcherSigmoid" +idCounter, sourceObject, targetObject, "=", 
									Sigmoid.weightedSigmoid(slope, cosineSim, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));

						} else {
							
							addAlignCell("WordEmbeddingMatcherSigmoid" +idCounter, sourceObject, targetObject, "=", 0);

						}


					} else {
						
						addAlignCell("WordEmbeddingMatcherSigmoid" +idCounter, sourceObject, targetObject, "=", 0);
						
					}

				}
			}

		} catch (Exception e) { e.printStackTrace(); }
		



	}


}


