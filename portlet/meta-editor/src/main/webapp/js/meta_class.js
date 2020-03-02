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

function showMcWindow(metaClassId) {
    var metaClass = null;

    if (metaClassId) {
        var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: LABEL_LOADING });
        loadMask.show();

        Ext.Ajax.request({
            url: dataUrl + '/core/meta/getMetaClassData',
            method: 'GET',
            params: {
                metaClassId: metaClassId
            },
            success: function(response) {
                loadMask.hide();

                metaClass = JSON.parse(response.responseText);
                if (metaClass.uiConfig !== null ) {
                    metaClass.uiConfig = metaClass.uiConfig.displayField;
                }

                createMcWindow(metaClass).show();
            }
        });
    } else {
        metaClass = {
            id: null,
            name: "",
            title: "",
            uiConfig: "",
            hashSize: 0,
            dictionary: false,
            operational: false,
            deleted: false
        };

        createMcWindow(metaClass).show();
    }
}

function createMcWindow(metaClass) {
    Ext.define('constantModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'nameRu', 'nameKz']
    });

    var buttonSave = Ext.create('Ext.button.Button', {
        id: "buttonSave",
        text: LABEL_SAVE,
        handler : function() {
            var form = Ext.getCmp('formMetaClass').getForm();
            if (form.isValid()) {
                var formValues = form.getValues();

                var uiConfig = {
                    displayField: formValues.uiConfig
                }

                // формирую обьект мета класс для отправки в бэк
                var metaClassData = {
                    id: metaClass.id,
                    name: formValues.name,
                    title: formValues.title,
                    uiConfig: uiConfig,
                    hashSize: formValues.hashSize,
                    periodTypeId: formValues.periodTypeId,
                    dictionary: formValues.dictionary,
                    operational: formValues.operational,
                    deleted: formValues.deleted
                };

                var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_SAVING });
                loadMask.show();

                Ext.Ajax.request({
                    url: dataUrl + '/core/meta/saveMetaClass',
                    method: 'POST',
                    waitMsg: label_SAVING,
                    jsonData: metaClassData,
                    reader: {
                        type: 'json',
                        root: 'data'
                    },
                    success: function(response, opts) {
                        loadMask.hide();

                        Ext.getCmp('windowMetaClass').destroy();

                        var metaClassGrid = Ext.getCmp('metaClassGrid');

                        // перегружаем весь грид чтобы отобразить изменения
                        metaClassGrid.getStore().load();
                        metaClassGrid.getView().refresh();
                    },
                    failure: function(response, opts) {
                        loadMask.hide();

                        Ext.Msg.alert(label_ERROR, JSON.parse(response.responseText).errorMessage);
                    }
                });
            } else {
                Ext.Msg.alert(LABEL_ATTENTION, LABEL_ALL_FIELDS);
            }
        }
    });

    var buttonClose = Ext.create('Ext.button.Button', {
        id: 'buttonClose',
        text: LABEL_CANCEL,
        handler : function() {
            Ext.getCmp('windowMetaClass').destroy();
        }
    });

    var hashSizeList = [
        [0, label_2min],
        [4, label_10min],
        [32, label_50min],
        [128, label_50minmore]
    ];

    var formMetaClass = Ext.create('Ext.form.Panel', {
        id: 'formMetaClass',
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
                fieldLabel: LABEL_CODE + '*',
                name: 'name',
                value: metaClass.name,
                allowBlank: false,
                maskRe: /[_a-z]/,
                validator: function(v) {
                    if (/^([_a-z])+$/.test(v)) {
                        return true;
                    } else {
                        return 'Допускается ввод только строчных латинских букв и символа "_" !';
                    }
                },
                readOnly: metaClass.id != null,
                blankText: LABEL_REQUIRED_FIELD,
            },
            {
                fieldLabel: LABEL_TITLE + '*',
                name: 'title',
                value: '',
                required: true
            },
            {
                fieldLabel: LABEL_UI_CONFIG + '*',
                name: 'uiConfig',
                value: '',
                required: true
            },
            {
                fieldLabel: label_PERIODICY,
                id: 'comPeriodType',
                name: 'periodTypeId',
                xtype: 'combobox',
                allowBlank: false,
                editable : false,
                readOnly: metaClass.id != null,
                store: Ext.create('Ext.data.Store', {
                    model: 'constantModel',
                    pageSize: 100,
                    proxy: {
                        type: 'ajax',
                        url: dataUrl + '/utils/text/getTextListByType',
                        extraParams: {
                            types: ['PERIOD_TYPE']
                        },
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
                            // по умолчанию укажем на первую запись
                            if (!metaClass.id && records.length > 0)
                                Ext.getCmp('comPeriodType').setValue(records[0].get('id'));
                        }
                    },
                    autoLoad: true,
                    remoteSort: true
                }),
                valueField: 'id',
                displayField: 'nameRu',
                queryMode: 'local'
            },
            {
                fieldLabel: label_SIZE_TABLE,
                id: 'comHashSize',
                name: 'hashSize',
                xtype: 'combobox',
                editable: false,
                readOnly: metaClass.id != null,
                store: new Ext.data.SimpleStore({
                    id: 0,
                    fields:['id', 'text'],
                    data: hashSizeList
                }),
                valueField: 'id',
                displayField: 'text',
                queryMode: 'local'
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
                boxLabel: LABEL_REFERENCE,
                name: 'dictionary',
                readOnly: metaClass.id != null,
                inputValue: true
            },
            {
                xtype: 'checkboxfield',
                cls: 'checkBox',
                boxLabel: label_OPER,
                name: 'operational',
                readOnly: metaClass.id != null,
                inputValue: true
            }],
        buttons: [buttonSave, buttonClose]
    });

    var form = Ext.getCmp('formMetaClass').getForm();

    form.setValues(metaClass);

    var windowMetaClass = new Ext.Window({
        id: "windowMetaClass",
        layout: 'fit',
        title: LABEL_META,
        modal: true,
        maximizable: true,
        items: [formMetaClass]
    });

    return windowMetaClass;
}
