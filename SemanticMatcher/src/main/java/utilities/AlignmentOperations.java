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

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
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

import evaluation.general.EvaluationScore;
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
	
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, IOException, URISyntaxException {	

	String alignmentPath = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/ATMONTO-AIRM/AML/input/ATM_AML_Automatic_040820.rdf";
	String outputPath = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/ATMONTO-AIRM/AML/output/";
	String ontoFile1 = "/Users/audunvennesland/ontologies/ATM/ATMOntoCoreMerged.owl";
	String ontoFile2 = "/Users/audunvennesland/ontologies/ATM/airm-mono.owl";
	
	extractAllConfidenceThresholds(alignmentPath, outputPath, ontoFile1, ontoFile2);
	
	String candidateAlignment = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/BIBFRAME-SCHEMAORG/AML/output/bibframe-schema-org_AML_0.0.rdf";
	String referenceAlignment = "./files/_PHD_EVALUATION/_EVALUATION_SEMPREC_REC/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQ-SUB.rdf";
	
	AlignmentParser aparser = new AlignmentParser(0);
	Alignment refalign =  aparser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignment)));
	Alignment candAlign =  aparser.parse(new URI(StringUtilities.convertToFileURL(candidateAlignment)));
	Alignment extractedAlignment = extractEqualRelations(candAlign, refalign);
	
	System.out.println("extractedAlignment contains " + extractedAlignment.nbCells());
	
	}
	
	/**
	 * Returns an EvaluationScore object that measures the precision, recall and F-measure of a candidate alignment against a reference alignment. The method considers the type of relation.
	 * @param candidateAlignment the alignment being evaluated
	 * @param referenceAlignment the alignment holding the correct set of relations.
	 * @return EvaluationScore
	 * @throws AlignmentException
	   Aug 13, 2020
	 */
	public static Alignment extractEqualRelations (Alignment candidateAlignment, Alignment referenceAlignment) throws AlignmentException {
		
		
		Alignment thisAlignment = new BasicAlignment();
		
		for (Cell rac : referenceAlignment) {
			for (Cell ac : candidateAlignment) {
				
				if (rac.getObject1AsURI().equals(ac.getObject1AsURI()) 
						&& rac.getObject2AsURI().equals(ac.getObject2AsURI()) 
						&& rac.getRelation().getRelation().equals(ac.getRelation().getRelation())) {
					thisAlignment.addAlignCell(ac.getObject1(), ac.getObject2(), ac.getRelation().getRelation(), ac.getStrength());
				}
				
			}
		}
		
		return thisAlignment;
				
	

	}
	
	public static void extractAllConfidenceThresholds (String alignmentPath, String storePath, String ontoFile1, String ontoFile2) throws AlignmentException, IOException, OWLOntologyCreationException {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(new File(ontoFile1));
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(new File(ontoFile2));
		
		String matcher = "AML";
		
		AlignmentParser parser = new AlignmentParser();		
		URIAlignment alignment = (URIAlignment) parser.parse(new File(alignmentPath).toURI().toString());
		
		double[] confidence = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};
				
		AlignmentVisitor renderer = null; 
		File outputAlignment = null;
		PrintWriter writer = null;
		String alignmentFileName = null;
		BasicAlignment cutAlignment = null;
		BasicAlignment clonedAlignment = null;
		URIAlignment tempAlignment = null;
		
		System.out.println("The initial alignment contains " + alignment.nbCells() + " relations");
		
		for (double conf : confidence) {
			cutAlignment = (BasicAlignment)(alignment.clone());
			cutAlignment.toURIAlignment();
			cutAlignment.cut(conf);
			
			System.out.println("The cut alignment at confidence " + conf + " contains " + cutAlignment.nbCells() + " relations.");
			alignmentFileName = storePath + "/" + StringUtilities.stripOntologyName(ontoFile1.toString()) + 
					"-" + StringUtilities.stripOntologyName(ontoFile2.toString()) + "_" + matcher + "_"+ conf + ".rdf";
			
			outputAlignment = new File(alignmentFileName);
			
			writer = new PrintWriter(
					new BufferedWriter(
							new FileWriter(outputAlignment)), true); 
			renderer = new RDFRendererVisitor(writer);
			
			clonedAlignment = (BasicAlignment)(cutAlignment.clone());
			
			System.out.println("The cloned alignment at confidence " + conf + " contains " + clonedAlignment.nbCells() + " relations.");
			
			
			tempAlignment = clonedAlignment.toURIAlignment();
			
			System.out.println("The temp alignment at confidence " + conf + " contains " + tempAlignment.nbCells() + " relations.");
			
			//tempAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
			tempAlignment.render(renderer);
			
			writer.flush();
			writer.close();
			

		}
	}
	
	/**
	 * Prints relations from an alignment to console with additional details related to each concept in the relation: definitions, subclasses and superclasses. Primarily used
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
	 * Prints relations from an alignment to CSV with additional details related to each concept in the relation: definitions, subclasses and superclasses. Primarily used
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
	
	
	/**
	 * Removes relations from an alignment having a confidence of 0.0
	 * @param inputAlignment
	 * @return URIAlignment without relations having a confidence of 0.0
	 * @throws AlignmentException
	   Jul 17, 2019
	 */
	public static URIAlignment removeZeroConfidenceRelations(BasicAlignment inputAlignment) throws AlignmentException {
	
		URIAlignment alignmentWithNonZeroRelations = new URIAlignment();
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = inputAlignment.getOntology1URI();
		URI onto2URI = inputAlignment.getOntology2URI();
		
		
		alignmentWithNonZeroRelations.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		
		for (Cell c : inputAlignment) {
			if (c.getStrength() != 0.0) {
				
				alignmentWithNonZeroRelations.addAlignCell(c.getId(), c.getObject1AsURI(), c.getObject2AsURI(), c.getRelation().getRelation(), c.getStrength());
				
			} 
		}
		
		return alignmentWithNonZeroRelations;

	}
	
	/**
	 * Sorts an alignment by confidence value (in descending order)
	 * @param alignment an input alignment
	 * @return a list of cells (relations)
	 * @throws AlignmentException
	   Jul 17, 2019
	 */
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
	 * @return URIAlignment holding all relations from a set of individual alignments
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
			if (subCell.getRelation().getRelation().equals("<")) {
				combinedAlignment.addAlignCell(subCell.getId(), subCell.getObject1(), subCell.getObject2(),  "&lt;", subCell.getStrength());
			} else {
			combinedAlignment.addAlignCell(subCell.getId(), subCell.getObject1(), subCell.getObject2(),  subCell.getRelation().getRelation(), subCell.getStrength());
			}
		}
		
		//need to initialise the alignment with ontology URIs and the type of relation (e.g. A5AlgebraRelation) otherwise exceptions are thrown
		URI onto1URI = eqAlignment.getOntology1URI();
		URI onto2URI = eqAlignment.getOntology2URI();
		
		combinedAlignment.init( onto1URI, onto2URI, A5AlgebraRelation.class, BasicConfidence.class );
		
		return combinedAlignment;
	}
	
	/**
	 * Extracts all subsumption relations from an alignment
	 * @param inputAlignment the input alignment from which subsumption relations will be extracted.
	 * @return an URIAlignment holding only subsumption relations
	 * @throws AlignmentException
	 * @throws IOException
	   Jul 17, 2019
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
	 * Extracts all equivalence relations from an alignment
	 * @param inputAlignment the input alignment from which equivalence relations will be extracted
	 * @return an URIAlignment holding only equivalence relations
	 * @throws AlignmentException
	 * @throws IOException
	   Jul 17, 2019
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
	 * This method normalizes (or scales) the confidence given to the relations between [0..1]. 
	 * @param initialAlignment an input alignment holding relations where the confidence values are to be normalized.
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

		double thisConfidence = 0;		

		for (Cell cell : initialAlignment) {

			thisConfidence = normMin + (cell.getStrength()-min)*(normMax-normMin) / (max-normMin);
			cell.setStrength(thisConfidence);

		}


	}

	

	/**
	 * Compares two alignments and returns an alignment with the diff set of relations in these two alignments.
	 * @param alignment1 input alignment 1
	 * @param alignment2 input alignment 2
	 * @return a BasicAlignment holding the relations that differ from input alignments 1 and 2
	 * @throws AlignmentException
	   Jul 17, 2019
	 */
	public static BasicAlignment createDiffAlignment(BasicAlignment alignment1, BasicAlignment alignment2) throws AlignmentException {

		BasicAlignment diffAlignment = new BasicAlignment();

		diffAlignment = (BasicAlignment) alignment1.diff(alignment2);

		return diffAlignment;

	}
	
	/**
	 * Sorts a map by its values in descending order.
	 * @param map an input map to be sorted
	 * @return a Map object with sorted values.
	   Jul 17, 2019
	 */
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
	 * Creates a map where the key is the name of an OWL class C and the value is a set of classes being the range in object properties where C is the domain.
	 * @param onto OWL ontology
	 * @return a Map<String, Set<String>> class to range associations.
	   Jul 17, 2019
	 */
	private static Map<String, Set<String>> getRangeMap(OWLOntology onto) {
		Map<String, Set<String>> rangeMap = new HashMap<String, Set<String>>();


		for (OWLClass c : onto.getClassesInSignature()) {
			Set<OWLObjectProperty> ops = new HashSet<OWLObjectProperty>();
			
			//get the object properties where c is domain
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
	
	/**
	 * Creates a subsumption reference alignment based on logical entailment from an equivalence reference alignment as input parameter.
	 * @param equivalenceReferenceAlignment the alignment holding equivalence relations from which subsumption relations will be entailed.
	 * @return a subsumption alignment created from sub- and superclasses of equivalent concepts
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 */
	private static BasicAlignment createSubsumptionReferenceAlignment(BasicAlignment equivalenceReferenceAlignment) throws OWLOntologyCreationException, AlignmentException, URISyntaxException {

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
					System.out.println(StringUtilities.getLabelWithoutPrefix(sc) + " < " + c.getObject2AsURI().getFragment() + " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

				}

				//then get all subclasses of e2 and make them subsumed by e1
			} if (onto2ClassesAndSubclasses.containsKey(c.getObject2().toString())) {

				subclasses = onto2ClassesAndSubclasses.get(c.getObject2().toString());

				for (String sc : subclasses) {
					subsumptionReferenceAlignment.addAlignCell(c.getObject1AsURI(), new URI(sc), ">", 1.0);
					//print justification for entailed subsumption relation
					System.out.println(c.getObject2AsURI().getFragment() + " > " + StringUtilities.getLabelWithoutPrefix(sc)+  " because: " + 
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
					System.out.println(StringUtilities.getLabelWithoutPrefix(sc) + " > " + c.getObject2AsURI().getFragment() + " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

				}
			} if (onto2ClassesAndSuperclasses.containsKey(c.getObject2().toString())) {
				superclasses = onto2ClassesAndSuperclasses.get(c.getObject2().toString());

				for (String sc : superclasses) {
					subsumptionReferenceAlignment.addAlignCell(c.getObject1AsURI(), new URI(sc), "<", 1.0);
					//print justification for entailed subsumption relation
					System.out.println(c.getObject1AsURI().getFragment() + " < " + StringUtilities.getLabelWithoutPrefix(sc) +  " because: " + 
							c.getObject1AsURI().getFragment() + " = " + c.getObject2AsURI().getFragment());

					//System.out.println("Adding " + sc + " as superclass to " + c.getObject1AsURI());
				}
			}
		}


		return subsumptionReferenceAlignment;
	}
	
	/**
	 * Used for transforming a mapping file generated by S-Match to the Alignment Format.
	 * @param representativeAlignmentFile an Alignment API alignment file including the ontology URIs of the two ontologies being matched.
	 * @param sMatchFile the alignment file generated by S-Match which is transformed to an alignment file formatted according to the Alignment API. 
	 * @return a BasicAlignment representing an alignment produced by the S-Match matching system.
	 * @throws IOException
	 * @throws AlignmentException
	 * @throws URISyntaxException
	 * @throws OWLOntologyCreationException
	   Jul 17, 2019
	 */
	private static BasicAlignment transformSMatchAlignment(File representativeAlignmentFile, File sMatchFile) throws IOException, AlignmentException, URISyntaxException, OWLOntologyCreationException {

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
		FileInputStream fstream = new FileInputStream(sMatchFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;

		URI concept1 = null;
		URI concept2 = null;
		String relation = null;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {

			String[] array = strLine.split("\t");

			for (int i = 0; i < array.length; i++) {

				System.out.println("Concept 1 is " + array[0].substring(array[0].lastIndexOf("\\") +1));
				OWLClass cls = OntologyOperations.getClassFromLabel(array[0].substring(array[0].lastIndexOf("\\") +1), onto);
				if (cls != null) {
				concept1 = URI.create(onto1URI.toString()+OntologyOperations.getClassFromLabel(array[0].substring(array[0].lastIndexOf("\\") +1), onto).getIRI().getFragment());
				} else {
					concept1 = URI.create(onto1URI.toString() + array[0].substring(array[0].lastIndexOf("\\") +1));
				}
				relation = array[1];
				concept2 = URI.create(onto2URI.toString() + array[2].substring(array[2].lastIndexOf("\\") +1));

				transformedAlignment.addAlignCell(concept1, concept2, relation, 0);

			}

		}

		br.close();

		return transformedAlignment;

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
	private static URIAlignment createAllRelations(File ontoFile1, File ontoFile2) throws OWLOntologyCreationException, AlignmentException {
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
	 * Converts an alignment generated by COMA to Alignment API format. The input file is formatted like this: [Class1] [Class2] = [Confidence]
	 * @param comaTextFilePath the alignment file generated by the COMA matcher.
	 * @param onto1 source ontology
	 * @param onto2 target ontology
	 * @return an URIAlignment formatted according to the Alignment API.
	 * @throws AlignmentException
	 * @throws FileNotFoundException
	 * @throws IOException
	   Dec 17, 2018
	 */
	private static URIAlignment convertFromComaToAlignmentAPI (String comaTextFilePath, OWLOntology onto1, OWLOntology onto2) throws AlignmentException, FileNotFoundException, IOException {
		
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
	 * This method converts an alignment in the Alignment API format to a format that is accepted by the STROMA enrichment system. Basically the path (superclasses) to each object in a cell is added as prefix to the object,
	 * and <-> is used to separate the object paths (meaning = in this case) and after : comes the confidence value.
	 * @param inputAlignmentFile the original alignment file in Alignment Format
	 * @param ontoFile1
	 * @param ontoFile2
	 * @throws AlignmentException
	 * @throws OWLOntologyCreationException
	   26. okt. 2018
	 */
	private static void convertToComaFormat(File inputAlignmentFile, File ontoFile1, File ontoFile2) throws AlignmentException, OWLOntologyCreationException {

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
					path1.append(StringUtilities.getLabelWithoutPrefix(s) + ".");
				}
				fullPath1 = path1.toString() + c.getObject1AsURI().getFragment();
			} else {
				fullPath1 = c.getObject1AsURI().getFragment();
			}

			if (superclasses2 != null) {
				
				path2 = new StringBuffer();
				fullPath2 = new String();
			for (String t : superclasses2) {
				path2.append(StringUtilities.getLabelWithoutPrefix(t) + ".");
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
	private static URIAlignment fixEntityOrder (BasicAlignment inputAlignment) throws AlignmentException {
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


}
