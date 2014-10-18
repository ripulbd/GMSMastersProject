package com.sipc.wyatt.nlp;

import org.tartarus.snowball.ext.PorterStemmer;

public class Stemmer {
	PorterStemmer stemmer = null;

	public Stemmer() {
		stemmer = new PorterStemmer();
	}

	public String stemming(String term) {
		stemmer.setCurrent(term);
		stemmer.stem();
		return stemmer.getCurrent().length() > 0 ? stemmer.getCurrent() : null;
	}
}
