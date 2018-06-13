package controllers;

import play.*;
import play.cache.Cache;
import play.db.DB;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.*;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import db.HikariCP;

import models.*;

public class Application extends Controller {

	
    public static void portfolio() {
        render();
    }
  
    public static void dividends() {
        render();
    }    

    public static void test() {
        render();
    }
    
	/*
	 * get quotes from cache or api if needed.   
	 */
	@Before
    static void loadQuotes(){
    	 
		Logger.info("@Before");
//check session hash map quote load time
		HashMap sessionHash = Cache.get(session.getId(), HashMap.class);
Logger.info("sessionHash==null: " + (sessionHash==null));
sessionHash=null;

		if(sessionHash == null){
			sessionHash = new HashMap();
			
			//if no session hash, read latest quotes from table portfolio.quote_hist			
	 		PreparedStatement ps = null;
	 		Connection con = null;
	 		ResultSet rs = null;
	 		
	    	try{
	    		String dateoffset = "";	    		
	    		final LocalDateTime now = LocalDateTime.now();
	    		final LocalDateTime open = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 8, 30); //Today, 0830
	    		if(now.isBefore(open)) {
	    			Logger.info("before opening bell so get yesterdays (-1) quotes");
	    			dateoffset = "-1";
	    		}
	    		else if(now.getDayOfWeek() == DayOfWeek.SATURDAY){
	    			Logger.info("It's Saturday, go get Fridays (-1) close quotes");
	    			dateoffset = "-1";
	    		}
	    		else if(now.getDayOfWeek() == DayOfWeek.SUNDAY){
	    			Logger.info("It's Sunday, go get Fridays (-2) close quotes");
	    			dateoffset = "-2";
	    		}
	    		
	    	    con = HikariCP.getConnection();
	    	    ps = con.prepareStatement("SELECT ticker,price FROM( " +
                                          "  SELECT ticker, price, RANK() over (partition BY ticker ORDER BY ts DESC) rnk " +
                                          "  FROM PORTFOLIO.QUOTE_HIST " +
                                          "  WHERE CAST(TS AS DATE) >= CURRENT_DATE-50" + dateoffset + 
                                          ")z " +
                                          "WHERE rnk = 1");
	    	    rs = ps.executeQuery();
	    	    int r=0;
	    	    
	    	    HashMap quotes = new HashMap<String, Quote>();	    	    
				while(rs.next()){
					String ticker = rs.getString("ticker").trim();
					String price = rs.getString("price");

					Quote q = new Quote(ticker, price);
					quotes.put(ticker,  q);
					r++;
				}
	    	    
	    	    ps.close();

	    	    if(r>0) {
	    	    	Logger.info("Quotes from DB: " + quotes);
	    	    	sessionHash.put("quotes", quotes);
	    	    	Cache.set(session.getId(), sessionHash, "30mn");	    	    	
	    	    	return;
	    	    }
	    	    
	    	}
	    	catch(Exception e){
	    		Logger.info("error getting quotes from DB: " + e.getMessage());
	    	}
	    	finally{
				try {				
					DbUtils.closeQuietly(ps);
					HikariCP.close();
				} 
				catch (Exception e) {
					Logger.error("getHistory: Error closing connection " + e.getMessage());
				} 
	    	}			

	    	// quotes are not in DB, get list of tickers to get prices for.
	    	Logger.info("no quotes in db so call webservice and get them");
	    	
	    	StringJoiner joiner = new StringJoiner(",");
	    	try{
	    	    con = HikariCP.getConnection();
	    	    ps = con.prepareStatement("SELECT ticker " +
                                          "FROM PORTFOLIO.HOLDINGS " +
                                          "GROUP BY 1");
	    	    rs = ps.executeQuery();
    	    
				while(rs.next()){
					String ticker = rs.getString("ticker");
					joiner.add(ticker.trim());
				}
	    	    ps.close();
	    	    
	    	}
	    	catch(Exception e){
	    		Logger.info("error getting quotes from DB: " + e.getMessage());
	    	}
	    	finally{
				try {				
					DbUtils.closeQuietly(ps);
					HikariCP.close();
				} 
				catch (Exception e) {
					Logger.error("getHistory: Error closing connection " + e.getMessage());
				} 
	    	}			
	    	String symbols = joiner.toString();    	
	    	Logger.info(symbols);
	    		
		    	
	    	try{		    	
		    	//String rawquotes = "{\"Meta Data\": {\"1. Information\": \"Batch Stock Market Quotes\", \"2. Notes\": \"IEX Real-Time Price provided for free by IEX (https://iextrading.com/developer/).\", \"3. Time Zone\": \"US/Eastern\"}, \"Stock Quotes\": [ {\"1. symbol\": \"KO\", \"2. price\": \"42.42\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"T\", \"2. price\": \"32.51\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 16:03:03\"}, {\"1. symbol\": \"AAPL\", \"2. price\": \"188.6900\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:47\"}, {\"1. symbol\": \"SPHD\", \"2. price\": \"40.08\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"SBUX\", \"2. price\": \"57.80\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"SKT\", \"2. price\": \"21.05\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"WM\", \"2. price\": \"82.96\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"ZNGA\", \"2. price\": \"4.17\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"OGE\", \"2. price\": \"34.39\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"} , {\"1. symbol\": \"JNJ\", \"2. price\": \"121.47\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"}, {\"1. symbol\": \"KMB\", \"2. price\": \"105.96\", \"3. volume\": \"--\", \"4. timestamp\": \"2018-05-25 15:59:45\"} ]}";		    	
		    	//rawquotes = rawquotes.replaceAll("1. symbol", "symbol").replaceAll("2. price","price").replaceAll("3. volume", "volume").replaceAll("4. timestamp","timestamp");
		    	//rawquotes = rawquotes.replaceAll("Stock Quotes", "quotes");
		    	//Logger.info(rawquotes.indexOf("\"quotes\":")+"");
		        //rawquotes = "{" + rawquotes.substring(rawquotes.indexOf("\"quotes\":"), rawquotes.length());    			
		    		
                WSRequest req = WS.url("https://www.alphavantage.co/query?function=BATCH_STOCK_QUOTES&symbols=" + symbols + "&apikey=CMWNJD2K400SVE2D");
                HttpResponse res = null;
                
                res = req.get();
                
                StringWriter writer = new StringWriter();
                IOUtils.copy(res.getStream(), writer);
                
                String rawquotes = writer.toString();
                rawquotes = rawquotes.replace("\n", "").replace("\r", "").replaceAll("  ", "");
                rawquotes = rawquotes.replaceAll("1. symbol", "symbol").replaceAll("2. price","price").replaceAll("3. volume", "volume").replaceAll("4. timestamp","timestamp");
                rawquotes = rawquotes.replaceAll("Stock Quotes", "quotes");
                rawquotes = "{" + rawquotes.substring(rawquotes.indexOf("\"quotes\":"), rawquotes.length());
	            Logger.info("stock quote response from webservice: " + rawquotes);

	            Gson gson = new GsonBuilder().create();
	    		QuoteResponse qr = gson.fromJson(rawquotes, QuoteResponse.class);
	    		
	    		sessionHash.put("quotes", qr.getQuotes());
	             
	    		// save these quotes to the DB
	    	    con = HikariCP.getConnection();


	    	    //ps = con.prepareStatement("INSERT INTO portfolio.quote_hist (ticker,price,ts) VALUES (?,?,?::timestamp)");
	    	    ps = con.prepareStatement("INSERT INTO portfolio.quote_hist (ts,ticker,price) VALUES (now(),?,?)");

	    	    for (String key : qr.getQuotes().keySet()) {
	
	    	        ps.setString(1, qr.getQuotes().get(key).getTicker());
	    	        ps.setDouble(2, qr.getQuotes().get(key).getPrice());
	    	        ps.addBatch();
	    	    }
	    	    ps.executeBatch();
	    	    ps.close();
	    	    Logger.info("quotes saved to DB");
	    	}
	    	catch(Exception e){
	    		Logger.info("error parsing quotes " + e.getMessage());
	    	}
	    	finally{
				try {				
					DbUtils.closeQuietly(ps);
					HikariCP.close();
				} 
				catch (Exception e) {
					Logger.error("getHistory: Error closing connection " + e.getMessage());
				} 
	    	}

	        //save the session in cache as authenticated for 90 minutes
	        Cache.set(session.getId(), sessionHash, "90mn");
		}
	}    
}
