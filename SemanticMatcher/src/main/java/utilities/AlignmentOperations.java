package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.BasicRelation;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

/**
 * @author audunvennesland
 * 19. aug. 2017 
 */
public class AlignmentOperations {
	
	/**
	 * Prints relations from an alignment with additional details related to each concept in the relation: definitions, subclasses and superclasses. Primarily used
	 * to analyse an alignment w.r.t tuning matchers.
	 * @param referenceAlignmentPath the path to the reference alignment being analysed.
	 * @param sourceOntologyPath path to the source ontology (owl) file
	 * @param targetOntologyPath path to the target ontology (owl) file
	 * @throws AlignmentException
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	   Feb 26, 2019
	 */
	public static void printExtendedReferenceAlignment (String referenceAlignmentPath, String sourceOntologyPath, String targetOntologyPath) throws AlignmentException, OWLOntologyCreationException, IOException {
		AlignmentParser parser = new AlignmentParser();		
		URIAlignment referenceAlignment = (URIAlignment) parser.parse(new File(referenceAlignmentPath).toURI().toString());
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(new File(sourceOntologyPath));
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(new File(targetOntologyPath));
		
		Map<String, Set<String>> classesAndDefinitionsSourceOntology = OntologyOperations.createClassAndDefMap(sourceOntology);
		Map<String, Set<String>> classesAndDefinitionsTargetOntology = OntologyOperations.createClassAndDefMap(targetOntology);
		
		for (Cell c : referenceAlignment) {
						
			OWLClass sourceCls = OntologyOperations.getClass(c.getObject1AsURI().getFragment(), sourceOntology);
			OWLClass targetCls = OntologyOperations.getClass(c.getObject2AsURI().getFragment(), targetOntology);
			
			Set<String> sourceClsSuperclasses = OntologyOperations.getEntitySuperclassesFragments(sourceOntology, sourceCls);
			Set<String> sourceClsSubclasses = OntologyOperations.getEntitySubclassesFragments(sourceOntology, sourceCls);
			Set<String> targetClsSuperclasses = OntologyOperations.getEntitySuperclassesFragments(targetOntology, targetCls);		
			Set<String> targetClsSubclasses = OntologyOperations.getEntitySubclassesFragments(targetOntology, targetCls);
			
			StringBuffer sourceDef = new StringBuffer();
			StringBuffer targetDef = new StringBuffer();
			StringBuffer sourceSupers = new StringBuffer();
			StringBuffer sourceSubs = new StringBuffer();
			StringBuffer targetSupers = new StringBuffer();
			StringBuffer targetSubs = new StringBuffer();

			
			System.out.println("\n" + c.getObject1AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getObject2AsURI().getFragment());
			System.out.println("Concept 1: " + c.getObject1AsURI().getFragment());

			if (classesAndDefinitionsSourceOntology.containsKey(c.getObject1AsURI().getFragment().toLowerCase())) {
				for (String s : classesAndDefinitionsSourceOntology.get(c.getObject1AsURI().getFragment().toLowerCase()))
				sourceDef.append(s);
			}
			System.out.println("Concept 1 Definition: " + sourceDef.toString());
			
			for (String sourceSuper : sourceClsSuperclasses) {
				sourceSupers.append(sourceSuper + " ");
			}
			
			System.out.println("Concept 1 Superclasses: " + sourceSupers.toString());
			
			for (String sourceSub : sourceClsSubclasses) {
				sourceSubs.append(sourceSub + " ");
			}
			
			System.out.println("Concept 1 Subclasses: " + sourceSubs.toString());
			
			
			System.out.println("Concept 2: " + c.getObject2AsURI().getFragment());
			
			if (classesAndDefinitionsTargetOntology.containsKey(c.getObject2AsURI().getFragment().toLowerCase())) {
				for (String t : classesAndDefinitionsTargetOntology.get(c.getObject2AsURI().getFragment().toLowerCase()) )
				targetDef.append(t);
			}
			
			System.out.println("Concept 2 Definition: " + targetDef.toString());
			
			if (targetClsSuperclasses != null && !targetClsSuperclasses.isEmpty())
			for (String targetSuper : targetClsSuperclasses) {
				targetSupers.append(targetSuper + " ");
			}
			
			System.out.println("Concept 2 Superclasses: " + targetSupers.toString());
			
			for (String targetSub : targetClsSubclasses) {
				targetSubs.append(targetSub + " ");
			}
			
			System.out.println("Concept 2 Subclasses: " + targetSubs.toString());
		}

		
	}
	
	/**
	 * Prints relations from an alignment with additional details related to each concept in the relation: definitions, subclasses and superclasses. Primarily used
	 * to analyse an alignment w.r.t tuning matchers.
	 * @param referenceAlignmentPath the path to the reference alignment being analysed.
	 * @param sourceOntologyPath path to the source ontology (owl) file
	 * @param targetOntologyPath path to the target ontology (owl) file
	 * @throws AlignmentException
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	   Feb 26, 2019
	 */
	public static void printExtendedReferenceAlignmentCSV (String referenceAlignmentPath, String sourceOntologyPath, String targetOntologyPath, String output) throws AlignmentException, OWLOntologyCreationException, IOException {
		AlignmentParser parser = new AlignmentParser();		
		URIAlignment referenceAlignment = (URIAlignment) parser.parse(new File(referenceAlignmentPath).toURI().toString());
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(new File(sourceOntologyPath));
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(new File(targetOntologyPath));
		
		Map<String, Set<String>> classesAndDefinitionsSourceOntology = OntologyOperations.createClassAndDefMap(sourceOntology);
		Map<String, Set<String>> classesAndDefinitionsTargetOntology = OntologyOperations.createClassAndDefMap(targetOntology);
		
		Map<String, Set<String>> rangeMapSourceOntology = getRangeMap(sourceOntology);
		Map<String, Set<String>> rangeMapTargetOntology = getRangeMap(targetOntology);
		
		File outputFile = new File(output);
		
		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputFile)), true); 
		
		writer.println("Relation,Concept,Definition,Subclasses,Superclasses,Object Properties, Data Properties, Range");
		
		for (Cell c : referenceAlignment) {
			
			System.out.println(c.getObject1AsURI() + " " + c.getObject2AsURI());
						
			OWLClass sourceCls = OntologyOperations.getClass(c.getObject1AsURI().getFragment(), sourceOntology);
			OWLClass targetCls = OntologyOperations.getClass(c.getObject2AsURI().getFragment(), targetOntology);
			
			Set<String> sourceClsSuperclasses = OntologyOperations.getEntitySuperclassesFragments(sourceOntology, sourceCls);
			Set<String> sourceClsSubclasses = OntologyOperations.getEntitySubclassesFragments(sourceOntology, sourceCls);
			Set<String> targetClsSuperclasses = OntologyOperations.getEntitySuperclassesFragments(targetOntology, targetCls);		
			Set<String> targetClsSubclasses = OntologyOperations.getEntitySubclassesFragments(targetOntology, targetCls);
			
			Set<String> sourceRangeSet = rangeMapSourceOntology.get(c.getObject1AsURI().getFragment().toLowerCase());
			Set<String> targetRangeSet = rangeMapTargetOntology.get(c.getObject2AsURI().getFragment().toLowerCase());
			
			Set<OWLObjectProperty> sourceObjectProperties = OntologyOperations.getObjectProperties(sourceOntology, sourceCls);
			Set<OWLDataProperty> sourceDataProperties = OntologyOperations.getDataProperties(sourceOntology, sourceCls);
			
			Set<OWLObjectProperty> targetObjectProperties = OntologyOperations.getObjectProperties(targetOntology, targetCls);
			Set<OWLDataProperty> targetDataProperties = OntologyOperations.getDataProperties(targetOntology, targetCls);
			
			StringBuffer sourceDef = new StringBuffer();
			StringBuffer targetDef = new StringBuffer();
			StringBuffer sourceSupers = new StringBuffer();
			StringBuffer sourceSubs = new StringBuffer();
			StringBuffer targetSupers = new StringBuffer();
			StringBuffer targetSubs = new StringBuffer();
			StringBuffer sourceRanges = new StringBuffer();
			StringBuffer targetRanges = new StringBuffer();
			StringBuffer sourceOPs = new StringBuffer();
			StringBuffer sourceDPs = new StringBuffer();
			StringBuffer targetOPs = new StringBuffer();
			StringBuffer targetDPs = new StringBuffer();
			
			String relation = c.getObject1AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getObject2AsURI().getFragment();
			
			
			//writer.println(c.getObject1AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getObject2AsURI().getFragment());

			if (classesAndDefinitionsSourceOntology.containsKey(c.getObject1AsURI().getFragment().toLowerCase())) {
				for (String s : classesAndDefinitionsSourceOntology.get(c.getObject1AsURI().getFragment().toLowerCase()))
				sourceDef.append(s);
			}

			for (String sourceSuper : sourceClsSuperclasses) {
				sourceSupers.append(sourceSuper + " ");
			}
			
			for (String sourceSub : sourceClsSubclasses) {
				sourceSubs.append(sourceSub + " ");
			}
			
			for (OWLObjectProperty sourceOP : sourceObjectProperties) {
				sourceOPs.append(sourceOP.getIRI().getFragment() + " ");
			}
			
			for (OWLDataProperty sourceDP : sourceDataProperties) {
				sourceDPs.append(sourceDP.getIRI().getFragment() + " ");
			}
			
			for (String sourceRange : sourceRangeSet) {
				sourceRanges.append(sourceRange + " ");
			}
			
			writer.println(relation + ","+ c.getObject1AsURI().getFragment() + "," + sourceDef.toString() + "," + sourceSubs.toString() + "," + sourceSupers + "," + sourceOPs.toString() + "," + sourceDPs.toString() + "," + sourceRanges.toString());

			
			if (classesAndDefinitionsTargetOntology.containsKey(c.getObject2AsURI().getFragment().toLowerCase())) {
				for (String t : classesAndDefinitionsTargetOntology.get(c.getObject2AsURI().getFragment().toLowerCase()) )
				targetDef.append(t);
			}
			
			if (targetClsSuperclasses != null && !targetClsSuperclasses.isEmpty())
			for (String targetSuper : targetClsSuperclasses) {
				targetSupers.append(targetSuper + " ");
			}

			for (String targetSub : targetClsSubclasses) {
				targetSubs.append(targetSub + " ");
			}
			
			for (String targetRange : targetRangeSet) {
				targetRanges.append(targetRange + " ");
			}

			for (OWLObjectProperty targetOP : targetObjectProperties) {
				targetOPs.append(targetOP.getIRI().getFragment() + " ");
			}
			

			for (OWLDataProperty targetDP : targetDataProperties) {
				targetDPs.append(targetDP.getIRI().getFragment() + " ");
			}

			writer.println(relation + "," + c.getObject2AsURI().getFragment() + "," + targetDef.toString() + "," + targetSubs.toString() + "," + targetSupers + "," + targetOPs.toString() + "," + targetDPs.toString() + "," + targetRanges.toString());
			writer.println(" ---- ");
		}

		
	}
	
	public static Map<String, Set<String>> getRangeMap(OWLOntology onto) {
		Map<String, Set<String>> rangeMap = new HashMap<String, Set<String>>();


		for (OWLClass c : onto.getClassesInSignature()) {
			Set<OWLObjectProperty> ops = new HashSet<OWLObjectProperty>();
			
			for (OWLObjectPropertyDomainAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
				
				if (op.getDomain().equals(c)) {
					for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
						ops.add(oop);
					}
				}
			}

			Set<String> range = new HashSet<String>();
			
			//get the range classes from the object properties 
			for (OWLObjectProperty oop : ops) {
				Set<OWLClassExpression> rangeCls = oop.getRanges(onto);
				for (OWLClassExpression oce : rangeCls) {
					if (!oce.isAnonymous()) {
						range.add(oce.asOWLClass().getIRI().getFragment());
					}
				}
			}
			rangeMap.put(c.getIRI().getFragment().toLowerCase(), range);
		}

		return rangeMap;
	}


	
	
	
	public static URIAlignment removeZeroConfidenceRelations(BasicAlignment inputAlignment) throws AlignmentException {
	
		URIAlignment alignmentWithNonZeroRelations = new URIAlignment();
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		
		alignmentWithNonZeroRelations.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		
		for (Cell c : inputAlignment) {
			if (c.getStrength() != 0.0) {
				
				alignmentWithNonZeroRelations.addAlignCell(c.getId(), c.getObject1AsURI(), c.getObject2AsURI(), c.getRelation(), c.getStrength());
				
			} 
		}
		
		return alignmentWithNonZeroRelations;

	}
	
	
	
	
//	public static void sortAlignment(URIAlignment alignment) throws AlignmentException {
//		System.out.println("\nPrinting unsorted alignment");
//		for (Cell c : alignment) {
//			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " +c.getRelation().getRelation() + ": " + c.getStrength());
//		}
//	
//		Map<Cell, Double> alignmentMap = new HashMap<Cell, Double>();
//		
//		for (Cell c : alignment) {
//			alignmentMap.put(c, c.getStrength());
//		}
//		
//		Map<Cell, Double> sortedAlignmentMap = sortByValues(alignmentMap);
//		
//		System.out.println("\nPrinting sorted alignment");
//		for (Entry<Cell, Double> e : sortedAlignmentMap.entrySet()) {
//		System.out.println(e.getKey().getObject1AsURI().getFragment() + " - " + e.getKey().getObject2AsURI().getFragment() + " - " + e.getKey().getRelation().getRelation() + ": " + e.getValue());
//		}
//	}
	
	public static List<Cell> sortAlignment(URIAlignment alignment) throws AlignmentException {

		Map<Cell, Double> alignmentMap = new HashMap<Cell, Double>();
		
		for (Cell c : alignment) {
			alignmentMap.put(c, c.getStrength());
		}
		
		Map<Cell, Double> sortedAlignmentMap = sortByValues(alignmentMap);
		
		List<Cell> sortedList = new LinkedList<Cell>();

		for (Entry<Cell, Double> e : sortedAlignmentMap.entrySet()) {
			sortedList.add(e.getKey());
		}
		
		
		return sortedList;
		
		
	}
	
	/**
	 * Combines the relations from all alignments in a folder into a single alignment, basically the union. 
	 * @param folderName
	 * @return Alignment holding all relations from a set of individual alignments
	 * @throws AlignmentException
	   Feb 12, 2019
	 */
	public static URIAlignment combineEQAndSUBAlignments(String eqAlignmentPath, String subAlignmentPath) throws AlignmentException {
		URIAlignment combinedAlignment = new URIAlignment();

		
		File eqAlignmentFile = new File(eqAlignmentPath);
		File subAlignmentFile = new File(subAlignmentPath);
		
		AlignmentParser parser = new AlignmentParser();
		
		URIAlignment eqAlignment = (URIAlignment) parser.parse(eqAlignmentFile.toURI().toString());
		URIAlignment subAlignment = (URIAlignment) parser.parse(subAlignmentFile.toURI().toString());
		
		for (Cell eqCell : eqAlignment) {
			combinedAlignment.addAlignCell(eqCell.getObject1(), eqCell.getObject2(),  eqCell.getRelation().getRelation(), eqCell.getStrength());
		}
		
		for (Cell subCell : subAlignment) {
			if (subCell.getRelation().toString().equals("<")) {
				BasicRelation subBy = new BasicRelation( "<");
				combinedAlignment.addAlignCell(subCell.getObject1(), subCell.getObject2(),  subBy.toString(), subCell.getStrength());
			} else {
			combinedAlignment.addAlignCell(subCell.getObject1(), subCell.getObject2(),  subCell.getRelation().getRelation(), subCell.getStrength());
			}
		}
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = eqAlignment.getOntology1URI();
		URI onto2URI = eqAlignment.getOntology2URI();
		
		combinedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		//normalize confidences between [0..1]
		normalizeConfidence(combinedAlignment);
		
		System.out.println("The combined alignment contains " + combinedAlignment.nbCells() + " relations");
		
		return combinedAlignment;
	}
	
	/**
	 * Combines the relations from all alignments in a folder into a single alignment, basically the union. 
	 * @param folderName
	 * @return Alignment holding all relations from a set of individual alignments
	 * @throws AlignmentException
	   Feb 12, 2019
	 */
	public static URIAlignment combineEQAndSUBAlignments(URIAlignment eqAlignment, URIAlignment subAlignment) throws AlignmentException {
		URIAlignment combinedAlignment = new URIAlignment();

		
		for (Cell eqCell : eqAlignment) {
			combinedAlignment.addAlignCell(eqCell.getId(), eqCell.getObject1(), eqCell.getObject2(),  eqCell.getRelation().getRelation(), eqCell.getStrength());
		}
		
		for (Cell subCell : subAlignment) {
			//need to transform the less-than symbol '<' to &lt;
			if (subCell.getRelation().toString().equals("<")) {
				BasicRelation subBy = new BasicRelation("<");
				combinedAlignment.addAlignCell(subCell.getId(), subCell.getObject1(), subCell.getObject2(),  subBy.toString(), subCell.getStrength());
			} else {
			combinedAlignment.addAlignCell(subCell.getId(), subCell.getObject1(), subCell.getObject2(),  subCell.getRelation().getRelation(), subCell.getStrength());
			}
		}
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = eqAlignment.getOntology1URI();
		URI onto2URI = eqAlignment.getOntology2URI();
		
		combinedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		//normalize confidences between [0..1]
		//normalizeConfidence(combinedAlignment);
		
		//System.out.println("The combined alignment contains " + combinedAlignment.nbCells() + " relations");
		
		return combinedAlignment;
	}
	
	/**
	 * Combines the relations from all alignments in a folder into a single alignment, basically the union. 
	 * @param folderName
	 * @return Alignment holding all relations from a set of individual alignments
	 * @throws AlignmentException
	   Feb 12, 2019
	 */
	public static URIAlignment combineAlignments(String folderName) throws AlignmentException {
		URIAlignment combinedAlignment = new URIAlignment();
		
		File folder = new File(folderName);
		File[] filesInDir = folder.listFiles();
		
		URIAlignment alignment = null;
		File alignmentFile = null;
		
		AlignmentParser parser = new AlignmentParser();
		
		for (int i = 0; i < filesInDir.length; i++) {			
			alignmentFile = filesInDir[i];
			
			alignment = (URIAlignment) parser.parse(alignmentFile.toURI().toString());
			
			System.out.println("Processing file " + alignmentFile.getPath() + " (" + alignment.nbCells() + " relations)");
			
			for (Cell c : alignment) {

				combinedAlignment.addAlignCell(c.getObject1(), c.getObject2(),  c.getRelation().getRelation(), c.getStrength());
				
			}

		}
		
		System.out.println("The combined alignment contains " + combinedAlignment.nbCells() + " relations");
		
		//normalize confidences between [0..1]
		normalizeConfidence(combinedAlignment);
		
		return combinedAlignment;
	}
	
	/**
	 * Extracts all subsumption relations from an alignment file
	 * @param inputAlignmentFile the input alignment from which subsumption relations will be extracted
	 * @param output path to which the created subsumption alignment will be stored
	 * @throws AlignmentException
	   Jan 5, 2019
	 * @throws IOException 
	 */
	public static void extractSubsumptionRelations(File inputAlignmentFile, String output) throws AlignmentException, IOException {
		URIAlignment subsumptionAlignment = new URIAlignment();
		
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());
		
		System.err.println("The input alignment contains " + inputAlignment.nbCells() + " correspondences");
		
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		System.out.println("The URIs are: ");
		System.out.println("onto1URI: " + onto1URI);
		System.out.println("onto2URI: " + onto2URI);
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		subsumptionAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		for (Cell c : inputAlignment) {
			if (!c.getRelation().getRelation().equals("=")) {
				subsumptionAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
			}
		}
		
		File outputAlignment = new File(output);

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		subsumptionAlignment.render(renderer);
		
		System.err.println("The subsumption alignment contains " + subsumptionAlignment.nbCells() + " correspondences");
		writer.flush();
		writer.close();
		
	}
	
	/**
	 * Extracts all subsumption relations from an alignment file
	 * @param inputAlignmentFile the input alignment from which subsumption relations will be extracted
	 * @param output path to which the created subsumption alignment will be stored
	 * @throws AlignmentException
	   Jan 5, 2019
	 * @throws IOException 
	 */
	public static URIAlignment extractSubsumptionRelations(URIAlignment inputAlignment) throws AlignmentException, IOException {
		URIAlignment subsumptionAlignment = new URIAlignment();
				
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		subsumptionAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		for (Cell c : inputAlignment) {
			if (!c.getRelation().getRelation().equals("=")) {
				subsumptionAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
			}
		}
		
		return subsumptionAlignment;
		
	}
	
	/**
	 * Extracts all equivalence relations from an alignment file
	 * @param inputAlignmentFile the input alignment from which equivalence relations will be extracted
	 * @param output path to which the created equivalence alignment will be stored
	 * @throws AlignmentException
	   Jan 5, 2019
	 * @throws IOException 
	 */
	public static void extractEquivalenceRelations(File inputAlignmentFile, String output) throws AlignmentException, IOException {
		URIAlignment equivalenceAlignment = new URIAlignment();
		
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());
		
		System.err.println("The input alignment contains " + inputAlignment.nbCells() + " correspondences");
		
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		equivalenceAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		for (Cell c : inputAlignment) {
			if (c.getRelation().getRelation().equals("=")) {
				equivalenceAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
			}
		}
		
		File outputAlignment = new File(output);

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		equivalenceAlignment.render(renderer);
		
		System.err.println("The equivalence alignment contains " + equivalenceAlignment.nbCells() + " correspondences");
		writer.flush();
		writer.close();
		
	}
	
	/**
	 * Extracts all equivalence relations from an alignment
	 * @param inputAlignmentFile the input alignment from which equivalence relations will be extracted
	 * @param output path to which the created equivalence alignment will be stored
	 * @throws AlignmentException
	   Jan 5, 2019
	 * @throws IOException 
	 */
	public static URIAlignment extractEquivalenceRelations(URIAlignment inputAlignment) throws AlignmentException, IOException {
		
		URIAlignment equivalenceAlignment = new URIAlignment();
		
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		equivalenceAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		for (Cell c : inputAlignment) {
			if (c.getRelation().getRelation().equals("=") || c.getRelation().equals("=")) {
				equivalenceAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
				System.out.println("Extracting " + c.getObject1() + " " + c.getObject2() + " " +  c.getRelation().getRelation() + " " +  c.getStrength());
			} 
		}
		
		return equivalenceAlignment;
	
	}
	
	/**
	 * Cuts an alignment based on a confidence threshold and returns the cut alignment
	 * @param originalAlignmentFile the original alignment file that will be cut to threshold
	 * @param threshold the threshold used to cut the alignment 
	 * @param output path to where the new alignment will be stored
	 * @return a BasicAlignment with entities in the correct order
	 * @throws AlignmentException
	 */
	public static void cutAlignment (File originalAlignmentFile, double threshold, String output) throws FileNotFoundException, IOException, AlignmentException {
		File cutAlignmentFile = new File(output);
		
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment originalAlignment = (BasicAlignment) parser.parse(originalAlignmentFile.toURI().toString());
		
		System.err.println("The original alignment contains " + originalAlignment.nbCells() + " correspondences");

		BasicAlignment cutAlignment = new URIAlignment();	
		
		BasicAlignment StringAlignment = (BasicAlignment)(originalAlignment.clone());

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = originalAlignment.getOntology1URI();
		URI onto2URI = originalAlignment.getOntology2URI();

		cutAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		StringAlignment.cut(threshold);
		
		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(cutAlignmentFile)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		StringAlignment.render(renderer);
		
		System.err.println("The new alignment contains " + StringAlignment.nbCells() + " correspondences");
		writer.flush();
		writer.close();


	}
	
	/**
	 * Converts an alignment generated by COMA to Alignment API format. The input file is formatted like this: [Class1] [Class2] = [Confidence]
	 * @param comaTextFilePath
	 * @param onto1
	 * @param onto2
	 * @return
	 * @throws AlignmentException
	 * @throws FileNotFoundException
	 * @throws IOException
	   Dec 17, 2018
	 */
	public static URIAlignment convertFromComaToAlignmentAPI (String comaTextFilePath, OWLOntology onto1, OWLOntology onto2) throws AlignmentException, FileNotFoundException, IOException {
		
		URIAlignment convertedAlignment = new URIAlignment();
		URI onto1URI = onto1.getOntologyID().getOntologyIRI().toURI();
		URI onto2URI = onto2.getOntologyID().getOntologyIRI().toURI();
		
		System.out.println("onto1URI: " + onto1URI);
		System.out.println("onto2URI: " + onto2URI);
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		convertedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		File comaTextFile = new File(comaTextFilePath);
		
		try (BufferedReader br = new BufferedReader(new FileReader(comaTextFile))) {
		    String line;
		    while ((line = br.readLine()) != null) {

		    	String[] lineArray = line.split(" ");
		    	convertedAlignment.addAlignCell(URI.create(onto1URI + "#" + lineArray[0]), URI.create(onto2URI + "#" + lineArray[1]), "=", Double.valueOf(lineArray[2]));

		    }
		}

		return convertedAlignment;
		
	}
	
	/**
	 * Simply prints the ontology URIs of an input alignment
	 * @param inputAlignmentFile
	 * @throws AlignmentException
	   Feb 12, 2019
	 */
	public static void printAlignmentURIs (File inputAlignmentFile) throws AlignmentException {
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());
		System.out.println("Printing URIs for ontology 1: ");
		for (Cell c : inputAlignment) {
			System.out.println(c.getObject1AsURI().getSchemeSpecificPart());
		}

	}
	
	/**
	 * Post-processes an alignment file and removes relations where the AIRM concept contains underscores (is a package class from UML) 
	 * or is really a boolean association (represented by concept names containing -is or -has), or has 0.0 as confidence (strength)
	 * @param inputAlignmentFile
	 * @param outputFile an alignment file without AIRM concepts representing packaging classes or boolean associations
	 * @throws AlignmentException
	 * @throws IOException
	   Jan 7, 2019
	 */
	public static void cleanAlignment(File inputAlignmentFile, String outputFile) throws AlignmentException, IOException {

		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());
		URIAlignment cleanedAlignment = new URIAlignment();
		
		System.err.println("The input alignment contains " + inputAlignment.nbCells() + " correspondences");
		
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		cleanedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		for (Cell c : inputAlignment) {
			if (!c.getObject1AsURI().getFragment().contains("_") && !c.getObject2AsURI().getFragment().contains("_") 
					&& !c.getObject1AsURI().getFragment().contains("-is") && !c.getObject2AsURI().getFragment().contains("-is") 
					&& !c.getObject1AsURI().getFragment().contains("-has") && !c.getObject2AsURI().getFragment().contains("-has")
					&& c.getStrength() != 0.0) {
				cleanedAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
				//System.out.println("Adding " + c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " to cleaned alignment");
			} else {
				//System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " contains underscores and must be removed");
			}
		}
		
		File outputAlignment = new File(outputFile);

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		cleanedAlignment.render(renderer);
		
		System.err.println("The Cleaned alignment contains " + cleanedAlignment.nbCells() + " correspondences");
		writer.flush();
		writer.close();
		
		
	}

	/**
	 * This method converts an alignment in the Alignment API format to a format that is accepted by the STROMA enrichment system. Basically the path (superclasses) to each object in a cell is added as prefix to the object,
	 * and <-> is used to separate the object paths (meaning = in this case) and after : comes the confidence value.
	 * @param inputAlignmentFile the original alignment file in Alignment Format
	 * @param ontoFile1
	 * @param ontoFile2
	 * @throws AlignmentException
	 * @throws OWLOntologyCreationException
	   26. okt. 2018
	 */
	public static void convertToComaFormat(File inputAlignmentFile, File ontoFile1, File ontoFile2) throws AlignmentException, OWLOntologyCreationException {

		AlignmentParser parser = new AlignmentParser();
		BasicAlignment inputAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		Map<String, Set<String>> super1 = OntologyOperations.getSuperclasses(onto1);
		System.out.println("Printing superclasses in ATMONTO");
		for (Entry<String, Set<String>> e : super1.entrySet()) {
			System.out.println("Class: " + e.getKey());
			for (String s : e.getValue()) {
				System.out.println("Superclass: " + s);
			}
		}
		
		Map<String, Set<String>> super2 = OntologyOperations.getSuperclasses(onto2);

		for (Cell c : inputAlignment) {
			Set<String> superclasses1 = super1.get(c.getObject1AsURI().toString());
			Set<String> superclasses2 = super2.get(c.getObject2AsURI().toString());

			StringBuffer path1 = null;
			StringBuffer path2 = null;
			String fullPath1 = null;
			String fullPath2 = null;
			
			//if object1 has superclasses -> append them all to path1
			if (superclasses1 != null) {
				
				path1 = new StringBuffer();
				fullPath1 = new String();
				for (String s : superclasses1) {
					path1.append(StringUtilities.getString(s) + ".");
				}
				fullPath1 = path1.toString() + c.getObject1AsURI().getFragment();
			} else {
				fullPath1 = c.getObject1AsURI().getFragment();
			}

			if (superclasses2 != null) {
				
				path2 = new StringBuffer();
				fullPath2 = new String();
			for (String t : superclasses2) {
				path2.append(StringUtilities.getString(t) + ".");
			}
			fullPath2 = path2.toString() + c.getObject2AsURI().getFragment();
			} else {
				fullPath2 = c.getObject2AsURI().getFragment();
			}

			System.out.println("- " + fullPath1 + " <-> " + fullPath2 +  ": " + c.getStrength());
		}
	}
	
	

	/**
	 * Ensures that the entities in a cell are represented in the same order as the ontology URIs
	 * @param inputAlignment
	 * @return a BasicAlignment with entities in the correct order
	 * @throws AlignmentException
	 */
	public static URIAlignment fixEntityOrder (BasicAlignment inputAlignment) throws AlignmentException {
		URIAlignment newReferenceAlignment = new URIAlignment();

		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();

		URI entity1 = null;
		URI entity2 = null;
		String relation = null;
		double threshold = 1.0;

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		newReferenceAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		for (Cell c : inputAlignment) {
			if (c.getObject1AsURI().toString().contains(onto1URI.toString())) {
				entity1 = c.getObject1AsURI();
				entity2 = c.getObject2AsURI();
				relation = c.getRelation().getRelation();
				newReferenceAlignment.addAlignCell(entity1, entity2, relation, threshold);

			} else if (c.getObject2().toString().contains(onto1URI.toString())) {
				System.out.println(c.getObject2AsURI());
				entity1 = c.getObject2AsURI();
				entity2 = c.getObject1AsURI();
				relation = c.getRelation().getRelation();

				if (relation.equals(">")) {
					relation = "<";
				} else if (relation.equals("<")) {
					relation = ">";
				} else {
					relation = "=";
				}

				newReferenceAlignment.addAlignCell(entity1, entity2, relation, threshold);

			}

		}


		return newReferenceAlignment;
	}

	/**
	 * This method normalizes (or scales) the confidence given to the relations between [0..1]. 
	 * @param initialAlignment
	 * @throws AlignmentException
	 */
	public static void normalizeConfidence (BasicAlignment initialAlignment) throws AlignmentException {

		URI onto1URI = initialAlignment.getOntology1URI();
		URI onto2URI = initialAlignment.getOntology2URI();

		BasicAlignment newAlignment = new BasicAlignment();


		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		newAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		Set<Double> confidenceValues = new HashSet<Double>();
		for (Cell c : initialAlignment) {
			confidenceValues.add(c.getStrength());
		}

		//get min value in dataset (A)
		double min = Collections.min(confidenceValues);

		//get max value in dataset (B)
		double max = Collections.max(confidenceValues);

		//set min value (a) in the normalized scale (i.e. 0)
		double normMin = 0;

		//set max value (b) in the normalized scale (i.e. 1.0)
		double normMax = 1.0;

		//calculate the normalized value for all entities (x) in the dataset
		//a + (x-A)(b-a) / (B-a)

		double thisConfidence = 0;		

		for (Cell cell : initialAlignment) {

			thisConfidence = normMin + (cell.getStrength()-min)*(normMax-normMin) / (max-normMin);
			cell.setStrength(thisConfidence);
			//newAlignment.addAlignCell(cell.getObject1(), cell.getObject2(), cell.getRelation().getRelation(), thisConfidence);

		}

		//return newAlignment;


	}

	/**
	 * Creates a subsumption reference alignment based on logical entailment from an equivalence reference alignment as input parameter.
	 * @param equivalenceReferenceAlignment the alignment holding equivalence relations from which subsumption relations will be entailed.
	 * @return a subsumption alignment created from sub- and superclasses of equivalent concepts
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	public static BasicAlignment createSubsumptionReferenceAlignment(BasicAlignment equivalenceReferenceAlignment) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

		BasicAlignment subsumptionReferenceAlignment = new URIAlignment();


		URI onto1URI = equivalenceReferenceAlignment.getOntology1URI();
		URI onto2URI = equivalenceReferenceAlignment.getOntology2URI();

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		subsumptionReferenceAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		File ontoFile1 = new File(equivalenceReferenceAlignment.getFile1().getRawPath());
		File ontoFile2 = new File(equivalenceReferenceAlignment.getFile2().getRawPath());

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		//get the ontologies from the alignment file
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		Map<String, Set<String>> onto1ClassesAndSubclasses = OntologyOperations.getSubclasses(onto1);
		Map<String, Set<String>> onto2ClassesAndSubclasses = OntologyOperations.getSubclasses(onto2);
		Map<String, Set<String>> onto1ClassesAndSuperclasses = OntologyOperations.getDirectSuperclasses(onto1);
		Map<String, Set<String>> onto2ClassesAndSuperclasses = OntologyOperations.getDirectSuperclasses(onto2);


		//for each cell in the alignment
		//get all subclasses of e1 and make them subsumed by e2			
		Set<String> subclasses = null;
		for (Cell c : equivalenceReferenceAlignment) {

			if (onto1ClassesAndSubclasses.containsKey(c.getObject1().toString())) {

				subclasses = onto1ClassesAndSubclasses.get(c.getObject1().toString());


				for (String sc : subclasses) {
					subsumptionReferenceAlignment.addAlignCell(new URI(sc), c.getObject2AsURI(), "<", 1.0);
					//print justification for entailed subsumption relation
					System.out.println(StringUtilities.getString(sc) + " < " + c.getObject2AsURI().getFragment() + " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

				}

				//then get all subclasses of e2 and make them subsumed by e1
			} if (onto2ClassesAndSubclasses.containsKey(c.getObject2().toString())) {

				subclasses = onto2ClassesAndSubclasses.get(c.getObject2().toString());

				for (String sc : subclasses) {
					subsumptionReferenceAlignment.addAlignCell(c.getObject1AsURI(), new URI(sc), ">", 1.0);
					//print justification for entailed subsumption relation
					System.out.println(c.getObject2AsURI().getFragment() + " > " + StringUtilities.getString(sc)+  " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

				}

			}
		}

		//for each cell in the alignment
		//get all superclasses of e1 and make them subsume e2
		//then get all subclasses of e2 and make them subsume e1
		Set<String> superclasses = null;
		for (Cell c : equivalenceReferenceAlignment) {

			if (onto1ClassesAndSuperclasses.containsKey(c.getObject1().toString())) {
				superclasses = onto1ClassesAndSuperclasses.get(c.getObject1().toString());

				for (String sc : superclasses) {
					subsumptionReferenceAlignment.addAlignCell(new URI(sc), c.getObject2AsURI(), ">", 1.0);
					//print justification for entailed subsumption relation
					System.out.println(StringUtilities.getString(sc) + " > " + c.getObject2AsURI().getFragment() + " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

				}
			} if (onto2ClassesAndSuperclasses.containsKey(c.getObject2().toString())) {
				superclasses = onto2ClassesAndSuperclasses.get(c.getObject2().toString());

				for (String sc : superclasses) {
					subsumptionReferenceAlignment.addAlignCell(c.getObject1AsURI(), new URI(sc), "<", 1.0);
					//print justification for entailed subsumption relation
					System.out.println(c.getObject1AsURI().getFragment() + " < " + StringUtilities.getString(sc) +  " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

					//System.out.println("Adding " + sc + " as superclass to " + c.getObject1AsURI());
				}
			}
		}


		return subsumptionReferenceAlignment;
	}


	/**
	 * Used for transforming a mapping file generated by S-Match to the Alignment Format. 
	 * @param inputFile a .txt file holding the S-Match mappings
	 * @return a BasicAlignment object representing the alignment in Alignment Format.
	 * @throws IOException
	 * @throws AlignmentException
	 * @throws URISyntaxException 
	 * @throws OWLOntologyCreationException 
	 */
	public static BasicAlignment transformSMatchAlignment(File representativeAlignmentFile, File inputFile) throws IOException, AlignmentException, URISyntaxException, OWLOntologyCreationException {

		BasicAlignment transformedAlignment = new URIAlignment();

		AlignmentParser parser = new AlignmentParser();
		BasicAlignment representativeAlignment = (BasicAlignment) parser.parse(representativeAlignmentFile.toURI().toString());

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = representativeAlignment.getOntology1URI();
		URI onto2URI = representativeAlignment.getOntology2URI();
		
		File ontoFile = new File("./files/ESWC_WordEmbedding_Experiment/ATMONTO-AIRM-O/ontologies/ATMOntoCoreMerged.owl");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		
		System.out.println("Test: The URIs are " + onto1URI.toString() + " and " + onto2URI.toString());

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		transformedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		// Open the file
		FileInputStream fstream = new FileInputStream(inputFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;

		URI concept1 = null;
		URI concept2 = null;
		String relation = null;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {

			String[] array = strLine.split("\t");

			for (int i = 0; i < array.length; i++) {
				
//				System.out.println("Printing contents of each line");
//				System.out.println("Concept 1: " + OntologyOperations.getClassFromLabel(array[0].substring(array[0].lastIndexOf("\\") +1), onto).getIRI().getFragment());
//				System.out.println("Relation: " + array[1]);
//				System.out.println("Concept 2: " + array[2].substring(array[2].lastIndexOf("\\") +1));

				System.out.println("Concept 1 is " + array[0].substring(array[0].lastIndexOf("\\") +1));
				OWLClass cls = OntologyOperations.getClassFromLabel(array[0].substring(array[0].lastIndexOf("\\") +1), onto);
				if (cls != null) {
				concept1 = URI.create(onto1URI.toString()+OntologyOperations.getClassFromLabel(array[0].substring(array[0].lastIndexOf("\\") +1), onto).getIRI().getFragment());
				} else {
					concept1 = URI.create(onto1URI.toString() + array[0].substring(array[0].lastIndexOf("\\") +1));
				}
				relation = array[1];
				//concept2 = URI.create(onto2URI.toString() + "#" + array[2].substring(array[2].lastIndexOf("\\") +1));
				concept2 = URI.create(onto2URI.toString() + array[2].substring(array[2].lastIndexOf("\\") +1));

				System.out.println("Concept1 URI: " + concept1.toString());
				System.out.println("Concept2 URI: " + concept2.toString());

				transformedAlignment.addAlignCell(concept1, concept2, relation, 0);

			}

		}

		//Close the input stream
		br.close();

		System.out.println("The cells in the transformed alignment are: ");

		for (Cell c : transformedAlignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " " + c.getRelation().getRelation() + " " + c.getObject2().toString());
		}

		return transformedAlignment;

	}

	public static void readAlignment(File inputAlignmentFile) throws AlignmentException {

		AlignmentParser parser = new AlignmentParser();
		BasicAlignment originalAlignment = (BasicAlignment) parser.parse(inputAlignmentFile.toURI().toString());

		System.out.println("Ontology 1 URI: " + originalAlignment.getOntology1URI());
		System.out.println("Ontology 2 URI: " + originalAlignment.getOntology2URI());
	}
	
	/**
	 * Imports two ontologies and creates an alignment holding all possible relations between all concepts in the two ontologies
	 * @param ontoFile1 the source ontology
	 * @param ontoFile2 the target ontology
	 * @return an alignment holding all possible relations between all concepts in two ontologies
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Feb 26, 2019
	 */
	public static URIAlignment createAllRelations(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException, AlignmentException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		URIAlignment alignment = new URIAlignment();
		
		URI onto1URI = onto1.getOntologyID().getOntologyIRI().toURI();
		URI onto2URI = onto2.getOntologyID().getOntologyIRI().toURI();
		System.out.println("Test: The URIs are " + onto1URI.toString() + " and " + onto2URI.toString());

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		alignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		
		for (OWLClass s : onto1.getClassesInSignature()) {
			for (OWLClass t : onto2.getClassesInSignature()) {
				alignment.addAlignCell(s.getIRI().toURI(), t.getIRI().toURI(), "=", 0.0);
			}
		}
		
		
		return alignment;

	}

	/**
	 * Increases an input value by 12 percent
	 * @param inputStrength the strength/confidence for a correspondence to be increased
	 * @return a value 12 percent higher than its input value
	 */
	public static double increaseCellStrength(double inputStrength, double addition) {

		double newStrength = inputStrength + (inputStrength * (addition/100));

		if (newStrength > 1.0) {
			newStrength = 1.0;
		}

		return newStrength;
	}

	/**
	 * Decreases an input value by 12 percent
	 * @param inputStrength the strength/confidence for a correspondence to be decreased
	 * @return a value 12 percent lower than its input value
	 */
	public static double reduceCellStrength(double inputStrength, double reduction) {

		double newStrength = inputStrength - (inputStrength * (reduction/100));

		if (newStrength > 1.0) {
			newStrength = 1.0;
		}

		return newStrength;
	}

	/**
	 * Prints all the cells in an alignment with the objects being represented in the string format (not their URIs)
	 * @param inputAlignment
	 * @throws AlignmentException
	 */
	public static void printAlignmentAsString(BasicAlignment inputAlignment) throws AlignmentException {

		for (Cell c : inputAlignment) {
			//System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getRelation().getRelation() + " - " + c.getStrength());
			System.out.println(c.getObject1() + " - " + c.getObject2() + " - " + c.getRelation().getRelation() + " - " + c.getStrength());
		}

	}

	/**
	 * Prints only the objects (entities) from all the cells in an alignment with the objects being represented in the string format (not their URIs)
	 * @param inputAlignment
	 * @throws AlignmentException
	 */
	public static void printAlignmentEntitiesAsString(BasicAlignment inputAlignment) throws AlignmentException {

		for (Cell c : inputAlignment) {
			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment());
		}

	}

	/**
	 * Creates a reference alignment in the Alignment Format with a list of relations described in a text file. The originalAlignmentFile is simply used for ensuring that the URIs associated with the concepts in a relation are correct.
	 * @param originalAlignmentFile
	 * @param inputFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws AlignmentException
	   19. okt. 2018
	 */
	public static BasicAlignment extendReferenceAlignment (File originalAlignmentFile, File inputFile) throws FileNotFoundException, IOException, AlignmentException {
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment originalAlignment = (BasicAlignment) parser.parse(originalAlignmentFile.toURI().toString());

		Map<String, String> cellMap = new HashMap<String, String>();

		BasicAlignment extendedAlignment = new URIAlignment();	

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = originalAlignment.getOntology1URI();
		URI onto2URI = originalAlignment.getOntology2URI();

		extendedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		// Open the file
		FileInputStream fstream = new FileInputStream(inputFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {

			//split each relation line (object1 - object2 - relation type - strength)
			String[] arr = strLine.split(" - ");	

			extendedAlignment.addAlignCell(URI.create(onto1URI.toString()+arr[0]), URI.create(onto2URI.toString()+arr[1]), arr[2], Double.parseDouble(arr[03]));


		}

		br.close();

		return extendedAlignment;

	}


	/*	*//**
	 * Creates a reference alignment in the Alignment Format from a list of relations described in a text file. The assumption is that all relations in this text file are also included in the original alignment file which is also provided as parameter. 
	 * TO-DO: A more usable method would parse the input ontologies for the entities included in relations from the text file. 
	 * @param originalAlignmentFile
	 * @param inputFile
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws AlignmentException
	 *//*
	public static BasicAlignment createReferenceAlignment (File originalAlignmentFile, File inputFile) throws FileNotFoundException, IOException, AlignmentException {
		AlignmentParser parser = new AlignmentParser();
		BasicAlignment originalAlignment = (BasicAlignment) parser.parse(originalAlignmentFile.toURI().toString());

		Map<String, String> cellMap = new HashMap<String, String>();

		BasicAlignment filteredAlignment = new URIAlignment();	

		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = originalAlignment.getOntology1URI();
		URI onto2URI = originalAlignment.getOntology2URI();

		filteredAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );

		// Open the file
		FileInputStream fstream = new FileInputStream(inputFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {

			String[] arr = strLine.split(" - ");	

			//System.out.println("Trying " + arr[0] + " and " + arr[1]);

			for (Cell c : originalAlignment) {
				if (c.getObject1AsURI().getFragment().equals(arr[0]) && c.getObject2AsURI().getFragment().equals(arr[1])) {
					filteredAlignment.addAlignCell(c.getObject1(), c.getObject2(), c.getRelation().getRelation(), c.getStrength());
					System.out.println("Adding " + c.getObject1AsURI().getFragment() + " and " + c.getObject2AsURI().getFragment() + " to reference alignment");
				} else {
					System.out.println(arr[0] + " and / or " + arr[1] + " is not in the original alignment");
				}
			}

		}

		//Close the input stream
		br.close();

		return filteredAlignment;

	}*/
	
	/**
	 * Normalises an alignment by ensuring that if two cells in an alignment contain the same object1, object2 and the same relation type, the cell with the highest confidence value is retained.
	 * @param inputAlignment
	 * @return normalised alignment holding cells where duplicates (with lower confidence values) are removed
	 * @throws AlignmentException
	   Mar 11, 2019
	 */
	public static URIAlignment normaliseAlignment (URIAlignment inputAlignment) throws AlignmentException {

		
		URIAlignment normalisedAlignment = new URIAlignment();
		
		URI sourceURI = inputAlignment.getOntology1URI();
		URI targetURI = inputAlignment.getOntology2URI();		
		normalisedAlignment.init( sourceURI, targetURI, A5AlgebraRelation.class, BasicConfidence.class );
		

		ArrayList<Cell> allCells = new ArrayList<Cell>();


			for (Cell c : inputAlignment) {
				allCells.add(c);
			}
		

		ArrayList<Cell> processed = new ArrayList<Cell>();
		ArrayList<Cell> toKeep = new ArrayList<Cell>();
		double thisStrength;
		double max;
		Cell currBestCell;
		Cell bestCell;

		
		for (Cell currentCell : allCells) {			
			
			//get the strength of currentCell
			thisStrength = 0;
			max = 0;
			currBestCell = null;
			bestCell = null;
			
			if (!processed.contains(currentCell)) {
				
				// get all cells that has the same object1 as currentCell
				ArrayList<Cell> sameObj1 = new ArrayList<Cell>();				
				for (Cell c : allCells) {
					if (c.getObject1().equals(currentCell.getObject1())) {
						sameObj1.add(c);
					}
				}
							
				//why bigger than 1 and not?
				if (sameObj1.size() > 1) {
					
					// placeholder for cells that contains the same object1 and
					// object 2 as currentCell AND that has the same relation type as currentCell
					ArrayList<Cell> sameObj2 = new ArrayList<Cell>();

					Object o2 = currentCell.getObject2();
					Relation rCurrent = currentCell.getRelation();

					//checking if the cells in sameObj1 also have the same object 2 as "currentCell", AND that their relation type is the same -> if so add the cells to "toCheck"
					for (Cell c2 : sameObj1) {
						if (o2.equals(c2.getObject2()) && rCurrent.equals(c2.getRelation())) {
							sameObj2.add(c2);
						}

					}
										
					//if toCheck is not null or an empty set
					if (sameObj2 != null && !sameObj2.isEmpty()) {

						for (Cell c : sameObj2) {
							thisStrength = max;
							
							if (c.getStrength() >= thisStrength) {
								max = c.getStrength();
								currBestCell = c;
								
							}
														
							//checking that c (this cell) in fact is not currentCell
							if (c != currentCell) {
								toKeep.add(c);
								processed.add(currentCell);
								
							}
						}
						
						bestCell = currBestCell;
						normalisedAlignment.addAlignCell(bestCell.getObject1(), bestCell.getObject2(), bestCell.getRelation().getRelation(), bestCell.getStrength());
					}				

				} else {
					
				}
			}
		}

		return normalisedAlignment;
		
		
	}
	
	

	public static BasicAlignment normaliseScore(BasicAlignment aToBeNormalised, BasicAlignment a2, BasicAlignment a3) {

		double avgConf_a1 = aToBeNormalised.avgConfidence();
		System.out.println("Avg conf a1: " + avgConf_a1);
		double avgConf_a2 = a2.avgConfidence();
		System.out.println("Avg conf a2: " + avgConf_a2);
		double avgConf_a3 = a3.avgConfidence();
		System.out.println("Avg conf a3: " + avgConf_a3);

		double addedStrength = 100 * (((avgConf_a2 + avgConf_a3) / 2) - (avgConf_a1));
		System.out.println("addedStrength is " + addedStrength);

		for (Cell c : aToBeNormalised) {
			double currentStrength = c.getStrength();
			System.out.println("CurrentStrength is " + currentStrength);
			c.setStrength(currentStrength + ((addedStrength / 100) * currentStrength));
		}

		BasicAlignment normalisedAlignment = aToBeNormalised;
		return normalisedAlignment;
	}

	public static BasicAlignment createDiffAlignment(File alignment1, File alignment2) throws AlignmentException {

		AlignmentParser parser = new AlignmentParser();
		AlignmentParser parser2 = new AlignmentParser(1);

		BasicAlignment a1 = (BasicAlignment) parser.parse(alignment1.toURI().toString());
		BasicAlignment a2 = (BasicAlignment) parser2.parse(alignment2.toURI().toString());

		BasicAlignment diffAlignment = new BasicAlignment();

		diffAlignment = (BasicAlignment) a1.diff(a2);

		return diffAlignment;

	}
	
	public static BasicAlignment createDiffAlignment(BasicAlignment BasicAlignment, BasicAlignment alignment2) throws AlignmentException {

		BasicAlignment diffAlignment = new BasicAlignment();

		diffAlignment = (BasicAlignment) BasicAlignment.diff(alignment2);

		return diffAlignment;

	}
	
	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
	    Comparator<K> valueComparator =  new Comparator<K>() {
	        public int compare(K k1, K k2) {
	            int compare = map.get(k2).compareTo(map.get(k1));
	            if (compare == 0) return 1;
	            else return compare;
	        }
	    };
	    Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	    sortedByValues.putAll(map);
	    return sortedByValues;
	}

	/**
	 * Testing
	 * @param args
	 * @throws AlignmentException 
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws AlignmentException, IOException, URISyntaxException, OWLOntologyCreationException {
		
		
		//public static void printExtendedReferenceAlignment (String referenceAlignmentPath, String sourceOntologyPath, String targetOntologyPath) throws AlignmentException, OWLOntologyCreationException, IOException {
//		String refAlign = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";
//		String onto1 = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl";
//		String onto2 = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl";
//		String output = "./files/_PHD_EVALUATION/EVALUATION/ATMONTO-AIRM-REFALIGN-EQUIVALENCE.csv";
//		
//		printExtendedReferenceAlignmentCSV(refAlign, onto1, onto2, output);
		
		//public static URIAlignment combineEQAndSUBAlignments(String eqAlignmentPath, String subAlignmentPath) throws AlignmentException {
		String eqAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/HARMONY_NOWEIGHT/EQUIVALENCE/ComputedHarmonyAlignment_WEIGHTED.rdf";
		String subAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/HARMONY_NOWEIGHT/SUBSUMPTION/ComputedHarmonyAlignment_ComputedHarmonyAlignment_WEIGHTED.rdf";
		
		URIAlignment combinedAlignment = combineEQAndSUBAlignments(eqAlignment, subAlignment);

		
		System.out.println("The combined alignment contains " + combinedAlignment.nbCells() + " relations");
		System.out.println("The URIs are " + combinedAlignment.getOntology1URI() + " and " + combinedAlignment.getOntology2URI());
		
		String output = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/HARMONY_NOWEIGHT/EQUIVALENCE_SUBSUMPTION/ComputedHarmonyAlignment_ComputedHarmonyAlignment_WEIGHTED_EQ_SUB.rdf";
		
		File outputAlignment = new File(output);

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);
		
		combinedAlignment.render(renderer);

		writer.flush();
		writer.close();
		
		
		
		//public static URIAlignment createAllRelations(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException, AlignmentException {
//		File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-301.rdf");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301302/301302-302.rdf");
//		
//		URIAlignment a = createAllRelations(ontoFile1, ontoFile2);
//		
//		String output = "./files/TestAlignment.rdf";
//		
//		File outputAlignment = new File(output);
//
//		PrintWriter writer = new PrintWriter(
//				new BufferedWriter(
//						new FileWriter(outputAlignment)), true); 
//		AlignmentVisitor renderer = new RDFRendererVisitor(writer);
//		
//		a.render(renderer);
//
//		writer.flush();
//		writer.close();
		
		/*COMBINE ALL ALIGNMENT FILES IN A FOLDER INTO A SINGLE ALIGNMENT FILE */
		//public static URIAlignment combineAlignments(String folderName) throws AlignmentException {
//		String folder = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/HARMONY/backup";
//		URIAlignment combinedAlignment = combineAlignments(folder);
//		for (Cell c : combinedAlignment) {
//			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getRelation().getRelation() + " - " + c.getStrength());
//		}
		
		/* SCALE THE CONFIDENCE OF RELATIONS BETWEEN [0..1]*/
		//public static void normalizeConfidence (BasicAlignment initialAlignment) throws AlignmentException {
//		File alignmentFile = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/HARMONY/ComputedHarmonyAlignment.rdf");
//		File alignmentFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/HARMONY/EQUIVALENCE/301302/301302-301-301302-302-RangeMatcher.rdf");
//		
//		AlignmentParser parser = new AlignmentParser();
//		URIAlignment alignment = (URIAlignment) parser.parse(alignmentFile.toURI().toString());
//		URIAlignment alignment2= (URIAlignment) parser.parse(alignmentFile2.toURI().toString());
//		
//		System.out.println("The alignent file contains "  + alignment2.nbCells() + " relations");
//		
//		//sortAlignment(alignment);
//		
//		System.out.println("\nPrinting initial alignment");
//		for (Cell c : alignment) {
//			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getRelation().getRelation() + " - " + c.getStrength());
//		}
//		
//		normalizeConfidence(alignment);
//		System.out.println("\nPrinting alignment after normalization");
//		for (Cell c : alignment) {
//			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment() + " - " + c.getRelation().getRelation() + " - " + c.getStrength());
//		}
//		
//		sortAlignment(alignment);
		
		/* TRANSFORM FROM COMA ALIGNMENT TO ALIGNMENT API */
//		String comafile = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/COMA/bibframe-schemaorg-coma.txt";
////		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
////		File ontoFile2= new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
//		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
//		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
//		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
//		URIAlignment comaAlignment = convertFromComaToAlignmentAPI (comafile, onto1, onto2);
//		System.out.println("Printing relations in alignment");
//		for (Cell c : comaAlignment) {
//			System.out.println(c.getObject1AsURI().getFragment() + " - " + c.getObject2AsURI().getFragment());
//		}
//		
//		//store the alignment
//		String output = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/COMA/bibframe-schemaorg-coma.rdf";
//		
//		File outputAlignment = new File(output);
//
//		PrintWriter writer = new PrintWriter(
//				new BufferedWriter(
//						new FileWriter(outputAlignment)), true); 
//		AlignmentVisitor renderer = new RDFRendererVisitor(writer);
//		
//		comaAlignment.render(renderer);
//
//		writer.flush();
//		writer.close();
		
		/* CONVERT FROM ALIGNMENT API TO A FORMAT ACCEPTED BY STROMA */
//		File inputAlignmentFile = new File("./files/ESWC_ATMONTO_AIRM/Evaluation/ATMONTO-AIRM/aml-atmonto-airm.rdf");		
//		convertToComaFormat(inputAlignmentFile, ontoFile1, ontoFile2);
		
		/* CLEAN ALIGNMENT FROM AIRM UNDERSCORED CONCEPTS */
//		File alignmentFile = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/BLOOMS/Blooms_ATMONTO-AIRM_Wikipedia.rdf");
//		String output = "./files/_PHD_EVALUATION/ATMONTO-AIRM/BLOOMS/Blooms_ATMONTO-AIRM_Wikipedia-cleaned.rdf";
//		
//		cleanAlignment(alignmentFile, output);

		/* EXTRACT SUBSUMPTION RELATIONS FROM ALIGNMENT */
//		File alignmentFile = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/COMPETITION/STROMA/bibframe-schemaorg-STROMA.rdf");
//		String subsumptionOutput = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/COMPETITION/STROMA/bibframe-schemaorg-STROMA-SUB.rdf";
//		String equivalenceOutput = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ALIGNMENTS/COMPETITION/STROMA/bibframe-schemaorg-STROMA-EQ.rdf";
//		extractSubsumptionRelations(alignmentFile, subsumptionOutput);
//		extractEquivalenceRelations(alignmentFile, equivalenceOutput);
		
		
		
//		printAlignmentURIs(alignmentFile);
		
		/* TRANSFORM SMATCH ALIGNMENT TO ALIGNMENT FORMAT*/
//		String onto1 = "atmonto";
//		String onto2 = "airm_o";
////		File refAlign = new File("./files/ESWC_WordEmbedding_Experiment/OAEI2011/ref_alignments/"+onto1+onto2+"/"+onto1+"-"+onto2+".rdf");
////		File smatchAlignmentFile = new File("./files/ESWC_WordEmbedding_Experiment/ATMONTO-AIRM-O/"+onto1+onto2+"/"+onto1+onto2+"-smatch-alignment-minimal.txt");
//		File refAlign = new File("./files/ESWC_WordEmbedding_Experiment/ATMONTO-AIRM-O/ref_alignment/ReferenceAlignment-ATMONTO-AIRM-O-EQ-SUB.rdf");
//		File smatchAlignmentFile = new File("./files/ESWC_WordEmbedding_Experiment/ATMONTO-AIRM-O/atmonto-airm-o-alignment-minimal.txt");
//		
//		BasicAlignment refAlignment = transformSMatchAlignment(refAlign, smatchAlignmentFile);
//		System.out.println("Test: The transformed alignment contains " + refAlignment.nbCells() + " relations");
//		
//		String output = "./files/ESWC_WordEmbedding_Experiment/ATMONTO-AIRM-O/smatch-alignment-minimal.rdf";
//		
//		File outputAlignment = new File(output);
//
//		PrintWriter writer = new PrintWriter(
//				new BufferedWriter(
//						new FileWriter(outputAlignment)), true); 
//		AlignmentVisitor renderer = new RDFRendererVisitor(writer);
//		
//		refAlignment.render(renderer);
//
//		writer.flush();
//		writer.close();
		
		
		/*TEST INGEST ALIGNMENT*/
//		File alignment1File = new File("./files/ESWC_ATMONTO_AIRM/aml-atmonto-airm.rdf");
//		File alignment2File = new File("./files/ESWC_ATMONTO_AIRM/conceptscopemismath.rdf");
//		
//		AlignmentParser parser = new AlignmentParser();
//		BasicAlignment alignment1 = (BasicAlignment) parser.parse(alignment1File.toURI().toString());
//		BasicAlignment alignment2 = (BasicAlignment) parser.parse(alignment2File.toURI().toString());
//		
//		///createDiffAlignment
//		
//		BasicAlignment diffAlignment = createDiffAlignment(alignment1, alignment2);
//		
//		
//		//store the computed reference alignment to file
//		String AlignmentFileName = "./files/ESWC_ATMONTO_AIRM/conceptScopeMismatchAlignment.rdf";
//		File outputAlignment = new File(AlignmentFileName);
//
//		PrintWriter writer = new PrintWriter(
//				new BufferedWriter(
//						new FileWriter(outputAlignment)), true); 
//		AlignmentVisitor renderer = new RDFRendererVisitor(writer);
//
//		diffAlignment.render(renderer);
//
//		writer.flush();
//		writer.close();

		/*CUT ALIGNMENT*/
//		File wordembeddingfile = new File("./files/wordembedding/ATM-m-m-GlobalVectors-04.rdf");
//		double threshold = 0.7;
//		String output = "./files/wordembedding/ATM-m-m-GlobalVectors-cut-07.rdf";
//		
//		cutAlignment(wordembeddingfile,threshold,output);
		
		
		
		
		
//		*** CREATING A SUBSUMPTION ALIGNMENT FROM EQ ALIGNMENT*** 
		
//		File eqReferenceAlignmentFile = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQ.rdf");
//
//		
//		File onto1File = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
//		File onto2File = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
//
//		//convertToComaFormat(eqReferenceAlignmentFile, onto1File, onto2File); 
//
//						AlignmentParser parser = new AlignmentParser();
//				BasicAlignment a1 = (BasicAlignment) parser.parse(eqReferenceAlignmentFile.toURI().toString());
//
//				BasicAlignment subReferenceAlignment = createSubsumptionReferenceAlignment(a1);
//
//				//store the computed reference alignment to file
//				String AlignmentFileName = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUB.rdf";
//				File outputAlignment = new File(AlignmentFileName);
//
//				PrintWriter writer = new PrintWriter(
//						new BufferedWriter(
//								new FileWriter(outputAlignment)), true); 
//				AlignmentVisitor renderer = new RDFRendererVisitor(writer);
//
//				subReferenceAlignment.render(renderer);
//
//				writer.flush();
//				writer.close();



		/*	//		//public static void readAlignment(File inputAlignmentFile) throws AlignmentException {
		File alignmentTestFile = new File("./files/smatch/MERONYM_SYN-Meronymy.rdf");

		//readAlignment(alignmentTestFile);


		File inputFile = new File("./files/smatch/airportheliport_aerodromeinfrastructure.txt");

		BasicAlignment smatchAlignment = transformSMatchAlignment(alignmentTestFile, inputFile);

		System.out.println(smatchAlignment.getOntology1URI());
		System.out.println(smatchAlignment.getOntology2URI());

		//store the computed alignment to file
		String AlignmentFileName = "./files/smatch/smatch-atmonto-airm.rdf";
		File outputAlignment = new File(AlignmentFileName);

		PrintWriter writer = new PrintWriter(
				new BufferedWriter(
						new FileWriter(outputAlignment)), true); 
		AlignmentVisitor renderer = new RDFRendererVisitor(writer);

		smatchAlignment.render(renderer);

		writer.flush();
		writer.close();*/



		//		File af = new File("./files/wndomainsexperiment/alignments/bibframe-schemaorg-ISub08.rdf");
		//
		//		AlignmentParser parser = new AlignmentParser();
		//		BasicAlignment a1 = (BasicAlignment) parser.parse(af.toURI().toString());
		//		
		//		BasicAlignment a1Inversed = a1.inverse();
		//		
		//		System.out.println("The alignment contains " + a1.nbCells() + " correspondences");
		//		
		//		for (Cell c : a1) {
		//			System.out.println(c.getObject1() + " " + c.getObject2());
		//		}
		//		
		//		System.out.println("Inversed alignment\n:");
		//		
		//		for (Cell c : a1Inversed) {
		//			System.out.println(c.getObject1() + " " + c.getObject2());
		//		}
		//		
		//		printAlignmentEntitiesAsString(a1);
		//		
		//		File alignmentFile1 = new File("./files/wndomainsexperiment/alignments/dbpedia-sumo-ISub08.rdf");
		//		File alignmentFile2 = new File("./files//wndomainsexperiment/alignments/operations/dbpedia-sumo/test.rdf");
		//
		//		
		//		BasicAlignment diffAlignment = createDiffAlignment(alignmentFile1, alignmentFile2);
		//		
		//		System.out.println("The diffAlignment contains " + diffAlignment.nbCells() + " cells");
		//		
		//		//store the computed alignment to file
		//		String diffAlignmentFileName = "./files//wndomainsexperiment/alignments/operations/dbpedia-sumo/diff.rdf";
		//		File outputAlignment = new File(diffAlignmentFileName);
		//		
		//		PrintWriter writer = new PrintWriter(
		//				new BufferedWriter(
		//						new FileWriter(outputAlignment)), true); 
		//		AlignmentVisitor renderer = new RDFRendererVisitor(writer);
		//
		//		diffAlignment.render(renderer);
		//
		//		writer.flush();
		//		writer.close();
	}

}
