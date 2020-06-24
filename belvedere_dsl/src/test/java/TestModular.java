import com.nirima.openapi.dsl.DSLExec;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestModular extends TestCase {


    private static final Logger log = LoggerFactory.getLogger(TestModular.class);
//
//
//    public void testOne() {
//        OpenAPI specification = new OpenAPI();
//
//        specification.setOpenapi("3.0.2");
//
//        Info info = new Info();
//        info.description("All your base");
//
//        specification.setInfo(info);
//
//        Yaml.prettyPrint(specification);
//
//    }

    public void testFn()
    {
        DSLExec dsl = new DSLExec(getClass().getResource("/modular/use_module.api"));
        
        OpenAPI spec = dsl.run();

        Yaml.prettyPrint(spec);

    }

}
