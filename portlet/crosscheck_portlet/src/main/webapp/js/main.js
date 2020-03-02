Ext.require([
    'Ext.tab.*',
    'Ext.tree.*',
    'Ext.data.*',
    'Ext.tip.*'
]);


Ext.Ajax.timeout= 3000000;
Ext.override(Ext.data.proxy.Ajax, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.proxy.Server, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.Connection, { timeout: Ext.Ajax.timeout });

Ext.onReady(function(){

    Ext.define('creditorListModel',{
        extend: 'Ext.data.Model',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType']
    });

    Ext.define('crossCheckStoreModel',{
        extend: 'Ext.data.Model',
        fields: ['id', 'dateBegin', 'dateEnd', 'creditorId', 'reportDate', 'status', 'statusName', 'username', 'creditorName']
    });

    Ext.define('crossCheckMessageModel',{
        extend: 'Ext.data.Model',
        fields: ['description', 'help', 'difference', 'outerValue', 'innerValue', 'isError', 'nonCritical' ]
    });

    var productStore = Ext.create('Ext.data.Store', {
        fields: ['id', 'code', 'name'],
        proxy: {
            type: 'ajax',
            extraParams: {
                userId: userId
            },
            url: dataUrl + '/core/user/getUserProductJsonList',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        autoLoad: true,
        listeners: {
            load: function(me, records, options) {
                var productId = records[0].get('id');

                Ext.getCmp('edProduct').setValue(productId);
            }
        }
    });

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

    Ext.Date.patterns={
        CustomFormat: "d.m.Y"
    };

    var creditorStore = Ext.create('Ext.data.Store', {
        id: 'creditorStore',
        model: 'creditorListModel',
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

    var creditorListGrid =  Ext.create('Ext.grid.Panel', {
        store: creditorStore,
        id: 'creditorListGrid',
        multiSelect: true,
        autoScroll: true,
        scroll: 'vertical',
        columns: [{
            text: label_CRED,
            dataIndex: 'name',
            width: 200,
            heigt: 200,
            flex: 1
        }],
        viewConfig: {
            emptyText: 'Нет данных для отображения'
        }
    });

    function executeCrossCheck(){
        var buttonExecute = Ext.create('Ext.button.Button', {
            text: 'Запустить',
            handler: function() {
                var productId = Ext.getCmp('edProduct').getValue();
                var checkedItem = Ext.getCmp('radioClauses').getValue();

                var creditorIds= [];
                if (isNb) {
                    for (var i = 0; i < creditorListGrid.store.getCount(); i++) {
                        if (creditorListGrid.getSelectionModel().isSelected(i)) {
                            creditorIds.push(creditorListGrid.store.getAt(i).data.id);
                        }
                    }
                } else {
                    creditorIds.push(creditorListGrid.store.getAt(0).data.id);
                }

                var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_PROCCESING });
                loadMask.show();
                Ext.Ajax.request({
                    url: dataUrl + '/report/crosscheck/executeCrossCheckAll',
                    timeout: 3000000,
                    params : {
                        userId: userId,
                        creditorIds: creditorIds,
                        productId: productId,
                        executeClause: checkedItem.radiog,
                        reportDate: Ext.Date.format(Ext.getCmp('reportDate').value, Ext.Date.patterns.CustomFormat)
                    },
                    method: 'POST',
                    success: function(response) {
                        loadMask.hide();
                        Ext.getCmp('windowExecute').destroy();
                        viewCrossCheckPanel();
                        crossCheckListGrid = Ext.getCmp('crossCheckListGrid');
                        if (crossCheckListGrid.store.data.length > 0) {
                            crossCheckListGrid.getSelectionModel().select(0);
                            loadCrossCheckMessage();
                        }
                    },
                    failure: function(response) {
                        loadMask.hide();

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
        });

        var buttonClose = Ext.create('Ext.button.Button', {
            text: 'Отмена',
            handler : function() {
                Ext.getCmp('windowExecute').destroy();
            }
        });

        var formExecute = Ext.create('Ext.form.Panel', {
            id: 'formExecute',
            width: 400,
            height: 100,
            fieldDefaults: {
                msgTarget: 'side'
            },
            defaults: {
                anchor: '100%'
            },
            bodyPadding: '5 5 0',
            items: [
                {
                    xtype: 'radiogroup',
                    fieldLabel: 'Условия запуска',
                    id: 'radioClauses',
                    columns: 2,
                    vertical: true,
                    items: [{
                        xtype: 'radio',
                        boxLabel: 'Полная проверка',
                        cls: 'radioButton',
                        name: 'radiog' ,
                        checked: true ,
                        inputValue: 'FULL'
                    } , {
                        xtype: 'radio',
                        boxLabel: 'Частичная проверка',
                        cls: 'radioButton',
                        name: 'radiog',
                        inputValue:'SIMPLE'
                    }]
                }],
            buttons: [buttonExecute, buttonClose]
        });

        var windowProduct = new Ext.Window({
            id: 'windowExecute',
            layout: 'fit',
            title: 'Запуск',
            modal: true,
            maximizable: true,
            items: [formExecute]
        });

        windowProduct.show();


    }

    function viewCrossCheckPanel() {
        var creditorIds= [];
        if (isNb) {
            for (var i = 0; i < creditorListGrid.store.getCount(); i++) {
                if (creditorListGrid.getSelectionModel().isSelected(i)) {
                    creditorIds.push(creditorListGrid.store.getAt(i).data.id);
                }
            }
        } else {
            creditorIds.push(creditorListGrid.store.getAt(0).data.id);
        }

        var crossCheckStore = Ext.create('Ext.data.Store', {
            id: 'crossCheckStore',
            model: 'crossCheckStoreModel',
            autoLoad: true,
            proxy: {
                type: 'ajax',
                url : dataUrl + '/report/crosscheck/getCrossCheck',
                jsonData: true,
                extraParams: {
                    creditorIds: creditorIds,
                    reportDate: Ext.Date.format(Ext.getCmp('reportDate').value, Ext.Date.patterns.CustomFormat),
                    productId: Ext.getCmp('edProduct').getValue()
                },
                actionMethods: {
                    read: 'GET'
                },
                reader: {
                    type: 'json',
                    root: 'data',
                    totalProperty: 'total'
                }
            }
        });


        var crossCheckListGrid =  Ext.create('Ext.grid.Panel', {
            store: crossCheckStore,
            id:'crossCheckListGrid',
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [
                        {
                            xtype: 'button',
                            region: 'west',
                            text: label_EXCEL,
                            handler: function() {
                                var crossCheckListGrid = Ext.getCmp('crossCheckListGrid');
                                var crossCheckId = crossCheckListGrid.getSelectionModel().getLastSelected().data.id;
                                var xhr = new XMLHttpRequest();
                                xhr.open("POST", dataUrl + "/report/crosscheck/exportToExcel?crossCheckId= "+ crossCheckId);
                                xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
                                xhr.responseType = "arraybuffer";
                                xhr.onload = function (oEvent) {
                                    var responseArray = new Uint8Array(this.response);

                                    // извлекаю наименование файла из заголовков
                                    var fileName = label_CROSSCHECK;
                                    // var disposition = xhr.getResponseHeader('Content-Disposition');
                                    // if (disposition && disposition.indexOf('attachment') !== -1) {
                                    //     var fileNameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                                    //     var matches = fileNameRegex.exec(disposition);
                                    //     if (matches != null && matches[1]) {
                                    //         fileName = matches[1].replace(/['"]/g, '');
                                    //     }
                                    // }

                                    var blob = new Blob([responseArray], {type: "application/vnd.ms-excel"});
                                    saveAs(blob, fileName + ".xls");
                                };

                                xhr.send();
                            }
                        }
                    ]

                }
            ],
            columns:[
                {
                    text: label_ORG_NAME,
                    dataIndex: 'creditorName',
                    width: 100,
                    heigt: 200,
                    flex: 1
                },
                {
                    xtype: 'datecolumn',
                    text: label_BEG_DATE,
                    dataIndex: 'dateBegin',
                    width: 100,
                    heigt: 200,
                    flex: 1,
                    format: 'd.m.Y H:i:s'
                },
                {
                    xtype: 'datecolumn',
                    text: label_END_DATE,
                    dataIndex: 'dateEnd',
                    width: 100,
                    heigt: 200,
                    flex: 1,
                    format: 'd.m.Y H:i:s'
                },
                {
                    text: label_STATUS,
                    dataIndex: 'statusName',
                    width: 100,
                    heigt: 200,
                    flex: 1
                },
                {
                    text: label_USER,
                    dataIndex: 'username',
                    width: 100,
                    heigt: 200,
                    flex: 1
                }
            ],
            listeners: {
                selectionchange: function () {
                    loadCrossCheckMessage()
                }
            }
        });
        crossCheckPanel = Ext.getCmp('crossCheckPanel');
        crossCheckPanel.add(crossCheckListGrid);
        Ext.getCmp('crossCheckPanel').show();

    }

    function loadCrossCheckMessage() {
        var crossCheckListGrid = Ext.getCmp('crossCheckListGrid');
        var crossCheckId = crossCheckListGrid.getSelectionModel().getLastSelected().data.id;
        console.log(crossCheckId);
        var crossCheckMessageStore = Ext.create('Ext.data.Store', {
            id: 'crossCheckMessageStore',
            model: 'crossCheckMessageModel',
            autoLoad: true,
            proxy: {
                type: 'ajax',
                url : dataUrl + '/report/crosscheck/getCrossCheckMessages',
                extraParams: {
                    crossCheckId: crossCheckId
                },
                actionMethods: {
                    read: 'GET'
                },
                reader: {
                    type: 'json',
                    root: 'data',
                    totalProperty: 'total'
                }
            }});

        crossCheckMessageListGrid =  Ext.create('Ext.grid.Panel', {
            store: crossCheckMessageStore,
            columns:[
                {
                    text: label_PARAMETR,
                    dataIndex: 'description',
                    flex: 1
                },
                {
                    text: label_VALUE_KR,
                    dataIndex: 'innerValue',
                    flex: 1
                },
                {
                    text: label_OUT_VALUE,
                    dataIndex: 'outerValue',
                    flex: 1
                },
                {
                    text: label_DIFF,
                    dataIndex: 'difference',
                    flex: 1
                }]
        });

        crossCheckMessagePanel = Ext.getCmp('crossCheckMessagePanel');
        crossCheckMessagePanel.removeAll();
        crossCheckMessagePanel.add(crossCheckMessageListGrid);
        Ext.getCmp('crossCheckMessagePanel').show();
    }

    Ext.create('Ext.panel.Panel', {
        renderTo: 'crosscheck-content',
        width: 1200,
        height: 900,
        scroll: 'both',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'panel',
            title: label_ORGS,
            id: 'orgsPanel',
            hidden: true,
            height:50,
            layout: {
                type: 'hbox'
            },
            items:[
                {
                    xtype: 'checkbox',
                    boxLabel: label_VU,
                    id:'cbBankSecondLev',
                    checked: true,
                    cls: 'checkBox',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("creditorListGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(bvuFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(bvuFilter);
                            }
                        }
                    }
                },
                {
                    xtype: 'checkbox',
                    boxLabel: label_PU,
                    id:'cbBankFirstLev',
                    checked:'true',
                    cls: 'checkBox',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("creditorListGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(bpuFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(bpuFilter);
                            }
                        }
                    }
                },
                {
                    xtype: 'checkbox',
                    boxLabel: label_IO,
                    id:'cbIpotekaOrg',
                    cls: 'checkBox',
                    checked:'true',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("creditorListGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(elseorgFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(elseorgFilter);
                            }
                        }
                    }
                },
                {
                    xtype: 'checkbox',
                    boxLabel: label_OTHER,
                    id:'cbOtherOrg',
                    checked:'true',
                    cls: 'checkBox',
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            grid = Ext.getCmp("creditorListGrid");
                            if (newValue == '1') {
                                grid.store.removeFilter(ipotechFilter);
                            } else if (newValue == '0') {
                                grid.store.addFilter(ipotechFilter);
                            }
                        }
                    }
                },
                {
                    xtype: 'button',
                    text: label_SELECT_ALL,
                    id:'btnChooseAll',
                    heigt: 30,
                    handler:   function() {
                        Ext.getCmp('cbBankSecondLev').setValue(true);
                        Ext.getCmp('cbBankFirstLev').setValue(true);
                        Ext.getCmp('cbIpotekaOrg').setValue(true);
                        Ext.getCmp('cbOtherOrg').setValue(true);
                    }
                },
                {
                    xtype: 'button',
                    text: label_OFF_SELECTED,
                    id:'btnDeSelectAll',
                    heigt: 30,
                    handler: function() {
                        Ext.getCmp('cbBankSecondLev').setValue(false);
                        Ext.getCmp('cbBankFirstLev').setValue(false);
                        Ext.getCmp('cbIpotekaOrg').setValue(false);
                        Ext.getCmp('cbOtherOrg').setValue(false);
                    }
                }
            ]
        },
            {
                xtype: 'textfield',
                hidden: true,
                id: 'searchText',
                listeners: {
                    change: function(field, newValue, oldValue, options) {
                        grid = Ext.getCmp("creditorListGrid");
                        if(newValue==''){
                            grid.store.clearFilter();
                            if (Ext.getCmp('cbBankSecondLev').checked == false) {
                                grid.store.addFilter(bvuFilter)
                            };
                            if (Ext.getCmp('cbBankFirstLev').checked == false) {
                                grid.store.addFilter(bpuFilter)
                            };
                            if (Ext.getCmp('cbOtherOrg').checked == false) {
                                grid.store.addFilter(elseorgFilter)
                            };
                            if (Ext.getCmp('cbIpotekaOrg').checked == false) {
                                grid.store.addFilter(ipotechFilter)
                            };

                            grid.getView().refresh();

                        }
                        else {
                            grid.store.filter([
                                {property: "name", value: newValue, anyMatch: true, caseSensitive: false}
                            ]);
                        }
                    }
                }
            },
            // панель кредиторов
            {
                xtype: 'panel',
                id: 'respondentsPanel',
                hidden: true,
                title: '',
                height:200,
                layout: 'fit',
                items: [creditorListGrid]
            },
            // панель управления
            {
                xtype: 'panel',
                title: '',
                height:75,
                items:[
                    {
                        xtype: 'button',
                        hidden: true,
                        text: label_ALL,
                        id:'btnChooseAllCreditor',
                        heigt: 30,
                        width : 200,
                        handler: function(){
                            creditorListGrid.getSelectionModel().selectAll();
                        }
                    },{
                        fieldLabel: 'Выберите продукт',
                        id: 'edProduct',
                        xtype: 'combobox',
                        store: productStore,
                        displayField: 'name',
                        width : 300,
                        labelWidth: 120,
                        valueField: 'id',
                        editable: false
                    },
                    {
                        xtype: 'datefield',
                        id:'reportDate',
                        anchor: '100%',
                        width : 200,
                        format: 'd.m.Y',
                        name: 'from_date',
                        maxValue: new Date()  // limited to the current date or prior
                    }
                ]
            },
            {
                xtype: 'panel',
                title: '',
                height:25,
                layout: {
                    type: 'hbox'
                },
                items:[
                    {
                        xtype: 'button',
                        text: label_SHOW,
                        id:'btnView',
                        heigt: 30,
                        handler: function() {
                            viewCrossCheckPanel();
                            crossCheckListGrid = Ext.getCmp('crossCheckListGrid');
                            if (crossCheckListGrid.store.data.length > 0) {
                                crossCheckListGrid.getSelectionModel().select(0);
                                loadCrossCheckMessage();
                            }
                        }
                    },
                    {
                        xtype: 'button',
                        text: label_CROSSCHECK,
                        id:'btnCrossCheck',
                        heigt: 30,
                        handler: function() {
                            executeCrossCheck();
                        }
                    }
                ]

            },
            {
                xtype: 'panel',
                height: 200,
                id: 'crossCheckPanel',
                hidden: true,
                layout: 'fit',
                title: label_START

            },
            {
                xtype: 'panel',
                height: 200,
                id: 'crossCheckMessagePanel',
                hidden: true,
                layout: 'fit',
                title: label_DATA
            }
        ],
        listeners: {
            afterrender: function () {
                creditorStore.load({
                    params: {
                        userId: userId
                    },
                    scope: this
                });
                if (isNb) {
                    Ext.getCmp("orgsPanel").show();
                    Ext.getCmp("searchText").show();
                    Ext.getCmp("respondentsPanel").show();
                    Ext.getCmp("btnChooseAllCreditor").show();
                }
            }
        }
    });
});