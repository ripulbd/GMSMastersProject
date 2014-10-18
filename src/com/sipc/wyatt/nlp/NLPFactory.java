package com.sipc.wyatt.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sipc.wyatt.dao.Dao;

public class NLPFactory {
	private ArrayList<String> tokenList = new ArrayList<String>();

	public NLPFactory() {
		
	}
	
	public ArrayList<String> init(String[] todoName) {
//		ArrayList<String> textList = Dao.getNewsInfo(VarDao.DESCRIPTION);
		return Dao.getNewsInfo(todoName);
	}
	
	/*
	 * Indexing the text
	 */
	public ArrayList<ArrayList<String> > start(String[] todoName) {
		ArrayList<String> textList = init(todoName);
		ArrayList<ArrayList<String> > arrList = getTSSList(textList);
		ArrayList<ArrayList<String> > returnList = new ArrayList<ArrayList<String> >();
		
		// Initialize collection
		ExCollection collection = new ExCollection(arrList);
		CollectionStatistics cs = new CollectionStatistics(collection);
		cs.calNumber();
		calTfIdf(collection);
		for(ExDocument doc : collection.getDocument()) {
			ArrayList<String> list = findKeywords(doc, NlpConfig.NUMOFKEYWORDS);
//			System.out.println(doc.getDocID() + " " + list);
			returnList.add(list);
		}
		return returnList;
	}
	
	/*
	 * Calculate tf-idf and find keywords, store in ExDocument.tfidf
	 * @param ExCollection
	 */
	public void calTfIdf(ExCollection collection) {
		for(ExDocument doc : collection.getDocument()) {
			Map<String, Integer> tf = doc.getTermFrequency();
			Map<String, Double> tfIdf = new HashMap<>();
			for(Map.Entry<String, Integer> entry : tf.entrySet()) {
				String key = entry.getKey();
				int value = entry.getValue();
				double idf = collection.getIDF(key);
				tfIdf.put(key, idf*value);
			}
			List<Entry<String, Double> > sortedTerm = sortTerm(tfIdf);
			doc.setTfIdf(tfIdf);
		}
	}
	
	/*
	 * Find keywords of document
	 * @param ExDocument, numWords
	 * @return ArrayList<String>
	 */
	public ArrayList<String> findKeywords(ExDocument document, int numWords) {
		ArrayList<String> returnList = new ArrayList<String>();
		int size = document.getTfIdf().size();
		int start = size/2-numWords/2;

		if(numWords > size) return null;
		while(start+numWords > size && start >= 0) {
			start--;
		}
		int index = 0;
		for(Map.Entry<String, Double> entry : document.getTfIdf().entrySet()) {
			if(index >= start+numWords) break;
			if(index >= start) {
				returnList.add(entry.getKey());
			}
			index++;
		}
		return returnList;
	}
	
	/*
	 * Sort term according to frequency
	 * @param <term, frequency>
	 * @return sorted <term, frequency> list
	 */
	public List<Entry<String, Double> > sortTerm(Map<String, Double> term) {
		List<Map.Entry<String, Double> >list = new ArrayList<Map.Entry<String, Double> >(term.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double> >(){

			@Override
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2) {
				// TODO Auto-generated method stub
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		return list;
	}
	
	/*
	 * get words with tokenizing, stopping, stemming
	 * @param ArrayList<String> textList Need te tokenizing, stopping, stemming
	 * @return ArrayList<ArrayList<String> >
	 */
	public ArrayList<ArrayList<String> > getTSSList(ArrayList<String> textList) {
		ProcessorIndexing pi = new ProcessorIndexing();
		Stemmer stemmer = new Stemmer();
		ArrayList<String> stemmedList = null;
		ArrayList<ArrayList<String> > returnList = new ArrayList<ArrayList<String> >();

		/*
		 * text Each document in textList
		 */
		for(String text : textList) {
			stemmedList = new ArrayList<String>();

			tokenList = pi.tokenizing(text);
			tokenList = pi.stopping(tokenList);
			for(String term : tokenList) {
				term = stemmer.stemming(term);
				if(term != null) {
					stemmedList.add(term);
				}
			}
			returnList.add(stemmedList);
		}
		return returnList;
	}
}
