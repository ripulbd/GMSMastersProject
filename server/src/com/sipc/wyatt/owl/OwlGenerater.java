package com.sipc.wyatt.owl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import com.sipc.wyatt.http.ExAlchemy;

public class OwlGenerater {
	public String[] nameSet = {"Badminton","Riddrie","Currency","Crookston","Racing","CWGCycling","CWGTableTennis","MountVernon","Garrowhill","CWGSquash","Murder","Cultural","Park","Ibrox","Celtic","Accident","Dennistoun","CWGTriathlon","Queenslie","Battlefield","Anderston","CWG","SocialEvent","CWGBoxing","Govanhill","Drygate","Easterhouse","Gymnastics","Social","Mansewood","CWGSwimming","Cycling","OpeningCeremony","Toryglen","Partick","Rogerfield","SexualAssault","Music","Millerston","Snooker","Finnieston","Yoker","Anniesland","Milton","BotanicGardens","Other","Lambhill","Balornock","Drumchapel","Broomhill","Garthamlock","Scotstoun","Glasgow","Possilpark","StockMarket","Townhead","Cathcart","Swimming","Summerston","Castlemilk","Hyndland","ScienceTechnology","Business","Economics","Kennishead","OtherIncidence","StockExchange","Boxing","Golf","Robroyston","G11","CWGHockey","G13","G12","Muirend","G15","F1","G14","King&apos;sPark","CWGDiving","Games","OtherEntianment","Cowlairs","Kelvinbridge","Darnley","G20","G22","G21","Gorbals","G23","Newlands","CWGJudo","G1","G2","G3","G4","Carntyne","G5","Mount_Florida","Employment","News","Parkhead","Organisation","G31","Health","Maryhill","G33","G32","G34","CWGWrestling","Football","CWGGymnastics","Club","CWGEvents","NorthKelvinside","ScottishIndependence","Location","EPL","Rugby","G40","G42","G41","G44","Sighthill","Cranhill","G43","Govan","G46","G45","Yorkhill","Deaconsbank","ScottishReferendum","Whiteinch","Cricket","CWGBowls","Judo","Book","League","G51","G53","G52","Atheletics","Carmunnock","Pollok","Ruchazie","OtherSport","Crime","Calton","Transport","Person","Kinning_Park","Investment","LALiga","Kidnap","Woodlands","Shawlands","G69","Squash","Knightswood","CWGAthelatics","Nitshill","Carnwadric","Provanhall","Croftfoot","UniversityOfGlasgow","Bowls","G76","Provanmill","OtherCWG","Baillieston","Shettleston","Barmulloch","Pollokshaws","BlythswoodHill","SportEvent","Craigend","Springburn","Dowanhill","CWGBadminton","Film","OtherCrime","Haghill","OtherSocial","Dalmarnock","Incidence","Sport","Lightburn","Cleveden","Education","Hillington","Carmyle","Rangers","Cardonald","Partickhill","Strathbungo","Ruchill","Assault","WestEnd","MerchantCity","CWGNetball","Royston","OtherPolitics","Theatre","ClosingCeremony","Environment","CWGWeightlifting","SPFL","Muirhead","CWGShooting","Burglary","Garnethill","CWGRugbySevens","Cowcaddens","Sandyhills","Bridgeton","Kelvindale","Arden","TableTennis","Mosspark","Pollokshields","Springboig","Woodside","Drug","Tennis","Hockey","Finance","Springhill","Tollcross","Hillhead","Homicide","Penilee","Enterntainment","Politics"};
	public int[][] map = new int[234][234];
	public String ans = "";
	public String keys = "";
	public String compostion = "";
	public int deep = 0;
	public boolean useAlchemy;

	public void dfs(int s, int k) {
		for(int i = 0; i < k; i++) ans += "\t";
		int flag = 0;
		for(int i = 0; i < 234; i++) {
			if(map[s][i] == 1) flag = 1;
		}
		if(1 == flag ) ans += "<topic name=\""+nameSet[s]+"\">\n";
		else ans += "<keyword name=\""+nameSet[s]+"\">\n";

		for(int i = 0; i < 234; i++) {
			if(map[s][i] == 1) {
				dfs(i, k+1);
			}
		}
		for(int i = 0; i < k; i++) ans += "\t";
		if(1 == flag) ans += "</topic>\n";
		else ans += "</keyword>\n";
	}
	
	public void processFile() throws Exception{
		String line1, line2;
		BufferedReader reader1 = new BufferedReader(new FileReader("data"+File.separator+"newdata_compostion.txt"));
		while((line1 = reader1.readLine()) != null) {
			useAlchemy = true;
			BufferedReader reader2 =  new BufferedReader(new FileReader("data"+File.separator+"newdata_keys.txt"));
			if(line1.charAt(0) == '#') continue;
			String[] info = line1.split("\t");
			System.out.println(info[1]);
			for(int i = 3; i < info.length; i += 2) {
				Double num = Double.parseDouble(info[i]);
				if(num.compareTo(0.3) > 0) {
//				System.out.println(info[i]);		// Relevence
					int docID = Integer.parseInt(info[i-1]);
					while((line2 = reader2.readLine()) != null) {
						ArrayList<String> keywordsList = new ArrayList<String>();
						if(!line2.split("\t")[0].equals(String.format("%s", docID))) continue;
						for(String key : line2.split("\t")[2].split(" ")) {
							keywordsList.add(key);
						}
//						System.out.println(keywordsList);
						find(keywordsList);
					}
				}
			}
			if(useAlchemy) {
				BufferedReader reader3 = new BufferedReader(new FileReader("data"+File.separator+"data2"+File.separator+info[1].split("/")[info[1].split("/").length-1]));
				String con = "";
				String tmp = "";
				while((tmp = reader3.readLine()) != null) {
					con += tmp;
				}
				// System.out.println("useAlchemyAPI");
				String jsonContent = ExAlchemy.getInfo("text/TextGetRankedConcepts", con);
				String param = "text";
				ArrayList<String> info2 = ExAlchemy.getJsonDetails(jsonContent, param);
				System.out.println("Category: "+info2);
			}
			reader2.close();
		}
		reader1.close();
	}
	
	public void dfs2find(int e) {
		deep++;
		for(int i = 0; i < 233; i++) {
			if(map[i][e] == 1) {
				dfs2find(i);
				if(deep > 1)
				System.out.print(nameSet[e]+"->");
			}
		}
	}
	
	public void find(ArrayList<String> keywordsList) {
		for(String key : keywordsList) {
			int index = -1;		// index of key
			for(int i = 0; i < 233; i++) {
				if(nameSet[i].toLowerCase().equals(key)) {
					index = i;
				}
			}
			if(-1 == index) continue;
			int j;
			for(j = 0; j < 233; j++) {
				if(map[index][j] == 1) break;
			}
			deep = 0;
			if(j == 233) {
				useAlchemy = false;
				dfs2find(index);
				System.out.print(keywordsList);
			}
			if(deep > 1) System.out.println();
		}
	}
	
	public void ontologyGenerater() {
		try {
			String line, parline;
			File file = new File("data"+File.separator+"OWL");
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int start = 0;
			for(int i = 0; i < 234; i++) {
				for(int j = 0; j < 234; j++) {
					map[i][j] = 0;
				}
			}
			while((line = reader.readLine()) != null) {
				line = line.trim();
				parline = reader.readLine().trim();
				int a, b;
				a = b = 0;
				for(int i = 0; i < nameSet.length; i++) {
					if(nameSet[i].equals(line)) {
						a = i;
					}
					if(nameSet[i].equals(parline)) {
						b = i;
					}
					if(nameSet[i].equals("News")) {
						start = i;
					}
				}
				map[b][a] = 1;
			}
			dfs(start, 0);
			System.out.println(ans);
			reader.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
