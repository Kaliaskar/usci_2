package kz.bsbnb.usci.util.service;

import kz.bsbnb.usci.model.util.Text;

import java.util.List;

public interface TextService {
    Text getText(Text text);

    Text getText(Long id);

    void updateText(Text global);

    List<Text> getTextListByType(List<String> types);

}
