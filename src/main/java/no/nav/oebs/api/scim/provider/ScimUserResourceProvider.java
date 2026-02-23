package no.nav.oebs.api.scim.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.service.ScimUserService;
import org.springframework.stereotype.Component;

/**
 * SCIM Group Provider - placeholder
 * Bryker JAX-RS ressurser direkte (ScimUSersResource)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScimUserResourceProvider {

    private final ScimUserService userService;


}

