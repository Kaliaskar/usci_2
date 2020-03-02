/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

function createExtJsComps() {
    Ext.define('UsciCheckboxField', {
        extend: 'Ext.form.field.Checkbox',
        initComponent: function() {
            this.fieldSubTpl[9] = '<input type="checkbox" id="{id}" {checked} {inputAttrTpl}';
            this.callParent();
        },
        getSubTplData: function() {
            var me = this;
            return Ext.apply(me.callParent(), {
                checked: (me.checked ? 'checked' : '')
            });
        }
    });

    Ext.override(Ext.form.NumberField, {
        forcePrecision: false,

        valueToRaw: function(value) {
            var me = this,decimalSeparator = me.decimalSeparator;
            value = me.parseValue(value);
            value = me.fixPrecision(value);
            value = Ext.isNumber(value) ? value : parseFloat(String(value).replace(decimalSeparator, '.'));
            if (isNaN(value)) {
                value = '';
            } else {
                value = me.forcePrecision ? value.toFixed(me.decimalPrecision) : parseFloat(value);
                value = String(value).replace(".", decimalSeparator);
            }
            return value;
        }
    });
}