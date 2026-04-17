package com.ecorating.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.function.Consumer;

/**
 * Клиент портала открытых данных Москвы (apidata.mos.ru, OData-подобный API).
 */
public interface MoscowOpenDataClient {

    /**
     * Загружает все строки набора с пагинацией ($top / $skip).
     */
    List<JsonNode> fetchAllRows(int datasetId);

    /**
     * Загружает строки с опциональным OData {@code $filter}, не более {@code maxRows} записей.
     */
    List<JsonNode> fetchRows(int datasetId, String oDataFilter, int maxRows);

    /**
     * То же, но после каждой загруженной страницы вызывает {@code afterEachPageAccumulated}
     * с текущим накопленным списком (для раннего снимка на диск).
     */
    List<JsonNode> fetchRows(
            int datasetId,
            String oDataFilter,
            int maxRows,
            Consumer<List<JsonNode>> afterEachPageAccumulated
    );
}
