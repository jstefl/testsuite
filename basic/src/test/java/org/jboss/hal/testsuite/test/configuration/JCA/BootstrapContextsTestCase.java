package org.jboss.hal.testsuite.test.configuration.JCA;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.dmr.ModelNode;
import org.jboss.hal.testsuite.category.Standalone;
import org.jboss.hal.testsuite.cli.CliClient;
import org.jboss.hal.testsuite.cli.CliClientFactory;
import org.jboss.hal.testsuite.cli.TimeoutException;
import org.jboss.hal.testsuite.dmr.Dispatcher;
import org.jboss.hal.testsuite.dmr.ResourceAddress;
import org.jboss.hal.testsuite.dmr.ResourceVerifier;
import org.jboss.hal.testsuite.finder.Application;
import org.jboss.hal.testsuite.finder.FinderNames;
import org.jboss.hal.testsuite.finder.FinderNavigation;
import org.jboss.hal.testsuite.fragment.shared.modal.ConfirmationWindow;
import org.jboss.hal.testsuite.page.config.JCAPage;
import org.jboss.hal.testsuite.page.config.StandaloneConfigEntryPoint;
import org.jboss.hal.testsuite.util.Console;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

/**
 * Created by pcyprian on 21.10.15.
 */
@RunWith(Arquillian.class)
@Category(Standalone.class)
public class BootstrapContextsTestCase {
    private static final String NAME = "test-BC";

    private FinderNavigation navigation;

    private ModelNode path = new ModelNode("/subsystem=jca/bootstrap-context=" + NAME);
    private ResourceAddress address = new ResourceAddress(path);
    Dispatcher dispatcher = new Dispatcher();
    ResourceVerifier verifier = new ResourceVerifier(dispatcher);
    CliClient cliClient = CliClientFactory.getClient();

    @Drone
    private WebDriver browser;
    @Page
    private JCAPage page;

    @Before
    public void before() {
        navigation = new FinderNavigation(browser, StandaloneConfigEntryPoint.class)
                .addAddress(FinderNames.CONFIGURATION, FinderNames.SUBSYSTEMS)
                .addAddress(FinderNames.SUBSYSTEM, "JCA");

        navigation.selectRow().invoke("View");
        Application.waitUntilVisible();
        page.switchToBootstrapContextsTab();
    }

    @After()
    public void after() {
        cliClient.reload();
    }

    @Test
    public void addBootstrapContexts() {
        page.clickButton("Add");
        page.getWindowFragment().getEditor().text("name", NAME);
        page.getWindowFragment().getEditor().select("workmanager", "default");
        page.getWindowFragment().clickButton("Save");

        verifier.verifyResource(address, true);

        cliClient.executeCommand("/subsystem=jca/bootstrap-context=" + NAME + ":remove");

        verifier.verifyResource(address, false);
    }

    @Test
    public void removeBootstrapContexts() {
        page.clickButton("Add");
        page.getWindowFragment().getEditor().text("name", NAME);
        page.getWindowFragment().getEditor().select("workmanager", "default");
        page.getWindowFragment().clickButton("Save");
        verifier.verifyResource(address, true);

        page.getResourceManager().getResourceTable().selectRowByText(0, NAME);
        page.clickButton("Remove");

        try {
            Console.withBrowser(browser).openedWindow(ConfirmationWindow.class).confirm();
        } catch (TimeoutException ignored) {
        }

        verifier.verifyResource(address, false);
    }

}
