function buildYTDChart( o, yr, ytdProjection ){
   	
	var cy = 0;
	var py = 0;	    		

	$.each(o, function(index, rec){
    	var activity_date = moment(rec.activity_date);
		var activity_year = activity_date.year();
		var activity_month = activity_date.month();
		
		var payout = rec.shares*rec.price;
 		
		if(yr < moment().format("YYYY")){
			if(activity_year == yr){
				console.log('adding cy ' + rec.ticker + ": "  +rec.activity_date + ", " + payout)
				cy+=payout;
		}
			else if(activity_year == yr-1){
				
				py+=payout;	
			}
		}
		else{
			if(activity_year == yr) 
				cy+=payout;
			else if(activity_year == yr-1 && activity_month <= moment().month()) 
				py+=payout;
		}
			
    });
	
	var data = {pylabel: yr-1, cylabel: yr, cy:cy, py:py}
	var ymax = Math.max(ytdProjection.cy, ytdProjection.py)
	 
	Highcharts.chart('ytd-dividend-chart', {
		 chart: {
               type:'column',
            backgroundColor: '#f2f2f2'
		 },
         title: {
             text: ''
         },
         subtitle: {
             text: ''
         },
         legend: {
         	borderWidth: 0
         },
         credits:{
         	enabled: false
         },
         tooltip: {
             formatter: function () {
            	 
                 return '<b>' + this.series.name + ' YTD<br/> $' +
                      + this.y.toFixed(2) + '</b>';
             }
         },
         xAxis: {
             categories: [
                 data.pylabel + ' vs ' + data.cylabel + ' YTD'
             ],
             crosshair: true
         },
	     yAxis: [{
	       max: ymax,
		   tickInterval: 100,
		   title: {
		       text: null
		   },
		   labels: {
			   formatter: function () {
			   	  return '$' + $.number(this.value);
		       }
	  	   }
	      }
	    ],
	    legend: {
	        enabled: false
	    },
	    plotOptions: {
	    	column: {
	            borderRadius: 5,
	            pointWidth: 45
	        }
	    },
	     
		series:[{
	        name: data.pylabel,
	        data: [data.py]
	    },{
	        name: data.cylabel,
	        data: [data.cy] 
	    }]
	});
	
}	   
 		