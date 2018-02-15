# BioAssay Template : Schema Format

The datastructure that the template editor operates on is referred to as a *schema*. The primary component is the *template*, but it can also optionally contain some number of *assays*, which are instances of data using the corresponding template.

The preferred file format for a schema is JSON based. It is also possible to serialise/deserialise using triples (typically Turtle/.ttl), but this format is relatively awkward, and suited only to use cases where RDF triples need to be manipulated directly.

The latest reference for the schema is the source code: the [Schema.java](https://github.com/cdd/bioassay-template/blob/master/src/com/cdd/bao/template/Schema.java) file has a method called `serialise`, which can be followed to the implementation.

The format is quite simple: 

    {
        "schemaPrefix": "{unique URI prefix to disambiguate this schema from all others}",
        "root": 
        {
            "name": "{name of template}",
            "descr": "{description}",
            "assignments":
            [
                ...
            ],
            "subGroups":
            [
                ...
            ],
        },
        "assays":
        [
          ...
        ]
    }

The data hierarchy for the template consists of *groups* and *assignments*. The first entry in the template (*root*) is just a group. A group contains some number of *assignments* and some number of *sub-groups*.

## Groups

The group object has the following properties:

* name
* descr
* groupURI
* assignments
* subGroups

The *name* should be a short descriptive label, while *descr* is a longer paragraph that is often displayed as a tooltip.

*groupURI* is used to refer to the group, and it is important for these URIs to be stable. Changing the value will affect how data is created and mapped to the template. The *groupURI* values do not need to be unique throughout the whole template, but they must be unique within their own branch.

*assignments* and *subGroups* are both arrays of objects. The *assignment* object is described below, while the *group* object follows the same format as described here.

## Assignments

The assignment object has the following properties:

* name
* descr
* propURI
* values
* suggestions

The *name* should be a short descriptive label, while *descr* is a longer paragraph that is often displayed as a tooltip.

The *propURI* typically corresponds to a URI within one of the underlying ontologies, though this is not guaranteed to be the case. The *name* and *descr* values are often taken from the corresponding ontology term by default, but these are frequently overridden by the creator of the template.

The *values* property is an array of objects that define the tree of ontology terms that are to be associated with the assignment. The object is defined in the next section.

The *suggestions* property provides a value that indicates how the content within the assignment should be treated. This is typically used when creating or analyzing content. The permitted values are:

* full: ontology terms are expected, and all suggestion models will be applied
* disabled: ontology terms are expected, but suggestions should be disabled
* field: the values should refer to field names (which are context specific)
* url: the values should be URLs, which are treated as clickable links rather than ontology terms
* id: the values should be database identifiers for assays (context specific)
* string: arbitrary string values
* number: floating point numeric values (double precision)
* integer: integral values (32-bit signed)
* date: values should be dates of the form "YYYY/MM/DD"

## Values

An array of value objects is used to provide enough information to build up a tree of ontology terms for a particular assignment.

Required properties are:

* uri
* name
* descr

The *uri* must be a valid URI, and is almost always found in the underlying ontologies, although this is not required. The *name* and *descr* properties usually match the labels and descriptions for the term, but they are present in the template datastructure so they can be overridden if necessary.

The default meaning of a value is that just that one term should be included for the assignment. However, values  assignment are usually composed by indicating one or several root branches. A value may additionally supply one of the following properties as boolean flags:

* wholeBranch
* exclude
* excludeBranch

So, to indicate a whole ontology branch, add `"wholeBranch": true`.

In some cases, it is desirable to include everything in a branch, except for some specific cases. Terms can be excluded by listing them and setting the *exclude* or *excludeBranch* flag.

## Assays

Annotated assay content can be included within the schema. 

**NOTE** the format is currently being redesigned. For the moment, consider only the template sections, described above.