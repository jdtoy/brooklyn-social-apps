package io.cloudsoft.socialapps.mongodb;

import groovy.time.TimeDuration;
import io.cloudsoft.socialapps.drupal.DrupalDriver;

import java.util.LinkedHashMap;
import java.util.Map;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.SoftwareProcessEntity;
import brooklyn.event.adapter.FunctionSensorAdapter;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.MutableMap;
import brooklyn.util.flags.SetFromFlag;

public class Mongodb extends SoftwareProcessEntity {

    @SetFromFlag("version")
    public static final BasicConfigKey<String> SUGGESTED_VERSION =
            new BasicConfigKey<String>(SoftwareProcessEntity.SUGGESTED_VERSION, "7.17");

    public Mongodb(Map flags) {
        this(flags, null);
    }

    public Mongodb(Entity owner) {
        this(new LinkedHashMap(), owner);
    }

    public Mongodb(Map flags, Entity owner) {
        super(flags, owner);
    }

    @Override
    public Class getDriverInterface() {
        return DrupalDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        FunctionSensorAdapter serviceUpAdapter = sensorRegistry.register(new ServiceUpSensorAdapter());
        serviceUpAdapter.poll(SERVICE_UP);
    }

    private class ServiceUpSensorAdapter extends FunctionSensorAdapter {

        public ServiceUpSensorAdapter() {
            //we want to scan every 10 seconds.
            super(MutableMap.of("period", new TimeDuration(0, 0, 10, 0)));
        }

        @Override
        public Object call() {
            return getDriver().isRunning();
        }
    }
}