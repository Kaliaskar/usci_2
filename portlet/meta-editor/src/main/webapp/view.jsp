<%@ page import="com.liferay.portal.model.Role" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="aui" uri="http://alloy.liferay.com/tld/aui" %>

<portlet:defineObjects />

<portlet:resourceURL var="getDataURL">
</portlet:resourceURL>

<%
    Long userId = PortalUtil.getUserId(request);
    boolean isDataManager = false;

    for (Role r : UserLocalServiceUtil.getUser(userId).getRoles()) {
        if (r.getName().equals("DataManager"))
            isDataManager = true;
    }
%>

<link rel="stylesheet" media="all" href="/static-usci/ext/resources/css/ext-all.css" />
<link rel="stylesheet" media="all" href="<%=request.getContextPath()%>/css/main.css" />

<script>
    var dataUrl = 'https://essptest.nationalbank.kz/api/usci';
    var contextPathUrl = '<%=request.getContextPath()%>';
    var isDataManager = '<%=isDataManager%>' == 'true';
</script>

<script src="/static-usci/ext/ext-all.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/st_format.js" type="text/javascript"></script>


<script src="<%=request.getContextPath()%>/<%= (String) request.getSession().getAttribute("lang_js") %>" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/lang/<%= ((ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY)).getLocale() %>.js" type="text/javascript"></script>


<script src="<%=request.getContextPath()%>/js/main.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/meta_class.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/meta_attr.js" type="text/javascript"></script>

<div id="meta-editor-content">

</div>