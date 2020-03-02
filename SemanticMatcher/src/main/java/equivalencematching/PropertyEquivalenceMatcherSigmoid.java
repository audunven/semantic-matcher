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

//import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import utilities.ISub;
import utilities.Sigmoid;
import utilities.SimilarityMetrics;
import utilities.StringUtilities;
import utilities.WordNet;

/**
 * The Property Matcher measures the similarity of the properties associated with the concepts to be matched. 
 * Both object properties and data properties where the concepts to be matched represent the domain or range class are 
 collected into single sets C_x_prop and C_y_prop and compared with Jaccard. 
 * This class computes confidence scores for each relation using the scores from the ontology profiling.
 * @author audunvennesland
 *
 */
public class PropertyEquivalenceMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {


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

	public PropertyEquivalenceMatcherSigmoid(OWLOntology ontoFile1, OWLOntology ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) {
		onto1 = ontoFile1;
		onto2 = ontoFile2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	/**
	 * 	/**
	 * Returns an alignment holding relations computed by the Property Equivalence Matcher (PEM).
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param profileScore the score obtained for this matcher in the ontology profiling
	 * @param slope the sigmoid slope parameter
	 * @param rangeMax the max value of the confidence transformation
	 * @param rangeMin the min value of the confidence transformation
	 * @return an URIAlignment holding a set of relations (cells)
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Jul 15, 2019
	 */
	public static URIAlignment returnPEMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment PEMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new PropertyEquivalenceMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment PropertyEquivalenceMatcherSigmoidAlignment = new BasicAlignment();

		PropertyEquivalenceMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		PropertyEquivalenceMatcherSigmoidAlignment.normalise();

		PEMAlignment = PropertyEquivalenceMatcherSigmoidAlignment.toURIAlignment();

		PEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return PEMAlignment;

	}

	/**
	 * Creates an alignment that on the basis of class and property similarity obtains a similarity score assigned to each relation in the alignment.
	 * A combination of Jaccard set similarity and the ISUB string similarity measure is used to compute the similarity score.
	 */
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

					//JACCARD SIMILARITY WITH ISUB AND EQUAL CONCEPTS
					sim = SimilarityMetrics.jaccardSetSimISubEqualConcepts(confidence, source, target, props1, props2);
					
					if (sim > 0 && sim <= 1) {
						//using sigmoid function to compute confidence
						addAlignCell("PropertyMatcher" + idCounter, sourceObject,targetObject, "=", 
								Sigmoid.weightedSigmoid(slope, sim, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));  
					} else {
						
						addAlignCell("PropertyMatcher" + idCounter, sourceObject, targetObject, "=", 0);
						
					}
				}
			}

		} catch (Exception e) { e.printStackTrace(); }
	}

	/**
	 * Creates a map that holds each class as key along with a set of properties (including their synonyms) as value.
	 * @param onto an input ontology
	 * @return a Map<String, Set<String>> holding classes and corresponding properties.
	 * @throws ClassNotFoundException
	 * @throws IOException
	   Jul 15, 2019
	 */
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


	/**
	 * In order to match properties the Property Matcher tries to identify the core concept of each property, inspired by the works of Michelle Cheatham et al. 
	 * The core concept is either the first verb in the label that is greater than 4 characters or, if no such verb exists, the first noun in the label, 
	 * together with any adjectives that qualify that noun. 
	 * A Part-of-Speech (POS) tagger is used for differentiating verbs, nouns and adjectives in a property name. 
	 * Currently, the POS tagger from the Stanford CoreNLP API is used.
	 * @param propName the property name from which the core concept is retrieved.
	 * @return the core concept of the property
	 * @throws IOException
	 * @throws ClassNotFoundException
	   Jul 15, 2019
	 */
	public static String getPropertyCoreConcept(String text) throws IOException, ClassNotFoundException {


		if (StringUtilities.isCompoundWord(text)) {
			text = StringUtilities.getCompoundWordWithSpaces(text);
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
	
	public static double jaccardSetSimISubEqualConcepts (double confidence, String concept1, String concept2, Set<String> set1, Set<String> set2) {

		ISub isubMatcher = new ISub();

		int intersection = 0;
		int refinedIntersection = 0;
		int refinedUnion = 0;
		double isubScore = 0;

		for (String s1 : set1) {
			for (String s2 : set2) {
				//using ISub to compute a similarity score
				isubScore = isubMatcher.score(s1,s2);
				if (isubScore > confidence) {
					intersection += 1;
				}
			}
		}
		
		int union = (set1.size() + set2.size()) - intersection;
		
		if (set1.contains(concept2.toLowerCase()) && set2.contains(concept1.toLowerCase())) {
			refinedIntersection = intersection +2;
			refinedUnion = union - 2;
		} else if (set1.contains(concept2.toLowerCase()) || set2.contains(concept1.toLowerCase())) {
			refinedIntersection = intersection +1;
			refinedUnion = union - 1;
		} else {
			refinedIntersection = intersection;
			refinedUnion = union;
		}

		double jaccardSetSim = (double) refinedIntersection / (double) refinedUnion;
		
		return jaccardSetSim;
	}

}



