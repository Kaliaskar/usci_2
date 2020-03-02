// get fields
function buildsFiels(json){
    var uniques=[];
    for (i = 0; i < json.length; i++) {
        for(x in json[i]) {
            if (!uniques.includes(x)) {
                uniques.push(x)
            }
        }
    }
    return uniques;
}

// create column info
function buildColumnsInfo(json) {
    var uniques=buildsFiels(json);
    var colsInfo=[];

    if (uniques.length>0){
        for(i=0; i<uniques.length; i++){
            colsInfo.push({
                text: uniques[i],
                dataIndex: uniques[i],
                flex: 0,
                editor: {
                    allowBlank: false
                }
            });
        }
    }
    return colsInfo;
}

// create model
function createModel( listOfField){
    var x = Ext.define('sqlModel', {
        extend: 'Ext.data.Model',
        fields: listOfField
    });
    return x;
}

// create store
function createStore(model, data) {
    var x = Ext.create('Ext.data.Store', {
        model: model
    });
    x.add(data);
    return x;
}

// create grid
function createGrid(model, lst_columns){
    var cellEditing = Ext.create('Ext.grid.plugin.CellEditing', {
        clicksToEdit: 1
    });

    var x = Ext.create('Ext.grid.Panel', {
        id: "sqlGrid",
        header: false,
        title: 'GRID',
        store: model,
        autoSizeColumn: true,
        columns: lst_columns,
        resizable: true,
        height: 400,
        columnLines: true,
        /*overflowY: 'auto',
        overflowX: 'auto',*/
        scroll: 'both',
        selType: 'rowmodel',
        tbar: [
            {
                xtype: 'button',
                cls: 'button-excel',
                title: 'dsfdsfadsfadsf',
                scale: 'large',
                height: 20,
                margin: '0 1 0 1',
                listeners: {
                    click: function () {
                        exportToEXL();
                    }
                }


            }]
    });
    return x;
}
