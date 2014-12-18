package uni.gla.cs.summary;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import uni.glas.cs.test.extractkeywords.Core;
import uni.glas.cs.test.extractkeywords.Keyword;

public class ScoreOfSentence {   
    /**
	 * all idf result.key:sen,value:idf
	 */
    public static Map<String, Double> idfMap = new LinkedHashMap<String, Double>();  
//    
    /**
     * keyword contaion, contain keywords  key:keyword  value:number
     */
    public static  Map<String, Integer> containWordOfAllDocNumberMap=new LinkedHashMap<String, Integer>();
	
	 /**
		 * http://blog.csdn.net/hughdove/article/details/6621632
		 * @param sentence
		 * @return
		 */
			public int getWordNum(String sentence) {
				Pattern expression = Pattern.compile("[a-zA-Z]+");// 
				String string1 = sentence.toString().toLowerCase();// 
				Matcher matcher = expression.matcher(string1);// 
				TreeMap<Object, Integer> myTreeMap = new TreeMap<Object, Integer>();// 
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

				return n;
			}
	 
		/**
		 * delete the sentences which contain few words
		 * @param senList
		 * @return
		 */
		public  List<String> removeSmallSen(List<String> senList)
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
		public double difference(String s1, String s2) throws IOException{
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
			return senListNew;
		}
	
    /**
     * @throws IOException 
     * 
    * @Title: segString
    * @Description: extract keyword
    * @param @param content
    * @param @return  Map<String, Integer>  
    * @return Map<String,Integer>   
    * @throws
     */
    public Map<String, Integer> segString(String content) throws IOException{

        Map<String, Integer> words = new LinkedHashMap<String, Integer>();
        List<Keyword> keywords =Core.guessFromString(content);
        for (int i=0;i<keywords.size();i++){
        words.putAll(keywords.get(i).getHashMap());
        }
      return words;
    }
    
   
    /**
     * 
    * @Title: tf
    * @Description: normalizing tf(w,d) = count(w, d) / size(d)
    * @param @param segWordsResult
    * @param @return    
    * @return HashMap<String,Double>   
    * @throws
     */
    public Map<String, Double> tf(Map<String, Integer> segWordsResult) { 
    	
        HashMap<String, Double> tf = new LinkedHashMap<String, Double>(); 
        if(segWordsResult==null || segWordsResult.size()==0){
    		return tf;
    	}
        Double size=Double.valueOf(segWordsResult.size());
        Set<String> keys=segWordsResult.keySet();
        for(String key: keys){
        	Integer value=segWordsResult.get(key);
        	tf.put(key, Double.valueOf(value)/size);
        }
        return tf;  
    }  
    
    /**
     * 
    * @Title: allTf
    * @Description: calculate all tf
    * @param @param dir
    * @param @return Map<String, Map<String, Double>>
    * @return Map<String,Map<String,Double>>   
    * @throws
     */
    public Map<String, Map<String, Double>> allTf(List<String> DocList){
    	Map<String, Map<String, Double>> allTfMap = new LinkedHashMap<String, Map<String, Double>>();
    	try{   		
    		for(String Doc : DocList){
    			Map<String, Integer> segs=segString(Doc);
  			    
    			allTfMap.put(Doc, tf(segs));
    		}
    	}catch(FileNotFoundException ffe){
    		ffe.printStackTrace();
    	}catch(IOException io){
    		io.printStackTrace();
    	}
    	return allTfMap;
    }
        
    
    
    /**
     * 
    * @Title: wordSegCount
    * @Description: store terms in LinkedHashMap
    * @param @param sen
    * @param @return    
    * @return Map<String,Map<String,Integer>>   
    * @throws
     */
    public Map<String, Map<String, Integer>> wordSegCount(List<String> DocList){
    	Map<String, Map<String, Integer>> allSegsMap = new LinkedHashMap<String, Map<String, Integer>>();
    	try{
    		for(String Doc : DocList){
    			Map<String, Integer> segs=segString(Doc);   			
  			    allSegsMap.put(Doc, segs);
    		}
    	}catch(FileNotFoundException ffe){
    		ffe.printStackTrace();
    	}catch(IOException io){
    		io.printStackTrace();
    	}
    	return allSegsMap;
    }
    
    
    /**
     * 
    * @Title: containWordOfAllDocNumber
    * @Description: the number of sec which contain the keyword  key:keyword  value:number
    * @param @param allSegsMap
    * @param @return    
    * @return Map<String,Integer>   
    * @throws
     */
    public static Map<String, Integer> containWordOfAllDocNumber(Map<String, Map<String, Integer>> allSegsMap){  	
    	
    	if(allSegsMap==null || allSegsMap.size()==0){
    		return containWordOfAllDocNumberMap;
    	}
    	
    	Set<String> fileList=allSegsMap.keySet();
    	for(String filePath: fileList){
    		Map<String, Integer> fileSegs=allSegsMap.get(filePath);
    		if(fileSegs==null || fileSegs.size()==0){
    			continue;
    		}
    		Set<String> segs=fileSegs.keySet();
    		for(String seg : segs){
    			if (containWordOfAllDocNumberMap.containsKey(seg)) {
    				containWordOfAllDocNumberMap.put(seg, containWordOfAllDocNumberMap.get(seg) + 1);
                } else {
                	containWordOfAllDocNumberMap.put(seg, 1);
                }
    		}
    		
    	}
    	return containWordOfAllDocNumberMap;
    }
    
    /**
     * 
    * @Title: idf
    * @Description: idf = log(n / docs(w, D)) 
    * @param @param containWordOfAllDocNumberMap
    * @param @return Map<String, Double> 
    * @return Map<String,Double>   
    * @throws
     */
    public static Map<String, Double> idf(Map<String, Map<String, Integer>> allSegsMap){   	
    	if(allSegsMap==null || allSegsMap.size()==0){
    		return idfMap;
    	}
    	containWordOfAllDocNumberMap=containWordOfAllDocNumber(allSegsMap);
    	Set<String> words=containWordOfAllDocNumberMap.keySet();
    	Double DocSize=Double.valueOf(allSegsMap.size());
    	for(String word: words){
    		Double number=Double.valueOf(containWordOfAllDocNumberMap.get(word));
    		idfMap.put(word, Math.log(DocSize/(number+1.0d)));
    	}
    	return idfMap;
    }
    
    /**
     * 
    * @Title: tfIdf
    * @Description: tf-idf
    * @param @param tf,idf
    * @return Map<String, Map<String, Double>>   
    * @throws
     */
    public Map<String, Map<String, Double>> tfIdf(Map<String, Map<String, Double>> allTfMap,Map<String, Double> idf){
    	Map<String, Map<String, Double>> tfIdfMap = new LinkedHashMap<String, Map<String, Double>>();  
    	Set<String> fileList=allTfMap.keySet();
     	for(String filePath : fileList){
    		Map<String, Double> tfMap=allTfMap.get(filePath);
    		Map<String, Double> docTfIdf=new LinkedHashMap<String,Double>();
    		Set<String> words=tfMap.keySet();
    		for(String word: words){
    			Double tfValue=Double.valueOf(tfMap.get(word));
        		Double idfValue=idf.get(word);
        		docTfIdf.put(word, tfValue*idfValue);
    		}
    		tfIdfMap.put(filePath, docTfIdf);
    	}    	
    	return tfIdfMap;
    }
    

	
	//delete tag <>
	public String fiterHtmlTag(String str, String tag, String replace) {   
		 Pattern pattern = Pattern.compile(tag);   
	     Matcher matcher = pattern.matcher(str);   
	     StringBuffer sb = new StringBuffer();   
	     boolean result1 = matcher.find();   
	     while (result1) {   
	         matcher.appendReplacement(sb, replace);   
	         result1 = matcher.find();   
	     }   
	     matcher.appendTail(sb);   
	     return sb.toString();
		}
	
	//get docmap<topiclist,docID> from json
	public  Map<String,List<String>> getdocmap(String filepath){	    
		BufferedReader reader = null;
		String laststr = "";
		List<Map> list=new ArrayList<Map>();
		try{				
			FileInputStream fileInputStream = new FileInputStream(filepath);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			reader = new BufferedReader(inputStreamReader);
			String tempString = null;	
			
			while((tempString = reader.readLine()) != null){
				laststr += tempString;
				Map<String, String> result = new LinkedHashMap<String, String>();
				JSONObject  jsonobject=new JSONObject(tempString);
		        Iterator<String> iterator = jsonobject.keys();
		        String key = null;
		        String value = null;		        
		        while (iterator.hasNext()) {
		            key = iterator.next();
		            value = jsonobject.getString(key);
		            result.put(key, value);
		            
		        }
		        list.add(result);
			}			
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}			
				
		Map <String, List<String>> getdocmap=new LinkedHashMap<String, List<String>>();
		for(int i=0;i<list.size();i++){
			String id=(String) list.get(i).get("id");
			String path=(String) list.get(i).get("path");
			String[] patharrray=path.split(";");
			for (int j=0;j<patharrray.length;j++){
				if(getdocmap.containsKey(patharrray[j])){
					List<String> doclist=getdocmap.get(patharrray[j]);
					doclist.add(id);						
					getdocmap.put(patharrray[j], doclist);						
				}else{
					List<String> doclist=new ArrayList<String>();
					doclist.add(id);
					getdocmap.put(patharrray[j], doclist);
				}
			}				
		}
		return getdocmap;
}
	
	
	
	//according to the ID, collect the information from mongodb
	 public  Map<String, List<Map<String, String>>> DocMap (Map <String, List<String>> doc) throws Exception{
		 
		 Map<String, List<Map<String, String>>> DocMap=new LinkedHashMap<String, List<Map<String, String>>>();
		 try{   
	 		 // To connect to mongodb server
	          MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
	          // Now connect to your databases
	          DB db = mongoClient.getDB( "demo" );
	          	 		 
	 		DBCollection collection = db.getCollection("records");	 		 
	 		BasicDBObject query =new BasicDBObject(); 	 			 		
	 		Set<String> segs=doc.keySet();

	 		
	 		
    		for(String seg : segs){
    			List <String> docNum=doc.get(seg);
    			List <Map<String, String>> doclist=new ArrayList<Map<String, String>>();
				List <String> sentence=new ArrayList<String>();
				List <String> story=new ArrayList<String>();
	 		    for(String number: docNum){ 
	 		    ObjectId objid=new ObjectId(number);
	 		    query.put("_id",objid ); 
		 		DBCursor cursor = collection.find(query);		 		
		 		String title = null;
		 		String description=null;
		 		String mainStory = null;
		 		
				while(cursor.hasNext()) {
					Map line = (Map) cursor.next();
					 title=(String)line.get("title");
					 description=(String)line.get("description");
					 mainStory=(String) line.get("mainStory");
					 
				}
							
				String str1=fiterHtmlTag(mainStory, "<([^>]*)>", "");				
				String str2=fiterHtmlTag(str1, "&quot;", "\"");
				String str3=fiterHtmlTag(str2, "&pound;", "");

				Map <String, String> docInfor=new LinkedHashMap<String, String>();
				docInfor.put("title",title);
				docInfor.put("description",description);
				docInfor.put("mainStory",str3);
				doclist.add(docInfor);
		 		}
		 		DocMap.put(seg, doclist); 		  
    		  }
		
		 }catch(Exception e){
	 	     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	 	   
	 	     }		 
		return DocMap;
	    }

	 
	 public  List <String> Doclist (Map<String, List<Map<String, String>>> docmap){
		List<String> DocList = new ArrayList<String>();
		Set<String> topiclist=docmap.keySet();
		for(String topic:topiclist){
			List<Map<String, String>> doclist=docmap.get(topic);
			for(Map<String, String>doc:doclist){
				DocList.add(doc.get("mainStory"));				
			}			
		}		 
		return DocList;	 
	 }
	 
	 public Map<List<String>,String> mainStoryMap (Map <String, List<Map<String, String>>> docmap){
		Map<List<String>,String> mainStoryMap=new LinkedHashMap<List<String>,String>();
		Set<String> topiclist=docmap.keySet();
		for(String topic:topiclist){
			List<String> mainStoryList=new ArrayList<String>();
			List<Map<String, String>> doclist=docmap.get(topic);
			for(Map<String, String>doc:doclist){
				mainStoryList.add(doc.get("mainStory"));				
			}
			mainStoryMap.put(mainStoryList, topic);
		}	
		return mainStoryMap; 	 
	 }
	
	 public Map<String,List<String>> SentenceList (Map<String, List<Map<String, String>>> docmap){
		Map<String,List <String>> SentenceList = new LinkedHashMap<String,List <String>>();
		Set<String> topiclist=docmap.keySet();
		for(String topic:topiclist){
			List<Map<String, String>> doclist=docmap.get(topic);
			List <String> sentence=new ArrayList<String>();
			for(Map<String, String>doc:doclist){
				String str2=doc.get("mainStory");	
				// To get sentence
				 Pattern p =Pattern.compile("[?.!]");
			     Matcher m = p.matcher(str2);
			     String[] substrs = p.split(str2);  
			     if(substrs.length > 0)   
			     {   
			         int count = 0;   
			         while(count < substrs.length)   
			         {   
			             if(m.find())   
			             {   
			            	 substrs[count] += m.group();   
			             }   
			             count++;   
			         }   
			     }   
			    
	             List<String> l=new ArrayList <String>();
	             Collections.addAll(l,substrs);
	             for (int i=0;i<l.size();i++){
		              if (l.get(i).equals("\"")){
			          l.set(i-1,l.get(i-1)+l.get(i));
			          l.remove(i);
			          i=i-1;
		              }
	             }	             
				sentence.addAll(l);
			}
			SentenceList.put(topic, sentence);
		} 
		return SentenceList;
	 }
	 
	 
	 
	 
	 //calculate average tfidf for each topic
	 public Map<String,Map<String, Double>> TopicTfidf(Map<String, Map<String, Double>> tfidfmap, Map<List<String>,String> mainstorymap){
		 Map<String,Map<String, Double>> TopicTfidf = new LinkedHashMap<String,Map<String, Double>>();
		 Set<String> docset=tfidfmap.keySet();
		 Set<List<String>> doclist=mainstorymap.keySet();
		 
		 for (List<String> doc:doclist){
			 Map<String, Double> wordtfidf=new LinkedHashMap<String, Double>();
			 for (String d:docset){	
				 if (doc.contains(d)){
					 Set<String> word=tfidfmap.get(d).keySet();
					 Map<String, Double> m=tfidfmap.get(d);
					 List<String> w=new ArrayList<String>();
					 w.addAll(word);					 
					 for(int i=0;i<w.size();i++){						 
						 if(wordtfidf.containsKey(w.get(i))){
							 wordtfidf.put(w.get(i), (m.get(w.get(i))+wordtfidf.get(w.get(i)))/2);								 
							 m.remove(w.get(i));							 								 
					      }
					 }
					 wordtfidf.putAll(m); 				 
				 }				 				 
			 }			 
			 TopicTfidf.put(mainstorymap.get(doc), wordtfidf);
		 }		 
		 return TopicTfidf;	 
	 }
	 
     //caculate the score for each sentence in topic
	 public Map<String,Map<String, Double>> sentencerank(Map<String,Map<String, Double>> topictfidf, Map<String,List <String>> sentencelist ) throws IOException{
		 Map<String,Map<String, Double>> SentenceRank = new LinkedHashMap<String,Map<String, Double>>();
		 Set<String> topicset=sentencelist.keySet();
		 for(String topic:topicset){
			 List<String> list=sentencelist.get(topic);
			 //remove small sentence
			 List<String>removeSmall=removeSmallSen(list);
			 //remove redundancy sentence
	    	 List<String>removeRedundancy=removeRedundancySen(removeSmall);			 
			 Map<String, Double> scorelengthmap=new LinkedHashMap<String, Double>();
			 for(String sentence:removeRedundancy){
				 Map<String, Integer> sentencemap=segString(sentence);
				 Set<String> keyword=sentencemap.keySet();
                 double score=0;
				 Map<String, Double> m=topictfidf.get(topic);				 
				 for(String word:keyword){
					if(m.containsKey(word)){
						score= score + m.get(word); 
					} 
				 }				 
				 scorelengthmap.put(sentence, score/sentencemap.size());
			 }
			 
			 //rank
			 Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();  
		        List<Map.Entry<String, Double>> entryList = new ArrayList<Map.Entry<String, Double>>(scorelengthmap.entrySet());  		        
		        Collections.sort(entryList, new MapValueComparator()); 		        
		        Iterator<Map.Entry<String, Double>> iter = entryList.iterator();  
		        Map.Entry<String, Double> tmpEntry = null;  
		        while (iter.hasNext()) {  
		            tmpEntry = iter.next();  
		            sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());  
		        }	        
			 SentenceRank.put(topic,sortedMap);	
		 }
		return SentenceRank; 
	 }
	 
	//Summary of topic. topnumber: choose top sentence number to make summary 
	 public  Map <String,String> makesummary ( Map<String,Map<String, Double>> sentencerank , Integer topnumber){	  
		 Map <String,String> Summary=new LinkedHashMap <String,String>();
	    	Set <String> topiclist=sentencerank.keySet();
	    	for(String topic:topiclist){
	    		Map<String, Double> sentencemap=sentencerank.get(topic);
	    		Set <String> sentence=sentencemap.keySet();
	    		int number=0;
	    		String summary="";
	    		for(String s:sentence){
	    			summary=summary+s;
	    			number++;
	    			if(number==topnumber){
	    				break;
	    			}
	    		}
	    		Summary.put(topic, summary);
	    	}
			return Summary;			 
	 }
	 
	 //get the summary of the given news
	 public String getSummary (List<String> docID) throws Exception{
		 String summary="";
		 Map<String,List<String>> map=new LinkedHashMap<String,List<String>>();
		 map.put("path", docID);
		 Map<String, List<Map<String, String>>> docmap=DocMap(map);
		 
		 List<String> DocList=Doclist(docmap); 
		 Map allTfMap=allTf(DocList);
		 Map tfIdfMap=tfIdf(allTfMap, idfMap);
		 Map mainStoryMap=mainStoryMap(docmap);
		 Map TopicTfidf=TopicTfidf(tfIdfMap,mainStoryMap);
	     Map SentenceList=SentenceList(docmap);
	     Map SentenceRank=sentencerank(TopicTfidf,SentenceList);
	     Map Summary=makesummary(SentenceRank,3);
	     summary=(String) Summary.get("path");		 		 
		return summary;
		 
	 }

    	 
	 public void run() throws Exception{
		    String filepath="C:\\Users\\Pasin\\Documents\\Work\\MSc Computing Science\\Team Project\\Code\\Summary\\list3.json";
		    Map<String, List<String>> docmap=getdocmap(filepath);
		    Map map=DocMap(docmap);
		    List AllDocList=Doclist(map);
	    	idfMap=idf(wordSegCount(AllDocList));
	 }
}



