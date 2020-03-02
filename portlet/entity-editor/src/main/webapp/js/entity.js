/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

function showNewEntityWindow(metaClass, callback) {
    var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_LOADING});
    loadMask.show();

    var form = Ext.create('Ext.form.Panel', {
        bodyPadding: '5 5 0',
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        height: 400,
        autoScroll: true
    });

    var suffix = '_new_ent';
    var windowId = 'windowNewEntity';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    loadAttributes(metaClass.id, function(attributes) {
        // добавляем каждый атрибут в форму
        for (var i = 0; i < attributes.length; i++) {
            form.add(getFormField(attributes[i], null, suffix));
        }

        var window = Ext.create("Ext.Window", {
            id: windowId,
            title: label_ADD_NOTE,
            bodyPadding: '2',
            width: 600,
            modal: true,
            closable: true,
            closeAction: 'hide',
            autoScroll: true,
            items: [form],
            tbar : [{
                text : label_SAVE,
                handler: function() {
                    if (!form.isValid())
                        return;

                    // очищаем дерево и добавляем совершенно новую запись сущности
                    var tree = Ext.getCmp('entityTreeView');

                    var rootNode = tree.getRootNode();
                    rootNode.removeAll();

                    rootNode.appendChild({
                        leaf: false,
                        title: metaClass.title,
                        name: metaClass.name,
                        metaType: "META_CLASS",
                        expanded: true
                    });

                    var entityNode = rootNode.getChildAt(0);

                    saveFormValues(entityNode, attributes, suffix);

                    tree.getView().refresh();

                    window.close();

                    if (callback)
                        callback();
                }
            }]
        });

        loadMask.hide();

        window.show();
    });
}

// добавление атрибута или элемента сета
function showAddEntityAttrWindow(node) {
    var metaClassId = null;
    var windowTitle = null;

    if (node.data.depth == 1) {
        metaClassId = Ext.getCmp('edMetaClass').value;
        windowTitle = Ext.getCmp('edMetaClass').getRawValue();
    } else if (node.parentNode.data.array) {
        metaClassId = node.parentNode.data.refClassId;

        windowTitle = node.parentNode.data.title;
    } else {
        metaClassId = node.data.refClassId;
        windowTitle = node.data.title;
    }

    if (node.data.array) {
        if (node.data.simple) {
            showAddArrayAttrWindow(node, windowTitle, false, function(form) {
                editorAction.commitEdit();
                Ext.getCmp('entityTreeView').getView().refresh();
            });
        } else if (node.data.dictionary){
            showArrayNewDictElWindow(metaClassId, node, windowTitle, false, function() {
                editorAction.commitEdit();
                Ext.getCmp('entityTreeView').getView().refresh();
            });
        } else {
            showArrayNewElWindow(metaClassId, node, windowTitle, false, function() {
                editorAction.commitEdit();
                Ext.getCmp('entityTreeView').getView().refresh();
            });
        }
    } else {
        showAddAttrWindow(node, metaClassId, windowTitle, false, function(form) {
            editorAction.commitEdit();
            Ext.getCmp('entityTreeView').getView().refresh();
        });
    }
}

// редактирование сущности
function showEditEntityWindow(node, metaClassId, windowTitle, searching, callback) {
    var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_LOADING});
    loadMask.show();

    var suffix = '_edit_ent';
    var windowId = 'windowEditEntity';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    var form = Ext.create('Ext.form.Panel', {
        bodyPadding: '5 5 0',
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        height: 400,
        autoScroll: true
    });

    loadAttributes(metaClassId, function(attributes) {
        var editableAttrs = [];
        for (var i = 0; i < attributes.length; i++) {
            var attribute = attributes[i];

            // внимание!!! костыль для справочника ref_portfolio
            // атрибуты creditor, code не даем на редактирование
            if (attribute.id == 111 || attribute.id == 194)
                continue;

            if (attribute.array || (!attribute.dictionary && !attribute.simple))
                continue;

            var editNode = null;
            for (var j = 0; j < node.childNodes.length; j++) {
                if (attribute.name === node.childNodes[j].data.name) {
                    editNode = node.childNodes[j];
                }
            }

            if (editNode) {
                editableAttrs.push(attribute);
                form.add(getFormField(attribute, editNode.data.value, suffix));
            }
        }

        if (editableAttrs.length == 0) {
            loadMask.hide();
            Ext.MessageBox.alert("", label_NO_AVA_ATTR);

            return;
        }

        var window = Ext.create("Ext.Window", {
            id: windowId,
            title: label_EDITING + windowTitle,
            bodyPadding: '2',
            width: 600,
            modal: true,
            closable: true,
            closeAction: 'hide',
            items: [form],
            tbar: [{
                text: label_SAVE_CHANGES,
                handler: function() {
                    if (!form.isValid())
                        return;

                    saveFormValues(node, editableAttrs, suffix, searching, false);

                    window.close();

                    if (callback)
                        callback(form);
                }
            }]
        });

        loadMask.hide();

        window.show();
    });
}

function showAddAttrWindow(node, metaClassId, windowTitle, searching, callback) {
    var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_LOADING});
    loadMask.show();

    var form = Ext.create('Ext.form.Panel', {
        bodyPadding: '5 5 0',
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        height: 400,
        autoScroll: true
    });

    var windowId = 'windowAddEntityAttr';
    var suffix = '_add_attr_ent';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    loadAttributes(metaClassId, function(attributes) {
        // в форму добавляем только атрибуты которые отсутствуют у сущности
        // для этого берем из бэка все атрибуты сущности
        // если нечего добавить то форма закрывается и выводится окно что все атрибуты добавлены
        var editableAttrs = [];
        for (var i = 0; i < attributes.length; i++) {
            var attribute = attributes[i];

            // если делаем поиск то только выбираем только ключевые атрибуты
            if (searching && !attribute.key)
                continue;

            var alreadyHas = false;
            for (var j = 0; j < node.childNodes.length; j++) {
                if (attribute.name === node.childNodes[j].data.name) {
                    alreadyHas = true;
                }
            }

            if (!alreadyHas) {
                editableAttrs.push(attribute);

                form.add(getFormField(attribute, null, suffix));
            }
        }

        if (editableAttrs.length == 0) {
            loadMask.hide();
            Ext.MessageBox.alert("", label_ALL_INFO + windowTitle + label_ALREADY_ADDED);

            return;
        }

        var window = Ext.create("Ext.Window", {
            id: windowId,
            title: label_ADD_TO + windowTitle,
            bodyPadding: '2',
            width: 600,
            modal: true,
            closable: true,
            closeAction: 'hide',
            items: [form],
            tbar: [{
                text: label_SAVE_NEW_RECORD,
                handler: function() {
                    if (!form.isValid())
                        return;

                    loadMask.show();

                    saveFormValues(node, editableAttrs, suffix, searching, true);

                    window.close();
                    callback(form);

                    loadMask.hide();
                }
            }]
        });

        loadMask.hide();

        window.show();
    });
}

// показывает окно ввода нового элемента сета
function showArrayNewElWindow(metaClassId, arrayNode, windowTitle, searching, callback) {
    var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_LOADING});
    loadMask.show();

    var windowId = 'windowArrayNewEl';
    var suffix = '_add_array_el';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    var form = Ext.create('Ext.form.Panel', {
        bodyPadding: '5 5 0',
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        height: 400,
        autoScroll: true
    });

    loadAttributes(metaClassId, function(attributes) {
        var window = Ext.create("Ext.Window", {
            id: windowId,
            title: label_ADD_TO + windowTitle,
            bodyPadding: '2',
            width: 600,
            modal: true,
            closable: true,
            closeAction: 'destroy',
            items: [form],
            tbar: [{
                text: label_SAVE_NEW_RECORD,
                handler: function() {
                    if (!form.isValid())
                        return;

                    // в значениях узла сета инкрементируем кол-вол записей
                    arrayNode.data.value = arrayNode.childNodes.length + 1;

                    // создаем новую ноду элемента сета
                    var newNode = Ext.create('entityModel', {
                        title: '[' + arrayNode.childNodes.length + ']',
                        name: '[' + arrayNode.childNodes.length + ']',
                        simple: false,
                        array: false,
                        value: arrayNode.childNodes.length + 1
                    });

                    saveFormValues(newNode, attributes, suffix, searching, true);

                    arrayNode.appendChild(newNode);

                    window.close();

                    if (callback)
                        callback();
                }
            }]
        });

        for (var i = 0; i < attributes.length; i++) {
            var attribute = attributes[i];

            // если делаем поиск то выбираем только ключевые атрибуты
            if (searching && !attribute.key)
                continue;

            form.add(getFormField(attribute, null, suffix));
        }

        loadMask.hide();

        window.show();
    });
}

function showArrayNewDictElWindow(metaClassId, arrayNode, windowTitle, searching, callback) {
    var loadMask = new Ext.LoadMask(Ext.getBody(), {msg: label_LOADING});
    loadMask.show();

    var windowId = 'windowArrayNewDictEl';
    var suffix = '_add_array_dictel';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    var form = Ext.create('Ext.form.Panel', {
        bodyPadding: '5 5 0',
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        height: 400,
        autoScroll: true
    });

    // в значениях узла сета инкрементируем кол-вол записей
    arrayNode.data.value = arrayNode.childNodes.length + 1;

    // создаем новую ноду элемента сета
    var newNode = Ext.create('entityModel', {
        title: '[' + arrayNode.childNodes.length + ']',
        name: '[' + arrayNode.childNodes.length + ']',
        simple: false,
        array: true,
        dictionary: true,
        refClassId: metaClassId,
        value: arrayNode.childNodes.length + 1
    });

    form.add(getFormField(newNode.data, null, suffix));

    loadAttributes(metaClassId, function(attributes) {
        var window = Ext.create("Ext.Window", {
            id: windowId,
            title: label_ADD_TO + windowTitle,
            bodyPadding: '2',
            width: 600,
            modal: true,
            closable: true,
            closeAction: 'destroy',
            items: [form],
            tbar: [{
                text: label_SAVE_NEW_RECORD,
                handler: function() {
                    if (!form.isValid())
                        return;

                    saveFormValues(newNode, [newNode.data], suffix, searching, true);

                    arrayNode.appendChild(newNode);

                    window.close();

                    if (callback)
                        callback();
                }
            }]
        });

        loadMask.hide();

        window.show();
    });
}

// открывает окно формы добавления
// аттрибута для массива простых
function showAddArrayAttrWindow(node, windowTitle, searching, callback) {
    var suffix = '_add_array_simplel';
    var windowId = 'windowAddArraySimpleEl';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    var form = Ext.create('Ext.form.Panel', {
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        autoScroll: true
    });

    node.data.value = node.childNodes.length + 1;

    // создаем новую ноду элемента сета
    var newNode = Ext.create('entityModel', {
        title: '[' + node.childNodes.length + ']',
        name: '[' + node.childNodes.length + ']',
        simple: true,
        array: true,
        metaType: node.data.typeCode,
        value: node.childNodes.length + 1
    });

    form.add(getFormField(newNode.data, null, suffix));

    var window = Ext.create("Ext.Window", {
        id: windowId,
        title: label_ADD_TO + windowTitle,
        width: 400,
        modal: true,
        closable: true,
        closeAction: 'destroy',
        items: [form],
        tbar: [{
            text: label_SAVE_NEW_RECORD,
            handler: function() {
                if (!form.isValid())
                    return;

                saveFormValues(newNode, [newNode.data], suffix, searching, true);

                node.appendChild(newNode);

                window.close();

                callback();
            }
        }]
    }).show();
}

function getFormField(attr, value, suffix, hideLabels = false) {
    var readOnly = false;
    var allowBlank = !(attr.required || attr.key);

    var formField = null;

    var fieldId = attr.name + "item" + suffix;
    var labelWidth = '60%', width = '40%';
    var fieldLabel = (!allowBlank ? "<b style='color:red'>*</b> " : "") + attr.title;

    if (hideLabels) {
        labelWidth = '0%';
        width = '100%';
        fieldLabel = '';
    }

    if (attr.array && suffix != '_add_array_simplel' && suffix != '_add_array_dictel') {
        formField = Ext.create("UsciCheckboxField", {
            id: fieldId,
            fieldLabel: fieldLabel,
            labelWidth: labelWidth,
            cls: 'checkBox',
            width: width,
            readOnly: readOnly,
            allowBlank: allowBlank,
            blankText: label_REQUIRED_FIELD,
            checked: (attr.key || attr.value)
        });
    } else if (attr.metaType == "DATE") {
        formField = Ext.create("Ext.form.field.Date", {
            id: fieldId,
            fieldLabel: fieldLabel,
            labelWidth: labelWidth,
            width: width,
            format: 'd.m.Y',
            value: value? new Date(value.replace(/(\d{2})\.(\d{2})\.(\d{4})/,'$3-$2-$1')): null,
            readOnly: readOnly,
            allowBlank: allowBlank,
            blankText: label_REQUIRED_FIELD
        });
    } else if (attr.metaType == "DATE_TIME") {
        formField = createDateTimePicker(fieldLabel, labelWidth, fieldId, width, value, allowBlank, readOnly);
    } else if (attr.metaType == "INTEGER" || attr.metaType == "DOUBLE") {
        formField = Ext.create("Ext.form.field.Number", {
            id: fieldId,
            fieldLabel: fieldLabel,
            labelWidth: labelWidth,
            width: width,
            value: value,
            allowDecimals: attr.metaType == "DOUBLE",
            readOnly: readOnly,
            allowBlank: allowBlank,
            blankText: label_REQUIRED_FIELD
        });
    } else if (attr.metaType == "BOOLEAN") {
        formField = Ext.create("Ext.form.field.ComboBox", {
            id: fieldId,
            fieldLabel: fieldLabel,
            labelWidth: labelWidth,
            width: width,
            readOnly: readOnly,
            allowBlank: allowBlank,
            blankText: label_REQUIRED_FIELD,
            editable: false,
            store: Ext.create('Ext.data.Store', {
                fields: ['value', 'title'],
                data: [
                    {value: true, title: label_YES},
                    {value: false, title: label_NO}
                ]
            }),
            displayField: 'title',
            valueField: 'value',
            value: value
        });
    } else if (attr.dictionary) {
        // внимание!!! костыль для справочника ref_portfolio
        // в атрибут creditor вставляю id кредитора
        // значение кредитора беру из бэка, то есть определяю кредитора из пользователя
        // также делаю поле невидимым
        if (!isNb && attr.id === 194) {
            Ext.Ajax.request({
                async: false,
                url: dataUrl + '/core/respondent/getRespondentByUserId',
                method: 'GET',
                params: {
                    userId: userId,
                    isNb: isNb
                },
                failure: function(response) {
                    var error = JSON.parse(response.responseText);

                    Ext.Msg.show({
                        title: label_ERROR,
                        msg: error.errorMessage,
                        width : 300,
                        buttons: Ext.MessageBox.YES
                    });
                },
                success: function (response) {
                    var data = JSON.parse(response.responseText);
                    formField = Ext.create("Ext.form.field.Text", {
                        id: fieldId,
                        fieldLabel: fieldLabel,
                        labelWidth: labelWidth,
                        width: width,
                        value: data.id,
                        readOnly: readOnly,
                        allowBlank: allowBlank,
                        blankText: label_REQUIRED_FIELD,
                        hidden: true
                    });
                }
            });
        }
        else {
            var edDictEntityId = Ext.create("Ext.form.field.Text", {
                id: fieldId,
                value: value,
                hidden: true
            });

            var edDictRespondentId = Ext.create("Ext.form.field.Text", {
                id: fieldId+"RESP",
                value: value,
                hidden: true
            });

            var edDictText = Ext.create("Ext.form.field.Text", {
                readOnly: true,
                width: '92%',
                value: value,
                allowBlank: allowBlank,
                blankText: label_REQUIRED_FIELD
            });

            var buttonDictPick = Ext.create('Ext.Button', {
                text: '...',
                width: 25,
                handler: function() {
                    showDictPicker(attr.refClassId, attr.title, function(selData) {
                        edDictEntityId.setValue(selData.entity_id);
                        edDictRespondentId.setValue(selData.creditor_id);

                        Ext.Ajax.request({
                            url: dataUrl + '/core/meta/getMetaClassData',
                            method: 'GET',
                            params: {
                                metaClassId: attr.refClassId
                            },
                            failure: function(response) {
                                var error = JSON.parse(response.responseText);

                                Ext.Msg.show({
                                    title: label_ERROR,
                                    msg: error.errorMessage,
                                    width : 300,
                                    buttons: Ext.MessageBox.YES
                                });
                            },
                            success: function (response) {
                                var data = JSON.parse(response.responseText);
                                edDictText.setValue(selData[data.uiConfig.displayField]);
                            }
                        });
                    });
                }
            });

            var panelDict = Ext.create('Ext.form.FieldContainer', {
                fieldLabel: fieldLabel,
                labelWidth: labelWidth,
                width: width,
                preventHeader: true,
                border: 0,
                layout: {
                    type: 'hbox',
                    align: 'stretch'
                },
                items: [edDictText, buttonDictPick, edDictEntityId]
            });

            formField = panelDict;
        }
    } else if (attr.metaType == "STRING") {
        formField = Ext.create("Ext.form.field.Text", {
            id: fieldId,
            fieldLabel: fieldLabel,
            labelWidth: labelWidth,
            width: width,
            value: value,
            readOnly: readOnly,
            allowBlank: allowBlank,
            blankText: label_REQUIRED_FIELD
        });

        // внимание!!! костыль для справочника ref_portfolio
        // в поле код подставляю максимальный код из всего справочника
        // делаю поле невидимым
        if (attr.id === 111) {
            Ext.Ajax.request({
                url: dataUrl + '/core/dict/getDictEntities',
                params: {
                    metaId : 59,
                    userId: userId,
                    isNb: isNb,
                    reportDate: new Date().toISOString().slice(0, 10)
                },
                async: false,
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
                success: function(result) {
                    var json = JSON.parse(result.responseText).data;

                    maxId = 0;
                    for (i = 0; i< json.length; i++) {
                        maxId = Math.max(maxId, parseInt(json[i].code));
                    }

                    maxId++;

                    formField.hidden = true;
                    formField.setValue(maxId);
                }
            });
        }
    } else if (!attr.simple && (suffix == '_new_ent' || suffix == '_add_attr_ent' || suffix == '_add_array_el')) {
        // если идет создание новой сущности или добавление атрибутов
        // отображаем комплексные сущности как галочки
        formField = Ext.create("Ext.form.field.Checkbox", {
            id: fieldId,
            fieldLabel: fieldLabel,
            labelWidth: labelWidth,
            cls: 'checkBox',
            width: width,
            readOnly: false,
            checked: false
        });
    } else if (suffix == '_add_array_simplel') {
        if (attr.typeCode == "DATE") {
            formField = Ext.create("Ext.form.field.Date", {
                id: fieldId,
                fieldLabel: fieldLabel,
                labelWidth: labelWidth,
                width: width,
                format: 'd.m.Y',
                value: value? new Date(value.replace(/(\d{2})\.(\d{2})\.(\d{4})/,'$3-$2-$1')): null,
                readOnly: readOnly,
                allowBlank: allowBlank,
                blankText: label_REQUIRED_FIELD
            });
        } else if (attr.typeCode == "DATE_TIME") {
            formField = createDateTimePicker(fieldLabel, labelWidth, fieldId, width, value, allowBlank, readOnly);
        } else if (attr.typeCode == "INTEGER" || attr.typeCode == "DOUBLE") {
            formField = Ext.create("Ext.form.field.Number", {
                id: fieldId,
                fieldLabel: fieldLabel,
                labelWidth: labelWidth,
                width: width,
                value: value,
                allowDecimals: attr.metaType == "DOUBLE",
                readOnly: readOnly,
                allowBlank: allowBlank,
                blankText: label_REQUIRED_FIELD
            });
        } else if (attr.typeCode == "BOOLEAN") {
            formField = Ext.create("Ext.form.field.ComboBox", {
                id: fieldId,
                fieldLabel: fieldLabel,
                labelWidth: labelWidth,
                width: width,
                readOnly: readOnly,
                allowBlank: allowBlank,
                blankText: label_REQUIRED_FIELD,
                editable: false,
                store: Ext.create('Ext.data.Store', {
                    fields: ['value', 'title'],
                    data: [
                        {value: true, title: label_YES},
                        {value: false, title: label_NO}
                    ]
                }),
                displayField: 'title',
                valueField: 'value',
                value: value
            });
        } else if (attr.typeCode == "STRING") {
            formField = Ext.create("Ext.form.field.Text", {
                id: fieldId,
                fieldLabel: fieldLabel,
                labelWidth: labelWidth,
                width: width,
                value: value,
                readOnly: readOnly,
                allowBlank: allowBlank,
                blankText: label_REQUIRED_FIELD
            });

            // внимание!!! костыль для справочника ref_portfolio
            // в поле код подставляю максимальный код из всего справочника
            // делаю поле невидимым
            if (attr.id === 111) {
                Ext.Ajax.request({
                    url: dataUrl + '/core/dict/getDictEntities',
                    params: {
                        metaId : 59,
                        userId: userId,
                        isNb: isNb,
                        reportDate: new Date().toISOString().slice(0, 10)
                    },
                    async: false,
                    method: 'GET',
                    failure: function(response) {
                        var error = JSON.parse(response.responseText);

                        Ext.Msg.show({
                            title: 'Ошибка',
                            msg: error.errorMessage,
                            width : 300,
                            buttons: Ext.MessageBox.YES
                        });
                    },
                    success: function(result) {
                        var json = JSON.parse(result.responseText).data;

                        maxId = 0;
                        for (i = 0; i< json.length; i++) {
                            maxId = Math.max(maxId, parseInt(json[i].code));
                        }

                        maxId++;

                        formField.hidden = true;
                        formField.setValue(maxId);
                    }
                });
            }
        }
    }

    return formField;
}

// переносит значения из полей форм в дерево сущности
// если идет добавление сущности или элемента в сет то создает новые узлы в дереве
function saveFormValues(treeNode, attributes, suffix, searching = false, newNode = true) {
    var entityId = null;
    var tree = null;

    if (!searching) {
        tree = Ext.getCmp('entityTreeView');
        var rootNode = tree.getRootNode();
        entityId = rootNode.childNodes[0].data.value;
    }

    if (treeNode.data.array && treeNode.data.simple) {
        var attribute = attributes[0];
        var field = Ext.getCmp(attribute.name + "item" + suffix);

        var fieldValue;

        if (attribute.metaType == "DATE") {
            fieldValue = field.getSubmitValue();
        } else if (attribute.metaType == "DATE_TIME") {
            fieldValue = field.getValueAsString();
        } else {
            fieldValue = field.getValue();
        }

        var currNode;

        currNode = treeNode;

        var valueBefore = currNode.data.value;
        currNode.data.value = fieldValue;
        currNode.data.array = false;

        if (entityId && attribute.key) {
            currNode.data.oldValue = currNode.data.oldValue? currNode.data.oldValue: valueBefore;
            isMaintenance = true;
        }

        currNode.data.leaf = true;
        currNode.data.iconCls = 'file';
        currNode.data.newNode = newNode;

    } else if (treeNode.data.array && treeNode.data.dictionary) {
        var attribute = attributes[0];

        var field = Ext.getCmp(attribute.name + "item" + suffix);
        var fieldResp = Ext.getCmp(attribute.name + "item" + suffix + "RESP");

        var fieldValue;
        var fieldRespValue = fieldResp.getValue();

        if (attribute.metaType == "DATE") {
            fieldValue = field.getSubmitValue();
        } else if (attribute.metaType == "DATE_TIME") {
            fieldValue = field.getValueAsString();
        } else {
            fieldValue = field.getValue();
        }

        var currNode;

        currNode = treeNode;

        var valueBefore = currNode.data.value;
        currNode.data.value = fieldValue;
        currNode.data.array = false;

        currNode.data.leaf = false;
        currNode.data.iconCls = 'folder';
        currNode.data.newNode = newNode;

        if (fieldValue)
            dictChange(currNode, fieldValue, fieldRespValue, attribute.refClassId);

    } else {
        for (var i = 0; i < attributes.length; i++) {
            var attribute = attributes[i];

            var field = Ext.getCmp(attribute.name + "item" + suffix);

            if (!field)
                continue;

            var fieldValue;

            if (attribute.metaType == "DATE") {
                fieldValue = field.getSubmitValue();
            } else if (attribute.metaType == "DATE_TIME") {
                fieldValue = field.getValueAsString();
            } else {
                fieldValue = field.getValue();
            }

            // если создаем новую сущность но значение отсутствует то
            // такое значение не отображаем в дереве
            if (!fieldValue && (suffix == '_new_ent' || suffix == '_add_attr_ent' || suffix == '_add_array_el'))
                continue;

            // получаем узел в дереве сущности чтобы обновить значение атрибута
            var existingAttrNode = treeNode.findChild('name', attribute.name);
            if (suffix == '_edit_ent_attr')
                existingAttrNode = treeNode;

            var currNode;

            // проверим если идет изменение то просто обновляем значение
            // если идет добавление то добавляем узел к родительскому узлу
            if (existingAttrNode) {
                currNode = existingAttrNode;
            } else {
                if (Ext.getCmp('entityTreeView').store.getNodeById(attribute.id)) {
                    attribute.id = attribute.id + treeNode.id;
                }
                treeNode.appendChild(attribute);
                currNode = treeNode.getChildAt(treeNode.childNodes.length - 1);
            }

            var valueBefore = currNode.data.value;
            currNode.data.value = fieldValue;

            if (attribute.simple && !attribute.array) {
                // изменение ключей сущности;
                // по новым сущностям изменения не фиксируем
                // сохраняем старый ключ в переменной oldValue
                // помечаем переменную isMaintenance=true что означет батч пойдет как на сопровождение
                if (entityId && attribute.key) {
                    currNode.data.oldValue = currNode.data.oldValue? currNode.data.oldValue: valueBefore;
                    isMaintenance = true;
                }

                currNode.data.leaf = true;
                currNode.data.iconCls = 'file';
            } else {
                currNode.data.leaf = false;
                currNode.data.iconCls = 'folder';

                // подгружаем справочник
                if ((attribute.dictionary && attribute.metaType == "META_CLASS") /*|| (attribute.dictionary && attribute.metaType == "META_SET")*/) {

                    var fieldResp = Ext.getCmp(attribute.name + "item" + suffix + "RESP");
                    var fieldRespValue = fieldResp.getValue();
                    // если не выбрали запись в справочнике
                    if (fieldValue)
                        dictChange(currNode, fieldValue, fieldRespValue, attribute.refClassId);
                }
            }
            currNode.data.newNode = newNode
        }
    }
}

// открывает окно формы редактирования
// отображает только один атрибут
function showEditAttrWindow(node, searching, callback) {
    var suffix = '_edit_ent_attr';
    var windowId = 'windowEditEntityAttr';

    if (Ext.getCmp(windowId))
        Ext.getCmp(windowId).destroy();

    var form = Ext.create('Ext.form.Panel', {
        width: '100%',
        defaults: {
            anchor: '100%'
        },
        autoScroll: true
    });

    var editField = getFormField(node.data, node.data.value, suffix, true);

    form.add(editField);

    var editorWindow = Ext.create("Ext.Window", {
        id: windowId,
        title: node.data.title,
        width: 400,
        modal: true,
        closable: true,
        closeAction: 'hide',
        items: [form],
        tbar: [{
            text: label_UPDATE_RECORD,
            handler: function() {
                if (!form.isValid())
                    return;

                saveFormValues(node, [node.data], suffix, searching, false);

                editorWindow.close();

                callback();
            }
        }]
    }).show();
}


// удаляет сущность или атрибут
function showDeleteWindow(node) {
    var rootNode = Ext.getCmp('entityTreeView').getRootNode();
    var entityId = rootNode.childNodes[0].data.value;
    var isNewNode = false;

    if (node.data.newNode !== null && node.data.newNode !== "undefined")
        isNewNode = node.data.newNode
    // существующая сущность то перед удалением запрашиваем подтверждение
    // также помечаем атрибут как удаленным
    if (entityId && !isNewNode) {
        Ext.MessageBox.alert({
            title: label_APLLY_DELETE,
            msg: label_ARE_YOU_SURE_DELETE + node.data.title + label_MEAN + node.data.value,
            buttons: Ext.MessageBox.YESNO,
            buttonText: {
                yes: label_YES,
                no: label_NO
            },
            fn: function(val) {
                if (val == 'yes') {
                    node.data.markedAsDeleted = true;
                    Ext.MessageBox.alert("", label_SUCC_OPERATION + label_DATA_SAVE);
                    node.set('iconCls','deleted');
                    editorAction.commitEdit();
                }
            }
        });
    } else {
        // если новая сущность то можно удалять атрибуты без пометки
        node.parentNode.removeChild(node);
    }
}