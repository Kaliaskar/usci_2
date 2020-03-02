/**
 * @author Artur Tkachenko
 * @author Jandos Iskakov
 */

Ext.require([
    'Ext.tab.*',
    'Ext.tree.*',
    'Ext.form.*',
    'Ext.data.*',
    'Ext.tip.*',
    'Ext.grid.*',
    'Ext.panel.*',
    'Ext.button.*'
]);

var metaTreeView = null;
var metaTreeViewStore = null;
var currentClassId = null;

function fillMetaClassTree(className, classTitle, classSync) {
    currentClassId = className;

    if (metaTreeView == null) {
        createMetaClassTree(className, classTitle, classSync);
    } else {
        var root = {
            id: className,
            text: classTitle,
            expanded: true
        };

        metaTreeViewStore.setRootNode(root);
    }
}

function clearExtjsComponent(cmp) {
    var f;
    while (f = cmp.items.first()) {
        cmp.remove(f, true);
    }
}

function createMetaClassTree(className, classTitle, classSync) {
    Ext.QuickTips.init();

    // store для дерева атрибутов в иерахической структуре
    // при каждом нажатий на узел в дереве идет обращение в бэкенд
    // чтобы подгрузить атрибуты комплексного атрибута
    metaTreeViewStore = Ext.create('Ext.data.TreeStore', {
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/meta/getMetaClassAttributesTree'
        },
        root: {
            id: className,
            text: classTitle,
            expanded: true
        },
        folderSort: true,
        sorters: [{
            property: 'text',
            direction: 'ASC'
        }]
    });

    // меню операций с мета атрибутами: изменить, добавить, удалить
    var metaTreeMenu = new Ext.menu.Menu({
        items: [
            {
                id: 'mtm-edit',
                text: LABEL_EDIT
            },
            {
                id: 'mtm-add',
                text: LABEL_ADD
            },
            {
                id: 'mtm-del',
                text: LABEL_DEL
            }
        ],
        listeners: {
            click: function(menu, item, e, eOpts) {
                var tree = Ext.getCmp('metaTreeView');
                var selectedNode = tree.getSelectionModel().getLastSelected();

                var parentPath = null;

                var classId = selectedNode.raw.classId;

                if (selectedNode.parentNode != null) {
                    parentPath = selectedNode.parentNode.data.id;
                } else {
                    parentPath = currentClassId;
                }

                switch (item.id) {
                    case 'mtm-del':
                        attrPath = selectedNode.data.id;
                        attrPathCode = null;
                        attrPathPart = null;

                        if (attrPath != null) {
                            pathArray = attrPath.split(".");

                            attrPathCode = pathArray[pathArray.length - 1];

                            attrPathPart = attrPath.substring(0, attrPath.length - (attrPathCode.length + 1));
                        }

                        Ext.Ajax.request({
                            url: dataUrl + '/core/meta/delMetaAttribute',
                            waitMsg: LABEL_ADDING,
                            params : {
                                attrPathPart: attrPathPart,
                                attrPathCode: attrPathCode
                            },
                            method: 'POST',
                            success: function(response, opts) {
                                selectedNode.parentNode.removeChild(selectedNode);
                            },
                            failure: function(response, opts) {
                                Ext.Msg.alert(label_ERROR, JSON.parse(response.responseText).errorMessage);
                            }
                        });

                        break;
                    case 'mtm-edit':
                        showMetaAttrWindow(currentClassId,
                            parentPath,
                            classSync,
                            selectedNode.data.id,
                            selectedNode.raw.data,
                            function() {
                                metaTreeViewStore.reload();
                            });
                        break;
                    case 'mtm-add':
                        showMetaAttrWindow(currentClassId,
                            parentPath, classSync, null, null,
                            function() {
                                metaTreeViewStore.reload();
                            });
                        break;
                }
            }
        }
    });

    // собственно само дерево атрибутов мета класса
    metaTreeView = Ext.create('Ext.tree.Panel', {
        store: metaTreeViewStore,
        id: 'metaTreeView',
        viewConfig: {
            plugins: {
                ptype: 'treeviewdragdrop'
            }
        },
        height: '100%',
        autoHeight: true,
        preventHeader: true,
        useArrows: true,
        listeners : {
            itemcontextmenu: function(view, record, item, index, event, eOpts) {
                if (!isDataManager)
                    return;
                var tree = Ext.getCmp('metaTreeView');
                var sync = Ext.getCmp('metaClassGrid').getSelectionModel().getLastSelected().data.sync;
                var selectedNode = tree.getSelectionModel().getLastSelected();

                // мета класс отображается как корень всего дерева
                // запрещаем редактировать сам мета класс
                if (selectedNode.data.id == currentClassId) {
                    metaTreeMenu.getComponent('mtm-edit').disable(true);
                    if (sync == true) {
                        metaTreeMenu.getComponent('mtm-del').disable(true);
                    }
                    else {
                        metaTreeMenu.getComponent('mtm-del').enable(true);
                    }
                }
                else {
                    metaTreeMenu.getComponent('mtm-edit').enable(true);
                    if (selectedNode.raw.data.isSync == true) {
                        metaTreeMenu.getComponent('mtm-del').disable(true);
                    }
                    else {
                        metaTreeMenu.getComponent('mtm-del').enable(true);
                    }
                }

                metaTreeMenu.showAt(event.getXY());
                event.stopEvent();
            }
        }
    });

    metaClassTreeContainer = Ext.getCmp('metaClassTreeContainer');
    clearExtjsComponent(metaClassTreeContainer);

    metaClassTreeContainer.add(metaTreeView);
    metaClassTreeContainer.doLayout();
}

function createMetaClassTreeStub(classId, className) {
    return Ext.create('Ext.panel.Panel', {
        preventHeader: true,
        html: LABEL_CHOOSE
    });
}

Ext.onReady(function() {
    Ext.define('metaClassModel', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'id', type: 'number'},
            {name: 'name', type: 'string'},
            {name: 'title', type: 'string'},
            {name: 'periodTypeId', type: 'number'},
            {name: 'hashSize', type: 'number'},
            {name: 'deleted', type: 'boolean'},
            {name: 'dictionary', type: 'boolean'},
            {name: 'operational', type: 'boolean'},
            {name: 'sync', type: 'boolean'}
        ]
    });

    var metaClassStore = Ext.create('Ext.data.Store', {
        id: 'metaClassStore',
        model: 'metaClassModel',
        autoLoad: true,
        remoteSort: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/meta/getMetaClasses',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    // создает грид мета классов
    // здесь идет вывод краткой информаций по мета классам в гриде
    var metaClassGrid = Ext.create('Ext.grid.Panel', {
        id: "metaClassGrid",
        store: metaClassStore,
        columns: [
            {
                header: '',
                hidden: !isDataManager,
                xtype: 'actioncolumn',
                width: 26,
                sortable: false,
                items: [{
                    icon: contextPathUrl + '/pics/edit.png',
                    tooltip: LABEL_EDIT,
                    handler: function (grid, rowIndex, colIndex) {
                        var record = metaClassStore.getAt(rowIndex);

                        // открываем окно для редактирования записи мета класса
                        showMcWindow(record.get('id'));
                    }}
                ]
            },
            {
                text : LABEL_CODE,
                dataIndex: 'name',
                flex: 1
            },
            {
                text : LABEL_TITLE,
                dataIndex: 'title',
                flex: 1
            },
            {
                xtype: 'checkcolumn',
                text : LABEL_REFERENCE,
                dataIndex: 'dictionary',
                listeners: {
                    beforecheckchange: function() {
                        return false;
                    }
                },
                flex: 1
            },
            {
                xtype: 'checkcolumn',
                text : label_OPER,
                dataIndex: 'operational',
                // единственный способ сделать столбец reaonly
                // сам ExtJs не предоставляет readonly
                listeners: {
                    beforecheckchange: function() {
                        return false;
                    }
                },
                flex: 1
            }
        ],
        viewConfig: {
            forceFit: true,
            getRowClass: function(record, index) {
                var record = metaClassStore.getAt(index);
                var deleted = record.get('deleted');
                return deleted === 1? 'disable': 'enable';
            }
        },
        xtype : 'panel',
        region: 'west',
        width: 300,
        collapsible: true,
        split: true,
        minSize: 50,
        title: LABEL_CLASSES,
        listeners : {
            cellclick: function(grid, td, cellIndex, record, tr, rowIndex, e, eOpts) {
                if (cellIndex > 0) {
                    fillMetaClassTree(record.get('name'), record.get('title'), record.get('sync'));
                }
            },
            itemdblclick: function(dv, record, item, index, e) {
                showMcWindow(Number(record.get('id')));
            }
        },
        dockedItems: [{
            xtype: 'toolbar',
            hidden: !isDataManager,
            items: [
                {
                    text: LABEL_ADD,
                    icon: contextPathUrl + '/pics/add.png',
                    handler: function() {
                        showMcWindow(null);
                    }
                },
                {
                    text: 'Создать обьекты в БД',
                    icon: contextPathUrl + '/pics/download.png',
                    handler: function() {
                        var loadMask = new Ext.LoadMask(Ext.getBody(), {
                            msg: label_SYNCING
                        });

                        loadMask.show();

                        Ext.Ajax.request({
                            url: dataUrl + '/core/meta/syncWithDb',
                            waitMsg: label_SYNCING,
                            method: 'POST',
                            success: function(response, opts) {
                                loadMask.hide();
                                Ext.Msg.alert(label_INFO, label_SUC_SYNC);
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
                }]
        }]
    });

    Ext.create('Ext.panel.Panel', {
        title: LABEL_DATA_PANEL,
        preventHeader: true,
        width: '100%',
        height: '500px',
        renderTo: "meta-editor-content",
        layout: 'border',
        defaults: {
            padding: '3'
        },
        items: [
            metaClassGrid,
            {
                xtype : 'panel',
                id: 'metaClassTreeContainer',
                region: 'center',
                title : LABEL_CLASS_ST,
                scroll: 'both',
                autoScroll: true,
                items: [
                    {
                        xtype: 'panel',
                        preventHeader: true,
                        html: LABEL_CHOOSE
                    }
                ],
                dockedItems: [{
                    xtype: 'toolbar',
                    items: [
                        {
                            text: LABEL_UNFOLD,
                            handler: function() {
                                metaTreeView.expandAll();
                            }
                        },
                        {
                            text: LABEL_FOLD,
                            handler: function() {
                                metaTreeView.collapseAll();
                            }
                        }]
                }]
            }]
    });
});