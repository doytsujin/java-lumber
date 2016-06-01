import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.logstash.beats.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by ph on 2016-06-01.
 */
public class BeatsHandlerTest {
    private SpyListener spyListener;
    private BeatsHandler beatsHandler;
    private Batch batch;

    private class SpyListener implements IMessageListener {
        private boolean onNewConnectionCalled = false;
        private boolean onNewMessageCalled = false;
        private boolean onConnectionCloseCalled = false;
        private final List<Message> lastMessages = new ArrayList<Message>();

        @Override
        public void onNewMessage(Message message) {
            onNewMessageCalled = true;
            lastMessages.add(message);
        }

        @Override
        public void onNewConnection(ChannelHandlerContext ctx) {
            onNewConnectionCalled = true;
        }

        @Override
        public void onConnectionClose(ChannelHandlerContext ctx) {
            onConnectionCloseCalled = true;
        }

        public boolean isOnNewConnectionCalled() {
            return onNewConnectionCalled;
        }

        public boolean isOnNewMessageCalled() {
            return onNewMessageCalled;
        }

        public boolean isOnConnectionCloseCalled() {
            return onConnectionCloseCalled;
        }

        public List<Message> getLastMessages() {
            return lastMessages;
        }
    }

    @Before
    public void setup() {
        spyListener = new SpyListener();
        beatsHandler = new BeatsHandler(spyListener);

        Message message1 = new Message(1, new HashMap());
        Message message2 = new Message(2, new HashMap());

        batch = new Batch();
        batch.setWindowSize(2);
        batch.setProtocol(Protocol.VERSION_1);
        batch.addMessage(message1);
        batch.addMessage(message2);

    }

    @Test
    public void TestItCalledOnNewConnectionOnListenerWhenHandlerIsAdded() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new BeatsHandler(spyListener));
        embeddedChannel.writeInbound(batch);

        assertTrue(spyListener.isOnNewConnectionCalled());
        embeddedChannel.close();
    }

    @Test
    public void TestItCalledOnConnectionCloseOnListenerWhenChannelIsRemoved() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new BeatsHandler(spyListener));
        embeddedChannel.writeInbound(batch);
        embeddedChannel.close();

        assertTrue(spyListener.isOnConnectionCloseCalled());
    }

    @Test
    public void TestIsCallingNewMessageOnEveryMessage() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new BeatsHandler(spyListener));
        embeddedChannel.writeInbound(batch);


        assertEquals(2, spyListener.getLastMessages().size());
        embeddedChannel.close();
    }

    @Test
    public void TestItCreateAckMessages() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new BeatsHandler(spyListener));
        embeddedChannel.writeInbound(batch);

        AckMessage ack = (AckMessage) embeddedChannel.readInbound();
        assertEquals(1, ack.getSequence());
/*
        ack = (AckMessage) embeddedChannel.readInbound();
        assertEquals(2, ack.getSequence());
*/
        embeddedChannel.close();
    }
}