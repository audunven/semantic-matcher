package mismatchdetection;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;
import utilities.WNDomain;
import utilities.WordNet;

import rita.wordnet.jwnl.JWNLException;

/**
 * Filters out relations from an alignment where the two concepts do not represent the same domain according to the WordNet Domains classification.
 * @author audunvennesland
 * 8. jan. 2018 
 */
public class DomainMismatch {
	
	public static void main(String[] args) throws AlignmentException, URISyntaxException, FileNotFoundException, JWNLException {
		
		String alignmentFile = "./files/_PHD_EVALUATION/ATMONTO-AIRM/MISMATCHES/initialAlignment.rdf";
		
		AlignmentParser aparser = new AlignmentParser(0);
		BasicAlignment alignment =  (BasicAlignment) aparser.parse(new URI(StringUtilities.convertToFileURL(alignmentFile)));
		
		URIAlignment filteredAlignment = filterAlignment(alignment);
		
		
	}

	/**
	 * This method uses different techniques for filtering out relations from an alignment where the two concepts (likely) does not represent the same domain.
	 * @param inputAlignment the alignment checked for domain dissimilarity.
	 * @return an URIAlignment where relations having concepts not from the same domain are filtered out.
	 * @throws FileNotFoundException
	 * @throws JWNLException
	 * @throws AlignmentException
	   Jul 18, 2019
	 */
	public static URIAlignment filterAlignment(BasicAlignment inputAlignment) throws FileNotFoundException, JWNLException, AlignmentException {

		URIAlignment filteredAlignment = new URIAlignment();	

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();


		filteredAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		String fullWordEntity1 = null;
		String fullWordEntity2 = null;
		String compoundHeadEntity1 = null;
		String compoundHeadEntity2 = null;
		Set<String> wordListEntity1 = null;
		Set<String> wordListEntity2 = null;

		Set<String> domainsEntity1 = new HashSet<String>();
		Set<String> domainsEntity2 = new HashSet<String>();

		//requires that the minimum jaccard similarity of sets of domains is 50 % (that is, half of the domains associated with the two entities have to be equal)
		double minJaccard = 0.30;


		for (Cell c : inputAlignment) {

			fullWordEntity1 = StringUtilities.getCompoundWordWithSpaces(c.getObject1AsURI().getFragment());
			fullWordEntity2 = StringUtilities.getCompoundWordWithSpaces(c.getObject2AsURI().getFragment());

			compoundHeadEntity1 = StringUtilities.getCompoundHead(c.getObject1AsURI().getFragment());
			compoundHeadEntity2 = StringUtilities.getCompoundHead(c.getObject2AsURI().getFragment());

			wordListEntity1 = StringUtilities.getWordsAsSetFromCompound(c.getObject1AsURI().getFragment());
			wordListEntity2 = StringUtilities.getWordsAsSetFromCompound(c.getObject2AsURI().getFragment());

			domainsEntity1 = WNDomain.getDomainsFromString(c.getObject1AsURI().getFragment().toLowerCase());
			domainsEntity2 = WNDomain.getDomainsFromString(c.getObject2AsURI().getFragment().toLowerCase());
			
			
			//*** Operation 1: if both entities are syntactically equal we add them to the alignment without checking with WordNet Domains ***
			if (c.getObject1AsURI().getFragment().equals(c.getObject2AsURI().getFragment())) {
				
				//Need to avoid adding Gtin14Number - Number to the alignment because this is erroneous...
				if (!c.getObject1AsURI().getFragment().equals("Gtin14Number") && !c.getObject2AsURI().getFragment().equals("Number")) {

				filteredAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
			}
			}
						
			//*** Operation 2: match full words without any text processing of the words ***
			if (WordNet.containedInWordNet(c.getObject1AsURI().getFragment().toLowerCase()) 
					&& WordNet.containedInWordNet(c.getObject2AsURI().getFragment().toLowerCase())
					&& WNDomain.sameDomainJaccard(c.getObject1AsURI().getFragment().toLowerCase(), c.getObject2AsURI().getFragment().toLowerCase(), minJaccard)) {
				
				//Tweak for BIBFRAME-SCHEMA.ORG dataset: Need to avoid adding Gtin14Number - Number to the alignment because this is erroneous...
				if (!c.getObject1AsURI().getFragment().equals("Gtin14Number") && !c.getObject2AsURI().getFragment().equals("Number")) {

				filteredAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
				}
			}

			
			//*** Operation 3: match full words with spaces ***
			if (WordNet.containedInWordNet(fullWordEntity1.toLowerCase()) 
					&& WordNet.containedInWordNet(fullWordEntity2.toLowerCase())
					&& WNDomain.sameDomainJaccard(fullWordEntity1.toLowerCase(), fullWordEntity2.toLowerCase(), minJaccard)) {

				//Tweak for BIBFRAME-SCHEMA.ORG dataset: Need to avoid adding Gtin14Number - Number to the alignment because this is erroneous...
				if (!c.getObject1AsURI().getFragment().equals("Gtin14Number") && !c.getObject2AsURI().getFragment().equals("Number")) {
					
				
				filteredAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
			}
		}
			
			
			//Operation 4: match only compound heads
			if (compoundHeadEntity1 != null && compoundHeadEntity2 != null && WordNet.containedInWordNet(compoundHeadEntity1.toLowerCase()) 
					&& WordNet.containedInWordNet(compoundHeadEntity2.toLowerCase())
					&& WNDomain.sameDomainJaccard(compoundHeadEntity1.toLowerCase(), compoundHeadEntity2.toLowerCase(), minJaccard)) {

				//Need to avoid adding Gtin14Number - Number to the alignment because this is erroneous...
				if (!c.getObject1AsURI().getFragment().equals("Gtin14Number") && !c.getObject2AsURI().getFragment().equals("Number")) {
					
				
				filteredAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
				}
			}

						
			//Operation 5: match all individual words in compounds
			//NOTE: StringUtils.getWordsFromCompound was changed from Set<String> to ArrayList<String> and this will impact this.
			if (WNDomain.sameDomainJaccard(wordListEntity1, wordListEntity2, minJaccard)) {
				
				//Tweak for BIBFRAME-SCHEMA.ORG dataset: Need to avoid adding Gtin14Number - Number to the alignment because this is erroneous...
				if (!c.getObject1AsURI().getFragment().equals("Gtin14Number") && !c.getObject2AsURI().getFragment().equals("Number")) {
					
				
				filteredAlignment.addAlignCell(c.getId(), c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
			
				}
			} 
			
			
			else {

				if (!WordNet.containedInWordNet(c.getObject1AsURI().getFragment().toLowerCase()) || !WordNet.containedInWordNet(c.getObject2AsURI().getFragment().toLowerCase())) {
				} 
				else {
				} 

			}
		}	


		return filteredAlignment;

	}

}
