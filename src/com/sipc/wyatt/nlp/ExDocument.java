package com.sipc.wyatt.nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ExDocument {
	private int docID;
	private Map<String, Integer> termFrequency = null;
	private Map<String, Double> tfIdf = null;
	private ArrayList<String> text = null;
	
	public ExDocument(int docID, ArrayList<String> text) {
		termFrequency = new HashMap<>();
		tfIdf = new HashMap<>();
		this.setDocID(docID);
		this.text = text;
	}
	
	/*
	 * Calculate TF weighting, sort it in Map<String, Integer> termFrequency
	 */
	public void calTF() {
		for(String doc : text) {
			/*
			 * For each text
			 */
			if(termFrequency.get(doc) == null) {
				termFrequency.put(doc, 1);
			}
			else {
				termFrequency.put(doc, termFrequency.get(doc)+1);
			}
		}
	}
	

	/*
	 * Whether it has termFrequency or not
	 * @param termFrequency
	 * @return true if doc has or false
	 */
	public boolean hasTerm(String value) {
		return termFrequency.get(value) == null ? false : true;
	}
	
	public Map<String, Integer> getTermFrequency() {
		return termFrequency;
	}

	public void setTermFrequency(Map<String, Integer> termFrequency) {
		this.termFrequency = termFrequency;
	}

	public int getDocID() {
		return docID;
	}

	public void setDocID(int docID) {
		this.docID = docID;
	}

	public ArrayList<String> getText() {
		return text;
	}

	public void setText(ArrayList<String> text) {
		this.text = text;
	}

	public Map<String, Double> getTfIdf() {
		return tfIdf;
	}

	public void setTfIdf(Map<String, Double> tfIdf) {
		this.tfIdf = tfIdf;
	}
	
	public void setTfIdf(List<Entry<String, Double> > tfIdf) {
		for(Map.Entry<String, Double> map : tfIdf) {
			this.tfIdf.put(map.getKey(), map.getValue());
		}
	}
}
