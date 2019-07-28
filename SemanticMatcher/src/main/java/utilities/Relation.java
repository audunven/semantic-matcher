package utilities;

import java.util.Comparator;

/**
 * A class that defines a semantic relation through the two concepts involved in the relation, the type of relation and the confidence measure associated with the relation
 * Implements Comparable in order to sort relations.
 * @author audunvennesland
 *
 */
public class Relation implements Comparable<Relation> {

	private String id;
	private String concept1;
	private String concept2;
	private String relationType;
	private double confidence;
	private int sigmoidSlope;

	/**
	 * Constructor holding params for a semantic relation
	 * @param id the unique identifier of a relation (cell)
	 * @param concept1 source concept
	 * @param concept2 target concept
	 * @param relationType type of relation
	 * @param confidence the confidence value assigned by a matcher
	 */
	public Relation(String id, String concept1, String concept2, String relationType, double confidence) {
		super();
		this.id = id;
		this.concept1 = concept1;
		this.concept2 = concept2;
		this.relationType = relationType;
		this.confidence = confidence;
	}
	
/*
	public Relation(String id, String concept1, String concept2, String relationType, double confidence, int sigmoidSlope) {
		super();
		this.id = id;
		this.concept1 = concept1;
		this.concept2 = concept2;
		this.relationType = relationType;
		this.confidence = confidence;
		this.sigmoidSlope = sigmoidSlope;
	}*/

	public Relation() {}

	public String getConcept1() {
		return concept1;
	}
	
	public void setId (String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	//TODO: Should implement functionality for getting the fragment after "/" as well
	public String getConcept1Fragment() {
		return concept1.substring(concept1.lastIndexOf("#") +1);
	}
	//TODO: Should implement functionality for getting the fragment after "/" as well
	public String getConcept2Fragment() {
		return concept2.substring(concept2.lastIndexOf("#") +1);
	}

	public void setConcept1(String concept1) {
		this.concept1 = concept1;
	}

	public String getConcept2() {
		return concept2;
	}

	public void setConcept2(String concept2) {
		this.concept2 = concept2;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	public void setSigmoidSlope(int sigmoidSlope) {
		this.sigmoidSlope = sigmoidSlope;
	}
	
	public int getSigmoidSlope () {
		return sigmoidSlope;
	}


	public int compareTo(Relation rel) {
		double otherConfidence = rel.confidence;
		
		if (confidence == otherConfidence) 
			return 0;
		else if (confidence > otherConfidence)
			return -1;
		else 
			return 1;
	}
	
	public String toString() {
		return this.id + " - " + this.getConcept1Fragment() + " - " + this.getConcept2Fragment() + "  " + this.getRelationType() + " " + this.getConfidence();
	}


}
