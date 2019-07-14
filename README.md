# Source Code for Sematic Matcher Artefacts
This repository contains the source code for the semantic matching artefacts. All code is written in Java (1.7). 

## Algorithms for Equivalence and Subsumption Matching
The matching algorithms are separated into Equivalence Matchers and Subsumption Matchers
### Equivalence Matchers
* Word Embedding Matcher
* Definition Equivalence Matcher
* Graph Equivalence Matcher
* Lexical Equivalence Matcher
* Property Equivalence Matcher

### Subsumption Matchers
* Compound Matcher
* Context Subsumption Matcher
* Definition Subsumption Matcher
* Lexical Subsumption Matcher

## Mismatch Detection Strategies
* Concept Scope Mismatch Detection
* Structure Mismatch Detection
* Domain Mismatch Detection

## Alignment Combination Methods
* Profile Weight (with and without matcher selection)
* Average Aggregation
* Majority Vote

## Evaluation Code
* Misc code for evaluating the 3 datasets with respect to precision, recall and F-measure.

## Miscellaneous Code
Basically code that enables processing of the semantic matching system as a whole. This includes code for: 
* interaction with the Neo4J graph database.
* interaction with WordNet lexicon.
* interaction with the WordNet Domains classification.
* language processing using the Standfor NLP libraries.
* string (pre)processing
* and various utility classes
