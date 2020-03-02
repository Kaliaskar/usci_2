function newRuleForm(){
    return new Ext.Window({
        id: 'newRuleForm',
        layout: 'fit',
        modal: 'true',
        title: label_NEW_RULE,
        items: [
            Ext.create('Ext.form.Panel',{
                region: 'center',
                width: 1200,
                height: 700,
                items: [
                    Ext.create('Ext.form.TextField', {
                        fieldLabel: label_POCK,
                        labelWidth: 55,
                        value: Ext.getCmp('elemComboPackage').getRawValue(),
                        disabled: true,
                        padding: 3
                    }),
                    Ext.create('Ext.form.DateField', {
                        id: 'elemNewRuleDate',
                        fieldLabel: 'дата',
                        labelWidth: 55,
                        format: 'd.m.Y',
                        padding: 3
                    }),
                    Ext.create('Ext.form.TextField', {
                        id: 'txtTitle',
                        fieldLabel: label_NAME_LIL,
                        labelWidth: 55,
                        padding: 3
                    }),
                    Ext.create('Ext.form.Panel',{
                        tbar: [
                            {
                                text: label_ADD_BIG,
                                hidden: !isDataManager,
                                id: 'btnNewRuleSubmit',
                                handler: function(){
                                    Ext.Date.patterns={
                                        CustomFormat: "d.m.Y"
                                    };
                                    var datefromfield = Ext.getCmp('elemNewRuleDate').value;
                                    Ext.Ajax.request({
                                        disableCaching: false,
                                        url: dataUrl+'/rule/createRule',
                                        method: 'PUT',
                                        params: {
                                            title: Ext.getCmp('txtTitle').value,
                                            ruleBody: newRuleEditor.getSession().getValue(),
                                            date: Ext.Date.format(datefromfield, Ext.Date.patterns.CustomFormat),
                                            packageId: Ext.getCmp('elemComboPackage').value,
                                            pkgName: Ext.getCmp('elemComboPackage').getRawValue()
                                        },
                                        reader: {
                                            type: 'json',
                                            root: 'data'
                                        },
                                        success: function(response){
										  Ext.Msg.alert('', label_ADDED);
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
                    }),
                    {
                        html: "<div id='bknew-rule' style='height: 600px;'>function(){}</div>",
                    }
                ]
            })
        ]
    });
}