package mismatchdetection;

import java.io.File;
import java.io.FileNotFoundException;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import fr.inrialpes.exmo.align.impl.URIAlignment;

import rita.wordnet.jwnl.JWNLException;

/**
 * Removes mismatches from an alignment using the Concept Scope Mismatch Detection, Structure Mismatch Detection and Domain Mismatch Detection strategies.
 * @author audunvennesland
 *
 */
public class MismatchDetection {
	
	/**
	 * From an initial alignment concept scope-, structure-, and domain mismatches are removed
	 * @param combinedEQAlignment the alignment from which mismatches are filtered out.
	 * @param ontoFile1 the source ontology
	 * @param ontoFile2 the target ontology
	 * @return an URIAlignment where mismatches identified by the mismatch strategies are filtered out.
	 * @throws AlignmentException
	 * @throws OWLOntologyCreationException
	 * @throws FileNotFoundException
	 * @throws JWNLException
	   Jul 18, 2019
	 */
	public static URIAlignment removeMismatches (URIAlignment combinedEQAlignment, File ontoFile1, File ontoFile2) throws AlignmentException, OWLOntologyCreationException, FileNotFoundException, JWNLException {

		URIAlignment conceptScopeMismatchDetection = ConceptScopeMismatch.detectConceptScopeMismatch(combinedEQAlignment);
		URIAlignment structureMismatchDetection = StructureMismatch.detectStructureMismatches(conceptScopeMismatchDetection, ontoFile1, ontoFile2);
		URIAlignment domainMismatchDetection = DomainMismatch.filterAlignment(structureMismatchDetection);

		return domainMismatchDetection;
	}

}
