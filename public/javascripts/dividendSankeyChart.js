function buildSankeyChart( o, yr ){
   	
	var cy = 0;
	var py = 0;	    		
	
	var series = [];
	var tmpsankeydata = [];

	$.each(o, function(index, rec){
    	var activity_date = moment(rec.activity_date);
		var activity_year = activity_date.year();
		var activity_month = activity_date.month();
		
		var payout = rec.shares*rec.price;
		
		if(activity_year == yr){
			
			if(series[rec.ticker] == null) series[rec.ticker] = 0;			
			series[rec.ticker]+= rec.price*rec.shares;
			
			tmpsankeydata[rec.ticker] = [ rec.ticker, rec.sector, series[rec.ticker] ]
			
			if(series[rec.sector] == null) series[rec.sector] = 0;			
			series[rec.sector]+= rec.price*rec.shares;
			
			tmpsankeydata[rec.sector] = [ rec.sector, 'Dividends', series[rec.sector] ]			
		}
			
    });
	
	var sankeydata = [];
	for(key in tmpsankeydata){
		sankeydata.push(tmpsankeydata[key]);
	}
	 
	Highcharts.chart('sankey-chart', {
		chart:{ 
            backgroundColor: '#f2f2f2'
		},
	    title: {
	        text: ''
	    },
        credits:{
         	enabled: false
        },
        plotOptions:{
        	series:{
        		colors: [ '#00e5b5', '#00dcb6', '#01d3b8', '#01caba', '#02c1bc', '#02b9be', '#03b0bf', '#03a7c1', '#049ec3', '#0495c5', '#058dc7'],
        		dataLabels:{
        			color: '#000000',
        			borderColor: '#000000'
        				
        		}
        	}
        },
	    series: [{
	        keys: ['from', 'to', 'weight'],
	        data: sankeydata,
	        type: 'sankey'
	        
	    }]

	});
}	   
 		