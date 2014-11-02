package com.sipc.wyatt;

import java.io.IOException;
import java.util.ArrayList;

import com.sipc.wyatt.dao.VarDao;
import com.sipc.wyatt.http.ExAlchemy;
import com.sipc.wyatt.nlp.ExStanfordParser;
import com.sipc.wyatt.nlp.NLPFactory;
import com.sipc.wyatt.owl.OwlGenerater;



public class ExMain {
	public static void main(String[] args) throws Exception {
		NLPFactory fa = new NLPFactory();
		/*
		String[] text1 = {VarDao.DESCRIPTION, VarDao.TITLE, VarDao.MAINSTORY};
		ArrayList<String> content = fa.init(text1);
		int i = 1;
		for(String str : content) {
			System.out.println(i+"|||"+str.replaceAll("<[^>]*>", "")+"###");
			i++;
		}
		*/
		/*
		 * Modeling
		 */
		String[] text = {VarDao.DESCRIPTION, VarDao.TITLE};
		ArrayList<ArrayList<String> > keywordsList = fa.start(text);
		
//		for(ArrayList<String> str : keywordsList) {
//			System.out.println(str);
//		}
		OwlGenerater owl = new OwlGenerater();
		owl.ontologyGenerater();
		owl.processFile();
		/*
		 * Topic Tagging
		 * @param info Categories
		 * @param neList Each ArrayList<String> of neList has this format <ORGANIZATION,Glasgow Warriors,PERSON,Gregor Townsend>.
		 * 			Every two element is a pair, containing name entities and words.
		 */
		ArrayList<String> textList = fa.getTodoList();
		ArrayList<ArrayList<String>> neList = new ArrayList<ArrayList<String>>();
		ExStanfordParser parser = new ExStanfordParser();
		for(String con : textList) {
			System.out.println(con);
			/*
			 * AlchemyAPI
			 */
			/*
			String jsonContent = ExAlchemy.getInfo("text/TextGetRankedConcepts", con);
			String param = "text";
			ArrayList<String> info = ExAlchemy.getJsonDetails(jsonContent, param);
			System.out.println("Category: "+info);
			jsonContent = ExAlchemy.getInfo("text/TextGetRankedNamedEntities", con);
			param = "text";
			info = ExAlchemy.getJsonDetails(jsonContent, param);
			System.out.println("Name Entities: "+info);
			*/
			/*
			 * Name Entities
			 * Stanford Parser
			 */
			neList.add(parser.getNameEntities(con));
		}
		
		/*
		 * Summarizing
		 */
		
	}
}
