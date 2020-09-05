package evaluation.general;

import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import utilities.StringUtilities;

public class ComputeSyntacticEvaluationScores {
	
	public static void main(String[] args) throws AlignmentException, URISyntaxException {

		String alignmentFileName = "./files/_PHD_EVALUATION/ATMONTO-AIRM/ALIGNMENTS/COMPETITION/EVALUATION_COMPETITION/SUBSUMPTION_EQUIVALENCE/Blooms_wiki_ATMONTO-AIRM_EQ-SUB.rdf";
		String referenceAlignmentFileName = "./files/_PHD_EVALUATION/ATMONTO-AIRM/REFALIGN/ReferenceAlignment-ATMONTO-AIRM-EQ-SUB.rdf";
		
		AlignmentParser candAlignParser = new AlignmentParser(0);
		AlignmentParser refAlignParser = new AlignmentParser(0);
		
		Alignment candidateAlignment = candAlignParser.parse(new URI(StringUtilities.convertToFileURL(alignmentFileName)));
		Alignment referenceAlignment = refAlignParser.parse(new URI(StringUtilities.convertToFileURL(referenceAlignmentFileName)));
		
		
		EvaluationScore score = getSyntacticEvaluationScore(candidateAlignment,referenceAlignment);
		
		System.out.println("Precision: " + score.getPrecision());
		System.out.println("Recall: " + score.getRecall());
		System.out.println("F-measure: " + score.getfMeasure());
	}
	
	/**
	 * Returns an EvaluationScore object that measures the precision, recall and F-measure of a candidate alignment against a reference alignment. The method considers the type of relation.
	 * @param candidateAlignment the alignment being evaluated
	 * @param referenceAlignment the alignment holding the correct set of relations.
	 * @return EvaluationScore
	 * @throws AlignmentException
	   Aug 13, 2020
	 */
	public static EvaluationScore getSyntacticEvaluationScore (Alignment candidateAlignment, Alignment referenceAlignment) throws AlignmentException {
		
		EvaluationScore score = new EvaluationScore();
		
		int numRelsInCandidate = candidateAlignment.nbCells();
				int numRelsInReference = referenceAlignment.nbCells();
				
		int numTP = 0;
		
		for (Cell rac : referenceAlignment) {
			for (Cell ac : candidateAlignment) {
				
				if (rac.getObject1AsURI().equals(ac.getObject1AsURI()) 
						&& rac.getObject2AsURI().equals(ac.getObject2AsURI()) 
						&& rac.getRelation().getRelation().equals(ac.getRelation().getRelation())) {
					numTP++;
				}
				
			}
		}
				
		double precision = (double)numTP / (double)numRelsInCandidate;
		double recall = (double)numTP / (double)numRelsInReference;
		double fmeasure = 2*(precision*recall) / (precision+recall);
		
		score.setPrecision(precision);
		score.setRecall(recall);
		score.setfMeasure(fmeasure);
		
		return score;

	}

}
