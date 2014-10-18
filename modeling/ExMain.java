package com.sipc.wyatt;

import com.sipc.wyatt.dao.VarDao;
import com.sipc.wyatt.nlp.NLPFactory;


public class ExMain {
	public static void main(String[] args) {
		NLPFactory fa = new NLPFactory();
		String[] keywords = {VarDao.DESCRIPTION, VarDao.TITLE};
		fa.start(keywords);
	}
}
