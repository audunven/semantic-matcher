package utilities;

import java.util.Comparator;

public class RelationComparator implements Comparator<Relation> {


	@Override
	public int compare(Relation rel1, Relation rel2) {
		int result = rel1.getConcept1Fragment().compareTo(rel2.getConcept1Fragment());
		if (result != 0) {
			return result;
		} else {
			return rel1.getConcept2Fragment().compareTo(rel2.getConcept2Fragment());
		}
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

