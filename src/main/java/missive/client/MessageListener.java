package missive.client;

import missive.common.protocol.Packet;

public interface MessageListener {
    void onPacketReceived(Packet packet);
    void onConnectionLost();
}
