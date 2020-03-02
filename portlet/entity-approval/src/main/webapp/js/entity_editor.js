/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

Ext.require([
    'Ext.tab.*',
    'Ext.tree.*',
    'Ext.data.*',
    'Ext.tip.*'
]);

Ext.onReady(function() {
    Ext.override(Ext.data.proxy.Ajax, {timeout: 300000});

    // доп работы по ExtJS
    createExtJsComps();

    // создаем модели мета данных
    createMetaModels();

    var operationTypes = [
        [1, 'Все операций'],
        [2, 'INSERT'],
        [3, 'UPDATE'],
        [4, 'DELETE'],
        [5, 'CLOSE'],
        [6, 'OPEN']
    ];

    var respondentId;
    var productId;

    function getFileName(value) {
        var startIndex = (value.indexOf('\\') >= 0 ? value.lastIndexOf('\\') : value.lastIndexOf('/'));
        var filename = value.substring(startIndex);
        if (filename.indexOf('\\') === 0 || filename.indexOf('/') === 0) {
            filename = filename.substring(1);
        }

        return filename;
    }

    var productStore = Ext.create('Ext.data.Store', {
        fields: ['id', 'code', 'name'],
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getProductList',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        autoLoad: true,
        listeners: {
            load: function(me, records, options) {
                productId = records[0].get('id');

                Ext.getCmp('edProduct').setValue(productId);
            }
        }
    });

    var respondentStore = Ext.create('Ext.data.Store', {
        id: 'respondentStore',
        fields: ['id', 'name'],
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getApproveRespondentJsonList',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        },
        autoLoad: true,
        listeners: {
            load: function(me, records, options) {
                respondentId = records[0].get('id');
                Ext.getCmp('edRespondent').setValue(respondentId);
            }
        }
    });

    var batchStore = Ext.create('Ext.data.Store', {
        id: 'batchStore',
        fields: ['id', 'fileName', 'respondent'],
        proxy: {
            type: 'ajax',
            url: dataUrl + '/receiver/batch/getBatchListToApprove',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        },
        autoLoad: false,
        listeners: {
            load: function(me, records, options) {
                if (records.length > 0) {
                    me.suspendEvents();
                    me.each(function(rec){
                        rec.set('fileName', getFileName(rec.get('fileName')))
                    });
                    me.resumeEvents();

                    var batchId = records[0].get('id');
                    Ext.getCmp('edBatch').setValue(batchId);
                } else {
                    Ext.getCmp('edBatch').setValue("");
                }
            }
        }
    });

    var entityStore = Ext.create('Ext.data.TreeStore', {
        model: 'entityModel',
        storeId: 'entityStore',
        folderSort: true,
        proxy: {
            type: 'memory'
        }
    });

    var approvalEntityStore = Ext.create('Ext.data.Store', {
        model: 'approvalEntityModel',
        id: 'approvalEntityStore',
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/eav/getEntityListMaintenance',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        listeners: {
            load: function(me, records, options) {
                comboValue = Ext.getCmp("edBatch").getValue();
                if (comboValue != "") {
                    /*index = batchStore.findExact('id',comboValue);
                    var respondent = batchStore.getAt(index).data.respondent;
                    approvalEntityStore.each(function(rec,index){
                        approvalEntityStore.getAt(index).data.respondent = respondent.name;
                        approvalEntityStore.getAt(index).commit();
                    });*/
                }
            }
        }
    });

    var approvalEntityGrid = Ext.create('Ext.grid.Panel', {
        id: "approvalEntityGrid",
        store: approvalEntityStore,
        columns: [
            {
                xtype: 'datecolumn',
                text : 'Отчетная дата',
                dataIndex: 'reportDate',
                format: 'd.m.Y',
                flex: 1
            },
            {
                text : 'Наименование',
                dataIndex: 'entityText',
                flex: 1
            },
            {
                text : 'Ключевые поля',
                dataIndex: 'entityKey',
                flex: 1
            },
            {
                xtype: 'checkcolumn',
                text : 'Одобрить',
                dataIndex: 'preApproved',
                flex: 1,
                listeners : {
                    checkchange : function(column, rowIndex, isChecked, record, e){
                         if (isChecked && approvalEntityStore.getAt(rowIndex).data.preDeclined == true) {
                            Ext.Msg.show({
                                title: 'Ошибка',
                                msg: 'Нельзя выбирать два поля \"Одобрить\" и \"Отклонить\" одновременно!',
                                width : 300,
                                buttons: Ext.MessageBox.YES
                            });
                            approvalEntityStore.getAt(rowIndex).data.preApproved = false;
                            approvalEntityStore.getAt(rowIndex).commit();
                         } else {
                            var baseEntityJson = {
                                 id: approvalEntityStore.getAt(rowIndex).data.id,
                                 preDeclined: approvalEntityStore.getAt(rowIndex).data.preDeclined,
                                 preApproved: approvalEntityStore.getAt(rowIndex).data.preApproved
                            };
                            Ext.Ajax.request({
                                url: dataUrl + '/core/eav/updateBaseEntityState',
                                method: 'POST',
                                jsonData: baseEntityJson,
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function(response) {
                                },
                                failure: function(response) {

                                    var error = JSON.parse(response.responseText);

                                    Ext.Msg.show({
                                        title: 'Ошибка',
                                        msg: error.errorMessage,
                                        width : 300,
                                        buttons: Ext.MessageBox.YES
                                    });
                                }
                            });
                         }
                     }
                }
            },
            {
                xtype: 'checkcolumn',
                text : 'Отклонить',
                dataIndex: 'preDeclined',
                flex: 1,
                listeners : {
                    checkchange : function(column, rowIndex, isChecked, record, e){
                         if (isChecked && approvalEntityStore.getAt(rowIndex).data.preApproved == true) {
                            Ext.Msg.show({
                                title: 'Ошибка',
                                msg: 'Нельзя выбирать два поля \"Одобрить\" и \"Отклонить\" одновременно!',
                                width : 300,
                                buttons: Ext.MessageBox.YES
                            });
                            approvalEntityStore.getAt(rowIndex).data.preDeclined = false;
                            approvalEntityStore.getAt(rowIndex).commit();
                         } else {
                            var baseEntityJson = {
                                 id: approvalEntityStore.getAt(rowIndex).data.id,
                                 preDeclined: approvalEntityStore.getAt(rowIndex).data.preDeclined,
                                 preApproved: approvalEntityStore.getAt(rowIndex).data.preApproved
                            };
                            Ext.Ajax.request({
                                url: dataUrl + '/core/eav/updateBaseEntityState',
                                method: 'POST',
                                jsonData: baseEntityJson,
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function(response) {
                                },
                                failure: function(response) {

                                    var error = JSON.parse(response.responseText);

                                    Ext.Msg.show({
                                        title: 'Ошибка',
                                        msg: error.errorMessage,
                                        width : 300,
                                        buttons: Ext.MessageBox.YES
                                    });
                                }
                            });
                         }
                    }
                }
            }
        ],
        viewConfig: {
            emptyText: 'Нет данных',
            markDirty:false
        },
        listeners : {
            cellclick: function(grid, td, cellIndex, record, tr, rowIndex, e, eOpts) {
                if (cellIndex > 0) {
                    var loadMask = new Ext.LoadMask(Ext.getCmp('mainPanel'), {msg: label_LOADING});
                    loadMask.show();

                    Ext.Ajax.request({
                        url: dataUrl + '/core/eav/getEntityDataMaintenance',
                        method: 'GET',
                        params: {
                            id: record.get('id')
                        },
                        success: function(response) {
                            loadMask.hide();

                            var jsonData = JSON.parse(response.responseText);
                            entityStore.setRootNode(jsonData);

                            Ext.getCmp('entityTreeView').getView().refresh();
                        },
                        failure: function(response) {
                            loadMask.hide();

                            var error = JSON.parse(response.responseText);

                            Ext.Msg.show({
                                title: 'Ошибка',
                                msg: error.errorMessage,
                                width : 300,
                                buttons: Ext.MessageBox.YES
                            });
                        }
                    });
                }
            }
        },
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            items: [
                {
                    text: 'Обработать',
                    handler: function() {
                        var jsonList = [];
                        var edBatch = Ext.getCmp("edBatch");
                        var approvalEntityGridStore = Ext.getCmp('approvalEntityGrid').getStore();
                        approvalEntityGridStore.suspendEvents();
                        approvalEntityGridStore.each(function(rec){
                            if (rec.data.preApproved == true || rec.data.preDeclined == true)
                                jsonList.push(rec.data);
                        });
                        approvalEntityGridStore.resumeEvents();

                        var loadMask = new Ext.LoadMask(Ext.getCmp('mainPanel'), {msg: label_LOADING});
                        loadMask.show();

                        Ext.Ajax.request({
                            url: dataUrl + '/core/eav/approveEntityMaintenance',
                            method: 'POST',
                            params: {
                                batchId: edBatch.getValue()
                            },
                            jsonData: jsonList,
                            success: function(response) {
                                loadMask.hide();

                                Ext.Msg.show({
                                    title: '',
                                    msg: 'Обработано успешно',
                                    width : 300,
                                    buttons: Ext.MessageBox.YES
                                });


                                batchStore.load({
                                    params: {
                                        productId: Ext.getCmp('edProduct').getValue()
                                    }
                                });

                                Ext.getCmp('approvalEntityGrid').getView().refresh();
                            },
                            failure: function(response) {
                                loadMask.hide();

                                var error = JSON.parse(response.responseText);

                                Ext.Msg.show({
                                    title: 'Ошибка',
                                    msg: error.errorMessage,
                                    width : 300,
                                    buttons: Ext.MessageBox.YES
                                });
                            }
                        });
                        Ext.getCmp('approvalEntityGrid').getView().refresh();
                    }
                }, {
                    xtype: 'tbseparator',
                }, {
                    text: 'Выделить все',
                    handler: function() {
                        var approvalEntityGridStore = Ext.getCmp('approvalEntityGrid').getStore();
                        approvalEntityGridStore.suspendEvents();
                        approvalEntityGridStore.each(function(rec){ rec.set('preApproved', true) })
                        approvalEntityGridStore.resumeEvents();
                        Ext.getCmp('approvalEntityGrid').getView().refresh();
                    }
                }, {
                    xtype: 'tbseparator',
                }, {
                    text: 'Снять выделение',
                    handler: function() {
                        var approvalEntityGridStore = Ext.getCmp('approvalEntityGrid').getStore();
                        approvalEntityGridStore.suspendEvents();
                        approvalEntityGridStore.each(function(rec){ rec.set('preApproved', false) })
                        approvalEntityGridStore.resumeEvents();
                        Ext.getCmp('approvalEntityGrid').getView().refresh();
                    }
                }, {
                    xtype: 'tbseparator',
                }, {
                    id: 'edOperTypes',
                    xtype: 'combobox',
                    displayField: 'operType',
                    store: new Ext.data.SimpleStore({
                        id: 0,
                        fields: ['id', 'operType'],
                        data: operationTypes
                    }),
                    labelWidth: 70,
                    valueField: 'operType',
                    emptyText: 'Выберите операций...',
                    fieldLabel: '',
                    editable: false,
                    width: '30%',
                    queryMode: 'local',
                    listeners: {
                        change: function (combo, value) {
                            grid = Ext.getCmp("approvalEntityGrid");
                            if (value == 'Все операций') {
                                grid.store.clearFilter();
                                grid.getView().refresh();
                            } else {
                                grid.store.clearFilter();
                                grid.store.filter([{
                                    property: "operType",
                                    value: value,
                                    anyMatch: true,
                                    caseSensitive: false
                                }]);
                            }
                        }
                    }
                }]
        }]
    });

    var entityGrid = Ext.create('Ext.tree.Panel', {
        id: 'entityTreeView',
        preventHeader: true,
        useArrows: true,
        rootVisible: false,
        store: entityStore,
        multiSelect: true,
        singleExpand: true,
        columns: [{
            xtype: 'treecolumn',
            text: label_TITLE,
            flex: 4,
            sortable: true,
            dataIndex: 'title'
        }, {
            text: label_CODE,
            flex: 4,
            sortable: true,
            dataIndex: 'name'
        }, {
            text: label_VALUE,
            flex: 2,
            dataIndex: 'value',
            sortable: true
        }, {
            text: label_OPEN_DATE,
            flex: 5,
            xtype: 'datecolumn',
            format: 'd.m.Y',
            dataIndex: 'openDate',
            sortable: true
        }, {
            text: label_CLOSE_DATE,
            flex: 6,
            xtype: 'datecolumn',
            format: 'd.m.Y',
            dataIndex: 'closeDate',
            sortable: true
        }]
    });

    var mainPanel = Ext.create('Ext.panel.Panel', {
        height: 500,
        renderTo: 'entity-approval-content',
        title: '&nbsp',
        id: 'mainPanel',
        layout: 'border',
        items: [{
            region: 'west',
            width: '35%',
            split: true,
            layout: 'border',
            items: [{
                region: 'north',
                height: '30%',
                split: true,
                layout: {
                    type: 'vbox',
                    padding: 5,
                    align: 'stretch'
                },
                items: [
                    {
                        fieldLabel: 'Продукт',
                        id: 'edProduct',
                        xtype: 'combobox',
                        store: productStore,
                        displayField: 'name',
                        width: '60%',
                        labelWidth: '40%',
                        valueField: 'id',
                        editable: false,
                        listeners: {
                            change: function () {
                                batchStore.load({
                                    params: {
                                        productId: Ext.getCmp('edProduct').getValue(),
                                        respondentId: Ext.getCmp('edRespondent').getValue()
                                    }
                                });
                            }
                        }
                    },
                    {
                        fieldLabel: 'Респондент',
                        id: 'edRespondent',
                        xtype: 'combobox',
                        store: respondentStore,
                        displayField: 'name',
                        width: '60%',
                        labelWidth: '40%',
                        valueField: 'id',
                        emptyText: 'Нет данных для отображения...',
                        editable: false,
                        listeners: {
                            change: function () {
                                batchStore.load({
                                    params: {
                                        productId: Ext.getCmp('edProduct').getValue(),
                                        respondentId: Ext.getCmp('edRespondent').getValue()
                                    }
                                });
                            }
                        }
                    },
                    {
                        id: 'edBatch',
                        xtype: 'combobox',
                        displayField: 'fileName',
                        store: batchStore,
                        labelWidth: 70,
                        valueField: 'id',
                        emptyText: 'Нет данных для отображения...',
                        fieldLabel: label_BATCH,
                        editable: false,
                        width: '60%',
                        labelWidth: '40%',
                        queryMode: 'local',
                        listeners: {
                            change: function (combo, value) {
                                grid = Ext.getCmp('approvalEntityGrid');
                                grid.store.removeAll();
                                if (value !== null) {
                                    grid.store.load({
                                        params: {
                                            batchId: value
                                        }
                                    });
                                    grid.getView().refresh();
                                }
                            }
                        }
                    }
                ]
            },
                {
                    region: 'center',
                    id: 'form-area',
                    title: 'Сущности',
                    height: '80%',
                    split: true,
                    items: [approvalEntityGrid],
                    autoScroll: true
                }]
        },
            {
                id: 'gridPanel',
                region: 'center',
                width: "40%",
                split: true,
                items: [entityGrid],
                autoScroll: true
            }]
    });

});
