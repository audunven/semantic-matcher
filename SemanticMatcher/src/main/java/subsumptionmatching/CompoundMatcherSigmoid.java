package subsumptionmatching;

import java.io.File;
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
import utilities.Sigmoid;
import utilities.StringUtilities;
import utilities.WordNet;

/**
 * CompoundMatcherSigmoid identifies subsumption relations based on so-called compounds, that is, a word comprised of individual words (e.g. electronicBook)
 * Inspired by the Compound Strategy described in "Arnold, Patrick, and Erhard Rahm. "Enriching ontology mappings with semantic relations." Data & Knowledge Engineering 93 (2014): 1-18".
 * @author audunvennesland
 *
 */
public class CompoundMatcherSigmoid extends ObjectAlignment implements AlignmentProcess {

	//these attributes are used to calculate the weight associated with the matcher's confidence value
	double profileScore;
	int slope;
	double rangeMin;
	double rangeMax;

	OWLOntology sourceOntology;
	OWLOntology targetOntology;

	public CompoundMatcherSigmoid(double profileScore) {
		this.profileScore = profileScore;
	}


	public CompoundMatcherSigmoid(OWLOntology onto1, OWLOntology onto2, double profileScore, int slope, double rangeMin, double rangeMax) {
		this.sourceOntology = onto1;
		this.targetOntology = onto2;
		this.profileScore = profileScore;
		this.slope = slope;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	

	public static URIAlignment returnCMAlignment (File ontoFile1, File ontoFile2, double profileScore, int slope, double rangeMin, double rangeMax) throws OWLOntologyCreationException, AlignmentException {

		URIAlignment CMAlignment = new URIAlignment();

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology onto1 = manager.loadOntologyFromOntologyDocument(ontoFile1);
		OWLOntology onto2 = manager.loadOntologyFromOntologyDocument(ontoFile2);

		AlignmentProcess a = new CompoundMatcherSigmoid(onto1, onto2, profileScore, slope, rangeMin, rangeMax);
		a.init(ontoFile1.toURI(), ontoFile2.toURI());
		Properties params = new Properties();
		params.setProperty("", "");
		a.align((Alignment)null, params);	
		BasicAlignment CompoundMatcherSigmoidAlignment = new BasicAlignment();

		CompoundMatcherSigmoidAlignment = (BasicAlignment) (a.clone());

		CMAlignment = CompoundMatcherSigmoidAlignment.toURIAlignment();

		CMAlignment.init( onto1.getOntologyID().getOntologyIRI().toURI(), onto2.getOntologyID().getOntologyIRI().toURI(), A5AlgebraRelation.class, BasicConfidence.class );

		return CMAlignment;

	}

	public void align( Alignment alignment, Properties param ) throws AlignmentException {

		//System.out.println("\nStarting Compound Matcher...");
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
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&lt;", 
									Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else if (numModifiers == 2) {
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&lt;", 
									Sigmoid.weightedSigmoid(slope, 0.75, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else {

							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&lt;", 
									Sigmoid.weightedSigmoid(slope, 0.5, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						}
					}

					//if s2´s compound head (Research[Project]) equals the full name of s1 (Project): source > target
					else if (isCompoundRelation(target, source)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(target)).size();

						if (numModifiers == 1) {
							
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&gt;", 
									Sigmoid.weightedSigmoid(slope, 1.0, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else if (numModifiers == 2) {
							
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&gt;", 
									Sigmoid.weightedSigmoid(slope, 0.75, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else {
							
							//using sigmoid function to compute confidence
							 addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&gt;", 
							Sigmoid.weightedSigmoid(slope, 0.50, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						}

					}

					//if any synonym of the compound head of s1 (Research[Undertaking]) equals the full name of s2 (Project): source < target
					else if(isCompoundSynRelation(source, target)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(source)).size();

						if (numModifiers == 1) {							
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&lt;", 
									Sigmoid.weightedSigmoid(slope, 0.75, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else if (numModifiers == 2) {
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&lt;", 
									Sigmoid.weightedSigmoid(slope, 0.5, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));
						} else {
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&lt;", 
									Sigmoid.weightedSigmoid(slope, 0.25, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						}
					}

					//if any synonyms of the compound head of s2 (Research[Undertaking]) equals the full name of s1 (Project): source > target
					else if(isCompoundSynRelation(target, source)) {

						numModifiers = StringUtilities.getWordsAsSetFromCompound(StringUtilities.getCompoundModifier(target)).size();

						if (numModifiers == 1) {
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&gt;", 
									Sigmoid.weightedSigmoid(slope, 0.75, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else if (numModifiers == 2) {							
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&gt;", 
									Sigmoid.weightedSigmoid(slope, 0.5, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax))); 
						} else {
							//using sigmoid function to compute confidence
							addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "&gt;", 
									Sigmoid.weightedSigmoid(slope, 0.25, Sigmoid.transformProfileWeight(profileScore, rangeMin, rangeMax)));  
						}

					}
					//if none of the patterns above fits...
					else {
						addAlignCell("CompoundMatcherSigmoid" +idCounter, sourceObject,targetObject, "!", 0.0); 
					}

				}
			}

		} catch (Exception e) { e.printStackTrace(); }

		//long endTime = System.currentTimeMillis();
		//System.out.println("Compound Matcher completed in " + (endTime - startTime) / 1000 + " seconds.");
	}

	

	public static boolean isCompoundRelation(String a, String b) {
		boolean test = false;

		String[] compounds = StringUtilities.getCompoundParts(a);

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