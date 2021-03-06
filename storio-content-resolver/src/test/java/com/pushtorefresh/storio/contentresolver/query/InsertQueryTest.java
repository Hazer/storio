package com.pushtorefresh.storio.contentresolver.query;

import android.net.Uri;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class InsertQueryTest {

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void nullUri() {
        new InsertQuery.Builder()
                .uri((Uri) null)
                .build();
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = RuntimeException.class)
    public void nullUriString() {
        new InsertQuery.Builder()
                .uri((String) null)
                .build();
    }

    @Test
    public void build() {
        final Uri uri = mock(Uri.class);

        final InsertQuery insertQuery = new InsertQuery.Builder()
                .uri(uri)
                .build();

        assertEquals(uri, insertQuery.uri());
    }
}
