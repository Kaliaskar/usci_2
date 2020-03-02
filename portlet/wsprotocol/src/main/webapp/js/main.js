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


Ext.Ajax.timeout= 240000;
Ext.override(Ext.data.proxy.Ajax, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.proxy.Server, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.Connection, { timeout: Ext.Ajax.timeout });

Ext.onReady(function () {

    function showXmlWindow(value) {

        var xmlDocument =  new DOMParser().parseFromString(value, "application/xml");
        var xsltDoc = new DOMParser().parseFromString([
            '<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">',
            ' <xsl:output omit-xml-declaration="yes" indent="yes"/>',
            '    <xsl:template match="node()|@*">',
            '      <xsl:copy>',
            '        <xsl:apply-templates select="node()|@*"/>',
            '      </xsl:copy>',
            '    </xsl:template>',
            '</xsl:stylesheet>',
        ].join('\n'), 'application/xml');
        var xsltProcessor = new XSLTProcessor();
        xsltProcessor.importStylesheet(xsltDoc);
        var resultDoc = xsltProcessor.transformToDocument(xmlDocument);
        var serializer = new XMLSerializer();
        var strXmlData = serializer.serializeToString(resultDoc);

        var xmlForm = Ext.create('Ext.form.Panel', {
            id: 'xmlForm',
            region: 'center',
            width: 615,
            fieldDefaults: {
                msgTarget: 'side'
            },
            defaults: {
                anchor: '100%'
            },
            bodyPadding: '5 5 0',
            items: [{
                fieldLabel: 'XML',
                name: 'id',
                readOnly: true,
                xtype: 'textarea',
                value: strXmlData,
                height: 415
            }],
            buttons: [
                Ext.create('Ext.button.Button', {
                    text: 'Отмена',
                    handler: function () {
                        Ext.getCmp('xmlWin').destroy();
                    }
                })
            ]
        });

        xmlWin = new Ext.Window({
            id: "xmlWin",
            layout: 'fit',
            title: 'XML',
            modal: true,
            maximizable: true,
            items: [xmlForm]
        });

        xmlWin.show();
    }

    Ext.Date.patterns={
        CustomFormat: "d.m.Y"
    };

    var requestStore = Ext.create('Ext.data.Store', {
        id: 'requestsStore',
        fields: ['id', 'reportDate', 'requestStatus', 'entitiesCount', 'requestBody', 'responseBody', 'resend'],
        autoLoad: false,
        proxy: {
            timeout: 240000,
            type: 'ajax',
            url: dataUrl+'/wsclient/kgd/getCtrRequestList',
            method: 'GET',
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'totalCount'
            }
        },
        sorters: [{
            property: 'id',
            direction: 'asc'
        }]
    });

    var panel = Ext.create('Ext.panel.Panel', {
        height: 1200,
        margin: 0,
        width: 1200,
        autoScroll: true,
        title: '          ',
        titleCollapse: false,
        renderTo: 'wsprotocol-content',
        id: 'MainPanel',
        items: [{
            xtype: 'datefield',
            id: 'reportDate',
            padding: 5,
            fieldLabel: label_REP_DATE,
            labelAlign: 'top',
            labelStyle: 'font-weight: bold;',
            format: 'd.m.Y'
        }, {
            xtype: 'button',
            margin: '0 0 5 5',
            text: label_SHOW,
            listeners: {
                click: function () {
                    grid = Ext.getCmp("requestGrid");
                    reportDate = Ext.Date.format(Ext.getCmp('reportDate').value, Ext.Date.patterns.CustomFormat);
                    grid.store.load({
                        params: {
                            reportDate: reportDate
                        },
                        scope: this
                    });
                    grid.getView().refresh();
                }
            }
        }, {
            xtype: 'gridpanel',
            viewConfig: { emptyText: label_NO_DATA },
            id: 'requestGrid',
            store: requestStore,
            height: 400,
            width: 1190,
            margin: '5 0 15 5',
            autoScroll: true,
            title: 'Информация о запросах',
            columns: [{
                xtype: 'datecolumn',
                width: 100,
                dataIndex: 'reportDate',
                text: label_DATE_REP,
                format: 'd.m.Y'
            }, {
                xtype: 'gridcolumn',
                width: 200,
                dataIndex: 'requestStatus',
                text: 'Статус',
                renderer: function(value, metaData, record, rowIndex, colIndex, store) {
                    if (value == "SUCCESS") {
                        value = "<span style='color:green' >Запрос прошел успешно</span>";
                        return value;
                    } else {
                        value = "<span style='color:red' >Ошибка</span>";
                        return value;
                    }
                }
            }, {
                xtype: 'gridcolumn',
                width: 200,
                dataIndex: 'entitiesCount',
                text: 'Количество платежей(переводов)'
            }, {
                xtype: 'actioncolumn',
                width: 100,
                dataIndex: 'requestBody',
                text: 'Запрос',
                align: 'center',
                items: [{
                    icon: contextPathUrl + '/pics/xml.png',
                    tooltip: 'Посмотреть XML',
                    handler: function(grid, rowIndex, colIndex) {
                        var rec = grid.getStore().getAt(rowIndex);
                        showXmlWindow(rec.get('requestBody'));
                    }
                }]
            }, {
                xtype: 'actioncolumn',
                width: 100,
                dataIndex: 'responseBody',
                text: 'Ответ',
                align: 'center',
                items: [{
                    icon: contextPathUrl + '/pics/xml.png',
                    tooltip: 'Посмотреть XML',
                    handler: function(grid, rowIndex, colIndex) {
                        var rec = grid.getStore().getAt(rowIndex);
                        showXmlWindow(rec.get('responseBody'));
                    }
                }]
            }, {
                xtype: 'gridcolumn',
                width: 200,
                dataIndex: 'resend',
                text: ' ',
                align: 'center',
                renderer  : function(value, metaData, record, rowIndex, colIndex, store) {
                    if (record.data.requestStatus == "ERROR") {
                        return '<a href="#">Переотправить</a>';
                    } else {
                        return null;
                    }
                }
            }],
            listeners: {
                cellclick: function (view, cell, cellIndex, record, row, rowIndex, e) {

                    var linkClicked = (e.target.tagName == 'A');
                    var clickedDataIndex =
                        view.panel.headerCt.getHeaderAtIndex(cellIndex).dataIndex;

                    if (linkClicked && clickedDataIndex == 'resend') {
                        var loadMask = new Ext.LoadMask(Ext.getCmp('MainPanel'), {msg: label_WAIT});
                        loadMask.show();

                        Ext.Ajax.request({
                            url: dataUrl + '/wsclient/kgd/resendCtrRequest',
                            method: 'POST',
                            waitMsg: 'Идет отправка...',
                            params: {
                                requestId: record.data.id,
                                isUpdate: true,
                                reportDate: Ext.Date.format(new Date(record.data.reportDate), 'd.m.Y')
                            },
                            reader: {
                                type: 'json',
                                root: 'data'
                            },
                            success: function(response, opts) {
                                loadMask.hide();

                                grid = Ext.getCmp("requestGrid");
                                reportDate = Ext.Date.format(Ext.getCmp('reportDate').value, Ext.Date.patterns.CustomFormat);
                                grid.store.load({
                                    params: {
                                        reportDate: reportDate
                                    },
                                    scope: this
                                });
                                grid.getView().refresh();
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
                            }
                        });
                    }
                }
            }
        }]
    });
});
