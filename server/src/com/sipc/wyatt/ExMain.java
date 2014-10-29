package com.sipc.wyatt;

import java.util.ArrayList;

import com.sipc.wyatt.dao.VarDao;
import com.sipc.wyatt.nlp.NLPFactory;


public class ExMain {
	public static void main(String[] args) {
		/*
		 * Modeling
		 */
		NLPFactory fa = new NLPFactory();
		String[] text = {VarDao.DESCRIPTION, VarDao.TITLE};
		ArrayList<ArrayList<String> > keywordsList = fa.start(text);
		System.out.println(keywordsList);
		System.out.println(keywordsList.size());
	/*
	 * 
	 */
		
	}
}
