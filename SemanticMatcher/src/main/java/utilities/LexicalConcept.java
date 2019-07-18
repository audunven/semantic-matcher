package utilities;

import java.net.URI;
import java.util.Set;

/**
 * Represents an object using different lexical properties relevant for lexical matching of ontology concepts.
 * @author audunvennesland
 *
 * @see subsumptionmatching.LexicalSubsumptionMatcher
 */
public class LexicalConcept {
	
	String lexicalConceptName;
	URI uriName;
	Set<String> hyponyms;
	Set<String> glossTokens;
	int wordNetFrequency;
	
	
	public LexicalConcept(String lexicalConceptName, URI uriName, Set<String> hyponyms, Set<String> glossTokens, int wordNetFrequency) {
		super();
		this.lexicalConceptName = lexicalConceptName;
		this.uriName = uriName;
		this.hyponyms = hyponyms;
		this.glossTokens = glossTokens;
		this.wordNetFrequency = wordNetFrequency;
	}
	
	
	public LexicalConcept(String lexicalConceptName, URI uriName, Set<String> hyponyms) {
		super();
		this.lexicalConceptName = lexicalConceptName;
		this.uriName = uriName;
		this.hyponyms = hyponyms;
	}
	
	public LexicalConcept(String lexicalConceptName, URI uriName, Set<String> hyponyms, Set<String> glossTokens) {
		super();
		this.lexicalConceptName = lexicalConceptName;
		this.uriName = uriName;
		this.hyponyms = hyponyms;
		this.glossTokens = glossTokens;
	}
	
	public LexicalConcept(String lexicalConceptName) {
		this.lexicalConceptName = lexicalConceptName;
	}

	public LexicalConcept() {

	}

	public String getLexicalConceptName() {
		return lexicalConceptName;
	}

	public void setLexicalConceptName(String lexicalConceptName) {
		this.lexicalConceptName = lexicalConceptName;
	}
	
	public URI getURIName() {
		return uriName;
	}
	
	public void setURIName(URI uri) {
		this.uriName = uri;
	}

	public Set<String> getHyponyms() {
		return hyponyms;
	}

	public void setHyponyms(Set<String> hyponyms) {
		this.hyponyms = hyponyms;
	}

	public Set<String> getGlossTokens() {
		return glossTokens;
	}

	public void setGlossTokens(Set<String> glossTokens) {
		this.glossTokens = glossTokens;
	}
	
	public int getWordNetFrequency() {
		return this.wordNetFrequency;
	}
	
	public void setWordNetFrequency(int wordNetFrequency) {
		this.wordNetFrequency = wordNetFrequency;
	}
	
//enables the possibility of comparing using "contains" and "containsAll" when a set of LexicalConcepts
    @Override
  public boolean equals(Object v) {
        boolean retVal = false;

        if (v instanceof LexicalConcept){
        	LexicalConcept ptr = (LexicalConcept) v;
            retVal = ptr.lexicalConceptName.equals(this.lexicalConceptName);
        }

     return retVal;
  }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.lexicalConceptName != null ? this.lexicalConceptName.hashCode() : 0);
        return hash;
    }
}
