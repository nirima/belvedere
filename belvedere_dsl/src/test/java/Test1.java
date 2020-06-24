import com.nirima.openapi.dsl.APIValidation;
import com.nirima.openapi.dsl.DSLExec;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;

public class Test1 extends TestCase {


    private static final Logger log = LoggerFactory.getLogger(Test1.class);
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

    public void testTwo() throws MalformedURLException {
        DSLExec dsl = new DSLExec(getClass().getResource("/test.api"));
        //DSLExec dsl = new DSLExec(getClass().getResource("/petstore_security.api"));

        //dsl = new DSLExec(new File("/Users/magnayn/dev/allocate/Integration-APIs/api/allocate/vacancy_booking.api").toURL());

        //dsl = new DSLExec(new File("/Users/magnayn/dev/allocate/Integration-APIs/api/allocate/schema/common/AsyncResponse.schema").toURL());

        OpenAPI spec = dsl.run();


        Yaml.prettyPrint(spec);

        APIValidation v = new APIValidation();
        v.validate(dsl.getContext(), spec);

    }

}
