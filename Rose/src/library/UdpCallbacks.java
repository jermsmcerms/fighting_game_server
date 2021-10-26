package library;

import java.net.SocketAddress;

import network.UdpMsg;

public interface UdpCallbacks {
    void onMsg(SocketAddress from, UdpMsg msg);
}
