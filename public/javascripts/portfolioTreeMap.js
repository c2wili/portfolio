function loadTreeMap( json ){
	var colors = ["#FF0000","#CC0000","#990000","#660000","#330000","#000000","#003300","#006600","#009900","#00CC00","#00FF00"];
/*11 buckets
figure out the range
divide by 11 to get buckets
*/
var max=0
var min=0;
$.each( json, function( key, o ) {
	max=Math.ceil(Math.max(max,o.gainloss));
	min=Math.floor(Math.min(min,o.gainloss));
});
//var bucket = Math.floor()
if(max <=0) max = 0;
console.log("max: " + Math.ceil(max));
console.log("min: " + Math.floor(min));
console.log(json );
	$.each( json, function( key, o ) {

        o.value = o.shares*o.price;
        o.name = o.name + "<br/>" + o.ticker
        
        //o.colorValue = o.gainloss*-1;
        var sp = -600;
        for(i=0;i<colors.length;i++){
        	o.color = colors[i];
        	if(o.gainloss <= sp){
        		break;
        	}
        	sp+=100;
        }
    });
	
	//return;
	
	Highcharts.chart('portfolio-tree-map', {
		chart:{ 
            backgroundColor: '#f2f2f2'
		},
	    series: [{
	    	opacity: .15,

	        type: 'treemap',
	        layoutAlgorithm: 'squarified',
	        data: json
	    }],
	    title: {
	        text: ''
	    },
        credits:{
         	enabled: false
        }
	});


}	   
 		