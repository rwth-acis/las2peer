
index(title, version, content, sessionid, agentid)::=<<
<!DOCTYPE html>
<head>
    <title>$title$</title>
    <style>
body{
    position:relative;
    background: url(/www/las2peer-logo.svg) no-repeat center center fixed;
    -webkit-background-size: contain;
    -moz-background-size: contain;
    -o-background-size: contain;
    background-size: contain;
    width:100%;
    height:100%;
    margin:0
}
body:after{
    position:fixed;
    content:"";
    top:0;
    left:0;
    right:0;
    bottom:0;
    background:rgba(255,255,255,0.75);
    z-index:-1;
}
    </style>
</head>
<body>
    <h1>$title$</h1>
    <h5>Version: $version$</h5>
    <table border="1" style="width: 100%; table-layout: fixed">
        <tr>
            <td style="vertical-align: top">
                <ul>
                    <li><a href="/www/status">Show Node Status</a></li>
                    <li><a href="/www/services">Service Version Lookup</a></li>
$if(sessionid)$
                    <li><a href="/www/logout?sessionid=$sessionid$">Logout</a></li>
$else$
                    <li><a href="/www/login">Login/Registration</a></li>
$endif$
                    <li><a href="/www/upload">Upload Service Jar Package</a></li>
                    <li><a href="/www/admin">Node Administration</a></li>
                </ul>
            </td>
            <td style="width: 70%; word-wrap: break-word; vertical-align: top">
                $content$
            </td>
        </tr>
    </table>
</body>
</html>
>>