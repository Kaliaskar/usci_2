package kz.bsbnb.usci.core.controller;

import kz.bsbnb.usci.core.service.PositionService;
import kz.bsbnb.usci.model.adm.Position;
import kz.bsbnb.usci.util.json.ext.ExtJsJson;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/position")
public class PositionRestController {
    private final PositionService positionService;

    public PositionRestController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping(value = "getPositionList")
    public List<Position> getPositionList() {
        return positionService.getPositionList();
    }

    @GetMapping(value = "getUserPositionListByProduct")
    public List<Position> getUserPositionListByProduct(@RequestParam(name = "userId") Long userId,
                                                       @RequestParam(name = "productId") Long productId) {
        return positionService.getUserPosListByProductId(userId, productId);
    }

    @GetMapping(value = "getPositionById")
    public Position getPositionById(@RequestParam(name = "id") Long id) {
        return positionService.getPositionById(id);
    }

    @PutMapping(value = "addUserPosition")
    public ExtJsJson addUserPosition(@RequestParam(name = "userId") Long userId,
                                       @RequestParam(name = "productId") Long productId,
                                       @RequestParam(name = "positionIds") List<Long> positionIds) {
        positionService.addUserPosition(userId, productId, positionIds);
        return new ExtJsJson(true);
    }

    @PostMapping(value = "delUserPosition")
    public ExtJsJson delUserPosition(@RequestParam(name = "userId") Long userId,
                                       @RequestParam(name = "productId") Long productId,
                                       @RequestParam(name = "positionIds") List<Long> positionIds) {
        positionService.delUserPosition(userId, productId, positionIds);
        return new ExtJsJson(true);
    }

}
