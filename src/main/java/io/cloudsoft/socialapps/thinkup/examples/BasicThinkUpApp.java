package io.cloudsoft.socialapps.thinkup.examples;

import io.cloudsoft.socialapps.thinkup.ThinkUp;

import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.Lists;

@Catalog(name="Simple ThinkUp",
        description="ThinkUp, social media insights engine.",
        iconUrl="http://brettterpstra.com/uploads/2012/02/ThinkUpIcon2.png")
public class BasicThinkUpApp extends AbstractApplication {

    public static final Logger log = LoggerFactory.getLogger(BasicThinkUpApp.class);

    @CatalogConfig(label="Email", priority=2)
    public static final ConfigKey<String> EMAIL = ConfigKeys.newConfigKey(
            "ThinkUp.adminEmail", "Admin Email Address", "your_email@your_domain_set_in_brooklyn");

    @CatalogConfig(label="Password", priority=3)
    public static final ConfigKey<String> PASSWORD = ConfigKeys.newConfigKey(
            "ThinkUp.adminPassword", "Admin Password", "isATopSecret");

    @CatalogConfig(label="ThinkUp Apps's Title", priority=1)
    public static final ConfigKey<String> THINKUP_TITLE = ConfigKeys.newConfigKey(
            "ThinkUp.appTitlePrefix", "ThinkUp Apps's Title", "Brooklyn's ");

    @CatalogConfig(label="Timezone", priority=4)
    public static final ConfigKey<String> THINKUP_TIMEZONE = ConfigKeys.newConfigKey(
            "ThinkUp.timeZone", "Timezone", "Europe/London");


    final static String SCRIPT = "create database thinkup; " +
            "grant all privileges on thinkup.* TO 'www-data'@'localhost'  IDENTIFIED BY 'password'; " +
            "grant all privileges on thinkup.* TO 'www-data'@'127.0.0.1'  IDENTIFIED BY 'password'; " +
            "grant all privileges on thinkup.* TO 'www-data'@'%'  IDENTIFIED BY 'password';" +
            "flush privileges;";
    
    private MySqlNode mysql;
    private ThinkUp thinkup;

    @Override
    public void init() {
        mysql = addChild(BasicEntitySpec.newInstance(MySqlNode.class)
                .configure("creationScriptContents", SCRIPT));

        thinkup = addChild(BasicEntitySpec.newInstance(ThinkUp.class)
                .configure(ThinkUp.DATABASE_UP, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.SERVICE_UP))
                .configure(ThinkUp.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mysql, MySqlNode.HOSTNAME))
                .configure(ThinkUp.DATABASE_NAME, "thinkup")
                .configure(ThinkUp.DATABASE_USER, "www-data")
                .configure(ThinkUp.DATABASE_PASSWORD, "password")
                .configure(ThinkUp.APP_TITLE_PREFIX, getConfig(THINKUP_TITLE))
                .configure(ThinkUp.THINKUP_ADMIN_EMAIL, getConfig(EMAIL))
                .configure(ThinkUp.THINKUP_ADMIN_PASSWORD, getConfig(PASSWORD))
                .configure(ThinkUp.THINKUP_TIMEZONE, getConfig(THINKUP_TIMEZONE))
                );
    }

    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "aws-ec2:eu-west-1");

        // Image: {id=us-east-1/ami-7d7bfc14, providerId=ami-7d7bfc14, name=RightImage_CentOS_6.3_x64_v5.8.8.5, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=centos, arch=paravirtual, version=6.0, description=rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.5.manifest.xml, is64Bit=true}, description=rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.5.manifest.xml, version=5.8.8.5, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        // TODO Set for only us-east-1 region, rather than all aws-ec2
        BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
//        brooklynProperties.put("brooklyn.jclouds.aws-ec2.image-id", "us-east-1/ami-7ce17315");
//        brooklynProperties.put("brooklyn.jclouds.cloudservers-uk.image-name-regex", "CentOS 6.0");
//        brooklynProperties.remove("brooklyn.jclouds.cloudservers-uk.image-id");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .brooklynProperties(brooklynProperties)
                .application(EntitySpecs.appSpec(BasicThinkUpApp.class)
                        .displayName("ThinkUp app"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
