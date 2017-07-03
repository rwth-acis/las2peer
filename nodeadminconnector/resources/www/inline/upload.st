
upload(agentid, error, msg)::=<<
<h4>Service Package Upload</h4>
$if(agentid)$

<form method="POST" action="" enctype="multipart/form-data">
Please select a service package (*.jar) file<br>
<input type="file" name="jarfile"><br>
<input type="submit" name="upload" value="Upload Service Package"><br>
</form>

$if(error)$
<b style="color:#A00">$error$</b>
$endif$

$if(msg)$
<b>$msg$</b>
$endif$

$else$
<b>You have to login, to upload a service package.</b>
$endif$
>>