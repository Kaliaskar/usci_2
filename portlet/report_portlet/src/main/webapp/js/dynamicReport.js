function getColomnList(tableName) {
    var columnList = Ext.getCmp('FIELD_ID');
    var columnClass = null;
    Ext.Ajax.request({
        url: dataUrl + '/report/report/getValueList',
        method: 'GET',
        params: {
            userId: userId,
            tableName: tableName,
            procedureName:'REPORTER.INPUT_PARAMETER_SC_FIELDS'
        },
        success: function(response) {
            columnClass = JSON.parse(response.responseText);
            columnList.removeAll();
            for(var i=0; i < columnClass.length; i++) {
                columnList.add({
                    xtype: 'checkbox',
                    cls: 'checkBox',
                    width: 300,
                    inputValue: columnClass[i].value,
                    boxLabel: columnClass[i].displayName,
                    checked: false,
                    name: 'myGroup'
                });
            }
        }
    });
    columnList.doLayout();
}



function addFilter(tableName){
    var columnList = Ext.getCmp('FIELD_ID');
    var filterPanel = Ext.getCmp('filterPanel');
    var columnClass = null;
    var columnStore = Ext.create('Ext.data.Store', {
        id: 'columnStore',
        model: 'valueModel',
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/report/report/getValueList',
            method: 'GET',
            extraParams: {
                userId: userId,
                tableName: tableName,
                procedureName:'REPORTER.INPUT_PARAMETER_SC_FIELDS'
            },
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }

    });

    var filterValuePanel =  Ext.create('Ext.form.Panel', {
        layout:'hbox',
        border: false,
        items: [
            {
                xtype: 'combobox',
                name: 'columnName',
                store: columnStore,
                valueField:'displayName',
                displayField:'displayName',
                fieldLabel: 'Показатель',
                margin: '0 5 0 0',
                listeners: {
                    change: function () {
                        var record =  columnStore.findRecord('displayName', this.getValue());
                        var cmpPanel = this.up('panel').id;
                        console.log(cmpPanel);
                        addFilterConfig(record.data.value, cmpPanel);
                    }
                }
            }
            ,{
                xtpye: 'panel',
                border: false,
                margin: '0 5 0 0',
                layout: 'hbox'
            },
            ,{
                xtype: 'button',
                html: 'Удалить фильтр',
                handler: function() {
                    this.up('panel').destroy();
                }
            }]

    });
    filterPanel.add(filterValuePanel);
}

function addFilterConfig(columntype, panelName) {
    var filterValuePanel = Ext.getCmp(panelName);
    var filterPanel = Ext.getCmp('filterPanel');
    if (columntype == 'DATE') {
        var operation = ["=", "<>", "<=", ">=", "<", ">"];
        var valueComp = Ext.create('Ext.form.field.Date', {
            name: 'filterValue',
            anchor: '100%',
            format: 'd.m.Y',
            fieldLabel: 'Значение',
            maxValue: new Date()
        });
    }
    else if (columntype == 'BOOLEAN') {
        var operation = ["="];
        var possibleValue = ["TRUE", "FALSE"];
        var valueComp = Ext.create('Ext.form.field.ComboBox', {
            name: 'filterValue',
            store: possibleValue,
            valueField:'value',
            displayField:'value',
            fieldLabel: 'Заначение'
        });
    }
    else {
        if (columntype == 'VARCHAR2') {
            var operation = ["=", "<>"];
        }
        else {
            var operation = ["=", "<>", "<=", ">=", "<", ">"];
        }
        var valueComp = Ext.create('Ext.form.field.Text', {
            fieldLabel: 'Значение',
            name: 'filterValue'
        });
    }

    var operationComp = Ext.create('Ext.form.field.ComboBox', {
        name:'operationValue',
        store: operation,
        margin: '0 10 0 0',
        valueField: 'value',
        displayField: 'value',
        fieldLabel: 'Фильтр'
    });
    filterValuePanel.add(operationComp);
    filterValuePanel.add(valueComp);
    filterPanel.show();
}
