package io.cloudsoft.socialapps.wordpress;


import java.util.Arrays;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import brooklyn.enricher.basic.SensorPropagatingEnricher;
import brooklyn.entity.AbstractEc2LiveTest;
import brooklyn.entity.Entity;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.Location;
import brooklyn.test.HttpTestUtils;

public class WordpressClusterEc2LiveTest extends AbstractEc2LiveTest {

    final static String SCRIPT = "create database wordpress; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on wordpress.* TO 'wordpress'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";
    
    private ControlledDynamicWebAppCluster cluster;

    @Override
    protected void doTest(Location loc) throws Exception {
        MySqlNode mysql = app.createAndManageChild(BasicEntitySpec.newInstance(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        cluster = app.createAndManageChild(BasicEntitySpec.newInstance(ControlledDynamicWebAppCluster.class)
                .configure(ControlledDynamicWebAppCluster.INITIAL_SIZE, 2)
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, BasicEntitySpec.newInstance(Wordpress.class)
                        .configure(Wordpress.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                        .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                        .configure(Wordpress.DATABASE_NAME, "wordpress")
                        .configure(Wordpress.DATABASE_USER, "wordpress")
                        .configure(Wordpress.DATABASE_PASSWORD, "password")
                        .configure(Wordpress.WEBLOG_TITLE, "my custom title")
                        .configure(Wordpress.WEBLOG_ADMIN_EMAIL, "aled.sage@gmail.com")));
                        

        SensorPropagatingEnricher.newInstanceListeningTo(cluster, WebAppService.ROOT_URL).addToEntityAndEmitAll(cluster);

        app.start(Arrays.asList(loc));

        String rootUrl = cluster.getAttribute(Wordpress.ROOT_URL);
        HttpTestUtils.assertContentEventuallyContainsText(rootUrl, "my custom title");
        
        for (Entity wordpress : cluster.getCluster().getMembers()) {
            String wordpressUrl = wordpress.getAttribute(Wordpress.ROOT_URL);
            HttpTestUtils.assertContentEventuallyContainsText(wordpressUrl, "my custom title");
        }
    }

    @Override
    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
//        super.tearDown(); // FIXME
        // no-op; leave it running for debugging
    }
    
    @Override
    @Test(groups = {"Live"})
    public void test_CentOS_6_3() throws Exception {
        super.test_CentOS_6_3();
    }

    @Test(enabled=false)
    public void testDummy() {} // Convince testng IDE integration that this really does have test methods  
}
