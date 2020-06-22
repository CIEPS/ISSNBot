package org.issn.issnbot;

public enum WikidataUpdateStatus {

		// Indicate that the statement was created in Wikidata
		CREATED(1),
		// Indicate previously synchronized value is updated
		UPDATED(2),
		// Indicate that the value already existed and references were added on it
		ADDED_REFERENCE(3),
		// Indicate that the value already existed and qualifiers were added on it
		ADDED_QUALIFIER(4),
		// Indicate that the value already existed and qualifiers were updated on it
		UPDATED_QUALIFIER(5),
		// Indicate that the ISSN reference on the statement was updated
		UPDATE_REFERENCE(6),
		// Indicate no update took place
		NOTHING(7),
		// Indicate the value was not set in the provided data
		EMPTY(8),
		// Indicate mixed updates in case of multiple values (e.g. offcial websites)
		MIXED(10),
		
		// Indicate that old values were deprecated
		PREVIOUS_VALUE_DEPRECATED(100),
		// Indicate the previous value was deleted
		PREVIOUS_VALUE_DELETED(101),
		// Indicate the previous value was ledt untouched
		PREVIOUS_VALUE_UNTOUCHED(102),
		// Indicate no previous value was found
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
