package com.kwizzad;

import com.kwizzad.model.OpenTransaction;

import java.util.Collection;

/**
 * Created by tvsmiles on 24.05.17.
 */

public interface PendingTransactionsCallback {
    void callback(Collection<OpenTransaction> transactions);
}
