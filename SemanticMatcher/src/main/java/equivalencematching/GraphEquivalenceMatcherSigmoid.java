package equivalencematching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.graph.MutableGraph;

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.ontowrap.OntowrapException;
import graph.SimpleGraph;
import utilities.ISub;
import utilities.Sigmoid;
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
public class GraphEquivalenceMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;
	
	OWLOntology sourceOntology;
	OWLOntology targetOntology;
	
	static MutableGraph<String> onto1Graph = null;
	static MutableGraph<String> onto2Graph = null;

	ISub iSubMatcher = new ISub();
	
	/**
	 * This threshold is used for the ISUB similarity.
	 */
	private static final double THRESHOLD = 0.9;


	public GraphEquivalenceMatcherSigmoid(OWLOntology sourceOntology, OWLOntology targetOntology, double profileScore, int slope, double rangeMin, double rangeMax) {

		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.sourceOntology = sourceOntology;
		this.targetOntology = targetOntology;
	}
	

	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {

		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double testProfileScore = 1.0;

		AlignmentProcess a = new GraphEquivalenceMatcherSigmoid(sourceOntology, targetOntology, testProfileScore, 3, 0.5, 0.7);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment graphMatcherAlignment = new BasicAlignment();

		graphMatcherAlignment = (BasicAlignment) (a.clone());

		graphMatcherAlignment.normalise();

		

		System.out.println("Evaluation with threshold 0.2:");
		graphMatcherAlignment.cut(0.2);
		System.out.println("\nThe alignment contains " + graphMatcherAlignment.nbCells() + " relations");
		Evaluator.evaluateSingleAlignment(graphMatcherAlignment, referenceAlignment);
		
		System.out.println("Printing relations at 0.2:");
		for (Cell c : graphMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

	}

	


	/**
	 * Computes an alignment of semantic relations from the computeStrucProx() method
	 */
	public void align( Alignment alignment, Properties param ) throws AlignmentException {
		
		MutableGraph<String> onto1Graph = SimpleGraph.createGraph(sourceOntology);
		MutableGraph<String> onto2Graph = SimpleGraph.createGraph(targetOntology);
		

		int idCounter = 0;
		try {

			for ( Object cl2: ontology2().getClasses() ){
				for ( Object cl1: ontology1().getClasses() ){

					idCounter++; 

					addAlignCell("GraphMatcher" + idCounter, cl1,cl2, "=", 
							Sigmoid.weightedSigmoid(slope, computeStructProx(cl1,cl2, onto1Graph, onto2Graph), Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
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
	public double computeStructProx(Object o1, Object o2, MutableGraph<String> onto1Graph, MutableGraph<String> onto2Graph) throws OWLOntologyCreationException, OntowrapException, IOException {

		String s1 = ontology1().getEntityName(o1).toLowerCase();
		String s2 = ontology2().getEntityName(o2).toLowerCase();

		//get the parent nodes of a class from ontology 1
		List<String> onto1Parents = SimpleGraph.getParents(s1, onto1Graph);

		//get the parent nodes of a class from ontology 2
		List<String> onto2Parents = SimpleGraph.getParents(s2, onto2Graph);

		//find distance from s1 node to owl:Thing
		int distanceC1ToRoot = SimpleGraph.getNodeDepth(s1, onto1Graph);

		//find distance from s2 to owl:Thing
		int distanceC2ToRoot = SimpleGraph.getNodeDepth(s2, onto2Graph);

		double iSubSimScore = 0;
		ISub iSubMatcher = new ISub();

		//map to keep the pair of ancestors matching above the threshold
		Map<String,String> matchingMap = new HashMap<String,String>();

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
		for (Entry<String, String> entry : matchingMap.entrySet()) {
			
			String anc1 = entry.getKey();
			String anc2 = entry.getValue();
			
			avgAncestorDistanceToRoot = SimpleGraph.getNodeDepth(anc1, onto1Graph) + SimpleGraph.getNodeDepth(anc2, onto2Graph);
			
			currentStructProx = (2 * avgAncestorDistanceToRoot) / (distanceC1ToRoot + distanceC2ToRoot);

			if (currentStructProx > structProx) {

				structProx = currentStructProx;
			}

		}
		
		return structProx;
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
	public static URIAlignment returnGEMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment GEMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new GraphEquivalenceMatcherSigmoid(sourceOntology, targetOntology, profileScore, 3, 0.5, 0.7);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	

		BasicAlignment GraphEquivalenceMatcherSigmoidAlignment = new BasicAlignment();

		GraphEquivalenceMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		GraphEquivalenceMatcherSigmoidAlignment.normalise();

		GEMAlignment = GraphEquivalenceMatcherSigmoidAlignment.toURIAlignment();

		GEMAlignment.init( sourceOntology.getOntologyID().getOntologyIRI().toURI(), targetOntology.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return GEMAlignment;

	}	




}