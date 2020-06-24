package com.nirima.openapi.dsl

import groovy.util.logging.Slf4j
import io.swagger.models.License

import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.*
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.apache.commons.beanutils.BeanUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.joda.time.DateTime

import java.lang.reflect.Array


public class DSLContext {

    private DSLContext parent;

    private URL baseURL;
    private InputStream inputStream;

    public String[] profiles = [];

    public Set references = [];

    public DSLContext newContext(String filename) {

        DSLContext child = new DSLContext(new URL(this.baseURL, filename));
        child.parent = this;
        return child;
    }

    public DSLContext(URL baseURL) {
        this.baseURL = baseURL;
    }

    public DSLContext(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getData() {
        if (baseURL != null)
            return baseURL.text;

        return inputStream.text;
    }

    public void addReference(item, usesName) {
        references.add(usesName);
        if (parent)
            parent.addReference(item, usesName);
    }

    void seenSchema(name, schema) {


    }


    @Override
    public String toString() {
        return new StringJoiner(", ", DSLContext.class.getSimpleName() + "[", "]")
                .add("baseURL=" + baseURL)
                .toString();
    }
}

@Slf4j
public class DSLItem<T> {

    DSLContext context;
    T self;

    public DSLItem(DSLContext context, T i) {
        this.context = context;
        this.self = i;
        assert context != null
    }

    void evaluate(Closure closure1) {
        closure1.delegate = this;
        closure1.resolveStrategy = Closure.DELEGATE_FIRST;
        closure1()
    }

    void evaluate(Closure closure1, Object params) {
        closure1.delegate = this;
        closure1.resolveStrategy = Closure.DELEGATE_FIRST;
        closure1(params)
    }



    public Object methodMissing(String name, Object args) {
        // here we extract the closure from arguments, etc
        // return "methodMissing called with name '" + name + "' and args = " + args;
        org.apache.commons.beanutils.BeanUtilsBean.getInstance().getProperty(self, name);

        try {
            BeanUtils.setProperty(self, name, args)
        } catch (Exception ex) {
            log.error("Error trying to set ${name} on ${this.getClass()}");
            throw ex;
        }
        return self;
    }

    def propertyMissing(String name, Object value) {

        //println "     set ${name}=${value} on ${this}"

        try {

            // Throw exception if not there
            org.apache.commons.beanutils.BeanUtilsBean.getInstance().getProperty(self, name);

            BeanUtils.setProperty(self, name, value);

        }
        catch (MissingPropertyException e1) {
            println(
                    "Error setting state for resource ${getProxy()} property ${name} with value ${value}");
        }
        catch (Exception ex) {
            println(
                    "Error setting state for resource ${getProxy()} property ${name} with value ${value}");
            throw ex;
        }

        //println "     done set ${name}=${value} on ${this}"

    }

    public Script load(String relativePath) {

        DSLContext childContext = new DSLContext(new URL(context.baseURL, relativePath));

        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(DelegatingScript.class.getName());

        DelegatingScript dslScript = (DelegatingScript) new GroovyShell(getClass().getClassLoader(),
                new Binding(), cc).
                parse(childContext.getData());

        dslScript.setDelegate(this);

        return dslScript;
    }

    void process(Closure closure) {
        if( closure == null )
            return;
        
        try {
            closure.delegate = this;

            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }
        catch (Exception ex) {
            throw new DSLException("Error processing definiton for ${self.getClass()}", ex);
        }
    }

    DSLSchema schema(Closure c) {

        DSLSchema child = new DSLSchema(context);
        child.process(c);
        self.setSchema(child.self);
        return child;
    }

    void profile(String name, Closure closure) {
        if( context.profiles.contains(name)) {
            process(closure);
        }
    }

    // TODO : May be better as profile if:'foo', then:{} else:{} ?
    void profile(String name, Closure closure, Closure c2) {


        if( context.profiles.contains(name)) {
            process(closure);
        } else {
            process(c2);
        }
    }

    void profile(Map map) {
        if( context.profiles.contains(map.get("if"))) {
            process(map.get("then"));
        } else {
            process(map.get("else"));
        }
    }


}

@Slf4j
public class DSLOpenAPI extends DSLItem<OpenAPI> {


    OpenAPI theAPI;

    Closure closure;

    public DSLOpenAPI(DSLContext context, Closure c) {
        super(context, new OpenAPI());

        self.setPaths(new Paths());

        this.closure = c;

        theAPI = self;
    }

    public void accept(Object context) {

        //  closure.delegate = this;
        //  closure.resolveStrategy = Closure.DELEGATE_FIRST

        //   closure()
        process(closure);
    }

//    public void evaluate(Closure closure1) {
//        closure1.delegate = closure.delegate;
//        closure1.resolveStrategy = Closure.DELEGATE_FIRST;
//        closure1()
//    }

    public DSLSecurityRequirement security(String scope, Closure c) {
        DSLSecurityRequirement securityRequirement = new DSLSecurityRequirement(context);
        securityRequirement.self.addList(scope);
        securityRequirement.process(c);

        self.addSecurityItem(securityRequirement.self);

        return securityRequirement

    }

    public DSLInfo info(Closure c) {
        DSLInfo dslInfo = new DSLInfo(context);
        dslInfo.process(c);
        self.setInfo(dslInfo.self);
        return dslInfo;
    }

    DSLServer server(Closure c) {
        DSLServer s = new DSLServer(context);
        s.process(c);

        if (self.getServers() == null)
            self.setServers(new ArrayList<Server>());

        self.getServers().add(s.self);

        return s;
    }

    public DSLPathItem path(String name, Closure c) {
        DSLPathItem pi = new DSLPathItem(this, name, context);
        pi.process(c);
        self.getPaths().addPathItem(name, pi.self);
        return pi;
    }

    public DSLComponents components(Closure c) {
        DSLComponents components = new DSLComponents(context);
        components.process(c);
        self.setComponents(components.self);
        return components;
    }

    public DSLTagItem tag(String name, Closure c) {
        def item = new DSLTagItem(context, new Tag());
        item.name = name;

        self.addTagsItem(item.self);

        return item;
    }

}

@Slf4j
class DSLTagItem extends DSLItem<Tag> {

    DSLTagItem(DSLContext context, Tag i) {
        super(context, i)
    }
}

@Slf4j
class DSLPathItem extends DSLItem<PathItem> {

    private final String name;
    private final DSLOpenAPI parent;

    List<DSLOperation> operations = new ArrayList<>();

    DSLPathItem(DSLOpenAPI parent, String name, DSLContext context) {
        super(context, new PathItem());
        this.name = name;
        this.parent = parent;
    }

    public DSLOperation post(Closure c) {
        DSLOperation pi = new DSLOperation(context);
        pi.process(c);
        self.setPost(pi.self);
        return pi;
    }

    public DSLPathItem path(String n2, Closure c) {
        DSLPathItem pi = new DSLPathItem(parent, this.name + n2, context);
        pi.process(c);
        parent.self.getPaths().addPathItem(pi.name, pi.self);
        return pi;
    }


    public DSLOperation operation(OperationType method, String operationId, Closure closure) {
        DSLOperation pi = new DSLOperation(context);
        operations.add(pi);
        pi.process(closure);

        switch (method) {
            case OperationType.GET:
                self.setGet(pi.self);
                break;
            case OperationType.PUT:
                self.setPut(pi.self);
                break;
            case OperationType.POST:
                self.setPost(pi.self);
                break;

            case OperationType.DELETE:
                self.setDelete(pi.self);
                break;


        }

        pi.self.setOperationId(operationId);

        return pi;
    }


}


@Slf4j
class DSLServer extends DSLItem<Server> {

    DSLServer(DSLContext context) {
        super(context, new Server());
    }
}

@Slf4j
class DSLApiResponse extends DSLItem<ApiResponse> {

    DSLApiResponse(DSLContext context) {
        super(context, new ApiResponse());
    }

    public DSLMediaType content(String type, Closure c) {
        if (self.getContent() == null)
            self.setContent(new Content());

        DSLMediaType pi = new DSLMediaType(context);
        pi.process(c);
        self.getContent().addMediaType(type, pi.self);
        return pi;

    }

    public DSLHeader header(String type, Closure c) {

        DSLHeader pi = new DSLHeader(context);
        pi.process(c);
        self.addHeaderObject(type, pi.self);
        return pi;

    }
}

@Slf4j
class DSLHeader extends DSLItem<Header> {

    DSLHeader(DSLContext context) {
        super(context, new Header());

    }

    DSLSchema schema(Map elements, Closure c) {
        DSLSchema s = new DSLSchema(context);
        s.process(c);

        self.setSchema(s.self);
        return s;
    }

    DSLHeader style(String style) {

        self.setStyle(Header.StyleEnum.valueOf(style.toUpperCase()));

        return this;
    }

}

@Slf4j
class DSLComponents extends DSLItem<Components> {

    DSLComponents(DSLContext context) {
        super(context, new Components());
    }

    void include(String filename) {


        DSLContext childContext = context.newContext(filename);


        DSL d = new DSL();
        d.parseScript(childContext);

        DSLOpenAPI subset = null;

        try {
            subset = d.runScript();
            subset.accept(childContext);

        } catch (Exception ex) {
            throw new DSLException("Error processing included file ${filename}", ex);
        }

        def components = subset.self.getComponents();
        if (components == null) {
            log.warn("No components found in ${childContext} ?");
        } else {
            components.getSchemas().each { k, v ->

                self.addSchemas(k, v);
                log.info "  Child schema included ${k}";

            };
        }

    }

    DSLSecurity security(String key, Closure c) {
        DSLSecurity sec = new DSLSecurity(context);
        sec.process(c);

        self.addSecuritySchemes(key, sec.self);

        return sec;

    }


    DSLSchema schema(String name, Closure c) {
        try {
            DSLSchema s = new DSLSchema(context);
            s.process(c);

            self.addSchemas(name, s.self);
            return s;
        } catch (Exception ex) {
            throw new DSLException("Error processing schema ${name}", ex);
        }
    }

    DSLSchema schema(LinkedHashMap items, Closure c) {

        DSLSchema s = DSLSchemaBuilder.forContext(context).build(items, c);

        self.addSchemas(items.keySet().first(), s.self);

        return s;
    }


}

@Slf4j
class DSLSchemaBuilder {
    DSLContext context;

    private DSLSchemaBuilder(context) {
        this.context = context;
    }

    static DSLSchemaBuilder forContext(DSLContext context) {
        return new DSLSchemaBuilder(context);
    }

    DSLSchema build0(LinkedHashMap items, Closure c) {
        Class cls = items.values().first();

        DSLSchema s = new DSLSchema(context);

        // Belt and braces
        if (s.self == null)
            s.self = new StringSchema();

        s.process(c);

        return s;
    }

    DSLSchema build(LinkedHashMap items, Closure c) {
        def cls = items.values().first();
        String name = items.keySet().first();

        DSLSchema s = new DSLSchema(context);

        if (cls instanceof Class)
            s.self = makeSchemaFromClass(cls, items, c);
        else if (cls instanceof String) {
            s.self = new Schema();
            s.self.$ref(cls);
            context.addReference(s.self, s.self.get$ref());
        } else {
            // Assume closure?
            DSLSchema resultContainer = new DSLSchema(context, new Schema(), cls);

            s.self = resultContainer.self;
            /*def op = cls()

            def dat = op.item;

            s.item = op.item;
            s.item.name = name;

             */
            //return s;

            s.self.name = name;
            context.seenSchema(name, s.self);


            s.process(c);

            self.addProperties(name, s.self);
            return;

        }

        s.self.name = name;
        context.seenSchema(name, s.self);

        // Don't believe it's neccessary to process the closure
        // This is required:
        // schema(foo:String) { description "foo" }
        // inside a schema so need to process the parts inside.
        s.process(c);

        return s;
    }

    Schema makeSchemaFromClass(Class cls, LinkedHashMap data, Closure closure) {
        try {
            if (cls == String.class)
                return new StringSchema();
            else if (cls == Date.class)
                return new DateTimeSchema();
            else if (cls == BigDecimal.class || cls == Double.class || cls == double.class)
                return new NumberSchema();
            else if (cls == Boolean.class || cls == boolean.class)
                return new BooleanSchema();
            else if (cls == Integer.class || cls == int.class) {
                def r = new IntegerSchema();
                return r;
            } else if (cls == Long.class || cls == long.class) {
                def r = new IntegerSchema();
                r.format = "int64";
                return r;
            } else if (cls.isEnum()) {
                def ss = new StringSchema();
                cls.getEnumConstants().each() { ss.addEnumItem(it.toString()) }
                return ss;
            } else if (cls == Collection.class) {
                def t = cls.getTypeParameters()[0];
                println t;
            } else if (cls.isArray()) {
                Class clsx = cls.getComponentType()
                def ars = new ArraySchema();

                ars.items = makeSchemaFromClass(clsx, data, null);

                return ars;
            } else if (cls == Array) {

                def ars = new ArraySchema();
                ars.items = new Schema();
                Object arrayType = data['arrayType'];

                if (arrayType == null) {
                    DSLSchema resultContainer = new DSLSchema(context, new Schema(), closure);

                    // This is an implicit object
                    ars.items = resultContainer.self;
                    return ars;
                }

                // arrayType might be a string containing the type, or a
                // class
                if (arrayType instanceof Class) {
                    if (arrayType == String.class)
                        arrayType = "string";

                    ars.setType(arrayType);

                } else {
                    ars.items.set$ref(arrayType);

                    // SELF? context.addReference(self, ars.items.get$ref());
                }

                return ars;

            }
            else if (cls == Map) {

                def ars = new MapSchema();

                Object mapType = data['mapType'];

                if (mapType == null) {
                    DSLSchema resultContainer = new DSLSchema(context, new Schema(), closure);

                    // This is an implicit object
                    ars.additionalProperties = resultContainer.self;
                    return ars;
                }

                if (mapType instanceof Class) {
                    if (mapType == String.class)
                        mapType = "string";

                    def os = new ObjectSchema();
                    os.setType(mapType);

                    ars.additionalProperties = os;


                } else {

                    def os = new ObjectSchema();
                    os.set$ref(mapType);

                    ars.setAdditionalProperties(os);

                    //ars.addionalP items.set$ref(arrayType);
                    //context.addReference(self, ars.items.get$ref());
                }

                return ars;
            }
            else {
                log.error("Unknown parameter type ${cls}");
                throw new DSLException("Unknown parameter type ${cls}");
            }
        } catch (Exception ex) {
            throw new DSLException("Error trying to make schema from ${cls} : ${data}", ex);
        }
    }
}


@Slf4j
class DSLOperation extends DSLItem<Operation> {

    DSLOperation(DSLContext context) {
        super(context, new Operation());
    }

    DSLRequestBody requestBody(Closure c) {
        DSLRequestBody pi = new DSLRequestBody(context);
        pi.process(c);
        self.setRequestBody(pi.self);
        return pi;
    }

    DSLParameter parameter(LinkedHashMap m, Closure c) {
        DSLParameter param = new DSLParameter(context);
        param.process(c);

        // parameter map name:Type

        def e = m.entrySet().first();
        String name = e.key;
        String type = e.value;

        String inType = m.get('in');
        param.self.setIn(inType); // TODO make into type so can do in:Path

        param.self.name = name;
        param.self.schema = new StringSchema(); // TODO from type

        self.addParametersItem(param.self);

        return param;
    }

    DSLApiResponse response(String type, Closure c) {
        DSLApiResponse pi = new DSLApiResponse(context);
        pi.process(c);

        if (self.getResponses() == null)
            self.setResponses(new ApiResponses());

        self.getResponses().put(type, pi.self);
        return pi;
    }

    public DSLSecurityRequirement security(String scope, Closure c) {
        DSLSecurityRequirement securityRequirement = new DSLSecurityRequirement(context);
        securityRequirement.self.addList(scope);
        securityRequirement.process(c);

        self.addSecurityItem(securityRequirement.self);

        return securityRequirement

    }

    DSLOperation tags(String tags) {
        self.addTagsItem(tags);
        return this;
    }


    DSLOperation tags(Object[] tags) {
        tags.each() {
            self.addTagsItem(it)
        }
        return this;
    }

    void extension(String name, Closure c) {
        DSLExtension child = new DSLExtension();
        child.process(c);

        this.self.addExtension('x-' + name, child)

    }

}

@Slf4j
class DSLParameter extends DSLItem<Parameter> {
    DSLParameter(DSLContext context) {
        super(context, new Parameter());
    }

    //DSLParameter setStyle(String s) {
    //
    //}

    DSLParameter style(String style) {
        self.setStyle(Parameter.StyleEnum.valueOf(style.toUpperCase()));
        return this;
    }

    DSLParameter example(String example) {
        self.setExample(example);
        return this;
    }
}


@Slf4j
class DSLRequestBody
        extends DSLItem<RequestBody> {

    DSLRequestBody(DSLContext context) {
        super(context, new RequestBody());
    }

    public DSLMediaType content(String type, Closure c) {
        if (self.getContent() == null)
            self.setContent(new Content());

        DSLMediaType pi = new DSLMediaType(context);
        pi.process(c);
        self.getContent().addMediaType(type, pi.self);
        return pi;

    }
}

@Slf4j
class DSLSchema
        extends DSLItem<Schema<?>> {

    DSLSchema(DSLContext context) {
        super(context, new ObjectSchema());
    }

    public DSLSchema(DSLContext context, Closure c) {
        super(context, new ObjectSchema());

        process(c);
    }

    public DSLSchema(DSLContext context, Schema s, Closure c) {
        super(context, s);

        process(c);
    }

    DSLSchema example(String value) {
        self.setExample(value);
        return this;
    }

    DSLSchema pattern(String pattern) {
        self.setPattern(pattern);
        return this;
    }

    DSLSchema ref(LinkedHashMap refspec) {
        // format schema:item
        try {
            String refspecValue = refspec.values().first();

            self.set$ref(refspecValue);
            context.addReference(self, self.get$ref());
        } catch (Exception ex) {
            throw new DSLException("Error trying to set a link ref from ${refspec}");
        }
        return this;
    }

    DSLSchema required(DSLSchema schema) {

        if (schema.self.name == null) {
            throw new DSLException("Cannot add unnamed schema as required into ${self.name}");
        }

        self.addRequiredItem(schema.self.name);
        return schema;

    }

    DSLSchema schema(String name, Closure c) {
        DSLSchema s = new DSLSchema(context);
        s.process(c);

        s.self.name = name;

        context.seenSchema(name, s.self);

        if (self.getProperties() == null)
            self.setProperties(new HashMap<>());

        self.getProperties().put(name, s.self);
        return s;
    }

    DSLSchema schema(LinkedHashMap items) {

        schema(items, {});
    }

    DSLSchema schema(LinkedHashMap items, Closure c) {

        def cls = items.values().first();
        String name = items.keySet().first();

        DSLSchema s = new DSLSchema(context);

        if (cls instanceof Class)
            s.self = makeSchemaFromClass(cls, items, c);
        else if (cls instanceof String) {
            s.self = new Schema();
            s.self.$ref(cls);
            context.addReference(s.self, s.self.get$ref());
        } else {
            // Assume closure?
            DSLSchema resultContainer = new DSLSchema(context, new Schema(), cls);

            s.self = resultContainer.self;
            /*def op = cls()

            def dat = op.item;

            s.item = op.item;
            s.item.name = name;

             */
            //return s;

            s.self.name = name;
            context.seenSchema(name, s.self);


            s.process(c);

            self.addProperties(name, s.self);
            return;

        }

        s.self.name = name;
        context.seenSchema(name, s.self);

        // Don't believe it's neccessary to process the closure
        // This is required:
        // schema(foo:String) { description "foo" }
        // inside a schema so need to process the parts inside.
        s.process(c);

        self.addProperties(name, s.self);

        return s;
    }

    Schema makeSchemaFromClass(Class cls, LinkedHashMap data, Closure closure) {
        try {
            if (cls == String.class)
                return new StringSchema();
            else if (cls == Date.class)
                return new DateSchema();
            else if (cls == DateTime.class)
                return new DateTimeSchema();
            else if (cls == BigDecimal.class || cls == Double.class || cls == double.class)
                return new NumberSchema();
            else if (cls == Boolean.class || cls == boolean.class)
                return new BooleanSchema();
            else if (cls == Integer.class || cls == int.class) {
                def r = new IntegerSchema();
                return r;
            } else if (cls == Long.class || cls == long.class) {
                def r = new IntegerSchema();
                r.format = "int64";
                return r;
            } else if (cls.isEnum()) {
                def ss = new StringSchema();
                cls.getEnumConstants().each() { ss.addEnumItem(it.toString()) }
                return ss;
            } else if (cls == Collection.class) {
                def t = cls.getTypeParameters()[0];
                println t;
            } else if (cls.isArray()) {
                Class clsx = cls.getComponentType()
                def ars = new ArraySchema();

                ars.items = makeSchemaFromClass(clsx, data, null);

                return ars;
            } else if (cls == Array) {

                def ars = new ArraySchema();
                ars.items = new Schema();
                Object arrayType = data['arrayType'];

                if (arrayType == null) {
                    DSLSchema resultContainer = new DSLSchema(context, new Schema(), closure);

                    // This is an implicit object
                    ars.items = resultContainer.self;
                    return ars;
                }

                // arrayType might be a string containing the type, or a
                // class
                if (arrayType instanceof Class) {
                    if (arrayType == String.class)
                        arrayType = "string";

                    ars.setType(arrayType);

                } else {
                    ars.items.set$ref(arrayType);
                    context.addReference(self, ars.items.get$ref());
                }

                return ars;


            } else if (cls == Map) {

                def ars = new MapSchema();

                Object mapType = data['mapType'];

                if (mapType == null) {
                    DSLSchema resultContainer = new DSLSchema(context, new Schema(), closure);

                    // This is an implicit object
                    ars.additionalProperties = resultContainer.self;
                    return ars;
                }

                if (mapType instanceof Class) {
                    if (mapType == String.class)
                        mapType = "string";

                    def os = new ObjectSchema();
                    os.setType(mapType);

                    ars.additionalProperties = os;


                } else {

                    def os = new ObjectSchema();
                    os.set$ref(mapType);

                    ars.setAdditionalProperties(os);

                    //ars.addionalP items.set$ref(arrayType);
                    //context.addReference(self, ars.items.get$ref());
                }

                return ars;
            } else {
                log.error("Unknown parameter type ${cls}");
                throw new DSLException("Unknown parameter type ${cls}");
            }
        } catch (Exception ex) {
            throw new DSLException("Error trying to make schema from ${cls} : ${data}", ex);
        }
    }

}


@Slf4j
class DSLMediaType
        extends DSLItem<MediaType> {

    DSLMediaType(DSLContext context) {
        super(context, new MediaType());
    }

    DSLSchema schema(LinkedHashMap elements, Closure c = {}) {

        DSLSchema s = DSLSchemaBuilder.forContext(context).build(elements, c);

        self.setSchema(s.self);
        return s;
    }
}

public class DSLLicense extends DSLItem<io.swagger.v3.oas.models.info.License> {

    DSLLicense(DSLContext context) {
        super(context, new io.swagger.v3.oas.models.info.License())
    }
}

public class DSLInfo extends DSLItem<Info> {

    public DSLInfo(DSLContext context) {
        super(context, new Info());

    }

    public void contact(Closure c) {
        DSLContact child = new DSLContact(context);
        child.process(c);
        self.setContact(child.self);
    }

    public void extension(String name, Closure c) {
        DSLExtension child = new DSLExtension();
        child.process(c);

        this.self.addExtension('x-' + name, child)

    }

    public DSLLicense license(Closure c) {
        DSLLicense l = new DSLLicense(context);
        l.process(c);
        self.setLicense(l.self);
        return l;
    }

}

public class DSLExtension extends LinkedHashMap {

    void process(Closure closure) {
        closure.delegate = this;

        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }


    def propertyMissing(String name, Object value) {


        this.put(name, value);

    }

    Object methodMissing(String name, Object args) {

        if (args == null)
            return item;

        if (args.getClass().isArray()) {
            if (((Object[]) args).length == 1) {
                // Single element
                Object arg = args[0];

                if (arg instanceof Closure) {
                    DSLExtension e = new DSLExtension();
                    e.process(arg);
                    this.put(name, e);

                } else {
                    this.put(name, arg);
                }

                return item;

            }
        }


        if (arg instanceof Closure) {
            DSLExtension e = new DSLExtension();
            e.process(args);
            this.put(name, e);

        } else {
            this.put(name, args);
        }

        return item;
    }

}


@Slf4j
class DSLContact extends DSLItem<Contact> {
    public DSLContact(DSLContext context) {
        super(context, new Contact());
    }
}

class DSLSecurity extends DSLItem<SecurityScheme> {

    DSLSecurity(DSLContext context) {
        super(context, new SecurityScheme())
    }

    public DSLSecurity type(String type) {
        self.setType(SecurityScheme.Type.valueOf(type.toUpperCase()));
        return this;
    }

    public DSLSecurity setIn(String type) {
        self.setIn(SecurityScheme.In.valueOf(type.toUpperCase()));
        return this;
    }

    void extension(String name, String value) {
        this.self.addExtension(name, value);
    }

    void extension(String name, Closure c) {
        DSLExtension child = new DSLExtension();
        child.process(c);

        this.self.addExtension(name, child)

    }
}

class DSLSecurityRequirement extends DSLItem<SecurityRequirement> {

    DSLSecurityRequirement(DSLContext context) {
        super(context, new SecurityRequirement())
    }
}
