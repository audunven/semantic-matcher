package subsumptionmatching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
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
import utilities.StringUtilities;

public class ContextSubsumptionMatcher extends ObjectAlignment implements AlignmentProcess {

	static OWLOntology onto1;
	static OWLOntology onto2;
	
	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;
	
	static Map<String, Set<String>> classesAndSubclassesMapOnto1 = new HashMap<String, Set<String>>();
	static Map<String, Set<String>> classesAndSubclassesMapOnto2 = new HashMap<String, Set<String>>();

	static Map<String, Set<String>> classesAndSuperclassesMapOnto1 = new HashMap<String, Set<String>>();
	static Map<String, Set<String>> classesAndSuperclassesMapOnto2 = new HashMap<String, Set<String>>();

	public ContextSubsumptionMatcher(OWLOntology ontoFile1, OWLOntology ontoFile2, double profileScore) {
		onto1 = ontoFile1;
		onto2 = ontoFile2;
		this.profileScore = profileScore;
	}
	
	public ContextSubsumptionMatcher(OWLOntology ontoFile1, OWLOntology ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) {
		onto1 = ontoFile1;
		onto2 = ontoFile2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	public static void main(String[] args) throws AlignmentException, IOException, URISyntaxException, OWLOntologyCreationException {

		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUBSUMPTION.rdf";

//		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
//		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-SUBSUMPTION.rdf";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double testProfileScore = 0.58;
		int testSlope = 12;
		double testRangeMin = 0.5;
		double testRangeMax = 0.7;

		AlignmentProcess a = new ContextSubsumptionMatcher(sourceOntology, targetOntology, testProfileScore, testSlope, testRangeMin, testRangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment contextSubsumptionMatcherAlignment = new BasicAlignment();

		contextSubsumptionMatcherAlignment = (BasicAlignment) (a.clone());
		
		System.out.println("The 0.0 alignment contains " + contextSubsumptionMatcherAlignment.nbCells() + " relations");		

		System.out.println("\nThe alignment contains " + contextSubsumptionMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(contextSubsumptionMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.2:");
		contextSubsumptionMatcherAlignment.cut(0.2);
		Evaluator.evaluateSingleAlignment(contextSubsumptionMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		contextSubsumptionMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(contextSubsumptionMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		contextSubsumptionMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(contextSubsumptionMatcherAlignment, referenceAlignment);
		
		System.out.println("Printing relations at 0.6:");
		for (Cell c : contextSubsumptionMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.9:");
		contextSubsumptionMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(contextSubsumptionMatcherAlignment, referenceAlignment);



	}
	
	public static URIAlignment returnCSMAlignment (File ontoFile1, File ontoFile2, double weight) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment CSMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new ContextSubsumptionMatcher(onto1, onto2, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment contextSubsumptionMatcherAlignment = new BasicAlignment();

		contextSubsumptionMatcherAlignment = (BasicAlignment) (a.clone());

		CSMAlignment = contextSubsumptionMatcherAlignment.toURIAlignment();

		CSMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return CSMAlignment;

	}
	
	public void align(Alignment alignment, Properties param) throws AlignmentException {

		//get concepts and associated subclasses for onto1
		try {
			classesAndSubclassesMapOnto1 = OntologyOperations.getClassesAndAllSubClasses(onto1);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}

		//get concepts and associated subclasses for onto1
		try {
			classesAndSubclassesMapOnto2 = OntologyOperations.getClassesAndAllSubClasses(onto2);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}

		try {
			classesAndSuperclassesMapOnto1 = OntologyOperations.getClassesAndAllSuperClasses(onto1);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}
		try {
			classesAndSuperclassesMapOnto2 = OntologyOperations.getClassesAndAllSuperClasses(onto2);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}

		System.out.println("Finished creating the initial data structures");

		Set<String> sourceSubclasses = new HashSet<String>();
		Set<String> targetSubclasses = new HashSet<String>();
		Set<String> sourceSuperclasses = new HashSet<String>();
		Set<String> targetSuperclasses = new HashSet<String>();
		
		int idCounter = 0;

		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){
					
					idCounter++;

					String source = ontology1().getEntityName(sourceObject);
					String target = ontology2().getEntityName(targetObject);

					sourceSubclasses = classesAndSubclassesMapOnto1.get(source);
					targetSubclasses = classesAndSubclassesMapOnto2.get(target);
					
					sourceSuperclasses = classesAndSuperclassesMapOnto1.get(source);
					targetSuperclasses = classesAndSuperclassesMapOnto2.get(target);


					//if the source concept equals a child of the target concept: source < target 1.0
					if (targetSubclasses.contains(source)) {

						addAlignCell("ContextSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&lt;", 1.0 * profileScore);
								
					}

					//if the target concept equals a child of the source concept: source > target 1.0
					else if (sourceSubclasses.contains(target)) {

						addAlignCell("ContextSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&gt;", 1.0 * profileScore);
						
					}

					//if the source concept equals a parent of the target concept: source > target 1.0
					else if (targetSuperclasses.contains(source)) {

						addAlignCell("ContextSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&gt;", 1.0 * profileScore);
					}

					//if the target concept equals a parent of the source concept: source < target 1.0
					else if (sourceSuperclasses.contains(target)) {

						addAlignCell("ContextSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&lt;", 1.0 * profileScore);
						
					}


					else {
						addAlignCell("ContextSubsumptionMatcher" + idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
					}

				}

			}

		} catch (Exception e) { e.printStackTrace(); }
	}


}
