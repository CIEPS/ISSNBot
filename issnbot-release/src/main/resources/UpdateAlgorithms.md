# ISSNBot update algorithm
_note : this file is automatically generated from the documentation in `WikidataSerial.java`._

This is the algorithms ISSNBot uses to update Wikidata. They are applied in the order listed here.

## ISSN-L (P7363)

The ISSN-L processing is relatively straightforward because it does not hold the ISSN reference (being an identifier), like title, language etc.:
- If multiple ISSN-L are found on the same item, this is not a situation we can deal with, so this is considered an error.
- If an ISSN-L statement with the same value does not exist, then:
  - Create a new ISSN-L statement on the item, without any reference.


## Label

Label is created if it does not already exists.
- If lang code is mul or mis, don't do anything.
- If there is no label in the provided language, and the title value does not exists as an alias, add it as a label


## Alias

Alias is created if it does not already exists.
- If lang code is mul or mis, don't do anything.
- If the ISSN title does not exist in the provided language neither as a label nor as an alias, add it as an alias


## Title (P1476)

Title and Language processing are similar:
- If a title with the same value (case-insensitive, unicode-encoding insensitive) does not exist, then:
  - if a single title value exists with a proper ISSN reference, it indicates the value in ISSN register has changed : update the existing title statement with ISSN reference.
  - otherwise if no such values exist, create a new title statement on the item, with an ISSN reference.
- Otherwise, if the existing value is the same but does not have proper ISSN reference, add ISSN reference on the existing statement.
- Otherwise, if the existing value already have an ISSN reference, but with a different ISSN, update the reference to the new reference ISSN.
- Otherwise (same value exists, with ISSN reference having correct value), don't do anything.


## Language (P407)

Title and Language processing are similar:
- If the provided language code cannot be translated to a Wikidata QID, then this is an error.
- If a language with the same value does not exist, then:
  - if a single language value exists with a proper ISSN reference, it indicates the value in ISSN register has changed : update the existing statement with ISSN reference.
  - otherwise if no such values exist, create a new language statement on the item, with an ISSN reference.
- Otherwise, if the existing value is the same but does not have proper ISSN reference, add ISSN reference on the existing statement.
- Otherwise, if the existing value already have an ISSN reference, but with a different ISSN, update the reference to the new reference ISSN.
- Otherwise (same value exists, with ISSN reference having correct value), don't do anything.


## Country of origina (P495)

The country of origin processing is specific in that we consider that the place of publication can change over time in ISSN register and history should be tracked by deprecating previous values:
- If the provided country code cannot be translated to a Wikidata QID, then this is an error.
- If a country of origin with the same value does not exist, then:
  - Create a new country of origin statement on the item, with an ISSN reference.
  - Any other existing non-deprecated country of origin statements on the item with an ISSN reference (and a difference value) are marked as deprecated.
- Otherwise, if the existing value is the same but does not have proper ISSN reference, add ISSN reference on the existing statement.
- Otherwise, if the existing value already have an ISSN reference, but with a different ISSN, update the reference to the new reference ISSN.
- Otherwise (same value exists, with ISSN reference having correct value), don't do anything.


## Official Website (P856)

The official website processing is specific in that 1/ we consider that it can change over time and history should be tracked by deprecating previous values and 2/ it is multivalued:
- For every website value to be synchronized:
  - If an official website statement with the same value (ignoring final '/') does not exist, then:
    - Create a new official website statement on the item, with an ISSN reference.
  - Otherwise, if the existing value is the same but does not have proper ISSN reference, add ISSN reference on the existing statement.
  - Otherwise, if the existing value already have an ISSN reference, but with a different ISSN, update the reference to the new reference ISSN.
  - Otherwise (same value exists, with ISSN reference having correct value), don't do anything.
- Then, for every existing official website statement on the item:
  - If its value it not in the values to be synchronized, and it is not deprecated, and it has an ISSN reference, then deprecate it.    We consider a previously synchronized value has changed in the ISSN register, and we deprecated the value.

## Unknown ISSN deletion

It may happen than ISSN gets reorganized in different ISSN-L families. In that case, ISSN values can 'move' on other serials:
- For every ISSN statement on the item:
  - If the statements has 'named as' and 'distribution format' qualifiers (indicating it comes from ISSN register)...
  - AND if that ISSN value is not associated to the serial in the input data, then:
    - Delete that statement.


## Cancelled ISSN (P236 with deprecated rank)

Cancelled ISSNs are wrong identifiers that have once been issued but are incorrect. They are synchronized to Wikidata because references to them may potentially exists.
They are stored as P236 values with a deprecated rank and a 'reason for deprecation' stating 'incorrect identifier'.
- If an ISSN statement, with deprecated rank and with the same value does not exist (not comparing qualifiers), then:
  - If an ISSN statement exists, but not having deprecated rank, set its rank to deprecated.
  - Otherwise, create an ISSN statement with deprecated rank and 'reason for deprecation'='incorrect identifier'
- Otherwise, the value already exists with deprecated rank, so do nothing.


## Unknown Cancelled ISSN deletion (P236 with deprecated rank)

It may happen than Cancelled ISSNs gets reorganized in different ISSN-L families. In that case, Cancelled ISSN values can 'move' on other serials:
- For every ISSN statement on the item:
  - If the statements has 'deprecated' rank and has a reference 'reason for deprecation' equal to 'incorrect identifier value' (indicating a cancelled ISSN), then:
  - AND if value is not in the Cancelled ISSNs associated to the serial in the input data, then:
    - Delete that statement.

