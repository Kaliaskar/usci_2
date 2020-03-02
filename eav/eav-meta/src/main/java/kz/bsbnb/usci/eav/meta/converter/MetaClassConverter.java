package kz.bsbnb.usci.eav.meta.converter;

import kz.bsbnb.usci.eav.model.meta.MetaClass;
import kz.bsbnb.usci.eav.model.meta.json.MetaClassJson;
import kz.bsbnb.usci.model.util.DtoConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Jandos Iskakov
 */

@Component
public class MetaClassConverter implements DtoConverter<MetaClass, MetaClassJson> {

    @Override
    public MetaClassJson convertToDto(MetaClass metaClass) {
        MetaClassJson metaClassJson = new MetaClassJson();

        metaClassJson.setId(metaClass.getId());
        metaClassJson.setPeriodTypeId(metaClass.getPeriodType().getId());
        metaClassJson.setDeleted(metaClass.isDeleted());
        metaClassJson.setDictionary(metaClass.isDictionary());
        metaClassJson.setHashSize(metaClass.getHashSize());
        metaClassJson.setOperational(metaClass.isOperational());
        metaClassJson.setName(metaClass.getClassName());
        metaClassJson.setSync(metaClass.isSync());
        metaClassJson.setTitle(StringUtils.isEmpty(metaClass.getClassTitle())? metaClass.getClassName(): metaClass.getClassTitle());

        return metaClassJson;
    }

    @Override
    public void updateFromDto(MetaClass metaClass, MetaClassJson dto) {

    }
    
}
