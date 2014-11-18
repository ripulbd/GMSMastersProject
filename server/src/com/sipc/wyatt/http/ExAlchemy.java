package com.sipc.wyatt.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.stream.JsonParser;

public class ExAlchemy {
	private static final String APIKEY = "3f07faf2bf9dc29f4a0d40072dfc09e6e3e2fbd9";

	/*
	 * @param callName URL
	 * @param text Text need to be processed
	 * @return String Json format
	 */
	public static String getInfo(String callName, String text) {
		PrintWriter out = null;
		BufferedReader in = null;
		String param = "apikey=" + APIKEY + "&text=" + text
				+ "&outputMode=json";
		String result = "";
		try {
			URL url = new URL("http://access.alchemyapi.com/calls/" + callName);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);

			out = new PrintWriter(conn.getOutputStream());
			out.print(param);
			out.flush();
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				result += line;
			}
			// System.out.println(result);
		} catch (Exception e) {
			System.out.println("Error");
			e.printStackTrace();
		}
		return result;
	}

	/*
	 * @param String text(Json format)
	 * @param String param, it will return the value of json[param]
	 * @return ArrayList<String>
	 */
	public static ArrayList<String> getJsonDetails(String text, String param) {
		ArrayList<String> list = new ArrayList<String>();
		try {
			JsonParser parser = Json.createParser(new StringReader(text));
			int flag = 0;
			while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch (event) {
				case START_ARRAY:
				case END_ARRAY:
				case START_OBJECT:
				case END_OBJECT:
				case VALUE_FALSE:
				case VALUE_NULL:
				case VALUE_TRUE:
//					System.out.println(event.toString());
					break;
				case KEY_NAME:
					if(parser.getString().equals(param)) {
						flag = 1;
					}
//					System.out.print(event.toString() + " "
//							+ parser.getString() + " - ");
					break;
				case VALUE_STRING:
				case VALUE_NUMBER:
					if(1 == flag) {
						list.add(parser.getString());
						flag = 0;
					}
//					System.out.println(event.toString() + " "
//							+ parser.getString());
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}
