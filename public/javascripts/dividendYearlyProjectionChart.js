function buildProjectedChart( o, yr, ytdProjection ){
 	
 
	var py = 0;	    		

	$.each(o, function(index, rec){
    	var activity_date = moment(rec.activity_date);
		var activity_year = activity_date.year();
		
		var payout = rec.shares*rec.price;
		if(activity_year == yr-1) 
			py+=payout;
			
    });
	
	var data = {pylabel: yr-1, cylabel: yr, cy:ytdProjection, py:py}
    
	Highcharts.chart('proj-dividend-chart', {
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
                     + this.y.toFixed(2) + '</b>';
            }
        },
        xAxis: {
            categories: [
           	 data.pylabel + ' vs ' + data.cylabel + ' Projected'
            ],
            crosshair: true
        },
	     yAxis: [{
	       max: ytdProjection,
	       tickInterval: 100,
		   opposite: true,
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
	        name: data.pylabel + ' Actual',
	        data: [ data.py ]
	    },{
	        name: data.cylabel,
	        data: [ data.cy ] 
	    }]
	});	
	
}	   
 		