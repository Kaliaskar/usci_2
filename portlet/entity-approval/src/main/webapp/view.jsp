<%@ page import="com.liferay.portal.model.Role" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="java.util.Date" %>
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

    Date date = new Date();
    Long millis = date.getTime();
%>

<link rel="stylesheet" media="all" href="/static-usci/ext/resources/css/ext-all.css" />
<link rel="stylesheet" media="all" href="<%=request.getContextPath()%>/css/main.css" />

<script>
    var dataUrl = 'https://nbessp.nationalbank.kz/api/usci';
    var contextPathUrl = '<%=request.getContextPath()%>';
    var userId = '<%=userId%>';
    var isNb = '<%=isNb%>' == 'true';
</script>

<style>
    .node {
        border: 1px solid black;
        margin: 5px;
    }

    .leaf {
        margin: 5px;
    }

    .loading {
        display: none;
        background-image: url("<%=request.getContextPath()%>/pics/loading.gif");
        background-repeat: no-repeat;
        background-position: 0 center;
        text-decoration: none;
        width: 10px;
        cursor: default;
    }

    .not-filled{
        display: none;
        text-decoration: none;
        cursor: default;
        color: red;
        margin-left: 2px;
    }

    .deleted {
        background-image: url("/static-usci/ext/resources/ext-theme-classic/images/tree/drop-no.gif");
    }
</style>

<script src="/static-usci/ext/ext-all.js" type="text/javascript"></script>
<script src="/static-usci/js/FileSaver.js" type="text/javascript"></script>

<script src="<%=request.getContextPath()%>/js/lang/default.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/<%= (String) request.getSession().getAttribute("lang_js") %>" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/moment.js" type="text/javascript"></script>

<script src="<%=request.getContextPath()%>/js/eav.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/entity_editor.js?'<%=millis%>'" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/usci_ext.js" type="text/javascript"></script>


<c:choose>
    <c:when test="${not empty error}">
        ${error}
    </c:when>
    <c:when test="${empty error}">
        <div id="entity-approval-content">
    </c:when>
</c:choose>

</div>