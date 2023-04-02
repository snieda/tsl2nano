package de.tsl2.nano.modelkit.service;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.jboss.resteasy.annotations.GZIP;

import de.tsl2.nano.modelkit.impl.ModelKit;

@RequestScoped
@Path("/modelkit")
@GZIP
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "authentication", type = SecuritySchemeType.APIKEY, apiKeyName = "x-auth-token", in = SecuritySchemeIn.HEADER, description = "modelkit authentication")
public class ModelKitResource {

    // static {
    //     SortModel.create();
    // }

    @GET
    @Path("/get-modelkits")
    @Operation(summary = "current modelkits", description = "provides all current modelkits")
    public List<ModelKit> getConfigurations() {
        return ModelKit.getConfigurations();
    }

    @POST
    @Path("/reset-modelkits")
    @Operation(summary = "set new modelkits", description = "resets all current modelkits with given new ones")
    public void configure(
            @Parameter(description = "all new modelkits to overwrite existing ones", required = true, example = "[]") ModelKit... sortConfigurations) {
        ModelKit.saveAsJSon(sortConfigurations);
    }

    @POST
    @Path("/update-modelkit")
    @Operation(summary = "change explicit modelkit", description = "chnages a value of a modelkit")
    public void updateConfiguration(
            @Parameter(description = "shortname of modelkit to change", required = true, example = "std") @QueryParam("kitName") String kitName,
            @Parameter(description = "attribute name of modelkit to change", required = true, example = "cron") @QueryParam("property") String property,
            @Parameter(description = "new value to be set on modelkit", required = true, example = "* * * ? * MON-THU *") @QueryParam("value") String value) {

        ModelKit.updateConfiguration(kitName, property, value);
    }

    @POST
    @Path("update-kitelement")
    @Operation(summary = "change modelkit element", description = "changes one element of named modelkit")
    public void updateConfigElement(
            @Parameter(description = "shortname of modelkit to change", required = true, example = "std") @QueryParam("kitName") String kitName,
            @Parameter(description = "element type (Def, Fact, Func, Comp, Group", required = true, example = "Def") @QueryParam("typeName") String typeName,
            @Parameter(description = "json expression for new element", required = true, example = "{\"name\": \"std.group1Items\", \"value\": [\"test\"]}") @QueryParam("json") String elementAsJSon) {

        ModelKit.updateConfigurationElement(kitName, typeName, elementAsJSon);
    }
}
