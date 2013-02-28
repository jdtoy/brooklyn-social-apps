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

    /*
     * FIXME Fails because nginx and wordpress don't play nicely together. It only works if nginx and wordpress are on the same machine:
     * 
     * Running this on the VM with nginx (with conf/server.conf pointing at wordpress servers, on port 80:
     *     $> wget http://localhost:8000
     *     
     *     --2013-02-28 11:30:55--  http://localhost:8000/
     *     Resolving localhost... 127.0.0.1
     *     Connecting to localhost|127.0.0.1|:8000... connected.
     *     HTTP request sent, awaiting response... 301 Moved Permanently
     *     Location: http://localhost/ [following]
     *     --2013-02-28 11:30:55--  http://localhost/
     *     Connecting to localhost|127.0.0.1|:80... failed: Connection refused.
     * 
     * Various blogs talk about setting up fastcgi_pass in the nginx config, but trying that manually
     * it still doesn't work if nginx and wordpress are on different machines.
     *     e.g. http://elasticdog.com/2008/02/howto-install-wordpress-on-nginx/
     * 
     * Fix is to include the following in the location info (see http://zeroturnaround.com/labs/wordpress-protips-go-with-a-clustered-approach/#!/):
     *     proxy_set_header Host $host;
     *     proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
     *     proxy_set_header X-Real-IP $remote_addr;
     */
    
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

    // Convenience for easily running just this one test from Eclipse
    @Override
    @Test(groups = {"Live"})
    public void test_CentOS_6_3() throws Exception {
        super.test_CentOS_6_3();
    }

    @Test(enabled=false)
    public void testDummy() {} // Convince testng IDE integration that this really does have test methods  
}
