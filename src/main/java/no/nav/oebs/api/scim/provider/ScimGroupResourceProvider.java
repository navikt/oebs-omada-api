package no.nav.oebs.api.scim.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.service.ScimGroupService;
import org.springframework.stereotype.Component;

/**
 * SCIM Group Provider - placeholder
 * Bryker JAX-RS ressurser direkte (ScimGroupsResource)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScimGroupResourceProvider {

    private final ScimGroupService groupService;

}

