package attack;

import utils.LoggingUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SnifferFucker extends LoggingUtils {

    public SnifferFucker() throws IOException, InterruptedException {

        final String address = getUserInput("What is the IP of the interface to use? (e.g 192.168.1.1):");

        final String inet = getUserInput("What's the name of the interface ? (netsh interface show interface):");

        log("clearing arp cache...");

        execCmd("netsh interface IP delete arpcache");

        log("arp cache cleared!");

        log("preparing new arp cache...");

        for (int i = 0; i < 254; i++) {
            execCmd("netsh interface ip add neighbors " + inet + " 192.168.1." + i + " FF-00-00-00-00-00");
        }

        log("arp cache ready!");

        info("Checking LAN for sniffers...");

        int nbPasses = 0;
        AtomicInteger sniffersPass = new AtomicInteger();
        AtomicInteger sniffersTotal = new AtomicInteger();

        while (true) {
            final ExecutorService executorService = Executors.newFixedThreadPool(256);
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 254; i++) {

                // Prevent checking ourselves because it will say that we are sniffing
                if (!("192.168.1." + i).equals(address)) {

                    int finalI = i;
                    final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            if (InetAddress.getByName("192.168.1." + finalI).isReachable(NetworkInterface.getByName(address), 64, 3000)) {
                                info("SNIFFER DETECTED! '192.168.1." + finalI + "' is sniffing packets!");
                                sniffersTotal.getAndIncrement();
                                sniffersPass.getAndIncrement();
                            }
                        } catch (final IOException ignored) {
                        }
                    }, executorService);
                    futures.add(future);
                }
            }
            nbPasses++;
            log("pass " + nbPasses + " done, found " + sniffersPass.get() + " sniffers (" + sniffersTotal.get() + " total)");
            sniffersPass.set(0);

            Thread.sleep(3000);
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdownNow();
        }

    }

    public static String execCmd(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
