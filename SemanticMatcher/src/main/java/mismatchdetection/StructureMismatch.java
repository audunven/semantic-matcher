package mismatchdetection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;

import utilities.AlignmentOperations;
import utilities.OntologyOperations;

import rita.wordnet.jwnl.JWNLException;

public class StructureMismatch {
	
	public static void main(String[] args) throws IOException, AlignmentException, OWLOntologyCreationException, JWNLException {

	}
	
	/** 
	 * Iterates through relations in an already produced alignment, retrieves the context (domain and range classes linked to object properties of each pair of related classes), and measures the similarity between these
	 * contexts using Jaccard similarity.
	 * @param inputAlignmentFile the input alignment 
	 * @param onto1 source ontology
	 * @param onto2 target ontology
	 * @return an alignment that includes relations with context similarity above a certain threshold
	 * @throws AlignmentException
	 * @throws OWLOntologyCreationException 
	 * @throws JWNLException 
	 * @throws FileNotFoundException 
	 */
	public static URIAlignment detectStructureMismatches (BasicAlignment inputAlignment, File ontoFile1, File ontoFile2) throws AlignmentException, OWLOntologyCreationException, FileNotFoundException, JWNLException {
		URIAlignment structureMismatchAlignment = new URIAlignment();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
		//need to copy the URIs from the input alignment to the new alignment
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		structureMismatchAlignment.init(onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class);
		
		double sim = 0;


		for (Cell c : inputAlignment) {

			Set<OWLClass> contextC1 = OntologyOperations.getClassesTwoStepsAway(onto1, OntologyOperations.getClass(c.getObject1AsURI().getFragment(), onto1));

			Set<OWLClass> contextC2 = OntologyOperations.getClassesTwoStepsAway(onto2, OntologyOperations.getClass(c.getObject2AsURI().getFragment(), onto2));
						

			sim = simpleSim(contextC1, contextC2);
			if (sim == 0) {
				structureMismatchAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), sim);
			}
			
		}
		
		
		BasicAlignment filteredAlignment = AlignmentOperations.createDiffAlignment(inputAlignment, structureMismatchAlignment);
		
		return (URIAlignment) filteredAlignment;

	}
	
	public static double simpleSim (Set<OWLClass> set1, Set<OWLClass> set2) {

		int counter = 0;
		
		for (OWLClass ci : set1) {
			for (OWLClass cj : set2) {
				if (ci.getIRI().getFragment().equals(cj.getIRI().getFragment())) {
					counter++;
				} else {
				}
			}
		}
		return ((double)counter/10);
		
	}
	


}
