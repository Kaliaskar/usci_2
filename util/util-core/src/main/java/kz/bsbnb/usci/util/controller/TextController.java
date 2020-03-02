package kz.bsbnb.usci.util.controller;

import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.util.service.TextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@RestController
@RequestMapping(value = "/text")
public class TextController {
    private final TextService textService;

    public TextController(TextService textService) {
        this.textService = textService;
    }

    @GetMapping(value = "/getTextListByType")
    public List<Text> getTextListByType(@RequestParam List<String> types) {
        return textService.getTextListByType(types);
    }

    @GetMapping(value = "/getTextById")
    public Text getTextById(@RequestParam(name = "id") Long id) {
        return textService.getText(id);
    }

}
