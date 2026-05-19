package com.cascade.utils;

import com.cascade.delta.CausalDelta;
import java.util.Map;

/**
 * JsonUtils — Lightweight JSON serialization for CausalDelta (no external deps).
 */
public final class JsonUtils {
    private JsonUtils() {}

    public static String toJson(CausalDelta delta) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"eventId\":\"").append(delta.getEventId()).append("\",");
        sb.append("\"productId\":\"").append(delta.getProductId()).append("\",");
        sb.append("\"warehouseId\":\"").append(delta.getWarehouseId()).append("\",");
        sb.append("\"delta\":").append(delta.getDelta()).append(",");
        sb.append("\"timestamp\":\"").append(delta.getTimestamp()).append("\"");
        if (delta.hasCausalContext()) {
            sb.append(",\"causalContext\":{");
            boolean first = true;
            for (Map.Entry<String, Long> e : delta.getCausalContext().entrySet()) {
                if (!first) sb.append(","); first = false;
                sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            }
            sb.append("}");
        }
        if (delta.getSource() != null) sb.append(",\"source\":\"").append(delta.getSource()).append("\"");
        sb.append("}");
        return sb.toString();
    }
}
