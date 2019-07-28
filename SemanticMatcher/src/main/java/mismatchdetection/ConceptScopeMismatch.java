package mismatchdetection;

import java.io.IOException;
import java.net.URI;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import utilities.AlignmentOperations;
import utilities.StringUtilities;

public class ConceptScopeMismatch {
	

	
	/** 
	 * Detects "concept scope mismatches" on the basis of the following "compound pattern": the part component of the part-whole relationship includes the name of
	 * its whole as its qualifying compound. For example, an [aircraft]Engine represent a part of aircraft. 
	 * @param inputAlignment an already computed alignment
	 * @return the input alignment - the detected mismatch relations (cells)
	 * @throws AlignmentException
	   Nov 26, 2018
	 * @throws IOException 
	 */

	public static URIAlignment detectConceptScopeMismatch(BasicAlignment inputAlignment) throws AlignmentException {
		URIAlignment conceptScopeMismatchAlignment = new URIAlignment();
		
		//need to copy the URIs from the input alignment to the new alignment
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		conceptScopeMismatchAlignment.init(onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class);
		
		String qualifier = null;
		String compoundHead = null;
		for (Cell c : inputAlignment) {
			if (StringUtilities.isCompoundWord(c.getObject1AsURI().getFragment())) {
				qualifier = StringUtilities.getCompoundFirstQualifier(c.getObject1AsURI().getFragment());
				compoundHead = StringUtilities.getCompoundHead(c.getObject1AsURI().getFragment());
				
				//e.g. [Cloud]Layer - Cloud || Aircraft[Flow]-Flow
				if (qualifier.toLowerCase().equals(c.getObject2AsURI().getFragment().toLowerCase()) || compoundHead.toLowerCase().equals(c.getObject2AsURI().getFragment().toLowerCase())) {
					conceptScopeMismatchAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
				}

			} else if (StringUtilities.isCompoundWord(c.getObject2AsURI().getFragment())) {
				qualifier = StringUtilities.getCompoundFirstQualifier(c.getObject2AsURI().getFragment());
				compoundHead = StringUtilities.getCompoundHead(c.getObject2AsURI().getFragment());
				//e.g. [Sector] || Location-Reference[Location]
				if (qualifier.toLowerCase().equals(c.getObject1AsURI().getFragment().toLowerCase()) || compoundHead.toLowerCase().equals(c.getObject1AsURI().getFragment().toLowerCase())) {
					conceptScopeMismatchAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
				}
			}
		}
				
		BasicAlignment filteredAlignment = AlignmentOperations.createDiffAlignment(inputAlignment, conceptScopeMismatchAlignment);
		
		
		return (URIAlignment) filteredAlignment;

	}

	

}
