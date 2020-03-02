Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*'
]);


var ruleListGrid = null;
var packageStore = null;


function deleteRule(rowIndex){
    Ext.Ajax.request({
        url: dataUrl+'/rule/deleteRule',
        method: 'POST',
        waitMsg: 'adding',
        params : {
            ruleId: editor.ruleId,
            packageId: Ext.getCmp('elemComboPackage').value,
            pkgName: Ext.getCmp('elemComboPackage').getRawValue(),
            date: Ext.getCmp('elemPackageVersionCombo').value
        },
        reader: {
            type: 'json',
            root: 'data'
        },
        success: function(response, opts) {
            Ext.Msg.alert('', label_DELETED);
            rowIndex = (typeof rowIndex === 'undefined') ? -1 : rowIndex;

            ruleListGrid.store.removeAt(rowIndex);

            if (ruleListGrid.store.data.length == 0)
                reset();
            else {
                ruleListGrid.getSelectionModel().select(0);
                ruleListGrid.fireEvent("cellclick", ruleListGrid, null, 1, ruleListGrid.getSelectionModel().getLastSelected());
            }

        },
        failure: function(response, opts) {
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

function initGrid(){
    var store = Ext.create('Ext.data.ArrayStore', {
        fields: ['id','name','isActive'],
        proxy: {
            type: 'ajax',
            url : dataUrl+'/rule/getRuleTitles',
            reader: {
                type: 'json',
                root: 'data'
            }
        }
    });


    ruleListGrid = Ext.create('Ext.grid.Panel', {
        store: store,
        columns: [
            {
                text     : label_NAME,
                dataIndex: 'name',
                width: 200,
                flex: 1,
                field: {
                    allowBlank: false
                }
            }
        ],
        listeners : {
            cellclick: function(grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts){
                Ext.Ajax.request({
                    url: dataUrl+'/rule/getRule',
                    method: 'GET',
                    waitMsg: 'adding',
                    params: {
                        ruleId: newValue.data.id,
                        date: Ext.getCmp('elemPackageVersionCombo').value
                    },
                    reader: {
                        type: 'json',
                        root: 'data'
                    },
                    success: function(response, opts) {
                        var obj = JSON.parse(response.responseText);
                        editor.title = obj.title;
                        editor.backup = obj.rule;
                        editor.ruleId = newValue.data.id;
                        editor.infIndex = newValue.index;
                        editor.setValue(obj.rule, -1);
                        editor.$readOnly = false;
                    },
                    failure: function(response, opts) {
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
        },
        plugins: [
            Ext.create('Ext.grid.plugin.CellEditing', {
                clicksToEdit: 2
            })],
        dockedItems: [{
            xtype: 'toolbar',
            items: [{
                text: label_ADD,
                hidden: !isDataManager,
                icon: contextPathUrl + '/pics/add.png',
                id: 'btnNewRule',
                //hidden: readOnly,
                handler: function(e1,e2){
                    newRuleForm().show();

                    require(['ace/ace'],function(ace){
                        newRuleEditor = ace.edit('bknew-rule');
                    });
                }
            },
                {
                text: label_CANCEL,
                    hidden: !isDataManager,
                icon: contextPathUrl + '/pics/bin.png',
                // hidden: readOnly,
                id: 'btnFlush',
                handler: function(){
                    var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_WAIT });
                    loadMask.show();
                    Ext.Ajax.request({
                        url: dataUrl+'/rule/reloadCache',
                        method: 'POST',
                        waitMsg: 'adding',
                        reader: {
                            type: 'json'
                        },
                        actionMethods: {
                            read: 'POST',
                            root: 'data'
                        },
                        success: function(response, opts) {
                            loadMask.hide();
                            Ext.Msg.alert(label_INFO, label_CACHE);
                        },
                        failure: function(response, opts) {
                            loadMask.hide();
                            var error = JSON.parse(response.responseText);

                            Ext.Msg.show({
                                title: label_ERR_CACHE,
                                msg: error.message,
                                width : 300,
                                buttons: Ext.MessageBox.YES
                            });
                        }
                    });
                }
            },{
                text: label_HISTORY,
                id: 'btnHistory',
                icon: contextPathUrl + '/pics/copy2.png',
                hidden: true,
                disabled: true,
                handler: function(){
                    //createRuleForm().show();
                    historyForm(ruleListGrid.getSelectionModel().getLastSelected().data.id).show();
                }
            },{
                text: lable_REFRESH,
                id: 'btnRefresh',
                icon: contextPathUrl + '/pics/refresh.png',
                handler: function(){
                    updateRules();
                }
            },{
                text: label_POCKETS,
                    hidden: !isDataManager,
                id: 'btnPackages',
                //hidden: readOnly,
                handler: function(){
                    packageControlForm().show();
                }
            },{
                text: label_SEARCH,
                id: 'btnSearch',
                icon: contextPathUrl + '/pics/search.png',
                handler: function(){
                    Ext.getCmp('txtSearch').show();
                    Ext.getCmp('txtSearch').focus(false,200);
                }
            },{
                xtype: 'textfield',
                id: 'txtSearch',
                hidden: true,
                listeners: {
                    render: function(cmd) {
                        cmd.getEl().on('click',function(){Ext.EventObject.stopPropagation();});
                    },
                    scope: this,
                    specialkey: function(f,e) {
                        if(e.getKey() == e.ENTER) {
                            updateRules(Ext.getCmp('txtSearch').value);
                        } else if(e.getKey() == e.ESC) {
                            Ext.getCmp('txtSearch').hide();
                        }
                    }
                }
            }]
        }],
        height: '75%',
        region: 'south'
    });

    ruleListGrid.on('edit', function(e,r){
        if(readOnly) return;
        Ext.Ajax.request({
            url: dataUrl+'/rule/renameRule',
            method: 'PUT',
            waitMsg: 'adding',
            params : {
                ruleId: ruleListGrid.getSelectionModel().getLastSelected().data.id,
                title: r.value
            },
            reader: {
                type: 'json',
                root: 'data'
            },
            success: function(response, opts) {
            },
            failure: function(response, opts) {
                var error = JSON.parse(response.responseText);

                Ext.Msg.show({
                    title: label_ERROR,
                    msg: error.message,
                    width : 300,
                    buttons: Ext.MessageBox.YES
                });
            }
        });
    }, this);

    return ruleListGrid;
}

function reset(){
    editor.setValue("",-1);
    editor.$readOnly = true;
    editor.batchVersionId = -1;
    editor.backup = "";
    Ext.getCmp('btnCancel').setDisabled(true);
    Ext.getCmp('btnSave').setDisabled(true);
    Ext.getCmp('btnDel').setDisabled(true);
    ruleListGrid.store.loadData([],false);
}

function updateRules(searchText){

    if(Ext.getCmp('elemComboPackage').value == null || Ext.getCmp('elemPackageVersionCombo').value == null)
        return;
    reset();
    ruleListGrid.store.load(
        {
            params: {
                packageId:Ext.getCmp('elemComboPackage').value,
                date: Ext.getCmp('elemPackageVersionCombo').value,
                searchText: searchText
            },
            callback: function(a,b,success){
                if(success == false){
                    Ext.Msg.alert('',label_NO_VERSION);
                    reset();
                }else{
                    editor.batchVersionId = Ext.decode(b.response.responseText).batchVersionId;
                }
            },
            scope: this
        }
    );
    ruleListGrid.getView().refresh();
}

Ext.onReady(function(){
    Ext.define('packageListModel',{
        extend: 'Ext.data.Model',
        fields: ['name']
    });

    var map1 = new Ext.util.KeyMap(document,{
            key: "s",
            ctrl: true,
            shift: true,
            fn: function(){
                if(editor.isFocused()){
                    Ext.getCmp('btnSave').handler();
                }
            }
        }
    );

    var map2 = new Ext.util.KeyMap(document,{
            key: "a",
            ctrl: true,
            shift: true,
            fn: function(){
                Ext.getCmp('btnNewRule').handler();
            }
        }
    );

    var map3 = new Ext.util.KeyMap(document,{
            key: "e",
            ctrl: true,
            shift: true,
            fn: function(){
                Ext.getCmp('btnRun').handler();
            }
        }
    );

    var map4 = new Ext.util.KeyMap(document,{
            key: "f",
            ctrl: true,
            shift: true,
            fn: function(){
                Ext.getCmp('btnFlush').handler();
            }
        }
    );

    packageStore = Ext.create('Ext.data.Store',{
        id: 'packageStore',
        model: 'packageListModel',
        proxy: {
            type: 'ajax',
            url: dataUrl+'/package/getAllPackages',
            listeners : {
                exception: function(proxy, response, operation, eOpts) {
                    var r = JSON.parse(response.responseText);
                    if(r.errorMessage) {
                        Ext.Msg.show({
                            title: label_ERROR,
                            msg: r.errorMessage,
                            width : 300,
                            buttons: Ext.MessageBox.YES
                        });
                    }
                }
            },
            reader: {
                type: 'json',
                root: 'data'
            }
        },
        autoLoad: true,
        listeners: {
            load: function(me, records, options) {
                if (records.length > 0)
                    Ext.getCmp('elemComboPackage').setValue(records[0].get('id'));
            }
        }
    });

    packageVersionStore = Ext.create('Ext.data.Store',{
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

    var panel  = Ext.create('Ext.panel.Panel',{
            title : '',
            width : '100%',
            height : 600,
            renderTo: 'brms-content',
            layout : 'border',
            id: 'MainPanel',
            defaults : {
                split: true
            },
            items: [
                {
                    region: 'east',
                    id: 'elemRuleBody',
                    width: '75%',
                    html: "<div id='bkeditor'>function(){}</div>",
                    tbar: [
                        {
                            text : label_CAN,
                            hidden: !isDataManager,
                            //hidden: readOnly,
                            id: 'btnCancel',  icon: contextPathUrl + '/pics/undo.png', handler: function(){ editor.setValue(editor.backup, -1); }, disabled: true},
                        {
                            text: label_SAVE,
                            hidden: !isDataManager,
                            id: 'btnSave',
                            icon: contextPathUrl + '/pics/save.png',
                            disabled: true,
                            //hidden: readOnly,
                            handler: function(){
                                Ext.Ajax.request({
                                    url: dataUrl+'/rule/updateRule',
                                    method: 'PUT',
                                    waitMsg: 'adding',
                                    params : {
                                        ruleBody: editor.getSession().getValue(),
                                        ruleId: editor.ruleId,
                                        date: Ext.getCmp('elemPackageVersionCombo').value,
                                        pkgName: Ext.getCmp('elemComboPackage').getRawValue(),
                                        packageId: Ext.getCmp('elemComboPackage').value
                                    },
                                    reader: {
                                        type: 'json',
                                        root: 'data'
                                    },
                                    success: function(response, opts) {
                                        Ext.Msg.alert('', label_REFRESHED);
                                        ruleListGrid.fireEvent('cellclick', ruleListGrid, null, 1, ruleListGrid.getSelectionModel().getLastSelected()); //cellclick: function(grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts){
                                    },
                                    failure: function(response, opts) {
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
                        },
                        {text: label_DEL,
                            hidden: !isDataManager,
                            id: 'btnDel',
                            icon: contextPathUrl + '/pics/crop2.png',
                            disabled: true,
                           //hidden: readOnly,
                            handler: function(){
                                //Ext.Msg.alert("Сообщение","Вы точно хотите удалить правило ?");
                                Ext.Msg.show({
                                    title: label_CONFIRM,
                                    msg: label_WANT_DEL,
                                    buttons: Ext.Msg.OKCANCEL,
                                    fn: function(btn){
                                        if(btn=='ok')
                                            deleteRule(ruleListGrid.store.indexOf(ruleListGrid.getSelectionModel().getLastSelected()));
                                    }
                                });

                            }
                        }]
                },{
                    xtype: 'panel',
                    region: 'center',
                    layout: 'border',
                    defaults : {
                        split: true
                    },
                    items: [
                        {
                            xtype: 'panel',
                            region: 'center',
                            bodyStyle: 'padding: 15px',
                            items: [
                                {
                                    xtype : 'combobox',
                                    id: 'elemComboPackage',
                                    editable: false,
                                    store: packageStore,
                                    valueField:'id',
                                    displayField:'name',
                                    fieldLabel: label_CHOOSE_POCK,
                                    listeners: {
                                        change: function(){
                                            packageVersionStore.load({
                                                params: {
                                                    packageId: Ext.getCmp('elemComboPackage').value
                                                }});
                                        }
                                    }
                                },
                                {
                                    xtype: 'combobox',
                                    id: 'elemPackageVersionCombo',
                                    store: packageVersionStore,
                                    displayField: 'name',
                                    valueField: 'name',
                                    fieldLabel: label_DATE,
                                    listeners: {
                                        change: function(){
                                            updateRules();
                                        }
                                    }
                                }
                            ]
                        },
                        initGrid()
                    ]
                },{
                    region: 'south',
                    html: label_NO_ERRORS,
                    id: 'errorPanel',
                    collapsible: true,
                    collapsed: true,
                    height: '10%'
                }
            ]
        }
    ); //end of panel
});