<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.model.Role" %>
<%@ page import="java.util.Date" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="aui" uri="http://alloy.liferay.com/tld/aui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<portlet:defineObjects />

<%
  Long userId = PortalUtil.getUserId(request);
  boolean isNb = false;

  for(Role r :  UserLocalServiceUtil.getUser(userId).getRoles()) {
    if(r.getName().equals("NationalBankEmployee") || r.getName().equals("Administrator"))
      isNb = false;
  }

  Date date = new Date();
  Long millis = date.getTime();
%>

<portlet:resourceURL var="getDataURL">
</portlet:resourceURL>

<link rel="stylesheet" media="all" href="/static-usci/ext/resources/css/ext-all.css" />
<link rel="stylesheet" media="all" href="<%=request.getContextPath()%>/css/main.css?'<%=millis%>'" />


<script>
    var dataUrl = 'https://essptest.nationalbank.kz/api/usci';
    var contextPathUrl = '<%=request.getContextPath()%>';
    var userId = '<%=userId%>';
    var isNb = '<%=isNb%>' == 'true';
    var reportType = 'BANKS';
</script>

<script src="/static-usci/ext/ext-all.js" type="text/javascript"></script>
<script src="/static-usci/js/FileSaver.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/main.js?'<%=millis%>'" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/variables.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/exportReportToTable.js?'<%=millis%>'" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/exportReport.js?'<%=millis%>'" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/dynamicReport.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/moment.js" type="text/javascript"></script>


<script src="<%=request.getContextPath()%>/<%= (String) request.getSession().getAttribute("lang_js") %>" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/lang/<%= ((ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY)).getLocale() %>.js" type="text/javascript"></script>

<div id="report-content"></div>

