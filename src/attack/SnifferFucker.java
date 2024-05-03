package attack;

import utils.ColorsUtils;
import utils.LoggingUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SnifferFucker extends LoggingUtils {

    public SnifferFucker() throws IOException, InterruptedException {

        System.out.println(RED + "[WARNING] !!! /!\\ !!! [WARNING] ATTENTION REQUIRED [WARNING] !!! /!\\ !!! [WARNING]");
        System.out.println(RED_BRIGHT + "   This mode is purely experimental and could lead to falses or network degradation, proceed with caution!");
        System.out.println(RED_BRIGHT + "   You need to run this mode in a shell with administrator rights and have promiscuous mode enabled on your NIC!");
        System.out.println(RED_BRIGHT + "   Your ARP cache WILL BE POISONED !!!");
        System.out.println(RED_BRIGHT + "   This will create large waves of echo pings across the network, make sure your allowed to do this!");
        System.out.println(RED_BRIGHT + "   Using a separate NIC is highly recommended.");
        System.out.println(RED + "[WARNING] !!! /!\\ !!! [WARNING] ATTENTION REQUIRED [WARNING] !!! /!\\ !!! [WARNING]");

        Thread.sleep(5000);

        // Prepare to enumerate net interfaces
        final List<String> networkInterfaces = new ArrayList<>();
        final List<String> networkInterfacesIPs = new ArrayList<>();
        info("Running network interfaces lookup...");

        // Grab and list net interfaces
        final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        // Loop through all interfaces and add them
        int interfacesSize = 0;
        while(interfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.getInterfaceAddresses().isEmpty()) {
                networkInterfaces.add(
                        "    | #" + networkInterfaces.size() + " | '" + networkInterface.getDisplayName() + "' | '" + networkInterface.getInterfaceAddresses().get(0) + "'");
                interfacesSize++;
            }
        }

        if (interfacesSize == 0) {
            exitError("No network interface detected, are they enabled?", 52);
            return;
        } else info("Found " + interfacesSize + " network interfaces.");

        for (int i = 0; i < networkInterfaces.size(); i++) {
            networkInterfacesIPs.add(networkInterfaces.get(i).replaceAll(".*?(?<='/)", "").replaceAll("\\[.*", "").replaceAll("/.*", ""));
        }

        // Print net interfaces
        networkInterfaces.forEach(System.out::println);

        // Ask which net interface to use (according to the arraylist index)
        final int interfaceID = Integer.parseInt(getUserInput(
                "Please select the network interface's ID below:"));

        final FileWriter getNameFileWriter = new FileWriter("getName.bat");
        getNameFileWriter.write("@echo off\n" +
                "setlocal\n" +
                "setlocal enabledelayedexpansion\n" +
                "set \"_adapter=\"\n" +
                "set \"_ip=\"\n" +
                "for /f \"tokens=1* delims=:\" %%g in ('ipconfig /all') do (\n" +
                "  set \"_tmp=%%~g\"\n" +
                "  if \"!_tmp:adapter=!\"==\"!_tmp!\" (\n" +
                "    if not \"!_tmp:IPv4 Address=!\"==\"!_tmp!\" (\n" +
                "      for %%i in (%%~h) do (\n" +
                "      if not \"%%~i\"==\"\" set \"_ip=%%~i\"\n" +
                "      )\n" +
                "    set \"_ip=!_ip:(Preferred)=!\"\n" +
                "    if \"!_ip!\"==\"%1\" (\n" +
                "        @echo !_adapter!\n" +
                "      )\n" +
                "    )\n" +
                "  ) else (\n" +
                "    set \"_ip=\"\n" +
                "    set \"_adapter=!_tmp:*adapter =!\"\n" +
                "  )\n" +
                ")\n" +
                "endlocal");
        getNameFileWriter.close();

        // Extract the IP from the interface name + IP output
        final String interfaceIP = networkInterfaces.get(interfaceID).replaceAll(".*?(?<='/)", "").replaceAll("\\[.*", "").replaceAll("/.*", "");

        final String interfaceName = execCmdOut("getName.bat " + interfaceIP).replace("\n", "").replace("\r", "");

        new File("getName.bat").delete();

        log("clearing arp cache...");

        execCmd("netsh interface IP delete arpcache");

        log("arp cache cleared!");

        log("preparing new arp cache...");

        for (int i = 0; i <= 254; i++) {
            execCmd("netsh interface ip add neighbors \"" + interfaceName + "\" 192.168.1." + i + " FF-00-00-00-00-00");
        }

        log("arp cache ready!");

        log("enabling promiscuous mode...");

        final FileWriter promiscuousModeFileWriter = new FileWriter("promiscuous.ps1");
        promiscuousModeFileWriter.write("$byteIn = New-Object Byte[] 4\n" +
                "$byteOut = New-Object Byte[] 4\n" +
                "$byteData = New-Object Byte[] 4096\n" +
                "$byteIn[0] = 1\n" +
                "$byteIn[1-3] = 0\n" +
                "$byteOut[0-3] = 0\n" +
                "$Socket = New-Object System.Net.Sockets.Socket([Net.Sockets.AddressFamily]::InterNetwork, [Net.Sockets.SocketType]::Raw, [Net.Sockets.ProtocolType]::IP)\n" +
                "$Socket.SetSocketOption(\"IP\", \"HeaderIncluded\", $true)\n" +
                "$Socket.ReceiveBufferSize = 512000\n" +
                "$Endpoint = New-Object System.Net.IPEndpoint([Net.IPAddress]\"" + interfaceIP + "\", 0)\n" +
                "$Socket.Bind($Endpoint)\n" +
                "[void]$Socket.IOControl([Net.Sockets.IOControlCode]::ReceiveAll, $byteIn, $byteOut)");
        promiscuousModeFileWriter.close();

        execCmd("powershell.exe -ExecutionPolicy Bypass -File promiscuous.ps1");

        new File("promiscuous.ps1").delete();

        log("promiscuous state of nic: " + execCmdOut("powershell.exe -ExecutionPolicy Bypass -Command echo ($(Get-NetAdapter -Name '" + interfaceName + "').PromiscuousMode)"));

        info("Checking LAN for sniffers...");

        while (true) {
            final ExecutorService executorService = Executors.newFixedThreadPool(256);
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i <= 254; i++) {

                // Prevent checking ourselves because it will say that we are sniffing
                if (!networkInterfacesIPs.contains("192.168.1." + i)) {

                    int finalI = i;
                    final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            if (InetAddress.getByName("192.168.1." + finalI).isReachable(NetworkInterface.getByName(interfaceIP), 64, 3000) && checkPort("192.168.1." + finalI, 135) && checkPort("192.168.1." + finalI, 139) && checkPort("192.168.1." + finalI, 445)) {
                                info("SNIFFER DETECTED! '192.168.1." + finalI + "' is sniffing packets!");
                            }
                        } catch (final IOException ignored) {
                        }
                    }, executorService);
                    futures.add(future);
                }
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdownNow();
        }

    }

    public static String execCmdOut(final String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void execCmd(final String cmd) throws java.io.IOException {
        Runtime.getRuntime().exec(cmd);
    }

    public static boolean checkPort(final String ipv4, final int port) {

        try {
            new Socket().connect(new InetSocketAddress(ipv4, port), 1000);
            return true;
        } catch (final IOException ignored) {
            return false;
        }
    }
}
