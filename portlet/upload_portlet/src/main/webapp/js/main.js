Ext.require([
    'Ext.Msg',
    'Ext.panel.*',
    'Ext.form.*',
    'Ext.selection.CellModel',
    'Ext.grid.*',
    'Ext.data.*'
]);


Ext.onReady(function(){

    var uploadStore = Ext.create('Ext.data.Store', {
        fields: ['name', 'size', 'file', 'status']
    });

    var postDocument = function(url, store, i) {
        var xhr = new XMLHttpRequest();
        var fd = new FormData();
        xhr.open("POST", url, true);
        fd.append('file', store.getAt(i).data.file);
        xhr.onreadystatechange = function() {
            if (xhr.readyState == 4 && xhr.status == 200) {
                store.getAt(i).data.status = label_UPLOADED;
                store.getAt(i).commit();
            } else if (xhr.readyState == 4 && xhr.status != 200) {
                store.getAt(i).data.status = label_ERROR+xhr.responseText;
                store.getAt(i).commit();
            }
        };
        xhr.send(fd);
    };
    var panel = Ext.create('Ext.panel.Panel', {
        height: 600,
        width: '100%',
        hidden: isDataManager,
        renderTo: 'upload-content',
        id: 'NoAccessPanel',
        title: label_TITLE,
        html: '<p>Нет прав для загрузки</p>'
    });

    var panel = Ext.create('Ext.tab.Panel', {
            height: 600,
            width: '100%',
            hidden: !isDataManager,
            renderTo: 'upload-content',
            id: 'MainTabPanel',
            items: [{
                xtype: 'panel',
                title: label_TITLE,
                items: [{
                    multiSelect: true,
                    xtype: 'grid',
                    id: 'UploadGrid',
                    height: 400,
                    columns: [{
                        header: label_FILE_NAME,
                        dataIndex: 'name',
                        flex: 2
                    }, {
                        header: label_FILE_SIZE,
                        dataIndex: 'size',
                        flex: 1,
                        renderer: Ext.util.Format.fileSize
                    }, {
                        header: label_STATUS,
                        dataIndex: 'status',
                        flex: 1,
                        renderer: function(value, metaData, record, rowIndex, colIndex, store) {
                            var color = "grey";
                            if (value === label_READY) {
                                color = "blue";
                            } else if (value === label_UPLOADING) {
                                color = "orange";
                            } else if (value === label_UPLOADED) {
                                color = "green";
                            } else if (value === label_MAIN_ERROR) {
                                color = "red";
                            }
                            value = "<span style='color:"+color+"' >"+value+"</span>";
                            return value;
                        }
                    }],

                    viewConfig: {
                        emptyText: label_INFO,
                        deferEmptyText: false
                    },
                    store: uploadStore,

                    listeners: {

                        drop: {
                            element: 'el',
                            fn: 'drop'
                        },

                        dragstart: {
                            element: 'el',
                            fn: 'addDropZone'
                        },

                        dragenter: {
                            element: 'el',
                            fn: 'addDropZone'
                        },

                        dragover: {
                            element: 'el',
                            fn: 'addDropZone'
                        },

                        dragleave: {
                            element: 'el',
                            fn: 'removeDropZone'
                        },

                        dragexit: {
                            element: 'el',
                            fn: 'removeDropZone'
                        },

                    },

                    noop: function (e) {
                        e.stopEvent();
                    },

                    addDropZone: function (e) {
                        if (!e.browserEvent.dataTransfer || Ext.Array.from(e.browserEvent.dataTransfer.types).indexOf('Files') === -1) {
                            return;
                        }

                        e.stopEvent();

                        this.addCls('drag-over');
                    },

                    removeDropZone: function (e) {
                        var el = e.getTarget(),
                            thisEl = this.getEl();

                        e.stopEvent();

                        if (el === thisEl.dom) {
                            this.removeCls('drag-over');
                            return;
                        }

                        while (el !== thisEl.dom && el && el.parentNode) {
                            el = el.parentNode;
                        }

                        if (el !== thisEl.dom) {
                            this.removeCls('drag-over');
                        }

                    },

                    drop: function (e) {
                        e.stopEvent();
                        Ext.Array.forEach(Ext.Array.from(e.browserEvent.dataTransfer.files), function (file) {
                            uploadStore.add({
                                file: file,
                                name: file.name,
                                size: file.size,
                                status: label_READY

                            });
                        });
                        this.removeCls('drag-over');
                    },

                    tbar: [{
                        xtype: 'filefield',
                        hideLabel: true,
                        buttonOnly: true,
                        buttonText: label_CHOOSE,
                        listeners:{
                            afterrender:function(cmp){
                                cmp.fileInputEl.set({
                                    multiple:'multiple'
                                });
                            },
                            change:function(cmp){
                                Ext.Array.forEach(Ext.Array.from(cmp.fileInputEl.dom.files), function (file) {
                                    uploadStore.add({
                                        file: file,
                                        name: file.name,
                                        size: file.size,
                                        status: label_READY

                                    });
                                });
                                cmp.reset();
                            }
                        }
                    }, {
                        xtype: 'tbseparator',
                    }, {
                        text: label_TO_UPLOAD,
                        handler: function () {
                            for (var i = 0; i < uploadStore.data.items.length; i++) {
                                if (!(uploadStore.getAt(i).data.status === label_UPLOADED)) {
                                    uploadStore.getAt(i).data.status = label_UPLOADING;
                                    uploadStore.getAt(i).commit();
                                    postDocument(dataUrl+"/receiver/batch/uploadBatch?userId=" + userId + '&isNb=' + isNb, uploadStore, i);
                                }
                            }

                        }
                    }, {
                        xtype: 'tbseparator',
                    },{
                        text: label_DELETE_ALL,
                        handler: function () {
                            uploadStore.reload();
                        }
                    }, {
                        xtype: 'tbseparator',
                    }, {
                        text: label_DELETE_UPLOAD,
                        handler: function () {
                            for (var i = 0; i < uploadStore.data.items.length; i++) {
                                var record = uploadStore.getAt(i);
                                if ((record.data.status === label_UPLOADED)) {
                                    uploadStore.remove(record);
                                    i--;
                                }
                            }
                        }
                    },{
                        xtype: 'tbseparator',
                    }, {
                        text: label_DELETE_CHOSEN,
                        handler: function () {
                            uploadStore.remove(Ext.getCmp('UploadGrid').getSelectionModel().getSelection());
                        }
                    }]

                }],
                padding: 20
            }, {
                xtype: 'panel',
                hidden: true,
                title: label_EDS,
                items: [
                    {
                        xtype: 'panel',
                        border: false,
                        title: '',
                        layout: {
                            type: 'hbox',
                            align: 'middle',
                            pack: 'center'
                        },
                        items: [
                            {
                                xtype: 'checkboxfield',
                                disabled: true,
                                checked: false,
                                cls: 'checkBox',
                                margin: '10 0 0 0',
                                fieldLabel: '',
                                boxLabel: label_SENDING
                            }
                        ]
                    }
                ]
            }, {
                xtype: 'panel',
                hidden: true,
                title: label_DATE_SETTING,
                items: [
                    {
                        xtype: 'panel',
                        border: false,
                        padding: 10,
                        title: '',
                        items: [
                            {
                                xtype: 'datefield',
                                fieldLabel: label_ORGANIZATION,
                                labelAlign: 'top',
                                labelStyle: 'font-weight: bold;',
                                format: 'd.m.Y'
                            }
                        ]
                    }
                ]
            }],
            tabBar: {
                xtype: 'tabbar',
                layout: {
                    type: 'hbox',
                    align: 'stretch'
                }
            }
        });
});