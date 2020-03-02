Ext.require([
    'Ext.selection.CellModel',
    'Ext.tab.*',
    'Ext.tree.*',
    'Ext.data.*',
    'Ext.tip.*'
]);

Ext.Ajax.timeout= 3000000;
Ext.override(Ext.data.proxy.Ajax, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.proxy.Server, { timeout: Ext.Ajax.timeout });
Ext.override(Ext.data.Connection, { timeout: Ext.Ajax.timeout });

var prodIds = [];
var respIds = [];

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
    sorters: [{
        property: 'name',
        direction: 'asc'
    }],
    listeners: {
      load: function(me, records, options) {
          Ext.each(records, function(record) {
              respIds.push(record.get('id'));
          });
      }
    }
});

var userProductStore = Ext.create('Ext.data.Store', {
    id: 'userProductStore',
    fields: ['id', 'code', 'name'],
    autoLoad: true,
    proxy: {
        type: 'ajax',
        url: dataUrl + '/core/user/getUserProductJsonList',
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
        load: function(me, records, options) {
            Ext.each(records, function(record) {
                prodIds.push(record.get('id'));
            });
        }
    }
});

function tableReportGrid(reportId, parameterValues){
    var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: 'Идет формирование отчета...' });
    loadMask.show();
    Ext.Ajax.request({
        url: dataUrl + '/report/report/loadTableReport',
        method:'POST',
        params: {
            userId: userId,
            reportId: reportId
        },
        jsonData: parameterValues,
        success: function(result){
            loadMask.hide();
            json = JSON.parse(result.responseText);

            var resultPanel = Ext.getCmp('resultInPanel');
            resultPanel.removeAll();

            if (json.data) {
                var listSqlFields = buildsFiels(json.data);
                var sqlModel = createModel(listSqlFields);

                var grid = createGrid(
                    createStore(sqlModel, json.data),
                    buildColumnsInfo(json.data)
                );
                resultPanel.add(grid);
            }
        }, /*end of success*/
        failure: function(result){
            loadMask.hide();
            Ext.Msg.alert("Ошибка", JSON.parse(result.responseText).errorMessage);
        }

    }); /*end Ajax request*/
} /*end of function*/


// Вывод списка доступных отчетов
function createReportGrid() {
    var reportListStore = Ext.create('Ext.data.Store', {
        id: 'reportListStore',
        model: 'reportListModel',
        autoLoad: true,
        proxy: {
            type: 'ajax',
            url : dataUrl + '/report/report/loadReportList',
            extraParams: {
                reportType: reportType
            },
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        }});

    var reportListGrid = Ext.create('Ext.grid.Panel', {
        store: reportListStore,
        id:'reportListGrid',
        width: 1200,
        height: 590,
        autoScroll: true,
        columns: [{ text: 'Название отчета', dataIndex: 'nameRu',  width: 100, heigt: 100, flex: 1}],
        listeners : {
            itemdblclick: function(grid, rowIndex, e, eOpts) {
                var reportId = reportListGrid.getSelectionModel().getLastSelected().data.id;
                var inParams = reportListGrid.getSelectionModel().getLastSelected().data.inputParameters;
                var reportName = reportListGrid.getSelectionModel().getLastSelected().data.nameRu;
                showParameterPanel(reportId, inParams, reportName);
            }
        }
    });

}



// Список сформированных отчетов
function ReportLoadGrid() {
    // Загрузка данных по сформированным отчетам
    var reportStore = Ext.create('Ext.data.Store', {
        id: 'reportStore',
        model: 'reportModel',
        autoLoad: false,
        proxy: {
            type: 'ajax',
            url : dataUrl + '/report/report/getReportList',
            actionMethods: {
                read: 'GET'
            },
            reader: {
                type: 'json',
                root: 'data',
                totalProperty: 'total'
            }
        }});

    var executedReportGrid = Ext.create('Ext.grid.Panel', {
        store: reportStore,
        id: 'executedReportGrid',
        width: 1200,
        height: 800,
        autoScroll: true,
        padding: 10,
        columns: [{
            xtype: 'gridcolumn',
            text: 'Пользователь',
            dataIndex: 'user',
            flex: 1
        },  {
            xtype: 'gridcolumn',
            text: 'Респондент',
            dataIndex: 'respondent',
            flex: 1
        },  {
            xtype: 'gridcolumn',
            text: 'Продукт',
            dataIndex: 'product',
            flex: 1
        },  {
            xtype: 'gridcolumn',
            text: 'Витрина',
            dataIndex: 'tableName',
            flex: 1
        },  {
            xtype: 'datecolumn',
            text: 'Отчетная дата',
            dataIndex: 'reportDate',
            format: 'd.m.Y',
            flex: 1
        },  {
            xtype: 'datecolumn',
            text: 'Дата начала формирования',
            dataIndex: 'beginDate',
            format: 'd.m.Y H:i:s',
            flex: 1
        },  {
            xtype: 'datecolumn',
            text: 'Дата завершения формирования',
            dataIndex: 'endDate',
            format: 'd.m.Y H:i:s',
            flex: 1
        },  {
            xtype: 'gridcolumn',
            text: 'Статус',
            dataIndex: 'status',
            flex: 1
        },  {
            xtype: 'gridcolumn',
            text: 'Файл',
            dataIndex: 'status',
            renderer: function(value, metaData, record, rowIndex, colIndex, store, view) {
                if (value == "Выполнено") {
                    return '<a href="#">Скачать</a>';
                } else {
                    return null;
                }
            },
            flex: 1
        }],
        dockedItems: [  {
            xtype: 'toolbar',
            dock: 'top',
            items: [{
                xtype: 'button',
                text: 'Обновить',
                listeners: {
                    click: function () {
                        executedReportGrid.store.load({
                            params: {
                                respondentIds: respIds,
                                productIds: prodIds
                            },
                            scope: this
                        });
                        executedReportGrid.getView().refresh();
                    }
                }
            }]
        }],
        listeners : {
            cellclick: function (view, cell, cellIndex, record, row, rowIndex, e) {
                id = record.data.id;

                var linkClicked = (e.target.tagName == 'A');
                var clickedDataIndex =
                    view.panel.headerCt.getHeaderAtIndex(cellIndex).dataIndex;

                if (linkClicked && clickedDataIndex == 'status') {
                    var loadMask = new Ext.LoadMask(Ext.getCmp('executedReports'), {msg: label_WAIT});
                    loadMask.show();

                    var xhr = new XMLHttpRequest();
                    xhr.open("GET", dataUrl+"/report/report/getReportFile?id="+id, true);
                    xhr.responseType = "arraybuffer";
                    xhr.onload = function (oEvent) {
                        if (xhr.status == 200) {
                            var responseArray = new Uint8Array(this.response);
                            var blob = new Blob([responseArray], {type: "application/zip"});
                            var fileName = record.data.tableName+record.data.respondent.id+record.data.beginDate;
                            saveAs(blob, fileName+'.csv.zip');
                        } else {
                            Ext.Msg.alert("",label_ERROR);
                        }
                    };
                    xhr.send();
                    loadMask.hide();
                }
            }
        }
    });
    return executedReportGrid;
}

Ext.onReady(function(reportId){

    createReportGrid();
    reportListGrid = Ext.getCmp('reportListGrid');
    ReportLoadGrid();
    executedReportGrid = Ext.getCmp('executedReportGrid');

    Ext.create('Ext.tab.Panel', {
        renderTo: 'report-content',
        title: 'Выходные формы',
        width: 1200,
        height: 900,
        items:[{
            title: 'Список отчетов',
            id:'reportListTab',
            items: [
                {
                    xtype: 'panel',
                    autoScroll: true,
                    height : '500px',
                    id: 'mainPanel',
                    items:[reportListGrid]
                }
            ]

        },  {
                title: 'Сформированные отчеты',
                id:'executedReports',
                tabConfig: {
                     listeners: {
                         click: function() {
                            executedReportGrid.store.load({
                                params: {
                                    respondentIds: respIds,
                                    productIds: prodIds
                                },
                                scope: this
                            });
                            executedReportGrid.getView().refresh();
                         }
                     }
                },
                items:[   executedReportGrid
                ]

            }
        ]
    });


});
