package org.zapto.samhippie.dotascraper;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import com.google.gson.*;

public class Main
{
	static final String KEY = "72A105F3230ACDB11A1BA542D6D2A77D";
	static final String MATCH_HISTORY = "https://api.steampowered.com/IDOTA2Match_570/GetMatchHistory/V001/?key=" + KEY + "&player_name=";
	static final String MATCH_INFO = "https://api.steampowered.com/IDOTA2Match_570/GetMatchDetails/V001/?key=" + KEY + "&match_id=";
	static final String HERO_LIST = "https://api.steampowered.com/IEconDOTA2_570/GetHeroes/v0001/?key=" + KEY + "&language=en_us";
	static final String PLAYER_ID = "http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=" + KEY + "&vanityurl=";
	
	HashMap<Integer, Hero> heroes;
	JsonParser parser;
	
	public static void main(String[] args)
	{
		Scanner input = new Scanner(System.in);
		System.out.println("Steam Profile Name (Must be set for profile URL):");
		new Main().run(input.next());
	}
	
	public void run(String username)
	{
		parser = new JsonParser();
		
		//gets 32 bit steam id
		JsonElement element = parser.parse(getJSON(PLAYER_ID + username));//var is generic, will be reused
		JsonObject object = element.getAsJsonObject();//same
		object = object.getAsJsonObject("response");//same
		long steamid64 = object.getAsJsonPrimitive("steamid").getAsLong();
		int steamID32 = (int) steamid64;//64 > 32 bit
		
		heroes = new HashMap<Integer, Hero>();

		//get hero list as JSON
		element = parser.parse(getJSON(HERO_LIST));//var is generic, will be reused
		object = element.getAsJsonObject();//same
		object = object.getAsJsonObject("result");//same
		JsonArray heroArray = object.getAsJsonArray("heroes");
		
		//loads heroes into hashmap
		System.out.println("Loading hero list...");
		for(JsonElement heroElement : heroArray)
		{
			JsonObject hero = heroElement.getAsJsonObject();
			int heroID = hero.getAsJsonPrimitive("id").getAsInt();
			String heroName = hero.getAsJsonPrimitive("localized_name").getAsString();
			heroes.put(heroID, new Hero(heroID, heroName));
		}
		System.out.println("Done");
		
		//will contain all match IDs
		HashSet<Long> matchSet = new HashSet<Long>();

		//gets match history as JSON, 25 latest
		element = parser.parse(getJSON(MATCH_HISTORY + username));
		object = element.getAsJsonObject();
		object = object.getAsJsonObject("result");
		int matchesAmount = object.getAsJsonPrimitive("total_results").getAsInt();
		JsonArray matches = object.getAsJsonArray("matches");
		long oldestMatchID = 0;
		
		//25 starting at oldest in previous list
		while(matchSet.size() < matchesAmount)//actually is an off by one error, but whatever. First game is free.
		{
			element = parser.parse(getJSON(MATCH_HISTORY + username + "&start_at_match_id=" + oldestMatchID));
			object = element.getAsJsonObject();
			object = object.getAsJsonObject("result");
			matches = object.getAsJsonArray("matches");
			
			//pulls out match ids
			System.out.println("Looking at a set of 25 matches...");
			int i = 0;
			for(JsonElement matchElement : matches)
			{
				JsonObject match = matchElement.getAsJsonObject();
				long matchID = match.getAsJsonPrimitive("match_id").getAsLong();
				matchSet.add(matchID);
				i++;
				System.out.println((i + 1) + " matches found");
				oldestMatchID = matchID;
			}
		}
		
		System.out.println("Begining analysis of " + matchesAmount + " matches.");
		int i = 0;
		for(long id: matchSet)
		{
			readMatch(id, steamID32);
			System.out.println(++i + " matches analyzed.");
		}
		
		System.out.println("All done. Hero Results:");
		
		showStats();
	}

	public void showStats()
	{
		//how enemies did
		Hero enemyMaxWin = heroes.get(5);//5 is arbitrary, probably not null
		Hero enemyMaxLoss = heroes.get(5);
		//how team did
		Hero allyMaxWin = heroes.get(5);
		Hero allyMaxLoss = heroes.get(5);
		//how I did
		Hero selfMaxWin = heroes.get(5);
		Hero selfMaxLoss = heroes.get(5);
		for(int i = 0; i < heroes.size(); i++)
		{
			Hero hero = heroes.get(i);
			if(hero == null || hero.name == "empty")//this is a problem, for some reason
				continue;
			//enemy
			if(hero.enemyWins / (hero.enemyLosses + 1) > enemyMaxWin.enemyWins / (enemyMaxWin.enemyLosses + 1) && hero.enemyWins + hero.enemyLosses > 5)//using W/L % when total games > 5
				enemyMaxWin = hero;
			if(hero.enemyWins / (hero.enemyLosses + 1) < enemyMaxLoss.enemyWins / (enemyMaxLoss.enemyLosses + 1) && hero.enemyWins + hero.enemyLosses > 5)//enemyLosses + 1 to avoid divide by zero
				enemyMaxLoss = hero;
			//ally
			if(hero.allyWins / (hero.allyLosses + 1) > allyMaxWin.allyWins / (allyMaxWin.allyLosses + 1) && hero.allyWins + hero.allyLosses > 5)//using W/L % when total games > 5
				allyMaxWin = hero;
			if(hero.allyWins / (hero.allyLosses + 1) < allyMaxLoss.allyWins / (allyMaxLoss.allyLosses + 1) && hero.allyWins + hero.allyLosses > 5)//allyLosses + 1 to avoid divide by zero
				allyMaxLoss = hero;
			//self
			if(hero.selfWins / (hero.selfLosses + 1) > selfMaxWin.selfWins / (selfMaxWin.selfLosses + 1) && hero.selfWins + hero.selfLosses > 5)//using W/L % when total games > 5
				selfMaxWin = hero;
			if(hero.selfWins / (hero.selfLosses + 1) < selfMaxLoss.selfWins / (selfMaxLoss.selfLosses + 1) && hero.selfWins + hero.selfLosses > 5)//selfLosses + 1 to avoid divide by zero
				selfMaxLoss = hero;

		}
		//enemy
		System.out.println("Best against: " + enemyMaxLoss);
		System.out.println("Worst against: " + enemyMaxWin);
		//ally
		System.out.println("Best with: " + allyMaxWin);
		System.out.println("Worst with: " + allyMaxLoss);
		//self
		System.out.println("Best as: " + selfMaxWin);
		System.out.println("Worst as: " + selfMaxLoss);
	}
	
	//looks at match, adds enemyWins/enemyLosses to other team's heroes
	public void readMatch(long matchID, int steamID32)
	{
		boolean didWin;
		
		//getting match info
		System.out.println("Loading match...");
		String json = getJSON(MATCH_INFO + matchID);//this is null for bot games
		if(json == null)
			return;
		JsonElement element = parser.parse(json);//var is generic, will be reused
		JsonObject object = element.getAsJsonObject();//same
		object = object.getAsJsonObject("result");//same
		boolean radiantWin = object.getAsJsonPrimitive("radiant_win").getAsBoolean();//will be used with didWin
		System.out.println("Done");
		
		JsonArray playerArray = object.getAsJsonArray("players");

		if(playerArray.size() < 10)
			return;//not counting games with leavers, I think
		
		//finding player's player_slot
		System.out.println("Finding player...");
		int slot = 0;
		JsonElement playerElement = playerArray.get(slot);
		while(playerElement.getAsJsonObject().getAsJsonPrimitive("account_id").getAsInt() != steamID32)
		{
			slot++;
			playerElement = playerArray.get(slot);
		}
		
		if(slot < 5)//player was on radiant
			didWin = radiantWin;
		else//player was on dire
			didWin = !radiantWin;
		System.out.println("Done");
		
		//looks at enemy heroes
		System.out.println("Analyzing enemy heroes...");
		if(slot < 5)//if on radiant
		{
			if(didWin)
			{
				for(int i = 5; i < 10; i++)//five dire heroes
					heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).enemyLosses++;//they lost
				
				for(int i = 0; i < 5; i++)//five radiant heroes
					if(i != slot)//not counting self
						heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).allyWins++;//we won
			}
			else
			{
				for(int i = 5; i < 10; i++)//five dire heroes
					heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).enemyWins++;//they won
				
				for(int i = 0; i < 5; i++)//five radiant heroes
					if(i != slot)//not counting self
						heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).allyLosses++;//we lost
			}
		}
		else//if on dire
		{
			if(didWin)
			{
				for(int i = 0; i < 5; i++)//five radiant heroes
					heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).enemyLosses++;//they lost
				
				for(int i = 5; i < 10; i++)//five dire heroes
					if(i != slot)//not counting self
						heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).allyWins++;//we won
			}
			else
			{
				for(int i = 0; i < 5; i++)//five radiant heroes
					heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).enemyWins++;//they won
				
				for(int i = 5; i < 10; i++)//five dire heroes
					if(i != slot)//not counting self
						heroes.get(playerArray.get(i).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).allyLosses++;//we lost
			}
		}
		
		if(didWin)
		{
			heroes.get(playerArray.get(slot).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).selfWins++;//I won
		}
		else
		{
			heroes.get(playerArray.get(slot).getAsJsonObject().getAsJsonPrimitive("hero_id").getAsInt()).selfLosses++;//I lost
		}
		System.out.println("Done");
	}
	
	public static String getJSON(String input) 
	{
	    try {
	        URL url = new URL(input);
	        URLConnection urlConnection = url.openConnection();
	        urlConnection.setConnectTimeout(1000);
	        return convertStreamToString(urlConnection.getInputStream());
	    } catch (Exception ex) {
	        return null;
	    }
	}
	
	public static String convertStreamToString(java.io.InputStream is) 
	{
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
}
