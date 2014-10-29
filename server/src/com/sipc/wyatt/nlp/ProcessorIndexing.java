package com.sipc.wyatt.nlp;

import java.util.ArrayList;



public class ProcessorIndexing {

	public ProcessorIndexing() {

	}
	
	/*
	 * Remove html tag
	 * @param .html text
	 * @return string which has been removed
	 */
	public String removeHTML(String text) {
		return text.replaceAll("<[^>]*>", "");
	}
	
	/*
	 * Tokenize a String into a ArrayList of String
	 * @param text The String to tokenize
	 */
	public ArrayList<String> tokenizing(String text) {
		text = removeHTML(text);
		ArrayList<String> returnTe = new ArrayList<String>();
		StringBuffer tok = new StringBuffer();

		for(int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if(Character.isLetterOrDigit(ch)) {
				tok.append(ch);
			}
			else if(tok.length() > 0) {
				returnTe.add(tok.toString());
				tok = new StringBuffer();
			}
		}
		return returnTe;
	}
	
	/*
	 * Remove stop words
	 * @param text A ArrayList of String
	 * @return text The ArrayList to be removed stop words
	 */
	public ArrayList<String> stopping(ArrayList<String> text) {
		String[] stopList = Stopper.getStopList();
		for(int k = 0; k < text.size(); k++) {
			String str = (String)text.get(k);
//		for(String str : text) {
			for(int i = 0; i < stopList.length; i++) {
				if(str.equals(stopList[i])) {
					text.remove(str);
				}
			}
		}
		return text;
	}
	
	/*
	 * Discard
	 * Replaced by Stemmer
	 */
	public ArrayList<String> stemming(ArrayList<String> text) {
		return text;
	}
}