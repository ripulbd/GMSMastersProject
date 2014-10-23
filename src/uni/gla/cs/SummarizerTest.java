package uni.gla.cs;

import java.io.File;

import net.sf.classifier4J.summariser.ISummariser;
import net.sf.classifier4J.summariser.SimpleSummariser;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class SummarizerTest {
	public static void main(String[] args) throws Exception {
		testC4JSummarizer();
		testLuceneSummarizer();
	}

  private static String[] TEST_FILES = {
//    "src\\test\\resources\\data\\nytimes-obama.txt",
//    "src\\test\\resources\\data\\glasgow.txt",
//      "src\\test\\resources\\data\\xijinping.txt",
    "src\\test\\resources\\data\\test.txt"
  };
  
  @Test
  public static void testC4JSummarizer() throws Exception {
    for (String testFile : TEST_FILES) {
      String text = FileUtils.readFileToString(new File(testFile), "UTF-8");
      ISummariser summarizer = new SimpleSummariser();
//      System.out.println("Input: " + testFile);
      String summary = summarizer.summarise(text, 2);
      // replace newlines with ellipses
      summary = summary.replaceAll("\n+", "...");
      System.out.println(">>> Summary (from C4J): " + summary);
    }
  }

  @Test
  public static void testLuceneSummarizer() throws Exception {
    for (String testFile : TEST_FILES) {
      String text = FileUtils.readFileToString(new File(testFile), "UTF-8");
      LuceneSummarizer summarizer = new LuceneSummarizer();
      summarizer.setAnalyzer(new SummaryAnalyzer());
//      summarizer.setNumSentences(2);//Original para
      summarizer.setNumSentences(4);
      summarizer.setTopTermCutoff(0.5F);
      summarizer.setSentenceDeboost(0.2F);
      summarizer.init();
//      System.out.println("Input: " + testFile);
      String summary = summarizer.summarize(text);
      System.out.println(
        ">>> Summary (from LuceneSummarizer): " + summary);
    }
  }
}