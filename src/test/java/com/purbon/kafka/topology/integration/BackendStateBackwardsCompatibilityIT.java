package com.purbon.kafka.topology.integration;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.JulieOps;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.utils.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.purbon.kafka.topology.CommandLineInterface.*;
import static com.purbon.kafka.topology.CommandLineInterface.CLIENT_CONFIG_OPTION;
import static com.purbon.kafka.topology.Constants.*;

public class BackendStateBackwardsCompatibilityIT {

    private static SaslPlaintextKafkaContainer container;

    @BeforeClass
    public static void setup() {
        container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
        container.start();
    }

    @AfterClass
    public static void teardown() {
        container.stop();
    }

    @Test
    public void shouldKeepBackwardsCompatibilityOnState() throws Exception {
        String fileOrDirPath = TestUtils.getResourceFilename("/backwards-compatible-state-descriptor.yaml");
        String clientConfigFile = TestUtils.getResourceFilename("/client-config.properties");

        Map<String, String> cliParams = new HashMap<>();
        cliParams.put(BROKERS_OPTION, container.getBootstrapServers());
        cliParams.put(DRY_RUN_OPTION, "true");
        cliParams.put(QUIET_OPTION, "true");
        cliParams.put(CLIENT_CONFIG_OPTION, clientConfigFile);

        final JulieOps julieOps = JulieOps.build(fileOrDirPath, cliParams);
        julieOps.run();
    }

}
