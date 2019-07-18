package equivalencematching;

import java.io.File;
import java.io.FileNotFoundException;
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
import utilities.OntologyOperations;
import utilities.Sigmoid;
import wordembedding.VectorExtractor;

/**
 * The Definitions Equivalence Matcher identifies equivalent concepts from the cosine similarity between embedding vectors associated with their labels and definitions.
 * In this class weights are imposed from the ontology profiling scores.
 */
public class DefinitionEquivalenceMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {
	
	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;
	String vectorFile;
	
	OWLOntology sourceOntology;
	OWLOntology targetOntology;
	
	static Map<String, double[]> sourceVectorMap = new HashMap<String, double[]>();
	static Map<String, double[]> targetVectorMap = new HashMap<String, double[]>();

	//constructor for sigmoid weighting scenario
	public DefinitionEquivalenceMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, String vectorFile, double profileScore, int slope, double rangeMin, double rangeMax) {
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		sourceOntology = onto1;
		targetOntology = onto2;
		this.vectorFile = vectorFile;
	}

	
	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {
		
		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";
		String vectorFile = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_embeddings.txt";
		
//		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
//		String referenceAlignment = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";
//		String vectorFile = "./files//_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";
		
//		File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-301.rdf");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-304.rdf");
//		String referenceAlignment = "./files/_PHD_EVALUATION/OAEI2011/REFALIGN/301304/301-304-EQUIVALENCE.rdf";
//		String vectorFile = "./files//_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double testProfileScore = 0.84;
		int testSlope = 12;
		double testRangeMin = 0.5;
		double testRangeMax = 0.7;

		AlignmentProcess a = new DefinitionEquivalenceMatcherSigmoid(sourceOntology, targetOntology, vectorFile, testProfileScore, testSlope, testRangeMin, testRangeMax);
		a.init(sourceOntology.getOntologyID().getOntologyIRI().toURI(), targetOntology.getOntologyID().getOntologyIRI().toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment definitionEquivalenceMatcherAlignment = new BasicAlignment();

		definitionEquivalenceMatcherAlignment = (BasicAlignment) (a.clone());

		definitionEquivalenceMatcherAlignment.normalise();

		System.out.println("\nThe alignment contains " + definitionEquivalenceMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(definitionEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.2:");
		definitionEquivalenceMatcherAlignment.cut(0.2);
		Evaluator.evaluateSingleAlignment(definitionEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		definitionEquivalenceMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(definitionEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		definitionEquivalenceMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(definitionEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.9:");
		definitionEquivalenceMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(definitionEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Printing relations at 0.9:");
		for (Cell c : definitionEquivalenceMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

	}
	
	/**
	 * Returns an alignment holding equivalence relations computed by the Definition Equivalence Matcher. 
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param vectorFile a vector file with embeddings
	 * @param profileScore the score from the ontology profiling process
	 * @param slope the sigmoid slope parameter
	 * @param rangeMax the max value of the confidence transformation
	 * @param rangeMin the min value of the confidence transformation
	 * @return an URIAlignment holding a set of relations (cells)
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Jul 14, 2019
	 */
	public static URIAlignment returnDEMAlignment (File ontoFile1, File ontoFile2, String vectorFile, double profileScore, int slope, double rangeMax, double rangeMin) throws OWLOntologyCreationException, AlignmentException {
		
		URIAlignment DEMAlignment = new URIAlignment();
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
		AlignmentProcess a = new DefinitionEquivalenceMatcherSigmoid(onto1, onto2, vectorFile, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment definitionEquivalenceMatcherAlignment = new BasicAlignment();

		definitionEquivalenceMatcherAlignment = (BasicAlignment) (a.clone());

		definitionEquivalenceMatcherAlignment.normalise();
		
		DEMAlignment = definitionEquivalenceMatcherAlignment.toURIAlignment();
		
		DEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
		
		return DEMAlignment;
		
	}
	
	/**
	 * Computes an alignment of semantic relations from measuring the cosine similarity between embedding vectors associated with the ontology concepts being matched.
	 */
	public void align(Alignment alignment, Properties param) throws AlignmentException {
		
		//create the vector map holding word - embedding vectors		
		Map<String, ArrayList<Double>> vectorMap = null;
		
		try {
			vectorMap = VectorExtractor.createVectorMap(new File(vectorFile));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		Map<String, double[]> sourceVectorMap = new HashMap<String, double[]>();
		Map<String, double[]> targetVectorMap = new HashMap<String, double[]>();
		
		double[] sourceGlobalVectors = null;
		double[] targetGlobalVectors = null;
		
		for (OWLClass sourceClass : sourceOntology.getClassesInSignature()) {
			try {
				sourceGlobalVectors = VectorExtractor.getGlobalVector(sourceClass.getIRI().getFragment().toLowerCase(), 
						OntologyOperations.getClassDefinitionFull(sourceOntology, sourceClass), vectorMap);
				
				if (sourceGlobalVectors != null) {
				sourceVectorMap.put(sourceClass.getIRI().getFragment(), sourceGlobalVectors);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
				
		for (OWLClass targetClass : targetOntology.getClassesInSignature()) {
			try {
				targetGlobalVectors = VectorExtractor.getGlobalVector(targetClass.getIRI().getFragment().toLowerCase(), 
						OntologyOperations.getClassDefinitionFull(targetOntology, targetClass), vectorMap);
				
				if (targetGlobalVectors != null) {
				targetVectorMap.put(targetClass.getIRI().getFragment(), targetGlobalVectors);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		double[] sourceVectors = null;
		double[] targetVectors = null;

		double cosineSim = 0;
		double idCounter = 0;
		

		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){

					String source = ontology1().getEntityName(sourceObject);
					String target = ontology2().getEntityName(targetObject);
					
					idCounter++;


					if (sourceVectorMap.containsKey(source) && targetVectorMap.containsKey(target)) {
						
						sourceVectors = sourceVectorMap.get(source);
						targetVectors = targetVectorMap.get(target);
						
						//ensure that both vectors have the same, correct size (not sure why they shouldn´t be...)
						if (sourceVectors.length == 300 && targetVectors.length == 300) {

							cosineSim = utilities.Cosine.cosineSimilarity(sourceVectors, targetVectors);
							
							//calculate the final confidence using a sigmoid function
							addAlignCell("DefinitionEquivalenceMatcher" +idCounter, sourceObject, targetObject, "=", 
									Sigmoid.weightedSigmoid(slope, cosineSim, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
							
							
						} else {
							addAlignCell("DefinitionEquivalenceMatcher" + idCounter, sourceObject, targetObject, "=", 0);

						}


					} else {
						addAlignCell("DefinitionEquivalenceMatcher" + idCounter, sourceObject, targetObject, "=", 0);
						
					}

				}
			}

		} catch (Exception e) { e.printStackTrace(); }
		

	}
	

}
