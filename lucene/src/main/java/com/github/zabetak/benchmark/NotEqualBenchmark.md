<!--
{% comment %}
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to you under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
{% endcomment %}
-->

# Benchmark inequality queries in Lucene

A benchmark of various ways to create and evaluate inequality queries, i.e., equivalent to the
SQL not equal operator (`<>`) over Lucene indexes.

The not equal comparison strictly follows the SQL semantics, that is `field <> 5` returns all
documents with values that are not equal to 5 but not those documents that do not have a value for
this field; in other words excluding `null` values.

The experiments cover the cases of string and integer fields. The other numeric fields provided
in Lucene should follow more or less the trends observed for the integer values.

Note that the score of the documents is not needed in this context.

## Experimental setup

### Index

In each experiment we create a single Lucene index comprised from two kind of fields: 
* High-cardinality fields simulating use-cases where the number of values in the field are
 mostly unique (e.g., primary keys in relational tables).
* Low-cardinality fields simulating use-cases where the number of distinct values is rather small
 (e.g., a field holding the _age_ or the _gender_ of people).
  
The number of documents to populate the index is configurable. In these experiments, we present
the results for indexes of size 1M, 10M, and 100M. It is impractical to create indexes of size 100M
at once so there intermediate commits every 100K documents. For the sake of the experiments, we
 configured Lucene to not merge any segments (using `NoMergePolicy.INSTANCE`).  
 
In real world, we do not always have certain information available at all times. In a table
holding customers, we may not always have a value for the `age` column. In a relational setting
, the value in such a column is `null`. In Lucene, the field simply does not exist for a given
document but the outcome is the same. Since we want to measure the performance of queries, taking
also into account `null` values, we left the `10%` of the total documents empty.

For each category (high/low-cardinality) we index the values in various types of fields. For the 
same document, all the fields are populated with the same value.

### Integer fields

For integer values we use the following types of Lucene fields:
* `IntPoint`
* `NumericDocValuesField`
* `StoredField`

High-cardinality fields are populated with integer values uniformly distributed in the range `0` to
`Integer.MAX_VALUE`.

Low-cardinality fields are populated with integer values uniformly distributed in the range `0` to
`110`. 

#### String fields  

For string values we use the following types of Lucene fields:
* `StringField`
* `SortedDocValuesField`

High-cardinality fields are populated with string values of length 20 having as prefix the document
id and as suffix random ASCII (for readability purposes) characters. The document id prefix makes
the values mostly unique.

Low-cardinality fields are populated with the string values `TRUE` and `FALSE` simulating a
boolean column that is represented as a string.  
     
### Queries

In the sequel, we present the queries that were used in these experiments as strings (using the
`Query#toString()` method). 

### Queries on integer fields

For presentation purposes, we assume that the SQL equivalent is `age <> 28`.

##### Q0
Using the `IntPoint` field.

    +*:* -(age:[28 TO 28] (+*:* -age:[-2147483648 TO 2147483647]))
##### Q1
Using the `IntPoint` field.

    +age:[-2147483648 TO 2147483647] -age:[28 TO 28]
##### Q2
Using the `IntPoint` field.

    age:[-2147483648 TO 27] age:[29 TO 2147483647]
##### Q3
Using the `NumericDocValuesField` field.

    age:[-2147483648 TO 27] age:[29 TO 2147483647]
##### Q4
Using both `NumericDocValuesField` and `IntPoint` field.

    +DocValuesFieldExistsQuery [field=age] -age:[28 TO 28]
##### Q5
Using only `NumericDocValuesField`.

    +DocValuesFieldExistsQuery [field=age] -age:[28 TO 28]

### Queries on string fields

For presentation purposes, we assume that the SQL equivalent is `firstname <> 'Victor'`.

##### Q0
Using only `StringField`.

    +*:* -(firstName:Victor (+*:* -firstName:*))
##### Q1
Using only `StringField`.

    +firstName:* -firstName:Victor
##### Q2
Using both `StringField` and `SortedDocValuesField`.

    +DocValuesFieldExistsQuery [field=firstName] -firstName:Victor
##### Q3
Using only `StringField` with a custom query (not provided by Lucene) relying on an iterator over
the term index.

    !firstName:Victor
##### Q4
Using only `SortedDocValuesField` with a custom query (not provided by Lucene) relying on an
iterator over the doc values field.

    firstName:!=[56 69 63 74 6f 72]

##### Q5
Using only `SortedDocValuesField`.

    firstName:{* TO [56 69 63 74 6f 72]} firstName:{[56 69 63 74 6f 72] TO *}


## Results 

This section reports the average time in milliseconds of creating and executing each query 5 
times over an index with 1M, 10M, and 100M documents.

We report the times over high-cardinality and low-cardinality fields separately. 

Furthermore, we distinguish between queries that go over all matches and queries that go over
only the TOP-10 in document order.

### Results on integer fields
 
#### High-cardinality

##### All matches

|Query|    1M|       10M|       100M|
|--|---------|--------- |-----------|
|Q0|	5.394|	 369.586|	3952.192|
|Q1|	4.561|	  78.140|	 797.092|
|Q2|	4.741|	  79.278|	 819.515|
|Q3|	3.777|	 311.542|	2349.370|
|Q4|	5.183|	  52.181|	 527.773|
|Q5|	5.579|	 194.481|	1822.767|

The fastest query is Q4 followed closely by Q1, and Q2. 

##### TOP-10 matches

|Query|	    1M|	    10M|        100M|
|--|----------|--------|------------|
|Q0|	 0.026|	 48.637|	 481.117|
|Q1|	 4.529|	 47.135|	 577.044|
|Q2|	23.800|	228.169|	2296.618|
|Q3|	45.916|	423.906|	4159.621|
|Q4|	 0.019|	  0.202|	   2.839|
|Q5|	 0.028|	  0.167|	   1.939|

The fastest queries are Q4, and Q5.

#### Low-cardinality

##### All matches

|Query|	   1M|	    10M|	    100M|
|--|----------|--------|------------|
|Q0|	5.421|	316.957|	3136.672|
|Q1|	5.092|	 56.783|	 570.193|
|Q2|	5.613|	 79.070|	 778.146|
|Q3|	3.777|	295.749|	2478.469|
|Q4|	6.055|	 48.745|	 489.782|
|Q5|	5.647|	167.275|	1710.167|

The fastest query is Q4 followed closely by Q1, and Q2. 

##### TOP-10 matches

|Query|	    1M|	    10M|	    100M|
|--|----------|--------|------------|
|Q0|	 0.030|	 18.769|	 185.562|
|Q1|	 1.381|	 18.030|	 180.196|
|Q2|	23.472|	224.569|	2232.488|
|Q3|	39.989|	411.295|	4121.313|
|Q4|	 0.021|	  4.248|	  46.226|
|Q5|	 0.028|	  0.157|	   1.649|

The fastest query is Q5 followed by Q4.

#### Summary

Overall, and since we assumed that we have all fields available the best query is Q4 since it
outperforms the others in most cases and some times by orders of magnitude.

### Results on string fields

#### High-cardinality

##### All matches

|Query|	   1M|  	10M|	    100M|
|--|---------|---------|------------|
|Q0|	6.790|	953.668|	9122.548|
|Q1|	6.477|	464.143|	4497.644|
|Q2|	5.958|	 51.219|   	 489.681|
|Q3|	5.866|	487.136|	4706.604|
|Q4|	5.881|	 97.157|	 970.654|
|Q5|	5.839|	 84.752|	 813.002|

The fastest query is Q2.

##### TOP-10 matches

|Query|	    1M| 	10M|	    100M|
|--|----------|--------|------------|
|Q0|	 0.036|	413.578|	4180.604|
|Q1|	40.537|	413.708|	4070.840|
|Q2|	 0.032|	  0.245|	   3.021|
|Q3|	44.645|	428.981|	4233.283|
|Q4|	 0.026|	  0.158|	   1.964|
|Q5|	 0.049|	  0.359|	   4.468|

The fastest query is Q4 followed closely by Q2, and Q5.

#### Low-cardinality

##### All matches

|Query|	   1M|	    10M|	    100M|
|--|---------|---------|------------|
|Q0|	3.210|	381.705|	3663.213|
|Q1|	3.017|	271.783|	2937.576|
|Q2|	1.992|	106.022|	1174.962|
|Q3|	2.087|	 55.918|	 544.984|
|Q4|	1.940|	 89.567|	 841.215|
|Q5|	1.972|	 86.884|	 836.173|

The fastest query is Q3, followed by the slightly slower Q4, and Q5.

##### TOP-10 matches

|Query|	    1M|	    10M|	    100M|
|--|----------|--------|------------|
|Q0|	 0.047|	  0.921|	  10.964|
|Q1|	11.222|	159.992|	1590.113|
|Q2|	 0.056|	  0.317|	   3.999|
|Q3|	 3.604|	 32.728|	 327.444|
|Q4|	 0.035|	  0.142|	   1.503|
|Q5|	 0.053|	  0.310|	   3.415|

The fastest query is Q4 followed closely by Q2, and Q5. 

#### Summary

Overall, the winner seems to be between Q2, and Q4, performing well in most 
cases. The advantage of Q2 is that it does not require additional development
since the involved queries are all provided in the official release of Lucene. 
The disadvantage of Q2 compared to Q4 is that it requires the existence of two
Lucene fields instead of one.   