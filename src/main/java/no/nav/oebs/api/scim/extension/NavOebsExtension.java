package no.nav.oebs.api.scim.extension;

import lombok.Data;
import org.apache.directory.scim.spec.annotation.Attribute;
import org.apache.directory.scim.spec.annotation.Schema;
import org.apache.directory.scim.spec.resources.ScimExtension;
import org.apache.directory.scim.spec.schema.AttributeDefinition;

import java.io.Serial;

/**
 * NAV OeBS-spesifikk SCIM-extension for bruker.
 * URN: urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User
 */
@Data
@Schema(
    id = NavOebsExtension.URN,
    name = "NavOebsExtension",
    description = "NAV OeBS-spesifikke brukerattributter"
)
public class NavOebsExtension implements ScimExtension {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String URN = "urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User";

    @Attribute(
        description = "Fullmakt tildelt brukeren i OeBS",
        mutability = AttributeDefinition.Mutability.READ_ONLY
    )
    private String fullmakt;

    @Override
    public String getUrn() {
        return URN;
    }
}

