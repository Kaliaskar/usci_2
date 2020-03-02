package kz.bsbnb.usci.receiver.writer;

import kz.bsbnb.usci.sync.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Kanat Tulbassiev
 */

@Component
@StepScope
public class RmiEventEntityWriter<BaseEntity> implements ItemWriter<BaseEntity> {
    private static final Logger logger = LoggerFactory.getLogger(RmiEventEntityWriter.class);

    private final SyncService syncService;

    public RmiEventEntityWriter(SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void write(List items) {
        logger.info("Отправка сущностей на обработку в SYNC {}", items);

        syncService.process(items);
    }

}
