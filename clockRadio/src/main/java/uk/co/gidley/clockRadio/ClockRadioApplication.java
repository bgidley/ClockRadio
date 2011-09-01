package uk.co.gidley.clockRadio;

import com.google.inject.Module;
import roboguice.application.RoboApplication;
import roboguice.config.RoboModule;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bgidley
 * Date: 01/09/2011
 * Time: 09:40
 */
public class ClockRadioApplication extends RoboApplication {

    @Override
    protected void addApplicationModules(List<Module> modules) {
        modules.add(new ClockRadioModule());
    }
}
