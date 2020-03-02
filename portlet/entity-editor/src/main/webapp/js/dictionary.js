/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

function createDictGrid(metaClassId, repDate, callback) {
    Ext.Ajax.request({
        url: dataUrl + '/core/meta/getMetaClassAttributesList',
        params : {
            metaClassId: metaClassId
        },
        method: 'GET',
        failure: function(response) {
            var error = JSON.parse(response.responseText);

            Ext.Msg.show({
                title: label_ERROR,
                msg: error.errorMessage,
                width : 300,
                buttons: Ext.MessageBox.YES
            });
        },
        success: function(response) {
            // получаем мета атрибуты у мета класса
            var attributes = JSON.parse(response.responseText);

            // обязательные поля сущности для модули
            var fields = [
                {name: 'entity_id', type: 'number'},
                {name: 'creditor_id', type: 'number'},
                {name: 'open_date', type: 'date'},
                {name: 'close_date', type: 'date'}
            ];

            // добавляю мета атрибуты в поля модели
            attributes.forEach(function(element) {
                fields.push({name: element.name});
            });

            var dictModel = Ext.define('dictModel', {
                extend: 'Ext.data.Model',
                fields: fields
            });

            // подгружаем записи справочника в хранилище
            // чтобы отобразить в гриде в табличной форме
            dictStore = Ext.create('Ext.data.Store', {
                model: 'dictModel',
                autoLoad: true,
                listeners: {
                    load: function(me, records, options) {
                        if (records.length > 0)
                            dictGrid.getSelectionModel().select(0);
                    }
                },
                proxy: {
                    type: 'ajax',
                    url: dataUrl + '/core/dict/getDictEntities',
                    extraParams: {
                        metaId : metaClassId,
                        userId: userId,
                        isNb: isNb,
                        reportDate: moment(repDate).local().format('YYYY-MM-DD'),
                    },
                    method: 'GET',
                    reader: {
                        type: 'json',
                        root: 'data',
                        totalProperty: 'total'
                    }
                },
                 sorters: [{
                     property: 'entity_id',
                     direction: 'asc'
                 }]
            });

            // создаем столбцы для грида справочника из мета атрибутов
            var columns = createColumns(attributes);

            // создаем грид чтобы отобразить данные справочника
            // передаем ему список столбцов
            var dictGrid = Ext.create('Ext.grid.Panel', {
                id: "dictGrid",
                height: 250,
                scrollable: true,
                store: dictStore,
                columns: columns,
                title: label_ITEMS,
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [{
                        xtype: 'button',
                        text: 'Найти по наименованию',
                        listeners: {
                            click: function () {
                                grid = Ext.getCmp("dictGrid");
                                grid.store.clearFilter();
                                var newValue = Ext.getCmp("searchInGrid").getValue();
                                if (newValue) {
                                    var matcher = new RegExp(Ext.String.escapeRegex(newValue), "i");
                                    grid.store.filter({
                                        filterFn: function(record) {
                                            return matcher.test(record.get('name_ru')) ||
                                                matcher.test(record.get('name_kz'))  ||
                                                matcher.test(record.get('code'));
                                        }
                                    });
                                }
                            }
                        }
                    }, {
                        xtype: 'textfield',
                        id: 'searchInGrid'
                    }]
                }]
            });

            callback(dictGrid);
        }
    });
}

// создает столбцы для грида справочника из мета атрибутов
function createColumns(attributes) {
    var columns = [];

    // обязательные поля грида добавляем собственноручно
    columns.push({text: 'ID', dataIndex: 'entity_id'});

    // добавляем столбцы для грида на основе справочника мета атрибутов
    for (var i = 0; i < attributes.length; i++) {
        columns.push({
            text: attributes[i].title,
            dataIndex: attributes[i].name
        });
    }

    columns.push({
        text: label_OPEN_DATE,
        dataIndex: 'open_date',
        xtype: 'datecolumn',
        minWidth : 120,
        format: 'd.m.Y'
    });

    columns.push({
        text: label_CLOSE_DATE,
        dataIndex: 'close_date',
        xtype: 'datecolumn',
        minWidth : 120,
        format: 'd.m.Y'
    });

    return columns;
}

function dictChange(node, entityId, respondentId, metaClassId, callback) {
    node.removeAll();

    var spinner = new Ext.LoadMask(Ext.getBody(), {msg: label_LOADING});
    spinner.show();

    var tempEntityStore = Ext.getStore('tempEntityStore');

    if (!tempEntityStore) {
        tempEntityStore = Ext.create('Ext.data.TreeStore', {
            model: 'entityModel',
            storeId: 'tempEntityStore',
            folderSort: true,
            proxy: {
                type: 'ajax',
                url: dataUrl + '/core/eav/getEntityData'
            }
        });
    }

    tempEntityStore.load({
        params: {
            entityId: entityId,
            respondentId: respondentId,
            metaClassId: metaClassId,
            date: new Date().toISOString().slice(0, 10),
            asRoot: false
        },
        callback: function(records, operation, success) {
            node.data.value = records[0].data.value;

            while (records[0].childNodes.length > 0) {
                node.appendChild(records[0].childNodes[0]);
            }

            spinner.hide();

            if (callback)
                callback();
        }
    });
}

function showDictPicker(classId, title, callback) {
    createDictGrid(classId, new Date(), function(dictGrid) {
        var window = new Ext.Window({
            id: 'windowDictSelect',
            modal: 'true',
            title: label_REF_CHOOSE + title + '\"',
            items: [
                Ext.create('Ext.form.Panel', {
                    region: 'center',
                    width: 600,
                    height: 300,
                    layout: 'border',
                    items: [
                        {
                            region: 'center',
                            bodyStyle: 'padding: 5px',
                            items: [
                                Ext.create('Ext.button.Button', {
                                    text: label_CHOOSE,
                                    handler: function() {
                                        var selected = dictGrid.getSelectionModel().getLastSelected();

                                        if (!selected) {
                                            //TODO: показать ошибку чтобы выбрали запись
                                            return;
                                        }

                                        window.close();

                                        callback(selected.data);
                                    }
                                }),
                                dictGrid
                            ]
                        }
                    ]
                })
            ]
        });

        window.show();
    });
}