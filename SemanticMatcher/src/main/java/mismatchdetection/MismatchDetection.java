package mismatchdetection;

import java.io.File;
import java.io.FileNotFoundException;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import net.didion.jwnl.JWNLException;

public class MismatchDetection {
	
	
	public static URIAlignment removeMismatches (URIAlignment combinedEQAlignment, File ontoFile1, File ontoFile2) throws AlignmentException, OWLOntologyCreationException, FileNotFoundException, JWNLException {

		URIAlignment conceptScopeMismatchDetection = ConceptScopeMismatch.detectConceptScopeMismatch(combinedEQAlignment);
		URIAlignment structureMismatchDetection = StructureMismatch.detectStructureMismatches(conceptScopeMismatchDetection, ontoFile1, ontoFile2);
		URIAlignment domainMismatchDetection = DomainMismatch.filterAlignment(structureMismatchDetection);

		return domainMismatchDetection;
	}

}
