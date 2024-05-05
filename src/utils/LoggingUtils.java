package utils;

import java.util.Scanner;

public class LoggingUtils extends ColorsUtils {

    public static boolean enableLog = true;

    public static void init() {
        if (getUserInput("Enable logging? This will show everything happening, like the log message you saw above and below. (Y/N)").toLowerCase().contains("y")) {
            enableLog = true;
            log("logging enabled!");
        } else enableLog = false;
    }

    public static String getUserInput(final String text) {
        final Scanner userInputScanner = new Scanner(System.in);
        System.out.println(YELLOW_BOLD + "[INP]" + RESET + " " + text + RESET);
        log("waiting user feedback...");
        return userInputScanner.nextLine();
    }

    public static void exitSuccess(final String text) {
        System.out.println(GREEN_BOLD + "[SUC]" + RESET + GREEN_BRIGHT + " " + text + RESET);
        log("program ran perfectly, exiting with code 0");
        System.exit(0);
    }

    public static void exitError(final String text, final int exitCode) {
        System.out.println(RED_BOLD + "[ERR]" + RESET + RED_BRIGHT + " " + text + RESET);
        log("program ran into an error, exiting with code " + exitCode);
        System.exit(exitCode);
    }

    public static void log(final String text) {
        if (enableLog) System.out.println(BLACK_BRIGHT + ITALIC + "[log] " + text + RESET);
    }

    public static void info(final String text) {
        System.out.println(CYAN_BOLD + "[INF] " + RESET + text + RESET);
    }

    public static void alert_high(final String text) {
        System.out.println(RED_BOLD_BRIGHT + "[ALE] " + RESET + text + RESET);
    }

    public static void alert_medium(final String text) {
        System.out.println(YELLOW_BOLD + "[ALE] " + RESET + text + RESET);
    }
}
