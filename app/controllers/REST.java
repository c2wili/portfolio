package controllers;

import play.*;
import play.cache.Cache;
import play.db.DB;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.*;
import play.mvc.Scope.Params;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import db.HikariCP;
import models.*;

public class REST extends Controller {

	private static Params requestParams;
	private static int portfolioId = 1;
	private static HashMap sessionHash;
	
	/*
	 * get quotes from cache or api if needed.   
	 */
	@Before
    static void loadQuotes(){
    	 
		sessionHash = Cache.get(session.getId(), HashMap.class);

	}

	public static void main() throws Exception {
    	
    	requestParams = params;
    	
    	String hnd = (String)params.get("hnd");
    	    
    	
    	if(hnd == null) throw new Exception("No handler value provded.");
    	Logger.info("handler: " + hnd);
    			
    	if(hnd.equals("overview")){
    		getOverview();
    	}
    	else if(hnd.equals("newholding")){
    		newHolding();
    	}    	
    	else if(hnd.equals("addposition")){
    		addPosition();
    	}
    	else if(hnd.equals("sellposition")){
    		sellPosition();
    	}    	
    	else if(hnd.equals("recorddividend")){
    		recordDividend();
    	}
    	else if(hnd.equals("history")){
    		getHistory();
    	}      	    	
    	else if(hnd.equals("deletesingletransaction")){
    		deleteHistory();
    	}
    	else if(hnd.equals("sankey")){
    		dividendSankey();
    	}   
    	else if(hnd.equals("swapportfolio")){
    		portfolioId = Integer.parseInt((String)params.get("v"));
    		renderJSON("{\"success\":true}");
    	}
    	else if(hnd.equals("getdividends")){
    		getDividendData();
    	}
    	else if(hnd.equals("updateprice")){
    		updateStockPrice();
    	}
    	else if(hnd.equals("getprices")){
    		updatePrices();
    	}
    	else{
    		throw new Exception("Handler value (" + hnd + ") is unrecognized.");
    	}    	    	   	
    }
	public static void updatePrices() throws Exception {
		
		PreparedStatement ps = null;
		Connection con = null;
		ResultSet rs = null;
		
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
				DbUtils.closeQuietly(rs);
				HikariCP.close();
			} 
			catch (Exception e) {
				Logger.error("updatePrices: Error closing connection " + e.getMessage());
			} 
    	}			
    	
    	String symbols = joiner.toString(); 
		try{		    	
	    		
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
    	    renderJSON("{\"success\":true}");
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
				Logger.error("updatePrices: Error closing connection " + e.getMessage());
			} 
    	}
	}

	public static void updateStockPrice() throws Exception {
	
		String ticker = (String)requestParams.get("ticker");
		String price = (String)requestParams.get("price");
		
		PreparedStatement ps = null;
		Connection con = null;
			
		try {
			con = HikariCP.getConnection();
			ps = con.prepareStatement("INSERT INTO portfolio.quote_hist (ts,ticker,price) VALUES (now(),?,?)");
			
		    
	        ps.setString(1, ticker);
	        ps.setDouble(2, Double.parseDouble(price));
	        ps.executeUpdate();
	        ps.close();
			
	        renderJSON("{\"success\":true}");
			
		} 
		catch (Exception e) {
			Logger.error("updateStockPrice caught exception " + e.getMessage());
			throw new Exception("ERROR: updateStockPrice() " + e.getMessage());  
		} 
		finally {
			
			try {		
				ps.close(); 
				HikariCP.close();
				
			} catch (SQLException e) {
				Logger.error("updateStockPrice: Error closing connection " + e.getMessage());
			} 
		}	
	}

	public static void getDividendData()throws Exception {
	
		PreparedStatement ps = null;
		Connection con = null;
		ResultSet rs = null;
	
		JsonArray  jsonRows = new JsonArray();
	
		try {
			con = HikariCP.getConnection();
	
			String sql = "\n SELECT t.*, h.sector, h.divperiod " +
			 "\n FROM portfolio.trades t " +
			 "\n JOIN portfolio.holdings h " +
			 "\n   ON t.ticker = h.ticker " +
			 "\n WHERE t.activity_type in ('dividend','lt gain','st gain') " +
			 "\n   AND t.portfolio_id = ? " +
			 "\n ORDER BY t.ticker, t.activity_date ";
	
			Logger.debug(sql);
	
			ps = con.prepareStatement(sql);
			ps.setInt(1, portfolioId);
	
			rs = ps.executeQuery();
	
	
	
			while(rs.next()){
	
				JsonObject jsonRow = new JsonObject();
	
				String ticker = rs.getString("ticker").trim();
				String activity_date = rs.getString("activity_date");
				String price = rs.getString("price");
				String shares = rs.getString("shares");
				String sector = rs.getString("sector");
				String divperiod = rs.getString("divperiod");
	
				//Logger.info(ticker);
	
				jsonRow.addProperty("ticker", ticker);
				jsonRow.addProperty("activity_date", activity_date);
				jsonRow.addProperty("sector", sector);
				jsonRow.addProperty("divperiod", Integer.parseInt(divperiod));
				jsonRow.addProperty("price", Double.parseDouble(price));
				jsonRow.addProperty("shares", Double.parseDouble(shares));
	
				jsonRows.add(jsonRow);
	
			}
			ps.close();
			rs.close();
	
			renderJSON(jsonRows);
	
		} 
		catch (Exception e) {
			Logger.error("getDividendData caught exception " + e.getMessage());
			throw new Exception("ERROR: getDividendData() " + e.getMessage());  
		} 
		finally {
	
			try {				
				rs.close();
				ps.close();
				HikariCP.close();
	
				// use the Play connection close method !important
				if(!con.isClosed()) con.close();
			} catch (SQLException e) {
		   	    Logger.error("getDividendData: Error closing connection " + e.getMessage());
			} 
  	    }

	}

    public static void deleteHistory() throws Exception {

    	String id = (String)requestParams.get("id");
    	String ticker = (String)requestParams.get("ticker");
    	String activity_type = (String)requestParams.get("activity_type");
    	     	
    	Logger.info("Remove historical record for " + ticker + ", id: " + id + ", type: " + activity_type );
		PreparedStatement ps = null;
		Connection con = null;
		
		try {
			con = HikariCP.getConnection();
			
			// remove the sales detail record for this sell transaction
			if(activity_type.equals("sell")){

				
				String sql = " DELETE FROM portfolio.salesdetail " +
					         "\nWHERE trades_id = ? " +
					         "\n  AND ticker = ? " +
					         "\n  and portfolio_id = ? ";

				Logger.debug("Removing sales detail record.");
				Logger.debug(sql);
				
				ps = con.prepareStatement(sql);
				ps.setInt(1, Integer.parseInt(id));
				ps.setString(2, ticker);
				ps.setInt(3, portfolioId);
				
				ps.executeUpdate();
				ps.close();
				
				sql = " DELETE FROM portfolio.trades " +
				      "\nWHERE id = ? " +
					  "\n  AND ticker = ? " +
					  "\n  AND activity_type = 'sell' " +
					  "\n  and portfolio_id = ? ";
				
				Logger.debug(sql);
				
				ps = con.prepareStatement(sql);
				ps.setInt(1, Integer.parseInt(id));
				ps.setString(2, ticker);
				ps.setInt(3, portfolioId);
				
				ps.executeUpdate();
				ps.close();

			}			
			// check for a drip entry for this dividend transaction removal
			else if(activity_type.equals("dividend") || activity_type.equals("lt gain") || activity_type.equals("st gain") ){
				
				Logger.info("Remove drip record " + ticker + ", " + activity_type);
				
				// remove any possible drip trade
				String sql = " DELETE FROM portfolio.trades t " +
						     "\n  WHERE t.ticker = ? " +
						     "\n  AND t.activity_type = 'drip' " +
						     "\n  and t.portfolio_id = ? " +
						     "\n  AND t.id = ? ";

				 
				Logger.debug(sql);
				
				ps = con.prepareStatement(sql);
				ps.setString(1, ticker);
				ps.setInt(2, portfolioId);
				ps.setInt(3, Integer.parseInt(id)+1);
				ps.executeUpdate();
				ps.close();
				
				// delete the dividend paid
				sql = " DELETE FROM portfolio.trades " +
					  "\nWHERE id = ? " +
					  "\n  AND ticker = ? " +
				      "\n  AND activity_type = ? " +
				      "\n  and portfolio_id = ? ";

				Logger.debug("Removing dividend paid record.");
				Logger.debug(sql);
				
				ps = con.prepareStatement(sql);
				ps.setInt(1, Integer.parseInt(id));
				ps.setString(2, ticker);
				ps.setString(3, activity_type);
				ps.setInt(4, portfolioId);
				
				ps.executeUpdate();
				
			}
			// remove the drip and dividend entry
			else if(activity_type.equals("drip")){

				// remove the corresponding dividend paid record
				String sql = " DELETE FROM portfolio.trades t " +
						     "\n  WHERE t.ticker = ? " +
						     "\n  AND t.activity_type = 'dividend' " +
						     "\n  and t.portfolio_id = ? " +
						     "\n  AND ROUND(price*shares,2) IN (SELECT ROUND(price*shares,2) " + 
						     "\n                                FROM portfolio.trades td " +
						     "\n                                WHERE td.activity_type = 'drip' " +
						     "\n                                  AND td.ticker = t.ticker " +
						     "\n                                  AND td.activity_date = t.activity_date " +
						     "\n                                  AND td.id = ? " +
						     "\n                                  and td.portfolio_id = t.portfolio_id " +						     
						     "\n                               ) ";

				Logger.debug("Removing possible drip record.");
				Logger.debug(sql);
				
				ps = con.prepareStatement(sql);
				ps.setString(1, ticker);
				ps.setInt(2, portfolioId);
				ps.setInt(3, Integer.parseInt(id));
				
				ps.executeUpdate();
				ps.close();
				
				// delete the drip record
				sql = " DELETE FROM portfolio.trades " +
					  "\nWHERE id = ? " +
					  "\n  AND ticker = ? " +
				      "\n  AND activity_type = 'drip' " +
				      "\n  and portfolio_id = ? ";

				Logger.debug("Removing dividend paid record.");
				Logger.debug(sql);
				
				ps = con.prepareStatement(sql);
				ps.setInt(1, Integer.parseInt(id));
				ps.setString(2, ticker);
				ps.setInt(3, portfolioId);
				
				ps.executeUpdate();
				
			}	
			// remove any other buy records
			else{
				String sql = " DELETE FROM portfolio.trades " +
						     "\nWHERE id = ? " +
						     "\n  AND ticker = ? " +
						     "\n  and portfolio_id = ? ";
				

				
				Logger.debug(sql);
				
				
				ps = con.prepareStatement(sql);
				ps.setInt(1, Integer.parseInt(id));
				ps.setString(2, ticker);
				ps.setInt(3, portfolioId);
				
				ps.executeUpdate();
				ps.close();
			}
			
 			renderJSON("{\"success\":true}");		
		} 
		catch (Exception e) {
			Logger.error("getHistory caught exception " + e.getMessage());
			throw new Exception("ERROR: getHistory() " + e.getMessage());  
		} 
		finally {
			
			try {				
				ps.close();
				HikariCP.close();
				
				// use the Play connection close method !important
				if(!con.isClosed()) con.close();
			} catch (SQLException e) {
				Logger.error("getHistory: Error closing connection " + e.getMessage());
			} 
		}
    	   	
    }

    public static void getHistory() throws Exception {
 
    	String ticker = (String)requestParams.get("ticker");
    	
    	System.out.println("get stock history for " + ticker);
    	
		PreparedStatement ps = null;
		Connection con = null;
		ResultSet rs = null;
	
		JsonArray  jsonRows = new JsonArray();
		
		try {
			con = HikariCP.getConnection();
			
			String sql = "\n SELECT t.id, t.portfolio_id, t.ticker,t.activity_type, t.activity_date,t.price " +
			             "\n        ,case when t.activity_type = 'sell' then s.shares else t.shares end as shares " +
			             "\n        ,COALESCE(CAST(s.basis AS VARCHAR(50)),'----') AS basis " +
			             "\n        ,CASE WHEN t.activity_type in ('dividend','lt gain','st gain') THEN CAST(t.shares*t.price AS VARCHAR(50)) " +
			             "\n              WHEN t.activity_type = 'sell' THEN CAST(s.gain_loss AS VARCHAR(50)) " +
			             "\n           ELSE '----' " +
			             "\n        END AS gainloss " +
						 "\nFROM portfolio.trades t " +
					     "\nLEFT OUTER JOIN portfolio.salesdetail s " + 
                         "\n  ON t.id = s.trades_id " + 
						 "\nWHERE t.ticker = ? " +
						 "\n  and t.portfolio_id = ? " +
						 "\nORDER BY activity_date, t.id asc";
			
			
			Logger.debug(sql);
			
			
			ps = con.prepareStatement(sql);
			ps.setString(1, ticker);
			ps.setInt(2, portfolioId);
			
			rs = ps.executeQuery();
			
			ResultSetMetaData rsMD = rs.getMetaData();
			int colCount = rsMD.getColumnCount();
			
			while(rs.next()){
				JsonObject jsonRow = new JsonObject();
				
				for (int c = 1; c <= colCount; c++) {
					String colHeader = rsMD.getColumnName(c);
					
					String colVal = "";

					try {
						colVal = rs.getString(colHeader).trim();
					} catch (Exception e) {
						colVal = rs.getString(colHeader);
					}
					if (colVal == null)
						colVal = "";
					
					if(colVal.equals("")){
						jsonRow.addProperty(colHeader, "");
					}
					else if(StringUtils.isNumeric(colVal)){
						jsonRow.addProperty(colHeader, Integer.parseInt(colVal));
					}
					else if(StringUtils.isNumeric(colVal.replaceAll("\\.","")) && StringUtils.countMatches(colVal,".") <= 1 ){
						jsonRow.addProperty(colHeader, Double.parseDouble(colVal));						
					}
					else{
						jsonRow.addProperty(colHeader, colVal);
					}
				}

				// add row to returning result set
				jsonRows.add(jsonRow);				
			}
			
			renderJSON(jsonRows);
		
		} 
		catch (Exception e) {
			Logger.error("getHistory caught exception " + e.getMessage());
			throw new Exception("ERROR: getHistory() " + e.getMessage());  
		} 
		finally {
			
			try {				
				rs.close();
				ps.close();
				HikariCP.close();
				
				// use the Play connection close method !important
				if(!con.isClosed()) con.close();
			} catch (SQLException e) {
				Logger.error("getHistory: Error closing connection " + e.getMessage());
			} 
		}
    	   	
    }
    
    public static void getOverview() throws Exception {
    	
    	String zeroshares = (String)requestParams.get("zeroshares");
    	 
    	
		PreparedStatement ps = null;
		Connection con = null;
		ResultSet rs = null;
	
		JsonArray  jsonRows = new JsonArray();
		
		try {
						
			con = HikariCP.getConnection();
			String showzeroshares = (zeroshares.equals("false")) ? "WHERE shares > shares_sold" : "";
			String sql = " SELECT x.* " +
	                     "\n ,SUM(CASE WHEN d.activity_date >= DATE_TRUNC('year', NOW()) THEN d.price*d.shares ELSE 0 END) div_ytd " +
	                     "\n ,SUM(d.price*d.shares) div_all " +
	                     "\n FROM( " +
		                 "\n SELECT portfolio_id, name, ticker, brokerage, divperiod, "+
					     "\n        SUM(shares_owned) AS shares,SUM(tot) AS COST, " +
					     "\n        CASE WHEN SUM(shares_owned)=0 THEN 0 else SUM(tot)/SUM(shares_owned) end AS avg_cost " +
					     "\n FROM( " +
					     "\n SELECT portfolio_id, name, brokerage,ticker,divperiod, " +
					     "\n    shares-shares_sold shares_owned, " +
					     "\n    (shares-shares_sold)*price AS tot " +
					     "\n FROM( " +
						 "\n SELECT h.portfolio_id, " + 
					     "\n        h.name, " + 
						 "\n        h.ticker,  " +
						 "\n        b.name AS brokerage,  " +
						 "\n        h.divperiod,  " +
						 "\n        t.id as transaction_id, " +
						 "\n        t.shares, " +
						 "\n        t.price, " +
						 "\n        SUM(COALESCE(s.shares,0)) AS shares_sold " +						 
						 "\n FROM portfolio.holdings h " +
                                                 "\n join portfolio.brokerage b " + 
                                                 "\n   on h.brokerage_id = b.id " + 
						 "\n left outer JOIN portfolio.trades t " +
						 "\n   ON h.ticker = t.ticker " +
						 "\n  AND h.portfolio_id = t.portfolio_id " +
						 "\n  AND t.activity_type IN ('buy', 'drip') " +
						 "\n LEFT OUTER JOIN portfolio.salesdetail s " +
						 "\n   ON t.id = s.buy_trades_id " +
						 "\n  AND t.portfolio_id = s.portfolio_id " +
						 "\n WHERE h.portfolio_id = ? " +						 
						 "\n GROUP BY 1,2,3,4,5,6,7,8 " +
						 "\n )z " +
						 "\n " + showzeroshares +
						 "\n )y " + 
						 "\n GROUP BY 1,2,3,4,5 " + 
						 "\n )x " + 
						 "\n LEFT OUTER JOIN portfolio.trades d " + 
						 "\n   ON x.ticker = d.ticker " +
						 "\n  AND x.portfolio_id = d.portfolio_id " +
						 "\nAND d.activity_type in ('dividend','lt gain','st gain') " + 
						 "\nGROUP BY 1,2,3,4,5,6,7,8  " +						 
						 "\nORDER BY name";
			
			
			Logger.debug(sql);
			
			
			ps = con.prepareStatement(sql);
			ps.setInt(1,portfolioId);
			
			rs = ps.executeQuery();
			
			ResultSetMetaData rsMD = rs.getMetaData();
			int colCount = rsMD.getColumnCount();
			
			HashMap sessionHash = Cache.get(session.getId(), HashMap.class);
			
			HashMap quotes = (HashMap) sessionHash.get("quotes");
			
			while(rs.next()){
				JsonObject jsonRow = new JsonObject();
				
				
				for (int c = 1; c <= colCount; c++) {
					String colHeader = rsMD.getColumnName(c);
					
					String colVal = "";
					
					try {
						colVal = rs.getString(colHeader).trim();
					} catch (Exception e) {
						colVal = rs.getString(colHeader);
					}
					if (colVal == null)
						colVal = "";
					
					if(colVal.equals("")){
						jsonRow.addProperty(colHeader, "");
					}
					else if(StringUtils.isNumeric(colVal)){
						jsonRow.addProperty(colHeader, Integer.parseInt(colVal));
					}
					else if(StringUtils.isNumeric(colVal.replaceAll("\\.","")) && StringUtils.countMatches(colVal,".") <= 1 ){
						jsonRow.addProperty(colHeader, Double.parseDouble(colVal));						
					}
					else{
						jsonRow.addProperty(colHeader, colVal);
					}
				}
				 
				if(quotes.containsKey(jsonRow.get("ticker").getAsString())){

					double shares = jsonRow.get("shares").getAsDouble();
				    double price = ((Quote)(quotes.get(jsonRow.get("ticker").getAsString()))).getPrice();
					jsonRow.addProperty("price", price );
					jsonRow.addProperty("val", shares*price );
					double gainloss = (shares*price)-(shares*jsonRow.get("avg_cost").getAsDouble());
					jsonRow.addProperty("gainloss", gainloss );
				}
				else{
					jsonRow.addProperty("price", 0);
					jsonRow.addProperty("val", 0);
					jsonRow.addProperty("gainloss", 0 );
				}
				 
				

				// add row to returning result set
				jsonRows.add(jsonRow);				
			}
			
			renderJSON(jsonRows);
		
		} 
		catch (Exception e) {
			Logger.error("getOverview caught exception " + e.getMessage());
			throw new Exception("ERROR: getOverview() " + e.getMessage());  
		} 
		finally {
			
			try {				
				rs.close();
				ps.close();
				HikariCP.close();
				
				// use the Play connection close method !important
				if(!con.isClosed()) con.close();
			} catch (SQLException e) {
				Logger.error("getOverview: Error closing connection " + e.getMessage());
			} 
		}
    	   	
    }
    
    public static void addPosition() throws Exception {
    	 
    	String ticker = (String)requestParams.get("ticker");
    	String date = (String)requestParams.get("date");
    	String shares = (String)requestParams.get("shares");
    	String price = (String)requestParams.get("price");
    	
 	PreparedStatement ps = null;
 	Connection con = null;
        ResultSet rs = null;
 	
 		try {
 			con = HikariCP.getConnection();
 			ps = con.prepareStatement("SELECT MAX(ID)+1 as MAXID FROM portfolio.trades");
 			rs = ps.executeQuery();
 			int nextid=0;
 			while(rs.next()){
 				String maxid = rs.getString("MAXID");
 				nextid = Integer.parseInt(maxid);
 			}
 			ps.close();
 			rs.close();
 				
 			if(nextid == 0) throw new Exception("Max id not determined");
 			
 			String sql = "INSERT INTO portfolio.trades(id,portfolio_id,ticker,activity_type,activity_date,price,shares) VALUES (?,?,?,'buy',?,?,?)";
 			Logger.debug(sql);
 			
 			ps = con.prepareStatement(sql);
 			ps.setInt(1, nextid);
 			ps.setInt(2, portfolioId);
 			ps.setString(3, ticker);
 			ps.setDate(4, java.sql.Date.valueOf(date)); 			 			
 			ps.setDouble(5, Double.parseDouble(price));
 			ps.setDouble(6, Double.parseDouble(shares));
 			
 			
 			ps.executeUpdate();
 			
 			renderJSON("{\"success\":true}");
 		
 		} 
 		catch (Exception e) {
 			Logger.error("addPosition caught exception " + e.getMessage());
 			throw new Exception("ERROR: addPosition() " + e.getMessage());  
 		} 
 		finally {
 			
 			try {				
                                DbUtils.closeQuietly(ps);
 				DbUtils.closeQuietly(rs);
 				HikariCP.close();
 				
 				// use the Play connection close method !important
 				if(!con.isClosed()) con.close();
 			} catch (SQLException e) {
 				Logger.error("addPosition: Error closing connection " + e.getMessage());
 			} 
 		}
     	   	
     }    

    public static void newHolding() throws Exception {
   	 
    	String ticker = (String)requestParams.get("ticker");
    	String name = (String)requestParams.get("name");
    	String brokerage = (String)requestParams.get("brokerage");
    	String drip = (String)requestParams.get("drip");
    	String period = (String)requestParams.get("period");
    	String sector = (String)requestParams.get("sector");
    	
 		PreparedStatement ps = null;
 		Connection con = null;
 	
 		
 		try {
 			con = HikariCP.getConnection();
 			
 			String sql = "INSERT INTO portfolio.holdings (portfolio_id,ticker,name,brokerage_id,drip,divperiod,sector) VALUES (?,?,?,?,?,?,?)";
 			Logger.debug(sql);
 			
 			ps = con.prepareStatement(sql);
 			ps.setInt(1, portfolioId);
 			ps.setString(2, ticker);
 			ps.setString(3, name);
 			ps.setInt(4, Integer.parseInt(brokerage));
 			ps.setInt(5, Integer.parseInt(drip)); 			 			
 			ps.setInt(6, Integer.parseInt(period));
 			ps.setString(7, sector);
 			
 			ps.executeUpdate();
 			
 			renderJSON("{\"success\":true}");
 		
 		} 
 		catch (Exception e) {
 			Logger.error("newHolding caught exception " + e.getMessage());
 			throw new Exception("ERROR: newHolding() " + e.getMessage());  
 		} 
 		finally {
 			
 			try {				
 				ps.close();
 				HikariCP.close();
 				
 				// use the Play connection close method !important
 				if(!con.isClosed()) con.close();
 			} catch (SQLException e) {
 				Logger.error("newHolding: Error closing connection " + e.getMessage());
 			} 
 		}
     	   	
     }    
    
    public static void sellPosition() throws Exception {
   
	    
    	String ticker = (String)requestParams.get("ticker");
    	String date = (String)requestParams.get("date");
    	String sshares = (String)requestParams.get("shares");
    	double shares = Double.parseDouble(sshares);
    	String price = (String)requestParams.get("price");
    	
 		PreparedStatement ps = null;
 		PreparedStatement psDetail = null;
 		Connection con = null;
 		ResultSet rs = null; 		
 		
 		try {
 			con = HikariCP.getConnection();
 			ps = con.prepareStatement("SELECT MAX(ID)+1 as MAXID FROM portfolio.trades");
 			rs = ps.executeQuery();
 			int sales_trade_id=0;
 			while(rs.next()){
 				String maxid = rs.getString("MAXID");
 				sales_trade_id = Integer.parseInt(maxid);
 			}
 			ps.close();
 			rs.close();
 				
 			if(sales_trade_id == 0) throw new Exception("ERROR: sellPosition() unable to determine sales transaction id");  
 			
 			// insert the sell transaction
 			String sql = "INSERT INTO portfolio.trades (id,portfolio_id,ticker,activity_type,activity_date,price,shares) VALUES (?,?,?,'sell',?,?,?)";
 			
 			ps = con.prepareStatement(sql);
 			ps.setInt(1, sales_trade_id);
 			ps.setInt(2, portfolioId);
 			ps.setString(3, ticker);
 			ps.setDate(4, java.sql.Date.valueOf(date)); 			 			
 			ps.setDouble(5, Double.parseDouble(price));
 			ps.setDouble(6, shares);
 			
 			ps.executeUpdate();
 			ps.close();

 			
 			// get all purchases that still have shares for this holding 			
 			sql = "SELECT x.*, shares-shares_sold shares_at_this_price " +
 			      "\nFROM ( " + 
 			      "\n  SELECT t.id AS buy_id " +
 	 			  "\n       ,t.activity_date " +
 	 			  "\n       ,t.shares " +
 	 			  "\n       ,t.price " + 
 	 			  "\n       ,SUM(COALESCE(s.shares,0)) shares_sold " +
 	 			  "\n  FROM portfolio.holdings h  " +
 	 			  "\n  JOIN portfolio.trades t  " +
 	 			  "\n    ON h.ticker = t.ticker  " +
 	 			  "\n   AND h.portfolio_id = t.portfolio_id  " +
 	 			  "\n  LEFT OUTER JOIN portfolio.salesdetail s " + 
 	 			  "\n    ON t.id = s.buy_trades_id " +
 	 			  "\n   AND t.portfolio_id = s.portfolio_id  " +
 	 			  "\n  WHERE h.portfolio_id = ? " +
 			      "\n    AND h.ticker = ? " +
 	 			  "\n    AND t.activity_type IN ('buy', 'drip') " + 
 	 			  "\n  GROUP BY 1,2,3,4 " +
 	 			  "\n )x " +
 	 			  "\nWHERE shares > shares_sold " +
 	 			  "\nORDER BY activity_date ";
 	 			   	
 			ps = con.prepareStatement(sql);
 			ps.setInt(1, portfolioId);
 			ps.setString(2, ticker);
 			rs = ps.executeQuery();

 			psDetail = con.prepareStatement("INSERT INTO portfolio.salesdetail " +
 			                                "(ts,portfolio_id,ticker, trades_id,buy_trades_id,sales_date,shares,sale_price,basis,gain_loss) " +
 			                                "VALUES (now(),?,?,?,?,?,?,?,?,?);");
 			
 			// determine the cost basis and create the salesdetail record(s)
 			while(rs.next()){
 			
 				double s = rs.getDouble("shares_at_this_price");
 				String basis = rs.getString("price");
 				int buyid = rs.getInt("buy_id");
 			 	 
 				
 				// This single buy transaction still has enough shares to cover the entire sale
 				if(s >= shares){
 					
 					double gainloss = (shares * Double.parseDouble(price)) - (shares * Double.parseDouble(basis));
 					
 					psDetail.setInt(1, portfolioId);
 					psDetail.setString(2, ticker);	
 					psDetail.setInt(3, sales_trade_id);
 					psDetail.setInt(4, buyid);
 					psDetail.setDate(5, java.sql.Date.valueOf(date)); 			 			
 					psDetail.setDouble(6, shares); //all shares at this price
 					psDetail.setDouble(7, Double.parseDouble(price));
 					psDetail.setDouble(8, Double.parseDouble(basis));
 					psDetail.setDouble(9, gainloss);
 		 			 					
 					psDetail.addBatch();
 					break;
 				}
 				else{
 					double gainloss = (s * Double.parseDouble(price)) - (s * Double.parseDouble(basis));
 					 					
 					// insert record for partial cover 		
 					psDetail.setInt(1, portfolioId);
 					psDetail.setString(2, ticker);
 					psDetail.setInt(3, sales_trade_id);
 					psDetail.setInt(4, buyid);
 					psDetail.setDate(5, java.sql.Date.valueOf(date)); 			 			
 					psDetail.setDouble(6, s); // only the shares at this price
 					psDetail.setDouble(7, Double.parseDouble(price));
 					psDetail.setDouble(8, Double.parseDouble(basis));
 					psDetail.setDouble(9, gainloss);
 		 			 					
 					psDetail.addBatch();	 					
 					
 					// decrement shares based on how many this buy covered
 				    shares = shares - s;
 				}
 				
 			}
 			
 			rs.close();
 			ps.close();
 			
 			psDetail.executeBatch();
 			psDetail.close();
 			
 			renderJSON("{\"success\":true}");
 		
 		} 
 		catch (Exception e) {
 			Logger.error("sellPosition caught exception " + e.getMessage());
 			throw new Exception("ERROR: sellPosition() " + e.getMessage());  
 		} 
 		finally {
 			
 			try {		
 				rs.close();
 				ps.close();
 				psDetail.close();
 				HikariCP.close();
 				
 			} catch (SQLException e) {
 				Logger.error("sellPosition: Error closing connection " + e.getMessage());
 			} 
 		}
     	   	
     }        
  
    public static void recordDividend()throws Exception {

    	String type = (String)requestParams.get("type");
    	String ticker = (String)requestParams.get("ticker");
    	String exdate = (String)requestParams.get("exdate");
    	String paydate = (String)requestParams.get("paydate");
    	String divamount = (String)requestParams.get("divamount");
    	  
    	Logger.info("Recording dividend " + ticker + " $" + divamount);

    	boolean drip = false;
    	if(((String)requestParams.get("drip")).equals("true")) drip = true;
    	
    	PreparedStatement ps = null;
 		Connection con = null;
 		ResultSet rs = null; 		
 		
 		try {
 			con = HikariCP.getConnection();

 	    	// how many shares owned as of the ex-div date
 	    	String sql = "SELECT COALESCE(SUM(CASE WHEN activity_type='sell' THEN shares*-1 ELSE shares END),0) AS shares_owned " +
                         "\n FROM portfolio.trades " +
                         "\n WHERE portfolio_id = ? " +
                         "\n   AND ticker = ? " +
                         "\n   AND activity_date < ?  " +
                         "\n   AND activity_type IN ('buy', 'drip', 'sell') ";
 			
 	    	Logger.debug(sql);
 			ps = con.prepareStatement(sql);
 			ps.setInt(1, portfolioId); 
 			ps.setString(2, ticker);
 			
 			String querydate = exdate;
 			// if this is a long term gain entry, shares must have been owned prior to 1 year from ex-date
 			if(type.equals("lt gain")) {
 				DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
 				DateTime dt = formatter.parseDateTime(exdate);
 				querydate = dt.minusYears(1).toString(formatter);
 			}
 			ps.setDate(3, java.sql.Date.valueOf(querydate));

 			rs = ps.executeQuery();

 			double shares_owned = 0.0;
 			
 			while(rs.next()){
 	 			String s = rs.getString("shares_owned");
 				shares_owned = Double.parseDouble(s);
 			} 			
 			
 			rs.close(); 			
 			ps.close();
 			
 			if(shares_owned == 0.0){
 				Logger.info("0 shares owned by ex-div date");
 				renderJSON("{\"success\":true}");
 			} 			
 			Logger.info("shares owned by ex-div date of " + exdate + " was " + shares_owned);
 	    	
 			ps = con.prepareStatement("SELECT MAX(ID)+1 as MAXID FROM portfolio.trades");
 			rs = ps.executeQuery();
 			int div_trade_id=0;
 			while(rs.next()){
 				String maxid = rs.getString("MAXID");
 				div_trade_id = Integer.parseInt(maxid);
 			}
 			ps.close();
 			rs.close();
 			
 			if(div_trade_id == 0) throw new Exception("ERROR: recordDividend() unable to determine dividend transaction id");  
 			
 	    	// insert dividend/st or lt gain paid record
 			sql = "INSERT INTO portfolio.trades(id, portfolio_id, ticker,activity_type,activity_date,price,shares) VALUES (?,?,?,?,?,?,?)";
 			
 			ps = con.prepareStatement(sql);
 			ps.setInt(1, div_trade_id);
 			ps.setInt(2, portfolioId);
 			ps.setString(3, ticker);
 			ps.setString(4, type);
 			ps.setDate(5, java.sql.Date.valueOf(paydate)); 			 			
 			ps.setDouble(6, Double.parseDouble(divamount));
 			ps.setDouble(7, shares_owned);
 			
 			ps.executeUpdate();
 			ps.close();

 			

 	    	// if this is a drip then purchase shares with the $$$    	    
 	    	if(drip){
 	    		String reinvestprice = (String)requestParams.get("reinvestprice");
 	    		
 	    		double dripshares = (shares_owned * Double.parseDouble(divamount)) / Double.parseDouble(reinvestprice);
		 	    Logger.info("Dividend re-invest shares purchased " + dripshares);
		 	    
 	 			sql = "INSERT INTO portfolio.trades(id,portfolio_id,ticker,activity_type,activity_date,price,shares) VALUES (?,?,?,'drip',?,?,?);";
 	 			
 	 			ps = con.prepareStatement(sql);
 	 			ps.setInt(1, div_trade_id+1);
 	 			ps.setInt(2, portfolioId);
 	 			ps.setString(3, ticker);
 	 			ps.setDate(4, java.sql.Date.valueOf(paydate)); 			 			
 	 			ps.setDouble(5, Double.parseDouble(reinvestprice));
 	 			ps.setDouble(6, dripshares);
 	 			
 	 			ps.executeUpdate();
 	 			ps.close(); 	    		
 	    	}
 			
 			renderJSON("{\"success\":true}");
 		
 		} 
 		catch (Exception e) {
 			Logger.error("recordDividend caught exception " + e.getMessage());
 			throw new Exception("ERROR: recordDividend() " + e.getMessage());  
 		} 
 		finally {
 			
 			try {		
 				rs.close();
 				ps.close(); 
 				HikariCP.close();
 				
 			} catch (SQLException e) {
 				Logger.error("recordDividend: Error closing connection " + e.getMessage());
 			} 
 		}
    	
    	
    	
    	renderJSON("{\"success\":true}");
    }
    
    public static void dividendSankey() throws Exception {
      	 
    	String yr = (String)requestParams.get("yr");
    	
		PreparedStatement ps = null;
		Connection con = null;
		ResultSet rs = null;
	 
		JsonObject jsonO = new JsonObject();
		
		try {
			con = HikariCP.getConnection();
			
			String sql = "\n SELECT nodes FROM ( " +
					     "\n  SELECT 1 AS indx, sector as nodes " +
					     "\n  FROM portfolio.holdings " +
					     "\n  WHERE portfolio_id = ? " +
					     "\n    and ticker IN (SELECT ticker " +
					     "\n                   FROM portfolio.trades " +
					     "\n                   WHERE portfolio_id = ? " +
					     "\n                     and extract(year from activity_date) = ? " +
					     "\n                     and activity_type in ('dividend','lt gain','st gain')) " +					     
					     "\n  GROUP BY 1,2 " +
					     "\n  UNION ALL  " +
					     "\n  SELECT 0 AS indx, ticker as nodes " +
					     "\n  FROM portfolio.holdings " +
					     "\n  WHERE portfolio_id = ? " +
					     "\n    and ticker IN (SELECT ticker " +
					     "\n                   FROM portfolio.trades " +
					     "\n                   WHERE portfolio_id = ? " +
					     "\n                     and extract(year from activity_date) = ? " +
					     "\n                     and activity_type in ('dividend','lt gain','st gain')) " +
					     "\n  GROUP BY 1,2 " +
					     "\n  UNION ALL  " +
					     "\n  SELECT 3 AS indx, 'Dividends' AS nodes " +					     
					     "\n)z  " +
					     "\nORDER BY indx,1";
			
			
			Logger.debug(sql);
			
			
			ps = con.prepareStatement(sql);
			ps.setInt(1, portfolioId);
			ps.setInt(2, portfolioId);
			ps.setInt(3, Integer.parseInt(yr));
			ps.setInt(4, portfolioId);
			ps.setInt(5, portfolioId);
			ps.setInt(6, Integer.parseInt(yr));		
			
			rs = ps.executeQuery();
			
			JsonArray jsonNodes = new JsonArray();
			
			while(rs.next()){
				JsonObject o = new JsonObject();
				
				String node = rs.getString("nodes");
				o.addProperty("name",  node);
				jsonNodes.add(o);				
			}

			jsonO.add("nodes", jsonNodes);
			
			ps.close();
			rs.close();
			
			
			sql = "\n SELECT src, tgt, divs " +
				  "\n FROM ( " +
				  "\n SELECT 0 AS indx, h.ticker AS src, h.sector AS tgt " +
				  "\n        ,SUM(shares*price) DIVs " +
				  "\n FROM portfolio.holdings h " +
				  "\n JOIN portfolio.trades t " +
				  "\n   ON h.portfolio_id = t.portfolio_id " +
				  "\n  AND h.ticker = t.ticker " +
				  "\n WHERE h.portfolio_id = ? " +
				  "\n   AND EXTRACT(YEAR from activity_date) = ? " +
				  "\n   AND t.activity_type in ('dividend','lt gain','st gain') " +
				  "\n GROUP BY 1,2,3 " +
				  "\n UNION ALL  " +
				  "\n SELECT 1 AS indx, sector AS src, 'Dividends' AS tgt " +
				  "\n        ,SUM(shares*price) DIVs " +
				  "\n FROM portfolio.holdings h " +
				  "\n JOIN portfolio.trades t " +
				  "\n   ON h.portfolio_id = t.portfolio_id " +
				  "\n  AND h.ticker = t.ticker " +
				  "\n WHERE h.portfolio_id = ? " +
				  "\n   AND EXTRACT(YEAR from activity_date) = ? " +
				  "\n   AND t.activity_type in ('dividend','lt gain','st gain') " +
				  "\n GROUP BY 1,2,3 " +
				  "\n )z " +
				  "\n WHERE DIVS>0 " + 
				  "\n ORDER BY indx,1";		
		
			Logger.debug(sql);
			
			
			ps = con.prepareStatement(sql);
			ps.setInt(1, portfolioId);
			ps.setInt(2, Integer.parseInt(yr));
			ps.setInt(3, portfolioId);
			ps.setInt(4, Integer.parseInt(yr));		
			
			rs = ps.executeQuery();
			
			JsonArray jsonLinks = new JsonArray();
			
			while(rs.next()){
				JsonObject o = new JsonObject();
				
				String src = rs.getString("src").trim();
				String tgt = rs.getString("tgt").trim();
				String divs = rs.getString("divs");
				o.addProperty("source",  src);
				o.addProperty("target",  tgt);
				o.addProperty("value",  divs);
				
				jsonLinks.add(o);				
			}
	
			jsonO.add("links", jsonLinks);
			
			ps.close();
			rs.close();
			
			
			renderJSON(jsonO);
		
		} 
		catch (Exception e) {
			Logger.error("dividendSankey caught exception " + e.getMessage());
			throw new Exception("ERROR: dividendSankey() " + e.getMessage());  
		} 
		finally {
			
			try {				
				rs.close();
				ps.close();
				HikariCP.close();
				
				// use the Play connection close method !important
				if(!con.isClosed()) con.close();
			} catch (SQLException e) {
				Logger.error("dividendSankey: Error closing connection " + e.getMessage());
			} 
		}
    	   	
    }     
    
    @Catch
    public static void handleExceptions(Throwable e) {
        if (e != null) {
            String result = "{ status: 500, msg: '" + e.getMessage() + "', errId: 1 }";
            response.status = 500;
            renderJSON(result);
        }
    }

}
