Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*',
    'Ext.button.*',
    'Ext.toolbar.*',
    'Ext.container.*'
]);


Ext.onReady(function(){

    Ext.Date.patterns={
        CustomFormat: "d.m.Y"
    };

    function getFileName(value) {
        var startIndex = (value.indexOf('\\') >= 0 ? value.lastIndexOf('\\') : value.lastIndexOf('/'));
        var filename = value.substring(startIndex);
        if (filename.indexOf('\\') === 0 || filename.indexOf('/') === 0) {
            filename = filename.substring(1);
        }

        return filename;
    }

    var maintenanceRespondentsStore = Ext.create('Ext.data.Store', {
        id: 'maintenanceRespondentsStore',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getUserRespondentList',
            extraParams: {userId: userId},
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        sorters: [{
            property: 'name',
            direction: 'asc'
        }]
    });

    var maintenanceBatchStore = Ext.create('Ext.data.Store', {
        id: 'maintenanceBatchStore',
        fields: ['id', 'reportDate', 'receiverDate', 'processBeginDate', 'processEndDate', 'fileName', 'statusId', 'status', 'respondent', 'respondentId', 'productId', 'product', 'totalEntityCount', 'actualEntityCount', 'successEntityCount', 'errorEntityCount', 'check'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/receiver/batch/getMaintenanceBatchList',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
    });

    var panel = Ext.create('Ext.panel.Panel', {
        height: 900,
        width: 1200,
        renderTo: 'approval-content',
        id: 'MainPanel',
        items: [{
            xtype: 'panel',
            title: label_APPROVAL,
            items: [{
                xtype: 'panel',
                height: 900,
                margin: 0,
                width: 1200,
                title: '',
                titleCollapse: false,
                items: [{
                    xtype: 'displayfield',
                    padding: 3,
                    style: 'font-size: 20px;',
                    labelCls: 'biggertext',
                    value: label_ORGS,
                    fieldCls: 'biggertext'
                }, {
                    xtype: 'textfield',
                    padding: 3,
                    width: 1190,
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("respondentsGrid");
                            if (newValue == '') {
                                grid.store.clearFilter();
                                grid.getView().refresh();
                            } else {
                                grid.store.filter([{
                                    property: "name",
                                    value: newValue,
                                    anyMatch: true,
                                    caseSensitive: false
                                }]);
                            }
                        }
                    }
                }, {
                    xtype: 'gridpanel',
                    store: maintenanceRespondentsStore,
                    multiSelect: true,
                    height: 244,
                    width: 1195,
                    padding: 5,
                    autoScroll: true,
                    title: '',
                    hideHeaders: true,
                    id: 'respondentsGrid',
                    scroll: 'vertical',
                    columns: [{
                        xtype: 'gridcolumn',
                        width: '100%',
                        dataIndex: 'name',
                        text: ''
                    }],
                    viewConfig: {
                        autoScroll: false
                    }
                }, {
                    xtype: 'button',
                    margin: 5,
                    padding: 3,
                    text: label_SELECT_ALL,
                    listeners: {
                        click: function () {
                            Ext.getCmp('respondentsGrid').getSelectionModel().selectAll();
                        }
                    }
                }, {
                    xtype: 'datefield',
                    id: 'reportDate',
                    padding: 5,
                    fieldLabel: label_REP_DATE,
                    labelAlign: 'top',
                    labelStyle: 'font-weight: bold;',
                    format: 'd.m.Y'
                }, {
                    xtype: 'button',
                    margin: '0 5 0 5',
                    text: label_DOWN_Q,
                    listeners: {
                        click: function () {
                            grid = Ext.getCmp("respondentsGrid");
                            gridBatch = Ext.getCmp("maintenanceBatchGrid");
                            var credIds = [];
                            for (var i = 0; i < grid.store.getCount(); i++) {
                                if (grid.getSelectionModel().isSelected(i)) {
                                    credIds.push(grid.store.getAt(i).data.id);
                                }

                            }
                            gridBatch.store.load({
                                params: {
                                    respondentIds: credIds,
                                    reportDate: Ext.Date.format(Ext.getCmp('reportDate').value, Ext.Date.patterns.CustomFormat),
                                    userId: userId
                                },
                                scope: this
                            });
                            gridBatch.getView().refresh();
                        }
                    }
                }, {
                    xtype: 'gridpanel',
                    id: 'maintenanceBatchGrid',
                    store: maintenanceBatchStore,
                    height: 400,
                    width: 1190,
                    margin: '5 0 15 5',
                    autoScroll: true,
                    title: '',
                    columns: [{
                        xtype: 'checkcolumn',
                        dataIndex : 'check',
                        width: 60,
                        text: ''
                    }, {
                        xtype: 'gridcolumn',
                        width: 220,
                        dataIndex: 'respondent',
                        renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                            return value.name;
                        },
                        text: label_ORG_NAME
                    }, {
                        xtype: 'gridcolumn',
                        width: 220,
                        dataIndex: 'fileName',
                        text: label_FILE_NAME,
                        renderer  : function(value, obj, record) {
                            var filename = getFileName(value);
                            return '<a href="#">'+filename+'</a>';
                        }
                    }, {
                        xtype: 'datecolumn',
                        width: 130,
                        dataIndex: 'receiverDate',
                        text: label_REC_DATE,
                        format: 'd.m.Y H:i:s'
                    }, {
                        xtype: 'datecolumn',
                        width: 136,
                        dataIndex: 'processBeginDate',
                        text: label_BEG_DATE,
                        format: 'd.m.Y H:i:s'
                    }, {
                        xtype: 'datecolumn',
                        width: 130,
                        dataIndex: 'processEndDate',
                        text: label_END_DATE,
                        format: 'd.m.Y H:i:s'
                    }, {
                        xtype: 'gridcolumn',
                        width: 95,
                        dataIndex: 'status',
                        renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                            return value.nameRu;
                        },
                        text: label_STATUS
                    }, {
                        xtype: 'datecolumn',
                        width: 100,
                        dataIndex: 'reportDate',
                        text: label_DATE_REP,
                        format: 'd.m.Y'
                    }],
                    dockedItems: [{
                        xtype: 'toolbar',
                        dock: 'bottom',
                        items: [{
                            xtype: 'button',
                            disabled: isDataManager,
                            text: label_SEND,
                            listeners: {
                                click: function () {
                                    gridBatch = Ext.getCmp("maintenanceBatchGrid");
                                    var batchIds = [];
                                    var records = [];
                                    for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                        if (gridBatch.store.getAt(i).data.check === true) {
                                            batchIds.push(gridBatch.store.getAt(i).data.id);
                                            records.push(gridBatch.store.getAt(i));
                                        }
                                    }

                                    Ext.Ajax.request({
                                        url: dataUrl+'/receiver/batch/approveAndSendMaintenance',
                                        method: 'POST',
                                        params: {
                                            batchIds: batchIds
                                        },
                                        reader: {
                                            type: 'json',
                                            root: 'data'
                                        },
                                        success: function() {
                                            Ext.Msg.alert(label_CONFIRMED);
                                            gridBatch.store.remove(records);
                                        },
                                        failure: function(response) {
                                            var error = JSON.parse(response.responseText);

                                            Ext.Msg.show({
                                                title: label_ERROR,
                                                msg: error.message,
                                                width : 300,
                                                buttons: Ext.MessageBox.YES
                                            });
                                        }
                                    });
                                    gridBatch.getView().refresh();
                                }
                            }
                        }, {
                            xtype: 'tbseparator'
                        }, {
                            xtype: 'button',
                            disabled: isDataManager,
                            text: label_CANCAL,
                            listeners: {
                                click: function () {
                                    gridBatch = Ext.getCmp("maintenanceBatchGrid");
                                    var batchIds = [];
                                    var records = [];
                                    for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                        if (gridBatch.store.getAt(i).data.check === true) {
                                            batchIds.push(gridBatch.store.getAt(i).data.id);
                                            records.push(gridBatch.store.getAt(i));
                                        }
                                    }

                                    Ext.Ajax.request({
                                        url: dataUrl+'/receiver/batch/declineAndSendMaintenance',
                                        method: 'POST',
                                        params: {
                                            batchIds: batchIds
                                        },
                                        reader: {
                                            type: 'json',
                                            root: 'data'
                                        },
                                        success: function() {
                                            Ext.Msg.alert(label_CANCELED);
                                            gridBatch.store.remove(records);
                                        },
                                        failure: function(response) {
                                            var error = JSON.parse(response.responseText);

                                            Ext.Msg.show({
                                                title: label_ERROR,
                                                msg: error.message,
                                                width : 300,
                                                buttons: Ext.MessageBox.YES
                                            });
                                        }
                                    });
                                    gridBatch.getView().refresh();
                                }
                            }
                        }]
                    }],
                    viewConfig:{
                        markDirty:false
                    }
                }]
            }]
        }]
    });
});