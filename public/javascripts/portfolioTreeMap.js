function loadTreeMap(  ){
   	 
	Highcharts.chart('portfolio-tree-map', {
		chart:{ 
            backgroundColor: '#f2f2f2'
		},		
	    colorAxis: {
	        minColor: '#03d600',
	        maxColor: '#FF0000'
	    },
	    series: [{
	    	opacity: .15,

	        type: 'treemap',
	        layoutAlgorithm: 'squarified',
	        data: [{
	            name: 'ATT',
	            value: 2124.87,
	            colorValue: 7
	        }, {
	            name: 'AAPL',
	            value: 3222.53,
	            colorValue: 0
	        }, {
	            name: 'KO',
	            value: 174.26,
	            colorValue: 2
	        }, {
	            name: 'JNJ',
	            value: 5496.41,
	            colorValue: 7
	        }, {
	            name: 'OGE',
	            value: 4018.59,
	            colorValue: 0
	        }, {
	            name: 'KMB',
	            value: 805.52,
	            colorValue: 1
	        }, {
	            name: 'G',
	            value: 1,
	            colorValue: 7
	        }]
	    }],
	    title: {
	        text: ''
	    },
        credits:{
         	enabled: false
        }
	});


}	   
 		