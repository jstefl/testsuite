package org.jboss.hal.testsuite.test.configuration.jgroups;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.hal.testsuite.cli.CliClient;
import org.jboss.hal.testsuite.cli.CliClientFactory;
import org.jboss.hal.testsuite.creaper.ManagementClientProvider;
import org.jboss.hal.testsuite.creaper.command.AddSocketBinding;
import org.jboss.hal.testsuite.finder.Application;
import org.jboss.hal.testsuite.finder.FinderNames;
import org.jboss.hal.testsuite.finder.FinderNavigation;
import org.jboss.hal.testsuite.fragment.config.jgroups.JGroupsProtocolPropertiesFragment;
import org.jboss.hal.testsuite.fragment.config.jgroups.JGroupsProtocolPropertyWizard;
import org.jboss.hal.testsuite.fragment.config.jgroups.JGroupsTransportPropertiesFragment;
import org.jboss.hal.testsuite.fragment.config.jgroups.JGroupsTransportPropertyWizard;
import org.jboss.hal.testsuite.page.config.DomainConfigEntryPoint;
import org.jboss.hal.testsuite.page.config.JGroupsPage;
import org.jboss.hal.testsuite.page.config.StandaloneConfigEntryPoint;
import org.jboss.hal.testsuite.test.util.ConfigAreaChecker;
import org.jboss.hal.testsuite.util.ConfigUtils;
import org.jboss.hal.testsuite.util.Console;
import org.jboss.hal.testsuite.util.ResourceVerifier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by jkasik <jkasik@redhat.com>
 */
public class JGroupAbstractTestCase {

    private static final Logger log = LoggerFactory.getLogger(JGroupAbstractTestCase.class);

    protected static final String PROPERTY_NAME = "URL";
    protected static final String PROPERTY_VALUE = "url";
    protected static final String PROPERTY_NAME_P = "URL1";
    protected static final String PROPERTY_VALUE_P = "url1";
    protected static final String DEFAULT_PROTOCOL = "UNICAST3";

    private static CliClient client = CliClientFactory.getClient();
    protected static ResourceVerifier verifier = new ResourceVerifier("", client);
    private ConfigAreaChecker checker = new ConfigAreaChecker(verifier);
    protected static JGroupsOperations jGroupsOperations = new JGroupsOperations(client);

    private FinderNavigation navigation;

    @Drone
    public WebDriver browser;

    @Page
    public JGroupsPage page;

    @Before
    public void before() {
        if (ConfigUtils.isDomain()) {
            navigation = new FinderNavigation(browser, DomainConfigEntryPoint.class)
                    .addAddress(FinderNames.CONFIGURATION, FinderNames.PROFILES)
                    .addAddress(FinderNames.PROFILE, "full-ha");
        } else {
            navigation = new FinderNavigation(browser, StandaloneConfigEntryPoint.class)
                    .addAddress(FinderNames.CONFIGURATION, FinderNames.SUBSYSTEMS);
        }
        navigation.addAddress(FinderNames.SUBSYSTEM, "JGroups")
                .selectRow(true)
                .invoke(FinderNames.VIEW);
        Application.waitUntilVisible();
    }

    @After
    public void after() {
        Console.withBrowser(browser).refresh();
        Console.withBrowser(browser).waitUntilLoaded();
    }

    @AfterClass
    public static void tearDown() {
        jGroupsOperations.removeTransportProperty(PROPERTY_NAME, PROPERTY_VALUE);
        jGroupsOperations.removeTransportProperty(PROPERTY_NAME_P, PROPERTY_VALUE_P);
        jGroupsOperations.removeProtocolProperty(DEFAULT_PROTOCOL, PROPERTY_NAME);
        jGroupsOperations.removeProtocolProperty(DEFAULT_PROTOCOL, PROPERTY_NAME_P);
    }

    @Test
    public void socketBindingEdit() throws IOException, CommandFailedException {
        String name = "JGroupsSocketBinding_";
        try (OnlineManagementClient client = ManagementClientProvider.createOnlineManagementClient()) {
            int port = AvailablePortFinder.getNextAvailable();
            log.info("Obtained port for socket binding '" + name + "' is " + port);
            client.apply(new AddSocketBinding.Builder(name)
                    .port(port)
                    .build());
        }
        checker.editTextAndAssert(page, "socketBinding", name).dmrAttribute("socket-binding").invoke();
    }

    @Test(expected = AssertionError.class)
    public void socketBindingEditInvalid() {
        String name = "JGroupsInvalidSocket_" + RandomStringUtils.randomAlphabetic(6);
        checker.editTextAndAssert(page, "socketBinding", name).dmrAttribute("socket-binding").invoke();
    }

    @Test
    public void diagnosticSocketEdit() {
        String name = "jgroups-udp";
        checker.editTextAndAssert(page, "diagSocketBinding", name)
                .dmrAttribute("diagnostics-socket-binding").invoke();
    }

    @Test(expected = AssertionError.class)
    public void diagnosticSocketEditInvalid() {
        String name = "qwedfsdg"; //non-existing socket binding
        checker.editTextAndAssert(page, "diagSocketBinding", name)
                .dmrAttribute("diagnostics-socket-binding").invoke();
    }

    @Test
    public void machineEdit() {
        String name = "JGroupsMachine_" + RandomStringUtils.randomAlphabetic(6);
        checker.editTextAndAssert(page, "machine", name).dmrAttribute("machine").invoke();
    }

    @Test
    public void sharedStatusEdit() {
        checker.editCheckboxAndAssert(page, "shared", true).dmrAttribute("shared").invoke();
    }

    @Test
    public void siteEdit() {
        String name = "JGroupsSite_" + RandomStringUtils.randomAlphabetic(6);
        checker.editTextAndAssert(page, "site", name).invoke();
    }

    @Test
    public void rackEdit() {
        String name = "JGroupsRack_" + RandomStringUtils.randomAlphabetic(6);
        checker.editTextAndAssert(page, "rack", name).invoke();
    }

    @Test
    public void createTransportProperty() {
        JGroupsTransportPropertiesFragment properties = page.getConfig().transportPropertiesConfig();
        JGroupsTransportPropertyWizard wizard = properties.addProperty();
        wizard.key(PROPERTY_NAME).value(PROPERTY_VALUE).finish();
        assertTrue(jGroupsOperations.verifyTransportProperty(PROPERTY_NAME, PROPERTY_VALUE));
    }

    @Test
    public void removeTransportProperty() {
        JGroupsTransportPropertiesFragment properties = page.getConfig().transportPropertiesConfig();
        properties.removeProperty(PROPERTY_NAME_P);
        assertFalse(jGroupsOperations.verifyTransportProperty(PROPERTY_NAME_P, PROPERTY_VALUE_P));
    }


    @InSequence(2)
    @Test
    public void createProtocolProperty() {
        page.switchToProtocol(DEFAULT_PROTOCOL);
        JGroupsProtocolPropertiesFragment properties = page.getConfig().protocolPropertiesConfig();
        JGroupsProtocolPropertyWizard wizard = properties.addProperty();
        wizard.key(PROPERTY_NAME).value(PROPERTY_VALUE).finish();
        assertTrue(jGroupsOperations.verifyProtocolProperty(DEFAULT_PROTOCOL, PROPERTY_NAME, PROPERTY_VALUE));
    }

    @InSequence(1)
    @Test
    public void removeProtocolProperty() {
        page.switchToProtocol(DEFAULT_PROTOCOL);
        JGroupsProtocolPropertiesFragment properties = page.getConfig().protocolPropertiesConfig();
        properties.removeProperty(PROPERTY_NAME_P);
        assertFalse(jGroupsOperations.verifyProtocolProperty(DEFAULT_PROTOCOL, PROPERTY_NAME_P, PROPERTY_VALUE_P));
    }
}
