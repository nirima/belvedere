package com.nirima.belvedere.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nirima.openapi.dsl.writer.DSLWriter;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

public class ReverseCommand extends Command {
    @Override
    public void execute() throws IOException {
        SwaggerParseResult result = new OpenAPIParser().readLocation( in.toURI().toURL().toString(),null,null);

        OpenAPI specification = result.getOpenAPI();

        String beforeYaml = Yaml.pretty().writeValueAsString(specification);

        DSLWriter writer = new DSLWriter(specification);
        String serialized = writer.generate(writer.getApi());


        if( out != null ) {
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(serialized.getBytes());
            fos.close();
        } else {
            System.out.println(serialized);

        }


    }
}
