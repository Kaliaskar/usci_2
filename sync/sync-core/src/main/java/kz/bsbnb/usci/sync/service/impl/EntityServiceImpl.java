package kz.bsbnb.usci.sync.service.impl;

import kz.bsbnb.usci.eav.model.base.BaseEntity;
import kz.bsbnb.usci.sync.service.BaseEntityHubService;
import kz.bsbnb.usci.eav.service.EntityRmiService;
import kz.bsbnb.usci.sync.service.EntityService;
import kz.bsbnb.usci.sync.service.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Kanat Tulbassiev
 */

@Service
public class EntityServiceImpl implements EntityService {
    private static final Logger logger = LoggerFactory.getLogger(EntityServiceImpl.class);

    private ServiceRepository serviceRepository;

    private final BaseEntityHubService baseEntityHubService;


    public EntityServiceImpl(ServiceRepository serviceRepository,
                             BaseEntityHubService baseEntityHubService) {
        this.serviceRepository = serviceRepository;
        this.baseEntityHubService = baseEntityHubService;
    }

    @Override
    public boolean process(BaseEntity entity) {
        logger.info("Отправка сущности на обработку в CORE {}", entity);

        EntityRmiService entityRmiService = (EntityRmiService) serviceRepository.getEntityRmiServices().get(0).getObject();

        return entityRmiService.process(entity);
    }

    @Override
    public Long find(BaseEntity baseEntity) {
         BaseEntity baseEntityCheck = baseEntityHubService.prepareBaseEntity(baseEntity, baseEntity.getRespondentId());
         if (baseEntityCheck != null)
             return  baseEntityCheck.getId();

         return null;
    }

}
