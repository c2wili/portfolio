function buildMonthlyComparison( o, yr ){
 
	var monthlyTotals = [];
	monthlyTotals['cy'] = [0,0,0,0,0,0,0,0,0,0,0,0]
	monthlyTotals['py'] = [0,0,0,0,0,0,0,0,0,0,0,0]	    		
	var monthfull=['january','february','march','april','may','june','july','august','september','october','november','december'];
		
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
 
    
    var ytd = {cy:0, py:0}
    
    var monthcompare = []; 
	for(i=0;i<12;i++){
		var change = 0;
		if(monthlyTotals.py[i] + monthlyTotals.cy[i] == 0)
			change = '---';
		else if(monthlyTotals.py[i] == 0)
			change = 100;
		else 
			change = ((monthlyTotals.cy[i]-monthlyTotals.py[i]) / monthlyTotals.py[i]) * 100;
		
		ytd.cy+=monthlyTotals.cy[i]
		if(i <= moment().month()) ytd.py+=monthlyTotals.py[i];
		
		var o = {mth:monthfull[i], mthnum:i+1, cy:monthlyTotals.cy[i], py:monthlyTotals.py[i],chg:change}
		monthcompare.push(o);
	}
	
	//build full year comparison
	var change = ((ytd.cy-ytd.py) / ytd.py) * 100;
	monthcompare.push({mth:"Year-to-Date", mthnum:999, cy:ytd.cy, py:ytd.py, chg:change});
      
    if($.fn.dataTable.isDataTable("#dividend-monthsummary-table")){
    	var tbl = $('#dividend-monthsummary-table').DataTable();
    	$('#dividend-monthsummary-table').empty();
    	tbl.destroy();
    }	 
	     
    var tbl = $('#dividend-monthsummary-table').DataTable( {
	        aaData: monthcompare
	     	,searching: false
		    ,info:false
			,lengthChange:false
			,ordering: false
		    ,paginate:false
		    //,scroller: true
		    //,scrollY: 400
	        ,columns: [
	        	{ data: "mth" },
	        	{ data: "mthnum", visible:false },
	        	{ data: "py" , className: "dt-right", render: function ( data, type, row ) {return '$'+$.number(data,2)} },
	        	{ data: "cy" , className: "dt-right", render: function ( data, type, row ) {return '$'+$.number(data,2)} },
	        	{ data: "chg", className: "dt-right", render: function ( data, type, row ) {if(data=='---') return data; else return $.number(data,3)+'%'} , createdCell: function(td, cellData, rowData, row, col) { if(cellData == '---'){} else if(cellData <= 0){ $(td).addClass('chg0'); } else if(cellData <5) {$(td).addClass('chg1');} else {$(td).addClass('chg2');} }  }
	        ]
	     	,destroy:true
     });
     
     $( tbl.column( 2 ).header() ).text( yr-1 );
     $( tbl.column( 3 ).header() ).text( yr );
}
