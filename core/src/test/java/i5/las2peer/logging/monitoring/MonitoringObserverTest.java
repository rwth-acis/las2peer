package i5.las2peer.logging.monitoring;

import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.communication.Message;
import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.MonitoringAgent;
import i5.las2peer.tools.CryptoTools;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.KeyPair;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for the MonitoringObserver.
 *
 * The MonitoringObserver should store incoming log messages and try to send them all at once when a certain amount
 * of messages is reached.
 * The messages are sent to a processing service. The MonitoringObserver should be resilient against the service
 * becoming suddenly unavailable and should continue to work as soon as the service becomes available again.
 */
@RunWith(MockitoJUnitRunner.class)
public class MonitoringObserverTest {

    @Mock
    private Node node;
    @Mock
    private MonitoringAgent receivingAgent;
    private static final String RECEIVING_AGENT_IDENTIFIER = "RECEIVING_AGENT_ID";
    private static final KeyPair RECEIVING_AGENT_KEY_PAIR = CryptoTools.generateKeyPair();
    private static final int MIN_MESSAGE_CACHE_SIZE = 50;
    private static final MonitoringEvent SOME_EVENT = MonitoringEvent.SERVICE_CUSTOM_MESSAGE_42;
    private static final String SOME_SOURCE_NODE = "some source node";

    /**
     * Configure the node mock to behave like a node with an existing processing service.
     *
     * @throws AgentException Should not be thrown because the service exists.
     */
    public void setupNodeWithExistingProcessingService() throws AgentException {
        reset(node);
        when(receivingAgent.getPublicKey()).thenReturn(RECEIVING_AGENT_KEY_PAIR.getPublic());
        when(receivingAgent.getIdentifier()).thenReturn(RECEIVING_AGENT_IDENTIFIER);
        when(node.getAgent(anyString())).thenReturn(receivingAgent);
    }

    /**
     * Simulate a node in a network without any monitoring agent.
     *
     * @throws AgentException Always thrown, because the service never exists.
     */
    public void setupNodeWithoutProcessingService() throws AgentException {
        reset(node);
        when(node.getAgent(anyString())).thenThrow(new AgentException("Agent not found"));
    }


    @Test
    public void testProcessingAgentExists() throws AgentException {
        // given
        setupNodeWithExistingProcessingService();
        MonitoringObserver observer = new MonitoringObserver(MIN_MESSAGE_CACHE_SIZE, this.node);
        triggerReadyForInit(observer);
        // when
        fillMessageCache(observer);
        // then
        ArgumentCaptor<Message> messageCapture = ArgumentCaptor.forClass(Message.class);
        verify(node, times(1)).sendMessage(messageCapture.capture(),
                any(MessageResultListener.class));
    }

    @Test
    public void testProcessingAgentMissing() throws AgentException {
        // given
        setupNodeWithoutProcessingService();
        MonitoringObserver observer = new MonitoringObserver(MIN_MESSAGE_CACHE_SIZE, this.node);
        triggerReadyForInit(observer);
        // when
        fillMessageCache(observer);
        // then
        verify(node, never()).sendMessage(any(Message.class), any(MessageResultListener.class));
    }

    @Test
    public void testProcessingAgentMissingAndThenAppearing() throws AgentException {
        // given
        setupNodeWithoutProcessingService();
        MonitoringObserver observer = new MonitoringObserver(MIN_MESSAGE_CACHE_SIZE, this.node);
        triggerReadyForInit(observer);
        fillMessageCache(observer);
        // when
        setupNodeWithExistingProcessingService();
        fillMessageCache(observer);
        // then
        verify(node, times(1)).sendMessage(any(Message.class),
                any(MessageResultListener.class));
    }

    @Test
    public void testProcessingAgentExistsAndThenMissingAndThenAppearing() throws AgentException {
        // given
        setupNodeWithExistingProcessingService();
        MonitoringObserver observer = new MonitoringObserver(MIN_MESSAGE_CACHE_SIZE, this.node);
        triggerReadyForInit(observer);
        fillMessageCache(observer);
        setupNodeWithoutProcessingService();
        fillMessageCache(observer);
        // when
        setupNodeWithExistingProcessingService();
        fillMessageCache(observer);
        // then
        // just test for single call because the mock is reset upon each "setup..." call and forgets previous calls
        verify(node, times(1)).sendMessage(any(Message.class),
                any(MessageResultListener.class));
    }

    private void triggerReadyForInit(MonitoringObserver observer) {
        simpleLog(observer, MonitoringEvent.NODE_STATUS_CHANGE, "RUNNING");
    }

    private void simpleLog(MonitoringObserver observer, MonitoringEvent event, String sourceNode, String sourceAgentId,
                           String destinationNode, String destinationAgentId, String remarks) {
        observer.log(System.currentTimeMillis(), event, sourceNode, sourceAgentId, destinationNode, destinationAgentId,
                remarks);
    }

    private void simpleLog(MonitoringObserver observer, MonitoringEvent event, String remarks) {
        simpleLog(observer, event, SOME_SOURCE_NODE, null, null, "test",
                remarks);
    }

    private void simpleLog(MonitoringObserver observer, MonitoringEvent event) {
        simpleLog(observer, event, null);
    }

    private void simpleLog(MonitoringObserver observer) {
        simpleLog(observer, SOME_EVENT);
    }

    private void fillMessageCache(MonitoringObserver observer) {
        for (int i = 0; i < MIN_MESSAGE_CACHE_SIZE; i++) {
            simpleLog(observer);
        }
    }
}
