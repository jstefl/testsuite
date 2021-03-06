package org.jboss.hal.testsuite.test.configuration.logging;

import org.apache.commons.lang.RandomStringUtils;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.hal.testsuite.category.Shared;
import org.jboss.hal.testsuite.creaper.ResourceVerifier;
import org.jboss.hal.testsuite.page.config.LoggingPage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Values;

import java.io.IOException;

/**
 * Created by pcyprian on 14.10.15.
 */
@RunWith(Arquillian.class)
@Category(Shared.class)
public class PeriodicSizeTestCase extends LoggingAbstractTestCase {

    private static final String PERIODIC_SIZE_HANDLER = "PeriodicSizeHandler" + RandomStringUtils.randomAlphanumeric(5);
    private static final String PERIODIC_SIZE_HANDLER_TBR = "PeriodicSizeHandler-tbr" + RandomStringUtils.randomAlphanumeric(5);
    private static final String PERIODIC_SIZE_HANDLER_TBA = "PeriodicSizeHandler-tba" + RandomStringUtils.randomAlphanumeric(5);

    private static final Address PERIODIC_SIZE_HANDLER_ADDRESS = LOGGING_SUBSYSTEM
            .and("periodic-size-rotating-file-handler", PERIODIC_SIZE_HANDLER);
    private static final Address PERIODIC_SIZE_HANDLER_TBR_ADDRESS = LOGGING_SUBSYSTEM
            .and("periodic-size-rotating-file-handler", PERIODIC_SIZE_HANDLER_TBR);
    private static final Address PERIODIC_SIZE_HANDLER_TBA_ADDRESS = LOGGING_SUBSYSTEM
            .and("periodic-size-rotating-file-handler", PERIODIC_SIZE_HANDLER_TBA);


    @BeforeClass
    public static void setUp() throws Exception {
        createPeriodicSizeFileHandler(PERIODIC_SIZE_HANDLER_ADDRESS, "periodic-handler.log");
        new ResourceVerifier(PERIODIC_SIZE_HANDLER_ADDRESS, client).verifyExists();
        createPeriodicSizeFileHandler(PERIODIC_SIZE_HANDLER_TBR_ADDRESS, "periodic-handler2.log");
        new ResourceVerifier(PERIODIC_SIZE_HANDLER_TBR_ADDRESS, client).verifyExists();
    }

    @AfterClass
    public static void tearDown() throws IOException, OperationException {
        operations.removeIfExists(PERIODIC_SIZE_HANDLER_ADDRESS);
        operations.removeIfExists(PERIODIC_SIZE_HANDLER_TBA_ADDRESS);
        operations.removeIfExists(PERIODIC_SIZE_HANDLER_TBR_ADDRESS);
    }

    @Drone
    private WebDriver browser;
    @Page
    private LoggingPage page;

    @Before
    public void before() {
        page.navigate();
        page.switchToHandlerTab();
        page.switchToPeriodicSize();
        page.selectHandler(PERIODIC_SIZE_HANDLER);
    }

    @Test
    public void addPeriodicSizeHandler() throws Exception {
        page.addPeriodicSizeHandler(PERIODIC_SIZE_HANDLER_TBA, ".yyyy-MM-dd,HH:mm", getTmpDirPath("logs"));

        new ResourceVerifier(PERIODIC_SIZE_HANDLER_TBA_ADDRESS, client).verifyExists();
    }

    @Test
    public void updatePeriodicSizeHandlerNamedFormatter() throws Exception {
        editTextAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "named-formatter", "PATTERN");
    }

    @Test
    public void updatePeriodicSizeHandlerEncoding() throws Exception {
        editTextAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "encoding", "UTF-8");
    }

    @Test
    public void updatePeriodicSizeHandlerAppend() throws Exception {
        editCheckboxAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "append", false);
    }

    @Test
    public void updatePeriodicSizeHandlerAutoflush() throws Exception {
        editCheckboxAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "autoflush", false);
    }

    @Test
    public void disablePeriodicSizeHandler() throws Exception {
        editCheckboxAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "enabled", false);
    }

    @Test
    public void updatePeriodicSizeHandlerLevel() throws Exception {
        selectOptionAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "level", "CONFIG");
    }

    @Test
    public void updatePeriodicSizeHandlerFilterSpec() throws Exception {
        editTextAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "filter-spec", "match(\"JBEAP.*\")");
    }

    @Test
    public void updatePeriodicSizeHandlerFormatter() throws Exception {
        editTextAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "formatter", "%d{HH:mm:ss,SSS}");
    }

    @Test
    public void updatePeriodicSizeHandlerRotateOnBoot() throws Exception {
        editCheckboxAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "rotate-on-boot", true);
    }

    @Test
    public void updatePeriodicSizeHandlerMaxBackupIndex() throws Exception {
        editTextAndVerify(PERIODIC_SIZE_HANDLER_ADDRESS, "max-backup-index", 3);
    }

    @Test
    public void removePeriodicSizeHandler() throws Exception {
        page.removeInTable(PERIODIC_SIZE_HANDLER_TBR);

        new ResourceVerifier(PERIODIC_SIZE_HANDLER_TBR_ADDRESS, client).verifyDoesNotExist();
    }

    private static void createPeriodicSizeFileHandler(Address address, String path) throws IOException {
        operations.add(address, Values.empty()
                .andObject("file", Values.empty()
                        .and("path", path)
                        .and("relative-to", "jboss.server.log.dir"))
                .and("suffix", "%H%m"));
    }

}
