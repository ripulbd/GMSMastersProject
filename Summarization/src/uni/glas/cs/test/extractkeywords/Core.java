package uni.glas.cs.test.extractkeywords;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.neo4j.impl.shell.apps.Set;

import uni.gla.cs.PorterStemAnalyzer;



public class Core {
	public static List<Keyword> guessFromString(String input) throws IOException {

	    // hack to keep dashed words (e.g. "non-specific" rather than "non" and "specific")
	    input = input.replaceAll("-+", "-0");
	    // replace any punctuation char but dashes and apostrophes and by a space
	    input = input.replaceAll("[\\p{Punct}&&[^'-]]+", " ");
	    // replace most common english contractions
	    input = input.replaceAll("(?:'(?:[tdsm]|[vr]e|ll))+\\b", "");

	    // tokenize input
	    TokenStream tokenStream = new ClassicTokenizer(Version.LUCENE_CURRENT, new StringReader(input));
	    // to lower case
	    tokenStream = new LowerCaseFilter(Version.LUCENE_CURRENT, tokenStream);
	    // remove dots from acronyms (and "'s" but already done manually above)
	    tokenStream = new ClassicFilter(tokenStream);
	    // convert any char to ASCII
	    tokenStream = new ASCIIFoldingFilter(tokenStream);
	    // remove english stop words
		String[] stopList = PorterStemAnalyzer.STOP_WORDS;
		HashSet<String> stopSets = new HashSet<String>();
		for (String string : stopList) {
			stopSets.add(string);
		}
//	    tokenStream = new StopFilter(Version.LUCENE_CURRENT, tokenStream, EnglishAnalyzer.getDefaultStopSet());
	    tokenStream = new StopFilter(Version.LUCENE_CURRENT, tokenStream, stopSets);

	    List<Keyword> keywords = new LinkedList<Keyword>();
	    CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);

	    // for each token
	    while (tokenStream.incrementToken()) {
	        String term = token.toString();
	        // stemmize
	        String stem =Utilities.stemmize(term);
	        if (stem != null) {
	            // create the keyword or get the existing one if any
	            Keyword keyword = Utilities.find(keywords, new Keyword(stem.replaceAll("-0", "-")));
	            // add its corresponding initial token
	            keyword.add(term.replaceAll("-0", "-"));
	        }
	    }
	    // reverse sort by frequency
	    Collections.sort(keywords);
	    return keywords;
	}


}
