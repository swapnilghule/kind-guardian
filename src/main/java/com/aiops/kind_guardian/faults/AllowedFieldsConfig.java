package com.aiops.kind_guardian.faults;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class AllowedFieldsConfig {

    private final Map<Topic, Set<String>> allowedFields = Map.of(
            Topic.WORKLOADS, Set.of(
                    "/spec/template/spec/containers/0/image",
                    "/spec/template/spec/containers/0/resources/limits/memory",
                    "/spec/template/spec/containers/0/resources/limits/cpu",
                    "/spec/replicas",
                    "/spec/template/spec/containers/0/config/configMap"
            ),
            Topic.SERVICES_NETWORKING, Set.of(
                    "/spec/selector/app",
                    "/spec/ports/0/targetPort"
            ),
            Topic.CONFIGURATION, Set.of(
                    "/spec/template/spec/containers/0/envFrom/0/configMapRef/name"
            ),
            Topic.TROUBLESHOOTING, Set.of(
                    "/spec/template/spec/containers/0/livenessProbe/httpGet/path",
                    "/spec/template/spec/containers/0/readinessProbe/httpGet/path"
            )
            // add more topics/fields as you extend to CKA/CKS
    );

    public boolean isAllowed(Topic topic, String field) {
        Set<String> fields = allowedFields.get(topic);
        return fields != null && fields.contains(field);
    }

    public Set<String> fieldsFor(Topic topic) {
        return allowedFields.getOrDefault(topic, Set.of());
    }
}
