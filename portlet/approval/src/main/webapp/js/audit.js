Ext.onReady(function () {

    Ext.require([

        'Ext.Msg',
        'Ext.panel.*',
        'Ext.form.*',
        'Ext.selection.CellModel',
        'Ext.grid.*',
        'Ext.data.*'

    ]);

    Ext.define('AuditEvemt', {
        extend: 'Ext.data.Model',
        fields: ['id', 'userId', 'auditTime', 'tableName', 'content', 'eventName', 'userName']
    });

    var selStore = Ext.create('Ext.data.Store', {
        id: 'selStore',
        model: 'AuditEvemt',
        remoteSort: true,
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/audit/getAuditJson',
            actionMethods: {
                read: 'GET'
            },
            extraParams: {
                userId: '10916'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        }
    });

    var selCreditorsGrid = Ext.create('Ext.grid.Panel', {
        id: 'selCreditorsGrid',
        sortable: true,
        multiSelect: true,
        store: selStore,
        columns: [{
            dataIndex: 'userName',
            flex: 1,
            text: 'Пользователь'

        }, {
            dataIndex: 'eventName',
            text: 'Действие',
            flex: 1
        }, {
            dataIndex: 'tableName',
            text: 'Таблица',
            flex: 1
        }, {
            dataIndex: 'content',
            text: 'Описание',
            flex: 1
        }, {
            dataIndex: 'auditTime',
            text: 'Время аудита',
            renderer: Ext.util.Format.dateRenderer('d.m.Y H:i:s'),
            flex: 1
        }]

    });

    Ext.create('Ext.Panel', {
        width: 900,
        height: 600,
        padding: 10,
        title: 'Audit',
        layout: 'border',
        renderTo: Ext.getBody(), //'admin-content',
        items: [{

            xtype: 'panel',
            region: 'north',
            margin: '0 0 0 0',
            autoScroll: true,

            height: 220,
            items: [{
                xtype: 'panel',
                layout: 'hbox',
                border: false,
                items: [{
                    xtype: 'datefield',
                    fieldLabel: 'Дата аудита не менее ',
                    allowBlank: false,
                    name: 'from_date',
                    id: 'from_date',
                    format: 'Y-m-d'
                }, {
                    xtype: 'datefield',
                    fieldLabel: 'Дата аудита не более ',
                    allowBlank: false,
                    name: 'to_date',
                    id: 'to_date',
                    format: 'Y-m-d',
                    margin: '0 0 0 10',
                }]
            }, {
                xtype: 'textfield',
                id: 'table',
                fieldLabel: 'Название таблицы содержит ',
                allowBlank: false,
                name: 'name'
            }, {
                xtype: 'button',
                id: 'AddButton',
                flex: 1,
                text: 'Применить фильтр',
                listeners: {
                    click:

                        function (field, newValue, oldValue, options) {

                            grid = Ext.getCmp("selCreditorsGrid");
                            val1 = Ext.getCmp("table");
                            grid.store.clearFilter();
                            grid.getView().refresh();
                            from_date = Ext.getCmp("from_date");
                            to_date = Ext.getCmp("to_date");
                            var dateFrom_formated = Ext.Date.format(from_date.getValue(), 'Y-m-d');
                            var dateTo_formated = Ext.Date.format(to_date.getValue(), 'Y-m-d');
                            if ((dateTo_formated == '') && (val1.rawValue == '')) {
                                for (var i = 0; i < grid.getStore().data.length; i++) {
                                    var record = grid.getStore().getAt(i);
                                    grid.store.filter(
                                        new Ext.util.Filter({
                                            filterFn: function (record) {
                                                grid.getView().refresh();
                                                return (record.data.auditTime >= dateFrom_formated)
                                            }
                                        }));
                                }
                            } else {
                                if ((dateFrom_formated == '') && (val1.rawValue == '')) {
                                    for (var i = 0; i < grid.getStore().data.length; i++) {
                                        var record = grid.getStore().getAt(i);
                                        grid.store.filter(
                                            new Ext.util.Filter({
                                                filterFn: function (record) {
                                                    grid.getView().refresh();
                                                    return (record.data.auditTime <= dateTo_formated)
                                                }
                                            }));
                                    }
                                } else {
                                    if ((dateFrom_formated == '') && (dateTo_formated == '')) {
                                        grid.store.filter([{
                                            property: "tableName",
                                            value: val1.rawValue,
                                            anyMatch: true,
                                            caseSensitive: false
                                        }]);
                                    } else {
                                        if (val1.rawValue == '') {
                                            for (var i = 0; i < grid.getStore().data.length; i++) {
                                                var record = grid.getStore().getAt(i);
                                                grid.store.filter(
                                                    new Ext.util.Filter({
                                                        filterFn: function (record) {
                                                            grid.getView().refresh();
                                                            return (record.data.auditTime >= dateFrom_formated &&
                                                                record.data.auditTime <= dateTo_formated
                                                            )
                                                        }
                                                    }));
                                            }
                                        } else {
                                            if (dateFrom_formated == '') {
                                                grid.store.filter([{
                                                    property: "tableName",
                                                    value: val1.rawValue,
                                                    anyMatch: true,
                                                    caseSensitive: false
                                                }]);
                                                for (var i = 0; i < grid.getStore().data.length; i++) {
                                                    var record = grid.getStore().getAt(i);
                                                    grid.store.filter(
                                                        new Ext.util.Filter({
                                                            filterFn: function (record) {
                                                                grid.getView().refresh();
                                                                return (
                                                                    record.data.auditTime <= dateTo_formated
                                                                )
                                                            }
                                                        }));

                                                }
                                            } else {
                                                if (dateTo_formated == '') {
                                                    grid.store.filter([{
                                                        property: "tableName",
                                                        value: val1.rawValue,
                                                        anyMatch: true,
                                                        caseSensitive: false
                                                    }]);
                                                    for (var i = 0; i < grid.getStore().data.length; i++) {
                                                        var record = grid.getStore().getAt(i);
                                                        grid.store.filter(
                                                            new Ext.util.Filter({
                                                                filterFn: function (record) {
                                                                    grid.getView().refresh();
                                                                    return (
                                                                        record.data.auditTime >= dateFrom_formated
                                                                    )
                                                                }
                                                            }));

                                                    }
                                                } else {
                                                    if ((dateFrom_formated == '') && (dateTo_formated == '') && (val1.rawValue == '')) {
                                                        grid.store.clearFilter();
                                                        grid.getView().refresh();
                                                    } else {
                                                        grid.store.filter([{
                                                            property: "tableName",
                                                            value: val1.rawValue,
                                                            anyMatch: true,
                                                            caseSensitive: false
                                                        }]);
                                                        for (var i = 0; i < grid.getStore().data.length; i++) {
                                                            var record = grid.getStore().getAt(i);
                                                            grid.store.filter(
                                                                new Ext.util.Filter({
                                                                    filterFn: function (record) {
                                                                        grid.getView().refresh();
                                                                        return (record.data.auditTime >= dateFrom_formated &&
                                                                            record.data.auditTime <= dateTo_formated
                                                                        )
                                                                    }
                                                                }));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }, {
                xtype: 'button',
                id: 'Button',
                flex: 1,
                text: 'Очистить фильтр',
                listeners: {
                    click:

                        function (field, newValue, oldValue, options) {
                            val1 = Ext.getCmp("table");
                            val1.setValue('');
                            val2 = Ext.getCmp("from_date");
                            val2.setValue('');
                            val3 = Ext.getCmp("to_date");
                            val3.setValue('');
                            grid = Ext.getCmp("selCreditorsGrid");
                            grid.store.clearFilter();
                            grid.getView().refresh();

                        }
                }
            }]
        }, {
            xtype: 'panel',
            autoScroll: true,
            title: 'Таблица аудита',
            region: 'south',
            height: 430,
            items: [selCreditorsGrid]
        }],
        renderTo: Ext.getBody()

    });

});
