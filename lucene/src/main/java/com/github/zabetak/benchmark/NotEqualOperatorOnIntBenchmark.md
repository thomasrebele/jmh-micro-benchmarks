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

# Benchmark inequality queries on integers in Lucene

A benchmark of various ways to create and evaluate the SQL not equal operator `<>` on integer values
residing in Lucene indexes.

The not equal comparison strictly follows the SQL semantics, that is `field <> 5` returns all
documents with values that are not equal to 5 but not those documents that do not have a value for
this field; in other words excluding `null` values.

## Setup

### Index

We create a single Lucene index comprised from two kind of fields: 
* High-cardinality fields populated with integer values uniformly distributed in the range `0` to
`Integer.MAX_VALUE`. They simulate use-cases where the number of values in the field are mostly
 unique such as primary keys in relational tables.
* Low-cardinality fields populated with integer values uniformly distributed in the range `0` to
`110`. They simulate use-cases where the number of distinct values is rather small. In a table
holding people this could be a field holding the _age_.

For each category the index has three fields with the following types:
* `IntPoint`
* `NumericDocValuesField`
* `StoredField`

in order to allow the comparison of different kind of queries. For the same document, the three
fields are populated with the same value.

The number of documents to populate the index is configurable. In these experiments, we present
the results for indexes of size 1M, 10M, and 100M. It is impractical to create indexes of size 100M
at once so there intermediate commits every 100K documents. For the sake of the experiments, we
 configured Lucene to not merge any segments (using `NoMergePolicy.INSTANCE`).  
 
In real world, we do not always have certain information available at all times. In a table
holding customers, we may not always have a value for the `age` column. In a relational setting
, the value in such a column is `null`. In Lucene, the field simply does not exist for a given
document but the outcome is the same. Since we want to measure the performance of queries, taking
also into account `null` values, we left the `10%` of the total documents empty.
     
### Queries

In the sequel, we present the queries that were used in these experiments as strings (using the
`Query#toString()` method). For presentation purposes, we assume that the SQL equivalent is `age
 <> 28`.

#### Q0
Using the `IntPoint` field.

    +*:* -(age:[28 TO 28] (+*:* -age:[-2147483648 TO 2147483647]))
#### Q1
Using the `IntPoint` field.

    +age:[-2147483648 TO 2147483647] -age:[28 TO 28]
#### Q2
Using the `IntPoint` field.

    age:[-2147483648 TO 27] age:[29 TO 2147483647]
#### Q3
Using the `NumericDocValuesField` field.

    age:[-2147483648 TO 27] age:[29 TO 2147483647]
#### Q4
Using both `NumericDocValuesField` and `IntPoint` field.

    +DocValuesFieldExistsQuery [field=age] -age:[28 TO 28]
#### Q5
Using only `NumericDocValuesField`.

    +DocValuesFieldExistsQuery [field=age] -age:[28 TO 28]

## Results 

### Queries on high-cardinality fields
 
|Query|1000000|10000000 | 100000000 |
|--|---------|--------- |-----------|
|Q0|	5.394|	369.586 |	3952.192|
|Q1|	4.561|	78.140  |	797.092 |
|Q2|	4.741|	79.278  |	819.515 |
|Q3|	3.777|	311.542 |	2349.370|
|Q4|	5.183|	52.181  |	527.773 |
|Q5|	5.579|	194.481 |	1822.767|