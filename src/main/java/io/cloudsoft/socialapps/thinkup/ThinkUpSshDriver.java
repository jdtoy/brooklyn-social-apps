package io.cloudsoft.socialapps.thinkup;


import static brooklyn.util.ssh.CommonCommands.alternatives;
import static brooklyn.util.ssh.CommonCommands.installPackage;
import static brooklyn.util.ssh.CommonCommands.ok;
import static brooklyn.util.ssh.CommonCommands.sudo;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ResourceUtils;
import brooklyn.util.ssh.CommonCommands;

public class ThinkUpSshDriver extends AbstractSoftwareProcessSshDriver implements ThinkUpDriver {

    // FIXME Only tested on CentOS
    // e.g. on debian, need to use /etc/init.d/apache2 instead of /etc/init.d/httpd
    
    // FIXME Some good security advice which we should follow at:
    // http://jeffreifman.com/detailed-wordpress-guide-for-aws/install-wordpress/
    // Also look at DrupalSshDriver. It does some good `chown`, `chgrp` and `chmod`.
    
    // FIXME There are other commands at:
    // http://jeffreifman.com/detailed-wordpress-guide-for-aws/install-wordpress/
    // to do with /etc/apache2/sites-available, `a2ensite wordpress` etc; not doing those currently.
    
    // FIXME Haven't set up email-server, so admin e-mails are not sent
    // See debian-specific postfix setup in DrupalSshDriver.
    
    private String expandedInstallDir;

	public ThinkUpSshDriver(ThinkUpImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

	@Override
	public ThinkUpImpl getEntity() {
	    return (ThinkUpImpl) super.getEntity();
	}
	
	protected String getExpandedInstallDir() {
		return expandedInstallDir;
	}
	
	protected String getWwwDir() {
	    return "/var/www";
	}
	
    @Override
    public void install() {
        DownloadResolver resolver = entity.getManagementContext().getEntityDownloadsManager().newDownloader(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();
        expandedInstallDir = getInstallDir()+"/"+resolver.getUnpackedDirectoryName("thinkup");
        
        List<String> commands = new LinkedList<String>();
        
        commands.add(installPackage(of("yum", "httpd", "apt", "apache2"), null));
        commands.add(alternatives(Arrays.asList(
                installPackage(of("yum", "php53", "apt", "php5"), null),
                installPackage("php")), "php/php53 not available"));
        commands.add(alternatives(Arrays.asList(
                installPackage(of("yum", "php53-mysql", "apt", "php5-mysql"), null),
                installPackage("php-mysql")), "php/php53 mysql not available"));
        commands.add(alternatives(Arrays.asList(
                installPackage(of("yum", "php53-gd", "apt", "php5-gd"), null),
                installPackage("php-gd")), "php/php53 gd not available"));
        commands.add(alternatives(Arrays.asList(
                installPackage(of("yum", "php-curl", "apt", "php5-curl"), null),
                installPackage("php-curl")), "php/php53 curl not available"));
        commands.add(alternatives(Arrays.asList(
                installPackage(of("yum", "php-cli", "apt", "php5-cli"), null),
                installPackage("php-cli")), "php/php53 cli not available"));
        commands.add(installPackage(of("apt", "libapache2-mod-php5"), null));
        commands.add(installPackage(of("apt", "libapache2-mod-auth-mysql"), null));
        
        // as per willomitzer comment at http://googolflex.com/?p=482 (only needed if selinux is on this box)
        commands.add(ok(sudo("setsebool -P httpd_can_network_connect 1")));
        
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
        //     .configure(ThinkUp.DATABASE_HOSTNAME, DependentConfiguration.attributeWhenReady(mySqlNode, MySqlNode.HOSTNAME))

        log.info("Waiting until the database is ready ...");
        entity.getConfig(ThinkUp.DATABASE_UP);
        log.info("> Database is up and running.");


        // https://github.com/ginatrapani/ThinkUp/blob/master/extras/scripts/ec2-install-userdata/ec2-install-userdata.sh
        String configFileContents = processTemplate(getEntity().getTemplateConfigurationUrl());
        String destinationConfigFile = format("/tmp/config.inc.php", getWwwDir());
        getMachine().copyTo(new ByteArrayInputStream(configFileContents.getBytes()), destinationConfigFile);

        String customInstallFileContents = processTemplate(getEntity().getTemplateCustomInstallPhpUrl());
        String destinationCustomInstallFile = format("/tmp/custom-install.php", getWwwDir());
        getMachine().copyTo(new ByteArrayInputStream(customInstallFileContents.getBytes()), destinationCustomInstallFile);


        List<String> commands = new LinkedList<String>();
        // Check www directory exists, create if not
        commands.add(sudo(format("mkdir -p %s", getWwwDir())));

        // Copy ThinkUp to www from the untaring dir
        commands.add(sudo(format("cp -R %s/webapp/* %s/", getExpandedInstallDir(), getWwwDir())));
        commands.add(sudo("rm -f /var/www/index.html"));

        // Touch config.inc.php, transfer pre-populated config.inc.php,
        // transfer custom-install.php in place, then set ThinkUp permissions.
        // Note: touch-and-replace allows manual completion of setup if something goes wrong.
        commands.add(format("cd %s", getWwwDir()));
        commands.add(sudo("touch /var/www/config.inc.php"));
        commands.add(sudo("chown www-data /var/www/config.inc.php"));
        // TODO - For secure deployment the data directory should be higher than the web root.
        commands.add(sudo("chown -R www-data /var/www/data/"));
        commands.add(sudo("chown -R www-data /var/www/_lib/view/compiled_view/"));


        // Complete Brooklyn install
        commands.add(sudo("cp -f /tmp/config.inc.php ."));
        commands.add(sudo("cp /tmp/custom-install.php ."));


        // Setup mail
        commands.add(sudo("ln -s /usr/sbin/sendmail /usr/bin/sendmail"));

        // Install ThinkUp.
        commands.add(format("php custom-install.php"));

        // Get metrics from apache httpd
        // Note ugliness of sed+cp is because wrapping `sed -i.bak '...'` in `sudo -E -n -s --` 
        // breaks on CentOS, because of quotes.
        String httpdConfFile = "/etc/httpd/conf/httpd.conf";
        String httpdConfTempFile = "/tmp/httpd.conf-"+entity.getId();
        String serverStatusConf = 
                "<Location /server-status>\n" +
                "SetHandler server-status\n" +
                "Order Deny,Allow\n" +
                "Deny from all\n" +
                "Allow from localhost\n" +
                "Allow from 127.0.0.1\n" +
                "</Location>\n";
        commands.add(format("sed 's/^#ExtendedStatus On/ExtendedStatus On/' %s > %s", httpdConfFile, httpdConfTempFile));
        
        // TODO Don't add this multiple times!
        // if [ \"`grep -E \"^SetHandler server-status\" "+httpdConfTempFile+"`\" == \"\" ]; then\n"
        commands.add("cat >> "+httpdConfTempFile+" << END_CONF_"+entity.getId()+"\n" +
        		serverStatusConf+"\n" +
				"END_CONF_"+entity.getId()+"\n");
        
        commands.add(sudo(format("cp %s %s", httpdConfTempFile, httpdConfFile)));

        // start it -- as customizing plugins requires this
        commands.add(sudo("/etc/init.d/httpd restart"));
        
        newScript(CUSTOMIZING).
                failOnNonZeroResultCode().
                body.append(commands).execute();

    }

    @Override
    public void launch() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/httpd start"));
        
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
