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


Ext.Ajax.timeout= 240000;
Ext.override(Ext.data.proxy.Ajax, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.proxy.Server, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.Connection, { timeout: Ext.Ajax.timeout });

Ext.override(Ext.data.Store, {
    removeAll: function(silent) {
        var me = this;

        me.clearData();
        if (me.snapshot) {
            me.snapshot.clear();
        }
        if (me.prefetchData) {
            me.prefetchData.clear();
        }

        delete me.guaranteedStart;
        delete me.guaranteedEnd;
        delete me.totalCount;

        if (silent !== true) {
            me.fireEvent('clear', me);
        }
    }
});



Ext.onReady(function () {

    var bvuFilter = new Ext.util.Filter({
        filterFn: function(item) {
            var obj = item.get('subjectType');
            return obj.id != '1' ? true : false;
        }
    });

    var bpuFilter = new Ext.util.Filter({
        filterFn: function(item) {
            var obj = item.get('subjectType');
            return obj.id != null ? true : false;
        }
    });

    var elseorgFilter = new Ext.util.Filter({
        filterFn: function(item) {
            var obj = item.get('subjectType');
            return obj.id != '3' ? true : false;
        }
    });

    var ipotechFilter = new Ext.util.Filter({
        filterFn: function(item) {
            var obj = item.get('subjectType');
            return obj.id != '2' ? true : false;
        }
    });

    var errorProtocolFilter = new Ext.util.Filter({
        filterFn: function(item) {
            var obj = item.get('status');
            if (obj != null) {
                return obj.code != 'ERROR' ? true : false;
            } else {
                return obj == null ? true : false;
            }
        }
    });

    var okProtocolFilter = new Ext.util.Filter({
        filterFn: function(item) {
            var obj = item.get('status');
            if (obj != null) {
                return obj.code != 'STORED' ? true : false;
            } else {
                return obj == null ? true : false;
            }
        }
    });

    function getFileName(value) {
        var startIndex = (value.indexOf('\\') >= 0 ? value.lastIndexOf('\\') : value.lastIndexOf('/'));
        var filename = value.substring(startIndex);
        if (filename.indexOf('\\') === 0 || filename.indexOf('/') === 0) {
            filename = filename.substring(1);
        }

        return filename;
    }

    Ext.Date.patterns={
        CustomFormat: "d.m.Y"
    };

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

    var batchStore = Ext.create('Ext.data.Store', {
        id: 'batchStore',
        fields: ['id', 'reportDate', 'receiverDate', 'processBeginDate', 'processEndDate', 'fileName', 'statusId', 'status', 'respondent', 'respondentId', 'productId', 'product', 'totalEntityCount', 'actualEntityCount', 'successEntityCount', 'errorEntityCount', 'maintenanceEntityCount', 'signInfo'],
        autoLoad: false,
        pageSize: 100,
        proxy: {
            timeout: 240000,
            type: 'ajax',
            url: dataUrl+'/receiver/batch/getBatchList',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
    });

    var credIds = [];
    var reportDate;
    var batchId;

    batchStore.on('beforeload', function() {
        var proxy = batchStore.getProxy();
        proxy.setExtraParam('respondentIds', credIds);
        proxy.setExtraParam('userId', userId);
        proxy.setExtraParam('isNb', isNb);
        proxy.setExtraParam('reportDate', reportDate);
    });

    var protocolStore = Ext.create('Ext.data.Store', {
        id: 'protocolStore',
        fields: ['batchId', 'entityId', 'metaClassId', 'statusId', 'status', 'textCode', 'operation', 'textRu', 'textKz', 'comments', 'entityText'],
        autoLoad: false,
        pageSize: 20000,
        buffered: true,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/receiver/batch/getBatchStatusList',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        }
    });

    protocolStore.on('beforeload', function() {
        var proxy = protocolStore.getProxy();
        proxy.setExtraParam('batchId', batchId);
    });

    var panel = Ext.create('Ext.panel.Panel', {
        height: 1200,
        margin: 0,
        width: 1200,
        autoScroll: true,
        title: '          ',
        titleCollapse: false,
        renderTo: 'protocol-content',
        id: 'MainPanel',
        items: [{
            xtype: 'displayfield',
            id: 'nbDisplayField',
            hidden: true,
            padding: 3,
            style: 'font-size: 20px;',
            labelCls: 'biggertext',
            value: label_ORGANIZATIONS,
            fieldCls: 'biggertext'
        }, {
            xtype: 'panel',
            id: 'nbCheckBoxPanel',
            hidden: true,
            width: 1195,
            border: false,
            title: '',
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'checkboxgroup',
                width: 1000,
                layout: {
                    type: 'checkboxgroup',
                    autoFlex: false
                },
                items: [{
                    xtype: 'checkboxfield',
                    checked: true,
                    cls: 'checkBox',
                    width: 135,
                    boxLabel: label_VU,
                    id: 'chkBVU',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("respondentsGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(bvuFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(bvuFilter);
                            }
                        }
                    }
                }, {
                    xtype: 'checkboxfield',
                    checked: true,
                    cls: 'checkBox',
                    width: 136,
                    boxLabel: label_PU,
                    id: 'chkBPU',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("respondentsGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(bpuFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(bpuFilter);
                            }
                        }
                    }
                }, {
                    xtype: 'checkboxfield',
                    checked: true,
                    cls: 'checkBox',
                    width: 152,
                    boxLabel: label_IO,
                    id: 'chkIpotech',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("respondentsGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(ipotechFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(ipotechFilter);
                            }
                        }
                    }
                }, {
                    xtype: 'checkboxfield',
                    checked: true,
                    cls: 'checkBox',
                    width: 465,
                    boxLabel: label_OTHERS,
                    id: 'chkElseOrg',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("respondentsGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(elseorgFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(elseorgFilter);
                            }
                        }
                    }
                }]
            }, {
                xtype: 'button',
                text: label_SELECT,
                listeners: {
                    click: function () {
                        Ext.getCmp('chkBVU').setValue(true);
                        Ext.getCmp('chkBPU').setValue(true);
                        Ext.getCmp('chkElseOrg').setValue(true);
                        Ext.getCmp('chkIpotech').setValue(true);
                    }
                }
            }, {
                xtype: 'button',
                text: label_OFF_SELECTED,
                listeners: {
                    click: function () {
                        Ext.getCmp('chkBVU').setValue(false);
                        Ext.getCmp('chkBPU').setValue(false);
                        Ext.getCmp('chkElseOrg').setValue(false);
                        Ext.getCmp('chkIpotech').setValue(false);
                    }
                }
            }]
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
                        if (Ext.getCmp('chkBVU').checked == false) {
                            grid.store.addFilter(bvuFilter)
                        };
                        if (Ext.getCmp('chkBPU').checked == false) {
                            grid.store.addFilter(bpuFilter)
                        };
                        if (Ext.getCmp('chkElseOrg').checked == false) {
                            grid.store.addFilter(elseorgFilter)
                        };
                        if (Ext.getCmp('chkIpotech').checked == false) {
                            grid.store.addFilter(ipotechFilter)
                        };
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
            hidden:true,
            multiSelect: true,
            height: 244,
            width: 1195,
            padding: 5,
            autoScroll: true,
            title: '',
            hideHeaders: true,
            id: 'respondentsGrid',
            store: respondentsStore,
            scroll: 'vertical',
            columns: [{
                xtype: 'gridcolumn',
                width: '100%',
                dataIndex: 'name',
                text: ''
            }],
            viewConfig: {
                emptyText: label_NO_DATA
            }
        }, {
            xtype: 'button',
            id: 'nbBtnAll',
            hidden: true,
            margin: 5,
            padding: 3,
            text: label_CHOOSE,
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
            margin: '0 0 5 5',
            text: label_SHOW,
            listeners: {
                click: function () {
                    credIds = [];
                    grid = Ext.getCmp("respondentsGrid");
                    gridBatch = Ext.getCmp("batchGrid");
                    if (isNb) {
                        for (var i = 0; i < grid.store.getCount(); i++) {
                            if (grid.getSelectionModel().isSelected(i)) {
                                credIds.push(grid.store.getAt(i).data.id);
                            }
                        }
                    } else {
                        credIds.push(grid.store.getAt(0).data.id);
                    }
                    reportDate = Ext.Date.format(Ext.getCmp('reportDate').value, Ext.Date.patterns.CustomFormat);
                    gridBatch.store.currentPage = 1;
                    gridBatch.store.load({
                        params: {
                            respondentIds: credIds,
                            userId: userId,
                            isNb: isNb,
                            reportDate: reportDate
                        },
                        scope: this
                    });
                    gridBatch.getView().refresh();
                }
            }
        }, {
            xtype: 'gridpanel',
            viewConfig: { emptyText: label_NO_DATA },
            id: 'batchGrid',
            store: batchStore,
            height: 400,
            width: 1190,
            margin: '5 0 15 5',
            autoScroll: true,
            title: label_FILE_INFO,
            columns: [{
                xtype: 'gridcolumn',
                width: 180,
                dataIndex: 'respondent',
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    return value.name;
                },
                text: label_ORG_NAME
            }, {
                xtype: 'gridcolumn',
                width: 180,
                dataIndex: 'fileName',
                text: label_FILE_NAME,
                renderer  : function(value, obj, record) {
                    var filename = getFileName(value);
                    return '<a href="#">'+filename+'</a>';
                }
            }, {
                xtype: 'gridcolumn',
                width: 180,
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
                xtype: 'gridcolumn',
                width: 95,
                dataIndex: 'totalEntityCount',
                text: 'Всего'
            }, {
                xtype: 'gridcolumn',
                width: 95,
                dataIndex: 'actualEntityCount',
                text: label_EDITED
            }, {
                xtype: 'gridcolumn',
                width: 135,
                dataIndex: 'successEntityCount',
                text: label_SUC_EDITED,
                renderer: function(value, metaData, record, rowIndex, colIndex, store) {
                    if (value !== 0) {
                        value = "<span style='color:green' >"+value+"</span>";
                        return value;
                    } else {
                        return value;
                    }
                }
            }, {
                xtype: 'gridcolumn',
                width: 135,
                dataIndex: 'errorEntityCount',
                text: label_ERR_EDITED,
                renderer: function(value, metaData, record, rowIndex, colIndex, store) {
                    if (value !== 0) {
                        value = "<span style='color:red' >"+value+"</span>";
                        return value;
                    } else {
                        return value;
                    }
                }
            }, {
                xtype: 'gridcolumn',
                width: 135,
                dataIndex: 'maintenanceEntityCount',
                text: 'На одобрении',
                renderer: function(value, metaData, record, rowIndex, colIndex, store) {
                    if (value !== 0) {
                        value = "<span style='color:orange' >"+value+"</span>";
                        return value;
                    } else {
                        return value;
                    }
                }
            }, {
                xtype: 'datecolumn',
                width: 100,
                dataIndex: 'reportDate',
                text: label_DATE_REP,
                format: 'd.m.Y'
            }, {
                xtype: 'datecolumn',
                width: 130,
                dataIndex: 'receiverDate',
                text: label_RECIEPED,
                format: 'd.m.Y H:i:s'
            }, {
                xtype: 'datecolumn',
                width: 130,
                dataIndex: 'processBeginDate',
                text: label_DATE_BEGINING,
                format: 'd.m.Y H:i:s'
            }, {
                xtype: 'datecolumn',
                width: 130,
                dataIndex: 'processEndDate',
                text: label_DATE_END,
                format: 'd.m.Y H:i:s'
            }],
            dockedItems: [{
                xtype: 'pagingtoolbar',
                id: 'tbarForFilesGrid',
                store: batchStore,
                dock: 'bottom',
                width: 360,
                afterPageText: 'из {0}',
                beforePageText: label_PAGE,
                displayInfo: true,
                displayMsg: label_FILES,
                emptyMsg: label_NO_DATA_SIG,
                firstText: label_FIRST,
                lastText: label_LAST,
                nextText: label_NEXT,
                prevText: label_PREVIOUS,
                refreshText: label_REFRESH
            }, {
                xtype: 'toolbar',
                dock: 'top',
                items: [{
                    xtype: 'button',
                    icon: contextPathUrl + '/pics/excel.png',
                    text: label_EXCEL,
                    listeners: {
                        click: function () {
                            gridBatch = Ext.getCmp("batchGrid");

                            var batchJsonList = {columnList: [], batchList: []};
                            for (var i = 0; i < gridBatch.columns.length; i++) {
                                if (i == 0 || i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6 || i == 10 ) {
                                    batchJsonList.columnList.push(gridBatch.columns[i].text);
                                }
                            }
                            for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                batchJsonList.batchList.push(gridBatch.store.getAt(i).data);
                            }

                            var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_WAIT});
                            loadMask.show();

                            var xhr = new XMLHttpRequest();
                            xhr.open("POST", dataUrl+"/receiver/batch/getBatchExcelContent", true);
                            xhr.setRequestHeader("Content-Type", "application/json");
                            xhr.responseType = "arraybuffer";
                            xhr.onload = function (oEvent) {
                                if (xhr.status == 200) {
                                    var responseArray = new Uint8Array(this.response);
                                    var blob = new Blob([responseArray], {type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
                                    saveAs(blob, 'files.xlsx');
                                } else {
                                    Ext.Msg.alert("",label_ERROR);
                                }
                                loadMask.hide();
                            };
                            xhr.send(JSON.stringify(batchJsonList));
                        }
                    }
                }]
            }],
            listeners: {
                cellclick: function (view, cell, cellIndex, record, row, rowIndex, e) {
                    batchId = record.data.id;
                    var batchPath = record.data.fileName;

                    var linkClicked = (e.target.tagName == 'A');
                    var clickedDataIndex =
                        view.panel.headerCt.getHeaderAtIndex(cellIndex).dataIndex;

                    if (linkClicked && clickedDataIndex == 'fileName') {
                        var loadMask = new Ext.LoadMask(Ext.getCmp('MainPanel'), {msg: label_WAIT});
                        loadMask.show();

                        var xhr = new XMLHttpRequest();
                        xhr.open("GET", dataUrl+"/receiver/batch/getBatchContent?batchId="+batchId, true);
                        xhr.responseType = "arraybuffer";
                        xhr.onload = function (oEvent) {
                            if (xhr.status == 200) {
                                var responseArray = new Uint8Array(this.response);
                                var blob = new Blob([responseArray], {type: "application/zip"});
                                var fileName = getFileName(batchPath);
                                saveAs(blob, fileName);
                            } else {
                                Ext.Msg.alert("",label_ERROR);
                            }
                        };
                        xhr.send();
                        loadMask.hide();
                    }
                    gridProtocol = Ext.getCmp("protocolGrid");
                    labelProtocol = Ext.getCmp("batchSignLabel");

                    var signInfo = record.data.signInfo;
                    if (signInfo == "" || signInfo == "null") {
                        labelProtocol.setText(label_NO_D);
                    } else {
                        labelProtocol.setText('Файл подписан: '+signInfo);
                    }

                    gridProtocol.store.removeAll();
                    gridProtocol.store.currentPage = 1;
                    gridProtocol.getView().refresh();

                    var loadMask = new Ext.LoadMask(Ext.getCmp('MainPanel'), {msg: label_WAIT});
                    loadMask.show();

                    gridProtocol.store.load({
                        params: {
                            batchId: batchId,
                            statusTypes: ['ERROR', 'STORED', 'MAINTENANCE']
                        },
                        scope: this
                    });
                    gridProtocol.show();
                    labelProtocol.show();
                    gridProtocol.getView().refresh();
                    loadMask.hide();
                }
            }
        }, {
            xtype: 'label',
            id: 'batchSignLabel',
            hidden: true,
            margin: '5 0 15 5',
            style: 'font-weight: bold',
            text: label_SIGN_FILE
        }, {
            xtype: 'gridpanel',
            viewConfig: {
                emptyText: label_NO_DATA
            },
            plugins: [{
                ptype: 'bufferedrenderer'
            }],
            selModel: {
                pruneRemoved: false
            },
            id: 'protocolGrid',
            hidden: true,
            store: protocolStore,
            height: 400,
            width: 1190,
            margin: '15 0 15 5',
            autoScroll: true,
            title: label_ERRORS,
            columns: [{
                xtype: 'gridcolumn',
                width: 50,
                dataIndex: 'status',
                text: '',
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    if (record.get('status') == null) {
                        return '<img src="'+ contextPathUrl + '/pics/accept.png"/>';
                    } else {
                        if (value.code == 'ERROR') {
                            return '<img src="'+ contextPathUrl + '/pics/cancel.png"/>';
                        } else if (value.code == 'STORED') {
                            return '<img src="'+ contextPathUrl + '/pics/accept.png"/>';
                        }
                    }
                }
            }, {
                xtype: 'gridcolumn',
                width: 136,
                dataIndex: 'status',
                text: label_NOTE_TYPE,
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    if (record.get('status') == null) {
                        return '';
                    } else {
                        return value.nameRu;
                    }
                }
            }, {
                xtype: 'gridcolumn',
                width: 70,
                dataIndex: 'entityId',
                text: label_ID_ENTITY
            }, {
                xtype: 'gridcolumn',
                width: 400,
                dataIndex: 'entityText',
                text: label_ENTITY
            }, {
                xtype: 'gridcolumn',
                width: 100,
                dataIndex: 'operation',
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    if (record.get('operation') == null) {
                        return '';
                    } else {
                        return value.nameRu;
                    }
                },
                text: label_TYPE_OPER
            }, {
                xtype: 'gridcolumn',
                width: 240,
                dataIndex: 'textRu',
                text: label_MESSAGE
            }, {
                xtype: 'gridcolumn',
                width: 240,
                dataIndex: 'comments',
                text: label_COMMENT
            }, {
                xtype: 'gridcolumn',
                width: 100,
                dataIndex: 'status',
                text: '',
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    if (record.get('status') == null) {
                        return '';
                    } else {
                        if (value.code == 'STORED') {
                            var entityId = record.get('entityId');
                            var metaClassId = record.get('metaClassId');
                            var repDate = Ext.getCmp('batchGrid').getSelectionModel().getLastSelected().data.reportDate;
                            var respondentId = Ext.getCmp('batchGrid').getSelectionModel().getLastSelected().data.respondentId;
                            return '<a href="'+entityEditorUrl+'?entityId='+entityId+'&metaClassId='+metaClassId+'&repDate='+repDate+'&respondentId='+respondentId+'" target="_blank">Просмотр</a>';
                        }
                    }
                }
            }],
            dockedItems: [/*{
                xtype: 'pagingtoolbar',
                id: 'tbarForProtocolsGrid',
                store: protocolStore,
                dock: 'bottom',
                width: 360,
                afterPageText: 'из {0}',
                beforePageText: label_PAGE,
                displayInfo: true,
                displayMsg: label_PROTOCOLS,
                emptyMsg: label_NO_DATA_SIG,
                firstText: label_FIRST,
                lastText: label_LAST,
                nextText: label_NEXT,
                prevText: label_PREVIOUS,
                refreshText: label_REFRESH
            }, */{
                xtype: 'toolbar',
                dock: 'top',
                items: [{
                    xtype: 'button',
                    id: 'completedButton',
                    text: 'Показать успешные',
                    listeners: {
                        click: function(){
                            grid = Ext.getCmp("protocolGrid");
                            grid.store.removeAll();
                            grid.store.load({
                                params: {
                                    batchId: batchId,
                                    statusTypes: ['STORED']
                                },
                                scope: this
                            });
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, {
                    xtype: 'button',
                    id: 'errorButton',
                    text: 'Показать ошибочные',
                    listeners: {
                        click: function(){
                            grid = Ext.getCmp("protocolGrid");
                            grid.store.removeAll();
                            grid.store.load({
                                params: {
                                    batchId: batchId,
                                    statusTypes: ['ERROR']
                                },
                                scope: this
                            });
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, {
                    xtype: 'button',
                    id: 'maintenanceButton',
                    text: 'Показать на одобрении',
                    listeners: {
                        click: function(){
                            grid = Ext.getCmp("protocolGrid");
                            grid.store.removeAll();
                            grid.store.load({
                                params: {
                                    batchId: batchId,
                                    statusTypes: ['MAINTENANCE']
                                },
                                scope: this
                            });
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, {
                    xtype: 'button',
                    id: 'allButton',
                    text: 'Показать все',
                    listeners: {
                        click: function(){
                            grid = Ext.getCmp("protocolGrid");
                            grid.store.removeAll();
                            grid.store.load({
                                params: {
                                    batchId: batchId,
                                    statusTypes: ['ERROR', 'STORED', 'MAINTENANCE']
                                },
                                scope: this
                            });
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, "->", {
                    xtype: 'button',
                    icon: contextPathUrl + '/pics/xml.png',
                    text: label_XML_FORMAT,
                    listeners: {
                        click: function () {
                            var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_WAIT});
                            loadMask.show();

                            gridProtocol = Ext.getCmp("protocolGrid");
                            var batchPath = Ext.getCmp('batchGrid').getSelectionModel().getLastSelected().data.fileName;

                            var xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><protocol></protocol>";
                            var parser = new DOMParser();
                            var xmlDocument = parser.parseFromString(xmlData, "application/xml");

                            var protocolElements = xmlDocument.getElementsByTagName("protocol");

                            for(var i = 0; i < gridProtocol.store.getCount(); i++){
                                if(gridProtocol.store.getAt(i).data.status != null){
                                    var type = gridProtocol.store.getAt(i).data.status.code;
                                    var level = gridProtocol.store.getAt(i).data.status.code;
                                    var entityId = gridProtocol.store.getAt(i).data.entityId;
                                    var entity = gridProtocol.store.getAt(i).data.entityText;
                                    var messagecode = gridProtocol.store.getAt(i).data.status.nameRu;
                                    var message = gridProtocol.store.getAt(i).data.textRu;
                                    var note = gridProtocol.store.getAt(i).data.comments;

                                    var protocolNode = xmlDocument.createElement("protocolRecord");

                                    var typeNode = xmlDocument.createElement("type");
                                    var protocolType = xmlDocument.createTextNode(type);
                                    typeNode.appendChild(protocolType);
                                    protocolNode.appendChild(typeNode);

                                    var levelNode = xmlDocument.createElement("level");
                                    var protocolLevel = xmlDocument.createTextNode(level);
                                    levelNode.appendChild(protocolLevel);
                                    protocolNode.appendChild(levelNode);

                                    var entityIdNode = xmlDocument.createElement("entityId");
                                    var protocolEntityId = xmlDocument.createTextNode(entityId);
                                    entityIdNode.appendChild(protocolEntityId);
                                    protocolNode.appendChild(entityIdNode);

                                    var entityNode = xmlDocument.createElement("entity");
                                    var protocolEntity = xmlDocument.createTextNode(entity);
                                    entityNode.appendChild(protocolEntity);
                                    protocolNode.appendChild(entityNode);

                                    var messagecodeNode = xmlDocument.createElement("messagecode");
                                    var protocolMessagecode = xmlDocument.createTextNode(messagecode);
                                    messagecodeNode.appendChild(protocolMessagecode);
                                    protocolNode.appendChild(messagecodeNode);

                                    var messageNode = xmlDocument.createElement("message");
                                    var protocolMessage = xmlDocument.createTextNode(message);
                                    messageNode.appendChild(protocolMessage);
                                    protocolNode.appendChild(messageNode);

                                    var noteNode = xmlDocument.createElement("note");
                                    var protocolNote = xmlDocument.createTextNode(note);
                                    noteNode.appendChild(protocolNote);
                                    protocolNode.appendChild(noteNode);

                                    protocolElements[0].appendChild(protocolNode);
                                }
                            }

                            var xsltDoc = new DOMParser().parseFromString([
                                '<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">',
                                ' <xsl:output omit-xml-declaration="yes" indent="yes"/>',
                                '    <xsl:template match="node()|@*">',
                                '      <xsl:copy>',
                                '        <xsl:apply-templates select="node()|@*"/>',
                                '      </xsl:copy>',
                                '    </xsl:template>',
                                '</xsl:stylesheet>',
                            ].join('\n'), 'application/xml');
                            var xsltProcessor = new XSLTProcessor();
                            xsltProcessor.importStylesheet(xsltDoc);
                            var resultDoc = xsltProcessor.transformToDocument(xmlDocument);
                            var serializer = new XMLSerializer();
                            var strXmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"+serializer.serializeToString(resultDoc);
                            var blob = new Blob([strXmlData], {type: "application/xml"});
                            var fileName = getFileName(batchPath);
                            saveAs(blob, fileName.substring(0, fileName.lastIndexOf('.'))+'.xml');

                            loadMask.hide();
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, {
                    xtype: 'button',
                    icon: contextPathUrl + '/pics/txt.png',
                    text: label_DOWN_ERRORS,
                    listeners: {
                        click: function () {
                            var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_WAIT});
                            loadMask.show();

                            gridProtocol = Ext.getCmp("protocolGrid");
                            var entityList = [];
                            var batchPath = Ext.getCmp('batchGrid').getSelectionModel().getLastSelected().data.fileName;

                            for (var i = 0; i < gridProtocol.store.getCount(); i++) {
                                if (gridProtocol.store.getAt(i).data.status != null && gridProtocol.store.getAt(i).data.status.code == 'ERROR') {
                                    entityList.push(gridProtocol.store.getAt(i).data.entityText);
                                }
                            }
                            var entityListString = entityList.join("\n")
                            var blob = new Blob([entityListString], {type: "text/plain"});
                            var fileName = getFileName(batchPath);
                            saveAs(blob, fileName.substring(0, fileName.lastIndexOf('.'))+'.txt');

                            loadMask.hide();
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, {
                    xtype: 'button',
                    icon: contextPathUrl + '/pics/excel.png',
                    text: label_EXCEL,
                    listeners: {
                        click: function () {
                            gridProtocol = Ext.getCmp("protocolGrid");

                            var batchPath = Ext.getCmp('batchGrid').getSelectionModel().getLastSelected().data.fileName;

                            var batchStatusJsonList = {columnList: [], protocolList: []};

                            batchStatusJsonList.columnList.push(gridProtocol.columns[2].text);
                            batchStatusJsonList.columnList.push(gridProtocol.columns[3].text);
                            batchStatusJsonList.columnList.push(gridProtocol.columns[1].text);
                            batchStatusJsonList.columnList.push(label_CRITIC);
                            batchStatusJsonList.columnList.push(gridProtocol.columns[5].text);
                            batchStatusJsonList.columnList.push(gridProtocol.columns[6].text);

                            for (var i = 0; i < gridProtocol.store.getCount(); i++) {
                                batchStatusJsonList.protocolList.push(gridProtocol.store.getAt(i).data);
                            }

                            var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_WAIT});
                            loadMask.show();

                            var xhr = new XMLHttpRequest();
                            xhr.open("POST", dataUrl+"/receiver/batch/getProtocolExcelContent", true);
                            xhr.setRequestHeader("Content-Type", "application/json");
                            xhr.responseType = "arraybuffer";
                            xhr.onload = function (oEvent) {
                                if (xhr.status == 200) {
                                    var responseArray = new Uint8Array(this.response);
                                    var blob = new Blob([responseArray], {type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
                                    var fileName = getFileName(batchPath);
                                    saveAs(blob, fileName.substring(0, fileName.lastIndexOf('.'))+'.xlsx');
                                } else {
                                    Ext.Msg.alert("",label_ERROR);
                                }
                                loadMask.hide();
                            };
                            xhr.send(JSON.stringify(batchStatusJsonList));
                        }
                    }
                }, {
                    xtype: 'tbseparator'
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
                    Ext.getCmp("nbCheckBoxPanel").show();
                    Ext.getCmp("nbSearchTextField").show();
                    Ext.getCmp("respondentsGrid").show();
                    Ext.getCmp("nbBtnAll").show();
                }
            }
        }
    });
});
