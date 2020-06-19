package org.issn.issnbot;

public enum WikidataUpdateStatus {

		// Indicate that the statement was created in Wikidata
		CREATED(1),
		// Indicate that the statement was created and older values were deprecated
		CREATED_AND_DEPRECATE_OLDER_VALUES(2),
		// Indicate that the value already existed and references were added on it
		ADDED_REFERENCE(3),
		// Indicate that the value already existed and qualifiers were added on it
		ADDED_QUALIFIER(5),
		// Indicate that the ISSN reference on the statement was updated
		UPDATE_REFERENCE_NOT_IMPLEMENTED(6),
		// Indicate the property was deleted
		DELETED(7),
		// Indicate no update took place
		NOTHING(8),
		// Indicate the value was not set in the provided data
		EMPTY(9),
		// Indicate mixed updates in case of multiple values (e.g. offcial websites)
		MIXED(8),
		
		;
		
		private int order;

		public int getOrder() {
			return order;
		}

		private WikidataUpdateStatus(int order) {
			this.order = order;
		}
}
