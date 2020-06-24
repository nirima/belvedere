# Belvedere

> A building, or architectural feature of a building, designed and situated to look out upon a pleasing scene.

## Write OpenAPI specifications a better way

** Please note that this is, very much, a work in progress and not a finished article! **

## Documentation
[Index](docs/README.md)


### OpenAPI

OpenAPI is an extremely useful way to document the _specification_ for an API - particularly because it creates an implementation-neutral artifact that documents both the calls and parameters, but also examples and useful documentation.

There are may great generators - doing the work of implementing to spec in a reliable way, and documentation / exploration tools, that allow you to communicate your API to a wider audience.

*However* - as an _authoring_ solution it leaves much to be desired. 

- The two native formats - JSON and YAML - are hard to work with.

You end up either playing 'hunt the curly brace or missing quote' or errors in getting the indentation wrong in YAML.

- The native formats are very verbose

This is inevitable given that they are lightweight serializations to an underlying data model. But, particularly for data structures, it feels like a lot of lines are required to specify even very simple DTOs.

- very hard to break apart APIs into multiple files and/or re-use existing components. 

There are some ways of referencing items through YAML file references, but this is limited (places where a $ref is allowed). Even when not needing re-use, it wouold be nice to be able to split up large APIs into separate sections.

- suffers from a lot of 'DRY' - don't repeat yourself. 

E.g: If an API has a standard response to a request which is '404', then this may be repeated many times over (which makes it harder to make global changes).

- hard if you want multiple variants

E.g: Deploying into a front-end gateway (e.g AWS API Gateway) may require additional extension values in the OpenAPI definition. Ideally we'd like to author these in one place, but we don't particularly want these to be a part of the API definition we give to the public. Also, we may wish to have 'public' and 'private' methods within the same API - authored in the same place, but published as two separate variants.

### One alternative

One alternative would be to design the API in a specific language (say, Java), then use the tooling (which exists already) to auto-document "what it sees".

This may have it's uses - and indeed this tool may be extended to allow definitions in multiple formats - but, if you look at the code generated for, say, Java - it is very annotation-heavy, and itself not neccesarily a pleasant way to _author_ specification.

## Belvedere

Belvedere leverages groovy to define a DSL (Domain Specific Language) for specifying OpenAPI structures. Thus, the input is 'Belvedere DSL', and the output is 'OpenAPI YAML'. 

Since it is built as a DSL, this enables language features to simplify API specifications, and even add metaprogramming in more complex scenarios.

The DSL is directly mapped to the underlying OpenAPI model - so it should be very familiar to existing authors. There will also be converters to accept OpenAPI API definitions and convert these to the DSL authoring format.

### Examples

This YAML:
```yaml
info:
  version: 1.0.0
  title: Swagger Petstore
  license:
    name: MIT
```
    
is equivalent to this DSL

```groovy
info {        
        title "Swagger Petstore"
        version "1.0.0"
        license {
            name "MIT"
        }
    }
```    
    
## Defining a path:

```groovy

path("/pets/{id}") {
       
        operation(OperationType.GET, 'getPet') {
            response('200') {
                description "Pet successfully found"
                content('application/json') {
                    schema {
                        ref schema: 'Pet'
                    }
                }
            }
        }
  /* .. more APIs .. */
}
```

## Avoiding DRY

You can define functions, then evaluate them in every declared operation to avoid repeated specification. E.g:

```groovy
path("/pets/{id}") {

        common_parameters = {
            tags "Pets"

            parameter(id: String, in: 'path') {
                description "The ID of the pet"
            }

            response('404') {
                description "The pet was not found"
            }


        }

        operation(OperationType.GET, 'getPet') {
            evaluate common_parameters; // <-- Include everything above

            response('200') {
                description "Pet successfully found"
                content('application/json') {
                    schema {
                        ref schema: 'Pet'
                    }
                }
            }
        }   
}
```

## Including files

Files can be included through their relative path:

```groovy
   components {
        include 'schema/common/AsyncResponse.schema' // Included file
        
        // Or specify directly here
        schema('Thing') {
            required schema (id:String) {
                description "Identifier for this thing"
            }

            schema('reason':String) {
                description "Reason for the request"
            }

            schema('requirements') {
                schema(name:String)
            }

        }
   }
```   

### Quickstart:


#### Use through docker

* Converting a single, isolated file (not suitable if you use includes)

```bash 
docker run -i magnayn/belvedere convert - <  ~/myfile.api  > ~/myfile.yaml
```

* Converting a file

```bash
docker run -v ~/api:/api -i magnayn/belvedere convert -f /api/crud1.api > ~/api/crud1.yaml
```

