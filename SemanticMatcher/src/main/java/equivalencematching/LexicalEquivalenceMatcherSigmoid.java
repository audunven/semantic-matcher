package equivalencematching;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.ontowrap.OntowrapException;
import utilities.Sigmoid;
import utilities.SimilarityMetrics;
import utilities.StringUtilities;
import utilities.WordNet;

/**
 * The Lexical Equivalence Matcher uses WordNet as a lexical resource for computing equivalence relations between ontology concepts.
 * This class computes confidence scores for each relation using the scores from the ontology profiling.
 * @author audunvennesland
 *
 */
public class LexicalEquivalenceMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;
	
	static OWLOntology sourceOntology;
	static OWLOntology targetOntology;
	
	public LexicalEquivalenceMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, double profileScore, int slope, double rangeMin, double rangeMax) {
		sourceOntology = onto1;
		targetOntology = onto2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}


	/**
	 * Returns an alignment from the Lexical Equivalence Matcher (LEM)
	 * @param ontoFile1 source ontology file
	 * @param ontoFile2 target ontology file
	 * @param weight a weight imposed on the confidence computation (default 1.0 -> no weight)
	 * @return an URIAlignment holding equivalence relations
	 * @throws OWLOntologyCreationException
	 * @throws AlignmentException
	   Jul 14, 2019
	 */
	public static URIAlignment returnLEMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {
		
		URIAlignment LEMAlignment = new URIAlignment();
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);
		
		AlignmentProcess a = new LexicalEquivalenceMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment LexicalEquivalenceMatcherSigmoidAlignment = new BasicAlignment();

		LexicalEquivalenceMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		LexicalEquivalenceMatcherSigmoidAlignment.normalise();
		
		LEMAlignment = LexicalEquivalenceMatcherSigmoidAlignment.toURIAlignment();
		
		LEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );
		
		return LEMAlignment;
		
	}

	/**
	 * Produces an alignment holding semantic relations computed by the wordNetMatch method.
	 */
	public void align(Alignment alignment, Properties param) throws AlignmentException {

		int idCounter = 0;

		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){

					idCounter++; 
					
					//using sigmoid function to compute confidence
					addAlignCell("LexicalEquivalenceMatcherSigmoid" + idCounter, sourceObject,targetObject, "=", 
							Sigmoid.weightedSigmoid(slope, wordNetMatch(sourceObject,targetObject), Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));  
				}
			}

		} catch (Exception e) { e.printStackTrace(); }
	}

	/**
	 * This method uses a combination of WordNet synonym similarity and Jiang-Conrath semantic similarity to infer equivalence relations between ontology concepts.
	 * @param o1 source ontology concept
	 * @param o2 target ontology concept 
	 * @return a score signifying the lexical similarity between the two input ontology concepts
	 * @throws OntowrapException
	   Jul 14, 2019
	 */
	public double wordNetMatch(Object o1, Object o2) throws OntowrapException {

		String source = ontology1().getEntityName(o1);
		String target = ontology2().getEntityName(o2);

		String sourceCompoundHead = null;
		String targetCompoundHead = null;
		String sourceModifierBasis = null;
		String targetModifierBasis = null;
		Set<String> sourceModifierTokens = new HashSet<String>();
		Set<String> targetModifierTokens = new HashSet<String>();
		
		double jcSim = 0;
		double jaccardSim = 0;
		double finalScore = 0; 

		Set<String> sourceSynonyms = new HashSet<String>();
		Set<String> targetSynonyms = new HashSet<String>();

		//if neither concept is a compound, retrieve their synonym sets and compare them using Jaccard
		if (!StringUtilities.isCompoundWord(source) && !StringUtilities.isCompoundWord(target)) {

			sourceSynonyms = WordNet.getAllSynonymSet(source.toLowerCase());
			targetSynonyms = WordNet.getAllSynonymSet(target.toLowerCase());

			if (!sourceSynonyms.isEmpty() && !targetSynonyms.isEmpty()) {
				
				jaccardSim = SimilarityMetrics.jaccardSetSim(sourceSynonyms, targetSynonyms);
			}
			
			jcSim = WordNet.computeJiangConrath(source.toLowerCase(), target.toLowerCase());
			
			finalScore = (jcSim + jaccardSim) / 2;

		}

		//if both source and target are compounds we consider the score as a combination of the Jiang-Conrath similarity between the compound heads and 
		//the Jaccard similarity of their respective compound modifiers
		else if (StringUtilities.isCompoundWord(source) && StringUtilities.isCompoundWord(target)) {
			sourceCompoundHead = StringUtilities.getCompoundHead(source);
			targetCompoundHead = StringUtilities.getCompoundHead(target);
			sourceModifierBasis = source.replace(sourceCompoundHead, "");
			targetModifierBasis = target.replace(targetCompoundHead, "");

			//02.03.2020: Replaced regex for decompounding
			sourceModifierTokens = new HashSet<String>(Arrays.asList(sourceModifierBasis.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")));		
			targetModifierTokens = new HashSet<String>(Arrays.asList(targetModifierBasis.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")));
			
//			sourceModifierTokens = new HashSet<String>(Arrays.asList(sourceModifierBasis.split("(?<=.)(?=\\p{Lu})")));		
//			targetModifierTokens = new HashSet<String>(Arrays.asList(targetModifierBasis.split("(?<=.)(?=\\p{Lu})")));


			//get the Jiang-Conrath similarity of the compound heads
			jcSim = WordNet.computeJiangConrath(sourceCompoundHead, targetCompoundHead);

			//compute the set similarity of synonyms associated with the modifiers
			for (String s : sourceModifierTokens) {
				List<String> sourceModifiersList = Arrays.asList(WordNet.getSynonyms(s.toLowerCase()));
				for (String syn : sourceModifiersList) {
					sourceSynonyms.add(syn);
				}
			}

			for (String t : targetModifierTokens) {
				List<String> targetModifiersList = Arrays.asList(WordNet.getSynonyms(t.toLowerCase()));
				for (String syn : targetModifiersList) {
					targetSynonyms.add(syn);
				}
			}

			//compute the Jaccard similarity for the compound modifier tokens
			if (!sourceSynonyms.isEmpty() && !targetSynonyms.isEmpty()) {
				jaccardSim = SimilarityMetrics.jaccardSetSim(sourceSynonyms, targetSynonyms);
				
				

				if (jcSim > 0.1 && jaccardSim > 0.1) {
									
				//give the jcSim more weight than the jaccard
				finalScore = (jcSim * 0.75) + (jaccardSim * 0.25);
				
				} else {
					finalScore = 0;
				}


				//if only the compounds are similar this is not sufficient grounds for saying that the two concepts are similar, so in this case the similarity should be 0!
			} else {

				finalScore = 0;
			}


		}


		//if only the source is a compound, split the modifier into tokens and compute the Jiang-Conrath between all modifier tokens + the compound head of the source against
		//the target concept
		else if (StringUtilities.isCompoundWord(source) && !StringUtilities.isCompoundWord(target)) {

			sourceCompoundHead = StringUtilities.getCompoundHead(source);
			
			//get the synonyms of the source compound head only
			sourceSynonyms = WordNet.getAllSynonymSet(sourceCompoundHead.toLowerCase());
			targetSynonyms = WordNet.getAllSynonymSet(target.toLowerCase());
			
			sourceModifierBasis = source.replace(sourceCompoundHead, "");
			
			sourceModifierTokens = new HashSet<String>(Arrays.asList(StringUtilities.getCompoundParts(sourceModifierBasis)));	
			
			double localJcSim = 0;

			//if the compound head of the source equals the target, we have most likely a subsumption relation, so we give that a score of zero
			if (sourceCompoundHead.equalsIgnoreCase(target)) {

				finalScore = 0;
			}

			//if any of the compound modifiers of the source equals the target, we have most likely a meronymic relation, so we give that a score of zero as well.
			else if (sourceModifierTokens.contains(target)) {

				finalScore = 0;
			}

			//if neither of the above scenarios occur, we compute the score as a combination of the Jiang-Conrath similarity between every compound modifier token of the source and the target, and
			//the Jiang-Conrath similarity between the compound head of the source and the target.
			else {

				for (String mod : sourceModifierTokens) {

					localJcSim += WordNet.computeJiangConrath(mod, target);				
				}

				jcSim = (localJcSim + WordNet.computeJiangConrath(sourceCompoundHead, target)) / (double) sourceModifierTokens.size();

				if (jcSim > 1.0) {
					jcSim = 1.0;
				}
				
				//compute the Jaccard similarity between the source compound head synonyms and target concept synonyms
				if (!sourceSynonyms.isEmpty() && !targetSynonyms.isEmpty()) {
					jaccardSim = SimilarityMetrics.jaccardSetSim(sourceSynonyms, targetSynonyms);
				} else {
					jaccardSim = 0;
				}
				

				finalScore = (jcSim + jaccardSim) / 2;

			}


		}

		//if only the target is a compound
		else if (StringUtilities.isCompoundWord(target) && !StringUtilities.isCompoundWord(source)) {
			targetCompoundHead = StringUtilities.getCompoundHead(target);
			
			//get the synonyms of the target compound head only
			targetSynonyms = WordNet.getAllSynonymSet(targetCompoundHead.toLowerCase());
			sourceSynonyms = WordNet.getAllSynonymSet(source.toLowerCase());
			
			targetModifierBasis = target.replace(targetCompoundHead, "");
			
			targetModifierTokens = new HashSet<String>(Arrays.asList(StringUtilities.getCompoundParts(targetModifierBasis)));
			
			double localJcSim = 0;

			//if the compound head of the target equals the source, we have most likely a subsumption relation, so we give that a score of zero
			if (targetCompoundHead.equalsIgnoreCase(source)) {
				finalScore = 0;
			}

			//if any of the compound modifiers of the target equals the source, we have most likely a meronymic relation, so we give that a score of zero as well.
			else if (targetModifierTokens.contains(source)) {
				finalScore = 0;
			}

			//if neither of the above scenarios occur, we compute the score as a combination of the Jiang-Conrath similarity between every compound modifier token of the target and the source, and
			//the Jiang-Conrath similarity between the compound head of the target and the source.
			else {

				for (String mod : targetModifierTokens) {

					localJcSim += WordNet.computeJiangConrath(mod, source);	
				}

				jcSim = (localJcSim + WordNet.computeJiangConrath(source, targetCompoundHead)) / (double) targetModifierTokens.size();

				if (jcSim > 1.0) {
					jcSim = 1.0;
				}

				//compute the Jaccard similarity between the source compound head synonyms and target concept synonyms
				if (!sourceSynonyms.isEmpty() && !targetSynonyms.isEmpty()) {
					jaccardSim = SimilarityMetrics.jaccardSetSim(sourceSynonyms, targetSynonyms);
				} else {
					jaccardSim = 0;
				}
				

				finalScore = (jcSim + jaccardSim) / 2;


			}
		}

		//if neither of the above scenarios occur, we compute the Jiang-Conrath similarity between source and target
		else {

			finalScore = WordNet.computeJiangConrath(source, target);
		}


		return finalScore;

	}
}