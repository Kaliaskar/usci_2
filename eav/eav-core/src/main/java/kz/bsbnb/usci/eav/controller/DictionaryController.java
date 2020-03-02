package kz.bsbnb.usci.eav.controller;

import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.service.EntityExtJsService;
import kz.bsbnb.usci.eav.service.MetaClassService;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/dict")
public class DictionaryController {
    private final MetaClassService metaClassService;
    private final EntityExtJsService dictionaryService;

    public DictionaryController(MetaClassService metaClassService,
                                EntityExtJsService dictionaryService) {
        this.metaClassService = metaClassService;
        this.dictionaryService = dictionaryService;
    }

    @GetMapping(value = "getDictEntities")
    public ExtJsJson getDictEntities(@RequestParam(name = "metaId") Long metaClassId,
                                     @RequestParam(name = "userId") Long userId,
                                     @RequestParam(name = "isNb") boolean isNb,
                                     @RequestParam(name = "reportDate", required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate) {
        List<Map<String, Object>> list = dictionaryService.loadEntityEntries(metaClassService.getMetaClass(metaClassId), reportDate, userId, isNb);
        return new ExtJsJson(list);
    }

    @GetMapping(value = "exportDictionaryToMsExcel")
    public ResponseEntity<InputStreamResource> exportDictionaryToMsExcel(@RequestParam(name = "metaClassId") Long metaClassId,
                                                                         @RequestParam(name = "userId") Long userId,
                                                                         @RequestParam(name = "isNb") boolean isNb,
                                                                         @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MetaClass metaClass = metaClassService.getMetaClass(metaClassId);

        String fileName = metaClass.getClassName() + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".xls";

        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(dictionaryService.exportDictionaryToMsExcel(metaClass, date, userId, isNb)));

        HttpHeaders header = createHeader();
        header.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());

        return ResponseEntity
                .ok()
                .headers(header)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(resource);
    }

    private static HttpHeaders createHeader() {
        // Чтоб не кэшировалось
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        //без этих значений выходило: Refused to get unsafe header "Accept-Ranges"
        headers.add("Access-Control-Allow-Headers", "Range");
        headers.add("Access-Control-Expose-Headers", "Accept-Ranges, Content-Encoding, Content-Length, Content-Range, Content-Disposition");
        return headers;
    }

}
