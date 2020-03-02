package kz.bsbnb.usci.util.client;

import kz.bsbnb.usci.model.util.Text;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "utils")
public interface TextClient {

    @GetMapping(value = "/text/getTextListByType")
    List<Text> getTextListByType(@RequestParam(name = "types") List<String> types);

    @GetMapping(value = "/text/getTextById")
    Text getTextById(@RequestParam(name = "id") Long id);

}
