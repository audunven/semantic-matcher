package graph;

import com.google.common.collect.Iterators;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import utilities.OntologyOperations;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class SimpleGraph {

	public SimpleGraph() {}

	public static MutableGraph<String> createGraph (OWLOntology onto) {

		//get classes and their superclasses
		Map<String, String> superClassMap = OntologyOperations.getClassesAndSuperClassesUsingPellet(onto);

		//get individual classes from the superClassMap
		Set<String> classes = superClassMap.keySet();

		//create the graph
		MutableGraph<String> graph = GraphBuilder.directed().allowsSelfLoops(false).build();

		//create a node for thing
		String thingNode = "Thing";

		for (String s : classes) {
			String superClass = null;

			for (Entry<String, String> entry : superClassMap.entrySet()) {
				if (s.equals(entry.getKey())) {
					superClass = superClassMap.get(entry.getKey());
					//create an is-a relationship from the class to its superclass. If a class does not have any defined superclasses, create an is-relationship to thing
					if (superClass != null) {
						graph.putEdge(s.toLowerCase(), superClass.toLowerCase());
					} else {
						graph.putEdge(s.toLowerCase(), thingNode.toLowerCase());
					}
				}
			}
		}

		return graph;
	}
	

	public static int getNodeDepth (String nodeName, MutableGraph<String> graph) {

		Iterator<String> iter = Traverser.forGraph(graph).breadthFirst(nodeName.toLowerCase()).iterator();

		Traverser.forGraph(graph).breadthFirst(nodeName.toLowerCase());

		//minus 1 since nodeName is also included by the traverser
		return Iterators.size(iter)-1;

	}

	
	public static List<String> getParents (String node, MutableGraph<String> graph) {
		List<String> parents = new LinkedList<String>();
		
		Iterator<String> iter = Traverser.forGraph(graph).breadthFirst(node).iterator();
		
		String currentNode = null;
		while (iter.hasNext()) {
			currentNode = iter.next();
			if (!currentNode.equals(node) && !currentNode.equalsIgnoreCase("thing")) {
			parents.add(currentNode);
			}
		}
		
		return parents;
		
	}


	public static Map<String, Integer> getOntologyHierarchy (OWLOntology onto, MutableGraph<String> graph) {

		Map<String, Integer> hierarchyMap = new LinkedHashMap<String, Integer>();
		Set<String> conceptsSet = OntologyOperations.getClassesAsString(onto);
		for (String s : conceptsSet) {
			if (!s.equalsIgnoreCase("Thing")) //we don´t need owl:Thing in the map
			hierarchyMap.put(s, getNodeDepth(s, graph)-2); //tweak with -2 to get the correct num edges to graph root
		}

		return hierarchyMap;


	}

	//test method
	public static void main(String[] args) throws OWLOntologyCreationException {

		File ontoFile = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto = manager.loadOntologyFromOntologyDocument(ontoFile);

		//create graph
		MutableGraph<String> graph = createGraph(onto);

		Map<String, Integer> hierarchyMap = getOntologyHierarchy(onto, graph);

		System.out.println("\nPrinting hierarchyMap:");
		for (Entry<String, Integer> e : hierarchyMap.entrySet()) {
			System.out.println(e.getKey() + " : " + e.getValue());
		}
		
		String node = "bikestore";
		
		List<String> parents = getParents(node, graph);
		
		System.out.println(parents);
		
		System.out.println("The depth of " + node + " is " + getNodeDepth(node, graph));


	}

} 