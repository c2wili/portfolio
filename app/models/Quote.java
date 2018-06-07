package models;

public class Quote {
	private String symbol;
	private String price;
	private String volume;
	private String timestamp;

	public Quote(String _ticker, String _price) {
		super();

		this.symbol = _ticker;
		this.price = _price;
		this.volume = "";
		this.timestamp = "";
		
	}
	@Override
	public String toString() {
		return symbol+":"+price ;
	}
	public String getTicker() {
		return symbol;
	}
	public double getPrice() {
		return Double.parseDouble(price);
	}
	public String getTimestamp() {
		return timestamp;
	}
	
}
