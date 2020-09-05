package utilities;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import edu.stanford.nlp.simple.Sentence;

/**
 * Contains various methods performing string processing operations.
 * @author audunvennesland
 *
 */
public class StringUtilities {

	static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	static OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
	
	public static void main(String[] args) {
		String input = "Challenge";
		
		System.out.println("Lemma: " + getLemma(input));
		
		String compound = "TESTAcademicResearchProject";
		
		String[] parts = getCompoundParts(compound);
		
		for (int i = 0; i < parts.length; i++) {
			System.out.println(parts[i]);
		}
	}

	/**
	 * Returns the lemma of a word using the Stanford SimpleNLP API
	 * @param input the word to create the lemma from
	 * @return the lemma of the input word
	   Jan 26, 2019
	 */
	public static String getLemma (String input) {
		String lemma = new Sentence(input).lemma(0);
		
		return lemma;
	}


	/**
	 * Takes a string as input and returns an arraylist of tokens from this string
	 * @param s: the input string to tokenize
	 * @param lowercase: if the output tokens should be lowercased
	 * @return an ArrayList of tokens
	 */
	public static ArrayList<String> tokenize(String s, boolean lowercase) {
		if (s == null) {
			return null;
		}

		ArrayList<String> strings = new ArrayList<String>();

		String current = "";
		Character prevC = 'x';

		for (Character c: s.toCharArray()) {

			if ((Character.isLowerCase(prevC) && Character.isUpperCase(c)) || 
					c == '_' || c == '-' || c == ' ' || c == '/' || c == '\\' || c == '>') {

				current = current.trim();

				if (current.length() > 0) {
					if (lowercase) 
						strings.add(current.toLowerCase());
					else
						strings.add(current);
				}

				current = "";
			}

			if (c != '_' && c != '-' && c != '/' && c != '\\' && c != '>') {
				current += c;
				prevC = c;
			}
		}

		current = current.trim();

		if (current.length() > 0) {
			// this check is to handle the id numbers in YAGO
			if (!(current.length() > 4 && Character.isDigit(current.charAt(0)) && 
					Character.isDigit(current.charAt(current.length()-1)))) {
				strings.add(current.toLowerCase());
			}
		}

		return strings;
	}
	
	
	/**
	 * Takes a string as input and returns list of lemmatized tokens from this string
	 * @param inputString the input string to tokenize
	 * @param lowercase if the output tokens should be lowercased
	 * @return a set of tokens
	 * @throws IOException
	   Jul 28, 2019
	 */
	public static List<String> tokenizeAndLemmatizeToList(String inputString, boolean lowercase) throws IOException {
		if (inputString == null) {
			return null;
		}
		
		//remove stopwords
		String stringWOStopWords = removeStopWords(inputString);

		List<String> strings = new LinkedList<>();

		String current = "";
		Character prevC = 'x';

		for (Character c: stringWOStopWords.toCharArray()) {

			if ((Character.isLowerCase(prevC) && Character.isUpperCase(c)) || 
					c == '_' || c == '-' || c == ' ' || c == '/' || c == '\\' || c == '>') {

				current = current.trim();

				if (current.length() > 0) {
					if (lowercase) 
						strings.add(getLemma(current).toLowerCase());
					else
						strings.add(getLemma(current));
				}

				current = "";
			}

			if (c != '_' && c != '-' && c != '/' && c != '\\' && c != '>') {
				current += c;
				prevC = c;
			}
		}

		current = current.trim();

		if (current.length() > 0) {
			if (!(current.length() > 4 && Character.isDigit(current.charAt(0)) && 
					Character.isDigit(current.charAt(current.length()-1)))) {
				strings.add(getLemma(current.toLowerCase()));
			}
		}

		return strings;
	}
	
	/**
	 * Returns a string of tokens
	 * @param inputString the input string to be tokenized
	 * @param lowercase whether the output tokens should be in lowercase
	 * @return a string of tokens from the input string
	   Jul 28, 2019
	 */
	public static String stringTokenize(String inputString, boolean lowercase) {
		String result = "";

		ArrayList<String> tokens = tokenize(inputString, lowercase);
		for (String token: tokens) {
			result += token + " ";
		}

		return result.trim();
	}


	/**
	 * Takes a filename as input and removes the IRI prefix from this file
	 * @param fileName a file name whose IRI prefix is to be removed
	 * @return filename without IRI prefix
	 */
	public static String stripPath(String fileName) {
		String trimmedPath = fileName.substring(fileName.lastIndexOf("/") + 1);
		return trimmedPath;

	}
	
	/**
	 * Returns the label from on ontology concept without any prefix
	 * @param label an input label with a prefix (e.g. an IRI prefix) 
	 * @return a label without any prefix
	   Jul 28, 2019
	 */
	public static String getLabelWithoutPrefix(String label) {

		if (label.contains("#")) {
			label = label.substring(label.indexOf('#')+1);
			return label;
		}

		if (label.contains("/")) {
			label = label.substring(label.lastIndexOf('/')+1);
			return label;
		}

		return label;
	}

	/**
	 * Removes underscores from a string (replaces underscores with "no space")
	 * @param input string with an underscore
	 * @return string without any underscores
	   Jul 28, 2019
	 */
	public static String replaceUnderscore (String input) {
		String newString = null;
		Pattern p = Pattern.compile( "_([a-zA-Z])" );
		Matcher m = p.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, m.group(1).toUpperCase());
		}

		m.appendTail(sb);
		newString = sb.toString();

		return newString;
	}

	
	/**
	 * Returns the name of the ontology from the full file path (owl or rdf suffixes)
	 * @param fileName a file name
	 * @return file name without owl or rdf suffix
	   Jul 28, 2019
	 */
	public static String stripOntologyName(String fileName) {

		String trimmedPath = fileName.substring(fileName.lastIndexOf("/") + 1);
		String owl = ".owl";
		String rdf = ".rdf";
		String stripped = null;

		if (fileName.endsWith(".owl")) {
			stripped = trimmedPath.substring(0, trimmedPath.indexOf(owl));
		} else {
			stripped = trimmedPath.substring(0, trimmedPath.indexOf(rdf));
		}

		return stripped;
	}

	/**
	 * Convert from a filename to a file URL.
	 * @param filename filename to be converted to URL.
	 * @return URL from input filename
	   Jul 28, 2019
	 */
	public static String convertToFileURL ( String filename )
	{

		String path = new File ( filename ).getAbsolutePath ();
		if ( File.separatorChar != '/' )
		{
			path = path.replace ( File.separatorChar, '/' );
		}
		if ( !path.startsWith ( "/" ) )
		{
			path = "/" + path;
		}
		String retVal =  "file:" + path;

		return retVal;
	}

	/**
	 * Ensures that the less-than relation is presented properly in an alignment file
	 * @param relType a relation type
	 * @return properly representation of less-than relation
	   Jul 28, 2019
	 */
	public static String validateRelationType (String relType) {
		if (relType.equals("<")) {
			relType = "&lt;";
		}

		return relType;
	}


	/**
	 * Removes stopwords from an input string
	 * @param inputString the input string from which stopwords will be removed
	 * @return inputString without stopwords
	   Jul 28, 2019
	 */
	public static String removeStopWords (String inputString) {

		List<String> stopWordsList = Arrays.asList(
				"a", "an", "and", "are", "as", "at", "be", "but", "by",
				"for", "if", "in", "into", "is", "it",
				"no", "not", "of", "on", "or", "such",
				"that", "the", "their", "then", "there", "these",
				"they", "this", "to", "was", "will", "with"
				);

		String[] words = inputString.split(" ");
		ArrayList<String> wordsList = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();

		for(String word : words)
		{
			String wordCompare = word.toLowerCase();
			if(!stopWordsList.contains(wordCompare))
			{
				wordsList.add(word);
			}
		}

		for (String str : wordsList){
			sb.append(str + " ");
		}

		return sb.toString();
	}
	
	public static String[] getCompoundParts(String input) {
		
		return input.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
		//return input.split("(?<=.)(?=\\p{Lu})");
	}

	
	/**
	 * Checks if an input string is a compound word
	 * @param inputString an input string
	 * @return true if inputString is compound, false if not
	   Jul 28, 2019
	 */
	public static boolean isCompoundWord(String input) {
		
		String[] compounds = getCompoundParts(input);
		
		//must check if s is not all uppercase
		if (compounds.length > 1 && !StringUtils.isAllUpperCase(input)) {
			return true;
		} else {
			return false;
		}
		
	}
	
//	public static String splitCompounds(String input) {
//
//		String[] compounds = getCompounds(input);
//
//		StringBuilder sb = new StringBuilder();
//
//		for (int i = 0; i < compounds.length; i++) {
//
//			sb.append(compounds[i] + " ");
//
//		}
//
//		String compoundedString = sb.toString();
//
//		return compoundedString;
//	}

	/**
	 * Retrieves all compound parts from an input string and returns these parts with space in between.
	 * @param inputString an input string
	 * @return compound parts from input string with space in between.
	   Jul 28, 2019
	 */
	public static String getCompoundWordWithSpaces (String inputString) {
				
		StringBuffer sb = new StringBuffer();
		
		ArrayList<String> compoundWordsList = getWordsFromCompound(inputString);
				
		for (String word : compoundWordsList) {
			
			sb.append(word + " ");
			
		}
		
		String compoundWordWithSpaces = sb.toString();
		
		return compoundWordWithSpaces;
	}

	/**
	 * Retrieves the compound head from a compound word
	 * @param inputString an input string
	 * @return the compound head
	   Jul 28, 2019
	 */
	public static String getCompoundHead(String input) {
		
		if (isCompoundWord(input)) {
		
		String[] compounds = getCompoundParts(input);
		String compoundHead = compounds[compounds.length-1];
		
		return compoundHead;
		
		} else {
			return null;
		}
	}
	
	
	/**
	 * Retrieves the compound qualifier (i.e. the first compound part in front of the compound head) from a compound word
	 * @param inputString an input string
	 * @return the compound qualifier 
	   Jul 28, 2019
	 */
	public static String getCompoundFirstQualifier(String input) {
		
		String[] compounds = getCompoundParts(input);
		
		String compoundQualifier = compounds[0];
		
		return compoundQualifier;
	}
	
	
	/**
	 * Retrieves the compound modifier (i.e. the modifying word (which may also represent sets of individual words) from a compound.
	 * @param inputString an input string
	 * @return the compound modifier.
	   Jul 28, 2019
	 */
	public static String getCompoundModifier(String inputString) {
		
		return inputString.replace(getCompoundHead(inputString), "");
	}
	
	
	/**
	 * Retrieves all compound parts from an input string and returns them as an array list 
	 * @param inputString an input string
	 * @return an arraylist of compound parts
	   Jul 28, 2019
	 */
	public static ArrayList<String> getWordsFromCompound (String input) {
		
		String[] compounds = getCompoundParts(input);
		
		ArrayList<String> compoundWordsList = new ArrayList<String>();
		
		for (int i = 0; i < compounds.length; i++) {
			compoundWordsList.add(compounds[i]);
		}
		
		return compoundWordsList;
		
	}
	
	/**
	 * Retrieves all compound parts from an input string and returns them as a set
	 * @param inputString an input string
	 * @return a set of compound parts
	   Jul 28, 2019
	 */
	public static Set<String> getWordsAsSetFromCompound (String input) {
		
		String[] compounds = getCompoundParts(input);
		
		Set<String> compoundWordsList = new HashSet<String>();
		
		for (int i = 0; i < compounds.length; i++) {
			compoundWordsList.add(compounds[i]);
		}
		
		return compoundWordsList;
		
	}
	



}
