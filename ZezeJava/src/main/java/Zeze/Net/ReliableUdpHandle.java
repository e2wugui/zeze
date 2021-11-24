package Zeze.Net;

@FunctionalInterface
public interface ReliableUdpHandle {
    void handle(ReliableUdp.Session session, ReliableUdp.Packet packet) throws Throwable;
}
