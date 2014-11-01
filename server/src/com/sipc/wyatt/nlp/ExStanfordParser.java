package com.sipc.wyatt.nlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class ExStanfordParser {
	public ArrayList<String> getNameEntities(String text) throws ClassCastException,
			ClassNotFoundException, IOException {
		ArrayList<String> neList = new ArrayList<String>();
		String serializedClassifier = "classifiers/english.all.3class.distsim.crf.ser.gz";

		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier
				.getClassifier(serializedClassifier);

		/*
		 * For either a file to annotate or for the hardcoded text text, this
		 * demo file shows two ways to process the output, for teaching
		 * purposes. For the file, it shows both how to run NER on a String and
		 * how to run it on a whole file. For the hard-coded String, it shows
		 * how to run it on a single sentence, and how to do this and produce an
		 * inline XML output format.
		 */
		String classifiedString = classifier.classifyWithInlineXML(text);
		System.out.println(classifiedString);
		String pattern = "<([^<>]+)>([^<>]+)</\\1>";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(classifiedString);
		while(m.find()) {
			neList.add(m.group(1));
			neList.add(m.group(2));
			System.out.println(m.group(1));
			System.out.println(m.group(2));
		}
		return neList;
	}
}
