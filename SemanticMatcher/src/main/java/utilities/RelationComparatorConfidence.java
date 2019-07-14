package utilities;

import java.util.Comparator;

public class RelationComparatorConfidence implements Comparator<Relation> {


	@Override
	public int compare(Relation rel1, Relation rel2) {
		if (rel1.getConfidence() > rel2.getConfidence()) 
			return -1;
		if (rel1.getConfidence() < rel2.getConfidence())
			return 1;
		return 0;
		
	}

}

//StringAndDateComparator implements Comparator<MyObject> {
//
//	   public int compare(MyObject first, MyObject second) {
//	        int result = first.getString().compareTo(second.getString());
//	        if (result != 0) {
//	            return result;
//	        }
//	        else {
//	            return first.getDate().compareTo(second.getDate());
//	        }
//	}

