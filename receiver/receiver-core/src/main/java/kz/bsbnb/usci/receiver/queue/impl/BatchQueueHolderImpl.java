package kz.bsbnb.usci.receiver.queue.impl;

import kz.bsbnb.usci.core.client.RespondentClient;
import kz.bsbnb.usci.model.exception.UsciException;
import kz.bsbnb.usci.model.respondent.Respondent;
import kz.bsbnb.usci.model.respondent.RespondentJson;
import kz.bsbnb.usci.receiver.batch.service.BatchService;
import kz.bsbnb.usci.receiver.model.Batch;
import kz.bsbnb.usci.receiver.model.QueueOrderType;
import kz.bsbnb.usci.receiver.model.json.BatchJson;
import kz.bsbnb.usci.receiver.queue.BatchQueueHolder;
import kz.bsbnb.usci.receiver.queue.BatchQueueOrder;
import kz.bsbnb.usci.util.client.ConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baurzhan Makhanbetov
 */

@Component
@Scope(value = "singleton")
public class BatchQueueHolderImpl implements BatchQueueHolder {
    private static final Logger logger = LoggerFactory.getLogger(BatchQueueHolderImpl.class);

    @Value("${batch.maxBatchNumber}")
    private int concurrencyLimit;

    private final Set<Long> activeRespondents = new HashSet<>();
    private Set<Long> respondentsWithPriority = new HashSet<>();

    private BatchQueueOrder order;
    private final Map<Long, Batch> queue = new HashMap<>();
    private final Map<Long, Long> batchRespondentMap = new HashMap<>();
    private final ConfigClient configClient;
    private final RespondentClient respondentClient;
    private final BatchService batchService;

    public BatchQueueHolderImpl(ConfigClient configClient,
                                RespondentClient respondentClient,
                                BatchService batchService) {
        this.configClient = configClient;
        this.respondentClient = respondentClient;
        this.batchService = batchService;
    }

    @PostConstruct
    public void init() {
        reloadConfig();
    }

    @Override
    public Optional<Batch> getNextBatch() {
        if (activeRespondents.size() < concurrencyLimit) {
            synchronized (this) {
                if (queue.size() > 0) {
                    Batch batchToAdd = getNextBatch(queue, respondentsWithPriority, order);
                    if (batchToAdd != null) {
                        activeRespondents.add(batchToAdd.getRespondentId());
                        queue.remove(batchToAdd.getId());
                        return Optional.of(batchToAdd);
                    }
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized void batchFinished(Long batchId) {
        if (!batchRespondentMap.containsKey(batchId))
            return;

        activeRespondents.remove(batchRespondentMap.get(batchId));
        batchRespondentMap.remove(batchId);
    }

    @Override
    public synchronized void addBatch(Batch batch) {
        queue.put(batch.getId(), batch);
        batchRespondentMap.put(batch.getId(), batch.getRespondentId());
    }

    private Batch getNextBatch(Map<Long, Batch> queue, Set<Long> respondentsWithPriority, BatchQueueOrder order) {
        Map<Long, Batch> batchPerRespondent = new HashMap<>();

        // выбираем от каждого респондента по одному батчу (если у кредитора несколько батчей то самый раний)
        for (Batch batch : queue.values()) {
            // уникальность респондента должна быть сохранена
            if (activeRespondents.contains(batch.getRespondentId()))
               continue;

            if (!batchPerRespondent.containsKey(batch.getProduct().getId())) {
                batchPerRespondent.put(batch.getProduct().getId(), batch);
            } else {
                Batch thatBatch = batchPerRespondent.get(batch.getProduct().getId());
                if (order.compare(batch, thatBatch) < 0)
                    batchPerRespondent.put(batch.getProduct().getId(), batch);
            }
        }

        // из полученных выбираем файлы приоритетных респондентов
        List<Batch> priorityRespondentFiles = new ArrayList<>(batchPerRespondent.size());

        for (Batch batch : batchPerRespondent.values()) {
            if (respondentsWithPriority.contains(batch.getProduct().getId()))
                priorityRespondentFiles.add(batch);
        }

        // если таковых нет, то пропускаем всех остальных
        if (priorityRespondentFiles.isEmpty())
            priorityRespondentFiles = new ArrayList<>(batchPerRespondent.values());

        return order.getNextBatch(priorityRespondentFiles);
    }

    private BatchQueueOrder getImplementationByEnum(QueueOrderType queueOrderType) {
        switch (queueOrderType) {
            case CREDITOR_CYCLE:
                return new RespondentCycleOrder();
            case MINIMUM_WEIGHT:
                return new MinimumWeightOrder();
            default:
                return new ChronologicalOrder();
        }
    }

    @Override
    public void reloadConfig() {
        QueueOrderType queueOrderType = QueueOrderType.valueOf(configClient.getQueueAlgorithm());

        order = getImplementationByEnum(queueOrderType);
        respondentsWithPriority = configClient.getPriorityRespondentIds();

        logger.info("Загружены настройки очереди: алгоритм = {}, приоритетные кредиторы = {}", queueOrderType, respondentsWithPriority);
    }

    /**
     * Удаляет батч из очереди и отменяет его дальнейшую загрузку
     */
    @Override
    @Transactional
    public synchronized void removeBatch(long batchId) {
        if (!queue.containsKey(batchId))
            throw new UsciException("Батч не содержится в очереди");

        long respondentId = batchRespondentMap.get(batchId);
        batchRespondentMap.remove(batchId);
        activeRespondents.remove(respondentId);
        queue.remove(batchId);

        batchService.cancelBatch(batchId);
    }

    @Override
    public List<BatchJson> getOrderedBatches(Set<Long> respondentsWithPriority, String queueAlgo) {
        QueueOrderType queueOrderType = QueueOrderType.valueOf(queueAlgo);

        List<Respondent> respondents = respondentClient.getRespondentList();
        Map<Long, Respondent> respondentMap = respondents.stream()
                .collect(Collectors.toMap(Respondent::getId, o-> o));

        BatchQueueOrder batchQueueOrder = getImplementationByEnum(queueOrderType);

        List<BatchJson> batchJsonList = new ArrayList<>();

        Map<Long, Batch> tempBatches = new HashMap<>(queue);
        for (int i = 0; i < tempBatches.size(); i++) {
            Batch batch = getNextBatch(tempBatches, respondentsWithPriority, batchQueueOrder);
            tempBatches.remove(batch.getId());

            Respondent respondent = respondentMap.get(batch.getRespondentId());

            BatchJson batchJson = new BatchJson();
            batchJson.setId(batch.getId());
            batchJson.setRespondentId(batch.getRespondentId());
            batchJson.setRespondent(new RespondentJson(respondent.getId(), respondent.getName()));
            batchJson.setFileName(batch.getFilePath());
            batchJson.setReceiverDate(batch.getReceiptDate());
            batchJson.setReportDate(batch.getReportDate());
            batchJson.setActualEntityCount(0L);
            batchJson.setTotalEntityCount(batch.getTotalEntityCount());

            batchJsonList.add(batchJson);
        }

        return batchJsonList;
    }

}
