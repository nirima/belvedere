package com.nirima.openapi.dsl.writer

import groovy.util.logging.Slf4j
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.beanutils.PropertyUtils
import org.apache.commons.lang3.ClassUtils

class DW {


    String write(Object o, String name) {
        def value = BeanUtils.getProperty(o, name);
        if (value == null)
            return '';
        return "${name} \"${value}\"\n";
    }
}

@Slf4j
public class BaseWriter<T> {

    // Name of the thing ; the default header uses this
    String name;
    T item;

    BaseWriter(String name, T item) {
        this.name = name;
        this.item = item;
    }

    static String indent(String s) {
      //  s.replace("")
      //  return '  ' + s.replace('\n', '\n  ');
      //  return s;

      return s.replaceAll(/(?m)^/,'  ');

    }


    String toDSL() {
        if (item == null)
            return;

        String s = writeHeader();

        def itemMap = PropertyUtils.describe(item);

        itemMap.each() { k, v ->
            s += indent(write(item, k));
        }


        s += writeFooter();

        return s;
    }

    String writeHeader() {
        if( name == null )
            return "";

        "${name} {\n"
    }

    String writeFooter() {
        if( name == null )
            return "";

        
        "}\n"
    }


    String write(Object o, String name) {
        def value = PropertyUtils.getProperty(o, name);
        if (value == null)
            return '';

        writeValue(o, name, value);
    }

    String writeValue(Object o, String name, Object value) {

        if (name == "class")
            return '';

        if( value.getClass() == Boolean.class ) {
            return "${name} ${value}\n";
        }

        if( value.getClass().isEnum() ) {
            return "${name} '${value}'\n"
        }

        if (ClassUtils.isPrimitiveOrWrapper(value.getClass()) || value.getClass() == String.class)
            return "${name} \"${value}\"\n";

        if( value.getClass() == BigDecimal.class) {
            return "${name} ${value}\n";
        }

        if (name == "extensions") {

            String ret = "";

            value.each() { k, v ->

                ret += "extension('${k}') { \n"

                v.each() { k2, v2 ->

                    ret += indent("""${k2} "${v2}"\n """);

                }


                ret += "}\n"


            }
            return ret;
        }

        if (value instanceof List) {

            if( value.size() == 0 )
                return "";

            Object type = value.get(0);
            String ret = "";

            if( type instanceof String ) {
                ret = "${name} ";
                value.eachWithIndex { Object entry, int i ->
                    try {

                        def val_string = "";
                        ret += "${i>0?', ':''}\"${entry}\"";
                        //ret += generate(v);
                    } catch (Exception ex) {
                        log.error(ex.toString());
                    }
                }

            } else {

                value.eachWithIndex { Object entry, int i ->
                    try {


                        ret += createFor(entry).toDSL();
                        //ret += generate(v);
                    } catch (Exception ex) {
                        log.error(ex.toString());
                    }
                }
         

            }
            ret += "\n"
            return ret;
        }

        if (value instanceof HashMap) {
            String ret = "";
            value.each() { k, v ->
                ret += indent(createFor(v).toDSL());
            }
            return ret;
        }

        if (value instanceof Class) {
            return value.toString();
        }

        return createFor(value).toDSL();
        //return "  ${name} \"${value}\"\n";
    }


    static BaseWriter createFor(Object item) {

        log.info("Create for ${item.getClass()}");

        assert item != null;

        String pkg = item.getClass().getPackage().getName().toString();
        assert pkg.startsWith("io.swagger.v3.oas.models");


        if (item instanceof OpenAPI) {
            return new OpenAPIWriter(item);
        }

        if (item instanceof HashMap) {
            return new MappedWriter(item.getClass().getSimpleName().toLowerCase(), item);
        }

        if (item instanceof Schema) {
            return new SchemaWriter("schema", item);
        }

        if (item instanceof Components) {
            return new ComponentsWriter(item);
        }


        if (item instanceof PathItem) {
            return new PathItemWriter(item);
        }

        if (item instanceof ApiResponse) {
            return new ApiResponseWriter(item);
        }

        if (item instanceof Header) {
            return new HeaderWriter(item);
        }

        if (item instanceof MediaType) {
            return new MediaTypeWriter(item);
        }

        if (item instanceof Parameter) {
            return new ParameterWriter(item);
        }




        return new BaseWriter(item.getClass().getSimpleName().toLowerCase(), item);
    }


}

class MediaTypeWriter extends BaseWriter<MediaType> {
    MediaTypeWriter(MediaType item) {
        super(null, item);
    }

    @Override
    String writeValue(Object o, String name, Object value) {

        if( name == "schema" )                                          {
            String ret = "${SchemaWriter.writeHeaderForSchema("type", value)} {";
            ret += indent(createFor(value).toDSL());
            ret += "}\n";

            return ret;
        }


        return super.writeValue(o, name, value)
    }
}

class HeaderWriter extends BaseWriter<Header> {
    HeaderWriter(Header item) {
        super("", item);
    }

    @Override
    String writeHeader() {
        return "";
    }

    String writeFooter() {
        return "";
    }

    @Override
    String writeValue(Object o, String name, Object value) {

        if( name == "schema" )                                          {
            String ret = "${SchemaWriter.writeHeaderForSchema("type", value)} {";
            ret += indent(createFor(value).toDSL());
            ret += "}\n";

            return ret;
        }


        return super.writeValue(o, name, value)
    }
}

class ApiResponseWriter extends BaseWriter<ApiResponse> {
    ApiResponseWriter(ApiResponse item) {
        super(null, item);
    }

    @Override
    String writeValue(Object o, String name, Object value) {

        if (name == "headers") {


            def ret = "";
            LinkedHashMap linkedHashMap = (LinkedHashMap) value;

            linkedHashMap.entrySet().each() { entry ->

                ret += "header('${entry.key}') {\n"
                ret += indent(createFor(entry.value).toDSL());
                ret += "}\n";


            }

            return ret;
        }


        if (name == "content") {


            def ret = "";
            LinkedHashMap linkedHashMap = (LinkedHashMap) value;

            linkedHashMap.entrySet().each() { entry ->

                ret += "content('${entry.key}') {\n"
                ret += indent(createFor(entry.value).toDSL());
                ret += "}\n";


            }

            return ret;
        }

        return super.writeValue(o, name, value)
    }
}


class PathItemWriter extends BaseWriter<PathItem> {
    PathItemWriter(PathItem item) {
        super("", item);
    }

    String toDSL() {

        String ret = "";

        if (item.get != null)
            ret += new OperationWriter("get", item.get).toDSL();

        if (item.put != null)
            ret += new OperationWriter("put", item.put).toDSL();

        if (item.post != null)
            ret += new OperationWriter("post", item.post).toDSL();


        return indent(ret);
    }

}


class ParameterWriter extends BaseWriter<Parameter> {
    ParameterWriter(Parameter item) {
        super("parameter", item)
    }


    String writeHeader() {
        def param = "parameter("

        param += "${item.name}:${SchemaWriter.typeFor(item.schema)}, in:'${item.in}'"

        param += ") {\n"

        return param;
    }

    @Override
    String writeValue(Object o, String name, Object value) {
        if( name == "in" || name == "name")
            return "";

        return super.writeValue(o, name, value)
    }
}

class OperationWriter extends BaseWriter<Operation> {

    OperationWriter(String name, Operation item) {
        super(name, item)
    }

    String writeHeader() {
        "operation(OperationType.${name.toUpperCase()}, '${item.operationId}'){ \n"
    }

    String writeValue(Object o, String name, Object value) {

        // done by parent
        if (name == "operationId")
            return "";

        if( name == "responses" ) {
            def ret = "";
            ApiResponses responses = value;
            responses.entrySet().each() { entry ->
                ret += "response('${entry.key}') {\n";
                ret += indent(createFor(entry.value).toDSL())
                ret += "}\n";
            }
            return ret;
        }
        
        return super.writeValue(o, name, value);
    }

}

class ComponentsWriter extends BaseWriter {

    ComponentsWriter(Components item) {
        super("components", item)
    }

    String writeValue(Object o, String name, Object value) {
        if (name == "schemas") {
            def ret = "";

            value.each() {
                ret += "${SchemaWriter.writeHeaderForSchema(it.key, it.value)} {\n";
                ret += createFor(it.value).toDSL();
                ret += "}\n";
            }

            return ret;
        }


        return super.writeValue(o, name, value);
    }

}

class OpenAPIWriter extends BaseWriter {

    OpenAPIWriter(OpenAPI item) {
        super("api", item);
    }


    String writeValue(Object o, String name, Object value) {
        if (name == "paths") {
            def ret = "";

            value.each() {
                ret += "path('${it.key}') {\n";
                ret += createFor(it.value).toDSL();
                ret += "}\n";
            }

            return ret;
        }


        return super.writeValue(o, name, value);
    }
}

class MappedWriter extends BaseWriter {
    MappedWriter(String name, Object item) {
        super(name, item);
    }
}


class SchemaWriter extends BaseWriter<Schema> {
    SchemaWriter(String name, Schema item) {
        super(name, item);
    }


    String writeHeader() {
        // "schema('${item.name}') {\n"
        ""
    }

    String writeFooter() {
        ""
    }

    public static String typeFor(Schema s) {

        String ret = "";

        switch (s.type) {
            case "object":
                break;


            case "array":
                ret += "Array, arrayType:'${s.items.$ref}'";
                break;

            case "integer":
                def type = "int";
                if (s.format == "int64")
                    type = "long";

                ret += "${type}";

                break;

            case "string":
                ret += "String";
                break;

            default:
                if( s.type == null )
                    ret += "'${s.$ref}'";
                else
                    ret += "${s.type}";
                break;
        }

        return ret;
    }

    public static String writeHeaderForSchema(String name, Schema s) {
        def ret = "schema('${name}'";

        switch (s.type) {
            case "object":
                break;


            case "array":
                ret += ":Array, arrayType:'${s.items.$ref}'";
                break;

            case "integer":
                def type = "int";
                if (s.format == "int64")
                    type = "long";

                ret += ":${type}";

                break;

            case "string":
                ret += ":String";
                break;

            default:
                if( s.type == null )
                    ret += ":'${s.$ref}'";
                else
                    ret += ":${s.type}";
                break;
        }

        ret += ")"

        return ret;
    }

    String writeValue(Object o, String name, Object value) {
        if (name == "type")
            return ""; // done by the parent

        if (name == "format")
            return ""; // done by the parent

        if (name == '$ref')
            return ""; // done by the parent

        if (name == 'required')
            return ""; // done by the parent

        if (name == "properties") {

            def required = item.getRequired();
            if( required == null )
                required = new ArrayList<>();

            def ret = "";
            LinkedHashMap linkedHashMap = (LinkedHashMap) value;

            linkedHashMap.entrySet().each() { entry ->



                ret += "${required.contains(entry.key) ? 'required ' : ''}${writeHeaderForSchema(entry.key, entry.value)} {\n"
                ret += indent(new SchemaWriter("schema", entry.value).toDSL());
                ret += "}\n";


            }

            return ret;
        }

        return super.writeValue(o, name, value);
    }
}


public class DSLWriter {

    OpenAPI api;
    DW writer = new DW();

    public DSLWriter(OpenAPI api) {
        this.api = api;
    }

    String generate(OpenAPI api) {

        OpenAPIWriter wx = new OpenAPIWriter(api);
        return wx.toDSL();

    }

    /*
     static String generate(ExternalDocumentation docs) {
         if( docs == null )
             return '';

         return """
     externalDocs {
         url '${docs.url}'
         description '${docs.description}'
         ${generate(docs.extensions)}
     }
 """
     }

     static String generate(Map<String, Object> extensions) {
         if(extensions == null )
             return '';

         String ret = "";

         extensions.each() { k,v ->

             ret += "  extension('${k}') { \n"

             v.each() { k2,v2 ->

                 ret += """        ${k2} ${v2}""";

             }


             ret += "}\n"


         }
     }  */
}
