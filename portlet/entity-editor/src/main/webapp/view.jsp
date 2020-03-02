<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portal.model.Role" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="aui" uri="http://alloy.liferay.com/tld/aui" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<portlet:defineObjects />

<%
    Long userId = PortalUtil.getUserId(request);

    boolean isDataManager = false;

    for (Role r : UserLocalServiceUtil.getUser(userId).getRoles()) {
        if (r.getName().equals("DictDataManager"))
            isDataManager = true;
    }

    ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
    String portletId = themeDisplay.getPortletDisplay().getPortletName();

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
    var portletId = '<%=portletId%>';
    var isDataManager = '<%=isDataManager%>' == 'true';

    var queryEntityId = '<%=renderRequest.getAttribute("queryEntityId")%>';
    var queryRepDate = '<%=renderRequest.getAttribute("queryRepDate")%>';
    var queryRespondentId = '<%=renderRequest.getAttribute("queryRespondentId")%>';
    var queryMetaClassId = '<%=renderRequest.getAttribute("queryMetaClassId")%>';
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

<script src="<%=request.getContextPath()%>/<%= (String) request.getSession().getAttribute("lang_js") %>" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/lang/<%= ((ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY)).getLocale() %>.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/moment.js" type="text/javascript"></script>

<% if (portletId.equals("entityeditorportlet")) { %>
    <script src="<%=request.getContextPath()%>/js/entity_editor.js" type="text/javascript"></script>
<% }  else { %>
    <script src="<%=request.getContextPath()%>/js/dict_editor.js" type="text/javascript"></script>
<% } %>

<script src="<%=request.getContextPath()%>/js/datetimepicker.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/entity.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/eav.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/search.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/usci_ext.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/utils.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/variables.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/xml.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/dictionary.js" type="text/javascript"></script>


<c:choose>
    <c:when test="${not empty error}">
        ${error}
    </c:when>
    <c:when test="${empty error}">
        <div id="entity-editor-content">
    </c:when>
</c:choose>

</div>