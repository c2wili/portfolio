

function buildSummaryCharts(){
	
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
               return '<b>' + this.x + ' ' + this.series.name + '<br/> $' +
                    + this.y.toFixed(2) + '</b>';
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
           name: 'Brands',
           colorByPoint: true,
           data: [ {
               name: 'QQ',
               y: 1.2
           }, {
               name: 'Other',
               y: 2.61
           }]
       }]
	});	
}