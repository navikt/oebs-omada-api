package no.nav.oebs.api.Group.v1;

import org.apache.directory.scim.spec.schema.Meta;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
public class OebsScimGroupMapper {

    public ScimGroup toScim(OebsScimGroupView view) {
        if (view == null) {
            return null;
        }

        ScimGroup group = new ScimGroup();

        // SCIM id = ferdig bygget scim_id fra viewet (f.eks. G$16@LAGER 1, A$13@TK13 NAV Økonomi)
        group.setId(view.getScimId());

        // externalId = original kode (group_number / responsibility_key)
        group.setExternalId(view.getObjectId().toString());

        // displayName = det lesbare navnet
        group.setDisplayName(view.getObjectName());

        // ScimGroup-konstruktøren setter allerede riktig schema (SCIM Group core),
        // men vi kan være eksplisitte og sørge for at schemas inneholder Group-URN:
        group.setSchemas(Set.of(ScimGroup.SCHEMA_URI));

        Meta meta = new Meta();
        meta.setResourceType(ScimGroup.RESOURCE_NAME);
        meta.setLastModified(LocalDateTime.from(Instant.now()));
        group.setMeta(meta);

        return group;
    }
}
