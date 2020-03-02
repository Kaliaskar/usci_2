/**
 * @author Artur Tkachenko
 * @author Jandos Iskakov
 */

Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.button.*'
]);

function showMetaAttrWindow(className, parentPath, classSync, attrPath, metaAttrData, callback) {
    var setKeyTypes = [
        ['ALL', label_ALL],
        ['ANY', label_OFF]
    ];

    var attributeTypes = [
        [1, LABEL_SIMPLE],
        [2, LABEL_COMPLEX],
        [3, LABEL_SIMPLE_SET],
        [4, LABEL_COMPLEX_SET]
    ];

    var attrSimpleTypes = [
        ['INTEGER', LABEL_NUMBER],
        ['DATE', LABEL_DATE],
        ['DATE_TIME', label_DATE_AND_TIME],
        ['STRING', LABEL_STRING],
        ['BOOLEAN', LABEL_BOOLEAN],
        ['DOUBLE', LABEL_FLOAT]
    ];

    var name = null;
    var attrPathPart = null;

    if (attrPath != null) {
        var pathArray = attrPath.split(".");

        // извлекаем наименование атрибута
        name = pathArray[pathArray.length - 1];

        attrPathPart = attrPath.substring(0, attrPath.length -(name.length + 1));
    } else {
        attrPathPart = parentPath;
    }

    var buttonSave = Ext.create('Ext.button.Button', {
        id: 'buttonSave',
        text: LABEL_SAVE,
        handler: function() {
            var form = Ext.getCmp('formMetaAttr').getForm();
            if (form.isValid()) {
                var editMetaAttr = form.getValues();

                if (metaAttrData)
                    editMetaAttr.id = metaAttrData.id;

                editMetaAttr.keyType = editMetaAttr.isNullableKey? 2: (editMetaAttr.isKey? 1: 0);

                if (!editMetaAttr.keySet)
                    editMetaAttr.keySet = 0;

                var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_SAVING });
                loadMask.show();

                Ext.Ajax.request({
                    url: dataUrl + '/core/meta/saveMetaAttribute',
                    method: 'POST',
                    waitMsg: label_SAVING,
                    jsonData: editMetaAttr,
                    reader: {
                        type: 'json',
                        root: 'data'
                    },
                    success: function(response, opts) {
                        loadMask.hide();
                        Ext.getCmp('windowMetaAttr').destroy();

                        if (callback)
                            callback();
                    },
                    failure: function(response, opts) {
                        loadMask.hide();
                        Ext.getCmp('windowMetaAttr').destroy();

                        Ext.Msg.alert(label_ERROR, JSON.parse(response.responseText).errorMessage);
                    }
                });
            } else {
                Ext.Msg.alert(LABEL_ATTENTION, LABEL_ERROR_EXIST);
            }
        }
    });

    var buttonClose = Ext.create('Ext.button.Button', {
        id: 'buttonClose',
        text: LABEL_CANCEL,
        handler : function() {
            Ext.getCmp('windowMetaAttr').destroy();
        }
    });

    Ext.define('metaClassModel', {
        extend: 'Ext.data.Model',
        fields: ['id','name', 'title']
    });

    var classesStore = Ext.create('Ext.data.Store', {
        model: 'metaClassModel',
        pageSize: 500,
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
        },
        autoLoad: true,
        remoteSort: true
    });

    var formMetaAttr = Ext.create('Ext.form.Panel', {
        id: 'formMetaAttr',
        region: 'center',
        width: 615,
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
                fieldLabel: LABEL_CLASS_CODE,
                name: 'className',
                value: className,
                readOnly: true
            },
            {
                fieldLabel: LABEL_PARENT_CODE ,
                name: 'parentPath',
                value: parentPath,
                readOnly: true
            },
            {
                fieldLabel: LABEL_ATTRIBUTE_PUTH,
                name: 'attrPathPart',
                value: attrPathPart,
                readOnly: true
            },
            {
                fieldLabel: LABEL_ATTRIBUTE_CODE + '*',
                id: 'edName',
                name: 'name',
                allowBlank: false,
                maskRe: /[_a-z]/,
                validator: function(v) {
                    if (/^([_a-z])+$/.test(v)) {
                        return true;
                    } else {
                        return 'Допускается ввод только строчных латинских букв и символа "_" !';
                    }
                },
                //readOnly: metaAttrData && metaAttrData.id != null,
                value: name
            },
            {
                fieldLabel: LABEL_ATTRIBUTE_TYPE + '*',
                id: 'comAttrType',
                name: 'type',
                xtype: 'combobox',
                allowBlank: false,
                //readOnly: classSync,//metaAttrData && metaAttrData.id != null,
                store: new Ext.data.SimpleStore({
                    id: 0,
                    fields: ['id', 'title'],
                    data: attributeTypes
                }),
                editable: false,
                valueField: 'id',
                displayField: 'title',
                queryMode: 'local',
                listeners: {
                    change: function (field, newValue, oldValue) {
                        onAttrTypeChange(newValue, metaAttrData);
                    }
                }
            },
            {
                fieldLabel: LABEL_ATTRIBUTE_CLASS + '*',
                id: 'comRefClass',
                name: 'refClassId',
                xtype: 'combobox',
                editable: false,
                store: classesStore,
                valueField: 'id',
                displayField: 'title'
            },
            {
                fieldLabel: LABEL_ATTRIBUTE_TYPE + '*',
                id: 'comSimpleType',
                name: 'typeCode',
                xtype: 'combobox',
                store: new Ext.data.SimpleStore({
                    id: 0,
                    fields:['id', 'text'],
                    data: attrSimpleTypes
                }),
                editable: false,
                valueField: 'id',
                displayField: 'text',
                queryMode: 'local'
            },
            {
                fieldLabel: LABEL_ARRTIBUTE_NAME + '*',
                id: 'edTitle',
                name: 'title',
                allowBlank: false
            },
            {
                xtype: 'checkboxfield',
                cls: 'checkBox',
                boxLabel: label_MUST,
                //readOnly: classSync,//metaAttrData && metaAttrData.id != null,
                name: 'required',
                inputValue: true
            },
            {
                xtype: 'checkboxfield',
                cls: 'checkBox',
                boxLabel: label_NOT_ACTIVE,
                name: 'deleted',
                inputValue: true
            },
            {
                xtype: 'checkboxfield',
                cls: 'checkBox',
                id: 'cbReference',
                boxLabel: label_LINK,
                name: 'reference',
                inputValue: true
            },
            {
                xtype: 'checkboxfield',
                cls: 'checkBox',
                boxLabel: label_NULLABLE,
                id: 'cbNullify',
                name: 'nullify',
                inputValue: true
            },
            {
                xtype: 'checkboxfield',
                cls: 'checkBox',
                id: 'cbParentIsKey',
                boxLabel: label_DOCH,
                name: 'parentIsKey',
                inputValue: true
            },
            {
                xtype : 'panel',
                title: label_MASS,
                bodyPadding: '5 5 0',
                items: [
                    {
                        xtype: 'checkboxfield',
                        cls: 'checkBox',
                        id: 'cbCumulative',
                        boxLabel: label_SOBIR,
                        name: 'cumulative',
                        inputValue: true
                    },
                    {
                        fieldLabel: label_TYPE_KEY,
                        id: 'comSetKeyType',
                        xtype: 'combobox',
                        name: 'setKeyType',
                        store: new Ext.data.SimpleStore({
                            id: 'ALL',
                            fields: ['code', 'name'],
                            data: setKeyTypes
                        }),
                        editable: false,
                        valueField: 'code',
                        displayField: 'name',
                        queryMode: 'local'
                    }]
            },
            {
                xtype : 'panel',
                title: label_KEYS,
                bodyPadding: '5 5 0',
                items: [{
                    xtype: 'checkboxfield',
                    cls: 'checkBox',
                    boxLabel: label_KEYABLE,
                    id: 'cbIsKey',
                    name: 'isKey',
                    inputValue: true,
                    listeners: {
                        change: function (checkbox, newVal, oldVal) {
                            onIsKeyChange(newVal);
                        }
                    }
                },
                    {
                        xtype: 'checkboxfield',
                        cls: 'checkBox',
                        boxLabel: label_EMPTY,
                        id: 'cbIsNullableKey',
                        name: 'isNullableKey',
                        inputValue: true
                    },
                    {
                        xtype: 'numberfield',
                        fieldLabel: label_GROUP_KEYS,
                        id: 'numKeySet',
                        name: 'keySet',
                        value: 0
                    }]
            }
        ],
        buttons: [buttonSave, buttonClose]
    });

    // если идет создание новой записи то простаюляю значения по умолчанию
    if (!metaAttrData) {
        Ext.getCmp('comAttrType').setValue(1);
        Ext.getCmp('comSimpleType').setValue('INTEGER');
        onIsKeyChange(false);
    }

    var form = Ext.getCmp('formMetaAttr').getForm();

    // загружаем инфу по мета атрибуту из бэка
    // затем присваиваем форме значения
    if (metaAttrData) {
        Ext.Ajax.request({
            url: dataUrl + '/core/meta/getMetaAttribute',
            waitMsg: LABEL_LOADING,
            params: {
                metaClassId: metaAttrData.classId,
                attributeId: metaAttrData.id
            },
            method: 'GET',
            success: function(response, opts) {
                json = JSON.parse(response.responseText);

                var tempMetaAttr = json.data;

                tempMetaAttr.isKey = tempMetaAttr.keyType === 1 || tempMetaAttr.keyType === 2;
                tempMetaAttr.isNullableKey = tempMetaAttr.keyType === 2;

                tempMetaAttr.attrPathPart = attrPathPart;

                form.setValues(tempMetaAttr);

                // поменяем доступность полей в зависимости от типа и ключа
                onAttrTypeChange(/*classSync, */tempMetaAttr.type, tempMetaAttr);
                onIsKeyChange(tempMetaAttr.isKey);
            },
            failure: function(response, opts) {
                Ext.Msg.alert(label_ERROR, JSON.parse(response.responseText).errorMessage);
            }
        });
    }

    var windowMetaAttr = new Ext.Window({
        id: 'windowMetaAttr',
        layout: 'fit',
        title: LABEL_ATTRIBUTE,
        modal: true,
        maximizable: true,
        items:[formMetaAttr]
    });

    windowMetaAttr.show();
}

function onAttrTypeChange(/*classSync, */attrType, metaAttrData) {
    var edName = Ext.getCmp('edName');
    var comAttrType = Ext.getCmp('comAttrType');

    var comSimpleType = Ext.getCmp('comSimpleType');
    var comRefClass = Ext.getCmp('comRefClass');
    var comSetKeyType = Ext.getCmp('comSetKeyType');

    var cbReference = Ext.getCmp('cbReference');
    var cbNullify = Ext.getCmp('cbNullify');
    var cbParentIsKey = Ext.getCmp('cbParentIsKey');
    var cbCumulative = Ext.getCmp('cbCumulative');

    var numKeySet = Ext.getCmp('numKeySet');
    var cbIsKey = Ext.getCmp('cbIsKey');
    var cbIsNullableKey = Ext.getCmp('cbIsNullableKey');

    onIsKeyChange(cbIsKey.getValue());

    // зарпрещаем изменять поля мета атрибута
    // потому что по настроенной логике уже могут быть совершены операций
    //if (metaAttrData && metaAttrData.id) {
    /*if (classSync == true) {
        edName.readOnly = true;

        comAttrType.readOnly = true;
        comSimpleType.readOnly = true;
        comRefClass.readOnly = true;
        comSetKeyType.readOnly = true;

        cbReference.readOnly = true;
        cbNullify.readOnly = true;
        cbParentIsKey.readOnly = true;
        cbCumulative.readOnly = true;

        cbIsKey.readOnly = true;
        cbIsNullableKey.readOnly = true;
        numKeySet.readOnly = true;

        return;
    }*/

    // в зависимости от типа атрибута делаем поля не доступными
    if (attrType === 1) {
        comRefClass.clearValue();

        comSimpleType.allowBlank = false;
        comRefClass.allowBlank = true;

        comSimpleType.setDisabled(false);
        comRefClass.setDisabled(true);
        cbReference.setDisabled(true);
        cbNullify.setDisabled(true);
        cbParentIsKey.setDisabled(true);
        cbCumulative.setDisabled(true);

        cbReference.setValue(false);
        cbNullify.setValue(false);
        cbParentIsKey.setValue(false);
        cbCumulative.setValue(false);
    }
    else if (attrType === 2) {
        comSimpleType.clearValue();

        comSimpleType.allowBlank = true;
        comRefClass.allowBlank = false;

        comSimpleType.setDisabled(true);
        cbCumulative.setDisabled(true);
        cbCumulative.setValue(false);

        comRefClass.setDisabled(false);
        cbReference.setDisabled(false);
        cbNullify.setDisabled(false);
        cbParentIsKey.setDisabled(false);
    }
    else if (attrType === 3) {
        comRefClass.clearValue();

        comSimpleType.allowBlank = false;
        comRefClass.allowBlank = true;

        comRefClass.setDisabled(true);
        cbReference.setDisabled(true);
        cbNullify.setDisabled(true);
        cbParentIsKey.setDisabled(true);

        cbCumulative.setDisabled(false);
        comSimpleType.setDisabled(false);

        cbReference.setValue(false);
        cbNullify.setValue(false);
        cbParentIsKey.setValue(false);
    }
    else if (attrType === 4) {
        comSimpleType.clearValue();

        comSimpleType.allowBlank = true;
        comRefClass.allowBlank = false;

        comSimpleType.setDisabled(true);
        cbNullify.setDisabled(true);
        cbNullify.setValue(false);

        comRefClass.setDisabled(false);
        cbCumulative.setDisabled(false);
        cbReference.setDisabled(false);
        cbParentIsKey.setDisabled(false);
    }
}

function onIsKeyChange(isKey) {
    var comAttrType = Ext.getCmp('comAttrType');
    var numKeySet = Ext.getCmp('numKeySet');
    var cbIsNullableKey = Ext.getCmp('cbIsNullableKey');
    var comSetKeyType = Ext.getCmp('comSetKeyType');

    var attrType = comAttrType.getValue();

    if (isKey) {
        if (numKeySet.value == 0)
            numKeySet.setValue(1);

        if (attrType == 1 || attrType == 2) {
            comSetKeyType.setDisabled(true);
            comSetKeyType.allowBlank = true;
            comSetKeyType.clearValue();
        }
        else {
            comSetKeyType.setDisabled(false);
            comSetKeyType.allowBlank = false;
        }

        numKeySet.readOnly = false;
        cbIsNullableKey.setReadOnly(false);
    }
    else {
        numKeySet.setValue(0);
        numKeySet.readOnly = true;
        cbIsNullableKey.setValue(false);
        cbIsNullableKey.setReadOnly(true);

        comSetKeyType.clearValue();
        comSetKeyType.allowBlank = true;
        comSetKeyType.setDisabled(true);
    }
}
