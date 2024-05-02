package utils;

import com.vnetpublishing.java.suapp.SuperUserApplication;

public class TestAdmin extends SuperUserApplication {

    public int run(final String[] args) {
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ignored) {}
        return -1;
    }
}
