package utilities;

/**
 * @author audunvennesland
 * 6. mar. 2017 
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.Synset;


public class WNDomain {

	/**
	 * Returns a list of offsets associated with an input word
	 * @param inputWord
	 * @return a list of synset offsets
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static List<Long> findSynsetOffset(String inputWord) throws FileNotFoundException, JWNLException {
		Synset[] synsets = WordNet.getSynsetsJWNL(inputWord);
		List<Long> offsetList = new ArrayList<Long>();

		if (synsets.length == 0) {
			//System.out.println("There are no synsets for " + inputWord);
		} else {
			for (Synset s : synsets) {
				offsetList.add(s.getOffset());		   
			}
		}

		return offsetList;
	}

	/**
	 * Returns a list (ArrayList) of domains converted from offsets (long)
	 * @param list of synsets offsets associated with WordNet synsets 
	 * @return a list (ArrayList) of domains converted from offsets (long)
	 * @throws FileNotFoundException 
	 */
	public static Set<String> convertOffsetToString(List<Long> offset) throws FileNotFoundException {
		Set<String> synsets = new HashSet<String>();

		for (Long l : offset) {
			synsets.add(findDomain("./files/wndomains/wn-domains-3.2-20070223.txt", l.toString()));
		}

		return synsets;
	}
	
	/**
	 * Returns true if two strings belong to the same domain, i.e. their offsets are associated with the same domains in WNDomains.
	 * @param s1 input string
	 * @param s2 input string
	 * @return boolean stating whether two strings are associated with the same domains
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static boolean sameDomain(String s1, String s2) throws FileNotFoundException, JWNLException {


		List<Long> l1 = findSynsetOffset(s1);
		List<Long> l2 = findSynsetOffset(s2);

		Set<String> s1_offsetStrings = convertOffsetToString(l1);
		Set<String> s2_offsetStrings = convertOffsetToString(l2);
		
		//remove factotum
		s1_offsetStrings.remove("factotum");
		s2_offsetStrings.remove("factotum");

		boolean same = sameDomain(s1_offsetStrings, s2_offsetStrings);

		return same;
	}
	
	/**
	 * Returns true if two strings belong to the same domain, i.e. their offsets are associated with the same domains in WNDomains.
	 * @param s1 input string
	 * @param s2 input string
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
	 * Checks if two separate arraylists contain equal strings. If the sets only share 1 common element, sameDomain is set to true, otherwise false
	 * @param c1 list of domains associated with an ontology concept
	 * @param c2 list of domains associated with an ontology concept
	 * @return boolean stating whether the two input arraylist contains equal values
	 */
	public static boolean sameDomain(Set<String> c1, Set<String> c2) {
		boolean similar = false;

		for (String s1 : c1) {
			for (String s2 : c2) {
				if (s1.equals(s2)) {
					similar = true;
					break;
				}
			}
		}

		return similar;
	}
	
	/**
	 * Checks if two separate arraylists contain equal strings. If the computed Jaccard score >= minJaccard, sameDomain is set to true, otherwise false
	 * @param c1 list of domains associated with an ontology concept
	 * @param c2 list of domains associated with an ontology concept
	 * @return boolean stating whether the two input arraylist contains equal values
	 */
	public static boolean sameDomainJaccard(Set<String> c1, Set<String> c2, double minJaccard) {
		boolean similar = false;

		double jaccardScore = SimilarityMetrics.jaccardSetSim(c1, c2);
		
		//System.out.println("The jaccard score is " + jaccardScore);
		
		//need to check that none of the sets are empty (this means that there are no domains) and that the computed jaccard score is above the minimum jaccard constraint		
		if ((!c1.isEmpty() && !c2.isEmpty()) && jaccardScore >= minJaccard) {
			similar = true;
		} else {
			similar = false;
		}

		return similar;
	}


	/**
	 * Retrieves the domains (as string) given a synset offset as parameter from the WordNet Domains domain classification file. In the classification file each line is represented as '[Synset offset] (tab) [Domain name]'
	 * @param WNDomainsFileName Name of the WordNet Domains domain classification file
	 * @param searchStr The input synset offset
	 * @return Domains associated with the synset offset in the WordNet Domains domain classification file
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
		
		return domain;
	}


	/**
	 * Returns an array list of domains associated with an ontology
	 * @param ontoFile The ontology for which domains should be retrieved
	 * @return A list of domains associated with all concepts in an ontology
	 * @throws OWLOntologyCreationException
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static ArrayList<String> getDomainsFromFile (File ontoFile) throws OWLOntologyCreationException, FileNotFoundException, JWNLException {

		ArrayList<String> domains = new ArrayList<String>();


		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();		
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);

		Set<OWLClass> classes = onto.getClassesInSignature();
		//System.out.println("Number of classes in ontology: " + classes.size());

		for (OWLClass cls : classes) {
			//System.out.println("Trying " + cls.getIRI().getFragment().toLowerCase());
			List<Long> offsetList = findSynsetOffset(cls.getIRI().getFragment().toLowerCase());
			for (Long l : offsetList) {
				String offset = l.toString();
				String domain = findDomain("./files/wndomains/wn-domains-3.2-20070223.txt", offset);
				//System.out.println("Found domain for " + cls.getIRI().getFragment().toLowerCase() + ": " + domain);
		
				domains.add(domain);
				
			}
		}

		//need to remove duplicates
		Set<String> hs = new HashSet<>();
		hs.addAll(domains);
		domains.clear();
		domains.addAll(hs);

		return domains;

	}

	/**
	 * Creates a map holding the domains associated with each class in an ontology. This is used for establishing a profile of the domain characteristics of an ontology.
	 * @param ontoFile The ontology file
	 * @return A map holding the ontology class as key and domains associated with the class as values
	 * @throws OWLOntologyCreationException
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 */
	public static Map<String, String> listDomains (File ontoFile) throws OWLOntologyCreationException, FileNotFoundException, JWNLException {

		Map<String, String> domains = new HashMap<String, String>();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();		
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);

		Set<OWLClass> classes = onto.getClassesInSignature();

		for (OWLClass cls : classes) {
			List<Long> offsetList = findSynsetOffset(cls.getIRI().getFragment().toLowerCase());
			for (Long l : offsetList) {
				String offset = l.toString();
				String domain = findDomain("./files/wndomains/wn-domains-3.2-20070223.txt", offset);
				domains.put(cls.getIRI().getFragment().toLowerCase(), domain);
			}
		}

		//need to remove duplicates
		//Set<String> hs = new HashSet<>();
		//hs.addAll(domains);
		//domains.clear();
		//domains.addAll(hs);

		return domains;

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
		StringBuffer sb = new StringBuffer();
		
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
	
	public static boolean conceptsFromSameDomain(String s1, String s2) throws FileNotFoundException, JWNLException {
				
		Set<String> concept1Tokens = StringUtilities.getWordsAsSetFromCompound(s1);
		Set<String> concept2Tokens = StringUtilities.getWordsAsSetFromCompound(s2);
		
		Set<String> concept1Domains = new HashSet<String>();
		Set<String> concept2Domains = new HashSet<String>();
		
		for (String s : concept1Tokens) {
			concept1Domains.addAll(getDomainTokensFromString(s));
		}
		
		for (String s : concept2Tokens) {
			concept2Domains.addAll(getDomainTokensFromString(s));
		}
		
		if (sameDomain(concept1Domains, concept2Domains)) {
			return true;
		} else {
			return false;
		}
		
		
	}


	/**
	 * Test method
	 * @param args
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 * @throws OWLOntologyCreationException
	 */
	public static void main(String[] args) throws FileNotFoundException, JWNLException, OWLOntologyCreationException{
		WNDomain fileSearch = new WNDomain();
		
		//<https://data.nasa.gov/ontologies/atmonto/ATM#AirportInfrastructureComponent> > <http://www.project-best.eu/owl/airm-mono/airm.owl#Terminal>0.6565596258223504
		
		String concept1 = "AirportInfrastructureComponent";
		String concept2 = "Terminal";
		
		System.out.println("Concepts from same domain?: " + WNDomain.conceptsFromSameDomain(concept1, concept2));
		

		       	String s1 = "airport";
		       	String s2 = "terminal";

		double minJaccard = 0.5;
		
		Set<String> synsetSetS1= getDomainTokensFromString(s1);
		
		System.err.println("Domains associated with " + s1);
		
		for (String word : synsetSetS1) {
			System.err.println(word);
		}
		
		Set<String> synsetSetS2= getDomainTokensFromString(s2);

		System.out.println("--- Offset(s) for " + s1 + " ---");
		for (String s1Offset : synsetSetS1) {
			System.out.println(s1Offset);
		}

		System.out.println("\n --- Offset(s) for " + s2 + "---");
		for (String s2Offset : synsetSetS2) {
			System.out.println(s2Offset);
		}

		System.out.println("\nFrom the same domain?: " + sameDomain(synsetSetS1, synsetSetS2));
		
		System.out.println("\nFrom the same domain (jaccard 0.5) " + sameDomainJaccard(s1, s2, minJaccard ));
		
		
		//<https://data.nasa.gov/ontologies/atmonto/ATM#AirportInfrastructureComponent> > <http://www.project-best.eu/owl/airm-mono/airm.owl#Terminal>0.6565596258223504
		
		Set<String> set1 = new HashSet<String>();
		set1.add("airport");
//		set1.add("infrastructure");
//		set1.add("component");
		
		Set<String> set2 = new HashSet<String>();
		set2.add("terminal");
		
		System.out.println("\nFrom the same domain?: " + sameDomain(set1, set2));
				
		
//		List<Long> s1List = findSynsetOffset(s1);
//		System.out.println("Offsets for " + s1 + ": ");
//		
//		for (Long l : s1List) {
//			System.out.println(l);
//		}
//		
//		List<Long> s2List = findSynsetOffset(s2);
//		System.out.println("Offsets for " + s2 + ": ");
//		for (Long l : s2List) {
//			System.out.println(l);
//		}
		

		/*       	
		File ontoFile = new File("./files/OAEI-16-conference/ontologies/Biblio_2015.rdf");

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();		
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);

		Set<OWLClass> classes = onto.getClassesInSignature();

		//findDomain(String WNDomainsFileName,String searchStr)

		for (OWLClass o : classes) {
			System.out.println("Trying " + Preprocessor.stringTokenize(o.getIRI().getFragment().toLowerCase(), true));
			System.out.println(findDomain("./files/wndomains/wn-domains-3.2-20070223.txt", o.getIRI().getFragment().toLowerCase()));
		}


		for (OWLClass o : classes) {
			List<Long> offset = findSynsetOffset(o.getIRI().getFragment().toLowerCase());

			for (Long l : offset) {
				if (l != null) {
				System.out.println(l);
				} else {
					System.out.println("There is no offset for " + o.getIRI().getFragment().toLowerCase());
				}
			}
		}
		 */

		/*	ArrayList<String> domains = getDomains(ontoFile);

       	System.out.println("Number of domains are " + domains.size());

       	System.out.println("Printing domains for conference.owl");

       	for (String s : domains) {
       		System.out.println(s);
       	}

       	System.out.println("List of domains");
       	Map<String, String> domainMap = listDomains(ontoFile);

       	for (Map.Entry<String, String> e : domainMap.entrySet()) {
       		System.out.println(e);

       	}*/






	}

}