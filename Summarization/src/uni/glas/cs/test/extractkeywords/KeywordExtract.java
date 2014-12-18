package uni.glas.cs.test.extractkeywords;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 
 * @author fajie extact keywords
 *
 */
public class KeywordExtract {
	
	public static void main(String[] args) throws IOException {
		String input = "mongkey likes bananas but dog really hates bananas the the a the the these where  those apples apple, but bananas also";
		List<Keyword> keywords = Core.guessFromString(input);
		 Map<String, Integer> words = new HashMap<String, Integer>();
		
		 for (int i=0;i<keywords.size();i++){
		        words.putAll(keywords.get(i).getHashMap());
		        }
		 System.out.println(words);	
}
}
