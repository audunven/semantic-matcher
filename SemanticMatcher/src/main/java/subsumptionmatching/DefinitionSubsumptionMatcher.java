package subsumptionmatching;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import utilities.StringUtilities;

public class DefinitionSubsumptionMatcher extends ObjectAlignment implements AlignmentProcess {

	OWLOntology sourceOntology;
	OWLOntology targetOntology;

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;

	static Map<String, List<String>> defMapSource = new HashMap<String, List<String>>();
	static Map<String, List<String>> defMapTarget = new HashMap<String, List<String>>();

	public DefinitionSubsumptionMatcher(OWLOntology onto1, OWLOntology onto2, double profileScore) {
		this.sourceOntology = onto1;
		this.targetOntology = onto2;
		this.profileScore = profileScore;
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

		double testWeight = 1.0;

		AlignmentProcess a = new DefinitionSubsumptionMatcher(sourceOntology, targetOntology, testWeight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment definitionsSubsumptionMatcherAlignment = new BasicAlignment();

		definitionsSubsumptionMatcherAlignment = (BasicAlignment) (a.clone());
		
		System.out.println("Number of relations in the 0.0 alignment: " + definitionsSubsumptionMatcherAlignment.nbCells());

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

	public static URIAlignment returnDSMAlignment (File ontoFile1, File ontoFile2, double weight) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment DSMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new DefinitionSubsumptionMatcher(onto1, onto2, weight);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment definitionSubsumptionMatcherAlignment = new BasicAlignment();

		definitionSubsumptionMatcherAlignment = (BasicAlignment) (a.clone());

		DSMAlignment = definitionSubsumptionMatcherAlignment.toURIAlignment();

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

								//if any of the compounds in source and target are from the same domain according to WNDomain, we return a confidence of 1.0
								if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject)) 
										&& !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&gt;", 1.0 * profileScore);
									
									//else we return a confidence of 0.75								
								} else if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject)) 
										|| !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
									
									//System.out.println(source + " and " + target + " are not from the same domain OR they belong to a meronym relationship!");
									
									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&gt;", 0.75 * profileScore);

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
									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&lt;", 1.0 * profileScore);
								
									//else we return a confidence of 0.75
								} else if (ConfirmSubsumption.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))
										|| !ConfirmSubsumption.isMeronym(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
									
									//System.out.println(source + " and " + target + " are not from the same domain OR they belong to a meronym relationship!");
									
									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&lt;", 0.75 * profileScore);
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
	
//	public void align(Alignment alignment, Properties param) throws AlignmentException {
//
//		try {
//			defMapSource = getDefMapTokens(sourceOntology);
//		} catch (JWNLException | IOException e1) {
//			e1.printStackTrace();
//		}
//		try {
//			defMapTarget = getDefMapTokens(targetOntology);
//		} catch (JWNLException | IOException e1) {
//			e1.printStackTrace();
//		}
//
//
//		List<String> sourceDefinition = null;
//		List<String> targetDefinition = null;
//
//		int idCounter = 0; 
//
//		try {
//			// Match classes
//			for ( Object sourceObject: ontology1().getClasses() ) {
//				for ( Object targetObject: ontology2().getClasses() ){
//
//					idCounter++;
//
//					String source = ontology1().getEntityName(sourceObject).toLowerCase();
//					String target = ontology2().getEntityName(targetObject).toLowerCase();
//
//					if (defMapSource.containsKey(source)) {
//
//						sourceDefinition = defMapSource.get(source);
//
//						//iterate all sourceDefinition tokens and check if they match the target concept and if these are from the same domain
//						for (String s : sourceDefinition) {
//
//							if (s.equalsIgnoreCase(target)) {
//
//								//if any of the compounds in source and target are from the same domain according to WNDomain, we return a confidence of 1.0
//								if (WNDomain.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {
//									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&gt;", 1.0 * profileScore);
//									
//									//else we return a confidence of 0.75								
//								} else {
//									
//									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&gt;", 0.75 * profileScore);
//
//								} 
//
//							} else {
//								addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
//							}
//
//						}
//
//
//					} else if (defMapTarget.containsKey(target)) {
//
//						targetDefinition = defMapTarget.get(target);
//
//						//iterate all targetDefinition tokens and check if they match the source concept and if these are from the same domain
//						for (String t : targetDefinition) {
//
//							if (t.equalsIgnoreCase(source)) {
//								
//								//if any of the compounds in source and target are from the same domain according to WNDomain, we return a confidence of 1.0
//								if (WNDomain.conceptsFromSameDomain(ontology1().getEntityName(sourceObject), ontology2().getEntityName(targetObject))) {							
//									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&lt;", 1.0 * profileScore);
//								
//									//else we return a confidence of 0.75
//								} else {
//									addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "&lt;", 0.75 * profileScore);
//								}
//							
//							}
//							
//							else {
//								addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
//							}
//						}
//
//
//					} else {
//						addAlignCell("DefinitionSubsumptionMatcher" +idCounter + "_" + profileScore + "_", sourceObject, targetObject, "!", 0.0);
//					}
//
//
//				}
//
//			}
//
//		} catch (Exception e) { e.printStackTrace(); }
//
//	}


	
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
				if (extract.contains(".")) {
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