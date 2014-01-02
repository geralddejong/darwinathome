<#import "spring.ftl" as spring />
<#assign exception = exception>
<#assign stackTrace = stackTrace>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
    <title>Exception</title>
    <script language="javascript">
    </script>
</head>
<body>
<div>
<h1>${exception}</h1>
<!--  The stack trace: -->
<pre>
${stackTrace}
</pre>
</div>
</body>
</html>
