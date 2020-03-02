/**
 * @author Jandos Iskakov
 */

function createDateTimePicker(fieldLabel, labelWidth, fieldId, width, value, allowBlank, readOnly) {
    var dateTime = value? moment(value, 'DD.MM.YYYY HH:mm:ss'): null;

    var dateField = Ext.create("Ext.form.field.Date", {
        id: fieldId,
        width: '60%',
        format: 'd.m.Y',
        value: dateTime? dateTime.toDate(): null,
        readOnly: readOnly,
        allowBlank: allowBlank,
        blankText: label_REQUIRED_FIELD,
        getDateTimeValue: function() {
            var rawDateTime = this.getValue();

            if (!rawDateTime)
                return null;

            var hours = Number(hourField.value);
            var minutes = Number(minField.value);
            var seconds = Number(secField.value);


            rawDateTime.setHours(hours);
            rawDateTime.setMinutes(minutes);
            rawDateTime.setSeconds(seconds);

            return rawDateTime;
        },
        getValueAsString: function() {
            var rawDateTime = this.getDateTimeValue();
            var asString = moment(rawDateTime).format('DD.MM.YYYY HH:mm:ss');
            return asString;
        }
    });

    var hourField = Ext.create("Ext.form.field.Number", {
        id: fieldId + '_hour',
        fieldLabel: '',
        labelWidth: '0%',
        width: 45,
        value: dateTime? dateTime.hours(): 0,
        allowDecimals: false,
        allowNegative: false,
        minValue: 0,
        maxValue: 23
    });

    var minField = Ext.create("Ext.form.field.Number", {
        id: fieldId + '_min',
        fieldLabel: '',
        labelWidth: '0%',
        width: 45,
        value: dateTime? dateTime.minutes(): 0,
        allowDecimals: false,
        allowNegative: false,
        minValue: 0,
        maxValue: 59
    });

    var secField = Ext.create("Ext.form.field.Number", {
        id: fieldId + '_sec',
        fieldLabel: '',
        labelWidth: '0%',
        width: 45,
        value: dateTime? dateTime.seconds(): 0,
        allowDecimals: false,
        allowNegative: false,
        minValue: 0,
        maxValue: 59
    });

    var panelDict = Ext.create('Ext.form.FieldContainer', {
        fieldLabel: fieldLabel,
        labelWidth: labelWidth,
        width: width,
        preventHeader: true,
        border: 0,
        layout: {
            type: 'hbox',
            align: 'stretch'
        },
        items: [dateField, hourField, minField, secField]
    });

    return panelDict;
}