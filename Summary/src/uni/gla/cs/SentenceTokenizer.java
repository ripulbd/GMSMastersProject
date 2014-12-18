package uni.gla.cs;

import java.io.File;
import java.text.BreakIterator;

import org.apache.commons.io.FileUtils;

import com.ibm.icu.text.RuleBasedBreakIterator;

/**
 * Tokenizes the input into sentences. Uses ICU4J's RuleBasedBreakIterator
 * with rule file adapted from a dump of RBBI.sentenceInstance.
 */
public class SentenceTokenizer {

  private String text;
  private int index = 0;
  private RuleBasedBreakIterator breakIterator;
  
  public SentenceTokenizer() throws Exception {
    super();
    this.breakIterator = new RuleBasedBreakIterator(
      FileUtils.readFileToString(
//      new File("src/main/resources/sentence_break_rules.txt"), "UTF-8"));
    new File("src\\test\\resources\\config\\sentence_break_rules.txt"), "UTF-8"));
  }
  
  public void setText(String text) {
    this.text = text;
    this.breakIterator.setText(text);
    this.index = 0;
  }
  
  public String nextSentence() {
    int end = breakIterator.next();
    if (end == BreakIterator.DONE) {
      return null;
    }
    String sentence = text.substring(index, end);
    index = end;
    return sentence;
  }
}