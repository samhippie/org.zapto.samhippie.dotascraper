package org.zapto.samhippie.dotascraper;

public class Hero
{
	String name;
	int id;
	int enemyWins;
	int enemyLosses;
	int allyWins;
	int allyLosses;
	int selfWins;
	int selfLosses;
	
	public Hero(int id, String name)
	{
		this.name = name;
		this.id = id;
	}
	
	public String toString()
	{
		return name + " EW: " + enemyWins + " EL: " + enemyLosses
					+ " AW: " + allyWins + " AL: " + allyLosses
					+ " SW: " + selfWins + " SL: " + selfLosses;
	}
}
