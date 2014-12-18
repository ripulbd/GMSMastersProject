package uni.glas.cs.test.extractkeywords;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class Utilities {
	public static String stemmize(String term) throws IOException {

	    // tokenize term
	    TokenStream tokenStream = new ClassicTokenizer(Version.LUCENE_CURRENT,  new StringReader(term));
	    // stemmize
	    tokenStream = new PorterStemFilter(tokenStream);

	    Set<String> stems = new HashSet<String>();
	    CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);
	    // for each token
	    while (tokenStream.incrementToken()) {
	        // add it in the dedicated set (to keep unicity)
	        stems.add(token.toString());
	    }

	    // if no stem or 2+ stems have been found, return null
	    if (stems.size() != 1) {
	        return null;
	    }

	    String stem = stems.iterator().next();

	    // if the stem has non-alphanumerical chars, return null
	    if (!stem.matches("[\\w-]+")) {
	        return null;
	    }

	    return stem;
	}

	public static <T> T find(Collection<T> collection, T example) {
	    for (T element : collection) {
	        if (element.equals(example)) {
	            return element;
	        }
	    }
	    collection.add(example);
	    return example;
	}
}
