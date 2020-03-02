Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*'

]);

Ext.onReady(function () {
    var periodName;

    Ext.define('Respondent', {
        extend: 'Ext.data.Model',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType']
    });

    Ext.define('User', {
        extend: 'Ext.data.Model',
        fields: ['id', 'userId', 'screenName', 'emailAddress', 'firstName', 'lastName', 'middleName', 'modifiedDate', 'isActive', 'respondents', 'isNb']
    });

    function createStoreCloneToAnother(sourceStore, targetStore) {
        var records = [];
        Ext.each(sourceStore.getRange(), function (record) {
            records.push(record.copy());
        });
        targetStore.add(records);
    }

    function createStoreClone(sourceStore) {
        var targetStore = Ext.create('Ext.data.Store', {
            model: sourceStore.model
        });
        var records = [];
        Ext.each(sourceStore.getRange(), function (record) {
            records.push(record.copy());
        });
        targetStore.add(records);
        return targetStore;
    }

    var leftRespondentsStore = Ext.create('Ext.data.Store', {
        id: 'leftRespondentsStore',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getRespondentJsonList',
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
            load: function (me, records, options) {
                rightRespondentsStore.load();
            }
        },
        sorters: [{
            property: 'name',
            direction: 'asc'
        }]
    });

    var rightRespondentsStore = Ext.create('Ext.data.Store', {
        id: 'rightRespondentsStore',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: false,
        listeners: {
            load: function (me, records, options) {
                createStoreCloneToAnother(leftRespondentsStore, rightRespondentsStore);
                Ext.Ajax.request({
                    url: dataUrl + '/utils/config/getDigitalSigningOrgIds',
                    method: 'GET',
                    reader: {
                        type: 'json',
                        root: 'data'
                    },
                    success: function (response) {
                        var signingRespondentIds = JSON.parse(response.responseText);
                        var records = [];
                        var storeItems = rightRespondentsStore.getRange();
                        for (var i = 0; i < storeItems.length; i++) {
                            if (Ext.Array.contains(signingRespondentIds, storeItems[i].get('id'))) {
                                leftRespondentsStore.remove(leftRespondentsStore.getById(storeItems[i].get('id')));
                                records.push(rightRespondentsStore.getById(storeItems[i].get('id')));
                            }
                        }
                        rightRespondentsStore.removeAll(true);
                        Ext.getCmp('rightRespondentsGrid').getView().refresh();
                        Ext.getCmp('rightRespondentsGrid').store.add(records);
                    },
                    failure: function (response) {
                        var error = JSON.parse(response.responseText);

                        Ext.Msg.show({
                            title: label_ERROR,
                            msg: error.message,
                            width: 300,
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

    var respondentStore = Ext.create('Ext.data.Store', {
        id: 'respondentStore',
        model: 'Respondent',
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getRespondentJsonList',
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
            load: function (me, records, options) {
                var userId = Ext.getCmp('userGrid').getSelectionModel().getLastSelected().data.userId;
                avlRespondentsGrid.reconfigure(createStoreClone(respondentStore));
                avlRespondentsGrid.getView().refresh();
                selRespondentsGrid.store.load({
                    params: {
                        userId: userId
                    },
                    scope: this
                });
                selRespondentsGrid.getView().refresh();
            }
        },
        sorters: [{
            property: 'name',
            direction: 'asc'
        }]
    });

    var userStore = Ext.create('Ext.data.Store', {
        id: 'userStore',
        model: 'User',
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/user/getUserList',
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
            property: 'emailAddress',
            direction: 'asc'
        }]
    });

    userStore.on('load', function (s) {
        if (userGrid.store.getCount() > 0) {
            Ext.getCmp('userGrid').getSelectionModel().select(0);
        }
        Ext.getCmp('userProducts').getStore().load();
    });

    var selStore = Ext.create('Ext.data.Store', {
        id: 'selStore',
        model: 'Respondent',
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getUserRespondentList',
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

    selStore.on('load', function (s) {
        if (selRespondentsGrid.store.getCount() > 0) {
            var records = selRespondentsGrid.store.snapshot || selRespondentsGrid.store.data;
            for (var i = 0; i < records.items.length; i++) {
                var founded = avlRespondentsGrid.store.findRecord('name', records.items[i].data.name)
                if (founded != null) {
                    avlRespondentsGrid.store.remove(founded);
                }
            }
            avlRespondentsGrid.getView().refresh();
        }
    });

    var productsStore = Ext.create('Ext.data.Store', {
        id: 'productsStore',
        fields: ['id', 'code', 'name'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getProductList',
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
            property: 'id',
            direction: 'asc'
        }],
        listeners: {
            load: function (me, records, options) {
                if (records.length > 0) {
                    Ext.getCmp('userProducts').setValue(records[0].get('id'));
                }
            }
        }
    });

    var allProductsStore = Ext.create('Ext.data.Store', {
        id: 'allProductsStore',
        fields: ['id', 'code', 'name'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getProductList',
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
            load: function (me, records, options) {
                var userId = Ext.getCmp('userGrid').getSelectionModel().getLastSelected().data.userId;
                Ext.getCmp('userAllProductsGrid').reconfigure(createStoreClone(allProductsStore));
                Ext.getCmp('userAllProductsGrid').getView().refresh();
                Ext.getCmp('userProductsGrid').store.load({
                    params: {
                        userId: userId
                    },
                    scope: this
                });
                Ext.getCmp('userProductsGrid').getView().refresh();
            }
        },
        sorters: [{
            property: 'id',
            direction: 'asc'
        }]
    });

    var userProductsStore = Ext.create('Ext.data.Store', {
        id: 'userProductsStore',
        fields: ['id', 'code', 'name'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/user/getUserProductJsonList',
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
            property: 'id',
            direction: 'asc'
        }]
    });

    userProductsStore.on('load', function (s) {
        if (Ext.getCmp('userProductsGrid').store.getCount() > 0) {
            var records = Ext.getCmp('userProductsGrid').store.snapshot || Ext.getCmp('userProductsGrid').store.data;
            for (var i = 0; i < records.items.length; i++) {
                var founded = Ext.getCmp('userAllProductsGrid').store.findRecord('name', records.items[i].data.name)
                if (founded != null) {
                    Ext.getCmp('userAllProductsGrid').store.remove(founded);
                }
            }
            Ext.getCmp('userAllProductsGrid').getView().refresh();
        }
    });

    var subjectProductsStore = Ext.create('Ext.data.Store', {
        id: 'subjectProductsStore',
        fields: ['id', 'code', 'name'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getProductList',
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
            property: 'id',
            direction: 'asc'
        }],
        listeners: {
            load: function (me, records, options) {
                if (records.length > 0) {
                    Ext.getCmp('subjectProducts').setValue(records[0].get('id'));
                }
            }
        }
    });

    var positionsStore = Ext.create('Ext.data.Store', {
        id: 'positionsStore',
        fields: ['id', 'nameRu', 'nameKz', 'shortNameRu', 'shortNameKz', 'level'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/position/getPositionList',
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
            property: 'id',
            direction: 'asc'
        }]
    });

    var userPositionsStore = Ext.create('Ext.data.Store', {
        id: 'userPositionsStore',
        fields: ['id', 'nameRu', 'nameKz', 'shortNameRu', 'shortNameKz', 'level'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/position/getUserPositionListByProduct',
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
            property: 'id',
            direction: 'asc'
        }]
    });

    var subjectTypeStore = Ext.create('Ext.data.Store', {
        id: 'subjectTypeStore',
        fields: ['id', 'code', 'nameRu', 'nameKz'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/subject/getSubjectTypeList',
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
            property: 'nameRu',
            direction: 'asc'
        }]
    });

    var periodTypeStore = Ext.create('Ext.data.Store', {
        id: 'periodTypeStore',
        fields: ['id', 'type', 'code', 'nameRu', 'nameKz'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/utils/text/getTextListByType',
            method: 'GET',
            extraParams: {
                types: ['PERIOD_TYPE']
            },
            reader: {
                type: 'json',
                root: ''
            }
        },
        sorters: [{
            property: 'id',
            direction: 'asc'
        }],
        listeners: {
            load: function (me, records, options) {
                if (records.length > 0)
                    Ext.getCmp('periodTypeCombo').setValue(records[0].get('id'));
            }
        }
    });

    var avlRespondentsGrid = Ext.create('Ext.grid.Panel', {
        id: 'avlRespondentsGrid',
        multiSelect: true,
        listeners: {
            deselect: function (grid, record) {
                Ext.getCmp('DeleteButton').setDisabled(true);
            },
            select: function (grid, record) {
                Ext.getCmp('DeleteButton').setDisabled(true);
                Ext.getCmp('AddButton').setDisabled(false);
            }
        },
        columns: [{
            dataIndex: 'name',
            flex: 1
        }]
    });

    var selRespondentsGrid = Ext.create('Ext.grid.Panel', {
        id: 'selRespondentsGrid',
        multiSelect: true,
        store: selStore,
        listeners: {
            deselect: function (grid, record) {
                Ext.getCmp('AddButton').setDisabled(true);
            },
            select: function (grid, record) {
                Ext.getCmp('AddButton').setDisabled(true);
                Ext.getCmp('DeleteButton').setDisabled(false);
            }
        },
        columns: [{
            dataIndex: 'name',
            flex: 1

        }],

    });

    var userGrid = Ext.create('Ext.grid.Panel', {
        id: 'userGrid',
        store: userStore,
        hideHeaders: true,
        columns: [{
            dataIndex: 'emailAddress',
            flex: 1
        }],
        listeners: {
            cellclick: function (grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts) {
                avlRespondentsGrid.reconfigure(createStoreClone(respondentStore));
                avlRespondentsGrid.getView().refresh();
                var userId = newValue.data.userId;
                selRespondentsGrid.store.load({
                    params: {
                        userId: userId
                    },
                    scope: this
                });
                selRespondentsGrid.getView().refresh();

                if (Ext.getCmp('userProducts').getValue() != '') {
                    var positions = [];
                    positionsStore.load({
                        callback: function () {
                            for (var i = 0; i < Ext.getCmp('positionsGrid').getStore().getRange().length; i++) {
                                positions.push(Ext.getCmp('positionsGrid').getStore().getRange()[i].get('id'));
                            }
                            Ext.getCmp('positionsGrid').getView().refresh();
                            var productId = Ext.getCmp('userProducts').getValue();
                            userPositionsStore.load({
                                params: {
                                    userId: userId,
                                    productId: productId
                                },
                                callback: function () {
                                    var userPositions = userPositionsStore.getRange();
                                    for (var i = 0; i < userPositions.length; i++) {
                                        if (Ext.Array.contains(positions, userPositions[i].get('id'))) {
                                            positionsStore.remove(positionsStore.getById(userPositions[i].get('id')));
                                        }
                                    }
                                    Ext.getCmp('positionsGrid').getView().refresh();
                                }
                            });
                            Ext.getCmp('userPositionsGrid').getView().refresh();
                            Ext.getCmp('positionsGrid').getView().refresh();
                        }
                    });
                } else {
                    Ext.Msg.alert('', 'Выберите продукт!');
                }

                Ext.getCmp('userAllProductsGrid').reconfigure(createStoreClone(allProductsStore));
                Ext.getCmp('userAllProductsGrid').getView().refresh();
                Ext.getCmp('userProductsGrid').store.load({
                    params: {
                        userId: userId
                    },
                    scope: this
                });
                Ext.getCmp('userProductsGrid').getView().refresh();
            }
        },
        tbar: [{
            xtype: 'textfield',
            padding: 3,
            width: 350,
            listeners: {
                change: function (field, newValue, oldValue, options) {
                    grid = Ext.getCmp("userGrid");
                    if (newValue == '') {
                        grid.store.clearFilter();
                        grid.getView().refresh();
                    } else {
                        grid.store.filter([{
                            property: "emailAddress",
                            value: newValue,
                            anyMatch: true,
                            caseSensitive: false
                        }]);
                    }
                }
            }
        }, {
            xtype: 'tbseparator',
        }]
    });

    var subjectTypeGrid = Ext.create('Ext.grid.Panel', {
        id: 'subjectTypeGrid',
        store: subjectTypeStore,
        columns: [{
            dataIndex: 'nameRu',
            flex: 1
        }],
        listeners: {
            viewready: function (thisGrid) {
                Ext.getCmp('subjectTypeGrid').getSelectionModel().select(0);
                Ext.getCmp('subjectProducts').getStore().load();
            },
            cellclick: function () {
                if (Ext.getCmp('subjectProducts').getValue() != '') {
                    Ext.Ajax.request({
                        url: dataUrl + '/core/subject/getSubjectTypeProductPeriod',
                        method: 'GET',
                        params: {
                            subjectTypeId: subjectTypeGrid.getSelectionModel().getLastSelected().data.id,
                            productId: Ext.getCmp('subjectProducts').getValue()
                        },
                        reader: {
                            type: 'json',
                            root: 'data'
                        },
                        success: function (response) {
                            periodName = response.responseText;
                            Ext.getCmp('periodTypeCombo').setValue(periodName);
                        },
                        failure: function (response) {
                            var error = JSON.parse(response.responseText);

                            Ext.Msg.show({
                                title: 'Ошибка',
                                msg: error.errorMessage,
                                width: 300,
                                buttons: Ext.MessageBox.YES
                            });

                            Ext.getCmp('periodTypeCombo').setValue("Регулярный");
                            periodName = "";
                        }
                    });
                } else {
                    Ext.Msg.alert('', label_CHOOSE_PRODUCT);
                }
            }
        }
    });

    var panel = Ext.create('Ext.tab.Panel', {
        height: 700,
        width: 1200,
        renderTo: 'admin-content',
        title: label_TITLE,
        padding: 10,
        id: 'MainTabPanel',
        items: [{
            xtype: 'panel',
            title: label_SETUP_USERS,
            width: 1100,
            height: 600,
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'panel',
                title: label_USERS,
                width: 200,
                split: true,
                autoScroll: true,
                flex: 1,
                items: [{
                    xtype: 'button',
                    padding: 3,
                    id: 'sync',
                    flex: 1,
                    text: label_SYNC_USERS,
                    listeners: {
                        click: function () {
                            Ext.Ajax.request({
                                url: dataUrlforUsers,
                                method: 'POST',
                                params: {
                                    op: 'SYNCUSERS'
                                },
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function () {
                                    Ext.Msg.alert(label_SYNC);
                                },
                                failure: function (response) {
                                    var error = JSON.parse(response.responseText);

                                    Ext.Msg.show({
                                        title: label_ERROR,
                                        msg: error.message,
                                        width: 300,
                                        buttons: Ext.MessageBox.YES
                                    });
                                }
                            });
                        }
                    }
                },
                    userGrid
                ]
            }, {
                xtype: 'tabpanel',
                width: 800,
                items: [{
                    xtype: 'panel',
                    title: label_SETUP_RESP,
                    width: 790,
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [{
                        xtype: 'panel',
                        title: label_APPOINTED_RESP,
                        autoScroll: true,
                        height: 300,
                        items: [avlRespondentsGrid]
                    }, {
                        xtype: 'panel',
                        height: 24,
                        layout: {
                            align: 'middle',
                            pack: 'center',
                            type: 'hbox'
                        },
                        items: [{
                            xtype: 'button',
                            id: 'AddButton',
                            flex: 1,
                            icon: contextPathUrl + '/icons/down_1.png',
                            text: label_ADD,
                            listeners: {
                                click: function (grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts) {
                                    var credIds = [];
                                    //TODO: временно убрал , пока в таблице нет значения isNb
                                    //var isNb = userGrid.getSelectionModel().getLastSelected().data.isNb;
                                    for (var i = 0; i < avlRespondentsGrid.store.getCount(); i++) {
                                        if (avlRespondentsGrid.getSelectionModel().isSelected(i)) {
                                            credIds.push(avlRespondentsGrid.store.getAt(i).data.id);
                                        }
                                    }
                                    var isNb = false;
                                    var portalUsers = Ext.decode(users);
                                    Ext.Array.each(portalUsers[userGrid.getSelectionModel().getLastSelected().data.userId], function (rec) {
                                        if (rec === 'NationalBankEmployee' || rec === 'Administrator') {
                                            isNb = true;
                                        }
                                    });

                                    if (isNb === false && credIds.length >= 1 && selStore.data.length > 0) {
                                        Ext.Msg.alert(label_ERROR, label_IMPOSSIBLE_ADDING);
                                    } else {
                                        Ext.Ajax.request({
                                            url: dataUrl + '/core/respondent/addUserRespondent',
                                            method: 'PUT',
                                            params: {
                                                userId: userGrid.getSelectionModel().getLastSelected().data.userId,
                                                respondentIds: credIds
                                            },
                                            reader: {
                                                type: 'json',
                                                root: 'data'
                                            },
                                            success: function () {
                                                if (Ext.getCmp('avlRespondentsGrid').getSelectionModel().hasSelection()) {
                                                    var records = Ext.getCmp('avlRespondentsGrid').getSelectionModel().getSelection();
                                                    selStore.add(records);
                                                    avlRespondentsGrid.store.remove(records);
                                                }
                                                selRespondentsGrid.getView().refresh();
                                                avlRespondentsGrid.getView().refresh();
                                            },
                                            failure: function (response) {
                                                var error = JSON.parse(response.responseText);

                                                Ext.Msg.show({
                                                    title: label_ERROR,
                                                    msg: error.message,
                                                    width: 300,
                                                    buttons: Ext.MessageBox.YES
                                                });
                                            }
                                        });
                                        selRespondentsGrid.getView().refresh();
                                    }
                                }
                            }
                        }, {
                            xtype: 'button',
                            id: 'DeleteButton',
                            flex: 1,
                            icon: contextPathUrl + '/icons/up_1.png',
                            text: label_DELETE,
                            listeners: {
                                click: function () {
                                    if (Ext.getCmp('selRespondentsGrid').getSelectionModel().hasSelection()) {
                                        var credIds = [];
                                        for (var i = 0; i < selRespondentsGrid.store.getCount(); i++) {
                                            if (selRespondentsGrid.getSelectionModel().isSelected(i)) {
                                                credIds.push(selRespondentsGrid.store.getAt(i).data.id);
                                            }
                                        }
                                        Ext.Ajax.request({
                                            url: dataUrl + '/core/respondent/delUserRespondent',
                                            method: 'POST',
                                            params: {
                                                userId: userGrid.getSelectionModel().getLastSelected().data.userId,
                                                respondentIds: credIds
                                            },
                                            reader: {
                                                type: 'json',
                                                root: 'data'
                                            },
                                            success: function () {
                                                if (Ext.getCmp('selRespondentsGrid').getSelectionModel().hasSelection()) {
                                                    var records = Ext.getCmp('selRespondentsGrid').getSelectionModel().getSelection();
                                                    selStore.remove(records);
                                                    avlRespondentsGrid.store.add(records);
                                                }
                                                avlRespondentsGrid.store.sort('name', 'ASC');
                                                selRespondentsGrid.getView().refresh();
                                                avlRespondentsGrid.getView().refresh();
                                            },
                                            failure: function (response) {
                                                var error = JSON.parse(response.responseText);

                                                Ext.Msg.show({
                                                    title: label_ERROR,
                                                    msg: error.message,
                                                    width: 300,
                                                    buttons: Ext.MessageBox.YES
                                                });
                                            }
                                        })

                                    };
                                    selRespondentsGrid.getView().refresh();
                                }
                            }
                        }]
                    }, {
                        xtype: 'panel',
                        title: label_AVAILABLE_RESP,
                        autoScroll: true,
                        height: 280,
                        items: [selRespondentsGrid]
                    }]
                }, {
                    xtype: 'panel',
                    title: label_SETUP_POST,
                    autoScroll: true,
                    width: 800,
                    height: 600,
                    layout: {
                        type: 'vbox',
                        align: 'center'
                    },
                    items: [{
                        xtype: 'combobox',
                        id: 'userProducts',
                        store: productsStore,
                        margin: '10 0 0 0',
                        width: 450,
                        editable: false,
                        valueField: 'id',
                        displayField: 'name',
                        labelWidth: 130,
                        fieldLabel: label_SELECTED_PROD,
                        labelAlign: 'left',
                        labelStyle: 'font-weight: bold; text-align:center;',
                        listeners: {
                            change: function () {
                                if (userGrid.getSelectionModel().hasSelection()) {
                                    var positions = [];
                                    positionsStore.load({
                                        callback: function () {
                                            for (var i = 0; i < Ext.getCmp('positionsGrid').getStore().getRange().length; i++) {
                                                positions.push(Ext.getCmp('positionsGrid').getStore().getRange()[i].get('id'));
                                            }
                                            Ext.getCmp('positionsGrid').getView().refresh();
                                            var productId = Ext.getCmp('userProducts').getValue();
                                            var userId = userGrid.getSelectionModel().getLastSelected().data.userId;
                                            userPositionsStore.load({
                                                params: {
                                                    userId: userId,
                                                    productId: productId
                                                },
                                                callback: function () {
                                                    var userPositions = userPositionsStore.getRange();
                                                    for (var i = 0; i < userPositions.length; i++) {
                                                        if (Ext.Array.contains(positions, userPositions[i].get('id'))) {
                                                            positionsStore.remove(positionsStore.getById(userPositions[i].get('id')));
                                                        }
                                                    }
                                                    Ext.getCmp('positionsGrid').getView().refresh();
                                                }
                                            });
                                            Ext.getCmp('userPositionsGrid').getView().refresh();
                                            Ext.getCmp('positionsGrid').getView().refresh();
                                        }
                                    });
                                } else {
                                    Ext.Msg.alert('', label_CHOOSE_USER);
                                }
                            }
                        }
                    }, {
                        xtype: 'label',
                        margin: '10 0 0 0',
                        style: 'font-weight: bold;',
                        text: label_CHOOSE_POST
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
                            width: '45%',
                            items: [{
                                xtype: 'gridpanel',
                                id: 'positionsGrid',
                                store: positionsStore,
                                multiSelect: true,
                                height: 176,
                                padding: 5,
                                autoScroll: true,
                                title: label_AVAILABLE_POST,
                                hideHeaders: true,
                                scroll: 'vertical',
                                columns: [{
                                    xtype: 'gridcolumn',
                                    width: '100%',
                                    dataIndex: 'nameRu',
                                    text: ''
                                }],
                                viewConfig: {
                                    autoScroll: false
                                }
                            }]
                        }, {
                            xtype: 'container',
                            width: '10%',
                            layout: {
                                type: 'vbox',
                                align: 'center',
                                pack: 'center'
                            },
                            items: [{
                                xtype: 'button',
                                margin: '0 0 20 0',
                                text: label_ADD,
                                listeners: {
                                    click: function () {
                                        var posIds = [];
                                        for (var i = 0; i < Ext.getCmp('positionsGrid').store.getCount(); i++) {
                                            if (Ext.getCmp('positionsGrid').getSelectionModel().isSelected(i)) {
                                                posIds.push(Ext.getCmp('positionsGrid').store.getAt(i).data.id);
                                            }

                                        }
                                        Ext.Ajax.request({
                                            url: dataUrl + '/core/position/addUserPosition',
                                            method: 'PUT',
                                            params: {
                                                userId: userGrid.getSelectionModel().getLastSelected().data.userId,
                                                productId: Ext.getCmp('userProducts').getValue(),
                                                positionIds: posIds
                                            },
                                            reader: {
                                                type: 'json',
                                                root: 'data'
                                            },
                                            success: function () {
                                                if (Ext.getCmp('positionsGrid').getSelectionModel().hasSelection()) {
                                                    var records = Ext.getCmp('positionsGrid').getSelectionModel().getSelection();
                                                    Ext.getCmp('positionsGrid').getStore().remove(records);
                                                    Ext.getCmp('userPositionsGrid').getStore().add(records);
                                                }
                                                Ext.getCmp('positionsGrid').getView().refresh();
                                                Ext.getCmp('userPositionsGrid').getView().refresh();
                                            },
                                            failure: function (response) {
                                                var error = JSON.parse(response.responseText);

                                                Ext.Msg.show({
                                                    title: label_ERROR,
                                                    msg: error.message,
                                                    width: 300,
                                                    buttons: Ext.MessageBox.YES
                                                });
                                            }
                                        });
                                        Ext.getCmp('userPositionsGrid').getView().refresh();
                                    }
                                }
                            }, {
                                xtype: 'button',
                                text: label_DELETE,
                                listeners: {
                                    click: function () {
                                        if (Ext.getCmp('userPositionsGrid').getSelectionModel().hasSelection()) {
                                            var posIds = [];
                                            for (var i = 0; i < Ext.getCmp('userPositionsGrid').store.getCount(); i++) {
                                                if (Ext.getCmp('userPositionsGrid').getSelectionModel().isSelected(i)) {
                                                    posIds.push(Ext.getCmp('userPositionsGrid').store.getAt(i).data.id);
                                                }
                                            }
                                            Ext.Ajax.request({
                                                url: dataUrl + '/core/position/delUserPosition',
                                                method: 'POST',
                                                params: {
                                                    userId: userGrid.getSelectionModel().getLastSelected().data.userId,
                                                    productId: Ext.getCmp('userProducts').getValue(),
                                                    positionIds: posIds
                                                },
                                                reader: {
                                                    type: 'json',
                                                    root: 'data'
                                                },
                                                success: function () {
                                                    if (Ext.getCmp('userPositionsGrid').getSelectionModel().hasSelection()) {
                                                        var records = Ext.getCmp('userPositionsGrid').getSelectionModel().getSelection();
                                                        Ext.getCmp('userPositionsGrid').getStore().remove(records);
                                                        Ext.getCmp('positionsGrid').getStore().add(records);
                                                    }
                                                    Ext.getCmp('positionsGrid').getView().refresh();
                                                    Ext.getCmp('userPositionsGrid').getView().refresh();
                                                },
                                                failure: function (response) {
                                                    var error = JSON.parse(response.responseText);

                                                    Ext.Msg.show({
                                                        title: label_ERROR,
                                                        msg: error.message,
                                                        width: 300,
                                                        buttons: Ext.MessageBox.YES
                                                    });
                                                }
                                            })

                                        };
                                        Ext.getCmp('userPositionsGrid').getView().refresh();
                                    }
                                }
                            }]
                        }, {
                            xtype: 'container',
                            width: '45%',
                            items: [{
                                xtype: 'gridpanel',
                                id: 'userPositionsGrid',
                                store: userPositionsStore,
                                multiSelect: true,
                                height: 176,
                                padding: 5,
                                autoScroll: true,
                                title: label_APPOINTED_POST,
                                hideHeaders: true,
                                scroll: 'vertical',
                                columns: [{
                                    xtype: 'gridcolumn',
                                    width: '100%',
                                    dataIndex: 'nameRu',
                                    text: ''
                                }],
                                viewConfig: {
                                    autoScroll: false
                                }
                            }]
                        }]
                    }]
                }, {
                    xtype: 'panel',
                    title: label_SETUP_PRODUCTS,
                    autoScroll: true,
                    width: 800,
                    height: 600,
                    layout: {
                        type: 'vbox',
                        align: 'center'
                    },
                    items: [{
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
                            width: '45%',
                            items: [{
                                xtype: 'gridpanel',
                                id: 'userAllProductsGrid',
                                multiSelect: true,
                                height: 176,
                                padding: 5,
                                autoScroll: true,
                                title: label_AVAILABLE_PRODUCTS,
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
                            width: '10%',
                            layout: {
                                type: 'vbox',
                                align: 'center',
                                pack: 'center'
                            },
                            items: [{
                                xtype: 'button',
                                margin: '0 0 20 0',
                                text: label_ADD,
                                listeners: {
                                    click: function () {
                                        var productIds = [];
                                        for (var i = 0; i < Ext.getCmp('userAllProductsGrid').store.getCount(); i++) {
                                            if (Ext.getCmp('userAllProductsGrid').getSelectionModel().isSelected(i)) {
                                                productIds.push(Ext.getCmp('userAllProductsGrid').store.getAt(i).data.id);
                                            }

                                        }
                                        Ext.Ajax.request({
                                            url: dataUrl + '/core/user/addUserProduct',
                                            method: 'PUT',
                                            params: {
                                                userId: userGrid.getSelectionModel().getLastSelected().data.userId,
                                                productIds: productIds
                                            },
                                            reader: {
                                                type: 'json',
                                                root: 'data'
                                            },
                                            success: function () {
                                                if (Ext.getCmp('userAllProductsGrid').getSelectionModel().hasSelection()) {
                                                    var records = Ext.getCmp('userAllProductsGrid').getSelectionModel().getSelection();
                                                    Ext.getCmp('userAllProductsGrid').getStore().remove(records);
                                                    Ext.getCmp('userProductsGrid').getStore().add(records);
                                                }
                                                Ext.getCmp('userAllProductsGrid').getView().refresh();
                                                Ext.getCmp('userProductsGrid').getView().refresh();
                                            },
                                            failure: function (response) {
                                                var error = JSON.parse(response.responseText);

                                                Ext.Msg.show({
                                                    title: label_ERROR,
                                                    msg: error.message,
                                                    width: 300,
                                                    buttons: Ext.MessageBox.YES
                                                });
                                            }
                                        });
                                        Ext.getCmp('userProductsGrid').getView().refresh();
                                    }
                                }
                            }, {
                                xtype: 'button',
                                text: label_DELETE,
                                listeners: {
                                    click: function () {
                                        if (Ext.getCmp('userProductsGrid').getSelectionModel().hasSelection()) {
                                            var productIds = [];
                                            for (var i = 0; i < Ext.getCmp('userProductsGrid').store.getCount(); i++) {
                                                if (Ext.getCmp('userProductsGrid').getSelectionModel().isSelected(i)) {
                                                    productIds.push(Ext.getCmp('userProductsGrid').store.getAt(i).data.id);
                                                }
                                            }
                                            Ext.Ajax.request({
                                                url: dataUrl + '/core/user/delUserProduct',
                                                method: 'POST',
                                                params: {
                                                    userId: userGrid.getSelectionModel().getLastSelected().data.userId,
                                                    productIds: productIds
                                                },
                                                reader: {
                                                    type: 'json',
                                                    root: 'data'
                                                },
                                                success: function () {
                                                    if (Ext.getCmp('userProductsGrid').getSelectionModel().hasSelection()) {
                                                        var records = Ext.getCmp('userProductsGrid').getSelectionModel().getSelection();
                                                        Ext.getCmp('userProductsGrid').getStore().remove(records);
                                                        Ext.getCmp('userAllProductsGrid').getStore().add(records);
                                                    }
                                                    Ext.getCmp('userAllProductsGrid').getView().refresh();
                                                    Ext.getCmp('userProductsGrid').getView().refresh();
                                                },
                                                failure: function (response) {
                                                    var error = JSON.parse(response.responseText);

                                                    Ext.Msg.show({
                                                        title: label_ERROR,
                                                        msg: error.message,
                                                        width: 300,
                                                        buttons: Ext.MessageBox.YES
                                                    });
                                                }
                                            })

                                        };
                                        Ext.getCmp('userProductsGrid').getView().refresh();
                                    }
                                }
                            }]
                        }, {
                            xtype: 'container',
                            width: '45%',
                            items: [{
                                xtype: 'gridpanel',
                                id: 'userProductsGrid',
                                store: userProductsStore,
                                multiSelect: true,
                                height: 176,
                                padding: 5,
                                autoScroll: true,
                                title: label_APPOINTED_PRODUCTS,
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
                    }]
                }]
            }]
        }, {
            xtype: 'panel',
            title: label_SETUP_PERIODICITY,
            height: 600,
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'panel',
                title: label_SUBJECTS,
                width: 200,
                split: true,
                autoScroll: true,
                flex: 1,
                items: [
                    subjectTypeGrid
                ]
            }, {
                xtype: 'panel',
                title: label_PERIODICITY,
                autoScroll: true,
                width: 800,
                height: 600,
                layout: {
                    type: 'vbox',
                    align: 'center'
                },
                items: [{
                    xtype: 'combobox',
                    id: 'subjectProducts',
                    store: subjectProductsStore,
                    margin: '20 0 0 0',
                    width: 450,
                    editable: false,
                    valueField: 'id',
                    displayField: 'name',
                    labelWidth: 130,
                    fieldLabel: label_SELECTED_PROD,
                    labelAlign: 'left',
                    labelStyle: 'font-weight: bold; text-align:center;',
                    listeners: {
                        change: function () {
                            if (subjectTypeGrid.getSelectionModel().hasSelection()) {
                                Ext.Ajax.request({
                                    url: dataUrl + '/core/subject/getSubjectTypeProductPeriod',
                                    method: 'GET',
                                    params: {
                                        subjectTypeId: subjectTypeGrid.getSelectionModel().getLastSelected().data.id,
                                        productId: Ext.getCmp('subjectProducts').getValue()
                                    },
                                    reader: {
                                        type: 'json',
                                        root: 'data'
                                    },
                                    success: function (response) {
                                        periodName = response.responseText;
                                        Ext.getCmp('periodTypeCombo').setValue(periodName);
                                    },
                                    failure: function (response) {
                                        var error = JSON.parse(response.responseText);

                                        Ext.Msg.show({
                                            title: label_ERROR,
                                            msg: error.errorMessage,
                                            width: 300,
                                            buttons: Ext.MessageBox.YES
                                        });

                                        Ext.getCmp('periodTypeCombo').setValue("Регулярный");
                                        periodName = "";
                                    }
                                });
                            } else {
                                Ext.Msg.alert('', label_CHOOSE_SUBJECT);
                            }
                        }
                    }
                }, {
                    xtype: 'combobox',
                    id: 'periodTypeCombo',
                    store: periodTypeStore,
                    margin: '20 0 0 0',
                    width: 450,
                    editable: false,
                    valueField: 'id',
                    displayField: 'nameRu',
                    labelWidth: 200,
                    fieldLabel: label_PERIOD,
                    labelAlign: 'left',
                    labelStyle: 'font-weight: bold; text-align:center;',
                    listeners: {
                        change: function () {
                            var record = periodTypeStore.findRecord('id', Ext.getCmp('periodTypeCombo').getValue());

                            if (record !== null && record.get('nameRu') !== periodName) {
                                Ext.getCmp('btnSavePeriod').setDisabled(false);
                            } else {
                                Ext.getCmp('btnSavePeriod').setDisabled(true);
                            }
                        }
                    }
                }, {
                    xtype: 'button',
                    id: 'btnSavePeriod',
                    margin: '20 0 20 0',
                    text: label_SAVE,
                    enabled: false,
                    listeners: {
                        click: function () {
                            if (subjectTypeGrid.getSelectionModel().hasSelection()) {
                                Ext.Ajax.request({
                                    url: dataUrl + '/core/subject/updateSubjectType',
                                    method: 'POST',
                                    params: {
                                        subjectTypeId: subjectTypeGrid.getSelectionModel().getLastSelected().data.id,
                                        productId: Ext.getCmp('subjectProducts').getValue(),
                                        periodId: Ext.getCmp('periodTypeCombo').getValue()
                                    },
                                    reader: {
                                        type: 'json',
                                        root: 'data'
                                    },
                                    success: function () {
                                        Ext.Msg.alert('', label_SUCCESS_UPDATE);
                                        Ext.getCmp('btnSavePeriod').setDisabled(true);
                                        var record = periodTypeStore.findRecord('id', Ext.getCmp('periodTypeCombo').getValue());
                                        periodName = record.get('nameRu');
                                    },
                                    failure: function (response) {
                                        var error = JSON.parse(response.responseText);

                                        Ext.Msg.show({
                                            title: label_ERROR,
                                            msg: error.errorMessage,
                                            width: 300,
                                            buttons: Ext.MessageBox.YES
                                        });
                                    }
                                });
                                Ext.getCmp('userPositionsGrid').getView().refresh();
                            } else {
                                Ext.Msg.alert('', label_CHOOSE_SUBJECT);
                            }
                        }
                    }
                }]
            }]
        }, {
            xtype: 'panel',
            title: label_SETUP_EDS,
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
                    xtype: 'label',
                    margin: '10 0 0 0',
                    style: 'font-weight: bold;',
                    text: label_REQUIRED_EDS
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
                            id: 'leftRespondentsGrid',
                            store: leftRespondentsStore,
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
                                    if (Ext.getCmp('leftRespondentsGrid').getSelectionModel().hasSelection()) {
                                        var records = Ext.getCmp('leftRespondentsGrid').getSelectionModel().getSelection();
                                        leftRespondentsStore.remove(records);
                                        rightRespondentsStore.add(records)
                                    }
                                }
                            }
                        }, {
                            xtype: 'button',
                            text: '<<',
                            listeners: {
                                click: function () {
                                    if (Ext.getCmp('rightRespondentsGrid').getSelectionModel().hasSelection()) {
                                        var records = Ext.getCmp('rightRespondentsGrid').getSelectionModel().getSelection();
                                        rightRespondentsStore.remove(records);
                                        leftRespondentsStore.add(records)
                                    }
                                }
                            }
                        }]
                    }, {
                        xtype: 'container',
                        width: '48%',
                        items: [{
                            xtype: 'gridpanel',
                            id: 'rightRespondentsGrid',
                            store: rightRespondentsStore,
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
                            digiGrid = Ext.getCmp("rightRespondentsGrid");
                            var credIds = [];
                            for (var i = 0; i < digiGrid.store.getCount(); i++)
                                credIds.push(digiGrid.store.getAt(i).data.id);

                            Ext.Ajax.request({
                                url: dataUrl + '/utils/config/updateDigitalSigningOrgIds',
                                method: 'POST',
                                params: {
                                    digitalSigningOrgIds: credIds
                                },
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function () {
                                    Ext.Msg.alert(label_UPDATED);
                                },
                                failure: function (response) {
                                    var error = JSON.parse(response.responseText);

                                    Ext.Msg.show({
                                        title: label_ERROR,
                                        msg: error.message,
                                        width: 300,
                                        buttons: Ext.MessageBox.YES
                                    });
                                }
                            });
                        }
                    }
                }]
            }]
        }]
    });
});
