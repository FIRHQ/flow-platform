package com.flowci.core.common.health;

import com.flowci.core.common.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
public class K8s extends AbstractHealthIndicator {

    @Autowired
    private AppProperties.K8s k8sProperties;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.status(Status.UP);

        boolean isInCluster = k8sProperties.isInCluster();
        builder.withDetail("isDeployedInCluster", isInCluster);

        if (isInCluster) {
            builder.withDetail("namespace", k8sProperties.getNamespace())
                    .withDetail("pod", k8sProperties.getPod())
                    .withDetail("pod ip", k8sProperties.getPodIp());
        }
    }
}
