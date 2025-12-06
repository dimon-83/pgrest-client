package com.github.pgrest.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PgPreferTest {
    @Test
    public void composeAll() {
        String h = PgPrefer.create()
                .handlingStrict()
                .timezone("America/Los_Angeles")
                .returnRepresentation()
                .countExact()
                .resolutionMergeDuplicates()
                .missingDefault()
                .maxAffected(10)
                .txRollback()
                .toHeaderValue();
        Assertions.assertTrue(h.contains("handling=strict"));
        Assertions.assertTrue(h.contains("timezone=America/Los_Angeles"));
        Assertions.assertTrue(h.contains("return=representation"));
        Assertions.assertTrue(h.contains("count=exact"));
        Assertions.assertTrue(h.contains("resolution=merge-duplicates"));
        Assertions.assertTrue(h.contains("missing=default"));
        Assertions.assertTrue(h.contains("max-affected=10"));
        Assertions.assertTrue(h.contains("tx=rollback"));
    }
}

