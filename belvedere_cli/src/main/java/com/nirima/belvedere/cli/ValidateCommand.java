package com.nirima.belvedere.cli;

import com.nirima.openapi.dsl.APIValidation;
import com.nirima.openapi.dsl.DSLExec;

import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;

public class ValidateCommand extends Command {
    @Override
    public void execute() throws IOException {
        DSLExec dsl = new DSLExec(in.toURI().toURL());

        OpenAPI spec = dsl.run();


        APIValidation v = new APIValidation();
        v.validate(dsl.getContext(), spec);


    }
}
