package io.cloudsoft.socialapps.wordpress;


import static brooklyn.entity.basic.lifecycle.CommonCommands.installPackage;
import static brooklyn.entity.basic.lifecycle.CommonCommands.sudo;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.basic.SshMachineLocation;

public class WordpressSshDriver extends AbstractSoftwareProcessSshDriver implements WordpressDriver {

    // FIXME Some good security advice which we should follow at:
    //       http://jeffreifman.com/detailed-wordpress-guide-for-aws/install-wordpress/
    
    private String expandedInstallDir;

	public WordpressSshDriver(WordpressImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

	@Override
	public WordpressImpl getEntity() {
	    return (WordpressImpl) super.getEntity();
	}
	
	protected String getExpandedInstallDir() {
		return expandedInstallDir;
	}
	
	protected String getWwwDir() {
	    return "/var/www/html";
	}
	
    @Override
    public void install() {
        DownloadResolver resolver = entity.getManagementContext().getEntityDownloadsRegistry().resolve(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();
        expandedInstallDir = getInstallDir()+"/"+resolver.getUnpackedDirectorName("wordpress");
        
        List<String> commands = new LinkedList<String>();
        
        //sudo apt-get install php5 libapache2-mod-php5 libapache2-mod-auth-mysql php5-mysql
        
        commands.add(installPackage(of("yum", "httpd", "apt", "apache2"), null));
        commands.add(installPackage(of("yum", "php", "apt", "php5"), null));
        commands.add(installPackage(of("yum", "php-mysql", "apt", "php5-mysql"), null));
        commands.add(installPackage(of("yum", "php-gd", "apt", "php5-gd"), null));
        commands.add(installPackage(of("apt", "libapache2-mod-php5"), null));
        
        commands.addAll(CommonCommands.downloadUrlAs(urls, saveAs));
        commands.add(CommonCommands.INSTALL_TAR);
        commands.add("tar xzfv " + saveAs);

        newScript(INSTALLING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public void customize() {
        // this call allows it to wait for the database startup to complete, if configured with:
        //     .configure(Wordpress.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME))
        log.info("Waiting till the database is up");
        entity.getConfig(Wordpress.DATABASE_UP);
        log.info("Database is up and running");

        String configFileContents = processTemplate(getEntity().getTemplateConfigurationUrl());
        String destinationConfigFile = format("/tmp/wp-config.php", getWwwDir());
        getMachine().copyTo(new ByteArrayInputStream(configFileContents.getBytes()), destinationConfigFile);
        
        String customInstallFileContents = processTemplate(getEntity().getTemplateCustomInstallPhpUrl());
        String destinationCustomInstallFile = format("/tmp/custom-install.php", getWwwDir());
        getMachine().copyTo(new ByteArrayInputStream(customInstallFileContents.getBytes()), destinationCustomInstallFile);

//      commands.add(sudo("cp sites/default/default.settings.php sites/default/settings.php"));
//      commands.add(sudo("mkdir -p /var/www/sites/default/files"));
//      commands.add(sudo("chmod o+w sites/default/settings.php"));
//      commands.add(sudo("chmod o+w sites/default"));
//      commands.add(sudo("chown -R www-data.www-data /var/www"));
//      commands.add(sudo("chgrp -R www-data /var/www/sites/default/files"));
//      commands.add(sudo("chmod -R g+u /var/www/sites/default/files"));
//      commands.add(sudo("rm index.html"));
//      commands.add("set HOSTNAME = 'hostname'");
//      commands.add(sudo("sed -i.bk s/HOST_NAME/$HOSTNAME/g /etc/postfix/main.cf"));
//      commands.add("set DEBIAN_FRONTEND='noninteractive'");
//      commands.add(installPackage(of("apt","postfix"),null));

      // FIXME And this stuff?
      //cd /etc/apache2/sites-available
      
      // FIXME And this stuff?
      //commands.add(sudo("a2ensite wordpress"));
      
//        String setupDrupalScript = new ResourceUtils(WordpressSshDriver.class).getResourceAsString("classpath://io/cloudsoft/socialapps/drupal/setup-drupal.php");
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$site_name", entity.getConfig(Drupal.SITE_NAME));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$site_mail", entity.getConfig(Drupal.SITE_MAIL));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$admin_name", entity.getConfig(Drupal.ADMIN_NAME));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$admin_email", entity.getConfig(Drupal.ADMIN_EMAIL));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$admin_password", entity.getConfig(Drupal.ADMIN_PASSWORD));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$database_port", "" + entity.getConfig(Drupal.DATABASE_PORT));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$database_schema", entity.getConfig(Drupal.DATABASE_SCHEMA));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$database_user", entity.getConfig(Drupal.DATABASE_USER));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$database_password", entity.getConfig(Drupal.DATABASE_PASSWORD));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$database_host", entity.getConfig(Drupal.DATABASE_HOST));
//        setupDrupalScript = setupDrupalScript.replaceAll("\\$database_driver", entity.getConfig(Drupal.DATABASE_DRIVER));
//
//        getLocation().copyTo(new ByteArrayInputStream(setupDrupalScript.getBytes()), "/tmp/setup-drupal.php");

        List<String> commands = new LinkedList<String>();
        commands.add(sudo(format("mkdir -p %s", getWwwDir())));
        commands.add(sudo(format("cp -R %s/* %s/", getExpandedInstallDir(), getWwwDir())));
        commands.add(format("cd %s", getWwwDir()));
        commands.add(sudo("cp /tmp/wp-config.php ."));
        commands.add(sudo("cp /tmp/custom-install.php ."));
        commands.add(format("php custom-install.php"));

        newScript(CUSTOMIZING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public void launch() {
        List<String> commands = new LinkedList<String>();
//        commands.add(sudo("test -f /etc/init.d/httpd && /etc/init.d/httpd stop"));
//        commands.add(sudo("test -f /etc/init.d/apache2 && /etc/init.d/apache2 stop"));
        commands.add(sudo("/etc/init.d/httpd stop"));
        commands.add(sudo("/etc/init.d/httpd start"));
        
        // FIXME Or on debian?
        //commands.add(sudo("/etc/init.d/apache2 start"));

        newScript(LAUNCHING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public boolean isRunning() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/httpd status"));

        return newScript(CHECK_RUNNING).
                body.append(commands).execute() == 0;
    }

    @Override
    public void stop() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/httpd stop"));

        newScript(STOPPING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }
}
