Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*',
    'Ext.button.*',
    'Ext.toolbar.*',
    'Ext.container.*'
]);

Ext.onReady(function () {

    function isEmpty(value) {
        return value === undefined || value === null || value.length === 0;
    }

    function getProfiles() {
        var profilesString = document.app.getProfileNames('|');
        return profilesString.split('|');
    }

    function getCertificates(profile, password) {
        var certificatesInfo = document.app.getCertificatesInfo(profile, password, 0, '', true, false, '|');
        return certificatesInfo.split('|');
    }

    function signHash(value, certificate, profile, password) {
        return document.app.createPKCS7(value, 0, null, certificate, true, profile, password, '1.3.6.1.4.1.6801.1.5.8', true);
    }

    function getFileName(value) {
        var startIndex = (value.indexOf('\\') >= 0 ? value.lastIndexOf('\\') : value.lastIndexOf('/'));
        var filename = value.substring(startIndex);
        if (filename.indexOf('\\') === 0 || filename.indexOf('/') === 0) {
            filename = filename.substring(1);
        }

        return filename;
    }

    var profilesStore = Ext.create('Ext.data.Store', {
        id: 'profilesStore',
        fields: ['name'],
        data : [
            {"name":"Нажмите на кнопку \"Загрузить профили\" ..."}
        ],
        autoload: false
    });

    var certificatesStore = Ext.create('Ext.data.Store', {
        id: 'certificatesStore',
        fields: ['name'],
        data : [
            {"name":"Нажмите на кнопку \"Получить сертификаты из профиля\" ..."}
        ],
        autoload: false
    });

    var userRespondentStore = Ext.create('Ext.data.Store', {
        id: 'userRespondentStore',
        fields: ['id', 'name', 'shortName', 'code', 'shutdownDate', 'changeDate', 'bin', 'rnn', 'bik', 'mainOffice', 'branches', 'subjectType'],
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getUserRespondentList',
            extraParams: {userId: userId},
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        },
        listeners: {
            load: function () {
                var creditorId = userRespondentStore.getAt(0).data.id;

                Ext.getCmp('batchSignGrid').store.load({
                    params: {
                        respondentId: creditorId,
                        userId: userId
                    },
                    scope: this
                });
                Ext.getCmp('batchSignGrid').getView().refresh();
            }
        },
        sorters: [{
            property: 'name',
            direction: 'asc'
        }]
    });

    var batchSignStore = Ext.create('Ext.data.Store', {
        id: 'batchSignStore',
        fields: ['id', 'receiverDate', 'fileName', 'hash', 'totalEntityCount', 'signSymbol', 'signature', 'check'],
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl+'/receiver/batch/getBatchListToSign',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
    });

    var panel = Ext.create('Ext.panel.Panel', {
        height: 800,
        margin: 0,
        width: 1200,
        autoScroll: true,
        title: '          ',
        titleCollapse: false,
        renderTo: 'signing-content',
        id: 'MainPanel',
        listeners: {
            afterrender: function () {
                Ext.getCmp('profilesCombo').setValue(profilesStore.getAt(0).data.name);
                Ext.getCmp('certsCombo').setValue(certificatesStore.getAt(0).data.name);
            }
        },
        items: [{
            xtype: 'button',
            margin: '10 0 0 10',
            text: label_DOWNLOAD,
            listeners: {
                click: function () {
                    var profiles = getProfiles();
                    var selectedValue = '';
                    var store = Ext.getCmp('profilesCombo').getStore();
                    store.removeAll();
                    Ext.getCmp('profilesCombo').setValue(null);
                    store.commitChanges();
                    for (var i = 0; i < profiles.length; i++) {
                        store.add({name: profiles[i]});
                        if (profiles[i] === 'profile://FSystem') {
                            selectedValue = profiles[i];
                        }
                    }
                    if (!isEmpty(selectedValue)) {
                        Ext.getCmp('profilesCombo').setValue(selectedValue);
                    }
                }
            }
        }, {
            xtype: 'combobox',
            id: 'profilesCombo',
            store: profilesStore,
            margin: '10 0 0 10',
            width: 400,
            editable : false,
            valueField: 'name',
            displayField: 'name',
            fieldLabel: label_SPISOK,
            labelWidth: 130,
            labelAlign: 'left',
            labelStyle: 'font-weight: bold;'
        }, {
            xtype: 'checkboxfield',
            margin: '10 0 0 10',
            cls: 'checkBox',
            fieldLabel: '',
            boxLabel: label_PROTECTED,
            listeners: {
                change: function (field, newValue, oldValue, options) {
                    passfield = Ext.getCmp("profilePasword");
                    if (newValue == '1') {
                        passfield.show();
                    } else if (newValue == '0') {
                        passfield.hide();
                    }
                }
            }
        }, {
            xtype: 'textfield',
            id: 'profilePasword',
            hidden: true,
            margin: '10 0 0 10',
            fieldLabel: label_PASSWORD,
            labelWidth: 130,
            inputType: 'password'
        }, {
            xtype: 'button',
            margin: '10 0 0 10',
            text: label_GET_SERT,
            listeners: {
                click: function () {
                    var profile = Ext.getCmp('profilesCombo').getValue();
                    var password = Ext.getCmp('profilePasword').getValue();
                    var certificates = getCertificates(profile, password);
                    if (!certificates || (certificates.length === 0)) {
                        Ext.Msg.alert(label_CANT_GET);
                        return;
                    }
                    var store = Ext.getCmp('certsCombo').getStore();
                    store.removeAll();
                    Ext.getCmp('certsCombo').setValue(null);
                    store.commitChanges();
                    for (var i = 0; i < certificates.length; i++) {
                        store.add({name: certificates[i]});
                    }
                    Ext.getCmp('certsCombo').setValue(store.getAt(0));
                }
            }
        }, {
            xtype: 'combobox',
            id: 'certsCombo',
            store: certificatesStore,
            margin: '10 0 0 10',
            width: 1000,
            editable : false,
            valueField: 'name',
            displayField: 'name',
            fieldLabel: label_SERTIFICATE,
            labelAlign: 'left',
            labelStyle: 'font-weight: bold;'
        }, {
            xtype: 'gridpanel',
            id: 'batchSignGrid',
            store: batchSignStore,
            height: 400,
            width: 1190,
            margin: '10 0 15 5',
            autoScroll: true,
            title: '',
            columns: [{
                xtype: 'checkcolumn',
                dataIndex : 'check',
                width: 60,
                text: ''
            }, {
                xtype: 'gridcolumn',
                width: 300,
                dataIndex: 'fileName',
                text: label_FILE_NAME
            }, {
                xtype: 'datecolumn',
                width: 130,
                dataIndex: 'receiverDate',
                text: label_REC_DATE,
                format: 'd.m.Y H:i:s'
            }, {
                xtype: 'gridcolumn',
                dataIndex : 'signSymbol',
                width: 136,
                text: label_SIGNED
            }],
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'bottom',
                items: [{
                    text: 'Выделить все',
                    handler: function() {
                        var batchSignGridStore = Ext.getCmp('batchSignGrid').getStore();
                        batchSignGridStore.suspendEvents();
                        batchSignGridStore.each(function(rec){ rec.set('check', true) })
                        batchSignGridStore.resumeEvents();
                        Ext.getCmp('batchSignGrid').getView().refresh();
                    }
                }, {
                    xtype: 'tbseparator',
                }, {
                    text: 'Снять выделение',
                    handler: function() {
                        var batchSignGridStore = Ext.getCmp('batchSignGrid').getStore();
                        batchSignGridStore.suspendEvents();
                        batchSignGridStore.each(function(rec){ rec.set('check', false) })
                        batchSignGridStore.resumeEvents();
                        Ext.getCmp('batchSignGrid').getView().refresh();
                    }
                }, {
                    xtype: 'tbseparator',
                }, {
                    xtype: 'button',
                    text: label_SING_SAVE,
                    listeners: {
                        click: function () {
                            var profile = Ext.getCmp('profilesCombo').getValue();
                            if (isEmpty(profile)) {
                                Ext.Msg.alert(label_CHOOSE_P);
                                return;
                            }
                            var certificate = Ext.getCmp('certsCombo').getValue();
                            if (isEmpty(certificate)) {
                                Ext.Msg.alert(label_CHOOSE_S);
                                return;
                            }
                            var password = Ext.getCmp('profilePasword').getValue();

                            var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: label_WAIT });
                            loadMask.show();
                            gridBatch = Ext.getCmp("batchSignGrid");
                            for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                if (gridBatch.store.getAt(i).data.check === true) {
                                    var pkcs7 = signHash(gridBatch.store.getAt(i).data.hash, certificate, profile, password);
                                    gridBatch.store.getAt(i).data.signature = pkcs7;
                                    gridBatch.store.getAt(i).data.signSymbol = '<img src="'+ contextPathUrl + '/pics/accept.png"/>';
                                    gridBatch.store.getAt(i).commit();
                                }
                            }
                            gridBatch.getView().refresh();

                            var creditorBin = userRespondentStore.getAt(0).data.bin;
                            var batchSignJsonList = { batchList: []};

                            for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                if (gridBatch.store.getAt(i).data.check === true) {
                                    batchSignJsonList.batchList.push(gridBatch.store.getAt(i).data);
                                }
                            }

                            Ext.Ajax.request({
                                url: dataUrl + '/receiver/batch/saveSignedBatchList',
                                method: 'POST',
                                params: {
                                    respondentBin: creditorBin
                                },
                                jsonData: batchSignJsonList,
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function() {
                                    loadMask.hide();
                                    Ext.Msg.alert(label_BATCHES_SAVED);
                                    gridBatch.store.remove(records);
                                },
                                failure: function(response) {
                                    loadMask.hide();
                                    var error = JSON.parse(response.responseText);

                                    Ext.Msg.show({
                                        title: label_ERROR,
                                        msg: error.message,
                                        width : 300,
                                        buttons: Ext.MessageBox.YES
                                    });
                                }
                            });
                            gridBatch.getView().refresh();
                        }
                    }
                }, {
                    xtype: 'tbseparator'
                }, {
                    xtype: 'button',
                    text: label_CANCEL_SELECTED,
                    listeners: {
                        click: function () {
                            gridBatch = Ext.getCmp("batchSignGrid");
                            var batchIds = [];
                            var records = [];
                            for (var i = 0; i < gridBatch.store.getCount(); i++) {
                                if (gridBatch.store.getAt(i).data.check === true) {
                                    batchIds.push(gridBatch.store.getAt(i).data.id);
                                    records.push(gridBatch.store.getAt(i));
                                }

                            }

                            Ext.Ajax.request({
                                url: dataUrl+'/receiver/batch/cancelBatch',
                                method: 'POST',
                                params: {
                                    batchIds: batchIds
                                },
                                reader: {
                                    type: 'json',
                                    root: 'data'
                                },
                                success: function() {
                                    Ext.Msg.alert(label_BATCHES_CANCELED);
                                    gridBatch.store.remove(records);
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
                            gridBatch.getView().refresh();
                        }
                    }
                }]
            }],
            viewConfig:{
                markDirty:false
            }
        }]
    });
});
