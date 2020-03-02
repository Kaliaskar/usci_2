/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

function createXml(currentNode, rootFlag, offset, arrayEl, first, operation) {
    var ret = {
        xml: "",
        childCnt : 0
    };

    if (arrayEl) {
        // добавляем открывающий тег по элементу сета
        ret.xml += offset + "<item>\n";
    } else {
        // добавляем открывающий тег по атрибуту
        // если это корень xml то добавляем к нему еще вид операций
        if (first) {
            //TODO: анализировать нужно ли CHECKED_REMOVE?
            if (currentNode.data.markedAsDeleted)
                operation = 'DELETE';

            if (currentNode.data.markedAsClosed)
                operation = 'CLOSE';

            ret.xml += offset + "<" + currentNode.data.name +
                (operation ? " operation=\"" + operation + "\"" : "") + ">\n";
        } else {
            ret.xml += offset + "<" + currentNode.data.name + ">\n";
        }
    }

    var children = currentNode.childNodes;

    for (var i = 0; i < children.length; i++) {
        // затирание атрибута признаком nil=true
        if (children[i].data.markedAsDeleted) {
            if (!children[i].data.name.match(/\d+/))
                ret.xml += offset + "  " + "<" + children[i].data.name + " xsi:nil=\"true\"/>\n";

            continue;
        }

        if (children[i].data.simple && !children[i].data.array) {
            // обрабатываем простые сеты; в xml они входят как <item>value</item>
            if (currentNode.data.array) {
                ret.xml += offset + "  " + "<item>";
                ret.xml += children[i].data.value;
                ret.xml += "</item>\n";
            } else {
                // изменился ключевой атрибут
                // добавляем в xml старое и новое значение атрибута
                if (children[i].data.key && children[i].data.oldValue) {
                    ret.xml += offset + " " + "<" + children[i].data.name + " data=\"" + children[i].data.value + "\" operation=\"NEW\">";
                    ret.xml += children[i].data.oldValue;
                    ret.xml += "</" + children[i].data.name + ">\n";
                } else {
                    // по справочникам включаем только ключевые атрибуты
                    // все остальные атрибуты пропускаем
                    if (currentNode.data.dictionary && !children[i].data.key && !rootFlag)
                        continue;

                    if (children[i].data.value == null) {
                        ret.xml += offset + "  " + "<" + children[i].data.name + " xsi:nil=\"true\"/>\n";
                    } else {
                        ret.xml += offset + "  " + "<" + children[i].data.name + ">";
                        ret.xml += children[i].data.value;
                        ret.xml += "</" + children[i].data.name + ">\n";
                    }
                }
            }
            ret.childCnt ++;
        } else {
            // комплексная сущность
            childRet = createXml(children[i], false, offset + "    ", currentNode.data.array, false, null);
            if (childRet.childCnt > 0) {
                ret.xml += childRet.xml;
                ret.childCnt++;
            }
        }
    }

    // добавляем в xml закрывающие теги
    // по элементам сета </item>, по остальным </name>
    if (arrayEl) {
        ret.xml += offset + "</item>\n";
    } else {
        ret.xml += offset + "</" + currentNode.data.name + ">\n";
    }

    /*if (ret.childCnt == 0) {
        ret.xml = offset + "<" + currentNode.data.name + "/>\n";
        ret.childCnt = 1;
    }*/

    return ret;
}

// процедура проверяет ключевые комплексные сущности (сеты тоже)
// чтобы у них были ключи; например primary_contract, documents
// если находится пустой ключевой атрибут выводит сообщение ошибки
function hasEmptyKeyAttr(node) {
    for (var i = 0; i < node.childNodes.length; i++) {
        var currentNode = node.childNodes[i];

        if (currentNode.data.simple)
            continue;

        if (currentNode.data.key && currentNode.childNodes.length == 0) {
            Ext.MessageBox.alert(label_ERROR, label_NOT_ADDED_KEY_ATRIBUT + currentNode.data.title);
            return true;
        } else if (currentNode.childNodes.length == 0) {
        } else if (hasEmptyKeyAttr(currentNode)) {
            return true;
        }
    }

    return false;
}

function sendXml(operation, date, metaClassId) {
    var tree = Ext.getCmp('entityTreeView');
    rootNode = tree.getRootNode();

    var xmlStr = "";
    for (var i = 0; i < rootNode.childNodes.length; i++) {
        if (hasEmptyKeyAttr(rootNode.childNodes[i])) {
            return;
        }
        xmlStr += createXml(rootNode.childNodes[i], true, "", false, true, operation).xml;
    }

    Ext.Ajax.request({
        url: dataUrl + '/receiver/batchEntry/saveXml',
        method: 'POST',
        params: {
            xmlData: es_escape(xmlStr),
            date: moment(date).local().format('YYYY-MM-DD'),
            userId: userId,
            // корень дерева, корневой узел
            entityId: rootNode.childNodes[0].data.value,
            metaClassId: metaClassId,
            isMaintenance: isMaintenance
        },
        success: function (response) {
            Ext.MessageBox.alert("", label_SUCCEFUL_SAVED);
        }
    });
}

function showXml() {
    var tree = Ext.getCmp('entityTreeView');
    rootNode = tree.getRootNode();

    var xmlStr = "";

    for (var i = 0; i < rootNode.childNodes.length; i++) {
        xmlStr += createXml(rootNode.childNodes[i], true, "", false, true, null).xml;
    }

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
            value: xmlStr,
            height: 415
        }],
        buttons: [
            Ext.create('Ext.button.Button', {
                text: label_CANCEL,
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