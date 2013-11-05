package io.cloudsoft.socialapps.thinkup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.webapp.WebAppServiceMethods;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.event.feed.ssh.SshFeed;
import brooklyn.event.feed.ssh.SshPollConfig;
import brooklyn.event.feed.ssh.SshValueFunctions;
import brooklyn.location.MachineLocation;
import brooklyn.location.basic.SshMachineLocation;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.io.ByteStreams;

public class ThinkUpImpl extends SoftwareProcessImpl implements ThinkUp {

    protected FunctionFeed serviceUpFeed;
    private SshFeed sshFeed;

    @Override
	public Class getDriverInterface() {
		return ThinkUpDriver.class;
	}

    @Override
    protected Collection<Integer> getRequiredOpenPorts() {
        Collection<Integer> ports = super.getRequiredOpenPorts();
        ports.add(80);
        ports.add(443);
        return ports;
    }
    
    @Override
    protected void connectSensors() {
        super.connectSensors();

        setAttribute(ThinkUp.ROOT_URL, String.format("http://%s:%s/", getAttribute(Attributes.HOSTNAME), getAttribute(HTTP_PORT)));

        serviceUpFeed = FunctionFeed.builder()
                .entity(this)
                .poll(new FunctionPollConfig<Object, Boolean>(SERVICE_UP)
                        .period(500, TimeUnit.MILLISECONDS)
                        .callable(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                return getDriver().isRunning();
                            }
                        })
                        .onError(Functions.constant(Boolean.FALSE)))
                .build();
        
        /*
         * Gives stdout such as:
         *     Total Accesses: 2
         *     Total kBytes: 0
         *     Uptime: 20
         *     ReqPerSec: .1
         *     BytesPerSec: 0
         *     BytesPerReq: 0
         *     BusyWorkers: 1
         *     IdleWorkers: 7
         */
        MachineLocation machine = getMachineOrNull();
        if (machine instanceof SshMachineLocation) {
            sshFeed = SshFeed.builder()
                    .entity(this)
                    .machine((SshMachineLocation) machine)
                    .poll(new SshPollConfig<Integer>(REQUEST_COUNT)
                            .period(1000)
                            .command("curl -f -L \"http://127.0.0.1/server-status?auto\"")
                            .failOnNonZeroResultCode()
                            .onError(Functions.constant(-1))
                            .onSuccess(SshValueFunctions.chain(SshValueFunctions.stdout(), new Function<String, Integer>() {
                                    @Override public Integer apply(@Nullable String stdout) {
                                        for (String line : stdout.split("\n")) {
                                            if (line.contains("Total Accesses")) {
                                                String val = line.split(":")[1].trim();
                                                return Integer.parseInt(val);
                                            }
                                        }
                                        LOG.info("Total Accesses not found in server-status, returning -1 (stdout="+stdout+")");
                                        return -1;
                                    }})))
                    .build();
        } else {
            LOG.warn("Location(s) %s not an ssh-machine location, so not polling for request-count", getLocations());
        }
        
        WebAppServiceMethods.connectWebAppServerPolicies(this);
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();

        if (serviceUpFeed != null) serviceUpFeed.stop();
        if (sshFeed != null) sshFeed.stop();
    }
    
    public String getTemplateConfigurationUrl() {
        return getConfig(TEMPLATE_CONFIGURATION_URL);
    }

    public String getTemplateCustomInstallPhpUrl() {
        return getConfig(TEMPLATE_CUSTOM_INSTALL_PHP_URL);
    }
    
	/** The name of the database for WordPress */
	public String getDatabaseName() {
	    return getConfig(DATABASE_NAME);
	}

	/** MySQL database username */
	public String getDatabaseUserName() {
        return getConfig(DATABASE_USER);
	}

	/** MySQL database password */
	public String getDatabasePassword() {
        return getConfig(DATABASE_PASSWORD);
	}

	/** MySQL hostname */
	public String getDatabaseHostname() {
        return getConfig(DATABASE_HOSTNAME);
	}

	public String getAppTitlePrefix() {
        return getConfig(APP_TITLE_PREFIX);
	}
	
//	public String getWeblogAdminEmail() {
//        return getConfig(WEBLOG_ADMIN_EMAIL);
//	}
//
//	public String getWeblogAdminPassword() {
//	    return getConfig(WEBLOG_ADMIN_PASSWORD);
//	}


    @Override
    public String getShortName() {
        return "thinkup-httpd";
    }
}
