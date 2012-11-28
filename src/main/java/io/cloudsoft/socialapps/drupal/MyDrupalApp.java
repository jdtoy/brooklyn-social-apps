package io.cloudsoft.socialapps.drupal;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.basic.LocationRegistry;
import brooklyn.util.MutableMap;

import java.util.Map;

import static java.util.Arrays.asList;

/**
 * starts an Drupal Server and MySQL server in AWS.
 */
public class MyDrupalApp extends AbstractApplication {
    private final static String SCRIPT = "create database drupal; " +
            "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES ON drupal.* TO 'drupal'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "FLUSH PRIVILEGES;";

    private Drupal drupal;
    private MySqlNode mySqlNode;

    public MyDrupalApp() {
        drupal = new Drupal(this);
        Map mysqlConf = MutableMap.of("creationScriptContents", SCRIPT);
        mySqlNode = new MySqlNode(mysqlConf, this);

        drupal.setConfig(Drupal.DATABASE_HOST, "127.0.0.1");
        drupal.setConfig(Drupal.DATABASE_SCHEMA, "drupal");
        drupal.setConfig(Drupal.DATABASE_USER, "drupal");
        drupal.setConfig(Drupal.DATABASE_PORT, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT));
        drupal.setConfig(Drupal.DATABASE_PASSWORD, "password");
        drupal.setConfig(Drupal.ADMIN_EMAIL, "alarmnummer@gmail.com");
        drupal.setConfig(Drupal.DATABASE_UP, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP));
    }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] args) {
        MyDrupalApp app = new MyDrupalApp();
        BrooklynLauncher.manage(app, 8081);

        //todo: we need to get a debian image.
        app.start(new LocationRegistry().getLocationsById(asList("aws-ec2:us-east-1")));
    }
}