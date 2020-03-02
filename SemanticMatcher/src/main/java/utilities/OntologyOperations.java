package utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

import fr.inrialpes.exmo.ontosim.string.StringDistances;
import net.didion.jwnl.JWNLException;
import rita.RiWordNet;

/**
 * @author audunvennesland Date:02.02.2017
 * @version 1.0
 */
public class OntologyOperations {


	/**
	 * An OWLOntologyManagermanages a set of ontologies. It is the main point
	 * for creating, loading and accessing ontologies.
	 */
	static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	
	/**
	 * The OWLReasonerFactory represents a reasoner creation point.
	 */
	static OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();

	/**
	 * A HashMap holding an OWLEntity as key and an ArrayList of instances
	 * associated with the OWLEntity
	 */
	private static HashMap<OWLEntity, ArrayList<String>> instanceMap = new HashMap<OWLEntity, ArrayList<String>>();

	static StringDistances ontoString = new StringDistances();

	/**
	 * Default constructor
	 */
	public OntologyOperations() {

	}

	/**
	 * Retrieves an OWLClass from its label
	 * @param label the label defining the OWLClass
	 * @param onto the OWL ontology holding the OWLClass (and label)
	 * @return the OWLClass object represented by label.
	   Jul 18, 2019
	 */
	public static OWLClass getClassFromLabel (String label, OWLOntology onto) {

		Set<OWLClass> classes = onto.getClassesInSignature();
		Map<String, OWLClass> labelToClassMap = new HashMap<String, OWLClass>();
		String lab = null;
		for (OWLClass cls : classes) {
			for (OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(cls.getIRI())) {
				if (a.getProperty().isLabel()) {
					if (a.getValue() instanceof OWLLiteral) {
						OWLLiteral val = (OWLLiteral) a.getValue();
						lab = val.getLiteral();
						labelToClassMap.put(lab, cls);
					}
				}
			}
		}

		OWLClass cls = labelToClassMap.get(label);
		return cls;
	}

	/**
	 * Retrieves the rdfs label from a class signature
	 * @param cls
	 * @param onto
	 * @return
	   Dec 7, 2018
	 */
	public static String getLabel (OWLClass cls, OWLOntology onto) {
		String label = null;

		IRI cIRI = cls.getIRI();
		for(OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(cIRI)) {
			if(a.getProperty().isLabel()) {
				if(a.getValue() instanceof OWLLiteral) {
					OWLLiteral val = (OWLLiteral) a.getValue();
					label = val.getLiteral();
				}
			}
		}

		return label;
	}



	/**
	 * Retrieves the subclasses for each entity in an ontology and returns a Map where the entity name is key and the set of associated subclasses is value
	 * @param onto the input OWLOntology
	 * @return Map<String, Set<String> where the entity name is key and the set of associated subclasses is value
	 */
	public static Map<String, Set<String>> getSubclasses(OWLOntology onto) {

		Map<String, Set<String>> allClassesAndSubclasses = new HashMap<String, Set<String>>();
		Map<String, Set<String>> classesAndSubclasses = new HashMap<String, Set<String>>();


		Set<OWLClass> allClasses = onto.getClassesInSignature();

		for (OWLClass cls : allClasses) {

			allClassesAndSubclasses.put(cls.getIRI().toString(), getEntitySubclasses(onto, cls));
		}

		//only keep the entries where there are subclasses in the set
		for (Entry<String, Set<String>> e : allClassesAndSubclasses.entrySet()) {

			if (!e.getKey().equals("Thing") && e.getValue().size() > 0) {
				classesAndSubclasses.put(e.getKey(), e.getValue());
			}
		}


		return classesAndSubclasses;

	}


	/**
	 * Helper method that retrieves a set of subclasses for an OWLClass (provided as parameter along with the OWLOntology which is needed for allowing the reasoner to get all subclasses for an OWLClass)
	 * @param onto the input OWLOntology
	 * @param inputClass the OWLClass for which subclasses will be retrieved
	 * @return Set<String> of subclasses for an OWLClass
	 */
	public static Set<String> getEntitySubclasses (OWLOntology onto, OWLClass inputClass) {
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);

		NodeSet<OWLClass> subclasses = reasoner.getSubClasses(inputClass, true);

		Set<String> subclsSet = new HashSet<String>();

		for (OWLClass cls : subclasses.getFlattened()) {
			if (!cls.isOWLNothing()) {
				subclsSet.add(cls.getIRI().toString());
			}
		}

		return subclsSet;

	}

	/**
	 * Helper method that retrieves a set of subclasses (fragments or proper name without URI) for an OWLClass (provided as parameter along with the OWLOntology which is needed for allowing the reasoner to get all subclasses for an OWLClass)
	 * @param onto the input OWLOntology
	 * @param inputClass the OWLClass for which subclasses will be retrieved
	 * @return Set<String> of subclasses for an OWLClass
	 */
	public static Set<String> getEntitySubclassesFragments (OWLOntology onto, OWLClass inputClass) {
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);

		NodeSet<OWLClass> subclasses = reasoner.getSubClasses(inputClass, true);

		Set<String> subclsSet = new HashSet<String>();

		for (OWLClass cls : subclasses.getFlattened()) {
			if (!cls.isOWLNothing()) {
				subclsSet.add(cls.getIRI().getFragment().toString());
			}
		}

		return subclsSet;

	}

	/**
	 * Retrieves the superclasses for each entity in an ontology and returns a Map where the entity name is key and the set of associated superclasses is value
	 * @param onto the input OWLOntology
	 * @return Map<String, Set<String> where the entity name is key and the set of associated superclasses is value
	 */
	public static Map<String, Set<String>> getSuperclasses(OWLOntology onto) {

		Map<String, Set<String>> allClassesAndSuperclasses = new HashMap<String, Set<String>>();
		Map<String, Set<String>> classesAndSuperclasses = new HashMap<String, Set<String>>();



		Set<OWLClass> allClasses = onto.getClassesInSignature();

		for (OWLClass cls : allClasses) {

			allClassesAndSuperclasses.put(cls.getIRI().toString(), getEntitySuperclasses(onto, cls));
		}

		//only keep the entries where there are subclasses in the set
		for (Entry<String, Set<String>> e : allClassesAndSuperclasses.entrySet()) {

			if (!e.getKey().equals("Thing") && e.getValue().size() > 0) {
				classesAndSuperclasses.put(e.getKey(), e.getValue());
			}
		}


		return classesAndSuperclasses;

	}

	/**
	 * Retrieves the DIRECT superclasses for each entity in an ontology and returns a Map where the entity name is key and the set of associated DIRECT superclasses is value
	 * @param onto the input OWLOntology
	 * @return Map<String, Set<String> where the entity name is key and the set of associated DIRECT superclasses is value
	 */
	public static Map<String, Set<String>> getDirectSuperclasses(OWLOntology onto) {

		Map<String, Set<String>> allClassesAndSuperclasses = new HashMap<String, Set<String>>();
		Map<String, Set<String>> classesAndSuperclasses = new HashMap<String, Set<String>>();


		Set<OWLClass> allClasses = onto.getClassesInSignature();

		for (OWLClass cls : allClasses) {

			allClassesAndSuperclasses.put(cls.getIRI().toString(), getDirectEntitySuperclasses(onto, cls));
		}

		//only keep the entries where there are subclasses in the set
		for (Entry<String, Set<String>> e : allClassesAndSuperclasses.entrySet()) {

			if (!e.getKey().equals("Thing") && e.getValue().size() > 0) {
				classesAndSuperclasses.put(e.getKey(), e.getValue());
			}
		}


		return classesAndSuperclasses;

	}


	/**
	 * Helper method that retrieves a set of ALL superclasses for an OWLClass (provided as parameter along with the OWLOntology which is needed for allowing the reasoner to get all superclasses for an OWLClass)
	 * @param onto the input OWLOntology
	 * @param inputClass the OWLClass for which superclasses will be retrieved
	 * @return Set<String> of superclasses for an OWLClass
	 */
	public static Set<String> getEntitySuperclasses (OWLOntology onto, OWLClass inputClass) {
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);

		NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(inputClass, true);

		Set<String> superclsSet = new HashSet<String>();

		for (OWLClass cls : superclasses.getFlattened()) {
			if (!cls.isOWLNothing() && !cls.isOWLThing()) {
				superclsSet.add(cls.getIRI().toString());
			}
		}

		return superclsSet;

	}

	/**
	 * Helper method that retrieves a set of ALL superclasses (their fragments or proper name without URI) for an OWLClass (provided as parameter along with the OWLOntology which is needed for allowing the reasoner to get all superclasses for an OWLClass)
	 * @param onto the input OWLOntology
	 * @param inputClass the OWLClass for which superclasses will be retrieved
	 * @return Set<String> of superclasses for an OWLClass
	 */
	public static Set<String> getEntitySuperclassesFragments (OWLOntology onto, OWLClass inputClass) {
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);

		Set<OWLClass> superclasses = reasoner.getSuperClasses(inputClass, true).getFlattened();

		Set<String> superclsSet = new HashSet<String>();

		for (OWLClass cls : superclasses) {
			if (!cls.isOWLNothing() && !cls.isOWLThing()) {
				superclsSet.add(cls.getIRI().getFragment().toString());
			}
		}

		return superclsSet;

	}

	/**
	 * Helper method that retrieves the set of DIRECT superclasses for an OWLClass (provided as parameter along with the OWLOntology which is needed for allowing the reasoner to get all superclasses for an OWLClass)
	 * @param onto the input OWLOntology
	 * @param inputClass the OWLClass for which DIRECT superclasses will be retrieved
	 * @return Set<String> of DIRECT superclasses for an OWLClass
	 */
	private static Set<String> getDirectEntitySuperclasses (OWLOntology onto, OWLClass inputClass) {
		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);


		NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(inputClass, true);

		Set<String> superclsSet = new HashSet<String>();

		for (OWLClass cls : superclasses.getFlattened()) {
			if (!cls.isOWLNothing() && !cls.isOWLThing()) {
				superclsSet.add(cls.getIRI().toString());
			}
		}

		return superclsSet;

	}

	/**
	 * Returns a Map holding a class as key and its superclass as value
	 * @param o  the input OWL ontology from which classes and superclasses should be derived
	 * @return classesAndSuperClasses a Map holding a class as key and its superclass as value
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses
	 * of this class will describe the reasons.
	 */
	public static Map<String, String> getClassesAndSuperClasses(OWLOntology o) throws OWLOntologyCreationException {

		OWLReasoner reasoner = reasonerFactory.createReasoner(o);
		Set<OWLClass> cls = o.getClassesInSignature();
		Map<String, String> classesAndSuperClasses = new HashMap<String, String>();
		ArrayList<OWLClass> classList = new ArrayList<OWLClass>();

		for (OWLClass i : cls) {
			classList.add(i);
		}

		// Iterate through the arraylist and for each class get the subclasses
		// belonging to it
		// Transform from OWLClass to String to simplify further processing...
		for (int i = 0; i < classList.size(); i++) {
			OWLClass currentClass = classList.get(i);
			NodeSet<OWLClass> n = reasoner.getSuperClasses(currentClass, true);
			Set<OWLClass> s = n.getFlattened();
			for (OWLClass j : s) {
				classesAndSuperClasses.put(currentClass.getIRI().getFragment(), j.getIRI().getFragment());
			}
		}

		manager.removeOntology(o);

		return classesAndSuperClasses;

	}

	/**
	 * Returns a Map holding a class as key and all its superclasses in a Set<String>
	 * @param o the input OWL ontology from which classes and superclasses should be derived
	 * @return classesAndSuperClasses a Map holding a class as key and its superclass as value
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses
	 * of this class will describe the reasons.
	 */
	public static Map<String, Set<String>> getClassesAndAllSuperClasses(OWLOntology onto) throws OWLOntologyCreationException {

		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);


		Map<String, Set<String>> classesAndSuperClasses = new HashMap<String, Set<String>>();

		for (OWLClass c : onto.getClassesInSignature()) {
			Set<String> superclasses = new HashSet<String>();

			NodeSet<OWLClass> n = reasoner.getSuperClasses(c, true);

			for (OWLClass cls : n.getFlattened()) {
				if (!cls.isOWLThing()) {
					superclasses.add(cls.getIRI().getFragment());

				}
			}

			classesAndSuperClasses.put(c.getIRI().getFragment(), superclasses);

		}

		manager.removeOntology(onto);

		return classesAndSuperClasses;

	}

	/**
	 * Returns a Map holding a class as key and all its subclasses in a Set<String>
	 * @param o the input OWL ontology from which classes and subclasses should be derived
	 * @return classesAndSubClasses a Map holding a class as key and its subclass as value
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static Map<String, Set<String>> getClassesAndAllSubClasses(OWLOntology onto) throws OWLOntologyCreationException {

		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);


		Map<String, Set<String>> classesAndSubClasses = new HashMap<String, Set<String>>();

		for (OWLClass c : onto.getClassesInSignature()) {
			Set<String> subclasses = new HashSet<String>();

			NodeSet<OWLClass> n = reasoner.getSubClasses(c, true);

			for (OWLClass cls : n.getFlattened()) {
				if (!cls.isOWLThing() || !cls.isOWLNothing()) {
					subclasses.add(cls.getIRI().getFragment());			
				}
			}

			classesAndSubClasses.put(c.getIRI().getFragment(), subclasses);

		}

		manager.removeOntology(onto);

		return classesAndSubClasses;

	}


	/**
	 * Retrieves an OWLClass from its class name represented as a string
	 * @param className class name represented as string
	 * @param ontology the OWL ontology holding the relevant OWLClass
	 * @return an OWLClass representing the class name represented as a string.
	 */
	public static OWLClass getClass(String className, OWLOntology ontology) {

		OWLClass relevantClass = null;

		Set<OWLClass> classes = ontology.getClassesInSignature();

		for (OWLClass cls : classes) {
			if (cls.getIRI().getFragment().equals(className)) {
				relevantClass = cls;
				break;
			} else {
				relevantClass = null;
			}
		}

		return relevantClass;


	}



	/**
	 * Get number of distinct classes in an ontology
	 * @param ontoFile the file path of the OWL ontology
	 * @return numClasses an integer stating how many OWL classes the OWL ontology has
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int getNumClasses(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		int numClasses = onto.getClassesInSignature().size();

		manager.removeOntology(onto);

		return numClasses;
	}

	/**
	 * Returns an integer stating how many object properties an OWL ontology has
	 * @param ontoFile the file path of the input OWL ontology
	 * @return numObjectProperties an integer stating number of object properties in an OWL ontology
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int getNumObjectProperties(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		int numObjectProperties = onto.getObjectPropertiesInSignature().size();

		manager.removeOntology(onto);

		return numObjectProperties;
	}

	/**
	 * Returns an integer stating how many individuals an OWL ontology has
	 * @param ontoFile the file path of the input OWL ontology
	 * @return numIndividuals an integer stating number of individuals in an OWL ontology
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int getNumIndividuals(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);

		int numIndividuals = onto.getIndividualsInSignature().size();

		manager.removeOntology(onto);

		return numIndividuals;
	}

	/**
	 * Returns an integer stating how many subclasses reside in an OWL ontology.
	 * The method iterates over all classes in the OWL ontology and for each
	 * class counts how many subclasses the current class have. This count is
	 * updated for each class being iterated.
	 * @param ontoFile the file path of the input OWL ontology
	 * @return totalSubClassCount an integer stating number of subclasses in an OWL ontology
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int getNumSubClasses(File ontoFile) throws OWLOntologyCreationException {

		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);

		OWLClass thisClass;
		NodeSet<OWLClass> subClasses;
		Iterator<OWLClass> itr = onto.getClassesInSignature().iterator();
		Map<OWLClass, NodeSet<OWLClass>> classesAndSubClasses = new HashMap<OWLClass, NodeSet<OWLClass>>();
		int subClassCount = 0;
		int totalSubClassCount = 0;

		while (itr.hasNext()) {
			thisClass = itr.next();
			subClasses = reasoner.getSubClasses(thisClass, true);
			subClassCount = subClasses.getNodes().size();
			classesAndSubClasses.put(thisClass, subClasses);
			totalSubClassCount += subClassCount;
		}

		manager.removeOntology(onto);

		return totalSubClassCount;
	}

	/**
	 * Returns an integer stating how many of the classes in an OWL ontology contains individuals (members)
	 * @param ontoFile the file path of the input OWL ontology
	 * @return countClassesWithIndividuals an integer stating number of classes having individuals/members in an OWL ontology
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int containsIndividuals(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		OWLReasoner reasoner = reasonerFactory.createReasoner(onto);
		Iterator<OWLClass> itr = onto.getClassesInSignature().iterator();
		int countClassesWithIndividuals = 0;

		OWLClass thisClass;

		while (itr.hasNext()) {
			thisClass = itr.next();
			if (!reasoner.getInstances(thisClass, true).isEmpty()) {
				countClassesWithIndividuals++;
			}

		}
		manager.removeOntology(onto);

		return countClassesWithIndividuals;
	}

	/**
	 * Returns an integer stating how many of the classes in an OWL ontology do
	 * not have comment annotations associated with them
	 * @param ontoFile the file path of the input OWL ontology
	 * @return numClassesWithoutComments an integer stating number of classes not having annotations associated with them
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int getNumClassesWithoutComments(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		Iterator<OWLClass> itr = onto.getClassesInSignature().iterator();
		int countClassesWithComments = 0;
		int sumClasses = onto.getClassesInSignature().size();

		IRI thisClass;

		while (itr.hasNext()) {
			thisClass = itr.next().getIRI();

			for (OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(thisClass)) {
				if (a.getProperty().isComment()) {
					countClassesWithComments++;
				}
			}

		}

		manager.removeOntology(onto);

		int numClassesWithoutComments = sumClasses - countClassesWithComments;

		return numClassesWithoutComments;
	}

	/**
	 * Returns an integer stating how many of the classes in an OWL ontology do
	 * not have label annotations associated with them
	 * @param ontoFile the file path of the input OWL ontology
	 * @return numClassesWithoutLabels an integer stating number of classes not having label annotations associated with them
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 */
	public static int getNumClassesWithoutLabels(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		Iterator<OWLClass> itr = onto.getClassesInSignature().iterator();
		int countClassesWithLabels = 0;
		int sumClasses = onto.getClassesInSignature().size();

		IRI thisClass;

		while (itr.hasNext()) {
			thisClass = itr.next().getIRI();

			for (OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(thisClass)) {
				if (a.getProperty().isLabel()) {
					countClassesWithLabels++;
				}
			}

		}

		manager.removeOntology(onto);

		int numClassesWithoutLabels = sumClasses - countClassesWithLabels;

		return numClassesWithoutLabels;
	}

	/**
	 * Returns a double stating the percentage of how many classes and object
	 * properties are present as words in WordNet. For object properties their
	 * prefix (e.g. isA, hasA, etc.) is stripped so only their "stem" is
	 * retained.
	 * @param ontoFile the file path of the input OWL ontology
	 * @return wordNetCoverage a double stating a percentage of how many of the
	 * classes and object properties are represented in WordNet
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 * @throws JWNLException 
	 * @throws FileNotFoundException 
	 */
	public static double getWordNetCoverage(File ontoFile) throws OWLOntologyCreationException, FileNotFoundException, JWNLException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		Iterator<OWLClass> itr = onto.getClassesInSignature().iterator();
		Iterator<OWLObjectProperty> itrOP = onto.getObjectPropertiesInSignature().iterator();

		String thisClass;
		String thisOP;

		int numClasses = onto.getClassesInSignature().size();
		//int numOPs = onto.getObjectPropertiesInSignature().size();

		int classCounter = 0;
		int OPCounter = 0;

		while (itr.hasNext()) {
			thisClass = itr.next().getIRI().getFragment();
			if (WordNet.containedInWordNet(thisClass) == true) {
				classCounter++;
			}
		}


		//double wordNetClassCoverage = ((double) classCounter / (double) numClasses);
		double wordNetCoverage = ((double) classCounter / (double) numClasses);
		//double wordNetOPCoverage = ((double) OPCounter / (double) numOPs);

		//double wordNetCoverage = (wordNetClassCoverage + wordNetOPCoverage) / 2;

		return wordNetCoverage;
	}


	/**
	 * Returns a double stating the percentage of how many classes and object
	 * properties are present as words in WordNet. For object properties their
	 * prefix (e.g. isA, hasA, etc.) is stripped so only their "stem" is
	 * retained.
	 * @param ontoFile the file path of the input OWL ontology
	 * @return wordNetCoverage a double stating a percentage of how many of the
	 * classes and object properties are represented in WordNet
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses of this class will describe the reasons.
	 * @throws JWNLException 
	 * @throws FileNotFoundException 
	 */
	public static double getWordNetCoverageComp(File ontoFile) throws OWLOntologyCreationException, FileNotFoundException, JWNLException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);
		Set<OWLClass> classes = onto.getClassesInSignature();

		int classCounter = 0;

		for (OWLClass cl : classes) {
			//get all tokens of the class name
			//02.03.2020: Replaced regex for decompounding
			String[] tokens = cl.getIRI().getFragment().split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
			//String[] tokens = cl.getIRI().getFragment().split("(?<=.)(?=\\p{Lu})");

			int numTokens = tokens.length;
			int tokenCounter = 0;
			int totalCounter = 0;

			for (int i = 0; i < tokens.length; i++) {

				if (WordNet.containedInWordNet(tokens[i])) {
					tokenCounter++;
				}
			}

			if (tokenCounter == numTokens) {
				totalCounter++;
			}

			classCounter += totalCounter;

		}


		int numClasses = onto.getClassesInSignature().size();

		double wordNetCoverage = ((double) classCounter / (double) numClasses);

		return wordNetCoverage;
	}

	

	




	/**
	 * Returns a boolean stating whether a term is considered a compound term
	 * (e.g. ElectronicBook)
	 * @param a the input string tested for being compound or not
	 * @return boolean stating whether the input string is a compound or not
	 */
	public static boolean isCompound(String a) {
		boolean test = false;

		//02.03.2020: Replaced regex for decompounding		
		String[] compounds = a.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
		//String[] compounds = a.split("(?<=.)(?=\\p{Lu})");

		if (compounds.length > 1) {
			test = true;
		}

		return test;
	}

	/**
	 * Returns a count of how many classes are considered compound words in an
	 * ontology
	 * @param ontoFile the file path of the input OWL ontology
	 * @return numCompounds a double stating the percentage of how many of the
	 * classes in the ontology are compounds
	 * @throws OWLOntologyCreationException An exception which describes an error during the creation of
	 * an ontology. If an ontology cannot be created then subclasses
	 * of this class will describe the reasons.
	 */
	public static double getNumClassCompounds(File ontoFile) throws OWLOntologyCreationException {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);

		Iterator<OWLClass> itr = onto.getClassesInSignature().iterator();

		String thisClass;

		int numClasses = onto.getClassesInSignature().size();

		int counter = 0;

		while (itr.hasNext()) {
			thisClass = StringUtilities.replaceUnderscore(itr.next().getIRI().getFragment());

			if (isCompound(thisClass) == true) {
				counter++;

			}
		}

		double numCompounds = ((double) counter / (double) numClasses);

		return numCompounds;
	}


	/**
	 * Retrieves all properties (as strings) where a class represents the domain. This includes both object and data properties.
	 * @param onto OWL ontology holding the class and associated properties.
	 * @param clsString the class being the domain of the properties.
	 * @return set of properties.
	 */
	public static Set<String> getProperties(OWLOntology onto, String clsString) {

		Set<OWLClass> allClasses = onto.getClassesInSignature();		

		Set<String> ops = new HashSet<String>();
		Set<String> dps = new HashSet<String>();

		for (OWLClass cls : allClasses) {
			if (cls.getIRI().getFragment().toLowerCase().equals(clsString)) {

				for (OWLObjectPropertyDomainAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
					if (op.getDomain().equals(cls)) {
						for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
							ops.add(oop.getIRI().getFragment().substring(oop.getIRI().getFragment().lastIndexOf("-") +1));
						}
					}
				}

				for (OWLObjectPropertyRangeAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE)) {
					if (op.getRange().equals(cls)) {
						for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
							ops.add(oop.getIRI().getFragment().substring(oop.getIRI().getFragment().lastIndexOf("-") +1));
						}
					}
				}

				for (OWLDataPropertyDomainAxiom dp : onto.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
					if (dp.getDomain().equals(cls)) {
						for (OWLDataProperty odp : dp.getDataPropertiesInSignature()) {
							dps.add(odp.getIRI().getFragment().substring(odp.getIRI().getFragment().lastIndexOf("-") +1));
						}
					}
				}

			}
		}

		//merge all object and data properties into one set
		Set<String> props = new HashSet<String>();
		props.addAll(ops);
		props.addAll(dps);

		return props;

	}

	/**
	 * Retrieves all object properties related to a class. 
	 * @param onto OWL ontology holding the class and associated properties.
	 * @param clsString the class being the domain of the properties.
	 * @return set of properties as OWLObjectProperty.
	 */
	public static Set<OWLObjectProperty> getObjectProperties(OWLOntology onto, OWLClass oCls) {

		Set<OWLClass> allClasses = onto.getClassesInSignature();		

		Set<OWLObjectProperty> ops = new HashSet<OWLObjectProperty>();

		for (OWLClass cls : allClasses) {
			if (cls.equals(oCls)) {

				for (OWLObjectPropertyDomainAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
					if (op.getDomain().equals(cls)) {
						for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
							ops.add(oop);
						}
					}
				}

				for (OWLObjectPropertyRangeAxiom op : onto.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE)) {
					if (op.getRange().equals(cls)) {
						for (OWLObjectProperty oop : op.getObjectPropertiesInSignature()) {
							ops.add(oop);
						}
					}
				}


			}
		}

		return ops;

	}

	/**
	 * Retrieves classes residing in the context of an OWL class. The context is represented by classes linked via object properties with a constraint
	 * that there are two "object property" edges between the class and its context classes.
	 * @param onto the OWL ontology holding the class cls.
	 * @param cls the OWL class whose context classes are retrieved.
	 * @return a set of OWL classes representing context classes of the OWL class cls.
	   Jul 18, 2019
	 */
	public static Set<OWLClass> getClassesTwoStepsAway (OWLOntology onto, OWLClass cls) {
		Set<OWLClass> classesTwoStepsAway = new HashSet<OWLClass>();
		Set<OWLObjectProperty> ops = getObjectProperties(onto, cls);
		Set<OWLClass> domains = new HashSet<OWLClass>();
		Set<OWLClass> ranges = new HashSet<OWLClass>();

		//get the classes one step away
		for (OWLObjectProperty op : ops) {

			domains = OntologyOperations.getDomainClasses(onto, op);
			ranges = OntologyOperations.getRangeClasses(onto, op);
			classesTwoStepsAway.addAll(domains);
			classesTwoStepsAway.addAll(ranges);

		}


		//clone classesTwoStepsAway to get the 2nd step away context
		Set<OWLClass> classesTwoStepsAwayClone = new HashSet<OWLClass>();
		classesTwoStepsAwayClone.addAll(classesTwoStepsAway);

		Set<OWLObjectProperty> ops2 = new HashSet<OWLObjectProperty>();

		for (OWLClass c : classesTwoStepsAwayClone) {
			ops2.addAll(getObjectProperties(onto, c));
		}

		for (OWLObjectProperty op : ops2) {
			domains = OntologyOperations.getDomainClasses(onto, op);
			ranges = OntologyOperations.getRangeClasses(onto, op);
			classesTwoStepsAway.addAll(domains);
			classesTwoStepsAway.addAll(ranges);
		}


		if (classesTwoStepsAway.contains(cls)) {
			classesTwoStepsAway.remove(cls);
		}

		//TODO: need to check that cls is not in the set
		return classesTwoStepsAway;
	}


	/**
	 * Retrieves all datatype properties related to an OWLClass. 
	 * @param onto the OWL ontology holding the OWL class.
	 * @param clsString
	 * @return
	 */
	
	/**
	 * Retrieves all datatype properties related to an OWLClass. 
	 * @param onto the OWL ontology holding the OWL class.
	 * @param oCls the OWL class whose data properties are retrived.
	 * @return a set of data properties (OWLDataProperty)
	   Jul 18, 2019
	 */
	public static Set<OWLDataProperty> getDataProperties(OWLOntology onto, OWLClass oCls) {

		Set<OWLClass> allClasses = onto.getClassesInSignature();		

		Set<OWLDataProperty> dps = new HashSet<OWLDataProperty>();

		for (OWLClass cls : allClasses) {
			if (cls.equals(oCls)) {

				for (OWLDataPropertyDomainAxiom dp : onto.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN)) {
					if (dp.getDomain().equals(cls)) {
						for (OWLDataProperty dtp : dp.getDataPropertiesInSignature()) {
							dps.add(dtp);
						}
					}
				}

			}
		}

		return dps;

	}

	
	/**
	 * Retrieves the natural language definition associated to an OWL class
	 * @param onto the OWL ontology holding the OWL class
	 * @param c an OWL class whose natural language definition is retrieved.
	 * @return a string representing the natural language definition of an OWL class.
	   Jul 18, 2019
	 */
	public static String getClassDefinition(OWLOntology onto, OWLClass c) {
		
		String definition = null;
		
		for (OWLClass cls : onto.getClassesInSignature()) {
			if (cls.equals(c)) {
				for (OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(cls.getIRI())) {
					if (a.getProperty().isComment()) {
						//need to use a.getValue() instead of a.getAnnotation() to avoid including 'Annotation rdfs comment' that is included before the real definition.
						definition = a.getValue().toString().replaceAll("\"", "");
					}
				}
			}

		}

		return definition;
	}

	/**
	 * Retrieves the natural language definition associated to an OWL class and omits the xsd:string declaration included in some ontologies.
	 * @param onto the OWL ontology holding the class whose definition is retrieved.
	 * @param c the OWL class whose definition is retrived.
	 * @return the natural language definition represented as a string.
	   Jul 18, 2019
	 */
	public static String getClassDefinitionFull (OWLOntology onto, OWLClass c) {

		StringBuilder sb = new StringBuilder();

		for (OWLClass cls : onto.getClassesInSignature()) {
			if (cls.equals(c)) {
				for (OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(cls.getIRI())) {
					sb.append(" " + a.getValue().toString().replaceAll("\\^\\^xsd:string", "").toLowerCase());
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Retrieves a set of tokens representing the natural language definition associated with an OWL class.
	 * @param onto the OWL ontology holding the OWL class.
	 * @param c the OWL class whose natural language definition is retrieved.
	 * @return a set of strings (tokens) representing a natural language definition associated with an OWL class.
	   Jul 18, 2019
	 */
	public static Set<String> getClassDefinitionTokensFull (OWLOntology onto, OWLClass c) {
		Set<String> definitionTokens = new HashSet<String>();

		for (OWLClass cls : onto.getClassesInSignature()) {
			if (cls.equals(c)) {
				for (OWLAnnotationAssertionAxiom a : onto.getAnnotationAssertionAxioms(cls.getIRI())) {
					//require that the definition is min 3 words
					if (a.getValue().toString().split(" ").length >= 3) {
						definitionTokens.add(StringUtilities.removeStopWords(a.getValue().toString().replaceAll("[^a-zA-Z0-9\\s]", "")).toLowerCase());
					}
				}
			}

		}


		return definitionTokens;
	}


	/**
	 * Returns a map holding classes and corresponding natural language definition tokens as values.
	 * @param onto the OWL ontology holding the OWL class.
	 * @return a map holding the class as key and a set of definition tokens as value.
	 * @throws IOException
	   Jul 18, 2019
	 */
	public static Map<String, Set<String>> createClassAndDefMap(OWLOntology onto) throws IOException {

		Map<String, Set<String>> classAndDefMap = new HashMap<String, Set<String>>();

		//get the definition tokens for each class c and lemmatize each token
		for (OWLClass c : onto.getClassesInSignature()) {
			classAndDefMap.put(c.getIRI().getFragment().toLowerCase(), getClassDefinitionTokensFull(onto, c));
		}

		return classAndDefMap;

	}

	/**
	 * Get all tokens in an OWL ontology returned as a set
	 * @param onto the OWL ontology whose ontology tokens are retrieved.
	 * @return a set of tokens
	   Jul 18, 2019
	 */
	public static Set<String> getAllOntologyTokens(OWLOntology onto) {
		
		//put both class names and tokens from definitions in tokenSet
		Set<String> tokensSet = new HashSet<String>();
		for (OWLClass c : onto.getClassesInSignature()) {
			if (isCompound(c.getIRI().getFragment())) {
				tokensSet.add(StringUtilities.splitCompounds(c.getIRI().getFragment()).toLowerCase());
				tokensSet.addAll(OntologyOperations.getClassDefinitionTokensFull(onto, c));
			} else {
				tokensSet.add(c.getIRI().getFragment().toLowerCase());
				tokensSet.addAll(OntologyOperations.getClassDefinitionTokensFull(onto, c));
			}
		}

		//for each token in tokenSet, split by whitespace, and add to tokens
		Set<String> tokens = new HashSet<String>();
		for (String s : tokensSet) {
			String[] tokenArr = s.split(" ");
			for (int i = 0; i < tokenArr.length; i++) {
				if (tokenArr[i].trim().length() > 0) {
					tokens.add(tokenArr[i]);
				}
			}
		}

		return tokens;
	}


	/**
	 * Retrieve all domain classes associated with an OWLObjectProperty.
	 * @param onto an OWL ontology
	 * @param op the OWLObjectProperty whose associated domain classes are retrieved.
	 * @return a set of OWL classes
	   Jul 27, 2019
	 */
	private static Set<OWLClass> getDomainClasses (OWLOntology onto, OWLObjectProperty op) {
		Set<OWLClass> clsSet = new HashSet<OWLClass>();

		//get the domain class(es)
		Set<OWLClassExpression> domainClasses = op.getDomains(onto);

		for (OWLClassExpression exp : domainClasses) {
			if (!exp.isAnonymous()) { //need to check if exp represents an anonymous class (a class expression without an IRI identifier)
				clsSet.add(exp.asOWLClass());
			}
		}


		return clsSet;
	}


	/**
	 * Retrieve all range classes associated with an OWLObjectProperty.
	 * @param onto an OWL ontology
	 * @param op the OWLObjectProperty whose associated range classes are retrieved.
	 * @return a set of OWL classes.
	   Jul 27, 2019
	 */
	public static Set<OWLClass> getRangeClasses (OWLOntology onto, OWLObjectProperty op) {
		Set<OWLClass> clsSet = new HashSet<OWLClass>();

		//get the domain class(es)
		Set<OWLClassExpression> rangeClasses = op.getRanges(onto);

		for (OWLClassExpression exp : rangeClasses) {
			if (!exp.isAnonymous()) { //need to check if exp represents an anonymous class (a class expression without an IRI identifier)
				clsSet.add(exp.asOWLClass());
			}
		}


		return clsSet;
	}

}