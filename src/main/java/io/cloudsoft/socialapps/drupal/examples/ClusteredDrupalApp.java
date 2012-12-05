package io.cloudsoft.socialapps.drupal.examples;

import brooklyn.config.BrooklynProperties;
import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.BasicConfigurableEntityFactory;
import brooklyn.entity.basic.ConfigurableEntityFactory;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.launcher.BrooklynServerDetails;
import brooklyn.location.Location;
import brooklyn.location.basic.jclouds.JcloudsLocation;
import brooklyn.util.CommandLineUtil;
import brooklyn.util.MutableMap;
import io.cloudsoft.socialapps.drupal.Drupal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static java.util.Arrays.asList;

/**
 * This example Application starts up a Scalable drupal environment.
 * <p/>
 * TODO: This is work in progress.
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
    private final ControlledDynamicWebAppCluster cluster;

    public ClusteredDrupalApp() {
        Map mysqlConf = MutableMap.of("creationScriptContents", SCRIPT);
        mySqlNode = new MySqlNode(mysqlConf, this);

        ConfigurableEntityFactory<Drupal> drupalFactory = new BasicConfigurableEntityFactory<Drupal>(Drupal.class);
        drupalFactory.setConfig(Drupal.DATABASE_UP, attributeWhenReady(mySqlNode, MySqlNode.SERVICE_UP));
        drupalFactory.setConfig(Drupal.DATABASE_HOST, attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME));
        drupalFactory.setConfig(Drupal.DATABASE_PORT, attributeWhenReady(mySqlNode, MySqlNode.MYSQL_PORT));
        drupalFactory.setConfig(Drupal.DATABASE_SCHEMA, "drupal");
        drupalFactory.setConfig(Drupal.DATABASE_USER, "drupal");
        drupalFactory.setConfig(Drupal.DATABASE_PASSWORD, "password");
        drupalFactory.setConfig(Drupal.ADMIN_EMAIL, "foo@bar.com");

        Map clusterProps = MutableMap.of("factory", drupalFactory, "initialSize", 2);
        cluster = new ControlledDynamicWebAppCluster(clusterProps, this);
        SensorPropagatingEnricher.newInstanceListeningTo(cluster, WebAppService.ROOT_URL).addToEntityAndEmitAll(this);
    }

    // can start in AWS by running this -- or use brooklyn CLI/REST for most clouds, or programmatic/config for set of fixed IP machines
    public static void main(String[] argv) throws Exception {
        ClusteredDrupalApp app = new ClusteredDrupalApp();
        List<String> args = new ArrayList<String>(asList(argv));
        BrooklynServerDetails server = BrooklynLauncher.newLauncher().
                webconsolePort(CommandLineUtil.getCommandLineOption(args, "--port", "8081+")).
                managing(app).
                launch();

        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
        //brooklynProperties.put("brooklyn.jclouds.aws-ec2.image-name-regex","ubuntu-oneiric");
        brooklynProperties.put("brooklyn.jclouds.cloudservers-uk.image-name-regex", "Debian");
        brooklynProperties.remove("brooklyn.jclouds.cloudservers-uk.image-id");
        brooklyn.location.basic.LocationRegistry locationRegistry = new brooklyn.location.basic.LocationRegistry(brooklynProperties);
        JcloudsLocation jcloudsLocation = (JcloudsLocation) locationRegistry.resolve("cloudservers-uk");

        //List<Location> locations = locationRegistry.resolve(!args.isEmpty() ? args : Arrays.asList("aws-ec2:us-east-1"));
        app.start(asList(jcloudsLocation));
    }
}