package kz.bsbnb.usci.portlet.entity.editor;

import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class EntityEditorPortlet extends MVCPortlet {

    @Override
    public void doView(RenderRequest renderRequest, RenderResponse renderResponse)
            throws IOException, PortletException {
        HttpServletRequest httpReq = PortalUtil.getOriginalServletRequest(
                PortalUtil.getHttpServletRequest(renderRequest));

        String reqEntityId = httpReq.getParameter("entityId");

        if (reqEntityId != null && reqEntityId.length() > 0) {
            Long queryEntityId = Long.parseLong(reqEntityId);
            String queryRepDate = httpReq.getParameter("repDate");
            Long queryRespondentId = Long.parseLong(httpReq.getParameter("respondentId"));
            Long queryMetaClassId = Long.parseLong(httpReq.getParameter("metaClassId"));

            renderRequest.setAttribute("queryEntityId", queryEntityId);
            renderRequest.setAttribute("queryRepDate", queryRepDate);
            renderRequest.setAttribute("queryRespondentId", queryRespondentId);
            renderRequest.setAttribute("queryMetaClassId", queryMetaClassId);
        }

        super.doView(renderRequest, renderResponse);
    }

}
