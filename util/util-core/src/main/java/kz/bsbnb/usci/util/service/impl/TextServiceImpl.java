package kz.bsbnb.usci.util.service.impl;

import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.util.dao.TextDao;
import kz.bsbnb.usci.util.repository.TextRepository;
import kz.bsbnb.usci.util.service.TextService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Jandos Iskakov
 */

@Service
public class TextServiceImpl implements TextService {
    private final TextRepository textRepository;
    private final TextDao textDao;

    public TextServiceImpl(TextRepository textRepository,
                           TextDao textDao) {
        this.textRepository = textRepository;
        this.textDao = textDao;
    }

    @Override
    public Text getText(Text text) {
        return textRepository.getConfig(text.getId());
    }

    @Override
    public Text getText(Long id) {
        return textRepository.getConfig(id);
    }

    @Override
    public void updateText(Text text) {
        //TODO:
    }

    @Override
    public List<Text> getTextListByType(List<String> types) {
        return textDao.getConstantsByType(types);
    }

}
