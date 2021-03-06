package org.jboss.hal.testsuite.test.configuration.infinispan;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.hal.testsuite.category.Shared;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Values;

import java.io.IOException;

/**
 * @author mkrajcov <mkrajcov@redhat.com>
 */
@RunWith(Arquillian.class)
@Category(Shared.class)
public class DistributedCacheTestCase extends AbstractCacheTestCase {

    @BeforeClass
    public static void beforeClass_() {
        initializeAddresses(CacheType.DISTRIBUTED);
    }

    @Before
    public void before_() {
        page.distributed();
        page.selectCache(CACHE_NAME);
    }

    protected void addCache() throws IOException {
        operations.add(CACHE_ADDRESS, Values.of("mode", "SYNC"));
        operations.add(TRANSACTION_ADDRESS);
        operations.add(STORE_ADDRESS, Values.of("class", "org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder"));
        operations.add(LOCKING_ADDRESS);
    }

    protected void deleteCache() throws IOException, OperationException {
        operations.removeIfExists(CACHE_ADDRESS);
    }
}

