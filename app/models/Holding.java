package models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Holding {
	private String ticker;
	private String name;
	private int brokerage;
	private int portfolio;
	private double gain_loss = 0.00;
	private double div_gain = 0.00;
	private double basis = 0.00;
	private double value = 0.00;
	private double current_price = 0.00;
	private double shares = 0.00000;
	
	private JsonArray history;

	public Holding(int _portfolio, String _ticker, String _name, int _brokerage) {
		super();

		this.portfolio = _portfolio;
		this.ticker = _ticker;
		this.brokerage = _brokerage;
		this.name = _name;
		this.history = new JsonArray();
		
	}
	
	public void updatePrice( String price ){
		this.current_price = Double.parseDouble(price);
	}
	public String getTicker() {
		return this.ticker;
	}
 
	public JsonArray getHistory() {
		return this.history;
	}
	
	public void processHistory(){
		
		
		for (JsonElement h : this.history) {
			JsonObject o = h.getAsJsonObject();
			
			if(o.get("activity_type").getAsString().equals("buy") || o.get("activity_type").getAsString().equals("drip")){
						
				// add shares to position
				this.shares+= o.get("shares").getAsDouble();				
			}
			else if(o.get("activity_type").getAsString().equals("sell") ){

				if(this.ticker.equals("VWENX")){
					//System.out.println("** SALE SALE SALE: " + o);	
				}				

				double sharesSold = o.get("shares").getAsDouble();
				double salePrice = o.get("price").getAsDouble();
				
				// subtract shares from position
				this.shares-= sharesSold;

				// update the buy history record for shares_sold and determine gain/loss
				for (JsonElement h2 : this.history) {
					JsonObject o2 = h2.getAsJsonObject();
					
					if(o2.get("activity_type").getAsString().equals("buy") || o2.get("activity_type").getAsString().equals("drip")){
 
						double sharesStillOwned = (o2.get("shares").getAsDouble() - o2.get("shares_sold").getAsDouble());

						// if not all shares from this purchase are sold yet
						if(sharesStillOwned > 0){							

							if(this.ticker.equals("VWENX")){
								//System.out.println("***** sharesSold remaining: " + sharesSold);
								//System.out.println("***** sharesStillOwned remaining: " + sharesStillOwned);
								//System.out.println("***** buy record: " + o2);								
								//System.out.println("**");
							}
							
							// sale covers this entire buy
							if(sharesSold >= sharesStillOwned){
								
								sharesSold-= sharesStillOwned;
								double gainloss = o2.get("gain_loss").getAsDouble()
										          +
										         ( (sharesStillOwned * salePrice)
										           -
										         (sharesStillOwned * o2.get("price").getAsDouble()) );
								
								o2.addProperty("gain_loss", gainloss);
								o2.addProperty("shares_sold", o2.get("shares").getAsDouble());
								this.gain_loss+= gainloss;
								
							}
							else{
								double gainloss = o2.get("gain_loss").getAsDouble()
								                   +
								                  ( (sharesSold * salePrice)
								                    -
								                  (sharesSold * o2.get("price").getAsDouble()) );
						        o2.addProperty("gain_loss", gainloss);
						        o2.addProperty("shares_sold", sharesSold+o2.get("shares_sold").getAsDouble());
						        
						        this.gain_loss+= gainloss;
								sharesSold = 0;
							}
						}
						else{
							//move on to the next buy record
							//System.out.println("***************all shares sold move on to the next one.");
						}
	/* 					
						if(this.ticker.equals("VWENX")){
							System.out.println("***** buy record: " + o2);
							System.out.println("*******************************************************************");
						}
 */
					}
					
					if(sharesSold == 0) break;
				}
			}
			else if(o.get("activity_type").getAsString().equals("dividend") || o.get("activity_type").getAsString().equals("lt gain") || o.get("activity_type").getAsString().equals("st gain")){
				
				// add to div gains
				this.div_gain+= o.get("shares").getAsDouble() * o.get("price").getAsDouble();		
				this.gain_loss+= o.get("shares").getAsDouble() * o.get("price").getAsDouble();	
			}
		}
		
		// update current value
		this.value = this.shares * this.current_price;
		
		// update cost basis
		double cb = 0.00;
		double so = 0.00;
		for (JsonElement h : this.history) {
			JsonObject o = h.getAsJsonObject();
			
			// all purchase records
			if(o.get("activity_type").getAsString().equals("buy") || o.get("activity_type").getAsString().equals("drip")){
				// that still have owned shares
				double sharesOwned = (o.get("shares").getAsDouble() - o.get("shares_sold").getAsDouble());
				if(this.ticker.equals("VWENX")){
					//System.out.println("sharesOwnded: " + sharesOwned + "|" + o);
				}
				if( sharesOwned > 0){
					if(this.ticker.equals("VWENX")){
						//System.out.println("sharesOwnded: " + sharesOwned + " use in basis calc");
					}
					cb = ( (so * cb) + ( sharesOwned * o.get("price").getAsDouble()) )
					       /
					     (so + sharesOwned);
					so+= sharesOwned;
				}
			}
		}
		this.basis = cb;
	}
	
	@Override
	public String toString(){
		
		StringBuffer sb = new StringBuffer();
		String brk = "";
		try{
		for (JsonElement h : this.history) {
			JsonObject o = h.getAsJsonObject();
			sb.append(brk + 
					  o.get("id").getAsInt() + "," +
					  o.get("portfolio_id").getAsInt() + "," +
					  o.get("ticker").getAsString() + "," +
					  o.get("brokerage_id").getAsInt() + "," +
					  o.get("activity_type").getAsString() + "," +
					  o.get("activity_date").getAsString() + "," +
					  o.get("price").getAsDouble() + "," +
					  o.get("shares").getAsDouble() + "," +
					  o.get("shares_sold").getAsDouble() + "," +
					  o.get("gain_loss").getAsDouble()	
					   
					);
			brk = "|";
		}
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
		return "******\n   ticker: " + this.ticker + 
			   "\n   name: " + this.name +
			   "\n   shares: " + this.shares +
			   "\n   current_price: " + this.current_price +
			   "\n   basis: " + this.basis +
			   "\n   value: " + this.value +
			   "\n   div_gain: " + this.div_gain +
			   "\n   gain_loss: " + this.gain_loss +
			   "\n   history: " + this.history +
			   "\n   history sb: " + sb + 
			   "\n*******";
	}
}
