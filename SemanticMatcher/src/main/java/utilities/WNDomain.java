package utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import rita.wordnet.jwnl.JWNLException;
import rita.wordnet.jwnl.wndata.Synset;

/**
 * Various methods used for interacting with the WordNet Domains Classification.
 * @author audunvennesland
 *
 */
public class WNDomain {
	
	final static String WN_DOMAINS_FILE = "./files/wndomains/wn-domains-3.2-20070223.txt";
	
	public static void main(String[] args) throws FileNotFoundException, JWNLException {
		
		String s1 = "car";
		String s2 = "automobile";
		
		String offset = "2853224";
		
		System.out.println("Domain is " + findDomain(WN_DOMAINS_FILE, offset));
		
		System.out.println(sameDomainJaccard(s1, s2, 0.3));
		
	}

	/**
	 * Returns a list of offsets associated with an input word
	 * @param inputWord the word whose offset is returned
	 * @return a list of synset offsets
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static List<Long> findSynsetOffset(String inputWord) throws FileNotFoundException, JWNLException {
		Synset[] synsets = WordNet.getSynsetsJWNL(inputWord);
		List<Long> offsetList = new ArrayList<Long>();

		if (synsets.length == 0) {
		} else {
			for (Synset s : synsets) {
				offsetList.add(s.getOffset());		   
			}
		}

		return offsetList;
	}

	/**
	 * Returns a list of domains converted from offsets (long)
	 * @param offset list of synsets offsets associated with WordNet synsets 
	 * @return a list of domains converted from offsets (long)
	 * @throws FileNotFoundException 
	 */
	public static Set<String> convertOffsetToString(List<Long> offset) throws FileNotFoundException {
		Set<String> synsets = new HashSet<String>();

		for (Long l : offset) {
			synsets.add(findDomain(WN_DOMAINS_FILE, l.toString()));
		}

		return synsets;
	}
	

	
	/**
	 * Returns true if two strings belong to the same domain, i.e. their offsets are associated with the same domains in WNDomains.
	 * @param s1 the first input string
	 * @param s2 the second input string
	 * @return boolean stating whether two strings are associated with the same domains
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static boolean sameDomainJaccard(String s1, String s2, double minJaccard) throws FileNotFoundException, JWNLException {


		List<Long> l1 = findSynsetOffset(s1);
		List<Long> l2 = findSynsetOffset(s2);

		Set<String> s1_offsetStrings = convertOffsetToString(l1);
		Set<String> s2_offsetStrings = convertOffsetToString(l2);
		
		//remove factotum
		s1_offsetStrings.remove("factotum");
		s2_offsetStrings.remove("factotum");

		boolean same = sameDomainJaccard(s1_offsetStrings, s2_offsetStrings, minJaccard);

		return same;
	}

	/**
	 * Checks if two separate sets contain equal strings. If the sets only share 1 common element, sameDomain is set to true, otherwise false
	 * @param firstSetOfDomains first set of domains associated with an ontology concept
	 * @param secondSetOfDomains second set of domains associated with an ontology concept
	 * @return boolean stating whether the two input sets contains equal values
	 */
	public static boolean sameDomain(Set<String> firstSetOfDomains, Set<String> secondSetOfDomains) {
		boolean similar = false;

		for (String s1 : firstSetOfDomains) {
			for (String s2 : secondSetOfDomains) {
				if (s1.equals(s2)) {
					similar = true;
					break;
				}
			}
		}

		return similar;
	}
	
	/**
	 * Checks if two separate sets contain equal strings. If the computed Jaccard score is greater than minJaccard parameter, sameDomain is set to true, otherwise false
	 * @param firstSetOfDomains first set of domains associated with an ontology concept
	 * @param secondSetOfDomains second set of domains associated with an ontology concept
	 * @return boolean stating whether the two input sets contains equal values
	 */
	public static boolean sameDomainJaccard(Set<String> firstSetOfDomains, Set<String> secondSetOfDomains, double minJaccard) {
		boolean similar = false;

		double jaccardScore = SimilarityMetrics.jaccardSetSim(firstSetOfDomains, secondSetOfDomains);
		
		//need to check that none of the sets are empty (this means that there are no domains) and that the computed jaccard score is above the minimum jaccard constraint		
		if ((!firstSetOfDomains.isEmpty() && !secondSetOfDomains.isEmpty()) && jaccardScore >= minJaccard) {
			similar = true;
		} else {
			similar = false;
		}

		return similar;
	}


	/**
	 * Retrieves the domains (as string) given a synset offset as parameter from the WordNet Domains domain classification file. In the classification file each line is represented as 'Synset offset (tab) Domain name'
	 * @param WNDomainsFileName Name of the WordNet Domains domain classification file
	 * @param searchStr the input synset offset
	 * @return domain(s) associated with the synset offset from the WordNet Domains domain classification file
	 * @throws FileNotFoundException
	 */
	private static String findDomain(String WNDomainsFileName,String searchStr) throws FileNotFoundException{    	
		String domain = null;
		Scanner scan = new Scanner(new File(WNDomainsFileName));
		
		while(scan.hasNext()){
			String line = scan.nextLine().toLowerCase().toString();
			if(line.contains(searchStr)){
				StringTokenizer tokenizer = new StringTokenizer(line, "	");
				List<String> parts = new ArrayList<String>();
				while(tokenizer.hasMoreTokens()) { 
					String part = tokenizer.nextToken();
					parts.add(part);
				}

				//adds only the textual representation of the domain to String domain (parts.get(0) is the synset number (long))
				domain = parts.get(1);
			}
		}
		
		//28.07.2019 added close method to scanner to avoid resource leak
		scan.close();
		
		return domain;
	}

	
	/**
	 * Retrieves a set of domain descriptions (from WNDomains) using a string representation of a word as parameter. 
	 * @param inputWord String representation of the word (synset)
	 * @return a set of strings representing domains associated with the input parameter
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Set<String> getDomainsFromString (String inputWord) throws FileNotFoundException, JWNLException {
		
		List<Long> synsetOffsets = null;
		Set<String> synsetList = new HashSet<String>();
		
		//get the synset offset if the word is represented in WordNet
		if (WordNet.containedInWordNet(inputWord)) {
			synsetOffsets = findSynsetOffset(inputWord);
			synsetList = convertOffsetToString(synsetOffsets);
		}
		
		//remove duplicates and "factotum"
		Set<String> synsetSet = new HashSet<String>();
		
		for (String s : synsetList) {
			
			if (!s.equals("factotum")) {
			synsetSet.add(s);
			}
		}
		
		return synsetSet;
	}
	
	/**
	 * Retrieves a set of domain descriptions (from WNDomains) using a string representation of a word as parameter. In this method "composite" domains (e.g. administration-politics) are split into individual tokens (e.g. administration AND polities).
	 * @param inputWord String representation of the word (synset)
	 * @return a set of strings representing domains associated with the input parameter
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Set<String> getDomainTokensFromString (String inputWord) throws FileNotFoundException, JWNLException {
		
		List<Long> synsetOffsets = null;
		Set<String> synsetList = new HashSet<String>();
		
		//get the synset offset if the word is represented in WordNet
		if (WordNet.containedInWordNet(inputWord)) {
			synsetOffsets = findSynsetOffset(inputWord);
			synsetList = convertOffsetToString(synsetOffsets);
		}
		
		//remove duplicates and "factotum"
		Set<String> synsetSet = new HashSet<String>();
		
		
		for (String s : synsetList) {
			
			if (!s.equals("factotum")) {
				
				String[] array = s.split(" ");
				for (String string : array) {
				
					synsetSet.add(string);
				}
			}
		}
		
		
		
		return synsetSet;
	}


}