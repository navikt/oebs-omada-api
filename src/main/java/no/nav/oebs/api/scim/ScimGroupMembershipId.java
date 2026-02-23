package no.nav.oebs.api.scim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * Composite key for ScimGroupMembershipEntity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScimGroupMembershipId implements Serializable {
    private Long userId;
    private String scimGroupId;
}
