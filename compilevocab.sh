#!/bin/tcsh
ant
java -jar ~/cdd/bax/pkg/BioAssayTemplate.jar compile\
 data/template/*.json data/template/branch/*.json\
 build/vocab.dump
