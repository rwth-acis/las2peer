
status(nodeid, cpuload, publicKey, storageSize, maxStorageSize, uptime, otherNodes, localServices)::=<<
node id: $nodeid$<br>
cpu load: $cpuload$<br>
local storage: <meter value="$storageSize$" min="0" max="$maxStorageSize$"></meter> $storageSize$ of $maxStorageSize$ used<br>
uptime: $uptime$<br>
<br>
node public key: $publicKey$<br>

<h3>Known Nodes In Network</h3>
<ul>
$otherNodes :{ node | <li>$node$</li> }$
</ul>

<h3>Local Running Services</h3>
<table width="100%">
<tr><th>Name</th><th width="10%">Version</th></tr>
$localServices :{ service | <tr><td><b style="color: #0A0">$service.name$</b></td><td>$service.version$</td></tr> }$
</table>

>>