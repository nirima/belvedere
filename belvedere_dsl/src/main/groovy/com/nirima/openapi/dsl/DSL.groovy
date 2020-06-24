package com.nirima.openapi.dsl

import groovy.util.logging.Slf4j
import io.swagger.v3.oas.models.OpenAPI
import org.apache.tools.ant.filters.StringInputStream
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.tools.shell.IO
import org.joda.time.DateTime

import java.lang.reflect.Array

class DSLExec {

    DSLOpenAPI dslOpenAPI;
    DSLContext context;


    public DSLExec(URL inputStream) {
        init(new DSLContext(inputStream));
    }

    DSLExec(InputStream stringInputStream) {
        init(new DSLContext(stringInputStream));
    }

    void init(DSLContext fis) {

        context = fis;
        DSL dsl = new DSL();

        dsl.parseScript(fis);
        dslOpenAPI = dsl.runScript();


    }

    public OpenAPI run() {
        def ctx = new Object();
        dslOpenAPI.accept(ctx);
        return dslOpenAPI.getTheAPI();
    }

    public void setProfiles(String[] p) {
        if( p != null )
            context.profiles = p;
    }

}

class DSLException extends RuntimeException
{
    DSLException() {
    }

    DSLException(String var1) {
        super(var1)
    }

    DSLException(String var1, Throwable var2) {
        super(var1, var2)
    }
}

@Slf4j
public class DSL {

    IoCapture c = new IoCapture();
    Script dslScript;

    DSLOpenAPI globe;

    void parseScript(DSLContext context) {
        Binding binding = new Binding();

      /*  binding.setProperty("out", c.io.out);
        binding.setProperty("print", new MethodClosure(c, "print"));
        binding.setProperty("println", new MethodClosure(c, "println"));
        binding.setProperty("echo", new MethodClosure(c, "println"));
        */
        

        def config = new CompilerConfiguration();

        def icz = new ImportCustomizer();
        icz.addImports('java.lang.reflect.Array', 'com.nirima.openapi.dsl.OperationType', 'org.joda.time.DateTime');
        

        config.addCompilationCustomizers(icz);

        GroovyShell shell = new GroovyShell(binding, config);


        try {
            dslScript = shell.parse(context.getData());
        }
        catch(Exception ex) {
            log.error("Error trying to parse file ${context}");
            throw new DSLException("Error parsing file", ex);
        }

        dslScript.metaClass = createEMC(dslScript.class,
                {
                    ExpandoMetaClass emc ->

                        emc.api = {
                            cl ->  globe = new DSLOpenAPI(context, cl);
                        }


                })



    }

    DSLOpenAPI runScript() {
        dslScript.run();
        return globe;
    }

    static ExpandoMetaClass createEMC(Class scriptClass, Closure cl) {
        ExpandoMetaClass emc = new ExpandoMetaClass(scriptClass, false);
        cl(emc)
        emc.initialize()
        return emc
    }

}

public class IoCapture implements Closeable {
    public final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    public final IO io;
    public final PrintStream ps;

    private final InputStream emptyInputStream;

    public IoCapture() {
        emptyInputStream = new ByteArrayInputStream(new byte[0]);

        io = new IO(emptyInputStream, byteArrayOutputStream, byteArrayOutputStream);

        ps = new PrintStream(byteArrayOutputStream);
    }

    public void print(String message) {
        ps.print(message);
        System.out.print(message);
    }

    public void println(Object message) {
        ps.println(message);
        System.out.println(message);
    }

    public String toString() {
        return new String(byteArrayOutputStream.toByteArray());
    }

    @Override
    public void close() throws IOException {
        emptyInputStream.close();
        io.close();
        ps.close();
    }
}