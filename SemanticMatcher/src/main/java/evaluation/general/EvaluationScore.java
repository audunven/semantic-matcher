package evaluation.general;

/**
 * An object representing an evaluation score.
 * @author audunvennesland
 * @see Evalutor
 */
public class EvaluationScore {
	
	private double precision;
	private double recall;
	private double fMeasure;
	
	public EvaluationScore() {}
	
	public EvaluationScore(double precision, double recall, double fMeasure) {
		precision = this.precision;
		recall = this.recall;
		fMeasure = this.fMeasure;
	}

	public double getPrecision() {
		return precision;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public double getRecall() {
		return recall;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	public double getfMeasure() {
		return fMeasure;
	}

	public void setfMeasure(double fMeasure) {
		this.fMeasure = fMeasure;
	}
	
	

}