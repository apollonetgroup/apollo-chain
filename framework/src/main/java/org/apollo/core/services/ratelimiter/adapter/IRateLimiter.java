package org.apollo.core.services.ratelimiter.adapter;

import org.apollo.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}
