/**
 * @author Olzhas Kaliaskar
 */

Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*'
]);

function showProductWindow(productId) {
    if (productId) {
        Ext.Ajax.request({
            url: dataUrl + '/core/product/getProductJsonById',
            method: 'GET',
            params: {
                id: productId
            },
            success: function(response) {
                var productData = JSON.parse(response.responseText);
                createProductWindow(productData);
            }
        });
    } else {
        createProductWindow({});
    }
}

function createProductWindow(productData) {
    var buttonSave = Ext.create('Ext.button.Button', {
        hidden: !isDataManager,
        text: label_SAVE,
        handler: function() {
            var form = Ext.getCmp('formProduct').getForm();

            if (!form.isValid()) {
                Ext.Msg.alert({
                    title: label_ERROR,
                    msg: label_FIELDS
                });

                return;
            }

            var formValues = form.getValues();

            var tempProductData = {
                id: productData.id,
                code: formValues.code,
                name: formValues.name,
                crosscheckPackageName: formValues.crosscheckPackageName
            };

            var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_SAVING });
            loadMask.show();

            Ext.Ajax.request({
                url: dataUrl + '/core/product/saveProduct',
                method: 'POST',
                waitMsg: label_SAVING,
                jsonData: tempProductData,
                reader: {
                    type: 'json',
                    root: 'data'
                },
                success: function(response, opts) {
                    loadMask.hide();

                    Ext.getCmp('windowProduct').destroy();

                    var productGrid = Ext.getCmp('productGrid');

                    // перегружаем весь грид чтобы отобразить изменения
                    productGrid.getStore().load();
                    productGrid.getView().refresh();
                },
                failure: function(response, opts) {
                    loadMask.hide();

                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    var buttonClose = Ext.create('Ext.button.Button', {
        text: label_CANCEL,
        handler : function() {
            Ext.getCmp('windowProduct').destroy();
        }
    });

    var formProduct = Ext.create('Ext.form.Panel', {
        id: 'formProduct',
        width: 520,
        height: 160,
        fieldDefaults: {
            msgTarget: 'side'
        },
        defaults: {
            anchor: '100%'
        },
        defaultType: 'textfield',
        bodyPadding: '5 5 0',
        items: [
            {
                fieldLabel: 'Код',
                name: 'code',
                allowBlank: false,
                readOnly: productData.id != null
            },
            {
                fieldLabel: label_NAME,
                name: 'name',
                allowBlank: false
            },
            {
                fieldLabel: 'Название пакета межформенного контроля',
                name: 'crosscheckPackageName',
                allowBlank: false
            }],
        buttons: [buttonSave, buttonClose]
    });

    var form = formProduct.getForm();
    form.setValues(productData);

    var windowProduct = new Ext.Window({
        id: 'windowProduct',
        layout: 'fit',
        title: label_PRODUCT,
        modal: true,
        maximizable: true,
        items: [formProduct]
    });

    windowProduct.show();
}

function loadMetaClasses() {
    // if (!isNb)
    //     return;

    var productGrid = Ext.getCmp('productGrid');
    var productId = productGrid.getSelectionModel().getLastSelected().data.id;

    var metaClassSelGrid = Ext.getCmp('metaClassSelGrid');
    var metaClassAvlGrid = Ext.getCmp('metaClassAvlGrid');

    metaClassSelGrid.store.load({
        params: {
            productId: productId,
            available: false
        }
    });

    metaClassAvlGrid.store.load({
        params: {
            productId: productId,
            available: true
        }
    });

    metaClassSelGrid.getView().refresh();
    metaClassAvlGrid.getView().refresh();
}

function loadPositions() {
    // if (!isNb)
    //     return;

    var productGrid = Ext.getCmp('productGrid');
    var productId = productGrid.getSelectionModel().getLastSelected().data.id;

    var positionSelGrid = Ext.getCmp('positionSelGrid');
    var positionAvlGrid = Ext.getCmp('positionAvlGrid');

    positionSelGrid.store.load({
        params: {
            productId: productId,
            available: false
        }
    });

    positionAvlGrid.store.load({
        params: {
            productId: productId,
            available: true
        }
    });

    positionSelGrid.getView().refresh();
    positionAvlGrid.getView().refresh();
}

Ext.onReady(function() {
    Ext.override(Ext.data.proxy.Ajax, {timeout: 1200000});

    Ext.define('productModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'code', 'name']
    });

    Ext.define('metaClassModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'name', 'title']
    });

    Ext.define('positionModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'nameRu']
    });

    Ext.define('iterationModel', {
        extend: 'Ext.data.Model',
        fields: ['iterationNumber', 'month', 'day', 'hour', 'min', 'dayTransfer']
    });

    var productStore = Ext.create('Ext.data.Store', {
        id: 'productStore',
        model: 'productModel',
        autoLoad: true,
        listeners: {
            load: function (me, records, options) {
                productGrid.getSelectionModel().select(0);
                loadMetaClasses();
                loadPositions();
                if (records.length > 0)
                    Ext.getCmp('comboProducts').setValue(records[0].get('id'));
            },
            scope: this
        },
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getProductList',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        },
    });

    var metaClassSelStore = Ext.create('Ext.data.Store', {
        id: 'metaClassSelStore',
        model: 'metaClassModel',
        pageSize: 100,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getMetaClasses',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    var metaClassAvlStore = Ext.create('Ext.data.Store', {
        id: 'metaClassAvlStore',
        model: 'metaClassModel',
        pageSize: 100,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getMetaClasses',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    var positionSelStore = Ext.create('Ext.data.Store', {
        id: 'positionSelStore',
        model: 'positionModel',
        pageSize: 100,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getPositions',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    var positionAvlStore = Ext.create('Ext.data.Store', {
        id: 'positionAvlStore',
        model: 'positionModel',
        pageSize: 100,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getPositions',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    var buttonDownloadXsd = Ext.create('Ext.button.Button', {
        text: label_DOWN,
        handler: function() {
            var productGrid = Ext.getCmp('productGrid');
            var productData = productGrid.getSelectionModel().getLastSelected().data;

            var xhr = new XMLHttpRequest();
            xhr.open('GET', dataUrl + '/core/product/getProductXsd?productId=' + productData.id);
            xhr.responseType = 'arraybuffer';

            xhr.onload = function (oEvent) {
                var responseArray = new Uint8Array(this.response);
                var blob = new Blob([responseArray], {type: 'application/xsd'});

                var fileName = productData.code + '.xsd';
                saveAs(blob, fileName);
            };

            xhr.send();
        }
    });

    var buttonGenerateXsd = Ext.create('Ext.button.Button', {
        hidden: !isDataManager,
        text: label_XSD,
        //hidden: !isNb,
        handler: function() {
            var loadMask = new Ext.LoadMask(Ext.getBody(), {
                msg: label_XSDING
            });

            loadMask.show();

            Ext.Ajax.request({
                url: dataUrl + '/core/product/generateXsd',
                waitMsg: label_XSDING,
                method: 'POST',
                success: function(response, opts) {
                    loadMask.hide();

                    Ext.Msg.alert(label_INFO, label_XSD_END);
                },
                failure: function(response, opts) {
                    loadMask.hide();

                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    var productGrid = Ext.create('Ext.grid.Panel', {
        id: 'productGrid',
        store: productStore,
        region: 'west',
       // width: isNb? '40%': '100%',
        width: '40%',
        minSize: 50,
        split: true,
        columns: [
            {
                text: label_CODE,
                dataIndex: 'code',
                flex: 1
            },
            {
                text: label_NAME,
                dataIndex: 'name',
                flex: 2
            }
        ],
        listeners: {
            cellclick: function(grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts) {
                loadMetaClasses();
                loadPositions();
            },
            itemdblclick: function(dv, record, item, index, e) {
                // if (!isNb)
                //     return;

                showProductWindow(Number(record.get('id')));
            }
        },
        tbar: [
            {
                text: label_ADD,
                hidden: !isDataManager,
                //hidden: !isNb,
                handler: function(e1, e2) {
                    showProductWindow();
                }
            },
            {
                xtype: 'tbseparator'
            },
            buttonDownloadXsd,
            {
                xtype: 'tbseparator',
              //  hidden: isNb
            },
            buttonGenerateXsd
        ]
    });

    var metaClassSelGrid = Ext.create('Ext.grid.Panel', {
        id: 'metaClassSelGrid',
        store: metaClassSelStore,
        viewConfig: { emptyText: label_NO_DATA },
        columns: [
            {
                text: label_CODE,
                dataIndex: 'name',
                flex: 1
            },
            {
                text: label_NAME,
                dataIndex: 'title',
                flex: 2
            }
        ],
    });

    var metaClassAvlGrid = Ext.create('Ext.grid.Panel', {
        id: 'metaClassAvlGrid',
        store: metaClassAvlStore,
        viewConfig: { emptyText: label_NO_DATA },
        columns: [
            {
                text: label_CODE,
                dataIndex: 'name',
                flex: 1
            },
            {
                text: label_NAME,
                dataIndex: 'title',
                flex: 2
            }
        ]
    });

    var positionSelGrid = Ext.create('Ext.grid.Panel', {
        id: 'positionSelGrid',
        store: positionSelStore,
        viewConfig: { emptyText: label_NO_DATA },
        columns: [
            {
                text: label_CODE,
                dataIndex: 'nameRu',
                flex: 1
            }
        ],
    });

    var positionAvlGrid = Ext.create('Ext.grid.Panel', {
        id: 'positionAvlGrid',
        store: positionAvlStore,
        viewConfig: { emptyText: label_NO_DATA },
        columns: [
            {
                text: label_CODE,
                dataIndex: 'nameRu',
                flex: 1
            }
        ]
    });

    var buttonAddMetaClass = Ext.create('Ext.button.Button', {
        id: 'buttonAddMetaClass',
        hidden: !isDataManager,
        text: label_ADD,
        //icon: contextPathUrl + '/icons/down_1.png',
        flex: 1,
        handler: function() {
            if (!metaClassAvlGrid.getSelectionModel().hasSelection() ||
                !productGrid.getSelectionModel().hasSelection())
                return;

            var metaIds = [];
            for (var i = 0; i < metaClassAvlGrid.store.getCount(); i++) {
                if (metaClassAvlGrid.getSelectionModel().isSelected(i)) {
                    metaIds.push(metaClassAvlGrid.store.getAt(i).data.id);
                }
            }

            var productData = productGrid.getSelectionModel().getLastSelected().data;

            Ext.Ajax.request({
                url: dataUrl + '/core/product/addProductMetaClass',
                method: 'PUT',
                params: {
                    productId: productData.id,
                    metaIds: metaIds
                },
                reader: {
                    type: 'json',
                    root: ''
                },
                success: function() {
                    loadMetaClasses();
                },
                failure: function(response, opts) {
                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    var buttonDeleteMetaClass = Ext.create('Ext.button.Button', {
        id: 'buttonDeleteMetaClass',
        flex: 1,
        hidden: !isDataManager,
        //icon: contextPathUrl + '/icons/up_1.png',
        text: label_DELETE,
        handler: function() {
            if (!metaClassSelGrid.getSelectionModel().hasSelection() ||
                !productGrid.getSelectionModel().hasSelection())
                return;

            var metaIds = [];
            for (var i = 0; i < metaClassSelGrid.store.getCount(); i++) {
                if (metaClassSelGrid.getSelectionModel().isSelected(i)) {
                    metaIds.push(metaClassSelGrid.store.getAt(i).data.id);
                }
            }

            var productData = productGrid.getSelectionModel().getLastSelected().data;

            Ext.Ajax.request({
                url: dataUrl + '/core/product/deleteProductMetaClass',
                method: 'POST',
                params: {
                    productId: productData.id,
                    metaIds: metaIds
                },
                reader: {
                    type: 'json',
                    root: ''
                },
                success: function() {
                    loadMetaClasses();
                },
                failure: function(response, opts) {
                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    var buttonAddPosition = Ext.create('Ext.button.Button', {
        id: 'buttonAddPosition',
        hidden: !isDataManager,
        text: label_ADD,
        flex: 1,
        handler: function() {
            if (!positionAvlGrid.getSelectionModel().hasSelection() ||
                !productGrid.getSelectionModel().hasSelection())
                return;

            var posIds = [];
            for (var i = 0; i < positionAvlGrid.store.getCount(); i++) {
                if (positionAvlGrid.getSelectionModel().isSelected(i)) {
                    posIds.push(positionAvlGrid.store.getAt(i).data.id);
                }
            }

            var productData = productGrid.getSelectionModel().getLastSelected().data;

            Ext.Ajax.request({
                url: dataUrl + '/core/product/addProductPosition',
                method: 'POST',
                params: {
                    productId: productData.id,
                    posIds: posIds
                },
                reader: {
                    type: 'json',
                    root: ''
                },
                success: function() {
                    loadPositions();
                },
                failure: function(response, opts) {
                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    var buttonDeletePosition = Ext.create('Ext.button.Button', {
        id: 'buttonDeletePosition',
        flex: 1,
        hidden: !isDataManager,
        text: label_DELETE,
        handler: function() {
            if (!positionSelGrid.getSelectionModel().hasSelection() ||
                !productGrid.getSelectionModel().hasSelection())
                return;

            var posIds = [];
            for (var i = 0; i < positionSelGrid.store.getCount(); i++) {
                if (positionSelGrid.getSelectionModel().isSelected(i)) {
                    posIds.push(positionSelGrid.store.getAt(i).data.id);
                }
            }

            var productData = productGrid.getSelectionModel().getLastSelected().data;

            Ext.Ajax.request({
                url: dataUrl + '/core/product/deleteProductPosition',
                method: 'POST',
                params: {
                    productId: productData.id,
                    posIds: posIds
                },
                reader: {
                    type: 'json',
                    root: ''
                },
                success: function() {
                    loadPositions();
                },
                failure: function(response, opts) {
                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        }
    });

    var iterationStore = Ext.create('Ext.data.Store', {
        id: 'iterationStore',
        model: 'iterationModel',
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/product/getApproveIterations',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        },
        listeners: {
            load: function (me, records, options) {
                iterationsPanel = Ext.getCmp('iterationsPanel');
                iterationsPanel.removeAll();
                if (records.length > 0) {
                    Ext.getCmp('numIterationCount').setValue(records.length);
                    for (var i = 0; i < records.length; i++) {
                        Ext.create('Ext.form.Panel', {
                            id: 'iterationPanel' + (i + 1),
                            title: 'Итерация №' + (i + 1),
                            height: 50,
                            layout: {
                                type: 'hbox',
                                align: 'middle'
                            },
                            items: [{
                                xtype: 'numberfield',
                                id: 'numMonth' + (i + 1),
                                fieldLabel: 'Месяцы',
                                labelWidth: 55,
                                width: 100,
                                value: records[i].data.month,
                                minValue: 0,
                                keyNavEnabled: false,
                                mouseWheelEnabled: false,
                                margin: '0 20 0 0'
                            }, {
                                xtype: 'numberfield',
                                id: 'numDay' + (i + 1),
                                fieldLabel: 'Дни',
                                labelWidth: 45,
                                width: 90,
                                value: records[i].data.day,
                                minValue: 0,
                                keyNavEnabled: false,
                                mouseWheelEnabled: false,
                                margin: '0 20 0 0'
                            }, {
                                xtype: 'numberfield',
                                id: 'numHour' + (i + 1),
                                fieldLabel: 'Часы',
                                labelWidth: 50,
                                width: 95,
                                value: records[i].data.hour,
                                minValue: 0,
                                keyNavEnabled: false,
                                mouseWheelEnabled: false,
                                margin: '0 20 0 0'
                            }, {
                                xtype: 'numberfield',
                                id: 'numMin' + (i + 1),
                                fieldLabel: 'Минуты',
                                labelWidth: 60,
                                width: 105,
                                value: records[i].data.min,
                                minValue: 0,
                                keyNavEnabled: false,
                                mouseWheelEnabled: false,
                                margin: '0 20 0 0'
                            }, {
                                xtype: 'checkbox',
                                cls: 'checkBox',
                                id: 'chkDayTransfer' + (i + 1),
                                boxLabel: 'С переносом на рабочий день',
                                width: 300,
                                checked: records[i].data.dayTransfer,
                                margin: '0 20 0 0'
                            }]
                        });
                        iterationsPanel.add(Ext.getCmp('iterationPanel' + (i + 1)));
                        iterationsPanel.doLayout();
                    }

                } else {
                    Ext.getCmp('numIterationCount').setValue(0);
                }
            },
            scope: this
        }
    });

    Ext.create('Ext.panel.Panel', {
        width: 1200,
        height: 600,
        padding: 10,
        renderTo: 'product-content',
        title: label_PRODUCTS,
        autoScroll: true,
        layout: {
            type: 'border',
            align: 'stretch'
        },
        items: [
            productGrid,
            {
            xtype: 'tabpanel',
            width: '100%',
            region: 'center',
            items: [{
                xtype: 'panel',
                title: 'Настройка мета классов',
                //width: 500,
                width: '100%',
                //hidden: !isNb,
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                items: [{
                    xtype: 'panel',
                    title: label_META,
                    autoScroll: true,
                    height: 300,
                    items: [metaClassSelGrid]
                }, {
                    xtype: 'panel',
                    height: 24,
                    layout: {
                        align: 'middle',
                        pack: 'center',
                        type: 'hbox'
                    },
                    items: [buttonAddMetaClass, buttonDeleteMetaClass]
                },  {
                    xtype: 'panel',
                    title: label_AVA_META,
                    autoScroll: true,
                    height: 280,
                    items: [metaClassAvlGrid]
                }]
            }, {
                xtype: 'panel',
                title: 'Настройка должностей',
                //width: 500,
                width: '100%',
                //hidden: !isNb,
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                items: [{
                    xtype: 'panel',
                    title: 'Выбранные должности',
                    autoScroll: true,
                    height: 300,
                    items: [positionSelGrid]
                }, {
                    xtype: 'panel',
                    height: 24,
                    layout: {
                        align: 'middle',
                        pack: 'center',
                        type: 'hbox'
                    },
                    items: [buttonAddPosition, buttonDeletePosition]
                },  {
                    xtype: 'panel',
                    title: 'Доступные должности',
                    autoScroll: true,
                    height: 280,
                    items: [positionAvlGrid]
                }]
            }, {
                xtype: 'panel',
                title: 'Настройка одобрения',
                hidden: !isDataManager,
                width: '100%',
                autoScroll: true,
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                items: [{
                    xtype: 'panel',
                    autoScroll: true,
                    height: 50,
                    layout: {
                        type: 'hbox',
                        align: 'middle'
                    },
                    items: [{
                        xtype: 'combobox',
                        id: 'comboProducts',
                        name: 'comboProdsId',
                        fieldLabel: 'Продукт',
                        labelWidth: 60,
                        width:350,
                        allowBlank: false,
                        editable: false,
                        store: productStore,
                        valueField: 'id',
                        displayField: 'name',
                        margin: '0 20 0 0',
                        listeners: {
                            change: function (record, newValue, oldValue, eOpts) {
                                productId = record.value;
                                iterationStore.load({
                                    params: {
                                        productId: productId
                                    }
                                });
                            }
                        }
                    }, {
                        xtype: 'numberfield',
                        id: 'numIterationCount',
                        fieldLabel: 'Количество итераций',
                        labelWidth: 130,
                        width: 175,
                        name: 'iterationCount',
                        value: 0,
                        minValue: 0,
                        editable: false,
                        keyNavEnabled: false,
                        mouseWheelEnabled: false,
                        listeners: {
                            spindown: function (record, eOpts) {
                                valueToRemove = record.value;
                                iterationsPanel = Ext.getCmp('iterationsPanel');
                                iterationsPanel.remove(Ext.getCmp('iterationPanel' + valueToRemove));
                                iterationsPanel.doLayout();
                            },
                            spinup: function (record, eOpts) {
                                valueToAdd = record.value;
                                iterationsPanel = Ext.getCmp('iterationsPanel');
                                Ext.create('Ext.form.Panel', {
                                    id: 'iterationPanel' + (valueToAdd + 1),
                                    title: 'Итерация №' + (valueToAdd + 1),
                                    height: 50,
                                    layout: {
                                        type: 'hbox',
                                        align: 'middle'
                                    },
                                    items: [{
                                        xtype: 'numberfield',
                                        id: 'numMonth' + (valueToAdd + 1),
                                        fieldLabel: 'Месяцы',
                                        labelWidth: 55,
                                        width: 100,
                                        value: 0,
                                        minValue: 0,
                                        keyNavEnabled: false,
                                        mouseWheelEnabled: false,
                                        margin: '0 20 0 0'
                                    }, {
                                        xtype: 'numberfield',
                                        id: 'numDay' + (valueToAdd + 1),
                                        fieldLabel: 'Дни',
                                        labelWidth: 45,
                                        width: 90,
                                        value: 0,
                                        minValue: 0,
                                        keyNavEnabled: false,
                                        mouseWheelEnabled: false,
                                        margin: '0 20 0 0'
                                    }, {
                                        xtype: 'numberfield',
                                        id: 'numHour' + (valueToAdd + 1),
                                        fieldLabel: 'Часы',
                                        labelWidth: 50,
                                        width: 95,
                                        value: 0,
                                        minValue: 0,
                                        keyNavEnabled: false,
                                        mouseWheelEnabled: false,
                                        margin: '0 20 0 0'
                                    }, {
                                        xtype: 'numberfield',
                                        id: 'numMin' + (valueToAdd + 1),
                                        fieldLabel: 'Минуты',
                                        labelWidth: 60,
                                        width: 105,
                                        value: 0,
                                        minValue: 0,
                                        keyNavEnabled: false,
                                        mouseWheelEnabled: false,
                                        margin: '0 20 0 0'
                                    }, {
                                        xtype: 'checkbox',
                                        cls: 'checkBox',
                                        id: 'chkDayTransfer' + (valueToAdd + 1),
                                        boxLabel: 'С переносом на рабочий день',
                                        width: 300,
                                        checked: true,
                                        margin: '0 20 0 0'
                                    }]
                                });
                                iterationsPanel.add(Ext.getCmp('iterationPanel' + (valueToAdd + 1)));
                                iterationsPanel.doLayout();
                            },
                        }
                    }]
                }, {
                    xtype: 'form',
                    title: 'Итераций',
                    id: 'iterationsPanel',
                    autoScroll: true,
                    height: 280,
                    items: []
                }, {
                    xtype: 'panel',
                    height: 40,
                    border: false,
                    layout: {
                        align: 'middle',
                        type: 'hbox'
                    },
                    items: [{
                        xtype: 'button',
                        margin: '0 0 0 5',
                        text: 'Сохранить',
                        //bind: true,
                        listeners: {
                            click: function () {
                                var jsonList = [];
                                var items = Ext.getCmp('iterationsPanel').items;
                                for (i=0; i<items.getCount(); ++i) {
                                    var json = {
                                        iterationNumber: i+1,
                                        month: Ext.getCmp('numMonth'+(i+1)).value,
                                        day: Ext.getCmp('numDay'+(i+1)).value,
                                        hour: Ext.getCmp('numHour'+(i+1)).value,
                                        min: Ext.getCmp('numMin'+(i+1)).value,
                                        dayTransfer: Ext.getCmp('chkDayTransfer'+(i+1)).value
                                    };
                                    jsonList.push(json);
                                }
                                Ext.Ajax.request({
                                    url: dataUrl + '/core/product/setApproveIterations',
                                    method: 'POST',
                                    jsonData: jsonList,
                                    params: {
                                        productId: Ext.getCmp('comboProducts').value
                                    },
                                    reader: {
                                        type: 'json',
                                        root: 'data'
                                    },
                                    success: function(response, opts) {
                                        Ext.Msg.show({
                                            title: '',
                                            msg: 'Изменения сохранены успешно',
                                            width : 300,
                                            buttons: Ext.MessageBox.YES
                                        });
                                    },
                                    failure: function(response, opts) {

                                        var error = JSON.parse(response.responseText);

                                        Ext.Msg.show({
                                            title: label_ERROR,
                                            msg: error.errorMessage,
                                            width : 300,
                                            buttons: Ext.MessageBox.YES
                                        });
                                    }
                                });

                            }
                        }
                    }]
                }]
            }]
        }]
    });
});

