package io.cloudsoft.socialapps.mongodb;

import static java.util.Arrays.asList;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.basic.LocationRegistry;

/**
 * starts an Mongodb Server in AWS.
 */
public class MyMongodbApp extends AbstractApplication {

    private Mongodb mongodb;

    public MyMongodbApp() {
        mongodb = new Mongodb(this);
   }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] args) {
        MyMongodbApp app = new MyMongodbApp();
        BrooklynLauncher.manage(app, 8081);

        //todo: we need to get a debian image.
        app.start(new LocationRegistry().getLocationsById(asList("aws-ec2:us-east-1")));
    }
}