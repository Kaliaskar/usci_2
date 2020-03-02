package kz.bsbnb.usci.util.repository.impl;

import kz.bsbnb.usci.model.util.Text;
import kz.bsbnb.usci.util.dao.TextDao;
import kz.bsbnb.usci.util.repository.TextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Repository
public class TextRepositoryImpl implements TextRepository, InitializingBean {
    private final static Logger logger = LoggerFactory.getLogger(TextRepositoryImpl.class);

    private HashMap<String, Text> cache = new HashMap<>();
    private final TextDao textDao;

    public TextRepositoryImpl(TextDao textDao) {
        this.textDao = textDao;
    }

    @Override
    public void afterPropertiesSet() {
        List<Text> textList = textDao.getAll();

        for (Text text : textList)
            cache.put(text.getType() + ":" + text.getCode(), text);
    }

    @Override
    public Text getConfig(String type, String code) {
        String key = type + ":" + code;

        Text text = cache.get(key);

        if (text == null) {
            text = textDao.get(type, code);
            cache.put(key, text);
        }

        return text;
    }

    @Override
    public Text getConfig(Long id) {
        return textDao.get(id);
    }

}
