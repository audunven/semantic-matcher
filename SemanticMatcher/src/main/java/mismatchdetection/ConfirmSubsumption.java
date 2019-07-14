package mismatchdetection;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import net.didion.jwnl.JWNLException;
import utilities.StringUtilities;
import utilities.WNDomain;
import utilities.WordNet;

public class ConfirmSubsumption {
	
	public static void main(String[] args) throws FileNotFoundException, JWNLException {
		
		String concept2 = "Terminal";
		String concept1 = "AirportSlot";
		
		System.out.println("Are " + concept1 + " and " + concept2 + " meronyms: " + isMeronym(concept1, concept2));
		
	}
	
	
	public static boolean conceptsFromSameDomain(String s1, String s2) throws FileNotFoundException, JWNLException {
		
		Set<String> concept1Tokens = StringUtilities.getWordsAsSetFromCompound(s1);
		Set<String> concept2Tokens = StringUtilities.getWordsAsSetFromCompound(s2);
		
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
	
	public static boolean isSynonym(String concept1, String concept2) {
		
		Set<String> concept1Synonyms = WordNet.getAllSynonymSet(concept1);
		Set<String> concept2Synonyms = WordNet.getAllSynonymSet(concept2);
		
		if (concept1Synonyms.contains(concept2.toLowerCase()) || concept2Synonyms.contains(concept2.toLowerCase())) {
			return true;
		} else {
			return false;
		}
		
	}

	
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
