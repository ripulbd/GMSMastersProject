package com.sipc.wyatt.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExCollection {
	public static int numDoc = 0;

	private ArrayList<ExDocument> document = null;
	private Map<String, Integer> termFrequency = null;
	
	public ExCollection(ArrayList<ArrayList<String> > arrList) {
		document = new ArrayList<ExDocument>();
		termFrequency = new HashMap<>();

		for(ArrayList<String> list : arrList) {
			document.add(new ExDocument(numDoc, list));
			numDoc++;
		}
	}

	/*
	 * Update tf
	 * @param tf of document
	 */
	public void updateTF(Map<String, Integer> tf) {
		for(Map.Entry<String, Integer> entry : tf.entrySet()) {
			String key = entry.getKey();
			Integer value = entry.getValue();
			
			if(termFrequency.get(key) == null) {
				termFrequency.put(key, value);
			}
			else {
				termFrequency.put(key, termFrequency.get(key)+value);
			}
		}
	}

	/*
	 * Calculate idf weighting
	 * @param term
	 * @return idf weighting
	 */
	public double getIDF(String term) {
		int dk = 0;		// dk means number of documents containing dk word
		for(ExDocument doc : document) {
			if(doc.hasTerm(term)) {
				dk++;
			}
		}
		return Math.log(((numDoc-dk)+0.5)/(dk+0.5));
	}
	
	public ArrayList<ExDocument> getDocument() {
		return document;
	}

	public void setDocument(ArrayList<ExDocument> document) {
		this.document = document;
	}

	public Map<String, Integer> getTermFrequency() {
		return termFrequency;
	}

	public void setTermFrequency(Map<String, Integer> termFrequency) {
		this.termFrequency = termFrequency;
	}
}
