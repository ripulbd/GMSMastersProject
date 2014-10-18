package com.sipc.wyatt.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CollectionStatistics {
	/*
	 * get statistics from collection
	 * @param ExCollection
	 */
//	private static Map<String, Integer> term = new HashMap<>();

	private ExCollection collection;
	
	public CollectionStatistics(ExCollection collection) {
		this.collection = collection;
	}
	
	/*
	 * Sort term according to frequency
	 * @param <term, frequency>
	 * @return sorted <term, frequency> list
	 */
	public List<Entry<String, Integer> > sortTerm(Map<String, Integer> term) {
		List<Map.Entry<String, Integer> >list = new ArrayList<Map.Entry<String, Integer> >(term.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer> >(){

			@Override
			public int compare(Entry<String, Integer> o1,
					Entry<String, Integer> o2) {
				// TODO Auto-generated method stub
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		return list;
	}

	public void calNumber() {
		for(ExDocument doc : collection.getDocument()) {
			doc.calTF();
			Map<String, Integer> term = doc.getTermFrequency();
//			List<Entry<String, Integer> > sortedTerm = sortTerm(term);

			collection.updateTF(term);
//			System.out.println(sortedTerm);
		}
	}

	public ExCollection getCollection() {
		return collection;
	}
}
