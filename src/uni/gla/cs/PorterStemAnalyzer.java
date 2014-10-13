package uni.gla.cs;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * PorterStemAnalyzer processes input
 * text by stemming English words to their roots.
 * This Analyzer also converts the input to lower case
 * and removes stop words.  A small set of default stop
 * words is defined in the STOP_WORDS
 * array, but a caller can specify an alternative set
 * of stop words by calling non-default constructor.
 */
public class PorterStemAnalyzer extends Analyzer {
    private Set<?> stopSet;

    /**
     * An array containing some common English words
     * that are usually not useful for searching.
     */
    public static final String[] STOP_WORDS =
    {
        "0", "1", "2", "3", "4", "5", "6", "7", "8",
        "9", "000", "$",
        "about", "after", "all", "also", "an", "and",
        "another", "any", "are", "as", "at", "be",
        "because", "been", "before", "being", "between",
        "both", "but", "by", "came", "can", "come",
        "could", "did", "do", "does", "each", "else",
        "for", "from", "get", "got", "has", "had",
        "he", "have", "her", "here", "him", "himself",
        "his", "how","if", "in", "into", "is", "it",
        "its", "just", "like", "make", "many", "me",
        "might", "more", "most", "much", "must", "my",
        "never", "now", "of", "on", "only", "or",
        "other", "our", "out", "over", "re", "said",
        "same", "see", "should", "since", "so", "some",
        "still", "such", "take", "than", "that", "the",
        "their", "them", "then", "there", "these",
        "they", "this", "those", "through", "to", "too",
        "under", "up", "use", "very", "want", "was",
        "way", "we", "well", "were", "what", "when",
        "where", "which", "while", "who", "will",
        "with", "would", "you", "your",
        "a", "b", "c", "d", "e", "f", "g", "h", "i",
        "j", "k", "l", "m", "n", "o", "p", "q", "r",
        "s", "t", "u", "v", "w", "x", "y", "z",
        "PK_OBJECT"
    };

    /**
     * Builds an analyzer.
     */
    public PorterStemAnalyzer() {
   
        this(STOP_WORDS);
    }

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param stopWords a String array of stop words
     */
    @SuppressWarnings("deprecation")
	public PorterStemAnalyzer(String[] stopWords) {
        stopSet = StopFilter.makeStopSet(stopWords);
    }


//    /**
//     * Builds an analyzer with the stop words from the given file.
//     * 
//     * @see WordlistLoader#getWordSet(File)
//     */
//    public PorterStemAnalyzer(File stopwords) throws IOException {
//        stopSet = WordlistLoader.getWordSet(stopwords);
//    }
//
    /** Constructs a {@link StandardTokenizer} filtered by a {@link
        StandardFilter}, a {@link LowerCaseFilter}, a {@link StopFilter} 
        and a {@link PorterStemFilter}. */
        @SuppressWarnings("deprecation")
		public TokenStream tokenStream(String fieldName, Reader reader) {
            StandardTokenizer tokenStream = new StandardTokenizer(Version.LUCENE_CURRENT, reader);
            tokenStream.setMaxTokenLength(StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
            TokenStream result = new StandardFilter(tokenStream);
            result = new LowerCaseFilter(result);
            result = new StopFilter(Version.LUCENE_CURRENT, result, stopSet);
            result = new PorterStemFilter(result);
            return result;
        }
}
