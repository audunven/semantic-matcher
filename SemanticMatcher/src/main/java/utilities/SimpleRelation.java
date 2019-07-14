package utilities;

public class SimpleRelation {
	
	private String source;
	private String target;
	private String relation;
	private double confidence;
	
	public SimpleRelation(String source, String target, String relation, double confidence) {
		super();
		this.source = source;
		this.target = target;
		this.relation = relation;
		this.confidence = confidence;
	}
	
	
	
	public String getSource() {
		return source;
	}



	public void setSource(String source) {
		this.source = source;
	}



	public String getTarget() {
		return target;
	}



	public void setTarget(String target) {
		this.target = target;
	}



	public SimpleRelation() {}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	
	
	

}
