package org.jboss.hal.testsuite.util.configurationchanges;

import org.apache.commons.lang3.StringUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Predicate;

/**
 * Class for obtaining configuration changes directly from model for comparation with UI
 *
 * @author Jan Kasik <jkasik@redhat.com>
 *         Created on 9/14/16.
 */
public class ConfigurationChangesResource implements ConfigurationChangesProvider {

    private final Operations operations;
    private final Address serviceAddress;

    private static final String OPERATIONS = "operations";
    private static final String OP = "operation";
    private static final String ADDRESS = "address";
    private static final String COMPOSITE = "composite";
    private static final String STEPS = "steps";
    private static final String OP_ADDR = "address";

    private final Logger logger = LoggerFactory.getLogger(ConfigurationChangesResource.class);

    public ConfigurationChangesResource(OnlineManagementClient client) {
        this.operations = new Operations(client);
        this.serviceAddress = client.options().isDomain ?
                Address.host(client.options().defaultHost).and("core-service", "management")
                        .and("service", "configuration-changes") :
                Address.coreService("management")
                        .and("service", "configuration-changes");
    }

    private Date parseDate(String datetimeString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        return format.parse(datetimeString);
    }

    private List<ConfigurationChange> getConfigurationChanges(Predicate<ConfigurationChange> predicate) throws IOException, ParseException {
        ModelNodeResult result = operations.invoke("list-changes", serviceAddress);
        result.assertSuccess();

        List<ConfigurationChange> changes = new LinkedList<>();
        for (ModelNode change : result.value().asList()) {

            logger.info(change.get("operations").asString());

            ConfigurationChange configurationChange = new Change(
                    parseDate(change.get("operation-date").asString()),
                    change.get("access-mechanism").asString(),
                    change.get("remote-address").asString(),
                    change.get("outcome").asString(),
                    extractOperationName(change),
                    StringUtils.abbreviate(extractResourceAddress(change), 66)
            );
            if (predicate.test(configurationChange)) {
                changes.add(configurationChange);
            }

        }
        return changes;
    }

    //from HAL
    private String extractOperationName(final ModelNode changeItem) {
        String opName = "";
        ModelNode operations = changeItem.get(OPERATIONS);
        for (ModelNode op1: operations.asList()) {
            opName = op1.get(OP).asString();
        }
        return opName;
    }

    //from HAL
    private String extractResourceAddress(ModelNode changeItem) {
        StringBuilder address = new StringBuilder();
        ModelNode operations = changeItem.get(OPERATIONS);
        for (ModelNode op1 : operations.asList()) {
            String opName = op1.get(OP).asString();
            if (COMPOSITE.equals(opName)) {

                List<ModelNode> steps = op1.get(STEPS).asList();
                for (int idxStep = 0; idxStep < steps.size(); idxStep++) {
                    ModelNode step = steps.get(idxStep);
                    if (step.hasDefined(OP_ADDR)) {
                        ModelNode addressNode = step.get(OP_ADDR);
                        List<ModelNode> modelNodes = addressNode.asList();
                        for (int i = 0; i < modelNodes.size(); i++) {
                            ModelNode addr = modelNodes.get(i);
                            Property p = addr.asProperty();
                            address.append(p.getName()).append(" = ").append(p.getValue().asString());
                            if (i + 1 < modelNodes.size())
                                address.append(" / ");
                        }
                    }
                    // separates each step resource address
                    if (idxStep + 1 < steps.size()) {
                        address.append(" | ");
                    }
                }

            } else {
                if (op1.hasDefined(OP_ADDR)) {
                    ModelNode addressNode = op1.get(OP_ADDR);
                    List<ModelNode> modelNodes = addressNode.asList();
                    for (int i = 0; i < modelNodes.size(); i++) {
                        ModelNode addr = modelNodes.get(i);
                        Property p = addr.asProperty();
                        address.append(p.getName()).append(" = ").append(p.getValue().asString());
                        if (i + 1 < modelNodes.size())
                            address.append(" / ");
                    }
                }
            }
        }
        return address.toString();
    }

    public List<ConfigurationChange> getAllConfigurationChanges() throws IOException, ParseException {
        return getConfigurationChanges(change -> true);
    }

    /**
     * Return all configuration changes from model which contains given filter string
     * @param filter string by which thew changes will be filtered
     * @return filtered configuration changes
     * @throws ParseException
     * @throws IOException
     */
    public List<ConfigurationChange> getFilteredConfigurationChanges(String filter) throws ParseException, IOException {
        return getConfigurationChanges(configurationChange -> {
            try {
                return configurationChange.getAccessMechanism().contains(filter) ||
                        configurationChange.getDatetime().toString().contains(filter) ||
                        configurationChange.getRemoteAddress().contains(filter) ||
                        configurationChange.getResult().contains(filter) ||
                        configurationChange.getOperation().contains(filter) ||
                        configurationChange.getResourceAddress().contains(filter);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Basic implementation of {@link ConfigurationChange}. Serves mainly as abstraction over values from model.
     * @see ConfigurationChange
     */
    private static final class Change implements ConfigurationChange {

        private Date datetime;
        private String accessMechanism;
        private String remoteAddress;
        private String outcome;
        private String operation;
        private String resourceAddress;

        private Change(Date datetime, String accessMechanism, String remoteAddress, String outcome, String operation, String address) {
            this.datetime = datetime;
            this.accessMechanism = accessMechanism;
            this.remoteAddress = !remoteAddress.isEmpty() ? remoteAddress.split("/")[0] : "";
            this.outcome = outcome;
            this.operation = operation;
            this.resourceAddress = address;
        }

        public Date getDatetime() {
            return datetime;
        }

        public String getAccessMechanism() {
            return accessMechanism;
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }

        public String getResult() {
            return outcome;
        }

        public String getOperation() {
            return operation;
        }

        public String getResourceAddress() {
            return resourceAddress;
        }

    }
}
