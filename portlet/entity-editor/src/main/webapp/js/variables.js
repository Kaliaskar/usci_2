/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

var fetchSize = 50;
var timeout = 300000;

// данная переменная нужна чтобы зафиксировать изменения
// запрещаем отправку XML если нет никаких изменений
var editorAction = {
    lastAction: 'none',
    hasUnsavedAction: function() {
        return this.lastAction != 'none';
    },
    commitEdit: function() {
        this.lastAction = 'edit';

        var lblOperation = Ext.getCmp('lblOperation');
        if (lblOperation)
            lblOperation.setText(label_NON_SAVED);
    }
}

var isMaintenance = false;