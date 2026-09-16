package com.logstream.index;

import java.util.Set;

/** Single source of truth for index field names and their types. */
public final class Fields {
    public static final String TIMESTAMP     = "timestamp";
    public static final String LEVEL         = "level";
    public static final String SERVICE       = "service";
    public static final String HOST          = "host";
    public static final String MESSAGE       = "message";
    public static final String RESPONSE_TIME = "response_time";
    public static final String STATUS_CODE   = "status_code";
    public static final String TRACE_ID      = "trace_id";

    /** Fields backed by LongPoint, so they accept >, <, >=, <= and ranges. */
    public static final Set<String> NUMERIC =
            Set.of(TIMESTAMP, RESPONSE_TIME, STATUS_CODE);

    /** Exact-match keyword fields: no analysis, so "billing-api" stays one term. */
    public static final Set<String> KEYWORD =
            Set.of(LEVEL, SERVICE, HOST, TRACE_ID);

    /** Analyzed field used when the user types a bare word with no field prefix. */
    public static final String DEFAULT_FIELD = MESSAGE;

    private Fields() {}
}
