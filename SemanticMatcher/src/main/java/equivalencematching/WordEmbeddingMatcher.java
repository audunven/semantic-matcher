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

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import wordembedding.VectorExtractor;

/**
 * The WordEmbeddingMatcher matches concepts from two ontologies based on their associated embedding vectors.
 * @author audunvennesland
 *
 */
public class WordEmbeddingMatcher extends ObjectAlignment implements AlignmentProcess {

	double weight;
	
	static OWLOntology sourceOntology;
	static OWLOntology targetOntology;
	static Map<String, double[]> vectorMapSourceOntology = new HashMap<String, double[]>();
	static Map<String, double[]> vectorMapTargetOntology = new HashMap<String, double[]>();

	String vectorFile;

	public WordEmbeddingMatcher(OWLOntology onto1, OWLOntology onto2, String vectorFile, double weight) {
		this.weight = weight;
		sourceOntology = onto1;
		targetOntology = onto2;
		this.vectorFile = vectorFile;
	}
	
	
	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {
		
		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		File ontoFile2 =  new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";
		String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_embeddings.txt";
		

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
		AlignmentProcess a = new WordEmbeddingMatcher(sourceOntology, targetOntology, vectorFile, 1.0);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment WordEmbeddingMatcherAlignment = new BasicAlignment();

		WordEmbeddingMatcherAlignment = (BasicAlignment) (a.clone());

		WordEmbeddingMatcherAlignment.normalise();

		System.out.println("\nThe alignment contains " + WordEmbeddingMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherAlignment, referenceAlignment);
		
		System.out.println("Evaluation with threshold 0.1:");
		WordEmbeddingMatcherAlignment.cut(0.1);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		WordEmbeddingMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		WordEmbeddingMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherAlignment, referenceAlignment);
		
		System.out.println("Printing relations at 0.6:");
		for (Cell c : WordEmbeddingMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.9:");
		WordEmbeddingMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(WordEmbeddingMatcherAlignment, referenceAlignment);


	}
	
	
	
	/**
	 * Returns an alignment holding relations computed by the Word Embedding Matcher (WEM).
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param vectorFile a file holding terms and corresponding embedding vectors.
	 * @param weight a weight imposed on the confidence value (default 1.0)
	 * @return an URIAlignment holding a set of relations (cells)
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Jul 15, 2019
	 */
	public static URIAlignment returnWEMAlignment (File ontoFile1, File ontoFile2, String vectorFile, double weight) throws OWLOntologyCreationException, AlignmentException {
		
		URIAlignment WEMAlignment = new URIAlignment();
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
		AlignmentProcess a = new WordEmbeddingMatcher(onto1, onto2, vectorFile, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment wordEmbeddingMatcherAlignment = new BasicAlignment();

		wordEmbeddingMatcherAlignment = (BasicAlignment) (a.clone());

		wordEmbeddingMatcherAlignment.normalise();
		
		WEMAlignment = wordEmbeddingMatcherAlignment.toURIAlignment();
		
		WEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
		
		return WEMAlignment;
		
	}
	
	/**
	 * Creates a map holding a class as key along with an array of vectors as value.
	 * @param onto an OWL ontology
	 * @param vectorFile a file holding terms and corresponding embedding vectors.
	 * @return a Map<String, double[]) representing classes and corresponding embedding vectors.
	 * @throws IOException
	   Jul 15, 2019
	 */
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

	/**
	 * Creates an alignment holding a set of relations.
	 * The confidence assigned to each relation is computed by the cosine similarity from embedding vectors associated with each concept name.
	 */
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
							
							
							//calculate using the basic profile score sim
							addAlignCell("WordEmbeddingMatcher" +idCounter + "_" + weight + "_", sourceObject, targetObject, "=", cosineSim * weight);

						} else {
							
							addAlignCell("WordEmbeddingMatcher" +idCounter + "_" + weight + "_", sourceObject, targetObject, "=", 0);

						}


					} else {
						
						addAlignCell("WordEmbeddingMatcher" +idCounter + "_" + weight + "_", sourceObject, targetObject, "=", 0);
						
					}

				}
			}

		} catch (Exception e) { e.printStackTrace(); }

	}


}


