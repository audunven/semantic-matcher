package ontologyprofiling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.ontowrap.OntowrapException;
import graph.Graph;
//import it.uniroma1.lcl.babelnet.BabelNet;
import net.didion.jwnl.JWNLException;
//import utilities.BabelNetOperations;
import utilities.OntologyOperations;
import utilities.StringUtilities;
import utilities.WordNet;

public class OntologyProfiler {

	final static double affinityThreshold = 0.2;
	final static double tokenEqualityThreshold = 0.2;
	//	final static BabelNet bn = BabelNet.getInstance();

	public OntologyProfiler() {
	}

	public static void main(String[] args) throws OWLOntologyCreationException, URISyntaxException, OntowrapException,
	FileNotFoundException, JWNLException, IOException {

//				String onto1Path = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl";
//				String onto2Path = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl";
//				String corpus = "./files/_PHD_EVALUATION/EMBEDDINGS/skybrary_embeddings.txt";

//		String onto1Path = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf";
//		String onto2Path = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl";
//		String corpus = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";
		
		String onto1Path = "./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-301.rdf";
		String onto2Path = "./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-304.rdf";
		String corpus = "./files/_PHD_EVALUATION/EMBEDDINGS/wikipedia_embeddings.txt";

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
				+ round((computeCompoundFraction(onto1, onto2)), 2));

		pw.println("*** Compound Fraction ***");
		pw.println("The Compound Fraction (CF) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeCompoundFraction(onto1, onto2)), 2));

		System.out.println("\n*** Corpus Coverage (CC) ***");
		System.out.println("The Corpus Coverage (CC) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeCorpusCoverage(onto1, onto2, corpus)), 2));

		pw.println("\n*** Corpus Coverage (CC) ***");
		pw.println("The Corpus Coverage (CC) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeCorpusCoverage(onto1, onto2, corpus)), 2));


		System.out.println("\n*** Definition Coverage ***");
		System.out.println("The Definition Coverage (DC) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeDefinitionCoverage(onto1, onto2)), 2));

		pw.println("\n*** Definition Coverage ***");
		pw.println("The Definition Coverage (DC) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeDefinitionCoverage(onto1, onto2)), 2));

		System.out.println("\n*** Property Fraction ***");
		System.out.println("The Property Fraction (PF) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computePropertyFraction(onto1, onto2)), 2));

		pw.println("\n*** Property Fraction ***");
		pw.println("The Property Fraction (DF) of  " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computePropertyFraction(onto1, onto2)), 2));

		System.out.println("\n*** Structural Profile ***");
		System.out.println("The Structural Profile (SP) of " + onto1.getName() + " and " + onto2.getName() + " is: " + 
				round((computeStructuralProfile(onto1, onto2)), 2));

		pw.println("\n*** Structural Profile ***");
		pw.println("The Structural Profile (SP) of " + onto1.getName() + " and " + onto2.getName() + " is: " + 
				round((computeStructuralProfile(onto1, onto2)), 2));


		System.out.println("\n*** Lexical Coverage (WordNet) ***");
		System.out.println("The Lexical Coverage (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeLexicalCoverage(onto1, onto2)), 2));

		pw.println("\n*** Lexical Coverage (WordNet) ***");
		pw.println("The Lexical Coverage (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeLexicalCoverage(onto1, onto2)), 2));


		System.out.println("\n*** Synonym Richness (WordNet) ***");
		System.out.println("The Synonym Richness (SR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeSynonymRichnessWordNet(onto1, onto2)), 2));

		pw.println("\n*** Synonym Richness (WordNet) ***");
		pw.println("The Synonym Richness (SR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeSynonymRichnessWordNet(onto1, onto2)), 2));

		System.out.println("\n*** Hyponym Richness (WordNet) ***");
		System.out.println("The Hyponym Richness (HR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeHyponymRichnessWordNet(onto1, onto2)), 2));

		pw.println("\n*** Hyponym Richness (WordNet) ***");
		pw.println("The Hyponym Richness (HR) (WordNet) of " + onto1.getName() + " and " + onto2.getName() + " is: "
				+ round((computeHyponymRichnessWordNet(onto1, onto2)), 2));


		pw.close();

		System.out.println("\nOntology Profiling results are stored at " + storedOntologyProfileFile.getPath());
	}

	public static Map<String, Double> computeOntologyProfileScores(File ontoFile1, File ontoFile2, String corpus) throws OWLOntologyCreationException, JWNLException, IOException {

		Map<String, Double> ontologyProfileScores = new HashMap<String, Double>();
		
		ontologyProfileScores.put("cf", computeCompoundFraction(ontoFile1, ontoFile2));
		ontologyProfileScores.put("cc", computeCorpusCoverage(ontoFile1, ontoFile2, corpus));
		ontologyProfileScores.put("dc", computeDefinitionCoverage(ontoFile1, ontoFile2));
		ontologyProfileScores.put("pf", computePropertyFraction(ontoFile1, ontoFile2));
		ontologyProfileScores.put("sp", computeStructuralProfile(ontoFile1, ontoFile2));
		ontologyProfileScores.put("lc", computeLexicalCoverage(ontoFile1, ontoFile2));
		ontologyProfileScores.put("sr", computeSynonymRichnessWordNet(ontoFile1, ontoFile2));
		ontologyProfileScores.put("hr", computeHyponymRichnessWordNet(ontoFile1, ontoFile2));

		return ontologyProfileScores;

	}

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
	 * 
	 * @param ontoFile
	 *            the file path of the input OWL ontology
	 * @return numCompounds a double stating the percentage of how many of the
	 *         classes in the ontology are compounds
	 * @throws OWLOntologyCreationException
	 *             An exception which describes an error during the creation of
	 *             an ontology. If an ontology cannot be created then subclasses
	 *             of this class will describe the reasons.
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
	 * Computes the concept name equality by finding the number of longest common substrings (lcs) in the two input ontologies. The lcs is considered as the longest sequence of
	 * identical characters in two concept names, but it should have a minimum length of 3 (English nouns are usually > 3 chars) and also a minimum length of 80 % of the average length of the 
	 * concept names to account for very common character sequences such as "ation" (organisation, publication, etc.).
	 * @param ontoFile1
	 * @param ontoFile2
	 * @return
	 * @throws OWLOntologyCreationException
	   Feb 6, 2019
	 */
	public static double computeConceptNameEquality(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		Set<String> lcs = new HashSet<String>();
		for (OWLClass s : onto1.getClassesInSignature()) {
			for (OWLClass t : onto2.getClassesInSignature()) {

				lcs.addAll(longestCommonSubstrings(s.getIRI().getFragment(), t.getIRI().getFragment()));
				if (longestCommonSubstrings(s.getIRI().getFragment(), t.getIRI().getFragment()).size() > 0) {
					break;
				}
			}
		}

		int minOntology = Math.min(onto1.getClassesInSignature().size(), onto2.getClassesInSignature().size());

		double cne = (double) lcs.size() / (double) minOntology;
		return cne;

	}

	public static double computeDefinitionTokenEquality(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException {
		int commonEntities = 0; 
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		int numClassesTotalOnto1 = onto1.getClassesInSignature().size();
		int numClassesTotalOnto2 = onto2.getClassesInSignature().size();
		int minEntities = Math.min(numClassesTotalOnto1, numClassesTotalOnto2);


		for (OWLClass s : onto1.getClassesInSignature()) {
			for (OWLClass t : onto2.getClassesInSignature()) {

				ArrayList<String> tokensS = new ArrayList<String>();
				ArrayList<String> tokensT = new ArrayList<String>();

				Set<String> sDefinitions = OntologyOperations.cleanClassDefinitions(onto1, s);

				Set<String> tDefinitions = OntologyOperations.cleanClassDefinitions(onto2, t);


				for (String sDef : sDefinitions) {
					tokensS = StringUtilities.tokenize(StringUtilities.removeStopWords(sDef), true);
				}

				for (String tDef : tDefinitions) {
					tokensT = StringUtilities.tokenize(StringUtilities.removeStopWords(tDef), true);
				}

				double tokenEquality = computeTokenEquality(tokensS, tokensT);

				if (tokenEquality > tokenEqualityThreshold) {
					commonEntities++;
					break; //to ensure that we don´t compare several target concepts with a single source concept
				}

			}		
		}	

		return (double) commonEntities / (double) minEntities;

	}

	public static double computeTokenEquality(ArrayList<String> sourceList, ArrayList<String> targetList) {
		int counter = 0;
		int numAvgTokensInList = ( sourceList.size() + targetList.size() ) / 2;
		for (String s : sourceList) {
			for (String t : targetList) {
				if (s.equalsIgnoreCase(t)) {
					counter++;
				}
			}
		}
		return (double) counter / (double) numAvgTokensInList;

	}

	/**
	 * Returns the average number of hyponyms in WordNet for each class in an
	 * ontology
	 * 
	 * @param ontoFile:
	 *            an ontology file
	 * @return the average number of hyponyms per class in an ontology
	 * @throws OWLOntologyCreationException
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
	 * Returns the average number of synonyms in WordNet for each class in an
	 * ontology
	 * 
	 * @param ontoFile:
	 *            an ontology file
	 * @return the average number of synonyms per class in an ontology
	 * @throws OWLOntologyCreationException
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


	public static double computePropertyFraction(File ontoFile) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		int numClassesTotal = onto.getClassesInSignature().size();
		int counter = 0;
		for (OWLClass cl : onto.getClassesInSignature()) {
			if (OntologyOperations.getProperties(onto, cl.getIRI().getFragment().toLowerCase()).size() > 0) {
				counter++;
			}
		}

		return (double) counter / (double) numClassesTotal;

	}

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
	 * WordNet Coverage (WC): The percentage of terms with label or local name
	 * present in WordNet.
	 * 
	 * @param onto1
	 * @param onto2
	 * @throws OWLOntologyCreationException
	 * @throws JWNLException
	 * @throws FileNotFoundException
	 */
	public static double computeLexicalCoverage(File ontoFile1, File ontoFile2)
			throws OWLOntologyCreationException, FileNotFoundException, JWNLException {

		double lexicalCoverage = (OntologyOperations.getWordNetCoverageComp(ontoFile1)
				+ OntologyOperations.getWordNetCoverageComp(ontoFile2)) / 2;

		return lexicalCoverage;
	}


	/**
	 * Returns a set of common substrings (after having compared character by
	 * character)
	 * 
	 * @param s:
	 *            an input string
	 * @param t:
	 *            an input string
	 * @return a set of common substrings among the input strings
	 */
	private static Set<String> longestCommonSubstrings(String s, String t) {

		int[][] table = new int[s.length()][t.length()];
		int lengthS = s.length();
		int lengthT = t.length();

		//make longest 80 percent of the average length of s and t
		int longest = (int)Math.round(((lengthS + lengthT) / 2) * 0.80);
		Set<String> result = new HashSet<String>();

		//English nouns usually contains more than 3 characters, so we limit the comparison of s and t > 3
		if (lengthS > 3 && lengthT > 3) {
			for (int i = 0; i < s.length(); i++) {
				for (int j = 0; j < t.length(); j++) {
					if (s.toLowerCase().charAt(i) != t.toLowerCase().charAt(j)) {
						continue;
					}

					table[i][j] = (i == 0 || j == 0) ? 1 : 1 + table[i - 1][j - 1];
					if (table[i][j] > longest) {
						longest = table[i][j];
						result.clear();
					}
					if (table[i][j] == longest) {
						result.add(s.substring(i - longest + 1, i + 1));
					}
				}
			}
		}

		return result;
	}

	private static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}


}
