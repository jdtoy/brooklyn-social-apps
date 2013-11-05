package io.cloudsoft.socialapps.thinkup;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(ThinkUpImpl.class)
public interface ThinkUp extends SoftwareProcess, WebAppService, HasShortName {

    @SetFromFlag("version")
    BasicConfigKey<String> SUGGESTED_VERSION = new BasicConfigKey<String>(
    		SoftwareProcess.SUGGESTED_VERSION, "v2.0-beta.8");

    public static final BasicConfigKey<Boolean> DATABASE_UP = new BasicConfigKey<Boolean>(
            Boolean.class, "database.up", "", true);

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "https://github.com/ginatrapani/ThinkUp/archive/${version}.tar.gz");

    @SetFromFlag("templateConfigurationUrl")
    ConfigKey<String> TEMPLATE_CONFIGURATION_URL = new BasicConfigKey<String>(
            String.class, "ThinkUp.ConfigurationUrl", "Template file (in freemarker format) for the config.inc.php file",
            "classpath://io/cloudsoft/socialapps/thinkup/config.inc.php");

    @SetFromFlag("templateCustomInstallPhpUrl")
    ConfigKey<String> TEMPLATE_CUSTOM_INSTALL_PHP_URL = new BasicConfigKey<String>(
            String.class, "ThinkUp.templateCustomInstallPhp", "Template file (in freemarker format) for the custom-install.php file",
            "classpath://io/cloudsoft/socialapps/thinkup/custom-install.php");

    @SetFromFlag("databaseName")
    ConfigKey<String> DATABASE_NAME = new BasicConfigKey<String>(
            String.class, "ThinkUp.databaseName", "name of the database for ThinkUp", null);

    @SetFromFlag("databaseUser")
    ConfigKey<String> DATABASE_USER = new BasicConfigKey<String>(
            String.class, "ThinkUp.databaseUser", "MySql database username", null);

    @SetFromFlag("databasePassword")
    ConfigKey<String> DATABASE_PASSWORD = new BasicConfigKey<String>(
            String.class, "ThinkUp.databasePassword", "MySql database password", null);

    @SetFromFlag("databaseHostname")
    ConfigKey<String> DATABASE_HOSTNAME = new BasicConfigKey<String>(
            String.class, "ThinkUp.databaseHostname", "MySql hostname", null);
    
    @SetFromFlag("adminEmail")
    ConfigKey<String> THINKUP_ADMIN_EMAIL = new BasicConfigKey<String>(
            String.class, "ThinkUp.adminEmail", "E-mail address for the admin user (default to empty)", "");

    @SetFromFlag("adminPassword")
    // TODO would be nice if empty password causes auto-gen
            ConfigKey<String> THINKUP_ADMIN_PASSWORD = new BasicConfigKey<String>(
            String.class, "ThinkUp.adminPassword", "Password for the admin user (defaults to 'password')", "password");

    @SetFromFlag("appTitlePrefix")
    ConfigKey<String> APP_TITLE_PREFIX = new BasicConfigKey<String>(
            String.class, "ThinkUp.appTitlePrefix", "Application title. e.g. for 'Jane Smith's ThinkUp', set this value to 'Jane Smith's '", "Brooklyn's ");

    @SetFromFlag("timeZone")
    ConfigKey<String> THINKUP_TIMEZONE = new BasicConfigKey<String>(
            String.class, "ThinkUp.timeZone", "Timezone", "Europe/London");



    @SetFromFlag("httpPort")
    PortAttributeSensorAndConfigKey HTTP_PORT = new PortAttributeSensorAndConfigKey(Attributes.HTTP_PORT, "80");
}
