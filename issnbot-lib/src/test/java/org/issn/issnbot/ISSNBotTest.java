package org.issn.issnbot;

import java.util.Arrays;
import java.util.HashMap;

import org.issn.issnbot.IssnBot;
import org.issn.issnbot.model.IssnValue;
import org.issn.issnbot.model.SerialEntry;
import org.issn.issnbot.model.SerialEntry.ValueWithReference;
import org.junit.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;

public class ISSNBotTest {

	@Test
	public void test() {
		try {
			IssnBot agent = new IssnBot(System.getProperty("login"), System.getProperty("password"));
			agent.setEditSummary("ISSN Bot test - See https://www.wikidata.org/wiki/Wikidata_talk:WikiProject_Periodicals#Data_donation_from_ISSN_Register_-_Feedback_welcome");
			
			SerialEntry aSerial = new SerialEntry();
			aSerial.setWikidataId("Q84025820");
			aSerial.setIssns(Arrays.asList(new IssnValue[] {
				// online
				new IssnValue("2037-1136", "Medicina subacquea e iperbarica (Online)", Datamodel.makeWikidataItemIdValue("Q1714118")),
				// printed
				new IssnValue("2035-8911", "Medicina subacquea e iperbarica (Testo stampato)", Datamodel.makeWikidataItemIdValue("Q1261026"))
			}));
			aSerial.setTitle(aSerial.new ValueWithReference("Medicina subacquea e iperbarica.", "2037-1136"));
			aSerial.setLang(aSerial.new ValueWithReference("it", "2037-1136"));
			
			agent.updateSerialItem(aSerial);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
