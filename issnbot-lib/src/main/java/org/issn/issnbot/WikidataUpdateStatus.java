package org.issn.issnbot;

import org.issn.issnbot.doc.Description;

public enum WikidataUpdateStatus {

		@Description(definition = "Indicates that a new statement was created in Wikidata.")
		CREATED(1),
		@Description(definition = "Indicate previously synchronized value is updated; "
				+ "for example if a language value was previously synchronized, and is updated in the ISSN registry, "
				+ "the bot will update the previous value instead of creating a new one. This is the case for title and language")
		UPDATED(2),
		@Description(definition = "Indicates that the value already existed in Wikidata, and the ISSN reference was added on it")
		ADDED_REFERENCE(3),
		@Description(definition = "Indicates that the value already existed in Wikidata, and qualifiers were added on it. This can happen only on ISSN statements.")
		ADDED_QUALIFIER(4),
		@Description(definition = "Indicates that the value already existed in Wikidata, with different qualifiers, and qualifiers were updated on it. "
				+ "For example if a distribution format or key title was previously synchronized on an ISSN statement, and is updated in the ISSN registry, "
				+ "the bot will update the previous value of the qualifier. This happens only on ISSN statements.")
		UPDATED_QUALIFIER(5),
		@Description(definition = "Indicates that the ISSN reference value on the statement was updated. "
				+ "This can happen only if the logic of selecting a reference ISSN in the input data changes.")
		UPDATE_REFERENCE(6),
		@Description(definition = "Indicates no update took place.")
		NOTHING(7),
		@Description(definition = "Indicates the value was not provided in the input data.")
		EMPTY(8),
		@Description(definition = "Indicates mixed updates in case of multiple values. This can happen only on official websites and cancelled ISSNs, "
				+ "where multiple values are possible. In that case, the status for each value is also given in the log file in parenthesis.")
		MIXED(10),
		@Description(definition = "Indicates that a plain ISSN statement in Wikidata was indicated as a Cancelled ISSN in the input file. "
				+ "In that case the ISSN statement is marked as deprecated in Wikidata.")
		SET_DEPRECATED(11),
		@Description(definition = "Indicates that an existing statement in Wikidata was marked with a deprecated rank. "
				+ "This happens for cases where we want historical values to be tracked in Wikidata, and not simply deleted, "
				+ "that is for the place of publications and the official website. A change in these values in the input data "
				+ "is not considered as fixing an error, but as a true change in the history of the serial : the publisher changed, "
				+ "so the place of publication chnaged, or the website URL changed. In that case previously synchronized values "
				+ "are marked as deprecated, instead of being deleted.")
		PREVIOUS_VALUE_DEPRECATED(100),
		@Description(definition = "Indicates a previous value was deleted. This can be the case only for ISSNs that were previously synchronized by the bot; "
				+ "in the case an ISSN-L family changes in the ISSN registry and the associated ISSNs are not the same as the one previously synchronized, "
				+ "then these ISSNs are deleted from the item.")
		PREVIOUS_VALUE_DELETED(101),
		@Description(definition = "Indicates a previous value was found in Wikidata, but was left untouched.")
		PREVIOUS_VALUE_UNTOUCHED(102),
		@Description(definition = "Indicates no previous value was found in Wikidata.")
		PREVIOUS_VALUE_NONE(103),
		;
		
		private int order;

		public int getOrder() {
			return order;
		}

		private WikidataUpdateStatus(int order) {
			this.order = order;
		}
}
