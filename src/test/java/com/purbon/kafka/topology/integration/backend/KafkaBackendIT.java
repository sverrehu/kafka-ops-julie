package com.purbon.kafka.topology.integration.backend;

import static com.purbon.kafka.topology.CommandLineInterface.BROKERS_OPTION;
import static com.purbon.kafka.topology.Constants.JULIE_INSTANCE_ID;
import static com.purbon.kafka.topology.Constants.JULIE_KAFKA_STATE_CONSUMER_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.purbon.kafka.topology.Configuration;
import com.purbon.kafka.topology.backend.BackendState;
import com.purbon.kafka.topology.backend.KafkaBackend;
import com.purbon.kafka.topology.integration.containerutils.ContainerFactory;
import com.purbon.kafka.topology.integration.containerutils.ContainerTestUtils;
import com.purbon.kafka.topology.integration.containerutils.SaslPlaintextKafkaContainer;
import com.purbon.kafka.topology.roles.TopologyAclBinding;
import java.util.*;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KafkaBackendIT {

  Configuration config;
  Properties props;

  private static SaslPlaintextKafkaContainer container;

  @Before
  public void before() {

    container = ContainerFactory.fetchSaslKafkaContainer(System.getProperty("cp.version"));
    container.start();
    ContainerTestUtils.resetAcls(container);

    props = new Properties();
    props.put(JULIE_INSTANCE_ID, "1234");

    Map<String, Object> saslConfig =
        ContainerTestUtils.getSaslConfig(
            container.getBootstrapServers(),
            SaslPlaintextKafkaContainer.JULIE_USERNAME,
            SaslPlaintextKafkaContainer.JULIE_PASSWORD);
    saslConfig.forEach((k, v) -> props.setProperty(k, String.valueOf(v)));

    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, container.getBootstrapServers());

    config = new Configuration(cliOps, props);
  }

  @After
  public void after() {
    container.stop();
  }

  @Test
  public void testExpectedFlow() throws InterruptedException {

    TopologyAclBinding binding1 =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "foo", "*", "Write", "User:foo", "LITERAL");
    TopologyAclBinding binding2 =
        TopologyAclBinding.build(
            ResourceType.TOPIC.name(), "bar", "*", "Read", "User:bar", "LITERAL");
    Collection<TopologyAclBinding> bindings1 = Collections.singleton(binding1);
    Collection<TopologyAclBinding> bindings2 = Arrays.asList(binding1, binding2);
    Collection<TopologyAclBinding> bindings3 = Collections.singleton(binding2);

    saveBindings(bindings1);
    loadAndVerifyBindings(bindings1);

    saveBindings(bindings2);
    loadAndVerifyBindings(bindings2);

    saveBindings(bindings3);
    loadAndVerifyBindings(bindings3);
  }

  private void saveBindings(Collection<TopologyAclBinding> bindings) {
    BackendState state = new BackendState();
    state.addBindings(bindings);
    KafkaBackend backend = new KafkaBackend();
    backend.configure(config);
    backend.save(state);
    backend.close();
  }

  private void loadAndVerifyBindings(Collection<TopologyAclBinding> expectedBindings) {
    KafkaBackend backend = new KafkaBackend();
    HashMap<String, String> cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, container.getBootstrapServers());
    props.put(JULIE_KAFKA_STATE_CONSUMER_GROUP_ID, "julieops-" + KafkaBackendIT.class.getName());
    Configuration config = new Configuration(cliOps, props);
    backend.configure(config);
    BackendState state = backend.load();
    assertThat(state.size()).isEqualTo(expectedBindings.size());
    for (TopologyAclBinding binding : expectedBindings) {
      assertThat(state.getBindings()).contains(binding);
    }
    backend.close();
  }
}
