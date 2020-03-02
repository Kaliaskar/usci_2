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

    var updateQueueBatchGrid = function () {
        grid = Ext.getCmp("respondentsGrid");
        gridBatch = Ext.getCmp("queueBatchGrid");
        var credIds = [];
        if (isNb) {
            for (var i = 0; i < grid.store.getCount(); i++) {
                if (grid.getSelectionModel().isSelected(i)) {
                    credIds.push(grid.store.getAt(i).data.id);
                }
            }
        } else {
            credIds.push(grid.store.getAt(0).data.id);
        }
        gridBatch.store.load({
            params: {
                respondentIds: credIds
            },
            scope: this
        });
        gridBatch.getView().refresh();
    }

    var runner = new Ext.util.TaskRunner();
    var task = runner.newTask({
        run: updateQueueBatchGrid,
        interval: 2000
    });

    function createStoreClone(sourceStore, targetStore){
        var records = [];
        Ext.each(sourceStore.getRange(), function(record){
            records.push(record.copy());
        });
        targetStore.add(records);
    }

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

    var respondentsStore = Ext.create('Ext.data.Store', {
        id: 'respondentsStore',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/core/respondent/getUserRespondentList',
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

    var admRespondentsStore = Ext.create('Ext.data.Store', {
        id: 'admRespondentsStore',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: false,
        listeners: {
            load: function(me, records, options) {
                createStoreClone(respondentsStore, admRespondentsStore);
            }
        },
        sorters: [{
            property: 'name',
            direction: 'asc'
        }]
    });

    var admRespondentsStore2 = Ext.create('Ext.data.Store', {
        id: 'admRespondentsStore2',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: false,
        listeners: {
            load: function(me, records, options) {
                createStoreClone(respondentsStore, admRespondentsStore2);
                Ext.Ajax.request({
                    url: dataUrl+'/utils/config/getPriorityRespondentIds',
                    method: 'GET',
                    reader: {
                        type: 'json',
                        root: 'data'
                    },
                    success: function(response) {
                        var priopriorityRespondentIds = JSON.parse(response.responseText);
                        var records = [];
                        var storeItems = admRespondentsStore2.getRange();
                        for(var i=0; i<storeItems.length; i++){
                            if(Ext.Array.contains(priopriorityRespondentIds, storeItems[i].get('id'))) {
                                admRespondentsStore.remove(admRespondentsStore.getById(storeItems[i].get('id')));
                                records.push(admRespondentsStore2.getById(storeItems[i].get('id')));
                            }
                        }
                        admRespondentsStore2.removeAll(true);
                        Ext.getCmp('priorityRespondentGrid').getView().refresh();
                        Ext.getCmp('priorityRespondentGrid').store.add(records);
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

            }
        },
        sorters: [{
            property: 'name',
            direction: 'asc'
        }]
    });

    var queueBatchStore = Ext.create('Ext.data.Store', {
        id: 'queueBatchStore',
        fields: ['id', 'reportDate', 'receiverDate', 'processBeginDate', 'processEndDate', 'fileName', 'statusId', 'status', 'respondent', 'respondentId', 'productId', 'product', 'totalEntityCount', 'actualEntityCount', 'successEntityCount', 'errorEntityCount'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/receiver/batch/getPendingBatchList',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
    });

    var previewStore = Ext.create('Ext.data.Store', {
        id: 'previewStore',
        fields: ['id', 'reportDate', 'receiverDate', 'processBeginDate', 'processEndDate', 'fileName', 'statusId', 'status', 'respondent', 'respondentId', 'productId', 'product', 'totalEntityCount', 'actualEntityCount', 'successEntityCount', 'errorEntityCount'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/receiver/batch/getQueuePreviewBatches',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
    });

    var queueOrderTypeStore = Ext.create('Ext.data.Store', {
        id: 'queueOrderTypeStore',
        fields: ['id', 'type', 'code', 'nameRu', 'nameKz'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/utils/text/getTextListByType',
            method: 'GET',
            extraParams: {types: ['QUEUE_ALGO_OPTION']},
            reader: {
                type: 'json',
                root: ''
            }
        },
        listeners: {
            load: function(me, records, options) {
                if (records.length > 0)
                    Ext.getCmp('queueOrderType').setValue(records[0].get('code'));
            }
        }
    });

    queueBatchStore.on('load', function(s){
        var filesPanel = Ext.getCmp('filesRezumePanel');
        filesPanel.items.each(function(item){
            if (item.id != 'filesCountLabel') {
                filesPanel.items.remove(item);
            }
            filesPanel.update();
            filesPanel.doLayout();
        });
        var batch = {};
        queueBatchStore.each(function(rec) {
            var creditor = rec.get('respondent');
            var name = creditor.name
            if (!batch.hasOwnProperty(name)) {
                batch[name] = 0;
            }
            ++batch[name];
        });
        for (key in batch) {
            var value = batch[key];
            filesPanel.add(new Ext.form.Label({
                text: key+' - '+value
            }));
        }
        Ext.getCmp('filesCountLabel').setValue(queueBatchStore.getCount());
    });

    respondentsStore.on('load', function(s){
        if (isDataManager) {
            admRespondentsStore.load();
            admRespondentsStore2.load();
        }
    });

    var panel = Ext.create('Ext.tab.Panel', {
        height: 900,
        width: 1200,
        renderTo: 'queue-content',
        id: 'MainTabPanel',
        items: [{
            xtype: 'panel',
            title: label_QUEUE,
            items: [{
                xtype: 'panel',
                height: 900,
                margin: 0,
                width: 1200,
                title: '',
                titleCollapse: false,
                items: [{
                    xtype: 'displayfield',
                    id: 'nbDisplayField',
                    hidden: true,
                    padding: 3,
                    style: 'font-size: 20px;',
                    labelCls: 'biggertext',
                    value: label_ORGS,
                    fieldCls: 'biggertext'
                }, {
                    xtype: 'textfield',
                    id: 'nbSearchTextField',
                    hidden: true,
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
                    multiSelect: true,
                    height: 244,
                    width: 1195,
                    padding: 5,
                    autoScroll: true,
                    title: '',
                    hideHeaders: true,
                    id: 'respondentsGrid',
                    hidden: true,
                    store: respondentsStore,
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
                    id: 'nbBtnAll',
                    hidden: true,
                    margin: 5,
                    padding: 3,
                    text: label_SELECT_ALL,
                    listeners: {
                        click: function () {
                            Ext.getCmp('respondentsGrid').getSelectionModel().selectAll();
                        }
                    }
                }, {
                    xtype: 'panel',
                    border: false,
                    title: '',
                    layout: {
                        type: 'hbox',
                        align: 'stretch'
                    },
                    items: [{
                        xtype: 'button',
                        margin: '0 5 0 5',
                        text: label_DOWN_Q,
                        listeners: {
                            click: function () {
                                updateQueueBatchGrid();
                            }
                        }
                    }, {
                        xtype: 'checkboxfield',
                        margin: '0 0 0 300',
                        cls: 'checkBox',
                        fieldLabel: '',
                        boxLabel: label_AUTO,
                        listeners: {
                            change: function (field, newValue, oldValue, options) {
                                if (newValue == '1') {
                                    task.start();
                                } else if (newValue == '0') {
                                    task.destroy();
                                }
                            }
                        }
                    }, {
                        xtype: 'button',
                        margin: '0 0 0 300',
                        text: label_EXCEL,
                        listeners: {
                            click: function () {
                                gridBatch = Ext.getCmp("queueBatchGrid");

                                var batchJsonList = {columnList: [], batchList: []};
                                for (var i = 0; i < gridBatch.columns.length; i++)
                                    batchJsonList.columnList.push(gridBatch.columns[i].text);

                                for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                    batchJsonList.batchList.push(gridBatch.store.getAt(i).data);
                                }

                                var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: 'Подождите...'});
                                loadMask.show();

                                var xhr = new XMLHttpRequest();
                                xhr.open("POST", dataUrl+"/receiver/batch/getQueueBatchExcelContent", true);
                                xhr.setRequestHeader("Content-Type", "application/json");
                                xhr.responseType = "arraybuffer";
                                xhr.onload = function (oEvent) {
                                    if (xhr.status == 200) {
                                        var responseArray = new Uint8Array(this.response);
                                        var blob = new Blob([responseArray], {type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
                                        saveAs(blob, 'queue.xlsx');
                                    } else {
                                        Ext.Msg.alert("",label_ERROR);
                                    }
                                    loadMask.hide();
                                };
                                xhr.send(JSON.stringify(batchJsonList));
                            }
                        }
                    }]
                }, {
                    xtype: 'tabpanel',
                    margin: '10 0 0 0',
                    activeTab: 0,
                    items: [{
                        xtype: 'panel',
                        title: label_FILES,
                        items: [{
                            xtype: 'gridpanel',
                            id: 'queueBatchGrid',
                            store: queueBatchStore,
                            height: 400,
                            width: 1190,
                            margin: 5,
                            autoScroll: true,
                            title: label_FILE_INFO,
                            viewConfig: {
                                emptyText: 'Нет данных для отображения'
                            },
                            columns: [{
                                xtype: 'rownumberer',
                                width: 40,
                                text: '#'
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
                                    return filename;
                                }
                            }, {
                                xtype: 'gridcolumn',
                                width: 150,
                                dataIndex: 'product',
                                text: label_PRODUCT,
                                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                                    return value.name;
                                }
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
                                width: 130,
                                dataIndex: 'receiverDate',
                                text: label_REC_TIME,
                                format: 'd.m.Y H:i:s'
                            }, {
                                xtype: 'gridcolumn',
                                width: 203,
                                dataIndex: 'actualEntityCount',
                                text: label_AMOUNT
                            }, {
                                xtype: 'datecolumn',
                                width: 100,
                                dataIndex: 'reportDate',
                                text: label_REP_DATE,
                                format: 'd.m.Y'
                            }],
                            dockedItems: [{
                                xtype: 'toolbar',
                                dock: 'top',
                                items: [{
                                    xtype: 'button',
                                    text: 'Удалить батч',
                                    id: 'buttonRemoveBatch',
                                    disabled: true,
                                    listeners: {
                                        click: function () {
                                            if (!Ext.getCmp('queueBatchGrid').getSelectionModel().hasSelection())
                                                return;

                                            var batch = Ext.getCmp('queueBatchGrid').getSelectionModel().getLastSelected().data;

                                            Ext.Ajax.request({
                                                url: dataUrl + '/receiver/batch/removeBatchFromQueue',
                                                method: 'POST',
                                                params: {
                                                    batchId: batch.id
                                                },
                                                reader: {
                                                    type: 'json',
                                                    root: 'data'
                                                },
                                                success: function() {
                                                    Ext.Msg.alert("Батч удален из очереди");

                                                    updateQueueBatchGrid();
                                                },
                                                failure: function(response) {
                                                    var error = JSON.parse(response.responseText);

                                                    Ext.Msg.show({
                                                        title: 'Ошибка',
                                                        msg: error.message,
                                                        width : 300,
                                                        buttons: Ext.MessageBox.YES
                                                    });
                                                }
                                            });
                                        }
                                    }
                                }]
                            }],
                            listeners: {
                                cellclick: function (view, cell, cellIndex, record, row, rowIndex, e) {
                                    var batch = record.data;
                                    var batchId = record.data.id;
                                    var batchPath = record.data.fileName;

                                    // удалять батчи из очереди можно только если статус батча "в очереди"
                                    // то есть если же батч уже в обработке то его уже нельзя удалить
                                    Ext.getCmp('buttonRemoveBatch').setDisabled(batch.statusId !== 18);

                                    var linkClicked = (e.target.tagName == 'A');
                                    var clickedDataIndex =
                                        view.panel.headerCt.getHeaderAtIndex(cellIndex).dataIndex;

                                    if (linkClicked && clickedDataIndex == 'fileName') {
                                        var xhr = new XMLHttpRequest();
                                        xhr.open("GET", dataUrl+"/receiver/batch/getBatchContent?batchId="+batchId, true);
                                        xhr.responseType = "arraybuffer";
                                        xhr.onload = function (oEvent) {
                                            if (xhr.status == 200) {
                                                var responseArray = new Uint8Array(this.response);
                                                var blob = new Blob([responseArray], {type: "application/zip"});
                                                var fileName = getFileName(batchPath);
                                                saveAs(blob, fileName+'.zip');
                                            } else {
                                                Ext.Msg.alert("",label_ERROR);
                                            }
                                        };
                                        xhr.send();
                                    }
                                }
                            }
                        }]
                    }, {
                        xtype: 'panel',
                        id: 'filesRezumePanel',
                        title: label_RESUME,
                        layout: {
                            type: 'vbox',
                            align: 'stretch'
                        },
                        items: [{
                            xtype: 'displayfield',
                            id: 'filesCountLabel',
                            width: 280,
                            fieldLabel: label_FILES_IN_Q,
                            labelStyle: 'font-weight: bold;',
                            labelWidth: 130,
                            fieldStyle: 'font-weight: bold;'
                        }]
                    }]
                }]
            }]
        }, {
            xtype: 'panel',
            hidden: !isDataManager,
            title: label_Q_CONTROL,
            items: [{
                xtype: 'panel',
                border: false,
                height: 513,
                margin: '5 0 0 0',
                title: '',
                layout: {
                    type: 'vbox',
                    align: 'center'
                },
                items: [{
                    xtype: 'combobox',
                    id: 'queueOrderType',
                    store: queueOrderTypeStore,
                    margin: '10 0 0 0',
                    width: 321,
                    editable : false,
                    valueField: 'code',
                    displayField: 'nameRu',
                    fieldLabel: label_SETTINGS,
                    labelAlign: 'top',
                    labelStyle: 'font-weight: bold; text-align:center;'
                }, {
                    xtype: 'label',
                    margin: '10 0 0 0',
                    style: 'font-weight: bold;',
                    text: label_CHOOSE_P
                }, {
                    xtype: 'panel',
                    border: false,
                    height: 184,
                    margin: '5 0 0 0',
                    width: '100%',
                    title: '',
                    layout: {
                        type: 'hbox',
                        align: 'stretch'
                    },
                    items: [{
                        xtype: 'container',
                        width: '48%',
                        items: [{
                            xtype: 'gridpanel',
                            id: 'nonPriorityRespondentGrid',
                            store: admRespondentsStore,
                            multiSelect: true,
                            height: 176,
                            padding: 5,
                            autoScroll: true,
                            title: '',
                            hideHeaders: true,
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
                        }]
                    }, {
                        xtype: 'container',
                        width: '4%',
                        layout: {
                            type: 'vbox',
                            align: 'center',
                            pack: 'center'
                        },
                        items: [{
                            xtype: 'button',
                            margin: '0 0 20 0',
                            text: '>>',
                            listeners: {
                                click: function () {
                                    if (Ext.getCmp('nonPriorityRespondentGrid').getSelectionModel().hasSelection()) {
                                        var records = Ext.getCmp('nonPriorityRespondentGrid').getSelectionModel().getSelection();
                                        Ext.getCmp('nonPriorityRespondentGrid').getStore().remove(records);
                                        Ext.getCmp('priorityRespondentGrid').getStore().add(records);
                                        Ext.getCmp('nonPriorityRespondentGrid').getView().refresh();
                                        Ext.getCmp('priorityRespondentGrid').getView().refresh();
                                    }
                                }
                            }
                        }, {
                            xtype: 'button',
                            text: '<<',
                            listeners: {
                                click: function () {
                                    if (Ext.getCmp('priorityRespondentGrid').getSelectionModel().hasSelection()) {
                                        var records = Ext.getCmp('priorityRespondentGrid').getSelectionModel().getSelection();
                                        Ext.getCmp('priorityRespondentGrid').getStore().remove(records);
                                        Ext.getCmp('nonPriorityRespondentGrid').getStore().add(records);
                                        Ext.getCmp('nonPriorityRespondentGrid').getView().refresh();
                                        Ext.getCmp('priorityRespondentGrid').getView().refresh();
                                    }
                                }
                            }
                        }]
                    }, {
                        xtype: 'container',
                        width: '48%',
                        items: [{
                            xtype: 'gridpanel',
                            id: 'priorityRespondentGrid',
                            store: admRespondentsStore2,
                            multiSelect: true,
                            height: 176,
                            padding: 5,
                            autoScroll: true,
                            title: '',
                            hideHeaders: true,
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
                        }]
                    }]
                }, {
                    xtype: 'button',
                    text: label_SAVE,
                    listeners: {
                        click: function () {
                            priorityGrid = Ext.getCmp("priorityRespondentGrid");
                            var credIds = [];
                            for (var i = 0; i < priorityGrid.store.getCount(); i++)
                                credIds.push(priorityGrid.store.getAt(i).data.id);
                            var queueAlgo = Ext.getCmp('queueOrderType').getValue();

                            Ext.Ajax.request({
                                url: dataUrl+'/utils/config/updateQueueConfig',
                                method: 'POST',
                                params: {
                                    priorityRespondentIds: credIds,
                                    queueAlgorithm: queueAlgo
                                },
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function() {
                                    Ext.Msg.alert(label_CONFIRMED);
                                    Ext.Ajax.request({
                                        url: dataUrl+'/receiver/batch/reloadQueueConfig',
                                        method: 'POST',
                                        reader: {
                                            type: 'json',
                                            root: 'data'
                                        },
                                        success: function() {
                                            Ext.Msg.show({
                                                title: '',
                                                msg: label_CONFIG,
                                                width : 300,
                                                buttons: Ext.MessageBox.YES
                                            });
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

                        }
                    }
                }, {
                    xtype: 'button',
                    margin: '20 0 0 0',
                    text: label_VIEW,
                    listeners: {
                        click: function () {
                            gridPreview = Ext.getCmp("previewGrid");
                            priorityGrid = Ext.getCmp("priorityRespondentGrid");
                            var credIds = [];
                            for (var i = 0; i < priorityGrid.store.getCount(); i++)
                                credIds.push(priorityGrid.store.getAt(i).data.id);
                            var queueAlgo = Ext.getCmp('queueOrderType').getValue();

                            gridPreview.store.load({
                                params: {
                                    respondentsWithPriority: credIds,
                                    queueAlgo: queueAlgo
                                },
                                scope: this
                            });
                            gridPreview.show();
                            gridPreview.getView().refresh();
                        }
                    }
                }, {
                    xtype: 'gridpanel',
                    id: 'previewGrid',
                    hidden: true,
                    store: previewStore,
                    height: 400,
                    width: 1190,
                    margin: 5,
                    autoScroll: true,
                    title: '',
                    columns: [{
                        xtype: 'rownumberer',
                        width: 40,
                        text: '#'
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
                            return filename;
                        }
                    }, {
                        xtype: 'gridcolumn',
                        width: 150,
                        dataIndex: 'product',
                        text: label_PRODUCT,
                        renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                            return value.name;
                        }
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
                        width: 130,
                        dataIndex: 'receiverDate',
                        text: label_REC_TIME,
                        format: 'd.m.Y H:i:s'
                    }, {
                        xtype: 'gridcolumn',
                        width: 203,
                        dataIndex: 'actualEntityCount',
                        text: label_AMOUNT
                    }, {
                        xtype: 'datecolumn',
                        width: 100,
                        dataIndex: 'reportDate',
                        text: label_REP_DATE,
                        format: 'd.m.Y'
                    }]
                }]
            }]
        }],
        listeners: {
            afterrender: function () {
                respondentsStore.load({
                    params: {
                        userId: userId
                    },
                    scope: this
                });
                if (isNb) {
                    Ext.getCmp("nbDisplayField").show();
                    Ext.getCmp("nbSearchTextField").show();
                    Ext.getCmp("respondentsGrid").show();
                    Ext.getCmp("nbBtnAll").show();
                }
            }
        }
    });
});