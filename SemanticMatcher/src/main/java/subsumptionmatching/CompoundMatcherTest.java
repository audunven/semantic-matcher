package subsumptionmatching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import alignmentcombination.HarmonySubsumption;
import equivalencematching.WordEmbeddingMatcher;
import evaluation.general.Evaluator;
import utilities.Sigmoid;
import utilities.StringUtilities;
import utilities.WordNet;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

/**
 * CompoundMatcher identifies subsumption relations based on so-called compounds, that is, a word comprised of individual words (e.g. electronicBook)
 * Inspired by the Compound Strategy described in "Arnold, Patrick, and Erhard Rahm. "Enriching ontology mappings with semantic relations." Data & Knowledge Engineering 93 (2014): 1-18".
 * @author audunvennesland
 *
 */
public class CompoundMatcherTest extends ObjectAlignment implements AlignmentProcess {

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;

	static OWLOntology sourceOntology;
	static OWLOntology targetOntology;

	public CompoundMatcherTest(double profileScore) {
		this.profileScore = profileScore;
	}

	public CompoundMatcherTest(OWLOntology onto1, OWLOntology onto2, double profileScore) {
		this.profileScore = profileScore;
		sourceOntology = onto1;
		targetOntology = onto2;
	}


	//test method
	public static void main(String[] args) throws OWLOntologyCreationException, AlignmentException, URISyntaxException, IOException {

		File ontoFile1 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301303/301303-301.rdf");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/OAEI2011/ONTOLOGIES/301303/301303-303.rdf");
		String referenceAlignment = "./files/_PHD_EVALUATION/OAEI2011/REFALIGN/301303/301-303-SUBSUMPTION.rdf";

		//		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		//		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		//		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-SUBSUMPTION.rdf";


		AlignmentProcess a = new CompoundMatcher(1.0);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment compoundMatcherAlignment = new BasicAlignment();

		compoundMatcherAlignment = (BasicAlignment) (a.clone());

		System.out.println("The 0.0 alignment contains " + compoundMatcherAlignment.nbCells() + " relations");

		//evaluate the Harmony alignment
		BasicAlignment harmonyAlignment = HarmonySubsumption.getHarmonyAlignment(compoundMatcherAlignment);
		System.out.println("The Harmony alignment contains " + harmonyAlignment.nbCells() + " cells");
		Evaluator.evaluateSingleAlignment(harmonyAlignment, referenceAlignment);

		System.out.println("\nThe alignment contains " + compoundMatcherAlignment.nbCells() + " relations");

		System.out.println("Evaluation with no cut threshold:");
		Evaluator.evaluateSingleAlignment(compoundMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.2:");
		compoundMatcherAlignment.cut(0.2);
		Evaluator.evaluateSingleAlignment(compoundMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.4:");
		compoundMatcherAlignment.cut(0.4);
		Evaluator.evaluateSingleAlignment(compoundMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.5:");
		compoundMatcherAlignment.cut(0.5);
		Evaluator.evaluateSingleAlignment(compoundMatcherAlignment, referenceAlignment);

		System.out.println("Evaluation with threshold 0.6:");
		compoundMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(compoundMatcherAlignment, referenceAlignment);

		System.out.println("Printing relations at 0.6:");
		for (Cell c : compoundMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getObject2() + " " + c.getRelation().getRelation() + " " + c.getStrength());
		}

		System.out.println("Evaluation with threshold 0.9:");
		compoundMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignmentDiff(compoundMatcherAlignment.toURIAlignment(), referenceAlignment);

	}

	public static URIAlignment returnCMAlignment (File ontoFile1, File ontoFile2, double weight) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment CMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new CompoundMatcher(onto1, onto2, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment compoundMatcherAlignment = new BasicAlignment();

		compoundMatcherAlignment = (BasicAlignment) (a.clone());

		CMAlignment = compoundMatcherAlignment.toURIAlignment();

		CMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return CMAlignment;

	}

	public void align( Alignment alignment, Properties param ) throws AlignmentException {

		System.out.println("\nStarting Compound Matcher...");
		//long startTime = System.currentTimeMillis();

		int idCounter = 0;

		int numModifiers = 0;

		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ){
				for ( Object targetObject: ontology2().getClasses() ){

					idCounter++;

					String source = ontology1().getEntityName(sourceObject);
					String target = ontology2().getEntityName(targetObject);

					//if s1´s compound head (Research[Project]) equals the full name of s2 (Project): source < target
					if (isCompoundRelation(source, target)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(source)).size();

						if (numModifiers == 1) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "&lt;", 1.0 * profileScore); 

						} else if (numModifiers == 2) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "&lt;", 0.75 * profileScore); 
							
						} else {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "&lt;", 0.50 * profileScore);

						}
					}

					//if s2´s compound head (Research[Project]) equals the full name of s1 (Project): source > target
					else if (isCompoundRelation(target, source)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(target)).size();

						if (numModifiers == 1) {
							
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "fr.inrialpes.exmo.align.impl.rel.SubsumeRelation", 1.0 * profileScore);

						} else if (numModifiers == 2) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "fr.inrialpes.exmo.align.impl.rel.SubsumeRelation", 0.75 * profileScore);

						} else {
							
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "fr.inrialpes.exmo.align.impl.rel.SubsumeRelation", 0.50 * profileScore);

						}

					}

					//if any synonym of the compound head of s1 (Research[Undertaking]) equals the full name of s2 (Project): source < target
					else if(isCompoundSynRelation(source, target)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(source)).size();

						if (numModifiers == 1) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "&lt;", 1.0 * profileScore);

						} else if (numModifiers == 2) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "&lt;", 0.5 * profileScore);

						} else {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "&lt;", 0.25 * profileScore);
	
						}
					}

					//if any synonyms of the compound head of s2 (Research[Undertaking]) equals the full name of s1 (Project): source > target
					else if(isCompoundSynRelation(target, source)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(target)).size();

						if (numModifiers == 1) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "fr.inrialpes.exmo.align.impl.rel.SubsumeRelation", 0.75 * profileScore);

						} else if (numModifiers == 2) {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "fr.inrialpes.exmo.align.impl.rel.SubsumeRelation", 0.50 * profileScore);
							

						} else {
							//basic weighting using profile score
							addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "fr.inrialpes.exmo.align.impl.rel.SubsumeRelation", 0.25 * profileScore);
							

						}

					}
					//if none of the patterns above fits...
					else {
						addAlignCell("CompoundMatcher" +idCounter + "_" + profileScore + "_", sourceObject,targetObject, "!", 0.0); 
					}

				}
			}

		} catch (Exception e) { e.printStackTrace(); }

	}

	

	public static boolean isCompoundRelation(String a, String b) {
		boolean test = false;

		String[] compounds = a.split("(?<=.)(?=\\p{Lu})");

		if (compounds.length > 2) {

			if (b.equals(compounds[compounds.length-1]) || b.equals(compounds[compounds.length-1]+compounds[compounds.length-2] + compounds[compounds.length-3])) {

				test = true;
			}

		} else if (compounds.length > 1) {

			if (b.equals(compounds[compounds.length-1]) || b.equals(compounds[compounds.length-1]+compounds[compounds.length-2])) {

				test = true;
			}
		}
		return test;

	}

	/**
	 * Find synonyms of concept b in WordNet and compares all of them to the compound head of concept a. If any of the synonyms are equal to the 
	 * compound head of a, b < a. 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isCompoundSynRelation(String a, String b) {
		boolean test = false;

		String compoundHead = StringUtilities.getCompoundHead(a);

		Set<String> synonyms = WordNet.getSynonymSet(b);
		synonyms.add(b.toLowerCase());

		for (String s : synonyms) {
			if (s.toLowerCase().equals(compoundHead)) {
				test = true;
			}
		}

		return test;
	}





}