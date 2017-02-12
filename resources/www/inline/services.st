
services(services, searchname)::=<<
<h3>Service Version Lookup</h3>
Enter service class name to search versions for:
<form method="GET" action="">
<input type="text" name="searchname" size="100" value="$searchname$">
<input type="submit" value="search">
</form>
<h3>Known Versions</h3>
<table width="100%">
<tr><th>Name</th><th width="25%">Version</th></tr>
$services :{ service | <tr><td>$service.name$</td><td>$service.version$</td></tr> }$
</table>
>>