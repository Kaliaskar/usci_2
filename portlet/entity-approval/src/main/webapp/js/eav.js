/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

function createMetaModels() {

    Ext.define('attributeModel', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'title', type: 'string'},
            {name: 'name', type: 'string'},
            {name: 'classId', type: 'number'},
            {name: 'simple', type: 'boolean'},
            {name: 'array', type: 'boolean'},
            {name: 'dictionary', type: 'boolean'},
            {name: 'typeCode', type: 'string'},
            {name: 'metaType', type: 'string'},
            {name: 'key', type: 'boolean'},
            {name: 'required', type: 'boolean'},
            {name: 'refClassId', type: 'number'},
            {name: 'refMetaType', type: 'string'},
            {name: 'value', type: 'auto'}
        ]
    });

    Ext.define('approvalEntityModel', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'entityId', type: 'number'},
            {name: 'entityText', type: 'string'},
            {name: 'entityKey', type: 'string'},
            {name: 'preApproved', type: 'boolean'},
            {name: 'preDeclined', type: 'boolean'},
            {name: 'metaClassId', type: 'number'},
            {name: 'reportDate', type: 'date', format: 'd.m.Y'},
            {name: 'respondentId', type: 'number'},
            {name: 'respondent', type: 'string'},
            {name: 'operType'}
        ]
    });

    Ext.define('entityModel', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'title', type: 'string'},
            {name: 'name', type: 'string'},
            {name: 'classId', type: 'number'},
            {name: 'simple', type: 'boolean'},
            {name: 'array', type: 'boolean'},
            {name: 'dictionary', type: 'boolean'},
            {name: 'typeCode', type: 'string'},
            {name: 'metaType', type: 'string'},
            {name: 'key', type: 'boolean'},
            {name: 'required', type: 'boolean'},
            {name: 'cumulative', type: 'boolean'},
            {name: 'refClassId', type: 'number'},
            {name: 'refType', type: 'string'},
            {name: 'openDate', type: 'date', format: 'd.m.Y'},
            {name: 'closeDate', type: 'date', format: 'd.m.Y'},
            {name: 'value', type: 'auto'}
        ]
    });
}

function loadAttributes(metaClassId, callback) {
    Ext.Ajax.request({
        url: dataUrl + '/core/meta/getMetaClassAttributesList',
        params : {
            metaClassId: metaClassId
        },
        method: 'GET',
        success: function(result) {
            var attributes = JSON.parse(result.responseText);
            callback(attributes);
        }
    });
}