package subsumptionmatching;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import rita.wordnet.jwnl.JWNLException;
import utilities.LexicalConcept;
import utilities.Sigmoid;
import utilities.WordNet;

public class LexicalSubsumptionMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	//static RiWordNet database = new RiWordNet("/Users/audunvennesland/Documents/PhD/Development/WordNet/WordNet-3.0/dict");
	Map<String, Double> matchingMap = new HashMap<String, Double>();
	OWLOntology sourceOntology;
	OWLOntology targetOntology;

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;


	public LexicalSubsumptionMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, double profileScore, int slope, double rangeMin, double rangeMax) {
		this.sourceOntology = onto1;
		this.targetOntology = onto2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	
	
	public static URIAlignment returnLSMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment LSMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new LexicalSubsumptionMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment LexicalSubsumptionMatcherSigmoidAlignment = new BasicAlignment();

		LexicalSubsumptionMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		LSMAlignment = LexicalSubsumptionMatcherSigmoidAlignment.toURIAlignment();

		LSMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return LSMAlignment;

	}

	public void align( Alignment alignment, Properties param ) throws AlignmentException {

		//System.out.println("\nStarting Lexical Subsumption Matcher...");
		long startTimeMatchingProcess = System.currentTimeMillis();

		LexicalConcept sourceLexicalConcept = new LexicalConcept();
		LexicalConcept targetLexicalConcept = new LexicalConcept();

		Set<String> hyponyms = new HashSet<String>();

		Map<String, LexicalConcept> onto1LexicalMap = new HashMap<String, LexicalConcept>();
		Map<String, LexicalConcept> onto2LexicalMap = new HashMap<String, LexicalConcept>();

		String lexicalName = null;

		try {
			for (Object source : ontology1().getClasses()) {
				//the lexical name is created by adding whitespace between compound words and using only lowercase
				lexicalName = WordNet.getLexicalName(ontology1().getEntityName(source)).toLowerCase();
				if (WordNet.containedInWordNet(lexicalName)) {
					hyponyms = WordNet.getHyponymsAsSet(lexicalName);
					//remove the whitespace added above before adding the lexical name to the LexicalConcept object (needed when comparing with the ontology concept and when computing the Resnik similarity)
					sourceLexicalConcept = new LexicalConcept(lexicalName.replace(" ", ""), ontology1().getEntityURI(source), hyponyms);
					onto1LexicalMap.put(lexicalName.replace(" ", ""), sourceLexicalConcept);
				}
			}
		} catch (OntowrapException | IOException | JWNLException e3 ) {
			e3.printStackTrace();
		} 

		try {
			for (Object target : ontology2().getClasses()) {
				//the lexical name is created by adding whitespace between compound words and using only lowercase
				lexicalName = WordNet.getLexicalName(ontology2().getEntityName(target)).toLowerCase();
				if (WordNet.containedInWordNet(lexicalName)) {
					hyponyms = WordNet.getHyponymsAsSet(lexicalName);
					//remove the whitespace added above before adding the lexical name to the LexicalConcept object (needed when comparing with the ontology concept and when computing the Resnik similarity)
					targetLexicalConcept = new LexicalConcept(lexicalName.replace(" ", ""), ontology2().getEntityURI(target), hyponyms);
					onto2LexicalMap.put(lexicalName.replace(" ", ""), targetLexicalConcept);
				}
			}
		} catch (OntowrapException | IOException | JWNLException e3 ) {
			e3.printStackTrace();
		} 

		String sourceEntity = null;
		String targetEntity = null;

		//required to have their representation without lowercase for the compound analysis
		String sourceEntityNormalCase = null;
		String targetEntityNormalCase = null;

		Set<String> hyponymsSource = new HashSet<String>();
		Set<String> hyponymsTarget = new HashSet<String>();

		double wordNetSimScore = 0;

		//just to have a unique name for the matcher (ID)
		int idCounter = 0;

		try {

			for ( Object source: ontology1().getClasses() ){
				for ( Object target: ontology2().getClasses() ){

					idCounter++;

					//get the entity names for source and target to make the code more readable
					sourceEntity = ontology1().getEntityName(source).toLowerCase();
					targetEntity = ontology2().getEntityName(target).toLowerCase();
					sourceEntityNormalCase = ontology1().getEntityName(source);
					targetEntityNormalCase = ontology2().getEntityName(target);


					//if both concepts are in WordNet, we compare their hyponyms and their Resnik similarity
					if (onto1LexicalMap.containsKey(sourceEntity) && onto2LexicalMap.containsKey(targetEntity)) {
						//get the hyponyms of source and target entities
						hyponymsSource = onto1LexicalMap.get(sourceEntity).getHyponyms();
						hyponymsTarget = onto2LexicalMap.get(targetEntity).getHyponyms();

						//measure the Resnik similarity between the source and target concepts
						wordNetSimScore = WordNet.computeResnik(onto1LexicalMap.get(sourceEntity).getLexicalConceptName(), onto2LexicalMap.get(targetEntity).getLexicalConceptName());

					}

					//if either hyponym set is empty -> score is 0
					if ((hyponymsSource == null || hyponymsSource.isEmpty()) || (hyponymsTarget == null || hyponymsTarget.isEmpty())) {
						addAlignCell("LexicalSubsumptionMatcherSigmoid" +idCounter, source, target, "=", 0);
					}


					else 

					{
						//if the full source is a part of the set of hyponyms of target and source and target are semantically similar (according to Resnik): source < target and score 1.0
						if ((hyponymsTarget.contains(sourceEntity) || hyponymsTarget.contains(sourceEntityNormalCase)) && wordNetSimScore > 0.75) {				
							addAlignCell("LexicalSubsumptionMatcherSigmoid" +idCounter, source, target, "&lt;", 
									Sigmoid.weightedSigmoid(slope, wordNetSimScore, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
						}

						//if the full target is a part of the set of hyponyms of source and source and target are semantically similar (according to Resnik): source > target and score 1.0
						else if ((hyponymsSource.contains(targetEntity) || hyponymsSource.contains(targetEntityNormalCase)) && wordNetSimScore > 0.75) {					
							addAlignCell("LexicalSubsumptionMatcherSigmoid" +idCounter, source, target, "&gt;", 
									Sigmoid.weightedSigmoid(slope, wordNetSimScore, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
						}


						else {

							addAlignCell("LexicalSubsumptionMatcherSigmoid" +idCounter, source, target, "=", 0);
						}
					}
				}

			}

		} catch (Exception e) { e.printStackTrace(); }

		long endTimeMatchingProcess = System.currentTimeMillis();

		//System.out.println("The matching operation took " + (endTimeMatchingProcess - startTimeMatchingProcess) / 1000 + " seconds.");
	}


	

}


