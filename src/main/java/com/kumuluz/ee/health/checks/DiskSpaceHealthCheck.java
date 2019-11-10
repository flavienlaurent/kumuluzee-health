/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.health.checks;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.annotations.BuiltInHealthCheck;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free disk space health check.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@ApplicationScoped
@BuiltInHealthCheck
public class DiskSpaceHealthCheck extends KumuluzHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(DiskSpaceHealthCheck.class.getName());

    // Default disk space threshold of 100 MB
    private static final long DEFAULT_THRESHOLD = 100000000;

    @Override
    public HealthCheckResponse call() {
        long threshold = ConfigurationUtil.getInstance()
                .getLong(name() + ".threshold")
                .orElse(DEFAULT_THRESHOLD);

        try {
            if (Files.getFileStore(Paths.get("/")).getUsableSpace() >= threshold) {
                return HealthCheckResponse.up(DiskSpaceHealthCheck.class.getSimpleName());
            } else {
                LOG.severe("Disk space is getting low.");
                return HealthCheckResponse.down(DiskSpaceHealthCheck.class.getSimpleName());
            }
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to read disk space.", exception);
            return HealthCheckResponse.down(DiskSpaceHealthCheck.class.getSimpleName());
        }
    }

    @Override
    public String name() {
        return kumuluzBaseHealthConfigPath + "disk-space-health-check";
    }

    @Override
    public boolean initSuccess() {
        return true;
    }
}
