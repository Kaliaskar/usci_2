var packageControlClassId = null;

function updateButtons(){
    if(packageControlClassId != null)
        Ext.getCmp('btnPackageControlAdd').setDisabled(false);
}

function packageControlForm(){

    var packageStore = Ext.create('Ext.data.Store',{
        id: 'packageStore',
        model: 'packageListModel',
        proxy: {
            type: 'ajax',
            url: dataUrl+'/package/getAllPackages',
            reader: {
                type: 'json',
                root: 'data'
            }
        },
        autoLoad: true,
        listeners: {
            load: function(me, records, options) {
                if (records.length > 0)
                    Ext.getCmp('elemComboPack').setValue(records[0].get('id'));
            }
        }
    });

    var packageVersionStore = Ext.create('Ext.data.Store',{
        id: 'packageVersionStore',
        model: 'packageListModel',
        proxy: {
            type: 'ajax',
            url: dataUrl+'/rule/getPackageVersions',
            reader: {
                type: 'json',
                root: 'data'
            }
        }
    });

    return new Ext.Window({
        id: 'packageControlForm',
        layout: 'fit',
        modal: 'true',
        title: label_PACK_CON,
        items: [
            Ext.create('Ext.form.Panel',{
                region: 'center',
                width: 600,
                items: [
                    {
                        xtype: 'combobox',
                        id: 'elemComboPack',
                        editable: false,
                        layout: 'border',
                        store: packageStore,
                        displayField: 'name',
                        valueField: 'id',
                        margin: 5,
                        listeners: {
                            change: function (control, newValue, oldValue, eOpts) {
                                packageControlClassId = control.value;
                                updateButtons();

                                packageVersionStore.load({
                                    params: {
                                        packageId: control.value
                                    }});
                            }
                        }
                    },
                    Ext.create('Ext.form.Panel',{
                        tbar: [
                            {
                                text: label_ADD_BIG,
                                id: 'btnPackageControlAdd',
                                handler: function(){

                                    new Ext.Window ({
                                        title: label_PACK_CON,
                                        id: 'wdwNewPackage',
                                        modal: 'true',
                                        items: [
                                            {
                                                xtype: 'textfield',
                                                id: 'newPackageName',
                                                margin: '10 0 0 10',
                                                fieldLabel: label_PACK_NAME,
                                                labelWidth: 130
                                            }, {
                                                xtype: 'button',
                                                text: 'ok',
                                                handler: function(a,b,c){
                                                    Ext.Ajax.request({
                                                        url: dataUrl+'/package/savePackage',
                                                        method: 'GET',
                                                        params: {
                                                            rulePackageName: Ext.getCmp('newPackageName').value
                                                        },
                                                        reader: {
                                                            type: 'json',
                                                            root: 'data'
                                                        },
                                                        success: function(response){
                                                            Ext.Msg.alert('', label_NEW_PACK);
                                                            packageStore.reload();
                                                            Ext.getCmp('wdwNewPackage').close();
                                                        },
                                                        failure: function(response){
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
                                        ]
                                    }).show();

                                }
                            },
                            {
                                text: label_DEL_BIG,
                                id: 'btnPackageControlDelete',
                                disabled: true,
                                hidden: true
                            },
                            {
                                text: label_REFRESH_BIG,
                                id: 'btnPackageControlUpdate',
                                disabled: true,
                                hidden: true
                            }
                        ]
                    })
                ]
            })
        ]
    });
}
