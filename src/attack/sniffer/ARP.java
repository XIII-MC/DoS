package attack.sniffer;

import attack.WiresharkFlood;
import utils.LoggingUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ARP extends LoggingUtils {

    public ARP() throws IOException, InterruptedException {

        System.out.println(RED + "/!\\ ATTENTION REQUIRED /!\\");
        System.out.println(RED_BRIGHT + "   This mode will erase and poison the ARP cache of one of your network interfaces,");
        System.out.println(RED_BRIGHT + "   your network interface will be put in promiscuous mode,");
        System.out.println(RED_BRIGHT + "   large waves of echo pings will be sent across the network to find sniffers.");
        System.out.println(RED_BRIGHT + "   Your internet and LAN access could get interrupted while using this!");
        System.out.println(" ");
        System.out.println(RED_BRIGHT + "   Everything will be put back together when your done. (ARP & Promiscuous)");
        System.out.println(" ");
        System.out.println(RED_BRIGHT + "   Use with caution and make sure your allowed to do this!");
        System.out.println(RED + "/!\\ ATTENTION REQUIRED /!\\");

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

        final String interfaceName = (execCmdOut("getName.bat " + interfaceIP)).replace("\n", "").replace("\r", "");

        new File("getName.bat").delete();

        final int vlBuffer = Integer.parseInt(getUserInput("After how many flags should we display the alert ? (buffer): "));

        final int vlCapFlood = Integer.parseInt(getUserInput("At which VL should we flood the sniffer ? (-1 to disable, 20 recommended): "));

        log("clearing arp cache...");

        execCmd("netsh interface IP delete arpcache");

        log("arp cache cleared!");

        log("preparing new arp cache...");

        for (int i = 0; i <= 254; i++) {
            execCmd("netsh interface ip add neighbors \"" + interfaceName + "\" 192.168.1." + i + " FF-00-00-00-00-00");
        }

        log("arp cache ready!");

        log("enabling promiscuous mode...");

        long pid = ProcessHandle.current().pid();

        final FileWriter promiscuousModeFileWriter = new FileWriter("promiscuous.ps1");
        promiscuousModeFileWriter.write("$(Get-NetAdapter -Name \"" + interfaceName + "\").PromiscuousMode\n" +
                "$byteIn = New-Object Byte[] 4\n" +
                "$byteOut = New-Object Byte[] 4\n" +
                "$byteData = New-Object Byte[] 4096\n" +
                "$byteIn[0] = 1\n" +
                "$byteIn[1-3] = 0\n" +
                "$byteOut[0-3] = 0\n" +
                "$Socket = New-Object System.Net.Sockets.Socket([Net.Sockets.AddressFamily]::InterNetwork, [Net.Sockets.SocketType]::Raw, [Net.Sockets.ProtocolType]::IP)\n" +
                "$Socket.SetSocketOption(\"IP\", \"HeaderIncluded\", $true)\n" +
                "$Socket.ReceiveBufferSize = 8192\n" +
                "$Endpoint = New-Object System.Net.IPEndpoint([Net.IPAddress]\"" + interfaceIP + "\", 0)\n" +
                "$Socket.Bind($Endpoint)\n" +
                "[void]$Socket.IOControl([Net.Sockets.IOControlCode]::ReceiveAll, $byteIn, $byteOut)\n" +
                "$(Get-NetAdapter -Name \"" + interfaceName + "\").PromiscuousMode\n" +
                "Wait-Process -Id " + pid + "\n" +
                "netsh interface IP delete arpcache\n" +
                "taskkill /F /PID ([System.Diagnostics.Process]::GetCurrentProcess().Id)");
        promiscuousModeFileWriter.close();

        new ProcessBuilder(
                "powershell.exe",
                "Start-Process powershell.exe -WindowStyle hidden '-NoExit \"[Console]::Title = ''PromiscuousMode''; .\\promiscuous.ps1\"'"
        ).start();

        Thread.sleep(2000);

        new File("promiscuous.ps1").delete();

        log("promiscuous state of nic: " + execCmdOut("powershell.exe -ExecutionPolicy Bypass -Command echo ($(Get-NetAdapter -Name '" + interfaceName + "').PromiscuousMode)"));

        info("Checking LAN for sniffers...");

        // K = IP | V = VL
        final HashMap<String, Integer> violations = new HashMap<>();
        final HashMap<String, Thread> floodThreads = new HashMap<>();

        while (true) {
            final ExecutorService executorService = Executors.newFixedThreadPool(256);
            final List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i <= 254; i++) {

                // Prevent checking ourselves because it will say that we are sniffing
                if (!networkInterfacesIPs.contains("192.168.1." + i)) {

                    violations.putIfAbsent("192.168.1." + i, 0);
                    floodThreads.putIfAbsent("192.168.1." + i, null);

                    int finalI = i;
                    final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            final long delayPing =  System.currentTimeMillis();

                            if (InetAddress.getByName("192.168.1." + finalI).isReachable(NetworkInterface.getByName(interfaceIP), 128, 5000) && checkPort("192.168.1." + finalI, 135) && checkPort("192.168.1." + finalI, 139) && checkPort("192.168.1." + finalI, 445) && !checkPort("192.168.1." + finalI, 53)) {
                                violations.put("192.168.1." + finalI, violations.get("192.168.1." + finalI) + 1);

                                if (violations.get("192.168.1." + finalI) >= vlBuffer) {
                                    alert_high("192.168.1." + finalI + " has answered an invalid packet!" + "\n" + BLACK_BRIGHT + ITALIC + "||||| CHECK_TYPE=ARP, FLAG=IMPOSSIBLE_RESPONSE, DEST_NIC=PROMISCUOUS, DEST_CLIENT=WINDOWS(135+139+445), VL=" + violations.get("192.168.1." + finalI) + ", LATENCY=" + (System.currentTimeMillis() - delayPing) + "ms, TTL=64, TIMEOUT=3000 @ " + new SimpleDateFormat("yyyy/dd/MM HH:mm:ss").format(Calendar.getInstance().getTime()));
                                }

                                // WireShark flood sniffer
                                if (vlCapFlood != -1 && violations.get("192.168.1." + finalI) >= vlCapFlood && floodThreads.get("192.168.1." + finalI) == null) {
                                    final Thread floodThread = new Thread(() -> {
                                        try {
                                            new WiresharkFlood("192.168.1." + finalI, 0);
                                        } catch (IOException | InterruptedException ignored) {}
                                    });
                                    floodThreads.put("192.168.1." + finalI, floodThread);
                                    floodThread.start();
                                }

                            } else if (violations.get("192.168.1." + finalI) > 0) {
                                if (violations.get("192.168.1." + finalI) >= vlBuffer) {
                                    alert_medium("192.168.1." + finalI + " has suspectedly stopped answering." + "\n" + BLACK_BRIGHT + ITALIC + "||||| CHECK_TYPE=ARP, FLAG=PROPER_RESPONSE_AFTER_IMPOSSIBLE_RESPONSE, DEST_NIC=NORMAL, DEST_CLIENT=UNKNOWN, VL=" + violations.get("192.168.1." + finalI) + ", LATENCY=" + (System.currentTimeMillis() - delayPing) + "ms, TTL=64, TIMEOUT=30000 @ " + new SimpleDateFormat("yyyy/dd/MM HH:mm:ss").format(Calendar.getInstance().getTime()));
                                }

                                violations.put("192.168.1." + finalI, 0);
                                if (vlCapFlood != -1 && floodThreads.get("192.168.1." + finalI) != null) {
                                    floodThreads.get("192.168.1." + finalI).interrupt();
                                    floodThreads.remove("192.168.1." + finalI);
                                }
                            }
                        } catch (final IOException ignored) {}
                    }, executorService);
                    futures.add(future);
                }

                Thread.sleep(10000);
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
