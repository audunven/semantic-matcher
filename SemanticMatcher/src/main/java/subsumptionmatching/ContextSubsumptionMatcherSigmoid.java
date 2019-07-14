package subsumptionmatching;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import alignmentcombination.HarmonySubsumption;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import utilities.OntologyOperations;
import utilities.Sigmoid;
import utilities.StringUtilities;

public class ContextSubsumptionMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	OWLOntology sourceOntology;
	OWLOntology targetOntology;
	
	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;
	
	static Map<String, Set<String>> classesAndSubclassesMapOnto1 = new HashMap<String, Set<String>>();
	static Map<String, Set<String>> classesAndSubclassesMapOnto2 = new HashMap<String, Set<String>>();

	static Map<String, Set<String>> classesAndSuperclassesMapOnto1 = new HashMap<String, Set<String>>();
	static Map<String, Set<String>> classesAndSuperclassesMapOnto2 = new HashMap<String, Set<String>>();


	
	public ContextSubsumptionMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, double profileScore, int slope, double rangeMin, double rangeMax) {
		this.sourceOntology = onto1;
		this.targetOntology = onto2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	
	
	public static URIAlignment returnCSMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment CSMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new ContextSubsumptionMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment ContextSubsumptionMatcherSigmoidAlignment = new BasicAlignment();

		ContextSubsumptionMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		CSMAlignment = ContextSubsumptionMatcherSigmoidAlignment.toURIAlignment();

		CSMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return CSMAlignment;

	}
	
	public void align(Alignment alignment, Properties param) throws AlignmentException {

		//get concepts and associated subclasses for onto1
		try {
			classesAndSubclassesMapOnto1 = OntologyOperations.getClassesAndAllSubClasses(sourceOntology);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}

		//get concepts and associated subclasses for onto1
		try {
			classesAndSubclassesMapOnto2 = OntologyOperations.getClassesAndAllSubClasses(targetOntology);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}

		try {
			classesAndSuperclassesMapOnto1 = OntologyOperations.getClassesAndAllSuperClasses(sourceOntology);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}
		try {
			classesAndSuperclassesMapOnto2 = OntologyOperations.getClassesAndAllSuperClasses(targetOntology);
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
						//using sigmoid function to compute confidence
						addAlignCell("ContextSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&lt;", 
								Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
					}

					//if the target concept equals a child of the source concept: source > target 1.0
					else if (sourceSubclasses.contains(target)) {
						//using sigmoid function to compute confidence
						addAlignCell("ContextSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&gt;", 
								Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
					}

					//if the source concept equals a parent of the target concept: source > target 1.0
					else if (targetSuperclasses.contains(source)) {
						//using sigmoid function to compute confidence
						addAlignCell("ContextSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&gt;", 
								Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
					}

					//if the target concept equals a parent of the source concept: source < target 1.0
					else if (sourceSuperclasses.contains(target)) {
						//using sigmoid function to compute confidence
						addAlignCell("ContextSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&lt;", 
								Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
					}


					else {
						addAlignCell("ContextSubsumptionMatcherSigmoid" + idCounter, sourceObject, targetObject, "!", 0.0);
					}

				}

			}

		} catch (Exception e) { e.printStackTrace(); }
	}



	public static boolean synonymMatch(Set<String> synonyms, Set<String> concepts) {

		//lowercase all concepts in the concepts set
		Set<String> lowerCasedConcepts = new HashSet<String>();
		for (String s : concepts) {
			lowerCasedConcepts.add(s.toLowerCase());
		}

		boolean match = false;

		for (String syn : synonyms) {
			if (lowerCasedConcepts.contains(syn.toLowerCase())) {
				match = true;
			}
		}

		return match;

	}

	public static boolean compoundRatioMatch(String source, Set<String> target) {

		int max = 0;
		boolean match = false;

		String[] sourceArray = source.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
		String[] targetArray = null;

		for (String t : target) {

			targetArray = t.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

			max = Math.max(sourceArray.length, targetArray.length);
			int equal = 0;

			for (int j = 0; j < targetArray.length; j++) {

				double ratio = 0;

				for (int i = 0; i < sourceArray.length; i++) {

					if (sourceArray[i].equalsIgnoreCase(targetArray[j])) {

						equal++;
					}

				}
				ratio = (double) equal / (double) max;
				if (ratio >= 0.6) {
					match = true;
				}

			}

		}

		return match;

	}

	public static boolean isCompoundWordInSet (Set<String> set) {
		boolean compound = false;

		for (String s : set) {
			if (StringUtilities.isCompoundWord(s)) {
				compound = true;
			}
		}

		return compound;

	}

}
