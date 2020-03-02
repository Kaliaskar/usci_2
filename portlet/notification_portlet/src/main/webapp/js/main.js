Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*',
    'Ext.tab.*',
    'Ext.tip.*'

]);

Ext.onReady(function () {

    checkBoxValues = {};

    var updateCheckBoxGroup = function() {
        var checkBoxGroup = Ext.getCmp('mailTemplateGroup');
        checkBoxGroup.removeAll();
        mailTemplateStore.each(function(record) {
            var mailTemplateName = record.get('mailTemplate');
            checkBoxGroup.add({
                cls: 'checkBox',
                inputValue: record.get('id'),
                boxLabel: mailTemplateName.nameRu,
                checked: record.get('enabled'),
                name: 'myGroup'
            });
        });
        checkBoxGroup.items.each(function(checkbox){
            var checkboxValue = checkbox.inputValue;
            checkBoxValues[checkboxValue] = checkbox.getValue() === true ? 'on' : 'off';
        });
    }

    var mailTemplateStore = Ext.create('Ext.data.Store', {
        id: 'mailTemplateStore',
        fields: ['id', 'userId', 'mailTemplate', 'enabled'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/mail/mail/getUserMailTemplateList',
            method: 'GET',
            extraParams: {
                userId: userId
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
    });

    mailTemplateStore.on({
        load: updateCheckBoxGroup
    });

    var panel = Ext.create('Ext.tab.Panel', {
        renderTo: 'notification-content',
        height: 500,
        width: 1200,
        items: [{
            xtype: 'panel',
            border: false,
            title: label_SETUP,
            layout: {
                type: 'vbox',
                align: 'center'
            },
            bodyPadding: 12,
            items: [{
                xtype: 'displayfield',
                padding: '20',
                style: 'font-size: 20px;',
                labelCls: 'biggertext',
                value: label_TYPES,
                fieldCls: 'biggertext'
            }, {
                xtype: 'checkboxgroup',
                id: 'mailTemplateGroup',
                width: 400,
                layout: {
                    type: 'vbox',
                    autoFlex: false
                },
                items: [],
                listeners: {
                    change: function (checkboxGroup, newValue) {
                        var values ;
                        if (typeof newValue.myGroup === 'object') {
                            values = newValue.myGroup;
                        } else {
                            values = [newValue.myGroup];
                        }
                        checkboxGroup.items.each(function(checkbox){
                            var checkboxValue = checkbox.inputValue;
                            checkBoxValues[checkboxValue] = values.indexOf(checkboxValue) !== -1 ? 'on' : 'off';
                        });
                    }
                }
            }, {
                xtype: 'button',
                text: label_SAVE,
                id: 'btnSave',
                height: 30,
                margin: '20px 10px 20px 180px',
                listeners: {
                    click: function () {
                        var recordList = { userMailTemplateList: []};
                        var founded;
                        for (key in checkBoxValues) {
                            var founded = mailTemplateStore.findRecord('id', key)
                            founded.data.enabled = checkBoxValues[key] === "on" ? true : false ;
                            recordList.userMailTemplateList.push(founded.data);
                        }
                        Ext.Ajax.request({
                            url: dataUrl+'/mail/mail/saveUserMailTemplateList',
                            method: 'POST',
                            jsonData: recordList,
                            reader: {
                                type: 'json',
                                root: 'data'
                            },
                            success: function() {
                                Ext.Msg.alert(label_SUC_SAVED);
                            },
                            failure: function(response) {
                                var error = JSON.parse(response.responseText);

                                Ext.Msg.show({
                                    title: label_ERROR,
                                    msg: error.message,
                                    width : 300,
                                    buttons: Ext.MessageBox.YES
                                });
                            }
                        });
                    }
                }
            }]
        }]
    });
});
