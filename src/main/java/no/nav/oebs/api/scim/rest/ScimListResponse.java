package no.nav.oebs.api.scim.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * SCIM 2.0 ListResponse wrapper
 * https://tools.ietf.org/html/rfc7644#section-3.4.2
 */
@Data
public class ScimListResponse<T> {
    private List<String> schemas;
    private Integer totalResults;
    private Integer startIndex;
    private Integer itemsPerPage;
    private List<T> Resources;  // Capitalized per SCIM spec
}


