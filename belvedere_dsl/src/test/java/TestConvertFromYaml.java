import io.swagger.v3.core.util.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import com.nirima.openapi.dsl.DSL;
import com.nirima.openapi.dsl.DSLExec;
import com.nirima.openapi.dsl.writer.DSLWriter;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.info.Info;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import junit.framework.TestCase;
import org.apache.tools.ant.filters.StringInputStream;
import org.testng.Assert;


public class TestConvertFromYaml extends TestCase {

    public void testConvert() throws URISyntaxException, IOException {
        SwaggerParseResult result = new OpenAPIParser().readLocation(  getClass().getResource("/petstore.yaml").toURI().toURL().toString(),null,null);

        OpenAPI specification = result.getOpenAPI();


        String beforeYaml = Yaml.pretty().writeValueAsString(specification);

        DSLWriter writer = new DSLWriter(specification);
        String serialized = writer.generate(writer.getApi());

    //    System.out.println(serialized);

        // Now re-yamlate it

        try(FileOutputStream fo = new FileOutputStream(new File("/tmp/petstore.api"))) {
            fo.write(serialized.getBytes());
        }


            DSLExec dsl = new DSLExec(new StringInputStream(serialized));

            OpenAPI spec = dsl.run();

        String afterYaml = Yaml.pretty().writeValueAsString(spec);


      //  Assert.assertEquals(beforeYaml, afterYaml);

    }
}
