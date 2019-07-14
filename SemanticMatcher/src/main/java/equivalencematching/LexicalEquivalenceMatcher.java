package equivalencematching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import alignmentcombination.HarmonyEquivalence;
import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.ontowrap.OntowrapException;
import utilities.Jaccard;
import utilities.Sigmoid;
import utilities.StringUtilities;
import utilities.WordNet;


public class LexicalEquivalenceMatcher extends ObjectAlignment implements AlignmentProcess {

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;

	static OWLOntology sourceOntology;
	static OWLOntology targetOntology;

	public LexicalEquivalenceMatcher(double profileScore) {
		this.profileScore = profileScore;
	}

	public LexicalEquivalenceMatcher(File ontoFile1, File ontoFile2, double profileScore) {
		this.profileScore = profileScore;
	}

	public LexicalEquivalenceMatcher(OWLOntology onto1, OWLOntology onto2, double profileScore) {
		this.profileScore = profileScore;
		sourceOntology = onto1;
		targetOntology = onto2;
	}



	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {

		//		File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-301.rdf");
		//		File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301304/301304-304.rdf");
		//		String referenceAlignment = "./files/_PHD_EVALUATION/OAEI2011/REFALIGN/301304/301-304-EQUIVALENCE.rdf";

//		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
//		File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
//		String referenceAlignment = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-EQUIVALENCE.rdf";

				File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
				File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
				String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQUIVALENCE.rdf";

		double testProfileScore = 0.72;


		AlignmentProcess a = new LexicalEquivalenceMatcher(sourceOntology, targetOntology, testProfileScore);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment LexicalEquivalenceMatcherAlignment = new BasicAlignment();

		LexicalEquivalenceMatcherAlignment = (BasicAlignment) (a.clone());

		LexicalEquivalenceMatcherAlignment.normalise();


		//evaluate the Harmony alignment
		BasicAlignment harmonyAlignment = HarmonyEquivalence.getHarmonyAlignment(LexicalEquivalenceMatcherAlignment);
		System.out.println("The Harmony alignment contains " + harmonyAlignment.nbCells() + " cells");
		Evaluator.evaluateSingleAlignment(harmonyAlignment, referenceAlignment);

		System.out.println("Printing Harmony Alignment: ");
		for (Cell c : harmonyAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("\nThe alignment contains " + LexicalEquivalenceMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(LexicalEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.2:");
		LexicalEquivalenceMatcherAlignment.cut(0.2);
		Evaluator.evaluateSingleAlignment(LexicalEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		LexicalEquivalenceMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(LexicalEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		LexicalEquivalenceMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(LexicalEquivalenceMatcherAlignment, referenceAlignment);

		System.out.println("Printing relations at 0.6:");
		for (Cell c : LexicalEquivalenceMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.9:");
		LexicalEquivalenceMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(LexicalEquivalenceMatcherAlignment, referenceAlignment);

	}

	public static URIAlignment returnLEMAlignment (File ontoFile1, File ontoFile2, double weight) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment LEMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new LexicalEquivalenceMatcher(ontoFile1, ontoFile2, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment lexicalEquivalenceMatcherAlignment = new BasicAlignment();

		lexicalEquivalenceMatcherAlignment = (BasicAlignment) (a.clone());

		lexicalEquivalenceMatcherAlignment.normalise();

		LEMAlignment = lexicalEquivalenceMatcherAlignment.toURIAlignment();

		LEMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return LEMAlignment;

	}

	public void align(Alignment alignment, Properties param) throws AlignmentException {

		int idCounter = 0;

		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){

					idCounter++;

					//basic weight using profile score
					addAlignCell("LexicalEquivalenceMatcher" + idCounter + "_" + profileScore + "_", sourceObject,targetObject, "=", wordNetMatch(sourceObject,targetObject) * profileScore);  

				}
			}

		} catch (Exception e) { e.printStackTrace(); }
	}

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

				jaccardSim = Jaccard.jaccardSetSim(sourceSynonyms, targetSynonyms);
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

			sourceModifierTokens = new HashSet<String>(Arrays.asList(sourceModifierBasis.split("(?<=.)(?=\\p{Lu})")));		
			targetModifierTokens = new HashSet<String>(Arrays.asList(targetModifierBasis.split("(?<=.)(?=\\p{Lu})")));


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
				jaccardSim = Jaccard.jaccardSetSim(sourceSynonyms, targetSynonyms);

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
			sourceModifierTokens = new HashSet<String>(Arrays.asList(sourceModifierBasis.split("(?<=.)(?=\\p{Lu})")));	
			double localJcSim = 0;

			//if the compound head of the source equals the target, we have most likely a subsumption relation, so we give that a score of zero
			if (sourceCompoundHead.equalsIgnoreCase(target)) {

				//System.out.println(sourceCompoundHead + " equals " + target + " so we give the relation " + source + " " + target + " a score of 0");
				finalScore = 0;
			}

			//if any of the compound modifiers of the source equals the target, we have most likely a meronymic relation, so we give that a score of zero as well.
			else if (sourceModifierTokens.contains(target)) {

				//System.out.println("sourceModifierToken contains " + target + " so we give the relation " + source + " " + target + " a score of 0");
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
					jaccardSim = Jaccard.jaccardSetSim(sourceSynonyms, targetSynonyms);
				} else {
					jaccardSim = 0;
				}


				finalScore = (jcSim + jaccardSim) / 2;
				
				if (finalScore > 0.3) {
					System.out.println("In this case source is a compound and target is not a compound: " + source + " - " + target);
					System.out.println("The Jiang Conrath sim is: " + jcSim);
					System.out.println("The Jaccard sim is: " + jaccardSim);
					
					System.out.println("sourceSynonyms: ");
					for (String s : sourceSynonyms) {
						System.out.println(s);
					}
					System.out.println("targetSynonyms: ");
					for (String s : targetSynonyms) {
						System.out.println(s);
					}
				}

			}


		}

		//if only the target is a compound
		else if (StringUtilities.isCompoundWord(target) && !StringUtilities.isCompoundWord(source)) {
			targetCompoundHead = StringUtilities.getCompoundHead(target);

			//get the synonyms of the target compound head only
			targetSynonyms = WordNet.getAllSynonymSet(targetCompoundHead.toLowerCase());
			sourceSynonyms = WordNet.getAllSynonymSet(source.toLowerCase());

			targetModifierBasis = target.replace(targetCompoundHead, "");
			targetModifierTokens = new HashSet<String>(Arrays.asList(targetModifierBasis.split("(?<=.)(?=\\p{Lu})")));
			double localJcSim = 0;

			//if the compound head of the target equals the source, we have most likely a subsumption relation, so we give that a score of zero
			if (targetCompoundHead.equalsIgnoreCase(source)) {
				//System.out.println(targetCompoundHead + " equals " + source + " so we give the relation " + source + " " + target + " a score of 0");
				finalScore = 0;
			}

			//if any of the compound modifiers of the target equals the source, we have most likely a meronymic relation, so we give that a score of zero as well.
			else if (targetModifierTokens.contains(source)) {
				//System.out.println("targetModifierTokens contains " + source + " so we give the relation " + source + " " + target + " a score of 0");
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
					jaccardSim = Jaccard.jaccardSetSim(sourceSynonyms, targetSynonyms);
				} else {
					jaccardSim = 0;
				}


				finalScore = (jcSim + jaccardSim) / 2;
				
				if (finalScore > 0.3) {
					System.out.println("\nIn this case target is a compound and source is not a compound: " + target + " - " + source);
					System.out.println("The Jiang Conrath sim is: " + jcSim);
					System.out.println("The Jaccard sim is: " + jaccardSim);
					
					System.out.println("sourceSynonyms: ");
					for (String s : sourceSynonyms) {
						System.out.println(s);
					}
					System.out.println("targetSynonyms: ");
					for (String s : targetSynonyms) {
						System.out.println(s);
					}
				}


			}
		}

		//if neither of the above scenarios occur, we compute the Jiang-Conrath similarity between source and target
		else {

			finalScore = WordNet.computeJiangConrath(source, target);
		}


		return finalScore;

	}
}
