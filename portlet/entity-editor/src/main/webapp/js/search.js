/**
 * @author Jandos Iskakov
 */

function fillSearchTree(metaClassId) {
    var tree = Ext.getCmp('searchTreeView');

    var rootNode = tree.getRootNode();
    rootNode.removeAll();

    var loadMask = new Ext.LoadMask(Ext.getCmp('mainPanel'), {msg:label_LOADING});
    loadMask.show();

    Ext.Ajax.request({
        url: dataUrl + '/core/meta/getMetaClassAttributesList',
        params : {
            metaClassId: metaClassId
        },
        method: 'GET',
        failure: function(response) {
            var error = JSON.parse(response.responseText);

            Ext.Msg.show({
                title: label_ERROR,
                msg: error.errorMessage,
                width : 300,
                buttons: Ext.MessageBox.YES
            });
        },
        success: function(response) {
            // получаем мета атрибуты у мета класса
            // затем атрибуты boolean, комплексные сущности создаем чтобы юзер
            // тыкал и добавлял параметры поиска
            var attributes = JSON.parse(response.responseText);

            for (var i = 0; i < attributes.length; i++) {
                var attribute = attributes[i];

                if (!attribute.key)
                    continue;

                if (attribute.simple && attribute.metaType === 'BOOLEAN') {
                    attribute.iconCls = 'file';
                    attribute.leaf = 'true';
                    attribute.value = false;
                    rootNode.appendChild(attribute);
                }
                else if (!attribute.simple && !attribute.dictionary) {
                    attribute.expanded = true;
                    rootNode.appendChild(attribute);
                }
            }

            loadMask.hide();

            Ext.getCmp('searchTreeView').getView().refresh();
        }
    });
}

// добавление атрибута или элемента сета
function showAddSearchParamWindow(node) {
    var metaClassId = null;
    var windowTitle = null;

    if (node.data.depth == 0) {
        metaClassId = Ext.getCmp('edMetaClass').value;
        windowTitle = Ext.getCmp('edMetaClass').getRawValue();
    } else {
        metaClassId = node.data.refClassId;
        windowTitle = node.data.title;
    }

    if (node.data.array) {
        if (node.data.simple) {
            //нет данных для тестирования
            throw label_SIMPLE_MASS;
        } else {
            showArrayNewElWindow(metaClassId, node, windowTitle, true, function() {
                Ext.getCmp('searchTreeView').getView().refresh();
            });
        }
    } else {
        showAddAttrWindow(node, metaClassId, windowTitle, true, function(form) {
            Ext.getCmp('searchTreeView').getView().refresh();
        });
    }
}

function getSearchDataFromTree(currentNode) {
    var entityData = {name: currentNode.data.name, value: currentNode.data.value, children: []};

    var children = currentNode.childNodes;

    for (var i = 0; i < children.length; i++) {
        if (children[i].data.simple) {
            // обрабатываем простые сеты
            if (currentNode.data.array) {
            } else {
                // по справочникам включаем только ключевые атрибуты
                // все остальные атрибуты пропускаем
                if (currentNode.data.dictionary && !children[i].data.key)
                    continue;

                var attrData = {name: children[i].data.name, value: children[i].data.value, children: []};
                entityData.children.push(attrData);
            }
        } else {
            // комплексная сущность
            var childEntityData = getSearchDataFromTree(children[i]);
            entityData.children.push(childEntityData);
        }
    }

    return entityData;
}