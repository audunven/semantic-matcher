package utilities;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
//import edu.stanford.nlp.simple.Sentence;
//import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.dictionary.Dictionary;
import rita.RiWordNet;



/**
 * This class contains all methods for accessing the WordNet lexicon. Three different libraries are used: JWNL, WS4J and RiWordNet. 
 * @author audunvennesland
 *
 */
public class WordNet {

	final static POS pos = POS.NOUN;
	private static ILexicalDatabase db = new NictWordNet();
	static RiWordNet database = new RiWordNet("/Users/audunvennesland/Documents/PhD/Development/WordNet/WordNet-3.0/dict");


	/**
	 * Processes a word so that it can be queried in WordNet, basically adding space between compounds and lowercasing it.
	 * @param inputWord
	 * @return
	   Feb 22, 2019
	 */
	public static String getLexicalName (String inputWord) {

		String lexicalName = null;

		//if all chars in the input word are uppercase, we keep it as is
		if (StringUtils.isAllUpperCase(inputWord)) {			
			return inputWord;

			//if the input word is a compound we split it
		} else if (StringUtilities.isCompoundWord(inputWord)) {
			lexicalName = StringUtilities.getCompoundWordWithSpaces(inputWord);
			return lexicalName.toLowerCase();
		}

		//if the word is not a compound nor only uppercase, we return the word in lowercase
		return inputWord.toLowerCase();
	}

	public static String getWordNetLemma (String inputWord) throws JWNLException, FileNotFoundException {

		JWNL.initialize(new FileInputStream("/Users/audunvennesland/git/Compose/compose/file_property.xml"));

		Dictionary dictionary = Dictionary.getInstance();

		IndexWord indexWord = dictionary.lookupIndexWord(pos, inputWord);

		String wordNetLemma = indexWord.getLemma();

		JWNL.shutdown();

		return wordNetLemma;	

	}

	/**
	 * Retrieves synsets from WordNet associated with an input word
	 * @param inputWord The input word for which synsets will be retrieved 
	 * @return Synsets associated with an input word
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Synset[] getSynsetsJWNL (String inputWord) throws FileNotFoundException, JWNLException {

		JWNL.initialize(new FileInputStream("/Users/audunvennesland/git/Compose/compose/file_property.xml"));
		Dictionary dictionary = Dictionary.getInstance();

		String token = StringUtilities.stringTokenize(inputWord, true);

		IndexWord indexWord = dictionary.lookupIndexWord(pos, token);


		Synset[] synsets = indexWord.getSenses();

		//				for (Synset s : synsets) {
		//					s.getGloss();
		//					s.getWord(0).getUsageCount();
		//				}

		JWNL.shutdown();

		return synsets;

	}

	public static boolean isHyponym(String source, String target) {

		Set<String> targetHyps = getAllHyponymsAsSet(target.toLowerCase());

		if (targetHyps.contains(source)) {
			return true;
		} else {
			return false;
		}

	}

	public static boolean containsHyponyms (String source, String target) {

		boolean containsHyponyms = false;

		Set<String> tokens1 = new HashSet<String>();
		Set<String> tokens2 = new HashSet<String>();
		Set<String> hyponyms = new HashSet<String>();

		for (String s : tokens1) {
			hyponyms.addAll(getHyponymsAsSet(s.toLowerCase()));
		}

		for (String s : tokens2) {
			if (hyponyms.contains(s.toLowerCase())) 
				containsHyponyms = true;
			else 
				containsHyponyms = false;

		}

		return containsHyponyms;

	}

	public static boolean containsMeronyms (String source, String target) {

		boolean containsHMeronyms = false;

		Set<String> tokens1 = new HashSet<String>();
		Set<String> tokens2 = new HashSet<String>();
		Set<String> meronyms = new HashSet<String>();

		for (String s : tokens1) {
			meronyms.addAll(getHyponymsAsSet(s.toLowerCase()));
		}

		for (String s : tokens2) {
			if (meronyms.contains(s.toLowerCase())) 
				containsHMeronyms = true;
			else 
				containsHMeronyms = false;

		}

		return containsHMeronyms;
	}

	public static String getGloss(String inputWord) throws FileNotFoundException, JWNLException {

		JWNL.initialize(new FileInputStream("/Users/audunvennesland/git/Compose/compose/file_property.xml"));
		Dictionary dictionary = Dictionary.getInstance();

		String token = StringUtilities.stringTokenize(inputWord, true);

		IndexWord indexWord = dictionary.lookupIndexWord(pos, token);

		Synset[] synsets = indexWord.getSenses();

		JWNL.shutdown();

		StringBuilder gloss = new StringBuilder();

		for (Synset s : synsets) {
			gloss.append(" " + s.getGloss());
		}

		return gloss.toString();


	}

	/**
	 * A method that checks if an input word is present in WordNet
	 * @param inputWord The input word for which presence in WordNet is checked
	 * @return a boolean stating whether or not the input word resides in WordNet
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static boolean containedInWordNet(String inputWord) throws FileNotFoundException, JWNLException {

		JWNL.initialize(new FileInputStream("/Users/audunvennesland/git/Compose/compose/file_property.xml"));

		Dictionary dictionary = Dictionary.getInstance();

		IndexWord indexWord = dictionary.lookupIndexWord(pos, inputWord);

		JWNL.shutdown();

		if (indexWord == null)
		{
			return false;
		}
		else
		{
			return true;
		}			

	}

	public static String[] getSynonyms(String inputWord) {
		String[] synonyms = database.getSynonyms(inputWord, "n");

		return synonyms;
	}



	public static String[] getAllSynonyms(String inputWord) {
		String[] synonyms = database.getAllSynonyms(inputWord, "n");

		return synonyms;
	}





	/**
	 * Retrieves all synonyms (nouns) for the inputWord from WordNet and returns them as a set
	 * @param inputWord
	 * @return
	 */
	public static Set<String> getSynonymSet(String inputWord) {

		Set<String> synSet = new HashSet<String>();

		String[] synonyms = database.getAllSynonyms(inputWord, "n");

		for (int i = 0; i < synonyms.length; i++) {
			synSet.add(synonyms[i]);
		}

		return synSet;
	}

	/**
	 * Retrieves all synonyms (nouns, verbs) for the inputWord from WordNet and returns them as a set
	 * @param inputWord
	 * @return
	 */
	public static Set<String> getAllSynonymSet(String inputWord) {

		Set<String> synSet = new HashSet<String>();

		String[] nounSynonyms = database.getAllSynonyms(inputWord, "n");
		String[] verbSynonyms = database.getAllSynonyms(inputWord, "v");

		//adding the noun synonyms to inputWord
		for (int i = 0; i < nounSynonyms.length; i++) {
			synSet.add(nounSynonyms[i]);
		}
		//adding the verb synonyms to inputWord
		for (int i = 0; i < verbSynonyms.length; i++) {
			synSet.add(verbSynonyms[i]);
		}

		return synSet;
	}

	public static String[] getHyponyms(String inputWord) {
		//String[] hyponyms = database.getAllHyponyms(inputWord, "n");
		String[] hyponyms = database.getHyponyms(inputWord, "n");

		return hyponyms;
	}

	public static Set<String> getHyponymsAsSet (String inputWord) {
		Set<String> hyponymSet = new HashSet<String>();

		String[] hyponyms = getHyponyms(inputWord);

		if (hyponyms.length > 0) {
			for (int i = 0; i < hyponyms.length; i++) {
				hyponymSet.add(hyponyms[i]);
			}

			return hyponymSet;
		} else {
			return null;
		}
	}



	public static String[] getAllHyponyms(String inputWord) {
		String[] hyponyms = database.getAllHyponyms(inputWord, "n");

		return hyponyms;
	}

	public static Set<String> getAllHyponymsAsSet (String inputWord) {
		Set<String> hyponymSet = new HashSet<String>();

		String[] hyponyms = getAllHyponyms(inputWord);

		if (hyponyms.length > 0) {
			for (int i = 0; i < hyponyms.length; i++) {
				hyponymSet.add(hyponyms[i]);
			}

			return hyponymSet;
		} else {
			return null;
		}
	}

	public static String[] getMeronyms(String inputWord) {
		String[] meronyms = database.getAllMeronyms(inputWord, "n");

		return meronyms;
	}

	public static String[] getAntonyms(String inputWord) {
		String[] antonyms = database.getAllAntonyms(inputWord, "n");

		return antonyms;
	}


	/**
	 * Retrieves all meronyms for the inputWord from WordNet and returns them as a set
	 * @param inputWord
	 * @return
	 */
	public static Set<String> getMeronymSet(String inputWord) {

		Set<String> merSet = new HashSet<String>();

		String[] meronyms = database.getAllMeronyms(inputWord, "n");

		for (int i = 0; i < meronyms.length; i++) {
			merSet.add(meronyms[i]);
		}

		return merSet;
	}

	/**
	 * Retrieves a set of meronyms (from WordNet) using a string representation of a word as parameter. In this method "composite" meronyms (e.g. air terminal) are split into individual tokens (e.g. air AND terminal).
	 * @param inputWord String representation of the word 
	 * @return a set of strings representing meronyms associated with the input parameter
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Set<String> getMeronymTokensFromString (String inputWord) throws FileNotFoundException, JWNLException {

		Set<String> meronyms = new HashSet<String>();
		StringBuffer sb = new StringBuffer();

		//get the meronym only if the word is represented in WordNet
		if (WordNet.containedInWordNet(inputWord.toLowerCase())) {
			meronyms = getMeronymSet(inputWord.toLowerCase());
		}

		Set<String> meronymTokens = new HashSet<String>();


		for (String s : meronyms) {

			String[] array = s.split(" ");
			for (String string : array) {

				meronymTokens.add(string);

			}
		}

		return meronymTokens;
	}

	/**
	 * Retrieves all meronyms from the input word provided as parameter as well as synonyms to the input word and returns the meronyms as a set. 
	 * @param inputWord
	 * @return a Set of meronyms
	 */
	public static Set<String> getMeronymsFromSynonyms(String inputWord) {

		Set<String> meronymsFromSynonymSet = new HashSet<String>();

		// get synonyms
		Set<String> synSet = WordNet.getSynonymSet(inputWord.toLowerCase());

		// add also s to set of synonym
		synSet.add(inputWord.toLowerCase());

		Set<String> tempMerSet = new HashSet<String>();

		for (String s : synSet) {
			tempMerSet.addAll(WordNet.getMeronymSet(s));

			meronymsFromSynonymSet.addAll(tempMerSet);
		}

		return meronymsFromSynonymSet;

	}

	private static String[] getHolonyms(String inputWord) {
		String[] holonyms = database.getAllHolonyms(inputWord, "n");

		return holonyms;
	}

	/**
	 * Retrieves all holonyms for the inputWord from WordNet and returns them as a set
	 * @param inputWord
	 * @return
	 */
	public static Set<String> getHolonymSet(String inputWord) {

		Set<String> holSet = new HashSet<String>();

		String[] holonyms = database.getAllHolonyms(inputWord, "n");

		for (int i = 0; i < holonyms.length; i++) {
			holSet.add(holonyms[i]);
		}

		return holSet;
	}

	/**
	 * Retrieves all holonyms from the input word provided as parameter as well as synonyms to the input word and returns the holonyms as a set. 
	 * @param inputWord
	 * @return a Set of holonyms
	 */
	public static Set<String> getHolonymsFromSynonyms(String inputWord) {

		Set<String> holonymsFromSynonymSet = new HashSet<String>();

		// get synonyms
		Set<String> synSet = WordNet.getSynonymSet(inputWord.toLowerCase());

		// add also s to set of synonym
		synSet.add(inputWord.toLowerCase());

		Set<String> tempHolSet = new HashSet<String>();

		for (String s : synSet) {
			tempHolSet.addAll(WordNet.getHolonymSet(s));

			holonymsFromSynonymSet.addAll(tempHolSet);
		}

		return holonymsFromSynonymSet;

	}

	public static String[] getHypernyms(String inputWord) {
		String[] hypernyms = database.getAllHypernyms(inputWord, "n");

		return hypernyms;
	}

	public static int[] getSynsetKeys(String inputWord) {
		//IndexWord ind = new IndexWord(inputWord, "n");
		int[] synsetKeys = database.getSenseIds(inputWord, "n");
		return synsetKeys;
	}

	public static String[] getSynsets(String inputWord) {
		String[] synset = database.getSynset(inputWord, "n");

		return synset;
	}

	public static int getNumSenses(String inputWord) {

		return database.getSenseCount(inputWord, "n");
	}

	public static void readWDDomains(File file) {

		FileInputStream fis = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;

		try {
			fis = new FileInputStream(file);

			// Here BufferedInputStream is added for fast reading.
			bis = new BufferedInputStream(fis);
			dis = new DataInputStream(bis);

			// dis.available() returns 0 if the file does not have more lines.
			while (dis.available() != 0) {

				// this statement reads the line from the file and print it to
				// the console.
				System.out.println(dis.readLine());
			}

			// dispose all the resources after using them.
			fis.close();
			bis.close();
			dis.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Lesk (1985) proposed that the relatedness of two words is proportional to to the extent of overlaps of their dictionary definitions. 
	 * This LESK measure is based on adapted Lesk from Banerjee and Pedersen (2002) which uses WordNet as the dictionary for the word definitions. 
	 * Computational cost is relatively high due to combinations of linked synsets to explore definitions, and need to process these texts.
	 * LESK(s1, s2) = sum_{s1' in linked(s1), s2' in linked(s2)}(overlap(s1'.definition, s2'.definition)). 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeLESK(String s1, String s2)  {


		WS4JConfiguration.getInstance().setMFS(true);
		double s = new Lesk(db).calcRelatednessOfWords(s1, s2);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}
	/**
	 * Idea is similar to JCN with small modification. LIN(s1, s2) = 2*IC(LCS(s1, s2) / (IC(s1) + IC(s2))
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeLin(String s1, String s2)  {

		WS4JConfiguration.getInstance().setMFS(true);
		double s = new Lin(db).calcRelatednessOfWords(s1, s2);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}

	/**
	 * This relatedness measure is based on an idea that two lexicalized concepts are semantically close if their WordNet synsets are connected 
	 * by a path that is not too long and that "does not change direction too often". 
	 * Computational cost is relatively high since recursive search is done on subtrees in the horizontal, upward and downward directions. 
	 * HSO(s1, s2) = const_C - path_length(s1, s2) - const_k * num_of_changes_of_directions(s1, s2) 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeHirstStOnge(String s1, String s2)  {


		WS4JConfiguration.getInstance().setMFS(true);
		double s = new HirstStOnge(db).calcRelatednessOfWords(s1, s2);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}

	/**
	 * Originally a distance measure which also uses the notion of information content, but in the form of the conditional probability of 
	 * encountering an instance of a child-synset given an instance of a parent synset.
	 * JCN(s1, s2) = 1 / jcn_distance where jcn_distance(s1, s2) = IC(s1) + IC(s2) - 2*IC(LCS(s1, s2)); 
	 * when it's 0, jcn_distance(s1, s2) = -Math.log_e( (freq(LCS(s1, s2).root) - 0.01D) / freq(LCS(s1, s2).root) ) 
	 * so that we can have a non-zero distance which results in infinite similarity.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeJiangConrath(String s1, String s2)  {

		WS4JConfiguration.getInstance().setMFS(true);
		double s = new JiangConrath(db).calcRelatednessOfWords(s1, s2);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}

	public static String getMostFrequentConcept (String s, String t) {

		WS4JConfiguration.getInstance().setMFS(true);

		Concept mostFrequent = new JiangConrath(db).getDB().getMostFrequentConcept(s, t);


		return mostFrequent.getName();

	}

	/** 
	 * Resnik defined the similarity between two synsets to be the information content of their lowest super-ordinate (most specific common subsumer) 
	 * RES(s1, s2) = IC(LCS(s1, s2)). 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeResnik(String s1, String s2)  {


		WS4JConfiguration.getInstance().setMFS(true);
		double s = new Resnik(db).calcRelatednessOfWords(s1, s2);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}


	public static double computeLeacockChodorow(String s1, String s2)  {


		WS4JConfiguration.getInstance().setMFS(true);
		double s = new LeacockChodorow(db).calcRelatednessOfWords(s1, s2);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}
	/**
	 * This measure calculates relatedness by considering the depths of the two synsets in the WordNet taxonomies, along with the depth of the LCS 
	 * WUP(s1, s2) = 2*dLCS.depth / ( min_{dlcs in dLCS}(s1.depth - dlcs.depth)) + min_{dlcs in dLCS}(s2.depth - dlcs.depth) ), 
	 * where dLCS(s1, s2) = argmax_{lcs in LCS(s1, s2)}(lcs.depth). 
	 * @param s1
	 * @param s2
	 * @return
	 * @throws JWNLException 
	 * @throws FileNotFoundException 
	 */
	public static double computeWuPalmer(String s1, String s2) throws FileNotFoundException, JWNLException  {

		double sim = 0;
		if (containedInWordNet(s1) && containedInWordNet(s2)) {

			WS4JConfiguration.getInstance().setMFS(true);
			sim = new WuPalmer(db).calcRelatednessOfWords(s1, s2);
		} else {
			sim = 0;
		}
		return sim;
	}

	/**
	 * This module computes the semantic relatedness of word senses by counting the number of nodes along the shortest path between the senses in the 'is-a' 
	 * hierarchies of WordNet.
	 * PATH(s1, s2) = 1 / path_length(s1, s2). 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computePath(String s1, String s2)  {

		WS4JConfiguration.getInstance().setMFS(true);
		double s = new Path(db).calcRelatednessOfWords(s1, s2);

		return s;
	}

	/**
	 * Checks how many of the classes in onto1 and onto2 are represented by synsets in WordNet
	 * @param onto1 OWLOntology 1
	 * @param onto2 OWLOntology 2
	 * @return an int stating how many classes are represented in WordNet
	 * @throws FileNotFoundException
	 * @throws JWNLException
	   Nov 26, 2018
	 */
	public static int wordNetRepresentation(OWLOntology onto1, OWLOntology onto2) throws FileNotFoundException, JWNLException {
		int rep = 0;	

		Set<OWLClass> onto1Cls = onto1.getClassesInSignature();
		Set<OWLClass> onto2Cls = onto2.getClassesInSignature();
		//merge the sets
		onto2Cls.addAll(onto1Cls);

		System.out.println("Test: There are " + onto2Cls.size() + " distinct classes in the two ontologies");

		for (OWLClass cls : onto2Cls) {
			if (WordNet.containedInWordNet(cls.getIRI().getFragment().toLowerCase())) {
				rep++;
			}
		}

		return rep;
	}

	public static double computeAvgJCSim(Set<String> concept1Mods, Set<String> concept2Mods) {

		double globalJCSim = 0;
		int counter = 0;

		for (String c1Mod : concept1Mods) {
			for (String c2Mod : concept2Mods) {
				counter++;
				double localJCSim = WordNet.computeJiangConrath(c1Mod.toLowerCase(), c2Mod.toLowerCase());
				System.out.println("The localJCSim between " + c1Mod + " and " + c2Mod + " is " + localJCSim);
				globalJCSim += localJCSim;
				System.out.println("The globalJCSim is now: " + globalJCSim);
				System.out.println("The counter is now " + counter);

			}
		}
		
		System.out.println("Returning " + globalJCSim + " / " + counter);

		return globalJCSim / counter;


	}

	public static void main(String[] args) throws FileNotFoundException, JWNLException {

		String concept1 = "strip";
		String concept2 = "cartoon";
		
		System.out.println("The similarity between " + concept1 + " and " + concept2 + " is " + computeJiangConrath(concept1, concept2));
		
		String c1 = "author";
		String c2 = "writer";
		
		Set<String> c1Set = getSynonymSet(c1);
		Set<String> c2Set = getSynonymSet(c2);
		
		System.out.println("The jaccard similarity between c1Set and c2Set is " + SimilarityMetrics.jaccardSetSim(c1Set, c2Set));



		//public static boolean containsHyponyms (String source, String target) {



		//		Set<String> s_set = getAllSynonymSet(s);
		//		Set<String> t_set = getAllSynonymSet(t);
		//		
		//		double jaccard = Jaccard.jaccardSetSim(s_set, t_set);
		//		
		//		System.out.println("\nThe Jaccard is " + jaccard);
		//		
		//		System.out.println("The Jiang-Conrath score between " + s + " and " + t + " is " + computeJiangConrath(s, t));
		//
		//		String term = "hemodialysis)aimedimproving";
		//		System.out.println("Is " + term + " contained in wordnet: " + containedInWordNet(term));
		//
		//		
		//		System.out.println("The hypernyms of " + term + " are:");
		//		
		//		String[] hypernyms = getHypernyms(term);
		//		
		//		for (int i = 0; i < hypernyms.length; i++) {
		//			System.out.println(hypernyms[i]);
		//		}
		//		
		//		System.out.println("The lemma of " + term + " is " + StringUtilities.getLemma(term));
		//		
		//		
		//		System.out.println("The hyponyms of " + term + " are:");
		//		String[] hyponymsOfWord = getHyponyms(term);
		//		System.out.println("Hyponyms of " + term + ": " + hyponymsOfWord.length);
		//		for (int i = 0; i < hyponymsOfWord.length; i++) {
		//			System.out.println(hyponymsOfWord[i]);
		//		}
		//
		//		System.out.println("\nAll hyponyms of " + term + " are:");
		//		String[] allhHyponymsOfWord = getAllHyponyms(term);
		//		for (int i = 0; i < allhHyponymsOfWord.length; i++) {
		//			System.out.println(allhHyponymsOfWord[i]);
		//		}
		//
		//		System.out.println("The gloss of " + term + " is: " + getGloss(term));


		//		String s1 = "article";
		//		String t1 = "publication";
		//		String t2 = "book";
		//		String t3 = "report";
		//		String t4 = "brochure";
		//		String t5 = "automobile";
		//		
		//		ArrayList<String> source = new ArrayList<String>();
		//		ArrayList<String> target = new ArrayList<String>();
		//		source.add(s1);
		//		target.add(t1);
		//		target.add(t2);
		//		target.add(t3);
		//		target.add(t4);
		//		target.add(t5);
		//		
		//		for (String s : source) {
		//			for (String t : target) {
		//				System.out.println("\nThe similarity between + " + s + " and " + t);
		//				System.out.println("Jiang Conrath: " + computeJiangConrath(s, t));
		//				System.out.println("HirstStOnge: " + computeHirstStOnge(s, t));
		//				System.out.println("Leacock Chodorow: " + computeLeacockChodorow(s, t));
		//				System.out.println("LESK: " + computeLESK(s, t));
		//				System.out.println("Resnik: " + computeResnik(s, t));
		//				System.out.println("Lin: " + computeLin(s, t));
		//				System.out.println("Wu-Palmer: " + computeWuPalmer(s, t));
		//			}
		//		}

		//		

		//		String word = "article";
		//		String lemma = new Sentence(word).lemma(0);
		//		System.out.println("The lemma of " + word + " is " + lemma);

		//		System.out.println("The gloss of " + word + " is ''" + getGloss(word) + "''");
		//		
		//		//public static Set<String> getSynonymSet(String inputWord) {
		//		Set<String> synonymSet = getAllSynonymSet(lemma);
		//		Set<String> meronymSet = getMeronymSet(word);
		//		Set<String> holonymSet = getHolonymSet(word);
		//		
		//		System.out.println("Synonyms for : " + word);
		//		for (String s : synonymSet) {
		//			System.out.println(s);
		//		}
		//		
		//		System.out.println("Meronyms:");
		//		for (String s : meronymSet) {
		//			System.out.println(s);
		//		}
		//		
		//		System.out.println("Holonyms:");
		//		for (String s : holonymSet) {
		//			System.out.println(s);
		//		}
		//		
		//		System.out.println("WuPalmer: " + computeWuPalmer("subsequence", "flight"));
		//		
	}

}
