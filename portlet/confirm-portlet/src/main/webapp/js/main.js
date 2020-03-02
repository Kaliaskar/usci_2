/**
 * @author Zhanar Akhmetova
 */

Ext.onReady(function() {
    function showConfirmWindow() {
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

        var signingPanel = Ext.create('Ext.panel.Panel', {
            id: 'signingPanel',
            margin: 0,
            width: '100%',
            height: 400,
            autoScroll: true,
            title: 'ЭЦП',
            titleCollapse: false,
            listeners: {
                afterrender: function () {
                    Ext.getCmp('profilesCombo').setValue(profilesStore.getAt(0).data.name);
                    Ext.getCmp('certsCombo').setValue(certificatesStore.getAt(0).data.name);
                }
            },
            items: [{
                xtype: 'button',
                margin: '10 0 0 10',
                text: label_DOWN_PRO,
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

                        store.commitChanges();
                    }
                }
            },
                {
                    xtype: 'combobox',
                    id: 'profilesCombo',
                    store: profilesStore,
                    margin: '10 0 0 10',
                    labelWidth: '40%',
                    width: '60%',
                    editable : false,
                    valueField: 'name',
                    displayField: 'name',
                    fieldLabel: label_SPISOK,
                    labelAlign: 'left',
                    labelStyle: 'font-weight: bold;'
                },
                {
                    xtype: 'checkboxfield',
                    margin: '10 0 0 10',
                    cls: 'checkBox',
                    fieldLabel: '',
                    boxLabel: label_PROTECTED,
                    listeners: {
                        change: function (field, newValue, oldValue, options) {
                            passfield = Ext.getCmp('profilePasword');
                            if (newValue == '1') {
                                passfield.show();
                            } else if (newValue == '0') {
                                passfield.hide();
                            }
                        }
                    }
                },
                {
                    xtype: 'textfield',
                    id: 'profilePasword',
                    hidden: true,
                    margin: '10 0 0 10',
                    fieldLabel: 'Пароль профиля',
                    labelWidth: '40%',
                    width: '60%',
                    inputType: 'password'
                },
                {
                    xtype: 'button',
                    margin: '10 0 0 10',
                    text: label_GET_SERT,
                    listeners: {
                        click: function () {
                            var profile = Ext.getCmp('profilesCombo').getValue();
                            var password = Ext.getCmp('profilePasword').getValue();

                            var certificates = getCertificates(profile, password);
                            if (!certificates || (certificates.length === 0)) {
                                Ext.Msg.alert(label_GET_ERROR);
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
                },
                {
                    xtype: 'combobox',
                    id: 'certsCombo',
                    store: certificatesStore,
                    margin: '10 0 0 10',
                    labelWidth: '40%',
                    width: '60%',
                    editable : false,
                    valueField: 'name',
                    displayField: 'name',
                    fieldLabel: 'Сертификат',
                    labelAlign: 'left',
                    labelStyle: 'font-weight: bold;'
                }]
        });

        var windowId = 'windowConfirm';

        if (Ext.getCmp(windowId))
            Ext.getCmp(windowId).destroy();

        var buttonConfirm = Ext.create('Ext.button.Button', {
            text: label_SIGN,
            handler: function() {
                var profile = Ext.getCmp('profilesCombo').getValue();
                if (isEmpty(profile) || profile.indexOf(label_PRESS) != -1) {
                    Ext.Msg.alert(label_ERROR, label_CHOOSE_P);
                    return;
                }

                var certificate = Ext.getCmp('certsCombo').getValue();
                if (isEmpty(certificate) || certificate.indexOf(label_PRESS) != -1) {
                    Ext.Msg.alert(label_ERROR, label_CHOOSE_S);
                    return;
                }

                var password = Ext.getCmp('profilePasword').getValue();

                var loadMask = new Ext.LoadMask(Ext.getCmp(windowId), {msg: label_WAIT});
                loadMask.show();

                var confirmData = confirmGrid.getSelectionModel().getLastSelected().data;
                var documentHash = null;

                var userName = getUserNameByCertificate(certificate);

                // получаем хэш документа чтобы подписывать
                Ext.Ajax.request({
                    async: false,
                    url: dataUrl + '/core/confirm/getConfirmDocumentHash',
                    method: 'GET',
                    params: {
                        userId: userId,
                        confirmId: confirmData.id,
                        userName: userName
                    },
                    success: function(response, opts) {
                        var data = response.responseText;
                        documentHash = data;
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

                        return;
                    }
                });

                var pkcs7 = signHash(documentHash, certificate, profile, password);

                Ext.Ajax.request({
                    url: dataUrl + '/core/confirm/approve',
                    method: 'POST',
                    params: {
                        userId: userId,
                        confirmId: confirmData.id,
                        documentHash: documentHash,
                        signature: pkcs7,
                        userName: userName
                    },
                    success: function (response, opts) {
                        loadMask.hide();

                        // вызываем метод чтобы заново вытащить текущий статус отчета
                        // то есть после операций в бэке необходимо обновить реквизиты
                        showConfirmInfo();

                        Ext.Msg.alert(label_INFO, label_REP_CONFIRMED);

                        window.close();
                    },
                    failure: function (response, opts) {
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
        });

        var window = new Ext.Window({
            id: windowId,
            modal: 'true',
            bodyPadding: '2',
            width: 600,
            closable: true,
            title: label_CONFIRM,
            items: [
                signingPanel
            ],
            tbar: [
                buttonConfirm,
                {
                    text: label_SHOW,
                    handler: function() {
                        var profile = Ext.getCmp('profilesCombo').getValue();
                        if (isEmpty(profile) || profile.indexOf(label_PRESS) != -1) {
                            Ext.Msg.alert(label_ERROR, label_CHOOSE_P);
                            return;
                        }

                        var certificate = Ext.getCmp('certsCombo').getValue();
                        if (isEmpty(certificate) || certificate.indexOf(label_PRESS) != -1) {
                            Ext.Msg.alert(label_ERROR, label_CHOOSE_S);
                            return;
                        }

                        var confirmData = confirmGrid.getSelectionModel().getLastSelected().data;

                        var loadMask = new Ext.LoadMask(Ext.getCmp(windowId), {msg: label_WAIT});
                        loadMask.show();

                        var userName = getUserNameByCertificate(certificate);

                        var xhr = new XMLHttpRequest();
                        xhr.open('GET', dataUrl + '/core/confirm/createConfirmDocument?confirmId=' + confirmData.id + '&userId='+userId + '&userName=' + userName, true);
                        xhr.responseType = 'arraybuffer';
                        xhr.onload = function (oEvent) {
                            if (xhr.status == 200) {
                                var responseArray = new Uint8Array(this.response);
                                var blob = new Blob([responseArray], {type: 'application/pdf'});
                                saveAs(blob, 'Соглашение.pdf');
                            } else {
                                Ext.Msg.alert(label_ERROR,'');
                            }

                            loadMask.hide();
                        };

                        xhr.send();
                    }
                }]
        });

        window.show();
    }

    function showConfirmInfo() {
        Ext.getCmp('buttonBack').setVisible(true);

        var confirmData = confirmGrid.getSelectionModel().getLastSelected().data;

        var messageGrid = Ext.getCmp('messageGrid');
        messageGrid.store.load({
            params: {
                confirmId: confirmData.id
            },
            scope: this
        });

        messageGrid.getView().refresh();

        var confirmStageGrid = Ext.getCmp('confirmStageGrid');
        confirmStageGrid.store.load({
            params: {
                confirmId: confirmData.id
            },
            scope: this
        });

        confirmStageGrid.getView().refresh();

        Ext.Ajax.request({
            url: dataUrl + '/core/confirm/getConfirmJson',
            method: 'GET',
            params: {
                confirmId: confirmData.id
            },
            reader: {
                type: 'json',
                root: 'data'
            },
            success: function (response, opts) {
                var confirm = Ext.decode(response.responseText);

                buttonConfirm.setDisabled(confirm.statusId == 300);

                var firstBatchLoadTime = null;
                if (confirm.firstBatchLoadTime)
                    firstBatchLoadTime = Ext.Date.format(new Date(confirm.firstBatchLoadTime), 'd.m.Y H:i:s');

                var lastBatchLoadTime = null;
                if (confirm.lastBatchLoadTime)
                    lastBatchLoadTime = Ext.Date.format(new Date(confirm.lastBatchLoadTime), 'd.m.Y H:i:s');

                var reportDate = Ext.Date.format(new Date(confirm.reportDate), 'd.m.Y');

                Ext.getCmp('edConfirmRespName').setText(confirmData.respondentName);
                Ext.getCmp('edConfirmRepDate').setText(reportDate);
                Ext.getCmp('edConfirmProduct').setText(confirm.productName?confirm.productName: label_UNKNOWN);
                Ext.getCmp('edConfirmStatusName').setText(confirm.statusName?confirm.statusName: label_UNKNOWN);
                Ext.getCmp('edConfirmFirstBatchLoadTime').setText(firstBatchLoadTime);
                Ext.getCmp('edConfirmLastBatchLoadTime').setText(lastBatchLoadTime);
                Ext.getCmp('edConfirmCrossCheckResult').setText(confirm.crossCheckStatusName?confirm.crossCheckStatusName:label_UNKNOWN);
            },
            failure: function (response, opts) {
                Ext.Msg.alert(label_ERROR,'');
            }
        });

        var layout = Ext.getCmp('mainPanel').getLayout();
        if (layout.getNext()) {
            layout.next();
        }
    }

    var uploadStore = Ext.create('Ext.data.Store', {
        fields: ['name', 'size', 'file', 'status']
    });

    Ext.define('fileModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'fileName']
    });

    Ext.define('respondentModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'name', 'shortName', 'code', 'subjectType']
    });

    Ext.define('confirmModel', {
        extend: 'Ext.data.Model',
        fields: ['respondentId', 'respondentName', 'statusId', 'reportDate', 'firstBatchLoadTime', 'lastBatchLoadTime', 'editTime', 'userName', 'statusName', 'productName']
    });

    Ext.define('confirmStageModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'statusName', 'stageDate', 'userId', 'userName', 'userPosName']
    });

    Ext.define('messageModel', {
        extend: 'Ext.data.Model',
        fields: ['id', 'userName', 'sendDate', 'text', 'confirmId', 'files']
    });

    var bankRFilter = new Ext.util.Filter({
        filterFn: function (item) {
            var obj = item.get('subjectType');
            return obj.id != '9' ? true : false;
        }
    });

    var bvuFilter = new Ext.util.Filter({
        filterFn: function (item) {
            var obj = item.get('subjectType');
            return obj.id != '3' ? true : false;
        }
    });

    var bpuFilter = new Ext.util.Filter({
        filterFn: function (item) {
            var obj = item.get('subjectType');
            return obj.id != '4' ? true : false;
        }
    });

    var elseorgFilter = new Ext.util.Filter({
        filterFn: function (item) {
            var obj = item.get('subjectType');
            return obj.id != '1' ? true : false;
        }
    });

    var ipotechFilter = new Ext.util.Filter({
        filterFn: function (item) {
            var obj = item.get('subjectType');
            return obj.id != '2' ? true : false;
        }
    });

    var messageStore = Ext.create('Ext.data.Store', {
        id: 'messageStore',
        model: 'messageModel',
        autoLoad: false,
        listeners: {
            load: function () {
                messageGrid.getSelectionModel().select(0);
            },
            scope: this
        },
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/confirm/getMessagesByConfirmId',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    var fileStore = Ext.create('Ext.data.Store', {
        id: 'fileStore',
        model: 'fileModel',
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/confirm/getFilesByMessageId',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: ''
            }
        }
    });

    var messageGrid = Ext.create('Ext.grid.Panel', {
        id: 'messageGrid',
        store: messageStore,
        multiSelect: true,
        viewConfig: { emptyText: label_NO_DATA },
        height: 100,
        title: label_MESSAGES,
        columns: [{
            text: label_SENDLER,
            dataIndex: 'userName',
            flex: 1
        },
            {
                text: label_TEXT,
                flex: 1,
                dataIndex: 'text',
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    return !value? label_NO_MESSAGE: value;
                }
            },
            {
                text: label_DATE_TIME,
                flex: 1,
                dataIndex: 'sendDate',
                renderer: Ext.util.Format.dateRenderer('d.m.Y H:i:s')
            },
            {
                text: label_ATTACH_FILES,
                flex: 1,
                dataIndex: 'files',
                renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                    var text = '';
                    for (var i = 0; i < value.length; i++) {
                        var file = value[i];
                        if (i > 0)
                            text += ', ';

                        text += file.fileName;
                    }
                    return text;
                }
            }],
        listeners: {
            cellclick: function (grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts, but) {
                var messageData = messageGrid.getSelectionModel().getLastSelected().data;

                fileGrid.store.load({
                    params: {
                        messageId: messageData.id
                    },
                    scope: this
                });

                fileGrid.getView().refresh();
            }
        }
    });

    var fileGrid = Ext.create('Ext.grid.Panel', {
        id: 'fileGrid',
        store: fileStore,
        height: 100,
        viewConfig: { emptyText: label_NO_ATTACH },
        title: label_ATTACH_FILES,
        columns: [{
            header: label_FILE_NAME,
            dataIndex: 'fileName',
            flex: 1
        },
            {
                header: label_DOWNLOAD,
                xtype: 'actioncolumn',
                width: 40,
                flex: 1,
                sortable: false,
                items: [{
                    icon: contextPathUrl + '/pics/download.png',
                    tooltip: label_DOWNLOAD,
                    handler: function (grid, rowIndex, colIndex) {
                        var file = fileStore.getAt(rowIndex).data;

                        var xhr = new XMLHttpRequest();
                        xhr.open('GET', dataUrl + '/core/confirm/getMessageFileContent?fileId=' + file.id, true);
                        xhr.responseType = 'arraybuffer';
                        xhr.onload = function (oEvent) {
                            if (xhr.status == 200) {
                                var responseArray = new Uint8Array(this.response);
                                var blob = new Blob([responseArray]);
                                saveAs(blob, file.fileName);
                            } else {
                                Ext.Msg.alert(label_ERROR,'');
                            }
                        };
                        xhr.send();
                    }
                }]
            }]
    });

    var respondentStore = Ext.create('Ext.data.Store', {
        id: 'respondentStore',
        model: 'respondentModel',
        remoteSort: true,
        autoLoad: true,
        listeners: {
            load: function () {
                respondentGrid.getSelectionModel().selectAll();
            },
            scope: this
        },
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/respondent/getRespondentList',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        }
    });

    var confirmStageStore = Ext.create('Ext.data.Store', {
        id: 'confirmStageStore',
        model: 'confirmStageModel',
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/confirm/getConfirmStageJsonList',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: '',
                totalProperty: 'total'
            }
        }
    });

    var confirmStageGrid = Ext.create('Ext.grid.Panel', {
        title: label_HISTORY,
        viewConfig: { emptyText: label_NO_DATA },
        id: 'confirmStageGrid',
        height: 150,
        store: confirmStageStore,
        columns: [{
            text: label_USER,
            dataIndex: 'userName',
            width: 200,
            heigt: 200,
            flex: 1
        },
            {
                text: label_POSITION,
                dataIndex: 'userPosName',
                flex: 1
            },
            {
                text: label_STATUS,
                dataIndex: 'statusName',
                flex: 1
            },
            {
                text: label_D_T,
                dataIndex: 'stageDate',
                renderer: Ext.util.Format.dateRenderer('d.m.Y H:i:s'),
                flex: 1
            },
            {
                header: label_DOWNLOAD_CON,
                xtype: 'actioncolumn',
                width: 40,
                flex: 1,
                sortable: false,
                items: [{
                    icon: contextPathUrl + '/pics/download.png',
                    tooltip: label_DOWNLOAD,
                    handler: function (grid, rowIndex, colIndex) {
                        var confirmStage = confirmStageStore.getAt(rowIndex).data;

                        var xhr = new XMLHttpRequest();
                        xhr.open('GET', dataUrl + '/core/confirm/getConfirmDocument?id=' + confirmStage.id, true);
                        xhr.responseType = 'arraybuffer';
                        xhr.onload = function (oEvent) {
                            if (xhr.status == 200) {
                                var responseArray = new Uint8Array(this.response);
                                var blob = new Blob([responseArray]);
                                saveAs(blob, 'Соглашение.pdf');
                            } else {
                                Ext.Msg.alert(label_ERROR,'');
                            }
                        };
                        xhr.send();
                    }
                }]
            }]
    });

    var respondentGrid = Ext.create('Ext.grid.Panel', {
        id: 'respondentGrid',
        multiSelect: true,
        hideHeaders: true,
        store: respondentStore,
        columns: [{
            text: label_RESP,
            dataIndex: 'name',
            width: 200,
            heigt: 200,
            flex: 1
        }]
    });

    var confirmStore = Ext.create('Ext.data.Store', {
        id: 'confirmStore',
        multiSelect: true,
        model: 'confirmModel',
        remoteSort: true,
        autoLoad: !isNb,
        proxy: {
            type: 'ajax',
            url: dataUrl + '/core/confirm/getConfirmList',
            actionMethods: {
                read: 'GET'
            },
            extraParams: {
                userId: userId,
                isNb: isNb
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        }
    });

    var confirmGrid = Ext.create('Ext.grid.Panel', {
        id: 'confirmGrid',
        multiSelect: true,
        store: confirmStore,
        viewConfig: { emptyText: label_NO_DATA },
        columns: [{
            text: label_RESP_NAME,
            dataIndex: 'respondentName',
            flex: 1
        },
            {
                text: label_PRODUCT,
                dataIndex: 'productName',
                flex: 1
            },
            {
                text: label_STATUS,
                dataIndex: 'statusName',
                flex: 1
            },
            {
                text: label_REP_DATE,
                dataIndex: 'reportDate',
                format: 'Y.m.d',
                renderer: Ext.util.Format.dateRenderer('d.m.Y'),
                flex: 1
            },
            {
                text: label_FIRST_DATE,
                dataIndex: 'firstBatchLoadTime',
                format: 'd.m.Y H:i:s',
                renderer: Ext.util.Format.dateRenderer('d.m.Y H:i:s'),
                flex: 1
            },
            {
                text: label_LAST_DATE,
                dataIndex: 'lastBatchLoadTime',
                format: 'd.m.Y H:i:s',
                renderer: Ext.util.Format.dateRenderer('d.m.Y H:i:s'),
                flex: 1
            }],
        listeners: {
            cellclick: function (grid, td, cellIndex, newValue, tr, rowIndex, e, eOpts, but) {
                showConfirmInfo();
            }
        }
    });

    var buttonShowConfirms = Ext.create('Ext.button.Button', {
        id: 'buttonShowConfirms',
        text: label_VIEW,
        handler: function() {
            if (isNb) {
                Ext.getCmp('downPanel').setVisible(true);

                var respondentIds = [];
                for (var i = 0; i < respondentGrid.store.getCount(); i++) {
                    if (respondentGrid.getSelectionModel().isSelected(i)) {
                        respondentIds.push(respondentGrid.store.getAt(i).data.id);
                    }
                }

                confirmGrid.store.load({
                    params: {
                        userId: userId,
                        isNb: isNb,
                        respondentIds: respondentIds,
                        reportDate: Ext.Date.format(Ext.getCmp('edRepDate').getValue(), 'Y-m-d')
                    },
                    scope: this
                });
            } else {
                confirmGrid.store.load({
                    params: {
                        userId: userId,
                        isNb: isNb,
                        reportDate: Ext.Date.format(Ext.getCmp('edRepDate').getValue(), 'Y-m-d')
                    },
                    scope: this
                });
            }

            confirmGrid.getView().refresh();
        }
    });

    var buttonConfirm = Ext.create('Ext.button.Button', {
        id: 'buttonConfirm',
        text: label_CONF,
        handler: function() {
            showConfirmWindow();
        }
    });

    if (isNb) {
        // кнопки регулятора не доступны респондентам
        buttonConfirm.setVisible(false);
    }

    var buttonBack = Ext.create('Ext.button.Button', {
        id: 'buttonBack',
        hidden: true,
        text: label_BACK,
        handler: function() {
            Ext.getCmp('buttonBack').setVisible(false);

            var layout = mainPanel.getLayout();
            if (layout.getPrev()) {
                layout.prev();
            }
        }
    });

    var buttonAddMessage = Ext.create('Ext.button.Button', {
        id: 'buttonAddMessage',
        text: label_ADD_MESS,
        handler: function() {
            var text = Ext.getCmp('newMessageText').getValue();
            if (text.length == 0) {
                Ext.Msg.alert(label_ERROR, label_FILL);
                return;
            }

            var loadMask = new Ext.LoadMask(Ext.getCmp('mainPanel'), {msg: label_WAIT});
            loadMask.show();

            var xhr = new XMLHttpRequest();
            var formData = new FormData();

            var confirmData = Ext.getCmp('confirmGrid').getSelectionModel().getLastSelected().data;

            var uploadUrl = dataUrl + '/core/confirm/addConfirmMessage?confirmId=' +
                confirmData.id + '&userId=' + userId + '&text=' + text;

            xhr.open('POST', uploadUrl, true);

            xhr.onreadystatechange = function() {
                loadMask.hide();

                if (xhr.readyState == 4 && xhr.status == 200) {
                    Ext.Msg.alert(label_INFO, label_SENDED_MESS);
                } else if (xhr.readyState == 4 && xhr.status != 200) {
                    Ext.Msg.alert(label_ERROR,'');
                }

                messageGrid.getStore().reload();
                messageGrid.getView().refresh();

                var lblAttachments = Ext.getCmp('lblAttachments');
                lblAttachments.setText('');
            };

            for (var i = 0; i < uploadStore.data.items.length; i++) {
                var data = uploadStore.getAt(i).data;
                formData.append('file', data.file);
            }

            // удаляю все прикрепленные файлы
            uploadStore.reload();

            xhr.send(formData);
        }
    });

    var newMessagePanel = Ext.create('Ext.Panel', {
        xtype: 'panel',
        autoScroll: true,
        id: 'messagePanel',
        title: label_NEW_MESS,
        layout: 'vbox',
        tbar: [buttonAddMessage,
            {xtype: 'label', text: label_ATTACHES},
            {xtype: 'label', id: 'lblAttachments', text: label_NO_FILES}
        ],
        bbar: [{
            xtype: 'filefield',
            name: 'file',
            id: 'attachFile',
            buttonOnly: true,
            hideLabel: true,
            buttonText: label_ATTACHING,
            msgTarget: 'side',
            listeners: {
                afterrender: function(cmp) {
                    cmp.fileInputEl.set({
                        multiple:'multiple'
                    });
                },
                change: function (cmp) {
                    var lblAttachments = Ext.getCmp('lblAttachments');
                    lblAttachments.setText('');

                    uploadStore.reload();

                    var attachments = '';
                    Ext.Array.forEach(Ext.Array.from(cmp.fileInputEl.dom.files), function (file) {
                        uploadStore.add({
                            file: file,
                            name: file.name,
                            size: file.size
                        });

                        if (attachments.length > 0)
                            attachments += ', ';

                        attachments += file.name;
                    });

                    lblAttachments.setText(attachments);
                }
            }
        }],
        items: [{
            xtype: 'htmleditor',
            id: 'newMessageText',
            width: '100%',
            height: 100
        }]
    });

    var confirmInfoPanel = Ext.create('Ext.Panel', {
        title: label_DETAILS,
        height: 200,
        width: '100%',
        layout: {
            type: 'table',
            columns: 2,
            tableAttrs: {
                style: {
                    width: '60%'
                }
            }
        },
        items: [{
            xtype: 'label',
            text: label_RESPONDENT
        },
            {
                xtype: 'label',
                id: 'edConfirmRespName'
            },
            {
                xtype: 'label',
                text: label_FIRST_DATE
            },
            {
                xtype: 'label',
                id: 'edConfirmFirstBatchLoadTime'
            },
            {
                xtype: 'label',
                text: label_LAST_DATE
            },
            {
                xtype: 'label',
                id: 'edConfirmLastBatchLoadTime'
            },
            {
                xtype: 'label',
                text: label_PRODUCT
            },
            {
                xtype: 'label',
                id: 'edConfirmProduct'
            },
            {
                xtype: 'label',
                text: label_CROSSCHECK
            },
            {
                xtype: 'label',
                id: 'edConfirmCrossCheckResult'
            },
            {
                xtype: 'label',
                text: label_REP_DATE
            },
            {
                xtype: 'label',
                id: 'edConfirmRepDate'
            },
            {
                xtype: 'label',
                text: label_STATUS
            },
            {
                xtype: 'label',
                id: 'edConfirmStatusName'
            },
            buttonConfirm]
    });

    var confirmDataPanel = Ext.create('Ext.Panel', {
        title: '',
        items: [confirmInfoPanel, confirmStageGrid,
            messageGrid, fileGrid, newMessagePanel]
    });

    var filterPanel = Ext.create('Ext.Panel', {
        title: label_RESP,
        hidden: !isNb,
        height: 50,
        layout: {
            type: 'hbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'checkbox',
            boxLabel: label_R,
            id: 'cbBankDev',
            checked: 'true',
            listeners: {
                change: function (field, newValue, oldValue, options) {
                    grid = Ext.getCmp('respondentGrid');
                    if (newValue == '1') {
                        grid.store.removeFilter(bankRFilter);
                    } else if (newValue == '0') {
                        grid.store.addFilter(bankRFilter);
                    }
                }
            }
        },
            {
                xtype: 'checkbox',
                boxLabel: label_VU,
                id: 'cbBankSecondLev',
                checked: 'true',
                listeners: {
                    change: function (field, newValue, oldValue, options) {
                        grid = Ext.getCmp('respondentGrid');
                        if (newValue == '1') {
                            grid.store.removeFilter(bvuFilter);
                        } else if (newValue == '0') {
                            grid.store.addFilter(bvuFilter);
                        }
                    }
                }
            },
            {
                xtype: 'checkbox',
                boxLabel: label_PU,
                id: 'cbBankFirstLev',
                checked: 'true',
                listeners: {
                    change: function (field, newValue, oldValue, options) {
                        grid = Ext.getCmp('respondentGrid');
                        if (newValue == '1') {
                            grid.store.removeFilter(bpuFilter);
                        } else if (newValue == '0') {
                            grid.store.addFilter(bpuFilter);
                        }
                    }
                }
            },
            {
                xtype: 'checkbox',
                boxLabel: label_IO,
                id: 'cbIpotekaOrg',
                checked: 'true',
                listeners: {
                    change: function (field, newValue, oldValue, options) {
                        grid = Ext.getCmp('respondentGrid');
                        if (newValue == '1') {
                            grid.store.removeFilter(elseorgFilter);
                        } else if (newValue == '0') {
                            grid.store.addFilter(elseorgFilter);
                        }
                    }
                }
            },
            {
                xtype: 'checkbox',
                boxLabel: label_OTHER,
                id: 'cbOtherOrg',
                checked: 'true',
                listeners: {
                    change: function (field, newValue, oldValue, options) {
                        grid = Ext.getCmp('respondentGrid');
                        if (newValue == '1') {
                            grid.store.removeFilter(ipotechFilter);
                        } else if (newValue == '0') {
                            grid.store.addFilter(ipotechFilter);
                        }
                    }
                }
            },
            {
                xtype: 'button',
                text: label_SELECT_ALL,
                id: 'btnChooseAll',
                heigt: 20,
                listeners: {
                    click: function () {
                        Ext.getCmp('cbBankDev').setValue(true);
                        Ext.getCmp('cbBankSecondLev').setValue(true);
                        Ext.getCmp('cbBankFirstLev').setValue(true);
                        Ext.getCmp('cbIpotekaOrg').setValue(true);
                        Ext.getCmp('cbOtherOrg').setValue(true);
                    }
                }
            },
            {
                xtype: 'button',
                text: label_OFF_SELECTED,
                id: 'btnDeSelectAll',
                heigt: 20,
                listeners: {
                    click: function () {
                        Ext.getCmp('cbBankDev').setValue(false);
                        Ext.getCmp('cbBankSecondLev').setValue(false);
                        Ext.getCmp('cbBankFirstLev').setValue(false);
                        Ext.getCmp('cbIpotekaOrg').setValue(false);
                        Ext.getCmp('cbOtherOrg').setValue(false);
                    }
                }
            }]
    });

    var edRespSearch = {
        xtype: 'textfield',
        id: 'textfield',
        hidden: !isNb,
        name: 'searchfield',
        listeners: {
            change: function (field, newValue, oldValue, options) {
                grid = Ext.getCmp('respondentGrid');

                if (newValue == '') {
                    grid.store.clearFilter();
                    if (Ext.getCmp('cbBankDev').checked == false) {
                        grid.store.addFilter(bankRFilter)
                    };
                    if (Ext.getCmp('cbBankSecondLev').checked == false) {
                        grid.store.addFilter(bvuFilter)
                    };
                    if (Ext.getCmp('cbBankFirstLev').checked == false) {
                        grid.store.addFilter(bpuFilter)
                    };
                    if (Ext.getCmp('cbOtherOrg').checked == false) {
                        grid.store.addFilter(elseorgFilter)
                    };
                    if (Ext.getCmp('cbIpotekaOrg').checked == false) {
                        grid.store.addFilter(ipotechFilter)
                    };
                    grid.getView().refresh();
                } else {
                    grid.store.filter([{
                        property: 'name',
                        value: newValue,
                        anyMatch: true,
                        caseSensitive: false
                    }]);
                }
            }
        }
    };

    var confirmListPanel = Ext.create('Ext.Panel', {
        title: '',
        items: [{
            xtype: 'panel',
            title: '',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [
                filterPanel,
                edRespSearch,
                // панель кредиторов
                {
                    xtype: 'panel',
                    id: 'respondents',
                    hidden: !isNb,
                    title: '',
                    height: 200,
                    overflowY: 'scroll',
                    items: [respondentGrid]
                },
                // панель управления
                {
                    xtype: 'panel',
                    title: '',
                    hidden: !isNb,
                    height: 50,
                    items: [{
                        xtype: 'button',
                        text: label_ALL,
                        id: 'btnChooseAllRespondent',
                        height: 30,
                        width: 200,
                        listeners: {
                            click: function () {
                                Ext.getCmp('respondentGrid').getSelectionModel().selectAll();
                            }
                        }
                    }]
                },
                {
                    xtype: 'panel',
                    title: '',
                    height: 35,
                    items: [
                        {
                            xtype: 'label',
                            type: 'vbox',
                            text: label_REP_DATE
                        },
                        {
                            xtype: 'datefield',
                            id: 'edRepDate',
                            format: 'd.m.Y',
                            anchor: '100%',
                            width: 200,
                            maxValue: new Date()
                        }
                    ]
                },
                {
                    xtype: 'panel',
                    title: '',
                    height: 25,
                    layout: {
                        type: 'hbox'
                    },
                    items: [buttonShowConfirms]
                },
                {
                    xtype: 'panel',
                    id: 'downPanel',
                    title: '',
                    overflowY: 'scroll',
                    height: 400,
                    items: [confirmGrid]
                }
            ]
        }]
    });

    var mainPanel = Ext.create('Ext.Panel', {
        title: label_CONFIRMATION,
        id: 'mainPanel',
        width: 1160,
        layout: 'card',
        items: [confirmListPanel, confirmDataPanel],
        tbar: [
            buttonBack
        ],
        renderTo: 'confirm-content'
    });
});

