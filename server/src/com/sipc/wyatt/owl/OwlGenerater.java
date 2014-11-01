package com.sipc.wyatt.owl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class OwlGenerater {
	public String[] nameSet = {"Badminton","Riddrie","Currency","Crookston","Racing","CWGCycling","CWGTableTennis","MountVernon","Garrowhill","CWGSquash","Murder","Cultural","Park","Ibrox","Celtic","Accident","Dennistoun","CWGTriathlon","Queenslie","Battlefield","Anderston","CWG","SocialEvent","CWGBoxing","Govanhill","Drygate","Easterhouse","Gymnastics","Social","Mansewood","CWGSwimming","Cycling","OpeningCeremony","Toryglen","Partick","Rogerfield","SexualAssault","Music","Millerston","Snooker","Finnieston","Yoker","Anniesland","Milton","BotanicGardens","Other","Lambhill","Balornock","Drumchapel","Broomhill","Garthamlock","Scotstoun","Glasgow","Possilpark","StockMarket","Townhead","Cathcart","Swimming","Summerston","Castlemilk","Hyndland","ScienceTechnology","Business","Economics","Kennishead","OtherIncidence","StockExchange","Boxing","Golf","Robroyston","G11","CWGHockey","G13","G12","Muirend","G15","F1","G14","King&apos;sPark","CWGDiving","Games","OtherEntianment","Cowlairs","Kelvinbridge","Darnley","G20","G22","G21","Gorbals","G23","Newlands","CWGJudo","G1","G2","G3","G4","Carntyne","G5","Mount_Florida","Employment","News","Parkhead","Organisation","G31","Health","Maryhill","G33","G32","G34","CWGWrestling","Football","CWGGymnastics","Club","CWGEvents","NorthKelvinside","ScottishIndependence","Location","EPL","Rugby","G40","G42","G41","G44","Sighthill","Cranhill","G43","Govan","G46","G45","Yorkhill","Deaconsbank","ScottishReferendum","Whiteinch","Cricket","CWGBowls","Judo","Book","League","G51","G53","G52","Atheletics","Carmunnock","Pollok","Ruchazie","OtherSport","Crime","Calton","Transport","Person","Kinning_Park","Investment","LALiga","Kidnap","Woodlands","Shawlands","G69","Squash","Knightswood","CWGAthelatics","Nitshill","Carnwadric","Provanhall","Croftfoot","UniversityOfGlasgow","Bowls","G76","Provanmill","OtherCWG","Baillieston","Shettleston","Barmulloch","Pollokshaws","BlythswoodHill","SportEvent","Craigend","Springburn","Dowanhill","CWGBadminton","Film","OtherCrime","Haghill","OtherSocial","Dalmarnock","Incidence","Sport","Lightburn","Cleveden","Education","Hillington","Carmyle","Rangers","Cardonald","Partickhill","Strathbungo","Ruchill","Assault","WestEnd","MerchantCity","CWGNetball","Royston","OtherPolitics","Theatre","ClosingCeremony","Environment","CWGWeightlifting","SPFL","Muirhead","CWGShooting","Burglary","Garnethill","CWGRugbySevens","Cowcaddens","Sandyhills","Bridgeton","Kelvindale","Arden","TableTennis","Mosspark","Pollokshields","Springboig","Woodside","Drug","Tennis","Hockey","Finance","Springhill","Tollcross","Hillhead","Homicide","Penilee","Enterntainment","Politics"};
	public int[][] map = new int[234][234];
	public String ans = "";

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
