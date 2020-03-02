package kz.bsbnb.usci.eav.meta.controller;

import kz.bsbnb.usci.eav.model.meta.json.MetaAttributeJson;
import kz.bsbnb.usci.eav.model.meta.json.MetaClassJson;
import kz.bsbnb.usci.eav.service.MetaClassService;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import kz.bsbnb.usci.util.json.ext.ExtJsTree;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/meta")
public class MetaClassController {
    private final MetaClassService metaClassService;

    public MetaClassController(MetaClassService metaClassService) {
        this.metaClassService = metaClassService;
    }

    @GetMapping(value = "getMetaClasses")
    public List<MetaClassJson> getMetaClasses() {
        return metaClassService.getMetaClassJsonList();
    }

    @GetMapping(value = "getMetaClassJsonListByUserId")
    public List<MetaClassJson> getMetaClassJsonListByUserId(@RequestParam(name = "userId") Long userId) {
        return metaClassService.getMetaClassJsonListByUserId(userId);
    }

    @GetMapping(value = "getDictionaries")
    public List<MetaClassJson> getDictionaries() {
        return metaClassService.getDictionaryJsonList();
    }

    @GetMapping(value = "getMetaClassAttributesTree")
    public List<ExtJsTree> getMetaClassAttributes(@RequestParam(name = "node") String node) {
        return metaClassService.getMetaClassAttributes(node);
    }

    @GetMapping(value = "getMetaClassData")
    public MetaClassJson getMetaClass(@RequestParam(name = "metaClassId") Long metaClassId) {
        return metaClassService.getMetaClassJson(metaClassId);
    }

    //TODO: переименовать в getMetaAttributes
    @GetMapping(value = "getMetaClassAttributesList")
    public List<MetaAttributeJson> getMetaAttributes(@RequestParam Long metaClassId) {
        return metaClassService.getMetaClassAttributes(metaClassId);
    }

    @PostMapping(value = "saveMetaClass")
    public void saveMetaClass(@RequestBody MetaClassJson metaClassJson) {
        metaClassService.saveMetaClass(metaClassJson);

    }

    @PostMapping(value = "saveMetaAttribute")
    public void saveMetaAttribute(@RequestBody MetaAttributeJson metaAttributeJson) {
        metaClassService.saveMetaAttribute(metaAttributeJson);

    }

    @PostMapping(value = "syncWithDb")
    public void syncWithDb() {
        metaClassService.syncWithDb();
    }

    @GetMapping(value = "getMetaAttribute")
    public ExtJsJson getMetaAttribute(@RequestParam(name = "metaClassId") Long metaClassId, @RequestParam(name = "attributeId") Long attributeId) {
        return new ExtJsJson(metaClassService.getMetaAttribute(metaClassId, attributeId));
    }

    @PostMapping(value = "delMetaAttribute")
    public void deleteAttribute(@RequestParam(name = "attrPathPart") String attrPathPart,
                                @RequestParam(name = "attrPathCode") String attrPathCode) {
        metaClassService.delMetaAttribute(attrPathPart, attrPathCode);

    }

}
