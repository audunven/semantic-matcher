package utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import rita.RiWordNet;
import rita.wordnet.jwnl.JWNL;
import rita.wordnet.jwnl.JWNLException;
import rita.wordnet.jwnl.dictionary.Dictionary;
import rita.wordnet.jwnl.wndata.IndexWord;
import rita.wordnet.jwnl.wndata.POS;
import rita.wordnet.jwnl.wndata.Synset;



/**
 * This class contains all methods for accessing the WordNet lexicon. Three different libraries are used: JWNL, WS4J and RiWordNet. 
 * @author audunvennesland
 *
 */
public class WordNet {

	final static POS pos = POS.NOUN;
	private static ILexicalDatabase db = new NictWordNet();
	//final static String JWNL_FILE = "./files/WordNet-3.0/file_property.xml";
	
	//Note that the WNDomains classification relies on WordNet-2.0
	static RiWordNet database = new RiWordNet("./files/WordNet-2.0/dict");
	
	public static void main(String[] args) throws FileNotFoundException, JWNLException {
		
		String inputWord = "automobile";
		
		System.out.println("Is " + inputWord + " contained in WordNet?: " + containedInWordNet(inputWord));
		
		System.out.println(getLexicalName(inputWord));
		
		Synset[] synsets = getSynsetsJWNL(inputWord);
		
		for (Synset s : synsets) {
			System.out.println(s);
		}
	}


	/**
	 * Processes a word so that it can be queried in WordNet, basically adding space between compounds and lowercasing it.
	 * @param inputWord the word to be processed
	 * @return a processed word ready for querying WordNet
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

	

	/**
	 * Retrieves synsets from WordNet associated with an input word using the JWNL library.
	 * @param inputWord The input word for which synsets will be retrieved 
	 * @return Synsets associated with an input word
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Synset[] getSynsetsJWNL (String inputWord) throws FileNotFoundException, JWNLException {

		//JWNL.initialize(new FileInputStream(JWNL_FILE));
		Dictionary dictionary = Dictionary.getInstance();

		String token = StringUtilities.stringTokenize(inputWord, true);

		IndexWord indexWord = dictionary.lookupIndexWord(pos, token);


		Synset[] synsets = indexWord.getSenses();

		//JWNL.shutdown();

		return synsets;

	}


	/**
	 * A method that checks if an input word is present in WordNet
	 * @param inputWord The input word for which presence in WordNet is checked
	 * @return a boolean stating whether or not the input word resides in WordNet
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static boolean containedInWordNet(String inputWord) throws FileNotFoundException, JWNLException {

		//JWNL.initialize(new FileInputStream(JWNL_FILE));

		Dictionary dictionary = Dictionary.getInstance();

		IndexWord indexWord = dictionary.lookupIndexWord(pos, inputWord.toLowerCase());

		//JWNL.shutdown();

		if (indexWord == null)
		{
			return false;
		}
		else
		{
			return true;
		}			

	}

	/**
	 * Retrieves a basic set of WordNet synonyms for inputWord and returns them as an array.
	 * @param inputWord the input word for which WordNet synonyms are retrieved.
	 * @return an array of WordNet synonyms
	   Jul 28, 2019
	 */
	public static String[] getSynonyms(String inputWord) {
		String[] synonyms = database.getSynonyms(inputWord, "n");

		return synonyms;
	}


	/**
	 * Retrieves an extended set of WordNet synonyms for inputWord and returns them as an array.
	 * @param inputWord the input word for which WordNet synonyms are retrieved.
	 * @return an array of WordNet synonyms
	   Jul 28, 2019
	 */
	public static String[] getAllSynonyms(String inputWord) {
		String[] synonyms = database.getAllSynonyms(inputWord, "n");

		return synonyms;
	}


	/**
	 * Retrieves all synonyms (nouns) for the inputWord from WordNet and returns them as a set
	 * @param inputWord the input word for which WordNet synonyms are retrieved.
	 * @return a set of WordNet synonyms
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
	 * @param inputWord the input word for which WordNet synonyms are retrieved.
	 * @return a set of WordNet synonyms
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

	/**
	 * Retrieves a basic set of WordNet hyponyms for the inputWord and returns them as an array
	 * @param inputWord the input word for which WordNet hyponyms are retrieved.
	 * @return an array of WordNet hyponyms
	   Jul 28, 2019
	 */
	public static String[] getHyponyms(String inputWord) {
		//String[] hyponyms = database.getAllHyponyms(inputWord, "n");
		String[] hyponyms = database.getHyponyms(inputWord, "n");

		return hyponyms;
	}
	
	/**
	 * Retrieves an extended set of WordNet hyponyms for the inputWord and returns them as an array
	 * @param inputWord the input word for which WordNet hyponyms are retrieved.
	 * @return an array of WordNet hyponyms
	   Jul 28, 2019
	 */
	private static String[] getAllHyponyms(String inputWord) {
		String[] hyponyms = database.getAllHyponyms(inputWord, "n");

		return hyponyms;
	}

	
	/**
	 * Retrieves a basic set of WordNet hyponyms for the inputWord and returns them as a set
	 * @param inputWord the input word for which WordNet hyponyms are retrieved.
	 * @return a set of WordNet hyponyms
	   Jul 28, 2019
	 */
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


	/**
	 * Retrieves an extended set of WordNet hyponyms for the inputWord and returns them as a set
	 * @param inputWord the input word for which WordNet hyponyms are retrieved.
	 * @return a set of WordNet hyponyms
	   Jul 28, 2019
	 */
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


	/**
	 * Retrieves all meronyms for the inputWord from WordNet and returns them as a set
	 * @param inputWord the input word for which WordNet meronyms are retrieved.
	 * @return a set of WordNet meronyms
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
	 * @param inputWord the input word for which WordNet meronyms are retrieved.
	 * @return a set of strings representing meronyms associated with the input parameter
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Set<String> getMeronymTokensFromString (String inputWord) throws FileNotFoundException, JWNLException {

		Set<String> meronyms = new HashSet<String>();

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
	 * Retrieves all holonyms for the inputWord from WordNet and returns them as a set
	 * @param inputWord the input word for which WordNet holonyms are retrieved.
	 * @return a set of WordNet holonyms
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
	 * Originally a distance measure which also uses the notion of information content, but in the form of the conditional probability of 
	 * encountering an instance of a child-synset given an instance of a parent synset.
	 * JCN(s1, s2) = 1 / jcn_distance where jcn_distance(s1, s2) = IC(s1) + IC(s2) - 2*IC(LCS(s1, s2)); 
	 * when it's 0, jcn_distance(s1, s2) = -Math.log_e( (freq(LCS(s1, s2).root) - 0.01D) / freq(LCS(s1, s2).root) ) 
	 * so that we can have a non-zero distance which results in infinite similarity.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeJiangConrath(String firstInputString, String secondInputString)  {

		WS4JConfiguration.getInstance().setMFS(true);
		double s = new JiangConrath(db).calcRelatednessOfWords(firstInputString, secondInputString);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}



	/** 
	 * Resnik defined the similarity between two synsets to be the information content of their lowest super-ordinate (most specific common subsumer) 
	 * RES(s1, s2) = IC(LCS(s1, s2)). 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static double computeResnik(String firstInputString, String secondInputString)  {


		WS4JConfiguration.getInstance().setMFS(true);
		double s = new Resnik(db).calcRelatednessOfWords(firstInputString, secondInputString);

		//need a work-around since some of the scores are above 1.0 (not allowed to have a confidence level above 1.0)
		if (s > 1.0) {
			s = 1.0;
		}
		return s;
	}


}