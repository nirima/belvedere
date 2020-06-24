package com.nirima.openapi.dsl

import io.swagger.v3.oas.models.OpenAPI

class APIValidation {


    public void validate(DSLContext ctx, OpenAPI api) {
        Set<String> s = api.getComponents()?.getSchemas()?.keySet();

        if( s == null )
            return;

        s = s.collect() { return "#/components/schemas/${it}".toString() };

        s.each { println "Defined Schemas: '${it}'"}

        ctx. references.each() { println "Reference to '${it}'" }

        ctx.references.each() {
            if( !s.contains(it) )
            {
                println "[Error]: Undefined reference to '${it}'";
            }
        }

        s.each() {
            if( !ctx.references.contains( it )) {
                println "[Warning]: Unused definition '${it}'";
            }
        }

    }
}
