/**
 * @author Maksat Nussipzhan
 * @author Baurzhan Makhambetov
 * @author Jandos Iskakov
 */

function es_escape(xmlStr) {
    return xmlStr.replace(new RegExp('&','g'), '&amp;');
}