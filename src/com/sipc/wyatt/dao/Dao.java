package com.sipc.wyatt.dao;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class Dao {
	private static Mongo mongo;
	private static DB db;
	private static DBCollection coll;
	
	/*
	 * initialize mongoDB
	 */
	static {
		try {
			mongo = new Mongo(VarDao.HOST, VarDao.PORT);
			db = mongo.getDB(VarDao.DBNAME);
			coll = db.getCollection(VarDao.COLLNAME);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MongoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * get news according to key from mongDB
	 * @param key
	 */
	public static ArrayList<String> getNewsInfo(String[] key) {
		ArrayList<String> returnVe = new ArrayList<String>();
		DBCursor cursor = coll.find();
		while(cursor.hasNext()) {
			Map line = (Map) cursor.next();
			String info = new String();
			for(String k : key) {
				info += line.get(k) + " ";
			}
			returnVe.add(info.trim());
			
		}
		return returnVe;
	}


}
