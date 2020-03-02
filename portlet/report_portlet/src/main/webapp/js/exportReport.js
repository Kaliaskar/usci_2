function constructPanel(reportParam) {
    var component = null;
    var filterPanel =  Ext.create('Ext.form.Panel', {
        id: 'filterPanel',
        layout: 'vbox',
        border: false,
        items: []
    });
    var reportExportPanel =  Ext.create('Ext.panel.Panel', {
        id: 'reportExportPanel',
        autoScroll: true,
        items: [
            {
                xtype: 'panel',
                id: 'headInPanel',
                padding: 10,
                height: 40,
                border: false,
                layout: 'hbox',
                items: [{
                    xtype: 'label',
                    id: 'headInLabel',
                    alignCenter: true
                },
                    {   xtype: 'component',
                        flex: 1
                    },
                    {
                        xtype: 'button',
                        id:'returnButton',
                        text: 'Вернуться к списку отчетов',
                        handler: function() { returnToMainPanel()}
                    }
                ]
            },
            {
                xtype: 'panel',
                id: 'middleInPanel',
                name: 'middleInPanel',
                padding: 10,
                autoScroll: true,
                height: 400,
                border: false
            },
            {
                xtype: 'panel',
                id: 'resultInPanel',
                height: 400,
                padding: 10,
                border: false

            }
        ]
    });

    for(var i=0; i<reportParam.length; i++) {
        if (reportParam[i].type == 'DATE') {
            Ext.create('Ext.form.field.Date', {
                id: reportParam[i].parameterName,
                name: reportParam[i].parameterName,
                anchor: '100%',
                format: 'd.m.Y',
                fieldLabel: reportParam[i].nameRu,
                maxValue: new Date()
            });
        }
        else if (reportParam[i].type == 'VITRINA') {
            var valueStore = Ext.create('Ext.data.Store', {
                id: 'columnStore',
                model: 'valueModel',
                autoLoad: true,
                proxy: {
                    type: 'ajax',
                    url: dataUrl + '/report/report/getValueList',
                    actionMethods: {
                        read: 'GET'
                    },
                    extraParams: {
                        userId: userId,
                        tableName: '',
                        procedureName: reportParam[i].procedureName
                    },
                    reader: {
                        type: 'json',
                        root: ''
                    }
                }
            });
            Ext.create('Ext.form.field.ComboBox', {
                id: reportParam[i].parameterName,
                name: reportParam[i].parameterName,
                store: valueStore,
                valueField:'value',
                displayField:'displayName',
                fieldLabel: reportParam[i].nameRu,
                listeners: {
                    select: function (newValue) {
                        getColomnList(this.getRawValue());
                    }
                }
            });
        }
        else if (reportParam[i].type == 'OPTION') {
            var columnList = Ext.create('Ext.form.CheckboxGroup', {
                id:  reportParam[i].parameterName,
                width: 500,
                columns: 3,
                vartical: true,
                items: []
            });
        }
        else if (reportParam[i].type == 'FILTER') {
            Ext.create('Ext.button.Button', {
                id:reportParam[i].parameterName,
                margin: '10 10 0 0',
                html: 'Добавить фильтр',
                handler:function(){
                    addFilter(Ext.getCmp('SHOWCASE_ID').getRawValue());
                }
            });
        }
        else {
            if (reportParam[i].procedureName != null) {
                var valueStore = Ext.create('Ext.data.Store', {
                    id: 'valueStore',
                    model: 'valueModel',
                    autoLoad: true,
                    proxy: {
                        type: 'ajax',
                        url: dataUrl + '/report/report/getValueList',
                        actionMethods: {
                            read: 'GET'
                        },
                        extraParams: {
                            userId: userId,
                            tableName: '',
                            procedureName: reportParam[i].procedureName
                        },
                        reader: {
                            type: 'json',
                            root: ''
                        }
                    }
                });
            }
            Ext.create('Ext.form.field.ComboBox', {
                id: reportParam[i].parameterName,
                name: reportParam[i].parameterName,
                store: valueStore,
                valueField:'value',
                displayField:'displayName',
                fieldLabel: reportParam[i].nameRu
            });
        }
    }
    Ext.create('Ext.form.field.Checkbox', {
        id:'isEscapeChk',
        cls: 'checkBox',
        width: 600,
        inputValue: '1',
        boxLabel: 'С экранированием символов (для корректного отображения текстовых полей в Excel)',
        checked: true,
        listeners: {
            change: function (field, newValue, oldValue, options) {
                if (newValue == '1') {
                    isEscape = 1;
                } else if (newValue == '0') {
                    isEscape = 0;
                }
            }
        }
    });

    middleInPanel = Ext.getCmp('middleInPanel');
    middleInPanel.add(Ext.getCmp('isEscapeChk'));
    for(var i=0; i < reportParam.length; i++) {
        middleInPanel.add(Ext.getCmp(reportParam[i].parameterName));
    }
    middleInPanel.add(filterPanel);
}

function collectParamaters(reportParams) {
    Ext.Date.patterns={ CustomFormat: "d-m-Y" };
    var repValueData= [];
    var checkBoxValues = [];
    for(var i=0; i < reportParams.length; i++) {
        var repValueClass = {
            order: null,
            name: "",
            display: "",
            value: "",
            type: ""
        };
        if (reportParams[i].type == 'LIST') {
            repValueClass.order = reportParams[i].orderNumber;
            repValueClass.name = reportParams[i].parameterName;
            repValueClass.type = reportParams[i].type;
            repValueClass.value = Ext.getCmp(reportParams[i].parameterName).value;
            repValueClass.display = Ext.getCmp(reportParams[i].parameterName).getRawValue();
        }
        else if (reportParams[i].type == 'OPTION') {
            repValueClass.order = reportParams[i].orderNumber;
            repValueClass.name = reportParams[i].parameterName;
            repValueClass.type = reportParams[i].type;
            var columnList = Ext.getCmp('FIELD_ID');
            columnList.items.each(function(checkbox){
                if (checkbox.getValue() == true) {
                    checkBoxValues.push('\"'+checkbox.boxLabel+'\"');
                }
            });
            repValueClass.value = checkBoxValues.toString();
        }
        else if (reportParams[i].type == 'VITRINA') {
            repValueClass.order = reportParams[i].orderNumber;
            repValueClass.name = reportParams[i].parameterName;
            repValueClass.type = reportParams[i].type;
            repValueClass.value = Ext.getCmp(reportParams[i].parameterName).value;
            repValueClass.display = Ext.getCmp(reportParams[i].parameterName).getRawValue();
        }
        else if (reportParams[i].type == 'FILTER') {
            repValueClass.order = reportParams[i].orderNumber;
            repValueClass.name = reportParams[i].parameterName;
            repValueClass.type = reportParams[i].type;
            var filterFormData = [];
            Ext.getCmp('filterPanel').items.each(function(item){
                var filterFormClass = item.getForm().getValues();
                filterFormData.push(filterFormClass);
            });
            repValueClass.value = getFiltervalue(filterFormData);


        }
        else {
            repValueClass.order = reportParams[i].orderNumber;
            repValueClass.name = reportParams[i].parameterName;
            repValueClass.type = reportParams[i].type;
            repValueClass.value = Ext.Date.format(Ext.getCmp(reportParams[i].parameterName).value, Ext.Date.patterns.CustomFormat);
            repValueClass.display = Ext.Date.format(Ext.getCmp(reportParams[i].parameterName).value, Ext.Date.patterns.CustomFormat);
        }

        repValueData.push(repValueClass);

    }

    console.log(repValueData);
    return repValueData;
}

// Создание панели ввода параметров для отчета

function showParameterPanel(reportId, reportParams, reportName) {
    var isEscape = 1;
    constructPanel(reportParams);
    mainPanel = Ext.getCmp('mainPanel');
    reportExportPanel = Ext.getCmp('reportExportPanel');
    headInLabel = Ext.getCmp('headInLabel');
    middleInPanel = Ext.getCmp('middleInPanel');

    var titleName = 'Выбран отчет "' + reportName + '". Заполните входные параметры.'
    headInLabel.text = titleName;

    var excuteReportButton = Ext.create('Ext.button.Button', {
        id:'excuteReportButton',
        margin: '10 10 0 0',
        text: 'Выгрузить отчет в XLS',
        handler: function() {
            var loadMask = new Ext.LoadMask(Ext.getBody(), { msg: 'Идет формирование отчета...' });
                loadMask.show();

            var params = collectParamaters(reportParams);
            var xhr = new XMLHttpRequest();
            xhr.open("POST", dataUrl + "/report/report/getReportData?userId= "+ userId + "&reportId=" + reportId);
            xhr.timeout = 3000000;
            xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
            xhr.responseType = "arraybuffer";
            xhr.onload = function (oEvent) {

                if (xhr.status == 200) {
                    if (xhr.response.byteLength == 0) {
                        Ext.Msg.alert("","Нет данных");
                    } else {
                        // извлекаю наименование файла из заголовков
                        var fileName = "";
                        var disposition = xhr.getResponseHeader('Content-Disposition');
                        if (disposition && disposition.indexOf('attachment') !== -1) {
                            var fileNameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                            var matches = fileNameRegex.exec(disposition);
                            if (matches != null && matches[1]) {
                                fileName = matches[1].replace(/['"]/g, '');
                            }
                        }
                        var responseArray = new Uint8Array(this.response);
                        var blob = new Blob([responseArray], {type: "application/zip"});
                        saveAs(blob, fileName + ".zip");
                    }
                } else {
                    Ext.Msg.alert("",label_ERROR);
                }
                loadMask.hide();
            };

            xhr.send(JSON.stringify(params));
        }
    });

    var executeReportBtn = Ext.create('Ext.button.Button', {
            id:'executeReportBtn',
            margin: '10 10 0 0',
            text: 'Запустить выгрузку отчета',
            handler: function() {
                Ext.Msg.show({
                    title: '',
                    msg: 'Выгрузка запущена. Смотрите статус во вкладке \"Сформированные отчеты\"',
                    width : 300,
                    buttons: Ext.MessageBox.YES
                });
                var params = collectParamaters(reportParams);
                Ext.Ajax.request({
                    url: dataUrl + '/report/report/executeReport',
                    method: 'POST',
                    params: {
                        userId: userId,
                        isEscape: isEscape
                    },
                    jsonData: params,
                    success: function(response) {
                    },
                    failure: function(response) {
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

    var tableReportButton = Ext.create('Ext.button.Button', {
        id:'tableReportButton',
        margin: '10 10 0 0',
        html: 'Загрузить таблицу отчета',
        handler:function(){
            var params = collectParamaters(reportParams);
            tableReportGrid(reportId, params);
        }
    });

    mainPanel.removeAll();
    //middleInPanel.add(excuteReportButton);
    middleInPanel.add(executeReportBtn);
    //middleInPanel.add(tableReportButton);
    middleInPanel.doLayout();
    mainPanel.add(reportExportPanel);
    mainPanel.doLayout();
}

function returnToMainPanel() {
    mainPanel = Ext.getCmp('mainPanel');
    createReportGrid();
    reportListGrid = Ext.getCmp('reportListGrid');
    mainPanel.removeAll();
    mainPanel.add(reportListGrid);
    mainPanel.doLayout();
}

function checkDateValue(date) {
    var formats = [
        "DD.MM.YYYY",
        "DD.MM.YYYY HH:mm:ss"
    ];
    return moment(date, formats, true).isValid();
    //return!!(function(d){return(d!=='Invalid Date'&&!isNaN(d))})(new Date(date));
};

function getFiltervalue(filterFormData) {
    var result = '';
    for(var i=0; i < filterFormData.length; i++) {
        console.log(checkDateValue(filterFormData[i].filterValue));
        if (checkDateValue(filterFormData[i].filterValue)) {
            result = result + ' AND '  + '\"' + filterFormData[i].columnName + '\"' + ' ' + filterFormData[i].operationValue;
            result = result + ' to_date(\'' + filterFormData[i].filterValue + '\',\'dd.mm.yyyy\')';
        }
        else {
            result = result + ' AND ' + '\"' + filterFormData[i].columnName + '\"' + ' ' + filterFormData[i].operationValue + ' ' + '\'' + filterFormData[i].filterValue + '\'';
        }
    }
    console.log(result);
    return result;
}
