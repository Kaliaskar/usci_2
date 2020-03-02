<%@ page import="com.liferay.portal.model.Role" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="aui" uri="http://alloy.liferay.com/tld/aui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<portlet:defineObjects />

<%
  Long userId = PortalUtil.getUserId(request);

  boolean isNb = false;

  for (Role r : UserLocalServiceUtil.getUser(userId).getRoles()) {
    if (r.getName().equals("NationalBankEmployee") || r.getName().equals("Administrator"))
      isNb = true;
  }
%>

<link rel="stylesheet" media="all" href="/static-usci/ext/resources/css/ext-all.css" />
<link rel="stylesheet" media="all" href="<%=request.getContextPath()%>/css/main.css" />

<script>
    var dataUrl = 'https://essptest.nationalbank.kz/api/usci';
    var contextPathUrl = '<%=request.getContextPath()%>';
    var userId = '<%=userId%>';
    var isNb = '<%=isNb%>' == 'true';
    var crossCheckPortletUrl = 'https://essptest.nationalbank.kz:';
    var isSenchaMode = false;
</script>

<script src="/static-usci/ext/ext-all.js" type="text/javascript"></script>
<script src="/static-usci/js/FileSaver.js" type="text/javascript"></script>

<script src="<%=request.getContextPath()%>/<%= (String) request.getSession().getAttribute("lang_js") %>" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/lang/<%= ((ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY)).getLocale() %>.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/main.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/signature.js" type="text/javascript"></script>

<script type="text/javascript" src="<%=request.getContextPath()%>/js/deployJava.js"></script>
<script type="text/javascript">
    var contextPath = '/static-usci';
    var attributes = {
        codebase: './',
        code: 'kz.gamma.TumarCSP.class',
        archive: contextPath + '/lib/commons-logging.jar,' + contextPath + '/lib/xmlsec-1.3.0.jar,' + contextPath + '/lib/crypto.gammaprov.jar,' + contextPath + '/lib/crypto-common.jar,' + contextPath + '/lib/sign-applet.jar',
        width: 0,
        height: 0,
        vspace: 0,
        hspace: 0,
        name: 'app'
    };
    var parameters = {java_arguments: '-Xmx256m'};
    var version = '1.6';
    deployJava.runApplet(attributes, parameters, version);
</script>

<div id="confirm-content">
</div>