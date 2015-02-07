<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<style>

body, html {
font-family:Arial,Verdana;
font-size:100%;
height:100%; 
}
input.searchbutton  {
    background: url(searchbutton.png);
    border: 0;
    display: block;
}
div.searchbox_center {
  position: fixed;
  top: 50%;
  left: 50%;
  margin-top: -50px;
  margin-left: -234px;
}
div.searchbox_top {
  position: fixed;
  top: 0px;
  left: 130px;
  margin-top: 2px;
  margin-left: 5px;
}
div.logo_center {
  position: fixed;
  top: 50%;
  left: 50%;
  margin-top: -140px;
  margin-left: -150px;
}
div.logo_top {
  position: fixed;
  top: 0;
  left: 0;
  max-height: 36px;
  height: 36px;
  width: auto;
  margin-top: 4px;
  margin-left: 5px;
}
div.resultDiv {
  position: absolute;
  margin-top: 40px;
  top: 20px;
  left: 110px;
  z-index:0;
}
div.topbox {
	position:fixed;
	left:0;
	top:0;
	background-color:#ffffff;
	height:50px;
	width:100%;
	z-index:1;
}
a
{
	text-decoration:none; 
	color:#6689e2;
}

</style>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>solidsearch.org</title>
        <script src="http://code.jquery.com/jquery-latest.js">   
        </script>
        <script>
        var page = 1;
        var searchRequest;
        
            $(document).ready(function() {                        
                $('#submit').click(function(event) { 
                	document.getElementById("solidsearchlogo").className = "logo_top";
                	document.getElementById("searchbox").className = "searchbox_top";
                    searchRequest=$('#search').val();
                $.get('/searchhead/rest/search',{q:searchRequest,s:page},function(responseText) { 
                        $('#resultDiv').html(responseText);  
                    });
                });

            });
            function nextPage(e){
            	page++;
                $.get('/searchhead/rest/search',{q:searchRequest,s:page},function(responseText) { 
                    $('#resultDiv').html(responseText);         
                });
            }
            function prevPage(e){
            	page--;
                $.get('/searchhead/rest/search',{q:searchRequest,s:page},function(responseText) { 
                    $('#resultDiv').html(responseText);         
                });
            }
        </script>
        
        <script type="text/javascript">
		function tableInputKeyPress(e){
			document.getElementById("solidsearchlogo").className = " logo_top";
		  document.getElementById("searchbox").className = " searchbox_top";
		  e=e||window.event;
		  var key = e.keyCode;
		  if(key==13) //Enter
		  {
			  $("#submit").click();
		     return false; //return true to submit, false to do nothing
		  }
		}
		</script>
</head>
<body OnLoad="document.form1.search.focus();">
	<div class="topbox">
	<div class="logo_center" id="solidsearchlogo" ><a href="http://www.solidsearch.org"><img style="max-width:100%; max-height:100%;" src="solidsearch.png"></a></div>
	<div class="searchbox_center" id="searchbox">
	<form name="form1">
		<table>
		<tr>
		<td>
		<input type="text" id="search" name="search" style="height: 26px; width: 400px; padding: 4px; margin: 0px; text-align: left;" onkeypress="return tableInputKeyPress(event)"/>
		</td>
		<td>
		<input type="button" id="submit" class="searchbutton" style="height: 34px; width: 68px; padding: 0px; margin: 0px; text-align: Center;" value=""/>
		</td>
		</tr>
		</table>
	</form>
	</div>
	</div>
	<div class="resultDiv" id="resultDiv"></div>
</body>
</html>
