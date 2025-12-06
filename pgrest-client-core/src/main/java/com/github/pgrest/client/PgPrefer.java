package com.github.pgrest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class PgPrefer {
    private String handling;
    private String timezone;
    private String ret;
    private String count;
    private String resolution;
    private String missing;
    private String maxAffected;
    private String tx;

    public static PgPrefer create() { return new PgPrefer(); }

    public PgPrefer handlingLenient() { this.handling = "handling=lenient"; return this; }
    public PgPrefer handlingStrict() { this.handling = "handling=strict"; return this; }

    public PgPrefer timezone(String tz) { this.timezone = tz == null || tz.isBlank() ? null : ("timezone=" + tz); return this; }

    public PgPrefer returnMinimal() { this.ret = "return=minimal"; return this; }
    public PgPrefer returnHeadersOnly() { this.ret = "return=headers-only"; return this; }
    public PgPrefer returnRepresentation() { this.ret = "return=representation"; return this; }

    public PgPrefer countNone() { this.count = "count=none"; return this; }
    public PgPrefer countExact() { this.count = "count=exact"; return this; }

    public PgPrefer resolutionMergeDuplicates() { this.resolution = "resolution=merge-duplicates"; return this; }
    public PgPrefer resolutionIgnoreDuplicates() { this.resolution = "resolution=ignore-duplicates"; return this; }

    public PgPrefer missingDefault() { this.missing = "missing=default"; return this; }

    public PgPrefer maxAffected(int n) { this.maxAffected = n <= 0 ? null : ("max-affected=" + n); return this; }

    public PgPrefer txCommit() { this.tx = "tx=commit"; return this; }
    public PgPrefer txRollback() { this.tx = "tx=rollback"; return this; }

    public String toHeaderValue() {
        List<String> parts = new ArrayList<>(8);
        if (handling != null) parts.add(handling);
        if (timezone != null) parts.add(timezone);
        if (ret != null) parts.add(ret);
        if (count != null) parts.add(count);
        if (resolution != null) parts.add(resolution);
        if (missing != null) parts.add(missing);
        if (maxAffected != null) parts.add(maxAffected);
        if (tx != null) parts.add(tx);
        if (parts.isEmpty()) return "";
        StringJoiner joiner = new StringJoiner(",");
        for (String s : parts) joiner.add(s);
        return joiner.toString();
    }
}

