package uni.gla.cs;
//Source: src/main/java/com/mycompany/myapp/summarizers/LuceneSummarizer.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;




/**
* Lucene based summarizer. Tokenizes a document into paragraphs and
* paragraphs into sentences, then builds a in-memory lucene index for
* the document with sentences as fields in single-field Lucene 
* documents with index time boosts specified by the paragraph and
* sentence number. Extracts the top terms from the in-memory index
* and issue a Boolean OR query to the index with these terms, then
* return the top few sentences found ordered by Lucene document id.
*/
public class LuceneSummarizer {

private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
private int numSentences = 2;
private float topTermCutoff;
// these two values are used to implement a simple linear deboost. If 
// a different algorithm is desired, these variables are likely to be
// no longer required.
private float sentenceDeboost;
private float sentenceDeboostBase = 0.5F;

private ParagraphTokenizer paragraphTokenizer;
private SentenceTokenizer sentenceTokenizer;

/**
* Allows setting a custom analyzer. Default is StandardAnalyzer
* if not specified.
* @param analyzer the analyzer to set.
*/
public void setAnalyzer(Analyzer analyzer) {
 this.analyzer = analyzer;
}

/**
* The number of sentences required in the summary. Default is 2.
* @param numSentences the number of sentences in summary.
*/
public void setNumSentences(int numSentences) {
 this.numSentences = numSentences;
}

/**
* This value specifies where to cutoff the term list for query.
* The text is loaded into an in-memory index, a sentence per
* Lucene Document. Then the index is queried for terms and their
* associated frequency in the index. The topTermCutoff is a 
* ratio from 0 to 1 which specifies how far to go down the 
* frequency ordered list of terms. The terms considered have 
* a frequency greater than topTermCutoff * topFrequency. 
* @param topTermCutoff a ratio specifying where the term list
*        will be cut off. Must be between 0 and 1. Default is
*        to consider all terms if this variable is not set,
*        ie topTermCutoff == 0. But it is recommended to set
*        an appropriate value (such as 0.5). 
*/
public void setTopTermCutoff(float topTermCutoff) {
 if (topTermCutoff < 0.0F || topTermCutoff > 1.0F) {
   throw new IllegalArgumentException(
     "Invalid value: 0.0F <= topTermCutoff <= 1.0F");
 }
 this.topTermCutoff = topTermCutoff;
}

/**
* Applies a index-time deboost to the sentences after the first
* one in all the paragraphs after the first one. This attempts to
* model the summarization heuristic that a summary can be generated
* by reading the first paragraph (in full) of a document, followed
* by the first sentence in every succeeding paragraph. The first 
* paragraph is not deboosted at all. For the second and succeeding
* paragraphs, the deboost is calculated as (1 - sentence_pos * deboost)
* until the value reaches sentenceDeboostBase (default 0.5) or less, 
* and then no more deboosting occurs. 
* @param sentenceDeboost the deboost value to set. Must be between 
*        0 and 1. Default is no deboosting, ie sentenceDeboost == 0.
*/
public void setSentenceDeboost(float sentenceDeboost) {
 if (sentenceDeboost < 0.0F || sentenceDeboost > 1.0F) {
   throw new IllegalArgumentException(
     "Invalid value: 0.0F <= sentenceDeboost <= 1.0F");
 }
 this.sentenceDeboost = sentenceDeboost;
}

/**
* This parameter is used in conjunction with sentenceDeboost. This
* value defines the base until which deboosting will occur and then
* stop. Default is set to 0.5 if not set. Must be between 0 and 1.
* @param sentenceDeboostBase the sentenceDeboostBase to set.
*/
public void setSentenceDeboostBase(float sentenceDeboostBase) {
 if (sentenceDeboostBase < 0.0F || sentenceDeboostBase > 1.0F) {
   throw new IllegalArgumentException(
     "Invalid value: 0.0F <= sentenceDeboostBase <= 1.0F");
 }
 this.sentenceDeboostBase = sentenceDeboostBase;
}

/**
* The init method pre-instantiates the Paragraph and Sentence tokenizers
* both of which are based on ICU4J RuleBasedBreakIterators, so they are
* expensive to set up, therefore we set them up once and reuse them.
* @throws Exception if one is thrown.
*/
public void init() throws Exception {
 this.paragraphTokenizer = new ParagraphTokenizer();
 this.sentenceTokenizer = new SentenceTokenizer();
}

/**
* This is the method that will be called by a client after setting up
* the summarizer, configuring it appropriately by calling the setters,
* and calling init() on it to instantiate its expensive objects.
* @param text the text to summarize. At this point, the text should
*        be plain text, converters ahead of this one in the chain
*        should have done the necessary things to remove HTML tags,
*        etc.
* @return the summary in the specified number of sentences. 
* @throws Exception if one is thrown.
*/
public String summarize(String text) throws Exception {
 RAMDirectory ramdir = new RAMDirectory();
 buildIndex(ramdir, text);
 Query topTermQuery = computeTopTermQuery(ramdir);
 String[] sentences = searchIndex(ramdir, topTermQuery);
 return StringUtils.join(sentences, " ... ");
}

/**
* Builds an in-memory index of the sentences in the text with the
* appropriate document boosts if specified.
* @param ramdir the RAM Directory to use.
* @param text the text to index.
* @throws Exception if one is thrown.
*/
private void buildIndex(Directory ramdir, String text) throws Exception {
 if (paragraphTokenizer == null || sentenceTokenizer == null) {
   throw new IllegalArgumentException(
     "Please call init() to instantiate tokenizers");
 }
 IndexWriter writer = new IndexWriter(ramdir, analyzer,
   MaxFieldLength.UNLIMITED);
 paragraphTokenizer.setText(text);
 String paragraph = null;
 int pno = 0;
 while ((paragraph = paragraphTokenizer.nextParagraph()) != null) {
   sentenceTokenizer.setText(paragraph);
   String sentence = null;
   int sno = 0;
   while ((sentence = sentenceTokenizer.nextSentence()) != null) {
     Document doc = new Document();
     doc.add(new Field("text", sentence, Store.YES, Index.ANALYZED));
     doc.setBoost(computeDeboost(pno, sno));
     writer.addDocument(doc);
     sno++;
   }
   pno++;
 }
 writer.commit();
 writer.close();
}

/**
* Applies a linear deboost function to simulate the manual heuristic of
* summarizing by skimming the first few sentences off a paragraph.
* @param paragraphNumber the paragraph number (0-based).
* @param sentenceNumber the sentence number (0-based).
* @return the deboost to apply to the current document.
*/
private float computeDeboost(int paragraphNumber, int sentenceNumber) {
 if (paragraphNumber > 0) {
   if (sentenceNumber > 0) {
     float deboost = 1.0F - (sentenceNumber * sentenceDeboost);
     return (deboost < sentenceDeboostBase) ? 
       sentenceDeboostBase : deboost; 
   }
 }
 return 1.0F;
}

/**
* Computes a term frequency map for the index at the specified location.
* Builds a Boolean OR query out of the "most frequent" terms in the index 
* and returns it. "Most Frequent" is defined as the terms whose frequencies
* are greater than or equal to the topTermCutoff * the frequency of the
* top term, where the topTermCutoff is number between 0 and 1.
* @param ramdir the directory where the index is created.
* @return a Boolean OR query.
* @throws Exception if one is thrown.
*/
private Query computeTopTermQuery(Directory ramdir) throws Exception {
 final Map<String,Integer> frequencyMap = 
   new HashMap<String,Integer>();
 List<String> termlist = new ArrayList<String>();
 IndexReader reader = IndexReader.open(ramdir);
 TermEnum terms = reader.terms();
 while (terms.next()) {
   Term term = terms.term();
   String termText = term.text();
   int frequency = reader.docFreq(term);
   frequencyMap.put(termText, frequency);
   termlist.add(termText);
 }
 reader.close();
 // sort the term map by frequency descending
 //YUAN Hide
 Collections.sort(termlist, new ReverseComparator(new ByValueComparator<String,Integer>(frequencyMap)));
  
 // retrieve the top terms based on topTermCutoff
 List<String> topTerms = new ArrayList<String>();
 float topFreq = -1.0F;
 for (String term : termlist) {
   if (topFreq < 0.0F) {
     // first term, capture the value
     topFreq = (float) frequencyMap.get(term);
     topTerms.add(term);
   } else {
     // not the first term, compute the ratio and discard if below
     // topTermCutoff score
     float ratio = (float) ((float) frequencyMap.get(term) / topFreq);
     if (ratio >= topTermCutoff) {
       topTerms.add(term);
     } else {
       break;
     }
   }
 }
 StringBuilder termBuf = new StringBuilder();
 BooleanQuery q = new BooleanQuery();
 for (String topTerm : topTerms) {
   termBuf.append(topTerm).
     append("(").
     append(frequencyMap.get(topTerm)).
     append(");");
   q.add(new TermQuery(new Term("text", topTerm)), Occur.SHOULD);
 }
 System.out.println(">>> top terms: " + termBuf.toString());
 System.out.println(">>> query: " + q.toString());
 return q;
}

/**
* Executes the query against the specified index, and returns a bounded
* collection of sentences ordered by document id (so the sentence ordering
* is preserved in the collection).
* @param ramdir the directory location of the index.
* @param query the Boolean OR query computed from the top terms.
* @return an array of sentences.
* @throws Exception if one is thrown.
*/
private String[] searchIndex(Directory ramdir, Query query) 
   throws Exception {
 SortedMap<Integer,String> sentenceMap = 
   new TreeMap<Integer,String>();
 IndexSearcher searcher = new IndexSearcher(ramdir);
 TopDocs topDocs = searcher.search(query, numSentences);
 for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
   int docId = scoreDoc.doc;
   Document doc = searcher.doc(docId);
   sentenceMap.put(scoreDoc.doc, StringUtils.chomp(doc.get("text")));
 }
 searcher.close();
 return sentenceMap.values().toArray(new String[0]);
}
}