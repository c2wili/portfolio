

function buildSummaryCharts( portfolios ){
	
 
	Highcharts.chart('all-accounts-pie', {
		 chart: {
            type:'pie',
            height:90,
            width:100,
            backgroundColor: 'transparent'
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
        	   console.log(this)
               return '<b>' + this.key + '<br/> $' + $.number(this.y,2) + '</b>';
           }
       },
       plotOptions: {
           pie: {
        	   size: 80,
               allowPointSelect: true,
               cursor: 'pointer',
               dataLabels: {
                   enabled: false,
                   format: '<b>{point.name}</b>: {point.percentage:.1f} %',
                   style: {
                       color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                   }
               }
           }
       },
       series: [{
           name: 'Accounts',
           colorByPoint: true,
           data: [ {
               name: 'Taxable',
               y: portfolios[1].getValue()
           }, {
               name: 'Retirement',
               y:  portfolios[2].getValue()
           }]
       }]
	});	
	
	 
	//console.log(portfolios)
	
	Highcharts.chart('taxable-ytd-dividend-chart', {
		 chart: {
              type:'column',
              height:110,
              width:100 ,
              backgroundColor: 'transparent'

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
        	visible:false,
            categories: [
                ''
            ],
            crosshair: true
        },
	     yAxis: [{
	       visible:false,
		   tickInterval: 50,
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
	            pointWidth: 25
	        }
	    },
	     
		series:[{
	        name: yr -1,
	        data: [ portfolios[1].getDividend( moment().subtract(1, 'years') ) ] 
	    },{
	        name: yr,
	        data: [ portfolios[1].getDividend( moment() ) ]
	    }]
	});	
	
	Highcharts.chart('retirement-ytd-dividend-chart', {
		 chart: {
             type:'column',
             height:110,
             width:100 ,
             backgroundColor: 'transparent'

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
    	   visible:false,
           categories: [
               ''
           ],
           crosshair: true
       },
	     yAxis: [{
	       visible:false,
		   tickInterval: 50,
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
	            pointWidth: 25
	        }
	    },
	     
		series:[{
	        name: yr -1,
	        data: [ portfolios[2].getDividend( moment().subtract(1, 'years') )  ]
	    },{
	        name: yr,
	        data: [ portfolios[2].getDividend( moment() )  ] 
	    }]
	});
	
	Highcharts.chart('retirement-portfolio-chart', {
		 chart: {
           type:'pie',
           height:90,
           width:100,
           backgroundColor: 'transparent'
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
       	   console.log(this)
              return '<b>' + this.key + '<br/> $' + $.number(this.y,2) + '</b>';
          }
      },
      plotOptions: {
          pie: {
       	   size: 80,
              allowPointSelect: true,
              cursor: 'pointer',
              dataLabels: {
                  enabled: false,
                  format: '<b>{point.name}</b>: {point.percentage:.1f} %',
                  style: {
                      color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
                  }
              }
          }
      },
      series: [{
          name: 'Accounts',
          colorByPoint: true,
          data: [ {
              name: '401k',
              y: portfolios[2].getValue(2)
          }, {
              name: 'Chris Roth',
              y:  portfolios[2].getValue(3)
          }, {
              name: 'Dao 403b',
              y:  portfolios[2].getValue(4)
          }]
      }]
	});	
	
}