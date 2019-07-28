package graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import utilities.ISub;
import utilities.OntologyOperations;

/**
 * Implementation of methods using the Neo4J DB to create and navigate ontology graphs. 
 * TODO: The code needs significant re-factoring and some methods are spread
 * @author audunvennesland
 *
 */
public class Graph {

	static GraphDatabaseService db;

	final static double THRESHOLD = 0.6;

	Label label;

	/**
	 * This label represents the graph/ontology to process
	 */
	static Label labelOnto1;
	
	/**
	 * This label represents the graph/ontology to process
	 */
	static Label labelOnto2;

	ISub iSubMatcher = new ISub();

	final static String key = "classname";

	public Graph(GraphDatabaseService db) {

		this.db = db;

	}

	/**
	 * Creates a Neo4J graph from an OWL ontology.
	 * @param onto the OWL ontology from which a graph is constructed.
	 * @param label the label distinguishes this graph from other graphs (ex. by using the ontology file name or URI)
	 * @throws OWLOntologyCreationException
	   Jul 18, 2019
	 */
	public static void createOntologyGraph(OWLOntology onto, Label label) throws OWLOntologyCreationException {

		Map<String, String> superClassMap = OntologyOperations.getClassesAndSuperClasses(onto);
		Set<String> classes = superClassMap.keySet();
		Iterator<String> itr = classes.iterator();

		try (Transaction tx = db.beginTx()) {
			// creating a node for owl:Thing
			Node thingNode = db.createNode(label);
			thingNode.setProperty("classname", "owl:Thing");

			// create nodes from the ontology, that is, create nodes and give them
			// properties (classname) according to their ontology names
			while (itr.hasNext()) {
				Node classNode = db.createNode(label);
				classNode.setProperty("classname", itr.next().toString());
			}

			// create isA relationships between classes and their superclasses
			ResourceIterable<Node> testNode = db.getAllNodes();
			ResourceIterator<Node> iter = testNode.iterator();

			// iterate through the nodes of the graph database
			while (iter.hasNext()) {
				Node n = iter.next();
				if (n.hasProperty("classname")) {
					String thisClassName = n.getProperty("classname").toString();
					String superClass = null;
					// check if thisClassName equals any of the keys in superClassMap
					for (Map.Entry<String, String> entry : superClassMap.entrySet()) {
						// if this graph node matches a key in the map...
						if (thisClassName.equals(entry.getKey())) {
							// get the superclass that belongs to the key in the map
							superClass = superClassMap.get(entry.getKey());
							// find the "superclass-node" that matches the map value belonging to this key
							Node superClassNode = db.findNode(label, "classname",
									(Object) superClassMap.get(thisClassName));
							// create an isA relationship from this graph node to its superclass
							// if a class does not have any defined super-classes, create an isA
							// relationship to owl:thing
							if (superClassNode != null) {
								n.createRelationshipTo(superClassNode, RelTypes.isA);
							} else {
								n.createRelationshipTo(thingNode, RelTypes.isA);
							}
						}
					}
				}
			}

			// TODO:create the individuals

			// TODO:create the object property relations

			// TODO:create the datatype properties

			tx.success();
		}

	}

	/**
	 * Retrieves the name associated with a node (i.e. the class name)
	 * @param n the node in the graph whose name is retrieved.
	 * @return a String representing the name of the node.
	   Jul 18, 2019
	 */
	public static String getNodeName(Node n) {

		String value = null;

		try (Transaction tx = db.beginTx()) {
			value = n.getProperty(key).toString();
			tx.success();
		}
		// registerShutdownHook(db);
		return value;
	}

	/**
	 * Returns a graph node given a label and property value (i.e. class name)
	 * 
	 * @param value the property value associated with the node
	 * @param label a label represents the graph/ontology to process
	 * @return the node searched for
	 */
	public static Node getNode(String value, Label label) {
		Node testNode = null;

		try (Transaction tx = db.beginTx()) {
			testNode = db.findNode(label, key, value);
			tx.success();
		}
		return testNode;

	}

//	/**
//	 * Returns the ID of a node given the Node instance as parameter
//	 * @param n a Node instance
//	 * @return the ID of a node as a long
//	 */
//	public long getNodeID(Node n) {
//
//		long id = 0;
//
//		try (Transaction tx = db.beginTx()) {
//			id = n.getId();
//			tx.success();
//
//		}
//
//		return id;
//	}

//	/**
//	 * Returns a Traverser that traverses the children of a node given a Node
//	 * instance as parameter
//	 * @param classNode a Node instance
//	 * @return a traverser
//	 */
//	public static Traverser getChildNodesTraverser(Node classNode) {
//
//		TraversalDescription td = null;
//		try (Transaction tx = db.beginTx()) {
//
//			td = db.traversalDescription().breadthFirst().relationships(RelTypes.isA, Direction.INCOMING)
//					.evaluator(Evaluators.excludeStartPosition());
//			tx.success();
//
//		}
//
//		return td.traverse(classNode);
//	}

//	/**
//	 * Returns an ArrayList of all child nodes of a node
//	 * 
//	 * @param classNode a Node instance
//	 * @param label representing the graph/ontology to process
//	 * @return an arraylist of child nodes of a given node
//	 */
//	public static ArrayList<Object> getClosestChildNodesAsList(Node classNode, Label label) {
//
//		ArrayList<Object> childNodeList = new ArrayList<Object>();
//		Traverser childNodesTraverser = null;
//
//		try (Transaction tx = db.beginTx()) {
//
//			childNodesTraverser = getChildNodesTraverser(classNode);
//
//			for (Path childNodePath : childNodesTraverser) {
//				if (childNodePath.length() == 1 && childNodePath.endNode().hasLabel(label)) {
//					childNodeList.add(childNodePath.endNode().getProperty("classname"));
//				}
//			}
//
//			tx.success();
//
//		}
//
//		return childNodeList;
//	}

//	/**
//	 * Returns the number of children a particular node in the graph has
//	 * 
//	 * @param classNode the node whose children nodes are counted
//	 * @param label the label of this graph 
//	 * @return number of children nodes as integer
//	 */
//	public static int getNumChildNodes(Node classNode, Label label) {
//
//		ArrayList<Object> childNodeList = new ArrayList<Object>();
//		Traverser childNodesTraverser = null;
//
//		try (Transaction tx = db.beginTx()) {
//
//			childNodesTraverser = getChildNodesTraverser(classNode);
//
//			for (Path childNodePath : childNodesTraverser) {
//				if (childNodePath.endNode().hasLabel(label)) {
//					childNodeList.add(childNodePath.endNode().getProperty("classname"));
//				}
//			}
//
//			tx.success();
//
//		}
//
//		return childNodeList.size();
//	}

	/**
	 * Returns a Traverser that traverses the parents of a node given a Node
	 * instance as parameter
	 * @param classNode a Node instance
	 * @return a traverser
	 */
	public static Traverser getParentNodeTraverser(Node classNode) {

		TraversalDescription td = null;

		try (Transaction tx = db.beginTx()) {

			td = db.traversalDescription().breadthFirst().relationships(RelTypes.isA, Direction.OUTGOING)
					.evaluator(Evaluators.excludeStartPosition());

			tx.success();

		}

		return td.traverse(classNode);
	}

//	/**
//	 * Returns an ArrayList holding the parent node of the node provided as
//	 * parameter
//	 * @param classNode a node for which the closest parent is to be returned
//	 * @param label a label representing the graph (ontology) to process
//	 * @return the closest parent node
//	 */
//	public static ArrayList<Object> getClosestParentNode(Node classNode, Label label) {
//
//		ArrayList<Object> parentNodeList = new ArrayList<Object>();
//		Traverser parentNodeTraverser = null;
//
//		try (Transaction tx = db.beginTx()) {
//
//			parentNodeTraverser = getParentNodeTraverser(classNode);
//
//			for (Path parentNodePath : parentNodeTraverser) {
//				if (parentNodePath.length() == 1 && parentNodePath.endNode().hasLabel(label)) {
//					parentNodeList.add(parentNodePath.endNode().getProperty("classname"));
//				}
//			}
//
//			tx.success();
//
//		}
//
//		return parentNodeList;
//	}

	/**
	 * Returns an ArrayList holding all parent nodes to the Node provided as
	 * parameter
	 * @param classNode the Node for which all parent nodes are to be retrieved
	 * @param label representing the graph/ontology to process
	 * @return all parent nodes to node provided as parameter
	 */
	public static ArrayList<Object> getAllParentNodes(Node classNode, Label label) {

		ArrayList<Object> parentNodeList = new ArrayList<Object>();
		Traverser parentNodeTraverser = null;

		try (Transaction tx = db.beginTx()) {

			parentNodeTraverser = getParentNodeTraverser(classNode);

			for (Path parentNodePath : parentNodeTraverser) {
				if (parentNodePath.endNode().hasLabel(label)) {
					parentNodeList.add(parentNodePath.endNode().getProperty("classname"));

				}

			}

			tx.success();

		}

		return parentNodeList;
	}

//	/**
//	 * This method finds the shortest path between two nodes used as parameters. The
//	 * path is the full path consisting of nodes and relationships between the
//	 * classNode.. ...and the parentNode.
//	 * @param parentNode the parent node to which the shortest path from classnode is created.
//	 * @param classNode the class node from which the shortest path to parentNode is created.
//	 * @param label representing the graph/ontology to process.
//	 * @param rel the type of relationship between classNode and parentNode.
//	 * @return Iterable<Path> paths
//	 */
//	public static Iterable<Path> findShortestPathBetweenNodes(Node parentNode, Node classNode, Label label,
//			RelationshipType rel) {
//
//		PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forType(rel), 15);
//		Iterable<Path> paths = finder.findAllPaths(classNode, parentNode);
//		return paths;
//
//	}

	/**
	 * Returns the distance from the Node provided as parameter and the root node
	 * (i.e. owl:Thing) We use a Map as a work-around to counting the edges between
	 * a given node and the root (owl:Thing). This is possible since a Map only
	 * allows unique keys and a numbered Neo4J path consists of a set of path items
	 * edge-count, node (property) where all nodes for each edge-count is listed
	 * (e.g. for the node "AcademicArticle" the upwards path is (1, Article), (2,
	 * Document), (3, owl:Thing)).
	 * @param classNode the node from which the distance to root is counted from.
	 * @return the (path) distance from classNode to the root node as an integer.
	 */
	public static int findDistanceToRoot(Node classNode) {
		
		Traverser parentNodeTraverser = null;
		Map<Object, Object> parentNodeMap = new HashMap<>();

		try (Transaction tx = db.beginTx()) {

			parentNodeTraverser = getParentNodeTraverser(classNode);
			
			for (Path parentNodePath : parentNodeTraverser) {
				parentNodeMap.put(parentNodePath.length(), parentNodePath.endNode().getProperty("classname"));

			}

			tx.success();

		}
		int distanceToRoot = parentNodeMap.size();

		return distanceToRoot;
	}

//	/**
//	 * This method finds the shortest path between two nodes used as parameters. The
//	 * path is the full path consisting of nodes and relationships between the
//	 * classNode.. ...and the rootNode.
//	 * 
//	 * @param rootNode
//	 * @param classNode
//	 * @param label
//	 * @param rel
//	 * @return Iterable<Path> paths
//	 */
//	public Iterable<Path> findShortestPathToRoot(Node rootNode, Node classNode, Label label, RelationshipType rel) {
//
//		PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forType(rel), 15);
//		Iterable<Path> paths = finder.findAllPaths(classNode, rootNode);
//		return paths;
//	}

	/**
	 * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely
	 * when the VM exits
	 * 
	 * @param graphDb the GraphDatabaseService
	 */
	public static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	private static enum RelTypes implements RelationshipType {
		isA
	}

//	public static <K, V extends Comparable<V>> V findMapMax(Map<K, V> map) {
//		Entry<K, V> maxEntry = Collections.max(map.entrySet(), new Comparator<Entry<K, V>>() {
//			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
//				return e1.getValue().compareTo(e2.getValue());
//			}
//		});
//		return maxEntry.getValue();
//	}

//	public static double computeStructuralAffinity(File ontoFile1, File ontoFile2, double threshold) throws OWLOntologyCreationException {
//		double structuralAffinity = 0;
//		
//		//load the two ontologies from file
//		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
//		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
//		
//		// create grahps of the two ontologies
//		long time = System.currentTimeMillis();
//		File dbFile = new File("/Users/audunvennesland/Documents/PhD/Development/Neo4J/" + time);
//		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(dbFile);
//		registerShutdownHook(db);
//
//		Label labelOnto1 = DynamicLabel.label(manager.getOntologyDocumentIRI(onto1).getFragment());
//		Label labelOnto2 = DynamicLabel.label(manager.getOntologyDocumentIRI(onto2).getFragment());
//
//		Graph loader = new Graph(db);
//		loader.createOntologyGraph(onto1, labelOnto1);
//		loader.createOntologyGraph(onto2, labelOnto2);
//
//		// adding 1 to include owl:thing...
//		int totalNumEntitiesOnto1 = onto1.getClassesInSignature().size() + 1;
//		int totalNumEntitiesOnto2 = onto2.getClassesInSignature().size() + 1;
//		
//		int minEntities = 0;
//
//		//find the ontology with the least number of entities, this is used to compute the structural affinity
//		if (totalNumEntitiesOnto1 < totalNumEntitiesOnto2) {
//			minEntities = totalNumEntitiesOnto1;
//		} else {
//			minEntities = totalNumEntitiesOnto2;
//		}
//		
//		int commonEntities = 0;
//		double ic1 = 0;
//		double ic2 = 0;
//		double diff = 0;
//
//		// create a Map representation of each ontology that holds the class as key and its position (depth) in the graph hierarchy as value
//		Map<OWLClass, Integer> onto1Hierarchy = new HashMap<OWLClass, Integer>();
//		for (OWLClass c : onto1.getClassesInSignature()) {
//			if (!c.getIRI().getFragment().equals("Thing"))
//			onto1Hierarchy.put(c, findDistanceToRoot(getNode(c.getIRI().getFragment(), labelOnto1)));
//		}
//
//		Map<OWLClass, Integer> onto2Hierarchy = new HashMap<OWLClass, Integer>();
//		for (OWLClass c : onto2.getClassesInSignature()) {
//			if (!c.getIRI().getFragment().equals("Thing"))
//			onto2Hierarchy.put(c, findDistanceToRoot(getNode(c.getIRI().getFragment(), labelOnto2)));
//		}
//
//		//find the max depth of each ontology
//		int onto1HiearchyMax = findMapMax(onto1Hierarchy);
//		int onto2HiearchyMax = findMapMax(onto2Hierarchy);
//
//		// find the ontology (map) with the lowest depth (value in map)
//		int min = 0;
//		if (onto1HiearchyMax <= onto2HiearchyMax) {
//			min = onto1HiearchyMax;
//		} else {
//			min = onto2HiearchyMax;
//		}
//
//		boolean match = false;
//		for (int i = 1; i <= min; i++) {
//			for (Entry<OWLClass, Integer> s : onto1Hierarchy.entrySet()) {
//				for (Entry<OWLClass, Integer> t : onto2Hierarchy.entrySet()) {
//					if (onto1Hierarchy.containsValue(i) && onto2Hierarchy.containsValue(i)) {
//
//						// creating sets holding concepts at each depth
//						Set<OWLClass> setS = new HashSet<OWLClass>();
//						Set<OWLClass> setT = new HashSet<OWLClass>();
//
//						if (s.getValue() == i) {
//							setS.add(s.getKey());
//						}
//						if (t.getValue() == i) {
//							setT.add(t.getKey());
//						}
//
//						for (OWLClass cls : setS) {
//							for (OWLClass clt : setT) {
//								ic1 = utilities.MathUtils.computeInformationContent(
//										getNumChildNodes(getNode(cls.getIRI().getFragment(), labelOnto1), labelOnto1),
//										totalNumEntitiesOnto1);
//								ic2 = utilities.MathUtils.computeInformationContent(
//										getNumChildNodes(getNode(clt.getIRI().getFragment(), labelOnto2), labelOnto2),
//										totalNumEntitiesOnto2);
//
//								diff = Math.abs(ic1 - ic2);
//								//System.out.println("The diff is " + diff + " for " + s.getKey().getIRI().getFragment() + " and " + t.getKey().getIRI().getFragment());
//								if (diff < threshold)
//									match = true;
//								break;
//
//							}
//						}
//
//					}
//
//				}
//
//			}
//			if (match) {
//				commonEntities++;
//			}
//		}
//
//		structuralAffinity = (double) commonEntities / (double) minEntities;
//		return structuralAffinity;
//
//	}

}