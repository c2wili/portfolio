function buildSectorSummary( o, yr){

    var totalpayout = 0;
    var yr = moment().year();
    var sectorsTmp = [];

    $.each(o, function(index, rec){
		var activity_date = moment(rec.activity_date);
		var activity_year = activity_date.year();
		
		if(activity_year == yr){
			
			var payout = rec.shares*rec.price;
			totalpayout+=payout;
			
			if(sectorsTmp[rec.sector] == null) 
				sectorsTmp[rec.sector] = {sector: rec.sector, amount:payout, pct:0};
			else
				sectorsTmp[rec.sector].amount+=payout;
		}
    });
    
    var sectors = [];
    for(key in sectorsTmp){
    	// determine % of total 
    	sectorsTmp[key].pct = (sectorsTmp[key].amount/totalpayout) * 100;
    	sectors.push(sectorsTmp[key]);
    }
     
    
    if($.fn.dataTable.isDataTable("#dividend-sectorsummary-table")){
    	var tbl = $('#dividend-sectorsummary-table').DataTable();
    	$('#dividend-sectorsummary-table').empty();
    	tbl.destroy();
    }	 

    var tbl = $('#dividend-sectorsummary-table').DataTable( {
	        aaData: sectors
	     	,searching: false
		    ,info:false
			,lengthChange:false
			,ordering: false
		    ,paginate:false
		    //,scroller: true
		    //,scrollY: 400
	        ,columns: [
	        	{ data: "sector" },
	        	{ data: "amount", className: "dt-right", render: function ( data, type, row ) {return '$'+$.number(data,2)} },
	        	{ data: "pct", className: "dt-right", render: function ( data, type, row ) {return $.number(data,3)+'%'}  }
	        ]
	     	,destroy:true
    });	
		     
		     
}