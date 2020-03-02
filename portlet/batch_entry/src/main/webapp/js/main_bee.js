Ext.require([
    'Ext.tab.*',
    'Ext.tree.*',
    'Ext.data.*',
    'Ext.tip.*'
]);

Ext.onReady(function() {

    var buttonSend = Ext.create('Ext.button.Button', {
        id: "entityEditorShowBtn",
        text: label_SEND,
        handler : function (){
            var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_WAIT });
            loadMask.show();
            Ext.Ajax.request({
                url: dataUrl+'/receiver/batchEntry/confirmBatchEntries',
                method: 'POST',
                params: {
                    userId: userId,
                    isNb: isNb
                },
                success: function(info) {
                    loadMask.hide();
                    Ext.MessageBox.alert(LABEL_SUCCESS, LABEL_SEND_APPROVAL);
                    store.load();

                },
                failure: function(response) {
                    loadMask.hide();
                    var error = JSON.parse(response.responseText);
                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.message,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    Ext.define('myModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'repDate', 'updateDate']
    });

    var store = Ext.create('Ext.data.Store', {
        model: 'myModel',
        remoteGroup: true,
        buffered: true,
        leadingBufferZone: 300,
        pageSize: 100,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/receiver/batchEntry/getBatchEntriesByUserId',
            method: 'GET',
            extraParams: {userId : userId},
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        autoLoad: true,
        remoteSort: true
    });

    var grid = Ext.create('Ext.grid.Panel', {
        id: "itemsGrid",
        viewConfig: { emptyText: label_NO_DATA },
        store: store,
        anchor: '100% 100%',
        columns: [
            {
                header: '',
                xtype: 'actioncolumn',
                width: 26,
                sortable: false,
                items: [{
                    icon: contextPathUrl + '/pics/edit.png',
                    tooltip: label_VIEW,
                    handler: function (grid, rowIndex, colIndex) {
                        var rec = store.getAt(rowIndex);
                        id_field = rec.get('id');

                        Ext.Ajax.request({
                            url: dataUrl+'/receiver/batchEntry/getBatchEntry',
                            method: 'GET',
                            params: {
                                batchEntryId: id_field
                            },
                            success: function(response) {
                                var obj = JSON.parse(response.responseText);
                                var xmlStr = obj.value;

                                var buttonClose = Ext.create('Ext.button.Button', {
                                    id: "itemFormCancel",
                                    text: label_CLOSE,
                                    handler : function (){
                                        Ext.getCmp('xmlFromWin').destroy();
                                    }
                                });

                                var xmlForm = Ext.create('Ext.form.Panel', {
                                    id: 'xmlForm',
                                    region: 'center',
                                    width: 615,
                                    fieldDefaults: {
                                        msgTarget: 'side'
                                    },
                                    defaults: {
                                        anchor: '100%'
                                    },

                                    bodyPadding: '5 5 0',
                                    items: [{
                                        fieldLabel: 'XML',
                                        name: 'id',
                                        xtype: 'textarea',
                                        value: xmlStr,
                                        height: 615
                                    }],

                                    buttons: [buttonClose]
                                });

                                xmlFromWin = new Ext.Window({
                                    id: "xmlFromWin",
                                    layout: 'fit',
                                    title:'XML',
                                    modal: true,
                                    maximizable: true,
                                    items:[xmlForm]
                                });

                                xmlFromWin.show();
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
                    }}
                ]
            },
            {
                header: '',
                xtype: 'actioncolumn',
                width: 26,
                sortable: false,
                items: [{
                    icon: contextPathUrl + '/pics/delete.png',
                    tooltip: label_DEL,
                    handler: function (grid, rowIndex, colIndex) {
                        var rec = store.getAt(rowIndex);
                        id_field = rec.get('id');

                        Ext.Ajax.request({
                            url: dataUrl+'/receiver/batchEntry/deleteBatchEntry',
                            method: 'POST',
                            params: {
                                batchEntryId: id_field
                            },
                            success: function() {
                                store.load();
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
                    }}
                ]
            },
            {
                text     : label_CODE,
                dataIndex: 'id',
                flex:1
            },
            {
                xtype: 'datecolumn',
                text     : label_REP_DATE,
                dataIndex: 'repDate',
                format: 'd.m.Y',
                flex:1
            },
            {
                xtype: 'datecolumn',
                text     : label_DATE,
                dataIndex: 'updateDate',
                format: 'd.m.Y H:i:s',
                flex:1
            }
        ],
        title: label_NOTES
    });

    grid.getStore().load({
        callback: function (records, operation, success) {
            if (!success) {
                Ext.Msg.show({
                    title: label_ERROR,
                    msg: operation.request.proxy.reader.rawData.errorMessage,
                    width : 300,
                    buttons: Ext.MessageBox.YES
                });
            }
        }
    });

    mainEntityEditorPanel = Ext.create('Ext.panel.Panel', {
        title : label_PANEL,
        preventHeader: true,
        width : '100%',
        height: '500px',
        layout: 'anchor',
        renderTo : 'batch-entry-content',
        //layout : 'border',
        defaults : {
            padding: '3'
        },
        items  : [grid],
        dockedItems: [
            buttonSend
        ]
    });
});
