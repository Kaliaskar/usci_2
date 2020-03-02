Ext.define('creditorListModel',{
    extend: 'Ext.data.Model',
    fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType']

});

Ext.define('loadModel',{
    extend: 'Ext.data.Model',
    fields: ['id', 'report', 'portalUserId', 'startTime', 'finishTime', 'note', 'files']
});

Ext.define('reportListModel',{
    extend: 'Ext.data.Model',
    fields: ['id', 'nameRu', 'nameKz', 'name', 'procedureName', 'type', 'orderNumber', 'inputParameters', 'exportTypesList']
});

Ext.define('reportModel',{
    extend: 'Ext.data.Model',
    fields: ['id', 'user', 'respondent', 'product', 'tableName', 'reportDate', 'beginDate', 'endDate', 'status']
});

Ext.define('valueModel',{
    extend: 'Ext.data.Model',
    fields: ['displayName', 'value']
})
