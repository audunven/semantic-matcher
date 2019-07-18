package equivalencematching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.ontowrap.OntowrapException;
import graph.Graph;
import utilities.ISub;
import utilities.StringUtilities;

@SuppressWarnings("deprecation")
/**
 * This matcher identifies the structural proximity of two nodes using the following steps:
 * <ul>
 *  <li>Calculate the distance (number of edges) from the two nodes n_1 and n_2 (ontology concepts to be matched) to their root (thing) as n_1_dist and n_2_dist respectively.</li>
 *  <li>Identify the set of ancestor nodes to n_1 and n_2 with similarity above a certain threshold.</li>
 *  <li>Calculate the distance from each pair of ancestor nodes to the respective graph's root and calculate the average distance avg_Anc_dist.</li>
 * </ul>
 * Then, when the above distances have been retrieved, compute the equivalence score between two nodes as follows:
 * GraphSim(n_1,n_2) = ((2 * avg_Anc_dist) / (n_1_dist + n_2_dist)) 
 * @author audunvennesland
 *
 */
public class GraphEquivalenceMatcher extends ObjectAlignment implements AlignmentProcess {

	/**
	 * This threshold is used for the ISUB similarity.
	 */
	final static double THRESHOLD = 0.9;
	
	/**
	 * The key used for retrieving property values from the Neo4J DB
	 */
	final static String KEY = "classname";

	/**
	 * This label represents the graph/ontology to process
	 */
	static Label labelOnto1;
	
	/**
	 * This label represents the graph/ontology to process
	 */
	static Label labelOnto2;

	static GraphDatabaseService db;

	ISub iSubMatcher = new ISub();

	double weight;


	public GraphEquivalenceMatcher(String ontology1Name, String ontology2Name, GraphDatabaseService database, double weight) {
		labelOnto1 = DynamicLabel.label(ontology1Name);
		labelOnto2 = DynamicLabel.label(ontology2Name);
		db = database;
		this.weight = weight;
	}


	public GraphEquivalenceMatcher() {

	}

	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {
		
		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";

//		File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301303/301303-301.rdf");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301303/301303-303.rdf");

		//create a new instance of the neo4j database in each run
		String ontologyParameter1 = null;
		String ontologyParameter2 = null;	
		Graph creator = null;
		OWLOntologyManager manager = null;
		OWLOntology o1 = null;
		OWLOntology o2 = null;
		Label labelO1 = null;
		Label labelO2 = null;
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String dbName = String.valueOf(timestamp.getTime());
		File dbFile = new File("/Users/audunvennesland/Documents/phd/development/Neo4J_new/" + dbName);	
		System.out.println("Creating a new NEO4J database");
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(dbFile);
		System.out.println("Database created");

		ontologyParameter1 = StringUtilities.stripPath(ontoFile1.toString());
		ontologyParameter2 = StringUtilities.stripPath(ontoFile2.toString());

		//create new graphs
		manager = OWLManager.createOWLOntologyManager();
		o1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		o2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		labelO1 = DynamicLabel.label( ontologyParameter1 );
		labelO2 = DynamicLabel.label( ontologyParameter2 );

		System.out.println("Creating ontology graphs");
		creator = new Graph(db);

		creator.createOntologyGraph(o1, labelO1);
		creator.createOntologyGraph(o2, labelO2);


		double testWeight = 1.0;

		AlignmentProcess a = new GraphEquivalenceMatcher(ontologyParameter1, ontologyParameter2, db, testWeight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment graphMatcherAlignment = new BasicAlignment();

		graphMatcherAlignment = (BasicAlignment) (a.clone());

		graphMatcherAlignment.normalise();
		
		System.out.println("\nThe alignment contains " + graphMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(graphMatcherAlignment, referenceAlignment);
		
		System.out.println("Evaluation with threshold 0.1:");
		graphMatcherAlignment.cut(0.1);
		Evaluator.evaluateSingleAlignment(graphMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		graphMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(graphMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		graphMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(graphMatcherAlignment, referenceAlignment);
		
		System.out.println("Printing relations at 0.6:");
		for (Cell c : graphMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.9:");
		graphMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(graphMatcherAlignment, referenceAlignment);


	}

	/**
	 * Returns an alignment object holding equivalence relations computed by the Graph Equivalence Matcher.
	 * @param ontoFile1 source ontology
	 * @param ontoFile2 target ontology
	 * @param weight a weight on the confidence value (default 1.0)
	 * @return An URIAlignment with equivalence relations computed by structural proximity. 
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Jul 14, 2019
	 */
	public static URIAlignment returnGEMAlignment (File ontoFile1, File ontoFile2, double weight) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment GEMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String dbName = String.valueOf(timestamp.getTime());
		File dbFile = new File("/Users/audunvennesland/Documents/phd/development/Neo4J_new/" + dbName);	
		GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(dbFile);

		Label labelO1 = DynamicLabel.label(StringUtilities.stripPath(ontoFile1.toString()));
		Label labelO2 = DynamicLabel.label(StringUtilities.stripPath(ontoFile2.toString()));
		Graph creator = new Graph(db);
		creator.createOntologyGraph(onto1, labelO1);
		creator.createOntologyGraph(onto2, labelO2);

		AlignmentProcess a = new GraphEquivalenceMatcher(StringUtilities.stripPath(ontoFile1.toString()), StringUtilities.stripPath(ontoFile2.toString()), db, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment graphEquivalenceMatcherAlignment = new BasicAlignment();

		graphEquivalenceMatcherAlignment = (BasicAlignment) (a.clone());

		graphEquivalenceMatcherAlignment.normalise();

		GEMAlignment = graphEquivalenceMatcherAlignment.toURIAlignment();

		GEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return GEMAlignment;

	}	


	/**
	 * Computes an alignment of semantic relations from the computeStrucProx() method
	 */
	public void align( Alignment alignment, Properties param ) throws AlignmentException {

		int idCounter = 0;
		
		try {

			for ( Object cl2: ontology2().getClasses() ){
				for ( Object cl1: ontology1().getClasses() ){

					idCounter++;

					//basic weighting using profile score
					addAlignCell("GraphMatcher" + idCounter + "_" + weight + "_", cl1,cl2, "=", computeStructProx(cl1,cl2) * weight);  

				}

			}

		} catch (Exception e) { e.printStackTrace(); }
	}


	/**
	 * This method computes the structural proximity of two input classes. 
	 * (1) First it finds the input classes in the corresponding graphs, and measures their distance to root (owl:Thing), 
	 * (2) then it retrieves the list of parent nodes to these two input classes,
	 * (3) then it matches the parent nodes of the corresponding input classes,
	 * (4) if the similarity of parent nodes is above the threshold, the distance to root for these parent nodes is counted,
	 * (5) finally, the structural proximity is computed as:
	 * (2 * avgAncestorDistanceToRoot) / (distanceC1ToRoot + distanceC2ToRoot)
	 * @param o1 an ontology object (OWL entity)
	 * @param o2 an ontology object (OWL entity)
	 * @return measure of similarity between the two input objects (ontology entities)
	 * @throws OWLOntologyCreationException
	 * @throws OntowrapException
	 * @throws IOException
	 */
	public double computeStructProx(Object o1, Object o2) throws OWLOntologyCreationException, OntowrapException, IOException {

		Graph.registerShutdownHook(db);		

		String s1 = ontology1().getEntityName(o1);
		String s2 = ontology2().getEntityName(o2);

		//get the s1 node from ontology 1
		Node s1Node = Graph.getNode(s1, labelOnto1);


		//get the s2 node from ontology 2
		Node s2Node = Graph.getNode(s2, labelOnto2);

		//get the parent nodes of a class from ontology 1
		ArrayList<Object> onto1Parents = Graph.getAllParentNodes(s1Node, labelOnto1);


		//get the parent nodes of a class from ontology 2
		ArrayList<Object> onto2Parents = Graph.getAllParentNodes(s2Node,labelOnto2);


		//find distance from s1 node to owl:Thing
		int distanceC1ToRoot = Graph.findDistanceToRoot(s1Node);

		//find distance from s2 to owl:Thing
		int distanceC2ToRoot = Graph.findDistanceToRoot(s2Node);

		double iSubSimScore = 0;
		ISub iSubMatcher = new ISub();

		//map to keep the pair of ancestors matching above the threshold
		Map<Object,Object> matchingMap = new HashMap<Object,Object>();

		//matching the parentnodes
		for (int i = 0; i < onto1Parents.size(); i++) {
			for (int j = 0; j < onto2Parents.size(); j++) {
				iSubSimScore = iSubMatcher.score(onto1Parents.get(i).toString(), onto2Parents.get(j).toString());

				if (iSubSimScore >= THRESHOLD) {

					matchingMap.put(onto1Parents.get(i) , onto2Parents.get(j));
				}	
			}
		}

		double structProx = 0;
		double currentStructProx = 0;
		double avgAncestorDistanceToRoot = 0;


		//loop through the matchingMap containing key-value pairs of ancestors from O1 and O2 being similar over the given threshold
		for (Entry<Object, Object> entry : matchingMap.entrySet()) {
			Node anc1 = Graph.getNode(entry.getKey().toString(), labelOnto1);
			Node anc2 = Graph.getNode(entry.getValue().toString(), labelOnto2);

			avgAncestorDistanceToRoot = (Graph.findDistanceToRoot(anc1) + Graph.findDistanceToRoot(anc2)) / 2;

			currentStructProx = (2 * avgAncestorDistanceToRoot) / (distanceC1ToRoot + distanceC2ToRoot);

			if (currentStructProx > structProx) {

				structProx = currentStructProx;
			}

		}

		return structProx;
	}

	


}