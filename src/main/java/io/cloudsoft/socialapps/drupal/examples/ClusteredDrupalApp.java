package io.cloudsoft.socialapps.drupal.examples;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.EntityFactory;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.webapp.DynamicWebAppCluster;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.basic.LocationRegistry;
import brooklyn.util.MutableMap;
import io.cloudsoft.socialapps.drupal.Drupal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static java.util.Arrays.asList;

/**
 * This example Application starts up a Scalable drupal environment.
 * <p/>
 * TODO: This is work in progres..
 * <p/>
 * <p/>
 * http://www.johnandcailin.com/blog/john/scaling-drupal-open-source-infrastructure-high-traffic-drupal-sites
 */
public class ClusteredDrupalApp extends AbstractApplication {

    public static final Logger log = LoggerFactory.getLogger(BasicDrupalApp.class);

    private final static String SCRIPT = "create database drupal; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'%'  IDENTIFIED BY 'password'; " +
            "FLUSH PRIVILEGES;";

    private final MySqlNode mySqlNode;
    private final DynamicWebAppCluster cluster;
    private final NginxController loadBalancer;

    public ClusteredDrupalApp() {
        Map mysqlConf = MutableMap.of("creationScriptContents", SCRIPT);
        mySqlNode = new MySqlNode(mysqlConf, this);

        Map clusterProps = MutableMap.of("factory", new DrupalFactory(), "initialSize", 1);
        cluster = new DynamicWebAppCluster(clusterProps, this);

        Map nginxProperties = MutableMap.of("serverPool", cluster, "domain", "localhost", "port", "80");
        loadBalancer = new NginxController(nginxProperties, this);
    }

    private class DrupalFactory implements EntityFactory {
        @Override
        public Entity newEntity(Map flags, Entity owner) {
            Drupal drupal = new Drupal(cluster);
            drupal.setConfig(Drupal.DATABASE_UP, attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP));
            drupal.setConfig(Drupal.DATABASE_HOST, attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME));
            drupal.setConfig(Drupal.DATABASE_PORT, attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT));
            drupal.setConfig(Drupal.DATABASE_SCHEMA, "drupal");
            drupal.setConfig(Drupal.DATABASE_USER, "drupal");
            drupal.setConfig(Drupal.DATABASE_PASSWORD, "password");
            drupal.setConfig(Drupal.ADMIN_EMAIL, "foo@bar.com");
            return drupal;
        }
    }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] args) throws Exception {
        ClusteredDrupalApp app = new ClusteredDrupalApp();
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