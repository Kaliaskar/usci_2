package kz.bsbnb.usci.report.controller;

import kz.bsbnb.usci.report.crosscheck.CrossCheck;
import kz.bsbnb.usci.report.crosscheck.CrossCheckMessageDisplayWrapper;
import kz.bsbnb.usci.report.service.CrossCheckService;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import kz.bsbnb.usci.util.json.ext.ExtJsList;
import kz.bsbnb.usci.wsclient.client.NSIClient;
import kz.bsbnb.usci.wsclient.service.NSIService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping(value = "/crosscheck")
public class CrossCheckController {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate npJdbcTemplate;
    private final CrossCheckService crossCheckService;
    private final NSIClient nsiClient;

    public CrossCheckController(JdbcTemplate jdbcTemplate,
                                NamedParameterJdbcTemplate npJdbcTemplate,
                                CrossCheckService crossCheckService,
                                NSIClient nsiClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.npJdbcTemplate = npJdbcTemplate;
        this.crossCheckService = crossCheckService;
        this.nsiClient = nsiClient;
    }

    @PostMapping(value = "executeCrossCheckAll")
    public void crossCheck(@RequestParam Long userId,
                           @RequestParam  List<Long> creditorIds,
                           @RequestParam  Long productId,
                           @RequestParam  String executeClause,
                           @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate reportDate)  {
        if (productId.equals(12L)) {
            nsiClient.saveCurrencyRates(SqlJdbcConverter.convertToString(reportDate));
        }
        if (creditorIds == null) {
            crossCheckService.crossCheckAll(userId, reportDate, productId, executeClause);
        } else {
            for(Long creditorId : creditorIds) {
                crossCheckService.crossCheck(userId, creditorId, reportDate, productId, executeClause);
            }
        }
    }

    @GetMapping(value = "getCrossCheck")
    public ExtJsList getCrossChecks(@RequestParam List<Long> creditorIds,
                                    @RequestParam @DateTimeFormat(pattern = "dd.MM.yyyy") LocalDate reportDate,
                                    @RequestParam Long productId)
    {
        return  new ExtJsList(crossCheckService.getCrossChecks(creditorIds, reportDate, productId));
    }

    @GetMapping(value = "getCrossCheckMessages")
    public List<CrossCheckMessageDisplayWrapper> getCrossCheckMessages(@RequestParam  Long crossCheckId) {
        return  crossCheckService.getCrossCheckMessages(crossCheckId);
    }

    @PostMapping(value = "exportToExcel")
    public ResponseEntity<InputStreamResource>  exportToExcel(@RequestParam  Long crossCheckId) {
        CrossCheck crossCheck = crossCheckService.getCrossCheck(crossCheckId);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(crossCheckService.exportToExcel(crossCheck)));
        String fileName = "Межформенный контроль";
        HttpHeaders header = createHeader();
        header.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());

        return ResponseEntity
                .ok()
                .headers(header)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(resource);
    }

    private static HttpHeaders createHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Access-Control-Allow-Headers", "Range");
        headers.add("Access-Control-Expose-Headers", "Accept-Ranges, Content-Encoding, Content-Length, Content-Range, Content-Disposition");
        return headers;
    }

}
