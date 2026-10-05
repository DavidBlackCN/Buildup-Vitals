package com.davidblackcn.buildupvitals.compat.thirst;

import com.thirstwastaken2.api.ThirstApi;

/** Loaded only behind the supported-version gate. No third-party implementation is copied. */
public final class ThirstBridge {
    private ThirstBridge() { }

    public static void invalidate() { ThirstApi.clearCache(); }
}
