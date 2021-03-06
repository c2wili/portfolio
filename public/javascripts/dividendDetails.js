
var currentSharesOwned = [];

function parseDividends( o, yr ){
	
 	var dividends = [];
	var tickers = [];

	var oDividend = [];
	var retire=false;
	for(i=0;i<12;i++){
		oDividend.push({actual:0, projected:0, prioryear:0, paid:false});
	}
	
		
    /* *********************************************************** */
    /* ************* remove in 2019 ****************************** */
    /* *********************************************************** */
    $.each(o, function(index, rec){
    	if(rec.ticker == 'VWENX'){
    		retire = true;
    		return false;
    	}
    });
    if(retire && moment().format("YYYY") == 2018){
    	o.push({ticker: 'VIEIX', activity_date: '2017-01-01', sector:'Mutual Fund',divperiod:3,price:0,shares:0})
    	o.push({ticker: 'VWILX', activity_date: '2017-01-01', sector:'Mutual Fund',divperiod:9,price:0,shares:0})
    	o.push({ticker: 'VWILX', activity_date: '2016-01-01', sector:'Mutual Fund',divperiod:9,price:0,shares:0})    	
    }
    /* *********************************************************** */
    /* *********************************************************** */
    /* *********************************************************** */
    
 	
    $.each(o, function(index, rec){   	

    	var activity_date = moment(rec.activity_date);
		var activity_year = activity_date.year();
		var activity_month = activity_date.month();
		
		var tmpO = {ticker: rec.ticker, details:[], divperiod: rec.divperiod, sector: rec.sector};
		
		tmpO.details[yr-1] = JSON.parse(JSON.stringify(oDividend));
		tmpO.details[yr] = JSON.parse(JSON.stringify(oDividend));
				
		var payout = rec.shares*rec.price;
		
		if(dividends[rec.ticker] == null){
			dividends[rec.ticker] = tmpO;
			tickers.push(rec.ticker);
		}
		else{
			tmpO=dividends[rec.ticker];
	    }

		// set the current year actual dividends paid
		if(tmpO.details[activity_year] == null) tmpO.details[activity_year] = JSON.parse(JSON.stringify(oDividend));	    			
		tmpO.details[activity_year][activity_month].actual+= payout;
		tmpO.details[activity_year][activity_month].paid = true;	
		tmpO.details[activity_year][activity_month].amount = rec.price
		tmpO.details[activity_year][activity_month].shares = rec.shares
		tmpO.current_shares = rec.current_shares;
		
		currentSharesOwned[rec.ticker] = rec.current_shares;
    });
    
    
    console.log("***");
    console.log(dividends)
    return dividends;
}    
/* ***********************    
    // function to build projection for the month
	function buildProjection(list){
		
		var totalNonZero = 0;
		var returnTotal = 0;
		for(i=0;i<list.length;i++){
			if(list[i] > 0){
				returnTotal+= list[i];
				totalNonZero++;
			}
		}
		if(totalNonZero == 0) return 0;
		
		return returnTotal / totalNonZero;	    					
	}
**************/	
    
    
function loadDividendDetails( o, yr ){    
	
	var dividends = parseDividends( o, yr )
	
    for(key in dividends){   	
    	var rec = dividends[key];
   	    	
    	var yearlydetails = rec.details;    	
    	for(year in yearlydetails){
//console.log(rec.ticker + ", " + year)
    		// fill in the prior year values.
    		if(yearlydetails[year-1]){
    			for(i=0;i<12;i++){
    				yearlydetails[year][i].prioryear = yearlydetails[year-1][i].actual
    			}
    		}

        	// while spinning through build the current year projections
    		if(year == yr){ 
    			
    			// if we don't own any shares, don't build future projections
    			if(rec.current_shares == 0) continue;
    			
    			var thisyeardividends = yearlydetails[year]
    			
    			//if we are looking at prior year data, no projections
    			if( yr < moment().format("YYYY") ){
    				for(i=0;i<12;i++){
        				thisyeardividends[i].projected = 0;
        			} 
    			}
    			// if this is january, current year projections are = same period prior year * 3% if available other wise remain 0.
    			else if( moment().month() == 0 ){
        			for(i=0;i<12;i++){
        				thisyeardividends[i].projected = thisyeardividends[i].prioryear*1.03;
        			}    				
    			}
    			// for all other months we have current year actuals we can use for future projections
    			else{
    			 	
    				if(rec.ticker == 'VWILX' && moment().format("YYYY") == 2018){
    					// only vwilx rn revisit first thing in 2019!!!!
    					thisyeardividends[11].projected = 256.840 * .8065;
    				}
    				else if(rec.ticker == 'VIEIX' && moment().format("YYYY") == 2018){
    					// only VIEIX rn revisit first thing in 2019!!!!
    					thisyeardividends[8].projected = 35;
    					thisyeardividends[11].projected = 35;
    				}
    				// for annual dividend payers, current year = prior year same month * 3%
    				else if(rec.divperiod == 9){
    					thisyeardividends[11].projected = thisyeardividends[11].prioryear*1.03;
    				}
    				// for monthly payers, each future month is an average of what has been paid this year    				
    				else if(rec.divperiod == 12){
    					
    					/* **************************************** */
    					/* *********** remove in 2019 ************* */
    					/* **************************************** */
    					if(rec.ticker == 'VFIXED' && moment().format("YYYY") == 2018){
    						var lastActual = 0
    						for(i=0;i<12;i++){        			
        						if(thisyeardividends[i].paid) lastActual = thisyeardividends[i].actual;
        				    }
    						for(i=moment().month();i<12;i++){        				    	
        				    	thisyeardividends[i].projected = lastActual;
        				    }
    						continue;
    					}
    					/* **************************************** */
    					
    					var thisyearaverage = 0;
    					var nonZero = 0;
    					for(i=0;i<12;i++){    						
    						if(thisyeardividends[i].actual>0){    							
    							thisyearaverage+=thisyeardividends[i].actual;
    							nonZero++;
    						}
    					}
    					
    				    for(i=moment().month();i<12;i++){        				    	
    				    	thisyeardividends[i].projected = thisyearaverage/nonZero;
    				    }
					}
    				else{
    					//get last actual payment
    					var lastActual=0;
    					console.log(rec) 		
    					for(i=0;i<12;i++){
    						if(thisyeardividends[i].paid) lastActual = thisyeardividends[i].actual;
    				    }
    					// set projected for all future months
    					for(i=moment().month();i<12;i++){
    						if(thisyeardividends[i].paid) continue;
    						
    						if(rec.divperiod == 1 && $.inArray(i,[0,3,6,9])>0){
    							thisyeardividends[i].projected = lastActual;
    						}
    						else if(rec.divperiod == 2 && $.inArray(i,[1,4,7,10])>0){
    							thisyeardividends[i].projected = lastActual;
    						}
    						else if(rec.divperiod == 3 && $.inArray(i,[2,5,8,11])>0){
    							thisyeardividends[i].projected = lastActual;
    						}
    						else if(rec.divperiod == 6 && $.inArray(i,[5,11])>0){
    							thisyeardividends[i].projected = lastActual;
    						}    						
    					}    					
    				}    				
    			}
    		}
    	} 
    }
    console.log("2*************");
    console.log(dividends);
    
    //data = [{ticker:"AAPL",ytd:41.61,divperiod:2,apr:{actual:99, projected:95, prioryear:75, paid:true},aug:13.87,dec:0,divperiod:2,feb:0,jan:0,jul:0,jun:0,mar:0,may:13.87,nov:13.87,oct:0,q1:0,q2:13.87,q3:13.87,q4:13.87,sep:0,ytd:41.61}]

    var monthNames=['jan','feb','mar','apr','may','jun','jul','aug','sep','oct','nov','dec'];
    var quarters=['q1','q1','q1','q2','q2','q2','q3','q3','q3','q4','q4','q4']
    var data = [];
    
    for(key in dividends){
    	
    	var rec = dividends[key];
    	 
    	var qtd=0;
		var ytd=rec.details[yr].reduce(function(total,amount,index){ return total + amount; });
		var obj = {ticker: rec.ticker, divperiod: rec.divperiod}

		obj.q1 = {actual:0, projected:0, paid:false};
		obj.q2 = {actual:0, projected:0, paid:false};
		obj.q3 = {actual:0, projected:0, paid:false};
		obj.q4 = {actual:0, projected:0, paid:false};
		obj.ytd = {actual:0, projected:0, paid:false};
		
		
		for(i=0;i<12;i++){		
            if(rec.details[yr][i].paid) rec.details[yr][i].projected=0;
			obj[monthNames[i]] = rec.details[yr][i];
			obj[quarters[i]].actual += rec.details[yr][i].actual;
			obj[quarters[i]].projected += rec.details[yr][i].projected;
			
		}
		
		obj.ytd.actual = obj.q1.actual + obj.q2.actual + obj.q3.actual + obj.q4.actual
		obj.ytd.projected = obj.q1.projected +obj.q2.projected + obj.q3.projected + obj.q4.projected
		
		data.push(obj);
		 
    }
    console.log("DATA");
    console.log(data);
    console.log("***");
 
      
 	 
    if($.fn.dataTable.isDataTable("#dividend-details-table")){
    	var tbl = $('#dividend-details-table').DataTable();
    	$('#dividend-details-table').empty();
    	tbl.destroy();
    }
    	 
    function formatCells(td, cellData, rowData, row, col){
 
    	if(( rowData.divperiod == 1 && (col == 2 || col == 6 || col == 10 || col == 14) ) || 
    	   ( rowData.divperiod == 2 && (col == 3 || col == 7 || col == 11 || col == 15) ) ||
    	   ( rowData.divperiod == 3 && (col == 4 || col == 8 || col == 12 || col == 16) ) ||
    	   ( rowData.divperiod == 6 && (col == 8 || col == 16) ) ||
    	   ( rowData.divperiod == 9 && (col == 16) ) ||
    	   ( rowData.divperiod == 12 && col > 1 && col < 17 )
    	   ){
    		if(cellData.paid)
    			if(cellData.actual >= cellData.prioryear)
    				$(td).addClass('divgain');
    			else 
    				$(td).addClass('divloss');
    		else{
    			if(currentSharesOwned[rowData.ticker]>0)
    				$(td).addClass('divexpected');
    		}
    	}
    }
    
    function renderMonths(data, type, row){

    	if(typeof(data) == "number"){
    		if(data == 0)
    			return "";
    		else
    			return '$' + $.number(data,2);
    	}
    	else if(typeof(data) == "object"){

    		if(data.paid){
    			var tip = yr + ": " + '$' + $.number(data.actual,2) +
    			        "\n" + (yr-1) + ": " + '$' + $.number(data.prioryear,2) +
    			        "\n****: " + '$' + $.number(data.actual - data.prioryear,2) ;
    			        
    			return '<span data-toggle="tooltip" title="' + tip + '">' + '$' + $.number(data.actual,2) + '</span>';
    		}
    		else{
    			if(data.actual == 0 && data.projected == 0)
    				return "";
    			else
    				return '$' + $.number(data.actual+data.projected,2);
    			
    		}
    	}
    	else{
    		return data;
    	}
    }
     
 	var dividenddetails = $('#dividend-details-table').DataTable( {
	        
 	        aaData: data
	     	,searching: false
		    ,info:false
			,lengthChange:false
			,ordering: false
		    ,paginate:false
		    //,scroller: true
		    //,scrollY: 400
	        ,columns: [
	        	{ data: "ticker", createdCell: formatCells },
	        	{ data: "divperiod",visible:false },
	        	{ data: "jan", className: "dt-right", render: renderMonths, createdCell: formatCells },
	            { data: "feb", className: "dt-right", render: renderMonths, createdCell: formatCells }, 
	            { data: "mar", className: "dt-right", render: renderMonths, createdCell: formatCells },  
	            { data: "q1", className: "dt-right dt-qtr", render:renderMonths , createdCell: formatCells  },
	            { data: "apr", className: "dt-right", render: renderMonths, createdCell: formatCells },
	            { data: "may", className: "dt-right", render: renderMonths, createdCell: formatCells },
	            { data: "jun", className: "dt-right", render: renderMonths, createdCell: formatCells },
	            { data: "q2", className: "dt-right dt-qtr", render: renderMonths, createdCell: formatCells  },
	            { data: "jul", className: "dt-right", render: renderMonths, createdCell: formatCells  },
	            { data: "aug", className: "dt-right", render: renderMonths, createdCell: formatCells  },
	            { data: "sep", className: "dt-right", render: renderMonths, createdCell: formatCells  },
	            { data: "q3", className: "dt-right dt-qtr", render: renderMonths, createdCell: formatCells  },
	            { data: "oct", className: "dt-right", render: renderMonths, createdCell: formatCells  },
	            { data: "nov", className: "dt-right", render: renderMonths, createdCell: formatCells  },
	            { data: "dec", className: "dt-right", render: renderMonths, createdCell: formatCells  },
	            { data: "q4", className: "dt-right dt-qtr", render: renderMonths, createdCell: formatCells  },
	            { data: "ytd", className: "dt-right", render: renderMonths, createdCell: formatCells  }
	        ]
	     	,destroy:true
	     	,footerCallback: function(row, data, start, end, display) {
	 	        var api = this.api(),
	 	        intVal = function (i) {

	 	        	return typeof i === 'number' ?
		 	                   i : 
		 	               typeof i === 'object' ? (i.actual+i.projected) : 0;
	 	              /*return typeof i === 'string' ?
	 	                   i.replace(/[, Rs]|(\.\d{2})/g,"")* 1 :
	 	                   typeof i === 'number' ?
	 	                   i : 0;
	 	                   */
	 	        },
	 	        
	 	       janTotal = api.column(2).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       febTotal = api.column(3).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       marTotal = api.column(4).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       q1Total  = api.column(5).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       aprTotal = api.column(6).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       mayTotal = api.column(7).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       junTotal = api.column(8).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       q2Total  = api.column(9).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       julTotal = api.column(10).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       augTotal = api.column(11).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       sepTotal = api.column(12).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       q3Total  = api.column(13).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       octTotal = api.column(14).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       novTotal = api.column(15).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       decTotal = api.column(16).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       q4Total  = api.column(17).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);
	 	       ytdTotal = api.column(18).data().reduce(function (a, b) {return intVal(a) + intVal(b);}, 0);

	 	       $( api.column(0).footer() ).html('Totals');
	 	       $( api.column(2).footer() ).html('$' + $.number(janTotal,2));
	 	       $( api.column(3).footer() ).html('$' + $.number(febTotal,2));
	 	       $( api.column(4).footer() ).html('$' + $.number(marTotal,2));
	 	       $( api.column(5).footer() ).html('$' + $.number(q1Total,2));
	 	       
	 	       $( api.column(6).footer() ).html('$' + $.number(aprTotal,2));
	 	       $( api.column(7).footer() ).html('$' + $.number(mayTotal,2));
	 	       $( api.column(8).footer() ).html('$' + $.number(junTotal,2));
	 	       $( api.column(9).footer() ).html('$' + $.number(q2Total,2));
	 	       
	 	       $( api.column(10).footer() ).html('$' + $.number(julTotal,2));
	 	       $( api.column(11).footer() ).html('$' + $.number(augTotal,2));
	 	       $( api.column(12).footer() ).html('$' + $.number(sepTotal,2));
	 	       $( api.column(13).footer() ).html('$' + $.number(q3Total,2));
	 	       
	 	       $( api.column(14).footer() ).html('$' + $.number(octTotal,2));
	 	       $( api.column(15).footer() ).html('$' + $.number(novTotal,2));
	 	       $( api.column(16).footer() ).html('$' + $.number(decTotal,2));			 	       
	 	       $( api.column(17).footer() ).html('$' + $.number(q4Total,2));
	 	       $( api.column(18).footer() ).html('$' + $.number(ytdTotal,2));
	 	    
	 	    }
     });
 	
 	var cyProjection = 0;
 	var pyActual = 0;
 	for(key in dividends){
 		for(i=0;i<12;i++){
 			cyProjection+= dividends[key].details[yr][i].actual + dividends[key].details[yr][i].projected;
 			pyActual+= dividends[key].details[yr-1][i].actual;
 		}
 	}
 
 	
 	return {cy: cyProjection, py: pyActual};
}
