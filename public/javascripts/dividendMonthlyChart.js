function buildMonthlyChart( o, yr ){

	var monthlyTotals = [];
	monthlyTotals['cy'] = [0,0,0,0,0,0,0,0,0,0,0,0]
	monthlyTotals['py'] = [0,0,0,0,0,0,0,0,0,0,0,0]	    		

	$.each(o, function(index, rec){
    	var activity_date = moment(rec.activity_date);
		var activity_year = activity_date.year();
		var activity_month = activity_date.month();
		
		var payout = rec.shares*rec.price;
		
		if(activity_year == yr)			
			monthlyTotals['cy'][activity_month]+=payout;
		else if(activity_year == yr-1)
			monthlyTotals['py'][activity_month]+=payout;
    });
	
	var data = {pylabel: yr-1, cylabel: yr, cy:monthlyTotals['cy'], py:monthlyTotals['py']}
    
	Highcharts.chart('monthly-dividend-chart', {
		 chart: {
             type:'column'
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
                 return '<b>' + this.x + ' ' + this.series.name + '<br/> $' +
                      + $.number(this.y,2) + '</b>';
             }
         },
         xAxis: {
             categories: [
                 'Jan',
                 'Feb',
                 'Mar',
                 'Apr',
                 'May',
                 'Jun',
                 'Jul',
                 'Aug',
                 'Sep',
                 'Oct',
                 'Nov',
                 'Dec'
             ],
             crosshair: true
         },
	     yAxis: [{
	       tickInterval: 10,
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
	    plotOptions: {
	    	column: {
	            borderRadius: 3
	        },
	        series: {
	        	pointWidth: 10
	        }		             
	    },
	     
		series:[{
	        name: data.pylabel,
	        data: data.py
	    },{
	        name: data.cylabel,
	        data: data.cy
	    }]
	});		    	 
}
