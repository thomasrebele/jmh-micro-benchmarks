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

# Benchmark SQL operators in Lucene

In this document, we present a benchmark with alternative ways to create and evaluate the following
SQL operators:
* NOT EQUAL (`<>`, `!=`);
* IS NULL;
* IS NOT NULL;

over data residing in Lucene indexes.

The experiments cover the cases of string and integer fields. The other numeric fields provided
in Lucene should follow more or less the trends observed for the integer values.

Note that the score of the documents, which usually plays an important role in the context of
information retrieval, is not present in most SQL use-cases.

The reported times are in milliseconds and it is the average of 5 iterations.

Lucene reads/writes data directly to/from the filesystem. There are data structures and caches
that remain on the JVM Heap but most of the data remain off heap. As in other data intensive
processes (DBMS) data can be found in the filesystem caches. There is no effort to clear the
caches before/after each experiment so the numbers reported here are with hot caches.

## System setup

The system used to perform this experiments has an Intel Core i7-8850H CPU at 2.60GHz × 12, 64GB
of DDR4 ram, a Samsung NVMe SSD disk. The OS is Ubuntu 19.10, and the JDK used is 1.8.0_212. The
 JVM max heap size is set to 16GB although the used heap rarely goes over 1GB. 

## Index setup

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
holding customers, we may not always have a value for the `age` column. In a relational setting,
the value in such a column is `null`. In Lucene, the field simply does not exist for a given
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

### String fields  

For string values we use the following types of Lucene fields:
* `StringField`
* `SortedDocValuesField`

High-cardinality fields are populated with string values of length 20 having as prefix the document
id and as suffix random ASCII (for readability purposes) characters. The document id prefix makes
the values mostly unique.

Low-cardinality fields are populated with the string values `TRUE` and `FALSE` simulating a
boolean column that is represented as a string.  

## Search mode

Lucene offers various search APIs accepting a query, which demonstrate different performance
characteristics. In these experiments we focus on three common use cases.

### All matches

Find all documents matching the query. 

The search is performed via `IndexSearcher#search(Query, Collector)` method and passing in a
collector that goes over all documents (`ScoreMode.COMPLETE_NO_SCORES`). In these experiments, we
use a collector that does nothing with the matches. In practice, this collector is useless but it
can provide a lower bound for the performance of the query. It is also a straightforward way to
compare the performance of the queries and respective indexes assuming that sooner or later all 
matches need to be retrieved.    

### TOP-10 matches

Find all documents matching the query and return 10 with the highest score. 

The search is performed via `IndexSearcher#search(Query, int)` method. This is a very common use
case in informational retrieval but less interesting in this setting where the score is not
necessary.

### Sort-10 matches

Find all documents matching the query, sort them based on the value of a field used in the query
and return the first 10. 

The search is performed via `IndexSearcher#search(Query, int, Sort)` method. Roughly this 
corresponds to an SQL query with an `ORDER BY` and `LIMIT` clause. These kind of queries appear 
often in cases evolving stateless pagination.

Note that if all matches need to be retrieved, then this method will always be slower compared to
the one with the collector. Furthermore, the measurements are more biased since the execution time
includes sort overhead. 

## IS NOT NULL benchmark

This section presents the experiments of creating and evaluating the `IS NOT NULL` operator.

The `IS NOT NULL` operator on both kind of fields returns the same number of results (~90% of the
total number of documents) since the percentage of null values is the same. 

### Integer fields

For the presentation of the Lucene queries, we assume the SQL query to be `age IS NOT NULL`.

#### Queries

##### Q0
Using only `IntPoint` field.

    age:[-2147483648 TO 2147483647]

##### Q1
Using only `NumericDocValues` field.

    age:[-2147483648 TO 2147483647]

##### Q2
Using only `NumericDocValues` field.

    DocValuesFieldExistsQuery [field=age]

#### Results

##### High-cardinality

|Query|1M|10M|100M|
|--|-----|-------|------|
|Q0|4.112|79.519|806.568|
|Q1|4.875|86.879|926.187|
|Q2|4.966|42.635|548.517|

##### Low-cardinality

|Query|1M|10M|100M|
|--|-----|-------|------|
|Q0|4.008|50.934|511.893|
|Q1|4.772|77.677|778.681|
|Q2|5.107|42.711|548.465|

#### Summary

For 1M documents, all the queries perform roughly the same and the difference is in the order of
nanoseconds.

For 10M documents and more, the fastest query is Q2, and the slowest is Q1. Q2 relies on a pure
iterator over all documents without additional comparisons as it happens to be the case for Q0, and
Q1. 

For low cardinality fields, Q0 performs better than Q2. Q0 uses a KD-tree, so one possible
explanation is that all documents fall into the same bucket of the KD-tree. Due to this, the number
of comparisons required to find the documents is minimal and thus insignificant compared to
fetching and iterating through the matches.  

### String fields

For the presentation of the Lucene queries, we assume the SQL query to be `firstname IS NOT NULL`.

#### Queries

##### Q0
Using only `StringField`.

    firstName:*

##### Q1
Using only `SortedDocValuesField`.

    firstName:{* TO *}

##### Q2 
Using only `SortedDocValuesField`.

    DocValuesFieldExistsQuery [field=firstName]
#### Results
 
##### High-cardinality

|Query|1M|10M|100M|
|--|-----|-------|--------|
|Q0|6.289|445.478|4409.846|
|Q1|3.845|51.017|484.860|
|Q2|4.855|44.436|472.200|

##### Low-cardinality

|Query|1M|10M|100M|
|--|-----|-------|--------|
|Q0|4.466|76.463|747.504|
|Q1|3.805|73.675|461.464|
|Q2|3.877|43.563|468.615|

#### Summary

The fastest query is Q2.

For high-cardinality fields Q2, and Q1, are 9x faster than Q0. Roughly this means that 
traversing the inverse index is slower than sequentially scanning all documents using the doc
value storage.
  
For low-cardinality fields the difference is significantly smaller. The fact that we have only
two distinct values means that the inverse index has only two entries with a long list of postings.
Essentially this makes all queries behave exactly like a sequential scan so we observe that the
performance of Q0 drops to the same order of magnitude with Q1, and Q2.

Last we can observe that the performance of Q2 is stable between high/low cardinality fields and
depends only on the number of matching documents.  

## IS NULL benchmark

This section presents the experiments of creating and evaluating the `IS NULL` operator.

The `IS NULL` operator on both kind of fields returns the same number of results (~10% of the
total number of documents) since the percentage of null values is the same. 

### Integer fields

For the presentation of the Lucene queries, we assume the SQL query to be `age IS NULL`.

#### Queries

##### Q0
Using only `IntPoint` field.

    +*:* -age:[-2147483648 TO 2147483647]

##### Q1
Using only `NumericDocValues` field.

    +*:* -age:[-2147483648 TO 2147483647]

##### Q2
Using only `NumericDocValues` field.

    +*:* -DocValuesFieldExistsQuery [field=age]

#### Results

##### High-cardinality

|Query|1M|10M|100M|
|--|-----|-------|------|
|Q0|0.744|96.571|1024.918|
|Q1|0.612|157.778|1600.494|
|Q2|0.775|74.914|712.817|

##### Low-cardinality

|Query|1M|10M|100M|
|--|-----|-------|------|
|Q0|0.714|68.790|693.346|
|Q1|0.626|129.497|1323.750|
|Q2|0.753|72.947|717.596|

### String fields

For the presentation of the Lucene queries, we assume the SQL query to be `firstname IS NULL`.

#### Queries

##### Q0
Using only `StringField`.

    +*:* -firstName:*

##### Q1
Using only `SortedSetDocValuesField`.

    +*:* -firstName:{* TO *}

##### Q2 
Using only `SortedSetDocValuesField`.

    +*:* -DocValuesFieldExistsQuery [field=firstName]
    
#### Results
 
##### High-cardinality
|Query|1M|10M|100M|
|--|-----|-------|--------|
|Q0|0.771|529.927|4792.408|
|Q1|0.791|71.965|782.578|
|Q2|0.778|69.607|720.868|


##### Low-cardinality
|Query|1M|10M|100M|
|--|-----|-------|--------|
|Q0|1.159|129.043|1195.973|
|Q1|0.729|70.072|696.629|
|Q2|0.721|75.998|831.797|

### Summary
The performance of queries follows more or less the same trends that were observed for the 
`IS NOT NULL` operator on integer and string fields respectively. The evaluation of the `IS NULL` 
operator is slower than the `IS NOT NULL`; in every case the query is more complex since it needs
to consider every document in the index in order to implement the exclusion. 

## NOT EQUAL benchmark

This section presents the experiments of creating and evaluating the `NOT EQUAL` operator.

The operators strictly follow the SQL semantics. For instance, `age <> 28` returns all documents
with values that are not equal to 28 but not those documents that do not have a value at all for
this field; in other words excluding `null` values.
 
### Integer fields

For the presentation of the Lucene queries, we assume the SQL query to be `age <> 28`. 

Note that the value `28` is not the actual value that was used in the measurements.

The choice of the query alternatives both for integer and string fields may surprise some people,
especially those very familiar with Lucene. They are included in the benchmark since they were
used in production systems at some point in time or were found in online forums as ways to evaluate
the `NOT EQUAL` operator.

#### Queries

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

#### Results
 
##### High-cardinality

|Query|    1M|       10M|       100M|
|--|---------|--------- |-----------|
|Q0|	5.394|	 369.586|	3952.192|
|Q1|	4.561|	  78.140|	 797.092|
|Q2|	4.741|	  79.278|	 819.515|
|Q3|	3.777|	 311.542|	2349.370|
|Q4|	5.183|	  52.181|	 527.773|
|Q5|	5.579|	 194.481|	1822.767|

##### Low-cardinality

|Query|	   1M|	    10M|	    100M|
|--|----------|--------|------------|
|Q0|	5.421|	316.957|	3136.672|
|Q1|	5.092|	 56.783|	 570.193|
|Q2|	5.613|	 79.070|	 778.146|
|Q3|	3.777|	295.749|	2478.469|
|Q4|	6.055|	 48.745|	 489.782|
|Q5|	5.647|	167.275|	1710.167|

#### Summary

For 1M documents, all the queries perform roughly the same and the difference is in the order of
nanoseconds.

For 10M documents and more, Q1, Q2, and Q4, are orders of magnitude faster than the rest, with Q4
being the fastest.

Q0 is the worst alternative. Using some first order logic it can be shown that Q0 can be reduced
to Q1. It is normal that by removing redundant clauses the performance of the query is improved. 

Note that high-selective queries on point values, such as `age:[28 TO 28]`, are very efficient since
we can locate very fast the matching documents with very few value comparisons by using the KD-index
structure. The costly part becomes reading the values from the filesystem and iterating through
the matches; if there are not many then the overall query is extremely fast. This kind of
query appears as part of Q0, Q1, and Q4. Given that this part of the query is extremely fast to
evaluate the overall performance is dominated by the rest of the query. 

Between Q1 and Q4, the comparison boils down to which is the more efficient way to obtain and
iterate through all matches. As we have seen also for the case of the `IS NOT NULL` operator
passing from doc values (Q4) requires fewer comparisons than passing by the KD-index to find
matching documents (Q1). 

### String fields

For the presentation of the Lucene queries, we assume the SQL query to be `firstname <> 'Victor'`.

#### Queries

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

#### Results

##### High-cardinality

|Query|	   1M|  	10M|	    100M|
|--|---------|---------|------------|
|Q0|	6.790|	953.668|	9122.548|
|Q1|	6.477|	464.143|	4497.644|
|Q2|	5.958|	 51.219|   	 489.681|
|Q3|	5.866|	487.136|	4706.604|
|Q4|	5.881|	 97.157|	 970.654|
|Q5|	5.839|	 84.752|	 813.002|

##### Low-cardinality

|Query|	   1M|	    10M|	    100M|
|--|---------|---------|------------|
|Q0|	3.210|	381.705|	3663.213|
|Q1|	3.017|	271.783|	2937.576|
|Q2|	1.992|	106.022|	1174.962|
|Q3|	2.087|	 55.918|	 544.984|
|Q4|	1.940|	 89.567|	 841.215|
|Q5|	1.972|	 86.884|	 836.173|

#### Summary

For 1M documents, the performance difference between queries is modest (up to 1.6X between the
worst and the best).  

For 10M documents and more, we can observe significant differences going up to 18X between the worst
and the best query.

The comparison of queries for both high and low selective fields cannot be performed since the
total number of results retrieved by the queries is different. 

For high-selective fields, the best query is Q2. Similar to point values, high-selective queries
over the term index, such as `firstName:Victor`, are very efficient. Using the trie structure
matching documents can be found efficiently with very few comparisons so the costly operation
becomes reading and iterating through the document ids. In this case the query matches only one
document so the reading cost is negligible. The performance bottleneck for Q2 is to obtain all
non-null documents via the doc values iterator. As we have seen previously (in the experiments of
the `IS NOT NULL`) iterating through the doc values is stable between high and low selective
queries and depends only on the number of documents.

For low-selective fields, the best query is Q3. The term index is very small (there are only two
distinct values) so Q3 can very quickly get rid of 1/2 of the documents. The costly part is
actually gathering and iterating through the matching document ids. The main disadvantage of Q3 is
that it is not provided in the official release of Lucene and needs more development effort
and testing to be used in practice. Right afterwards comes Q5 relying on doc values. As we can
observe the performance of Q5 is not affected by the cardinality of the field since we have to
iterate through all documents and decide to include or exclude the document. Q4 relies also on
doc values and can be faster in some other scenarios, but has also the disadvantage that is not
part of the official release.