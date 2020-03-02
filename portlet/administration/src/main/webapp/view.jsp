<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.liferay.portal.theme.ThemeDisplay" %>
<%@ page import="com.liferay.portal.kernel.util.WebKeys" %>
<%@ page import="com.liferay.portal.model.Role" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="java.util.*" %>
<%@ page import="org.springframework.boot.configurationprocessor.json.JSONObject" %>
<%@ page import="com.google.gson.JsonObject" %>
<%@ page import="com.google.gson.GsonBuilder" %>
<%@ page import="com.google.gson.Gson" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="aui" uri="http://alloy.liferay.com/tld/aui" %>


<portlet:defineObjects />

<%
    boolean isNb = false;

    for(Role r : UserLocalServiceUtil.getUser(PortalUtil.getUserId(request)).getRoles()) {
        if(r.getName().equals("NationalBankEmployee") || r.getName().equals("Administrator"))
            isNb = true;
    }

    //TODO: временно для определения isNb пользователей
    List<User> users = UserLocalServiceUtil.getUsers(0, UserLocalServiceUtil.getUsersCount());
    Map<Long, List<String>> usersRoleNamesMap = new HashMap();
    for (User user : users) {
        for (Role role : user.getRoles()) {
            if (!usersRoleNamesMap.containsKey(user.getUserId())) {
                usersRoleNamesMap.put(user.getUserId(), new LinkedList());
            }
            usersRoleNamesMap.get(user.getUserId()).add(role.getName());
        }
    }
    GsonBuilder gsonMapBuilder = new GsonBuilder();
    Gson gsonObject = gsonMapBuilder.create();
    String usersRoleNameJSON = gsonObject.toJson(usersRoleNamesMap);

%>


<portlet:resourceURL var="getDataURL">

    <%--<portlet:param name="metaId" value="testClass" />--%>

</portlet:resourceURL>

<link rel="stylesheet" media="all" href="/static-usci/ext/resources/css/ext-all.css" />
<link rel="stylesheet" media="all" href="<%=request.getContextPath()%>/css/main.css" />

<script>
    var dataUrlforUsers = '<%=getDataURL%>';
    var dataUrl = 'https://essptest.nationalbank.kz/api/usci';
    var contextPathUrl = '<%=request.getContextPath()%>';
    var users = '<%=usersRoleNameJSON%>';
</script>

<script src="<%=request.getContextPath()%>/js/require.js" type="text/javascript"></script>
<script src="/static-usci/ext/ext-all.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/main.js" type="text/javascript"></script>

<style type="text/css" media="screen">
    #bkeditor {
        position: relative;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        height: 100%;
    }
</style>


<script src="<%=request.getContextPath()%>/<%= (String) request.getSession().getAttribute("lang_js") %>" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/js/lang/<%= ((ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY)).getLocale() %>.js" type="text/javascript"></script>

<c:choose>
    <c:when test="${not empty error}">
        ${error}
    </c:when>
    <c:when test="${empty error}" >
        <div id="admin-content"></div>
    </c:when>
</c:choose>

