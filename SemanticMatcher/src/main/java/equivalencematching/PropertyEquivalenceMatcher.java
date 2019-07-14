package equivalencematching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import alignmentcombination.HarmonyEquivalence;
//import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import utilities.SimilarityMetrics;
import utilities.StringUtilities;
import utilities.WordNet;


public class PropertyEquivalenceMatcher extends ObjectAlignment implements AlignmentProcess {


	static OWLOntology onto1;
	static OWLOntology onto2;

	//the Stanford POS tagger used for computing the core concept of properties
	static MaxentTagger maxentTagger = new MaxentTagger("./files/taggers/english-left3words-distsim.tagger");


	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;


	//The ISUB confidence used in the combined Jaccard/ISub similarity measure
	final double confidence = 0.7;

	static Map<String, Set<String>> classAndPropMapOnto1 = new HashMap<String, Set<String>>();
	static Map<String, Set<String>> classAndPropMapOnto2 = new HashMap<String, Set<String>>();


	public PropertyEquivalenceMatcher(OWLOntology ontoFile1, OWLOntology ontoFile2, double profileScore) {
		onto1 = ontoFile1;
		onto2 = ontoFile2;
		this.profileScore = profileScore;
	}

	public PropertyEquivalenceMatcher(OWLOntology ontoFile1, OWLOntology ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) {
		onto1 = ontoFile1;
		onto2 = ontoFile2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {

		File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-301.rdf");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-302.rdf");
		String referenceAlignment = "./files/_PHD_EVALUATION/OAEI2011/REFALIGN/301302/301-302-EQUIVALENCE.rdf";

		//				File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		//				File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		//				String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double testProfileScore = 0.84;
		int testSlope = 12;
		double testRangeMin = 0.5;
		double testRangeMax = 0.7;

		AlignmentProcess a = new PropertyEquivalenceMatcher(sourceOntology, targetOntology, testProfileScore, testSlope, testRangeMin, testRangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment propertyMatcherAlignment = new BasicAlignment();

		propertyMatcherAlignment = (BasicAlignment) (a.clone());

		propertyMatcherAlignment.normalise();

		//evaluate the Harmony alignment
		BasicAlignment harmonyAlignment = HarmonyEquivalence.getHarmonyAlignment(propertyMatcherAlignment);
		System.out.println("The Harmony alignment contains " + harmonyAlignment.nbCells() + " cells");
		Evaluator.evaluateSingleAlignment(harmonyAlignment, referenceAlignment);

		System.out.println("Printing Harmony Alignment: ");
		for (Cell c : harmonyAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("\nThe alignment contains " + propertyMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(propertyMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.2:");
		propertyMatcherAlignment.cut(0.2);
		Evaluator.evaluateSingleAlignment(propertyMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		propertyMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(propertyMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		propertyMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(propertyMatcherAlignment, referenceAlignment);

		System.out.println("Printing relations at 0.9:");
		for (Cell c : propertyMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.9:");
		propertyMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(propertyMatcherAlignment, referenceAlignment);

	}

	public static URIAlignment returnPEMAlignment (File ontoFile1, File ontoFile2, double weight) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment PEMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new PropertyEquivalenceMatcher(onto1, onto2, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment propertyEquivalenceMatcherAlignment = new BasicAlignment();

		propertyEquivalenceMatcherAlignment = (BasicAlignment) (a.clone());

		propertyEquivalenceMatcherAlignment.normalise();

		PEMAlignment = propertyEquivalenceMatcherAlignment.toURIAlignment();

		PEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return PEMAlignment;

	}


	public void align(Alignment alignment, Properties param) throws AlignmentException {

		//construct a map holding a class as key and all props and synonyms of them as value
		try {
			classAndPropMapOnto1 = createClassAndPropMap(onto1);
		} catch (ClassNotFoundException | IOException e1) {
			e1.printStackTrace();
		}

		try {
			classAndPropMapOnto2 = createClassAndPropMap(onto2);
		} catch (ClassNotFoundException | IOException e1) {
			e1.printStackTrace();
		}

		double sim = 0;

		int idCounter = 0;

		try {
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){

					idCounter++;

					String source = ontology1().getEntityName(sourceObject).toLowerCase();
					String target = ontology2().getEntityName(targetObject).toLowerCase();

					Set<String> props1 = classAndPropMapOnto1.get(source);
					Set<String> props2 = classAndPropMapOnto2.get(target);
					
					System.out.println("\nMatching " + source + " and " + target);

					//JACCARD SIMILARITY WITH ISUB AND EQUAL CONCEPTS
					sim = SimilarityMetrics.jaccardSetSimISubEqualConcepts(confidence, source, target, props1, props2);
					
					if (sim > 0 && sim <= 1) {
						
						addAlignCell("PropertyMatcher" + idCounter + "_" + profileScore + "_", sourceObject,targetObject, "=", sim * profileScore);  
						
						//using sigmoid function to compute confidence
//						addAlignCell("PropertyMatcher" + idCounter, sourceObject,targetObject, "=", 
//								Sigmoid.weightedSigmoid(slope, sim, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));  
					} else {
						
						addAlignCell("PropertyMatcher" + idCounter + "_" + profileScore + "_", sourceObject, targetObject, "=", 0);
						
					}
				}
			}

		} catch (Exception e) { e.printStackTrace(); }
	}

	private static Map<String, Set<String>> createClassAndPropMap(OWLOntology onto) throws ClassNotFoundException, IOException {
		Map<String, Set<String>> classAndPropMap = new HashMap<String, Set<String>>();

		for (OWLClass cls : onto.getClassesInSignature()) {

			Set<String> ops = new HashSet<String>();
			Set<String> dps = new HashSet<String>();
			Set<String> propsSynonyms = new HashSet<String>();
			Set<String> propsCore = new HashSet<String>();
			Set<String> props = new HashSet<String>();

			for (OWLObjectPropertyDomainAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
				if (op.getDomain().equals(cls)) {
					for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
						ops.add(oop.getIRI().getFragment());
					}
				}
			}

			for (OWLObjectPropertyRangeAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE)) {
				if (op.getRange().equals(cls)) {
					for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
						ops.add(oop.getIRI().getFragment());
					}
				}
			}

			for (OWLDataPropertyDomainAxiom dp : onto.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
				if (dp.getDomain().equals(cls)) {
					for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
						dps.add(odp.getIRI().getFragment());
					}
				}
			}

			//merge all object and data properties into one set
			props.addAll(ops);
			props.addAll(dps);

			//get the core concept of each property				
			for (String prop : props) {
				propsCore.add(getPropertyCoreConcept(prop.substring(prop.lastIndexOf("-") +1)));
			}


			//once all properties (i.e. their core concepts) have been collected, we retrieve their synonyms (nouns, verbs, and adjectives) from WordNet
			//the query parameter to WordNet is the lemma of the property label.
			propsSynonyms = new HashSet<String>();

			for (String p : props) {
				propsSynonyms = WordNet.getAllSynonymSet(p.toLowerCase().replaceAll("\\s+", "")); //use the lemma + need to remove whitespace before querying WordNet
			}

			props.addAll(propsSynonyms);				

			classAndPropMap.put(cls.getIRI().getFragment().toLowerCase(), props);
		}

		return classAndPropMap;
	}


	public static String getPropertyCoreConcept(String text) throws IOException, ClassNotFoundException {


		if (StringUtilities.isCompoundWord(text)) {
			text = StringUtilities.splitCompounds(text);
		}

		String tag = maxentTagger.tagString(text);

		String[] eachTag = tag.split("\\s+");

		Multimap<String, String> posMap = LinkedListMultimap.create();
		for(int i = 0; i< eachTag.length; i++) {
			posMap.put(eachTag[i].split("_")[0], eachTag[i].split("_")[1]);
		}

		StringBuffer sb = new StringBuffer();
		for (Entry<String, String> e : posMap.entries()) {
			if (e.getValue().equals("VB") || e.getValue().equals("VBD") || e.getValue().equals("VBG") || e.getValue().equals("VBP") || e.getValue().equals("VBZ") || e.getValue().equals("VBN")) {
				if (e.getKey().length() > 3) {
					sb.append(e.getKey() + " ");
					break;
				}
			} else if (e.getValue().equals("JJ") || e.getValue().equals("JJR") || e.getValue().equals("JJS")) {
				sb.append(e.getKey() + " ");
			}

			else if (e.getValue().equals("NN") || e.getValue().equals("NNS") || e.getValue().equals("NNP") || e.getValue().equals("NNPS") || e.getValue().equals(".")) {
				sb.append(e.getKey() + " ");
				break;
			}

		}

		return sb.toString();
	}

}



