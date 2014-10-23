package uni.glas.cs.test.extractkeywords;

import java.io.IOException;
import java.util.List;
/**
 * 
 * @author fajie extact keywords
 *
 */
public class KeywordExtract {
	
	public static void main(String[] args) throws IOException {
		String input = "mongkey likes bananas but dog really hates bananas the the a the the these where  those apples apple";
		List<Keyword> keywords = Core.guessFromString(input);
		System.out.println(keywords);
	}
}
