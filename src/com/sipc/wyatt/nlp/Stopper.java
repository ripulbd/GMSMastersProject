package com.sipc.wyatt.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Stopper {
	private static String[] stopList = null;
	private static final String STOPLISTFILEPATH = "stoplist";

	/*
	 * get stop words from STOPLISTFILEPATH, store in stopList
	 */
	static {
		try {
			File file = new File(STOPLISTFILEPATH);
			Long fileLength = file.length();
			byte[] fileContent = new byte[fileLength.intValue()];
			FileInputStream inputStream = new FileInputStream(file);
			inputStream.read(fileContent);
			inputStream.close();
			
			stopList = new String(fileContent).trim().split(",");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String[] getStopList() {
		return stopList;
	}
}
