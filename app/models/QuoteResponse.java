package models;

import java.util.ArrayList;
import java.util.HashMap;

public class QuoteResponse {
	
	private ArrayList<Quote> quotes;

	@Override
	public String toString() {
		return "Quotes retrieved " + getNumberFound() +" - " + quotes.toString();
	}
	public HashMap<String, Quote> getQuotes(){
		HashMap<String, Quote> hash = new HashMap<String, Quote>();
		for(int i=0;i<quotes.size(); i++){
			hash.put(quotes.get(i).getTicker(), quotes.get(i));			
		}
		return hash;
	}
	public int getNumberFound() {
		return quotes.size();
	}
}
