package uk.co.gidley.clockRadio.test;

import android.test.ActivityInstrumentationTestCase2;
import uk.co.gidley.clockRadio.*;

public class HelloAndroidActivityTest extends ActivityInstrumentationTestCase2<HelloAndroidActivity> {

    public HelloAndroidActivityTest() {
        super("uk.co.gidley.clockRadio", HelloAndroidActivity.class);
    }

    public void testActivity() {
        HelloAndroidActivity activity = getActivity();
        assertNotNull(activity);
    }
}

