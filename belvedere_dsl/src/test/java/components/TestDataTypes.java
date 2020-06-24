package components;

import com.nirima.openapi.dsl.APIValidation;
import com.nirima.openapi.dsl.DSLExec;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TestDataTypes extends TestCase {

    public static class Tester {
        final URL url;

        private OpenAPI api;
        private DSLExec dslExec;

        Tester(String path) {
           this.url = getClass().getResource(path);
        }

        OpenAPI getOpenAPI() {
            if( this.api == null ) {
                DSLExec dsl = getDSLExec();

                this.api = dsl.run();
            }
            return this.api;
        }

        DSLExec getDSLExec() {
            if( this.dslExec == null ) {
                dslExec = new DSLExec(this.url);
            }
            return this.dslExec;
        }

        String getYaml() {
            OpenAPI api = getOpenAPI();
            return Yaml.pretty(api);
        }
    }

    public void testMapType() throws IOException {
        testConversion("/components/mapType.api");
    }

    public void testDataType() throws MalformedURLException {
        Tester t = new Tester("/components/dataType.api");
        
        OpenAPI spec = t.getOpenAPI();

        Yaml.prettyPrint(spec);

        APIValidation v = new APIValidation();
        v.validate(t.getDSLExec().getContext(), spec);


        Schema f = (Schema) spec.getComponents().getSchemas().get("TimeSpan").getProperties().get("from");
        assertEquals(f.getPattern(),"^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$");
        
    }

    public void testPathExample() throws MalformedURLException {
        DSLExec dsl = new DSLExec(getClass().getResource("/components/pathExample.api"));

        OpenAPI spec = dsl.run();

        Yaml.prettyPrint(spec);

        APIValidation v = new APIValidation();
        v.validate(dsl.getContext(), spec);

        Parameter p =spec.getPaths().get("/thing/{id}").getPost().getParameters().get(0);

        Object example = p.getExample();
        assertEquals("33C33192-7B8D-4EE0-AD41-47AACF240A29", example);
    }

    public void testHeaders() throws IOException {
        testConversion("/components/testHeaders.api");

      
    }

    public void testReturnTypes() throws IOException {
        testConversion("/components/returnTypes.api");
    }

    public void testCrud1() throws IOException {
        Tester t = new Tester("/crud/crud3.api");
        String yaml = t.getYaml();
        System.out.println(yaml);
    }

  

    private void testConversion(String url) throws IOException {
        Tester t = new Tester(url);
        String yamlFile = url.replace(".api",".yaml");

        String yaml = convert(getClass().getResourceAsStream(yamlFile), StandardCharsets.UTF_8);

        System.out.println(t.getYaml());
      //  assertEquals(yaml, t.getYaml());

    }

    public String convert(InputStream inputStream, Charset charset) throws IOException {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
