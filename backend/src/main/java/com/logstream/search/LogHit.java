package com.logstream.search;

import com.logstream.index.Fields;
import org.apache.lucene.document.Document;

/** One row in the results table. */
public class LogHit {
    private long timestamp;
    private String level;
    private String service;
    private String host;
    private String message;
    private long responseTime;
    private int statusCode;
    private String traceId;

    public static LogHit from(Document d) {
        LogHit h = new LogHit();
        h.timestamp = val(d, Fields.TIMESTAMP);
        h.level = str(d, Fields.LEVEL);
        h.service = str(d, Fields.SERVICE);
        h.host = str(d, Fields.HOST);
        h.message = str(d, Fields.MESSAGE);
        h.responseTime = val(d, Fields.RESPONSE_TIME);
        h.statusCode = (int) val(d, Fields.STATUS_CODE);
        h.traceId = str(d, Fields.TRACE_ID);
        return h;
    }

    private static String str(Document d, String f) {
        String v = d.get(f);
        return v == null ? "" : v;
    }

    private static long val(Document d, String f) {
        var v = d.getField(f);
        return v == null || v.numericValue() == null ? 0L : v.numericValue().longValue();
    }

    public long getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getService() { return service; }
    public String getHost() { return host; }
    public String getMessage() { return message; }
    public long getResponseTime() { return responseTime; }
    public int getStatusCode() { return statusCode; }
    public String getTraceId() { return traceId; }
}
