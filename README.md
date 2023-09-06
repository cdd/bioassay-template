BioAssay Template 
=================

An editor for specifying how the BioAssay Ontology, and other related vocabularies, are to be used to describe bioassays.

Primary author: Dr. Alex M. Clark (alex@collaborativedrug.com)

Released under the Apache License 2.0

Based on Java 8 using Eclipse, JavaFX and Apache Jena.

The [BioAssay Ontology](http://bioassayontology.org) is included as part of the distribution. Latest content can be obtained from the [GitHub repository](https://github.com/BioAssayOntology/BAO/releases).

This project has been described in the scientific literature:
A.M. Clark, N.K. Litterman, J.E. Kranz, P. Gund, K. Gregory, B.A. Bunin. _BioAssay templates for the semantic web_. PeerJ Computer Science **2**:e61 (2016) [link](http://doi.org/10.7717/peerj-cs.61)

The source code contains functionality for manipulating the data model used for templates, which guide the use of the
BioAssay Ontology (BAO) for annotating bioassay protocols. It also includes the BioAssay Schema Editor, which is an
interactive desktop application that allows editing of template schemata, and a preliminary interface for using the
current template to annotate assays.

The project is a continuation of the work begun in:
"Fast and accurate semantic annotation of bioassays exploiting a hybrid of machine learning and user confirmation":
Alex M. Clark; Barry A. Bunin; Nadia K. Litterman; Stephan C. Schürer; Ubbo Visser, _PeerJ_ 524 (2014) [link](https://peerj.com/articles/524)

A second manuscript, which describes this application, has been published in _PeerJ CompSci_ [link](https://peerj.com/articles/cs-61/)

Installation
============

To compile and run the package, download the files or synchronize with **git**. The deliverable can be compiled using **ant** (the
`build.xml` file is provided), or it can be opened as an **Eclipse** project. Java 8 is required, but all other dependencies are
included in the project.

To get started quickly without compiling, download two files:

  * the pre-built package: [pkg/BioAssayTemplate.jar](https://s3.us-east-2.amazonaws.com/cdd-bioassay-template/lib/BioAssayTemplate.jar)
  * the Common Assay Template: [data/template/schema.json](https://github.com/cdd/bioassay-template/blob/master/data/template/schema.json)
  
Run the application by double clicking on the `BioAssayTemplate.jar` file within the appropriate file manager tool, or run it with the
command line syntax: `java -jar BioAssayTemplate.jar`. As long as you have Java 8 installed, the interface should appear, with a blank 
schema window as the default. Use File|Open to locate and load the `schema.json` file.

When executing a build version with raw Java `.class` files, it is useful to add the command line parameter 
`-Dlog4j.configuration=file:cfg/log4j.properties` in order to prevent the logging mechanism from complaining needlessly.

Java Version
============

The transition to Java 11 has been problematic: the JavaFX dependency has been separated into an independent project, and Oracle has deprecated Java 8, making it difficult to download. As a transitional measure, the source code target is Java 8, but it can be compiled and run with either JDK 8 or JDK 11. There are two main options:

* use Eclipse with JDK 8 as the global default, and everything works as per usual
* use Eclipse with JDK 11 as the global default, which is coerced into using Java 8 compatibility

When using JDK 11, even in backward compatibility mode, it is not possible to *run* the template editor in graphical mode, because the JavaFX libraries are not part of the JRE anymore. For that reason, the `libjfx` library has been included in this project, which consists of just the platform independent language bindings, and **not** the actual compiled binaries, which are necessary for runtime use. The compiler is told to link against the JAR files, which resolves the compile-time issues. Running the classes or JAR file with Java 11 will fail unless additional parameters are added.

Assuming that JavaFX is installed in `/opt/javafx-sdk-11.0.1/lib` and defined by `$JFX`, the following syntax should be used:

`java --module-path $JFX --add-modules=javafx.controls -jar pkg/BioAssayTemplate.jar`

In the near future, the language syntax target will be updated to JDK 11.