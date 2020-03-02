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
    Ext.override(Ext.data.proxy.Ajax, {timeout: timeout});

    // доп работы по ExtJS
    createExtJsComps();

    // создаем модели мета данных
    createMetaModels();

    var respondentId;

    var respondentStore = Ext.create('Ext.data.Store', {
        fields: ['id', 'name'],
        proxy: {
            type: 'ajax',
            extraParams: {
                userId: userId
            },
            url: dataUrl + '/core/respondent/getUserRespondentList',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        autoLoad: true,
        listeners: {
            load: function(me, records, options) {
                if (queryRespondentId && queryRespondentId != 'null') {
                    respondentId = records[0].get('id');
                    return;
                }

                respondentId = records[0].get('id');
                // по умолчанию укажем на первого кредитора
                Ext.getCmp('edRespondent').setValue(respondentId);
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

    // при нажатий кнопки идет обращение в бэк для поиска
    var buttonSearch = Ext.create('Ext.button.Button', {
        id: "buttonSearch",
        text: label_SEARCH,
        handler: function() {
            // сбрасываю значение перед каждым поиском
            isMaintenance = false;

            Ext.getCmp('buttonSearch').disable();

            var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_SEARCHING });
            loadMask.show();

            var entityData = getSearchDataFromTree(searchTree.getRootNode());

            Ext.Ajax.request({
                url: dataUrl + '/core/eav/searchEntity',
                method: 'POST',
                params: {
                    respondentId: Ext.getCmp('edRespondent').value,
                    metaClassId: Ext.getCmp('edMetaClass').value,
                    date: moment(Ext.getCmp('edDate').value).local().format('YYYY-MM-DD')
                },
                jsonData: entityData,
                success: function(response) {
                    loadMask.hide();
                    Ext.getCmp('buttonSearch').enable();

                    var jsonData = JSON.parse(response.responseText);
                    entityStore.setRootNode(jsonData);

                    Ext.getCmp('entityTreeView').getView().refresh();
                },
                failure: function(response) {
                    loadMask.hide();

                    Ext.getCmp('buttonSearch').enable();

                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            });
        },
        maxWidth: 70,
        shadow: true
    });

    var buttonNewEntity = Ext.create('Ext.button.Button', {
        id: "buttonNewEntity",
        text: label_ADD,
        handler: function() {
            var edMetaClass = Ext.getCmp('edMetaClass');
            var classId = edMetaClass.value;
            var metaClass = edMetaClass.findRecordByValue(classId).data;

            showNewEntityWindow(metaClass);
        }
    });

    var searchStore = Ext.create('Ext.data.TreeStore', {
        model: 'entityModel',
        storeId: 'searchStore',
        folderSort: true,
        proxy: {
            type: 'memory'
        }
    });

    var searchTree = Ext.create('Ext.tree.Panel', {
        id: 'searchTreeView',
        preventHeader: true,
        useArrows: true,
        rootVisible: true,
        store: searchStore,
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
        }],
        listeners: {
            itemcontextmenu: function (me, node, item, index, e, eOpts) {
                if (e.button <= 0)
                    return;

                // пункты меню при нажатий правой кнопкой мыши
                var items = [];

                // меню активно только если нажали узел комплексного атрибута
                // добавляем значения по комплексному атрибуту
                // создал отдельное окошко чтобы не было путаницы
                if (!node.data.simple && !node.data.dictionary &&
                    ((node.parentNode && !node.parentNode.data.dictionary) || !node.parentNode)) {
                    items.push({
                        text: label_ADD,
                        handler: function() {
                            showAddSearchParamWindow(node);
                        }
                    });
                }

                // редактирование отдельного атрибута
                if (node.data.dictionary || (node.data.simple && !node.parentNode.data.dictionary))
                    items.push({
                        text: label_EDITING_ATTR,
                        handler: function() {
                            // если справочник то показываем пикер выбора справочника с гридом
                            // если обычный атрибут то диалговое окошко с атрибутом и его значением
                            if (node.data.dictionary) {
                                showDictPicker(node.data.refClassId, node.data.title, function(selData) {
                                    dictChange(node, selData.entity_id, selData.creditor_id, node.data.refClassId, function() {
                                        Ext.getCmp('searchTreeView').getView().refresh();
                                    });
                                });
                            } else {
                                showEditAttrWindow(node, true, function() {
                                    Ext.getCmp('searchTreeView').getView().refresh();
                                });
                            }
                        }
                    });

                // изменение параметров поиска
                // меню позволяет редактировать только комлексные атрибуты
                // то есть запрещает редактировать простые атрибуты или справочники
                if (!node.data.array && !node.data.simple && !node.data.dictionary) {
                    items.push({
                        text: label_CHANGE,
                        handler: function() {
                            var metaClassId = null;
                            var windowTitle = null;

                            if (node.data.depth == 0) {
                                metaClassId = Ext.getCmp('edMetaClass').value;
                                windowTitle = Ext.getCmp('edMetaClass').getRawValue();
                            } else {
                                metaClassId = node.data.refClassId;
                                windowTitle = node.data.title;
                            }

                            showEditEntityWindow(node, metaClassId, windowTitle, true, function() {
                                Ext.getCmp('searchTreeView').getView().refresh();
                            });
                        }
                    });
                }

                // удаление параметра поиска; никаких ограничений, удаляют что хотят
                if (node.data.depth > 0 && !node.parentNode.data.dictionary) {
                    items.push({
                        text: label_DEL,
                        handler: function() {
                            node.parentNode.removeChild(node);
                        }
                    });
                }

                if (items.length == 0)
                    items.push({
                        text: label_NO_AVA_OPER,
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
        }],
        listeners: {
            itemcontextmenu: function (me, node, item, index, e, eOpts) {
                if (e.button <= 0)
                    return;

                // пункты меню при нажатий правой кнопкой мыши
                var items = [];

                // пункты меню: "отправка изменений", "просмотр XML" отображаем только если нажали
                // корневой узел; например: кредит
                if (node.data.depth == 1) {
                    items.push({
                        text: label_SEND_CHANGES,
                        handler: function() {
                            sendXml(null, Ext.getCmp('edDate').value, Ext.getCmp('edMetaClass').value);
                        }
                    });

                    items.push({
                        text: 'XML',
                        handler: function() {
                            showXml();
                        }
                    });
                }

                // меню активно только если нажали узел комплексного атрибута
                // добавляем значения по комплексному атрибуту
                // создал отдельное окошко чтобы не было путаницы
                if (!node.data.simple && !node.data.dictionary &&
                    ((node.parentNode && !node.parentNode.data.dictionary) || !node.parentNode) || node.data.array) {
                    items.push({
                        text: label_ADD,
                        handler: function() {
                            showAddEntityAttrWindow(node);
                        }
                    });
                }

                if ((node.data.dictionary && !node.data.array) || (node.data.simple && !node.data.array && !node.parentNode.data.dictionary))
                    items.push({
                        text: label_EDITING_ATTR,
                        handler: function() {
                            // если справочник то показываем пикер выбора справочника с гридом
                            // если обычный атрибут то диалговое окошко с атрибутом и его значением
                            if (node.data.dictionary) {
                                if (node.parentNode.data.array && node.parentNode.data.dictionary) {
                                    showDictPicker(node.parentNode.data.refClassId, node.data.title, function(selData) {
                                        dictChange(node, selData.entity_id, selData.creditor_id, node.data.refClassId, function() {
                                            Ext.getCmp('entityTreeView').getView().refresh();
                                            editorAction.commitEdit();
                                        });
                                    });
                                } else {
                                    showDictPicker(node.data.refClassId, node.data.title, function(selData) {
                                        dictChange(node, selData.entity_id, selData.creditor_id, node.data.refClassId, function() {
                                            Ext.getCmp('entityTreeView').getView().refresh();
                                            editorAction.commitEdit();
                                        });
                                    });
                                }
                            } else {
                                showEditAttrWindow(node, false, function() {
                                    Ext.getCmp('entityTreeView').getView().refresh();
                                    editorAction.commitEdit();
                                });
                            }
                        }
                    });

                if (!(node.parentNode.data.array && node.parentNode.data.cumulative)) {
                    items.push({
                        text: label_DEL,
                        handler: function() {
                            showDeleteWindow(node);
                        },
                        disabled: node.data.key || node.parentNode.data.dictionary
                    });
                }

                if (items.length == 0)
                    items.push({
                        text: label_NO_AVA_OPER,
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

    var metaClassStore = Ext.create('Ext.data.Store', {
        model: 'metaClassModel',
        id: 'metaClassStore',
        pageSize: 100,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/meta/getMetaClassJsonListByUserId',
            actionMethods: {
                read: 'GET'
            },
            extraParams: {
                userId: userId
            },
            listeners : {
                exception: function(proxy, response, operation, eOpts) {
                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                }
            },
            reader: {
                type: 'json',
                root: ''
            }
        },
        listeners: {
            load: function(me, records, options) {
                var edMetaClass = Ext.getCmp("edMetaClass");

                var defaultMetaClassId = records[0].get('id');
                if (queryMetaClassId && queryMetaClassId != 'null')
                    defaultMetaClassId = Number(queryMetaClassId);

                Ext.getCmp('edMetaClass').setValue(defaultMetaClassId);
            }
        },
        autoLoad: true,
        remoteSort: true
    });

    var mainPanel = Ext.create('Ext.panel.Panel', {
        height: 500,
        renderTo: 'entity-editor-content',
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
                        fieldLabel: 'Метакласс',
                        id: 'edMetaClass',
                        xtype: 'combobox',
                        displayField: 'title',
                        width: '60%',
                        labelWidth: '40%',
                        valueField: 'id',
                        editable: false,
                        listeners: {
                            change: function(a, key, prev) {
                                if (a.store.getRange().length == 0)
                                    return;

                                // добавляем в дерево поиска сущности корень ноду
                                var metaClass = a.findRecordByValue(Number(key)).data;

                                var entityData = {
                                    "title": metaClass.title,
                                    "expanded": true
                                };

                                searchStore.setRootNode(entityData);

                                fillSearchTree(metaClass.id);

                                var rootData = Ext.create('entityModel', {
                                    title: '.',
                                    children: []
                                });

                                entityStore.setRootNode(rootData);
                            }
                        },
                        store: metaClassStore
                    },
                    {
                        id: 'edRespondent',
                        xtype: 'combobox',
                        displayField: 'name',
                        store: respondentStore,
                        labelWidth: 70,
                        valueField: 'id',
                        fieldLabel: label_RESPONDENT,
                        editable: false,
                        width: '60%',
                        labelWidth: '40%'
                    },
                    {
                        xtype: 'datefield',
                        id: 'edDate',
                        fieldLabel: label_DATE,
                        format: 'd.m.Y',
                        value: new Date(),
                        width: '60%',
                        labelWidth: '40%'
                    }
                ]
            },
                {
                    region: 'center',
                    id: 'form-area',
                    title: label_SEARCH_PARA,
                    height: '80%',
                    split: true,
                    items: [searchTree],
                    tbar: [buttonSearch]
                }]
        },
            {
                id: 'gridPanel',
                region: 'center',
                width: "40%",
                split: true,
                items: [entityGrid],
                autoScroll: true,
                tbar: [buttonNewEntity],
                bbar: [
                    {xtype: 'label', text: label_CHANGING},
                    {xtype: 'label', text: label_NO, id: 'lblOperation'}
                ]
            }]
    });

    if (queryEntityId && queryEntityId != 'null') {
        var edDate = Ext.getCmp("edDate");
        edDate.setValue(queryRepDate);

        var edRespondent = Ext.getCmp("edRespondent");
        edRespondent.setValue(Number(queryRespondentId));

        Ext.Ajax.request({
            url: dataUrl + '/core/respondent/getUserRespondentList',
            method: 'GET',
            params: {
                userId: userId
            },
            success: function(response) {
                var jsonData = JSON.parse(response.responseText);
                respondentId = jsonData.data[0].id;

                if (respondentId == Number(queryRespondentId) || respondentId == 0) {
                    var loadMask = new Ext.LoadMask(Ext.getCmp('mainPanel'), {msg: label_LOADING});
                    loadMask.show();

                    Ext.Ajax.request({
                        url: dataUrl + '/core/eav/getEntityData',
                        method: 'GET',
                        params: {
                            entityId: Number(queryEntityId),
                            respondentId: queryRespondentId,
                            metaClassId: queryMetaClassId,
                            date: moment(queryRepDate).local().format('YYYY-MM-DD'),
                            asRoot: true
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
                                title: label_ERROR,
                                msg: error.errorMessage,
                                width : 300,
                                buttons: Ext.MessageBox.YES
                            });
                        }
                    });
                } else {
                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: "У вас нет доступа к данной сущности!",
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                    queryRespondentId = null;
                    queryMetaClassId = null;
                    queryRepDate = null;
                }
            },
            failure: function(response) {
                var error = JSON.parse(response.responseText);

                Ext.Msg.show({
                    title: label_ERROR,
                    msg: error.errorMessage,
                    width : 300,
                    buttons: Ext.MessageBox.YES
                });
            }
        });
    } else {
        queryRespondentId = null;
        queryMetaClassId = null;
        queryRepDate = null;
    }

});
