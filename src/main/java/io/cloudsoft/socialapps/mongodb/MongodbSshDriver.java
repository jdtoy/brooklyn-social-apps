package io.cloudsoft.socialapps.mongodb;

import static brooklyn.entity.basic.lifecycle.CommonCommands.installPackage;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ResourceUtils;

public class MongodbSshDriver extends AbstractSoftwareProcessSshDriver implements MongodbDriver {

    public MongodbSshDriver(Mongodb entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void install() {
		String classpath = "classpath://io/cloudsoft/socialapps/mongodb/";
		String aptSourceFile = new ResourceUtils(MongodbSshDriver.class).getResourceAsString(classpath + "10gen.list");
		getLocation().copyTo(new ByteArrayInputStream(aptSourceFile.getBytes()),"/tmp/10gen.list");
		String yumSourceFile = (getLocation().getOsDetails().is64bit() == true) ? new ResourceUtils(MongodbSshDriver.class)
				.getResourceAsString(classpath + "10gen.64bit.repo"): new ResourceUtils(MongodbSshDriver.class)
						.getResourceAsString(classpath + "10gen.32bit.repo");
		getLocation().copyTo(new ByteArrayInputStream(yumSourceFile.getBytes()),"/tmp/10gen.repo");
		
        List<String> commands = new LinkedList<String>();
        commands.add(CommonCommands.INSTALL_TAR);
        commands.add(CommonCommands.INSTALL_CURL);
        commands.add(CommonCommands.on("centos", "cp /tmp/10gen.repo /etc/yum.repos.d"));
        commands.add(CommonCommands.on("ubuntu", "cp /tmp/10gen.list /etc/apt/sources.list.d"));
        commands.add(installPackage("mongodb-10gen"));

        newScript(INSTALLING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public void customize() {
    	/*
        List<String> commands = new LinkedList<String>();
        commands.add("cd /var/www");
        commands.add("php setup-drupal.php");
        commands.add("rm setup-drupal.php");

        newScript(CUSTOMIZING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
         */
    }

    @Override
    public void launch() {
        List<String> commands = new LinkedList<String>();
        commands.add("service mongodb start");

        newScript(LAUNCHING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }

    @Override
    public boolean isRunning() {
        List<String> commands = new LinkedList<String>();
        commands.add("service mongodb status");

        return newScript(CHECK_RUNNING).
                body.append(commands).execute() == 0;
    }

    @Override
    public void stop() {
        List<String> commands = new LinkedList<String>();
        commands.add("service mongodb stop");

        newScript(STOPPING).
                failOnNonZeroResultCode().
                body.append(commands).execute();
    }
    
}
