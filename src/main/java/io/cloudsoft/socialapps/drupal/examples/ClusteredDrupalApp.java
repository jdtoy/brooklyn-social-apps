package io.cloudsoft.socialapps.drupal.examples;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.basic.LocationRegistry;
import brooklyn.util.MutableMap;
import io.cloudsoft.socialapps.drupal.Drupal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.util.Arrays.asList;

/**
 * This example Application starts up a Scalable drupal environment.
 * <p/>
 * http://www.johnandcailin.com/blog/john/scaling-drupal-open-source-infrastructure-high-traffic-drupal-sites
 */
public class ClusteredDrupalApp extends AbstractApplication {

    public static final Logger log = LoggerFactory.getLogger(BasicDrupalApp.class);

    private final static String SCRIPT = "create database drupal; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "FLUSH PRIVILEGES;";

    private Drupal drupal;
    private MySqlNode mySqlNode;

    public ClusteredDrupalApp() {
        Map mysqlConf = MutableMap.of("creationScriptContents", SCRIPT);
        mySqlNode = new MySqlNode(mysqlConf, this);

        drupal = new Drupal(this);
        drupal.setConfig(Drupal.DATABASE_HOST, "127.0.0.1");
        drupal.setConfig(Drupal.DATABASE_SCHEMA, "drupal");
        drupal.setConfig(Drupal.DATABASE_USER, "drupal");
        drupal.setConfig(Drupal.DATABASE_PORT, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT));
        drupal.setConfig(Drupal.DATABASE_PASSWORD, "password");
        drupal.setConfig(Drupal.ADMIN_EMAIL, "foo@bar.com");
        drupal.setConfig(Drupal.DATABASE_UP, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP));
    }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] args) throws Exception {
        BasicDrupalApp app = new BasicDrupalApp();
        BrooklynLauncher.manage(app, 8081);

        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
        //brooklynProperties.put("brooklyn.jclouds.aws-ec2.image-name-regex","ubuntu-oneiric");
        brooklynProperties.put("brooklyn.jclouds.cloudservers-uk.image-name-regex", "Debian");
        brooklynProperties.remove("brooklyn.jclouds.cloudservers-uk.image-id");
        LocationRegistry locationRegistry = new LocationRegistry(brooklynProperties);

        log.info("Starting BasicDrupalApp");
        app.start(asList(locationRegistry.resolve("cloudservers-uk")));
        log.info("Finished creating BasicDrupalApp");
    }
}