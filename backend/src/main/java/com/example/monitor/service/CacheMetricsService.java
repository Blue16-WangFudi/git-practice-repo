package com.example.monitor.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Component
public class CacheMetricsService {

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder errors = new LongAdder();

    public void recordHit() {
        hits.increment();
    }

    public void recordMiss() {
        misses.increment();
    }

    public void recordError() {
        errors.increment();
    }

    public Map<String, Object> snapshot() {
        long hitCount = hits.sum();
        long missCount = misses.sum();
        long errorCount = errors.sum();
        long totalReads = hitCount + missCount;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("hits", hitCount);
        result.put("misses", missCount);
        result.put("errors", errorCount);
        result.put("totalReads", totalReads);
        result.put("hitRate", totalReads == 0 ? 0.0 : Math.round(hitCount * 10000.0 / totalReads) / 100.0);
        return result;
    }
}
