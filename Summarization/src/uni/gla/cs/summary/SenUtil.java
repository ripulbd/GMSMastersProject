package uni.gla.cs.summary;


import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import uni.gla.cs.SentenceTokenizer;
import uni.glas.cs.test.extractkeywords.Core;
import uni.glas.cs.test.extractkeywords.Keyword;

public class SenUtil {
	
	@Test
	public void test() throws IOException
	{
//		Map<String, Integer> senMap= segString("i love bananas and apples,bananas are very healthy");
//		System.out.println(senMap);
		List<String> senS=new ArrayList<String>();
		senS.add("Bin Laden is believed to be living in Afghanistan.");
		senS.add("Bin Laden, a Saudi exile, is thought to be living in Afghanistan.");
		senS.add("Bin Laden, a Saudi exile, is thought to be living in Afghanistan.");
	    senS.add("bananas are very healthy");
		senS.add("apples are very healthy");
		senS.add("list has an order");
		senS.add("cat do not love  and apples,bananas are very healthy");
		senS.add("list has an order");
		senS.add("cat love  and apples, bananas  healthy");
		senS.add("Bin Laden, a Saudi exile, is thought to be living in Afghanistan.");
		removeRedundancySen(senS);
//		System.out.println(senS);
	}
	/**
	 * http://blog.csdn.net/hughdove/article/details/6621632
	 * @param sentence
	 * @return
	 */
		public int getWordNum(String sentence) {
			Pattern expression = Pattern.compile("[a-zA-Z]+");// 
			String string1 = sentence.toString().toLowerCase();// 
			Matcher matcher = expression.matcher(string1);// 
			TreeMap myTreeMap = new TreeMap();// 
			int n = 0;//word num
			Object word = null;// words in the chapter
			Object num = null;// occurrence
			while (matcher.find()) {// whether match
				word = matcher.group();// 
				n++;// 
				if (myTreeMap.containsKey(word)) {//
					num = myTreeMap.get(word);// occurance
					Integer count = (Integer) num;
					myTreeMap.put(word, new Integer(count.intValue() + 1));
				} else {
					myTreeMap.put(word, new Integer(1));
				}
			}

//			System.out.println("word num:" + n);
			return n;
		}
		/**
		 * delete the sentences which contain few words
		 * @param senList
		 * @return
		 */
		public List<String> removeSmallSen(List<String> senList)
		{
			List<String> senListNew=new ArrayList<String>();
			for (String sen : senList) {
				int wordNum=this.getWordNum(sen);
				if (wordNum>=Config.senLength) {
					senListNew.add(sen);
				}
			}
			return senListNew;
			
			
		}
		/**
		 * Rs=2*(overlapping words)/(words in sentence1 and words in sentence2)
		 * @param senList
		 * @return 
		 * @throws IOException
		 */
/**
 * Calculate the overlap of the two sentences
 * @param s1
 * @param s2
 * @return
 * @throws IOException
 */
		public double difference(String s1, String s2) throws IOException
		{
			double Rs;
			Map<String, Integer> s1Map= segString(s1);
			Map<String, Integer> s2Map= segString(s2);
			int overlapword=0;
			for(String word:s1Map.keySet())
			{
				if (s2Map.containsKey(word)) {
					overlapword++;
				}
			}
			Rs=(double) (2*overlapword/(new Double(s1Map.size()+s2Map.size())));
			return Rs;
		}
		
		/**
		 * remove the sentences has high overlap based on Rs value
		 * @param senList
		 * @return
		 * @throws IOException
		 */
		public List<String>  removeRedundancySen(List<String> senList) throws IOException
		{
			Double Rs=0.0;
			List<String> senListNew=new ArrayList<String>();
			List<Integer> indexToRemove=new ArrayList<Integer>();
			for (int i = 0; i < senList.size(); i++) {
				String sen=senList.get(i);
				Boolean is = false;
				for (int j = i+1; j < senList.size(); j++) {
					String sen_com=senList.get(j);
					if (difference(sen,sen_com)>Config.redThreshold) {						
					    is = true;
					}
				}
				if (!is) {
					senListNew.add(sen);
				}				
			}
			
			System.out.println(senListNew);
			return senListNew;
		}
		/**
		 * @throws IOException
		 * 
		 * @Title: segString
		 * @Description: extract keyword
		 * @param @param content
		 * @param @return Map<String, Integer>
		 * @return Map<String,Integer>
		 * @throws
		 */
		public  Map<String, Integer> segString(String content)
				throws IOException {

			Map<String, Integer> words = new LinkedHashMap<String, Integer>();
			List<Keyword> keywords = Core.guessFromString(content);
			for (int i = 0; i < keywords.size(); i++) {
				words.putAll(keywords.get(i).getHashMap());
			}
			return words;
		}
/**
 * GET the sentence list
 * @param content
 * @return
 * @throws Exception
 */
	public List<String> getSens(String content) throws Exception {
		List<String> sens = new ArrayList<String>();
		SentenceTokenizer sentenceTokenizer = new SentenceTokenizer();
		sentenceTokenizer.setText(content);
		String sentence;
		while ((sentence = sentenceTokenizer.nextSentence()) != null) {
			String sen = sentence.trim();
			sens.add(sen);
		}
		return sens;
	}
	
	/**
	 * Output the top sentences
	 * @param allsens
	 * @throws IOException
	 */
	public void OutputSen(List<String> allsens) throws IOException
	{
		FileWriter fr = new FileWriter(Config.outputfile);
		for (int i = 0; i < allsens.size(); i++) {
			if (i >= Config.senNumCentroid) {
				break;
			}
			fr.write(allsens.get(i));
			fr.write("\n");
		}
		fr.close();
	}

		

}
