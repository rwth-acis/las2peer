
admin(token,error,result,stopError,stopResult,startService,localServices)::=<<
$if(token)$

Welcome, Admin.

<h3>Start A Service</h3>
<form method="POST" action="">
<input type="hidden" name="token" value="$token$">
<input type="text" name="startService" size="100" value="$startService$">
<input type="submit" value="Start Service">
</form>
<br>
$if(error)$
<b style="color: #C00">$error$</b>
$endif$
$if(result)$
$result$
$endif$

<h3>Stop Local Services</h3>

<table width="100%">
<tr><th>Name</th><th width="10%">Version</th></tr>
$localServices :{ service | <tr><td><b style="color: #0A0">$service.name$</b></td><td>$service.version$</td><td><form method="POST" action=""><input type="hidden" name="token" value="$token$"><input type="hidden" name="stopService" value="$service.name$@$service.version$"><input type="submit" value="Stop Service"></form></td></tr> }$
</table>
$if(stopError)$
<b style="color: #C00">$error$</b>
$endif$
$if(stopResult)$
$result$
$endif$

$else$

Enter token:
<form method="POST" action="">
<input type="password" name="token" size="100">
<input type="submit" value="Login">
</form>

$endif$
>>