package subsumptionmatching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import evaluation.general.Evaluator;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import mismatchdetection.ConfirmSubsumption;
import rita.wordnet.jwnl.JWNLException;
import utilities.OntologyOperations;
import utilities.Sigmoid;
import utilities.StringUtilities;
import utilities.WNDomain;

public class DefinitionSubsumptionMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	OWLOntology sourceOntology;
	OWLOntology targetOntology;

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;

	static Map<String, List<String>> defMapSource = new HashMap<String, List<String>>();
	static Map<String, List<String>> defMapTarget = new HashMap<String, List<String>>();

	public DefinitionSubsumptionMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, double profileScore, int slope, double rangeMin, double rangeMax) {
		this.sourceOntology = onto1;
		this.targetOntology= onto2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;

	}

	public static void main(String[] args) throws AlignmentException, IOException, URISyntaxException, OWLOntologyCreationException {


		File ontoFile1 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/ATMOntoCoreMerged.owl");
		File ontoFile2 = new File("./files/_PHD_EVALUATION/ATMONTO-AIRM/ONTOLOGIES/airm-mono.owl");
		String referenceAlignment = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-SUBSUMPTION.rdf";

		//		File ontoFile1 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/bibframe.rdf");
		//		File ontoFile2 = new File("./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/ONTOLOGIES/schema-org.owl");
		//		String referenceAlignment = "./files/_PHD_EVALUATION/BIBFRAME-SCHEMAORG/REFALIGN/ReferenceAlignment-BIBFRAME-SCHEMAORG-SUBSUMPTION.rdf";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology sourceOntology = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology targetOntology = manager.loadOntologyFromOntologyDocument(ontoFile2);

		double profileScore = 0.29;
		int slope = 3;
		double rangeMin = 0.5;
		double rangeMax = 0.7;

		AlignmentProcess a = new DefinitionSubsumptionMatcherSigmoid(sourceOntology, targetOntology, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment definitionsSubsumptionMatcherAlignment = new BasicAlignment();

		definitionsSubsumptionMatcherAlignment = (BasicAlignment) (a.clone());

		System.out.println("The 0.0 alignment contains " + definitionsSubsumptionMatcherAlignment.nbCells() + " relations");

		definitionsSubsumptionMatcherAlignment.cut(0.6);
		Evaluator.evaluateSingleAlignment(definitionsSubsumptionMatcherAlignment, referenceAlignment);
		System.out.println("Printing relations at 0.6:");
		for (Cell c : definitionsSubsumptionMatcherAlignment) {
			System.out.println(c.getObject1() + " " + c.getRelation().getRelation() + " " + c.getObject2() + c.getStrength());

		}

		System.out.println("Evaluation with threshold 0.9:");
		System.out.println("Number of relations: " + definitionsSubsumptionMatcherAlignment.nbCells());
		definitionsSubsumptionMatcherAlignment.cut(0.9);
		Evaluator.evaluateSingleAlignment(definitionsSubsumptionMatcherAlignment, referenceAlignment);

	}

	public static URIAlignment returnDSMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment DSMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new DefinitionSubsumptionMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment DefinitionSubsumptionMatcherSigmoidAlignment = new BasicAlignment();

		DefinitionSubsumptionMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		DSMAlignment = DefinitionSubsumptionMatcherSigmoidAlignment.toURIAlignment();

		DSMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return DSMAlignment;

	}

	public void align(Alignment alignment, Properties param) throws AlignmentException {

		try {
			defMapSource = getDefMapTokens(sourceOntology);
		} catch (JWNLException | IOException e1) {
			e1.printStackTrace();
		}
		try {
			defMapTarget = getDefMapTokens(targetOntology);
		} catch (JWNLException | IOException e1) {
			e1.printStackTrace();
		}


		List<String> sourceDefinition = null;
		List<String> targetDefinition = null;

		int idCounter = 0; 

		try {
			// Match classes
			for ( Object sourceObject: ontology1().getClasses() ) {
				for ( Object targetObject: ontology2().getClasses() ){

					idCounter++;

					String source = ontology1().getEntityName(sourceObject).toLowerCase();
					String target = ontology2().getEntityName(targetObject).toLowerCase();

					if (defMapSource.containsKey(source)) {

						sourceDefinition = defMapSource.get(source);

						//iterate all sourceDefinition tokens and check if they match the target concept and if these are from the same domain
						for (String s : sourceDefinition) {

							if (s.equalsIgnoreCase(target)) {

								//if any of the compounds in source and target are from the same domain AND they´re not meronyms we return a confidence of 1.0
								if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject)) 
										&& !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
									addAlignCell("DefinitionSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&gt;", 
											Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
									//else we return a confidence of 0.75
								} else if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject)) 
										|| !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
									addAlignCell("DefinitionSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&gt;", 
											Sigmoid.weightedSigmoid(slope, 0.75, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));

								} 

							} else {
								addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
							}

						}


					} else if (defMapTarget.containsKey(target)) {

						targetDefinition = defMapTarget.get(target);

						//iterate all targetDefinition tokens and check if they match the source concept and if these are from the same domain
						for (String t : targetDefinition) {

							if (t.equalsIgnoreCase(source)) {
								
								//if any of the compounds in source and target are from the same domain according to WNDomain, we return a confidence of 1.0
								if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))
										&& !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {	
								
								addAlignCell("DefinitionSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&lt;", 
										Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
								
								} else if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))
										|| !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
									
									addAlignCell("DefinitionSubsumptionMatcherSigmoid" +idCounter, sourceObject, targetObject, "&lt;", 
											Sigmoid.weightedSigmoid(slope, 0.75, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
								}
							
							}
							
							else {
								addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
							}
						}


					} else {
						addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
					}


				}

			}

		} catch (Exception e) { e.printStackTrace(); }

	}


	private static Map<String, List<String>> getDefMapTokens(OWLOntology onto) throws JWNLException, IOException {

		Map<String, List<String>> defMap = new HashMap<String, List<String>>();
		String extract = null;
		String def = null;
		String cut = null;
		String refined = null;


		for (OWLClass c : onto.getClassesInSignature()) {

			def = OntologyOperations.getClassDefinitionFull(onto, c).toLowerCase();

			//include only those definitions that contain lexico-syntactic patterns

			if (def.contains("including")) {
				extract = def.substring(def.indexOf("including")+10, def.length());
				if (extract.contains(".")) { //include only current sentence if more than one sentence in definition.
					cut = extract.substring(0, extract.indexOf("."));
					refined = removeStopWordsAndDigits(cut);
				} else {

					refined = removeStopWordsAndDigits(extract);
				}				

				List<String> tokens = StringUtilities.tokenizeAndLemmatizeToList(refined, true);				

				defMap.put(c.getIRI().getFragment().toLowerCase(), tokens);
			}

			else if (def.contains("includes")) {
				extract = def.substring(def.indexOf("includes")+9, def.length());
				if (extract.contains(".")) {
					cut = extract.substring(0, extract.indexOf("."));
					refined = removeStopWordsAndDigits(cut);
				} else {

					refined = removeStopWordsAndDigits(extract);
				}				


				List<String> tokens = StringUtilities.tokenizeAndLemmatizeToList(refined, true);

				defMap.put(c.getIRI().getFragment().toLowerCase(), tokens);
			}

			else if (def.contains("such as")) {
				extract = def.substring(def.indexOf("such as")+8, def.length());

				if (extract.contains(".")) {
					cut = extract.substring(0, extract.indexOf("."));
					refined = removeStopWordsAndDigits(cut);
				} else {

					refined = removeStopWordsAndDigits(extract);
				}

				List<String> tokens = StringUtilities.tokenizeAndLemmatizeToList(refined, true);

				defMap.put(c.getIRI().getFragment().toLowerCase(), tokens);
			} 


			else if (def.contains("e.g.")) {
				extract = def.substring(def.indexOf("e.g.")+5, def.length());

				if (extract.contains(".")) {
					cut = extract.substring(0, extract.indexOf("."));
					refined = removeStopWordsAndDigits(cut);
				} else {

					refined = removeStopWordsAndDigits(extract);
				}

				List<String> tokens = StringUtilities.tokenizeAndLemmatizeToList(refined, true);

				defMap.put(c.getIRI().getFragment().toLowerCase(), tokens);
			}


			else if (def.contains("for example")) {
				extract = def.substring(def.indexOf("for example")+12, def.length());

				if (extract.contains(".")) {
					cut = extract.substring(0, extract.indexOf("."));
					refined = removeStopWordsAndDigits(cut);
				} else {

					refined = removeStopWordsAndDigits(extract);
				}				

				List<String> tokens = StringUtilities.tokenizeAndLemmatizeToList(refined, true);

				defMap.put(c.getIRI().getFragment().toLowerCase(), tokens);
			}

		}

		return defMap;
	}



	private static String removeStopWordsAndDigits (String inputString) {

		List<String> stopWordsList = Arrays.asList(
				"a", "an", "and", "are", "as", "at", "be", "but", "by",
				"etc", "for", "if", "in", "into", "is", "it",
				"no", "not", "of", "on", "or", "such",
				"that", "the", "their", "then", "there", "these",
				"they", "this", "to", "was", "will", "with"
				);

		String[] words = inputString.split(" ");
		ArrayList<String> wordsList = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();

		for(String word : words)
		{
			String wordCompare = word.toLowerCase();
			if(!stopWordsList.contains(wordCompare))
			{
				wordsList.add(word);
			}
		}

		for (String str : wordsList){
			sb.append(str + " ");
		}

		return sb.toString().replaceAll("[0-9]","").replaceAll("\\s{2,}", " ").trim();

	}

}