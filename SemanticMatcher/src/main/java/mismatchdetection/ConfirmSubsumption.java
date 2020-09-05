package mismatchdetection;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import utilities.StringUtilities;
import utilities.WNDomain;
import utilities.WordNet;

import rita.wordnet.jwnl.JWNLException;

/**
 * Used in subsumption matching to ensure that the relation proposed is in fact subsumption.
 * @author audunvennesland
 * @see subsumptionmatching.DefinitionSubsumptionMatcher
 * @see subsumptionmatching.DefinitionSubsumptionMatcherSigmoid
 */
public class ConfirmSubsumption {
	
	public static void main(String[] args) throws FileNotFoundException, JWNLException {
		
		String concept2 = "Terminal";
		String concept1 = "AirportSlot";
		
		System.out.println("Are " + concept1 + " and " + concept2 + " meronyms: " + isMeronym(concept1, concept2));
		
	}
	
	/**
	 * Uses the WordNet Domains classification to check if two concepts represent the same domain.
	 * @param concept1 string representing a concept name
	 * @param concept2 string representing a concept name
	 * @return true if the two concepts represent the same domain, false if not.
	 * @throws FileNotFoundException
	 * @throws JWNLException
	   Jul 18, 2019
	 */
	public static boolean conceptsFromSameDomain(String concept1, String concept2) throws FileNotFoundException, JWNLException {
		
		Set<String> concept1Tokens = StringUtilities.getWordsAsSetFromCompound(concept1);
		Set<String> concept2Tokens = StringUtilities.getWordsAsSetFromCompound(concept2);
		
		Set<String> concept1Domains = new HashSet<String>();
		Set<String> concept2Domains = new HashSet<String>();
		
		for (String s : concept1Tokens) {
			concept1Domains.addAll(WNDomain.getDomainTokensFromString(s));
		}
		
		for (String s : concept2Tokens) {
			concept2Domains.addAll(WNDomain.getDomainTokensFromString(s));
		}
		
		if (WNDomain.sameDomain(concept1Domains, concept2Domains)) {
			return true;
		} else {
			return false;
		}
		
		
	}
	

	/**
	 * Checks if two concepts represent a meronymic relation.
	 * @param concept1 string representing a concept name
	 * @param concept2 string representing a concept name
	 * @return true if the concepts represent a meronymic relation, false if not.
	 * @throws FileNotFoundException
	 * @throws JWNLException
	   Jul 18, 2019
	 */
	public static boolean isMeronym(String concept1, String concept2) throws FileNotFoundException, JWNLException {

		Set<String> concept1Tokens = StringUtilities.getWordsAsSetFromCompound(concept1);
		Set<String> concept2Tokens = StringUtilities.getWordsAsSetFromCompound(concept2);
		
		Set<String> concept1Meronyms = new HashSet<String>();
		Set<String> concept2Meronyms = new HashSet<String>();
		
		for (String s : concept1Tokens) {
			concept1Meronyms.addAll(WordNet.getMeronymTokensFromString(s));
		}
		
		for (String s : concept2Tokens) {
			concept2Meronyms.addAll(WordNet.getMeronymTokensFromString(s));
		}
		
		if (concept1Meronyms.contains(concept2.toLowerCase()) || concept2Meronyms.contains(concept1.toLowerCase())) {
			return true;
		} else {
			return false;
		}

		
	}

}
