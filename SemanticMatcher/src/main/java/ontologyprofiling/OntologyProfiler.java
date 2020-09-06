package ontologyprofiling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.ontowrap.OntowrapException;

import utilities.MathUtils;
import utilities.OntologyOperations;
import utilities.StringUtilities;
import utilities.WordNet;

import rita.wordnet.jwnl.JWNLException;

/**
 * This class includes a set of metrics that evaluate the terminological, structural and lexical profile of the input ontologies. 
 * The evaluation measures are computed as an average metric for both ontologies. 
 * Based on these metrics, a set of optimal matchers are selected and configured given the ontologies to be matched.
 * @author audunvennesland
 *
 */
public class OntologyProfiler {

	final static double affinityThreshold = 0.2;
	final static double tokenEqualityThreshold = 0.2;
	//	final static BabelNet bn = BabelNet.getInstance();

	public OntologyProfiler() {
	}

	public static void main(String[] args) throws OWLOntologyCreationException, URISyntaxException, OntowrapException,
	FileNotFoundException, JWNLException, IOException {

		String onto1Path = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl";
		String onto2Path = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl";
		String corpus = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_embeddings.txt";

//		String onto1Path = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf";
//		String onto2Path = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl";
//		String corpus = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";
		
//		String onto1Path = "./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-301.rdf";
//		String onto2Path = "./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-304.rdf";
//		String corpus = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";

		System.out.println("Starting the Ontology Profiler");

		File onto1 = new File(onto1Path);
		File onto2 = new File(onto2Path);

		File storedOntologyProfileFile = new File("./files/_PHD_EVALUATION/ONTOLOGYPROFILES/" 
				+ StringUtilities.stripOntologyName(onto1.getName()) + "-" 
				+ StringUtilities.stripOntologyName(onto2.getName()) + ".txt");

		PrintWriter pw = new PrintWriter(storedOntologyProfileFile);

		System.out.println("\nOntology Profiling Results:\n");
		pw.println("\nOntology Profiling Results:\n");

		System.out.println("*** Compound Fraction ***");
		System.out.println("The Compound Fraction (CF) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeCompoundFraction(onto1, onto2)), 2));

		pw.println("*** Compound Fraction ***");
		pw.println("The Compound Fraction (CF) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeCompoundFraction(onto1, onto2)), 2));

		System.out.println("\n*** Corpus Coverage (CC) ***");
		System.out.println("The Corpus Coverage (CC) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeCorpusCoverage(onto1, onto2, corpus)), 2));

		pw.println("\n*** Corpus Coverage (CC) ***");
		pw.println("The Corpus Coverage (CC) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeCorpusCoverage(onto1, onto2, corpus)), 2));


		System.out.println("\n*** Definition Coverage ***");
		System.out.println("The Definition Coverage (DC) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeDefinitionCoverage(onto1, onto2)), 2));

		pw.println("\n*** Definition Coverage ***");
		pw.println("The Definition Coverage (DC) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeDefinitionCoverage(onto1, onto2)), 2));

		System.out.println("\n*** Property Fraction ***");
		System.out.println("The Property Fraction (PF) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computePropertyFraction(onto1, onto2)), 2));

		pw.println("\n*** Property Fraction ***");
		pw.println("The Property Fraction (DF) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computePropertyFraction(onto1, onto2)), 2));

		System.out.println("\n*** Structural Profile ***");
		System.out.println("The Structural Profile (SP) of " + onto1.getName() + " and " + onto2.getName() + " is: " + 
				MathUtils.round((computeStructuralProfile(onto1, onto2)), 2));

		pw.println("\n*** Structural Profile ***");
		pw.println("The Structural Profile (SP) of " + onto1.getName() + " and " + onto2.getName() + " is: " + 
				MathUtils.round((computeStructuralProfile(onto1, onto2)), 2));


		System.out.println("\n*** Lexical Coverage (WordNet) ***");
		System.out.println("The Lexical Coverage (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeLexicalCoverage(onto1, onto2)), 2));

		pw.println("\n*** Lexical Coverage (WordNet) ***");
		pw.println("The Lexical Coverage (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ MathUtils.round((computeLexicalCoverage(onto1, onto2)), 2));

//
//		System.out.println("\n*** Synonym Richness (WordNet) ***");
//		System.out.println("The Synonym Richness (SR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
//				+ MathUtils.round((computeSynonymRichnessWordNet(onto1, onto2)), 2));
//
//		pw.println("\n*** Synonym Richness (WordNet) ***");
//		pw.println("The Synonym Richness (SR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
//				+ MathUtils.round((computeSynonymRichnessWordNet(onto1, onto2)), 2));
//
//		System.out.println("\n*** Hyponym Richness (WordNet) ***");
//		System.out.println("The Hyponym Richness (HR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
//				+ MathUtils.round((computeHyponymRichnessWordNet(onto1, onto2)), 2));
//
//		pw.println("\n*** Hyponym Richness (WordNet) ***");
//		pw.println("The Hyponym Richness (HR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
//				+ MathUtils.round((computeHyponymRichnessWordNet(onto1, onto2)), 2));


		pw.close();

		System.out.println("\nOntology Profiling results are stored at " + storedOntologyProfileFile.getPath());
	}

	/**
	 * Creates a map that for two input ontologies represents the ontology profiling scores.
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param corpus a file with word embeddings used to compute the Corpus Coverage (CC) measure.
	 * @return a map where the short name of the ontology profiling metric is key and the score from the ontology profiling is value.
	 * @throws OWLOntologyCreationException
	 * @throws JWNLException
	 * @throws IOException
	   Jul 18, 2019
	 * @throws net.sf.extjwnl.JWNLException 
	 */
	public static Map<String, Double> computeOntologyProfileScores(File ontoFile1, File ontoFile2, String corpus) throws OWLOntologyCreationException, JWNLException, IOException {

		Map<String, Double> ontologyProfileScores = new HashMap<String, Double>();
		
		ontologyProfileScores.put("cf", computeCompoundFraction(ontoFile1, ontoFile2));
		ontologyProfileScores.put("cc", computeCorpusCoverage(ontoFile1, ontoFile2, corpus));
		ontologyProfileScores.put("dc", computeDefinitionCoverage(ontoFile1, ontoFile2));
		ontologyProfileScores.put("pf", computePropertyFraction(ontoFile1, ontoFile2));
		ontologyProfileScores.put("sp", computeStructuralProfile(ontoFile1, ontoFile2));
		ontologyProfileScores.put("lc", computeLexicalCoverage(ontoFile1, ontoFile2));
		//ontologyProfileScores.put("sr", computeSynonymRichnessWordNet(ontoFile1, ontoFile2));
		//ontologyProfileScores.put("hr", computeHyponymRichnessWordNet(ontoFile1, ontoFile2));

		return ontologyProfileScores;

	}

	/**
	 * The Corpus Coverage (CC) metric analyses how many individual tokens from the two input ontologies reside in a corpus representing word embeddings. 
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param corpusPath the path to the relevant file holding word embeddings.
	 * @return the Corpus Coverage score.
	 * @throws IOException
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static double computeCorpusCoverage(File ontoFile1, File ontoFile2, String corpusPath) throws IOException, OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);	

		File corpusFile = new File(corpusPath);

		Set<String> tokens = new HashSet<String>();
		tokens.addAll(OntologyOperations.getAllOntologyTokens(sourceOntology));
		tokens.addAll(OntologyOperations.getAllOntologyTokens(targetOntology));

		BufferedReader br=new BufferedReader(new FileReader(corpusFile));

		String line;
		String wordToCheck = null;
		int tokenMatchCounter = 0;

		while((line=br.readLine())!=null) {

			//get the first word of the line			
			int i = line.indexOf(' ');
			wordToCheck = line.substring(0, i);
			if (tokens.contains(wordToCheck)) {
				tokenMatchCounter++;
			}

		}

		br.close();

		return (double) tokenMatchCounter / (double) tokens.size();

	}

	/**
	 * The Structural Profile onsiders the coverage of subclasses and superclasses in the two ontologies, and is computed as the fraction 
	 * of classes that have sub- or superclasses associated with them over all classes in both ontologies.
	 * @param ontoFile1
	 * @param ontoFile2
	 * @return
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static double computeStructuralProfile(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double structureProfile = 0;

		int numClasses = onto1.getClassesInSignature().size() + onto2.getClassesInSignature().size();

		int subclasses = 0;
		int superclasses = 0;
		int counterOnto1 = 0;
		int counterOnto2 = 0;

		for (OWLClass c : onto1.getClassesInSignature()) {
			subclasses = OntologyOperations.getEntitySubclasses(onto1, c).size();
			superclasses = OntologyOperations.getEntitySuperclasses(onto1, c).size();

			if (subclasses > 0 || superclasses > 0) {
				counterOnto1++;
			}

		}

		for (OWLClass c : onto2.getClassesInSignature()) {
			subclasses = OntologyOperations.getEntitySubclasses(onto2, c).size();
			superclasses = OntologyOperations.getEntitySuperclasses(onto2, c).size();

			if (subclasses > 0 || superclasses > 0) {
				counterOnto2++;
			}

		}

		structureProfile = ((double) counterOnto1 + (double) counterOnto2) / (double) numClasses;

		return structureProfile;

	}

	/**
	 * Returns a count of how many classes are considered compound words in an
	 * ontology
	 * @param ontoFile the file path of the input OWL ontology
	 * @return numCompounds a double stating the percentage of how many of the classes in the ontology are compounds
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static double computeCompoundFraction(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		int numClassesTotalOnto1 = onto1.getClassesInSignature().size();
		int numClassesTotalOnto2 = onto2.getClassesInSignature().size();
		int counterOnto1 = 0;
		int counterOnto2 = 0;

		for (OWLClass cl : onto1.getClassesInSignature()) {
			if (OntologyOperations.isCompound(StringUtilities.replaceUnderscore(cl.getIRI().getFragment()))) {
				counterOnto1++;
			}
		}

		for (OWLClass cl : onto2.getClassesInSignature()) {
			if (OntologyOperations.isCompound(StringUtilities.replaceUnderscore(cl.getIRI().getFragment()))) {
				counterOnto2++;
			}
		}

		double compoundRatioOnto1 = (double) counterOnto1 / (double) numClassesTotalOnto1;
		double compoundRatioOnto2 = (double) counterOnto2 / (double) numClassesTotalOnto2;

		return (compoundRatioOnto1 + compoundRatioOnto2) / 2;
	}

	/**
	 * The Definition Coverage (DC) metric aims to capture how well annotated the concepts in the input ontologies are.
	 * It is calculated by measuring the fraction of concepts that are annotated in each of the two ontologies, and the 
	 * minimum of the two fractions is used to define the Definition Coverage. 
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @return a score representing the Definition Coverage (DC)
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static double computeDefinitionCoverage(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		int numClassesTotalOnto1 = onto1.getClassesInSignature().size();
		int numClassesTotalOnto2 = onto2.getClassesInSignature().size();
		int counterOnto1 = 0;
		int counterOnto2 = 0;


		for (OWLClass cl : onto1.getClassesInSignature()) {

			String def = OntologyOperations.getClassDefinition(onto1, cl);

			if (def != null) {
				
				String def_without_stopwords = StringUtilities.removeStopWords(def);
				
				String[] tokens = def_without_stopwords.split(" ");
				

				if (tokens.length > 10) {
					counterOnto1++;
				}

			}

		}


		for (OWLClass cl : onto2.getClassesInSignature()) {

			String def = OntologyOperations.getClassDefinition(onto2, cl);

			if (def != null) {
				
				String def_without_stopwords = StringUtilities.removeStopWords(def);

				String[] tokens = def_without_stopwords.split(" ");

				if (tokens.length > 10) {
					counterOnto2++;
				}

			}
		}

		double definitionCoverageOnto1 = (double) counterOnto1 / (double) numClassesTotalOnto1;
		double definitionCoverageOnto2 = (double) counterOnto2 / (double) numClassesTotalOnto2;


		return Math.min(definitionCoverageOnto1, definitionCoverageOnto2);

	}

	
	/**
	 * Returns the average number of hyponyms in WordNet for each class in an ontology
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @return a score representing the average number of hyponyms per class for two input ontologies.
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static double computeHyponymRichnessWordNet(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {

		double hyponymCounterOnto1 = 0;
		double hyponymCounterOnto2 = 0;

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		int numClassesTotalOnto1 = onto1.getClassesInSignature().size();
		int numClassesTotalOnto2 = onto2.getClassesInSignature().size();

		for (OWLClass cl : onto1.getClassesInSignature()) {
			String[] hyponyms = WordNet
					.getHyponyms(StringUtilities.stringTokenize(cl.getIRI().getFragment(), true));

			if (hyponyms.length > 0) {
				hyponymCounterOnto1++;
			}

		}

		for (OWLClass cl : onto2.getClassesInSignature()) {
			String[] hyponyms = WordNet
					.getHyponyms(StringUtilities.stringTokenize(cl.getIRI().getFragment(), true));

			if (hyponyms.length > 0) {
				hyponymCounterOnto2++;
			}

		}

		double hyponymRichnessOnto1 = (double) hyponymCounterOnto1 / (double) numClassesTotalOnto1;
		double hyponymRichnessOnto2 = (double) hyponymCounterOnto2 / (double) numClassesTotalOnto2;

		return (hyponymRichnessOnto1 + hyponymRichnessOnto2) / 2;
	}

	
	/**
	 * Returns the average number of synonyms in WordNet for each class in two input ontologies
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @return a score representing the average number of synonyms per class in two input ontologies.
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static double computeSynonymRichnessWordNet(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {

		double synonymCounterOnto1 = 0;
		double synonymCounterOnto2 = 0;

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		int numClassesTotalOnto1 = onto1.getClassesInSignature().size();
		int numClassesTotalOnto2 = onto2.getClassesInSignature().size();

		for (OWLClass cl : onto1.getClassesInSignature()) {
			Set<String> synonyms = WordNet
					.getSynonymSet(StringUtilities.stringTokenize(cl.getIRI().getFragment(), true));

			if (synonyms.size() > 0) {
				synonymCounterOnto1++;
			}

		}

		for (OWLClass cl : onto2.getClassesInSignature()) {
			Set<String> synonyms = WordNet
					.getSynonymSet(StringUtilities.stringTokenize(cl.getIRI().getFragment(), true));

			if (synonyms.size() > 0) {
				synonymCounterOnto2++;
			}

		}

		double synonymRichnessOnto1 = (double) synonymCounterOnto1 / (double) numClassesTotalOnto1;
		double synonymRichnessOnto2 = (double) synonymCounterOnto2 / (double) numClassesTotalOnto2;

		return (synonymRichnessOnto1 + synonymRichnessOnto2) / 2;
	}


	/**
	 * The Property Fraction (PF) measures the extent to which the input ontologies include properties (data- and object properties). 
	 * PF is computed as the fraction of classes that are associated with data- or object properties over the total number of classes in the two ontologies. 
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @return a score representing the Property Fraction (PF)
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static double computePropertyFraction(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		int numClassesTotalOnto1 = onto1.getClassesInSignature().size();
		int numClassesTotalOnto2 = onto2.getClassesInSignature().size();
		int counterOnto1 = 0;
		int counterOnto2 = 0;

		for (OWLClass cl : onto1.getClassesInSignature()) {
			if (OntologyOperations.getProperties(onto1, cl.getIRI().getFragment().toLowerCase()).size() > 0) {
				counterOnto1++;
			}
		}

		for (OWLClass cl : onto2.getClassesInSignature()) {
			if (OntologyOperations.getProperties(onto2, cl.getIRI().getFragment().toLowerCase()).size() > 0) {
				counterOnto2++;
			}
		}

		double propertyRatioOnto1 = (double) counterOnto1 / (double) numClassesTotalOnto1;
		double propertyRatioOnto2 = (double) counterOnto2 / (double) numClassesTotalOnto2;

		return (propertyRatioOnto1 + propertyRatioOnto2) / 2;

	}
	
	/**
	 * The Lexical Coverage (WC) measures the percentage of terms with label or local name present in WordNet.
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @return a score representing the Lexical Coverage (LC)
	 * @throws OWLOntologyCreationException
	 * @throws FileNotFoundException
	 * @throws JWNLException
	   Jul 18, 2019
	 * @throws net.sf.extjwnl.JWNLException 
	 */
	public static double computeLexicalCoverage(File ontoFile1, File ontoFile2)
			throws OWLOntologyCreationException, FileNotFoundException, JWNLException {

		double lexicalCoverage = (OntologyOperations.getWordNetCoverageComp(ontoFile1)
				+ OntologyOperations.getWordNetCoverageComp(ontoFile2)) / 2;

		return lexicalCoverage;
	}



}
