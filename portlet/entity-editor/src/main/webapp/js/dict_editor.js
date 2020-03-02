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
    function showEntityData(entityId, metaClassId, respondentId) {
        // показываю в поле entityId идентификатор сущности
        var edEntityId = Ext.getCmp("edEntityId");
        edEntityId.setValue(entityId);

        var date = Ext.getCmp('edDate').value;

        // при выборке записи сущности в гриде подгружаю сущность из бд
        // чтобы отобразить сущность в дереве сущности
        // подгружаем из бэкенда сущность в виде дерева
        entityStore.load({
            url: dataUrl + '/core/eav/getEntityData',
            method: 'GET',
            params: {
                entityId: entityId,
                respondentId: respondentId,
                metaClassId: metaClassId,
                date: moment(date).local().format('YYYY-MM-DD'),
                asRoot: true
            }
        });
    }

    Ext.override(Ext.data.proxy.Ajax, {timeout: timeout});

    // доп работы по ExtJS
    createExtJsComps();

    // создаем модели мета данных
    createMetaModels();

    var entityStore = Ext.create('Ext.data.TreeStore', {
        model: 'entityModel',
        storeId: 'entityStore',
        folderSort: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/eav/getEntityData'
        }
    });

    var buttonReload = Ext.create('Ext.button.Button', {
        id: "buttonReload",
        text: label_REFRESH,
        handler: function() {
            var dictGrid = Ext.getCmp('dictGrid');
            if (!dictGrid)
                return;

            var metaClassId = Ext.getCmp('edMetaClass').value;
            var repDate = Ext.getCmp('edDate').value;

            var dictStore = dictGrid.getStore();
            dictStore.proxy.extraParams = {
                metaId : metaClassId,
                userId: userId,
                isNb: isNb,
                reportDate: moment(repDate).local().format('YYYY-MM-DD')
            };

            dictStore.load();
            dictGrid.getView().refresh();
        }
    });

    var buttonNewEntity = Ext.create('Ext.button.Button', {
        id: "buttonNewEntity",
        hidden: !isDataManager,
        text: label_ADD,
        handler: function() {
            var edMetaClass = Ext.getCmp('edMetaClass');
            var classId = edMetaClass.value;
            var metaClass = edMetaClass.findRecordByValue(classId).data;

            showNewEntityWindow(metaClass);
        }
    });

    var buttonExport = Ext.create('Ext.button.Button', {
        id: "buttonExport",
        text: label_EXPORT,
        handler: function() {
            var metaClassId = Ext.getCmp('edMetaClass').value;
            var date = moment(Ext.getCmp('edDate').value).local().format('YYYY-MM-DD');

            var xhr = new XMLHttpRequest();
            xhr.open("GET", dataUrl + "/core/dict/exportDictionaryToMsExcel?metaClassId="+ metaClassId + '&userId=' + userId + '&isNb=' + isNb + '&date=' + date, true);
            xhr.responseType = "arraybuffer";
            xhr.onload = function (oEvent) {
                var responseArray = new Uint8Array(this.response);

                // извлекаю наименование файла из заголовков
                var fileName = "";

                var disposition = xhr.getResponseHeader('Content-Disposition');
                if (disposition && disposition.indexOf('attachment') !== -1) {
                    var fileNameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                    var matches = fileNameRegex.exec(disposition);
                    if (matches != null && matches[1]) {
                        fileName = matches[1].replace(/['"]/g, '');
                    }
                }

                var blob = new Blob([responseArray], {type: "application/vnd.ms-excel"});
                saveAs(blob, fileName);
            };

            xhr.send();
        }
    });

    var buttonOpenEntity = Ext.create('Ext.button.Button', {
        id: "buttonOpenEntity",
        text: label_OPEN,
        hidden: !isDataManager,
        handler: function() {
            sendXml('OPEN', Ext.getCmp('edDate').value, Ext.getCmp('edMetaClass').value);
        }
    });

    var buttonCloseEntity = Ext.create('Ext.button.Button', {
        id: "buttonCloseEntity",
        text: label_CLOSE,
        hidden: !isDataManager,
        handler: function() {
            sendXml('CLOSE', Ext.getCmp('edDate').value, Ext.getCmp('edMetaClass').value);
        }
    });

    var buttonDeleteEntity = Ext.create('Ext.button.Button', {
        id: "buttonDeleteEntity",
        text: label_DEL,
        hidden: !isDataManager,
        handler: function() {
            sendXml('DELETE', Ext.getCmp('edDate').value, Ext.getCmp('edMetaClass').value);
        }
    });

    var buttonShowXml = Ext.create('Ext.button.Button', {
        id: "buttonShowXml",
        text: 'XML',
        handler: function() {
            showXml();
        }
    });

    var buttonSendXml = Ext.create('Ext.button.Button', {
        id: "buttonSendXml",
        text: label_SAVE,
        hidden: !isDataManager,
        handler: function() {
            // по справочникам признак подтверждения всегда false
            isMaintenance = false;

            sendXml(null, Ext.getCmp('edDate').value, Ext.getCmp('edMetaClass').value);
        }
    });

    var entityTreeView = Ext.create('Ext.tree.Panel', {
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
            flex: 2,
            dataIndex: 'name',
            sortable: true
        }, {
            text: label_VALUE,
            flex: 2,
            dataIndex: 'value',
            sortable: true
        }, {
            text: label_OPEN_DATE,
            xtype: 'datecolumn',
            flex: 5,
            dataIndex: 'openDate',
            format: 'd.m.Y',
            sortable: true
        }, {
            text: label_CLOSE_DATE,
            xtype: 'datecolumn',
            flex: 6,
            dataIndex: 'closeDate',
            format: 'd.m.Y',
            sortable: true
        }],
        listeners: {
            itemcontextmenu: function (me, node, item, index, e, eOpts) {
                if (!isDataManager)
                    return;
                if (e.button <= 0)
                    return;

                // пункты меню при нажатий правой кнопкой мыши
                var items = [];

                // меню активно только если нажали узел комплексного атрибута
                // добавляем значения по комплексному атрибуту
                if (!node.data.simple) {
                    items.push({
                        text: label_ADD,
                        handler: function() {
                            showAddEntityAttrWindow(node);
                        }
                    });
                }

                // редактирование сущности и всех ее атрибутов
                if (!node.data.array && !node.data.simple && (!node.data.dictionary || node.data.depth == 1)) {
                    items.push({
                        text: label_CHANGE,
                        handler: function() {
                            var metaClassId = null;
                            var windowTitle = null;

                            if (node.data.depth == 1) {
                                metaClassId = Ext.getCmp('edMetaClass').value;
                                windowTitle = Ext.getCmp('edMetaClass').getRawValue();
                            } else {
                                metaClassId = node.data.refClassId;
                                windowTitle = node.data.title;
                            }

                            showEditEntityWindow(node, metaClassId, windowTitle, false, function() {
                                Ext.getCmp('entityTreeView').getView().refresh();
                            });
                        }
                    });
                }

                if (node.data.depth > 1) {
                    // редактирование отдельного атрибута
                    if (node.data.dictionary || (node.data.simple && !(node.parentNode.data.dictionary && node.parentNode.data.depth > 1)))
                        items.push({
                            text: label_EDIT_ATTR,
                            handler: function() {
                                // если справочник то показываем пикер выбора справочника с гридом
                                // если обычный атрибут то диалговое окошко с атрибутом и его значением
                                if (node.data.dictionary) {
                                    showDictPicker(node.data.refClassId, node.data.title, function(selData) {
                                        dictChange(node, selData.entity_id, selData.creditor_id, node.data.refClassId, function() {
                                            Ext.getCmp('entityTreeView').getView().refresh();
                                        });
                                    });
                                } else {
                                    showEditAttrWindow(node, false, function() {
                                        Ext.getCmp('entityTreeView').getView().refresh();
                                        editorAction.commitEdit();
                                    });
                                }
                            }
                        });

                    // запрещаем удалять элементы кумулятивного сета,
                    // ключевые атрибуты и также значения справочников
                    if (!(node.parentNode.data.array && node.parentNode.data.cumulative) && !node.data.key &&
                        !(node.parentNode.data.dictionary && node.parentNode.data.depth > 1)) {
                        items.push({
                            text: label_DEL,
                            handler: function () {
                                showDeleteWindow(node);
                            }
                        });
                    }
                }

                // внимание!!! костыль для справочника ref_portfolio
                // если нажали на атрибуты respondent, code то обнуляю меню
                // так как данные атрибуты нельзя редактировать
                if (node.data.classId == 36 && node.data.name == 'respondent' || node.data.name == 'code')
                    items = [];

                if (items.length == 0)
                    items.push({
                        text: label_NO_AVA,
                        handler: function() {
                        }
                    });

                var menu = new Ext.menu.Menu({
                    items: items
                });

                menu.showAt(e.xy);
                e.stopEvent();
            }
        }
    });

    var mainPanel = Ext.create('Ext.panel.Panel', {
        height: '700px',
        width: '100%',
        renderTo: 'entity-editor-content',
        title: '&nbsp',
        id: 'mainPanel',
        layout: 'border',
        defaults : {
            padding: '3'
        },
        dockedItems: [
            {
                fieldLabel: label_REF,
                id: 'edMetaClass',
                xtype: 'combobox',
                displayField: 'title',
                width: '60%',
                labelWidth: '40%',
                valueField: 'id',
                editable: false,
                listeners: {
                    change: function(a, key, prev) {
                        var metaClass = a.findRecordByValue(key).data;

                        // банки могут только работать со справочником ref_portfolio
                        // то есть для остальных справочников операций не доступны
                        if(!isNb) {
                            if (metaClass.name  == 'ref_portfolio') {
                                buttonNewEntity.show();
                                buttonSendXml.show();
                                buttonShowXml.show();
                            } else {
                                buttonNewEntity.hide();
                                buttonSendXml.hide();
                                buttonShowXml.hide();
                                buttonDeleteEntity.hide();
                                buttonCloseEntity.hide();
                                buttonOpenEntity.hide();
                            }
                        }

                        var repDate = Ext.getCmp('edDate').value;

                        // создаю грид справочника
                        createDictGrid(metaClass.id, repDate, function(dictGrid) {
                            dictGrid.on('itemclick', function(dv, record, item, index, e) {
                                var entityId = record.get('entity_id');
                                var respondentId = record.get('creditor_id');
                                showEntityData(entityId, metaClass.id, respondentId);
                            });

                            dictGrid.store.on('load', function(me, records, options) {
                                if (records.length > 0) {
                                    dictGrid.getSelectionModel().select(0);

                                    var entityId = records[0].data.entity_id;
                                    var respondentId = records[0].data.creditor_id;

                                    showEntityData(entityId, metaClass.id, respondentId);
                                }
                            });

                            var dictGridContainer = Ext.getCmp('gridPanel');
                            dictGridContainer.removeAll();
                            dictGridContainer.add(dictGrid);
                        });
                    }
                },
                store: Ext.create('Ext.data.Store', {
                    model: 'metaClassModel',
                    id: 'metaClassStore',
                    pageSize: 100,
                    proxy: {
                        type: 'ajax',
                        url: dataUrl + '/core/meta/getDictionaries',
                        actionMethods: {
                            read: 'GET'
                        },
                        reader: {
                            type: 'json',
                            root: ''
                        }
                    },
                    listeners: {
                        load: function(me, records, options) {
                            var metaClass = records[0];
                            Ext.getCmp('edMetaClass').setValue(metaClass.get('id'));
                        }
                    },
                    autoLoad: true,
                    remoteSort: true
                })
            },
            {
                xtype: 'textfield',
                id: 'edEntityId',
                fieldLabel: label_ID_ENTITY,
                width: '60%',
                labelWidth: '40%',
                enabled: true,
                disabled : true,
                value: (queryEntityId == "null" ? "" : queryEntityId)
            },
            {
                xtype: 'datefield',
                id: 'edDate',
                fieldLabel: label_DATE,
                format: 'd.m.Y',
                value: new Date(),
                width: '60%',
                labelWidth: '40%'
            },
            {
                xtype: 'tbseparator',
                height: 10
            }
        ],
        items: [
            {
                id: 'gridPanel',
                region: 'north',
                width: '100%',
                split: true,
                items: [],
                autoScroll: true,
                tbar: [buttonReload, buttonNewEntity, buttonSendXml, buttonShowXml,
                    buttonDeleteEntity, buttonOpenEntity, buttonCloseEntity,
                    buttonExport]
            },
            {
                xtype : 'panel',
                region: 'center',
                preventHeader: true,
                width: '100%',
                autoScroll: true,
                items: [entityTreeView]
            }
        ]
    });

});





