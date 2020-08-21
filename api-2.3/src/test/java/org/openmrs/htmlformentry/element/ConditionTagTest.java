package org.openmrs.htmlformentry.element;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ConditionTagTest extends BaseModuleContextSensitiveTest {
	
	// field names
	private String searchWidgetIdForRequiredCodedCondition = "w7";

	private String statusWidgetIdForRequiredCodedCondition = "w9";
	
	private String onsetDateWidgetIdForRequiredCodedCondition = "w11";
	
	private String searchWidgetIdForRequiredNonCodedCondition = "w14";
	
	private String statusWidgetIdForRequiredNonCodedCondition = "w16";
	
	private String onsetDateWidgetIdForRequiredNonCodedCondition = "w18";
	
	private String endDateWidgetIdForRequiredNonCodedCondition = "w19";
	
	private String searchWidgetIdForPresetCondition = "w21";
	
	private String statusWidgetIdForPresetCondition = "w23";
	
	private String onsetDateWidgetIdForPresetCondition = "w25";
	
	private String endDateWidgetIdForPresetCondition = "w26";
	
	private String searchWidgetIdForPresetConditionWithoutStatus = "w28";
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/RegressionTest-data-openmrs-2.30.xml");
	}
	
	@Test
	public void shouldRecordAndEditCondition() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "conditionForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				
				// setup for current condition
				request.addParameter(searchWidgetIdForRequiredCodedCondition, "Epilepsy");
				request.addParameter(searchWidgetIdForRequiredCodedCondition + "_hid", "3476");
				request.addParameter(statusWidgetIdForRequiredCodedCondition, "active");
				request.addParameter(onsetDateWidgetIdForRequiredCodedCondition, "2014-02-11");
				
				// setup for past condition
				request.addParameter(searchWidgetIdForRequiredNonCodedCondition, "Some required Non-coded Condition");
				request.addParameter(statusWidgetIdForRequiredNonCodedCondition, "inactive");
				request.addParameter(onsetDateWidgetIdForRequiredNonCodedCondition, "2013-02-11");
				request.setParameter(endDateWidgetIdForRequiredNonCodedCondition, "2019-04-11");
				
				// setup for preset condition
				request.addParameter(searchWidgetIdForPresetCondition, "Some preset condition");
				request.addParameter(statusWidgetIdForPresetCondition, "inactive");
				request.addParameter(onsetDateWidgetIdForPresetCondition, "2014-02-11");
				request.setParameter(endDateWidgetIdForPresetCondition, "2020-04-11");
				
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[3]);
				Concept expectedCondition = Context.getConceptService().getConceptByName("Epilepsy");
				
				results.assertNoErrors();
				Assert.assertEquals(3, conditions.length);
				
				Condition currentCondition = conditions[0];
				Assert.assertEquals(ConditionClinicalStatus.ACTIVE, currentCondition.getClinicalStatus());
				Assert.assertEquals(expectedCondition, currentCondition.getCondition().getCoded());
				Assert.assertEquals("2014-02-11", dateAsString(currentCondition.getOnsetDate()));
				Assert.assertNotNull(currentCondition.getId());
				
				Condition pastCondition = conditions[1];
				Assert.assertEquals(ConditionClinicalStatus.INACTIVE, pastCondition.getClinicalStatus());
				Assert.assertEquals("Some required Non-coded Condition", pastCondition.getCondition().getNonCoded());
				Assert.assertEquals("2013-02-11", dateAsString(pastCondition.getOnsetDate()));
				Assert.assertEquals("2019-04-11", dateAsString(pastCondition.getEndDate()));
				Assert.assertNotNull(pastCondition.getId());
				
				Condition presetCondition = conditions[2];
				Assert.assertEquals(ConditionClinicalStatus.INACTIVE, presetCondition.getClinicalStatus());
				Assert.assertEquals("Some preset condition", presetCondition.getCondition().getNonCoded());
				Assert.assertEquals("2014-02-11", dateAsString(presetCondition.getOnsetDate()));
				Assert.assertEquals("2020-04-11", dateAsString(presetCondition.getEndDate()));
				Assert.assertNotNull(presetCondition.getId());
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {

				// setup for current condition
				request.setParameter(onsetDateWidgetIdForRequiredCodedCondition, "2020-02-11");

				// setup for preset condition
				request.addParameter(searchWidgetIdForPresetCondition, "Some preset condition");
				request.removeParameter(statusWidgetIdForPresetCondition);
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				// setup
				Condition[] conditions = results.getEncounterCreated().getConditions().toArray(new Condition[2]);
				
				results.assertNoErrors();
				Assert.assertEquals(2, conditions.length);
				
				Condition currentCondition = conditions[0];
				Assert.assertEquals("2020-02-11", dateAsString(currentCondition.getOnsetDate()));
			}
			
		}.run();
	}

	@Test
	public void shouldInitializeDefaultValues() throws Exception {
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "conditionForm";
			}

			@Override
			public Encounter getEncounterToView() throws Exception {
				return Context.getEncounterService().getEncounter(101);
			}

			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				// Verify for condition
				assertTrue(html.contains("Condition: <span class=\"value\">Edema</span>"));
				// Verify for condition status
				assertTrue(html.contains("Status: <span class=\"value\">inactive</span>"));
				// Verify for onset date
				assertTrue(html.contains("Onset Date: <span class=\"value\">12/01/2017</span>"));
				// Verify for end date
				assertTrue(html.contains("End Date: <span class=\"value\">15/01/2019</span>"));

			}

			@Override
			public Patient getPatientToEdit() {
				return getPatient();
			}

			@Override
			public Encounter getEncounterToEdit() {
				return Context.getEncounterService().getEncounter(101);
			}

			@Override
			public void testEditFormHtml(String html) {
				// Verify the condition default value - 'Edema'
				assertTrue(html.contains(
				    "<input type=\"text\"  id=\"w7\" name=\"w7\"  onfocus=\"setupAutocomplete(this, 'conceptSearch.form','null','Diagnosis','null');\"class=\"autoCompleteText\"onchange=\"setValWhenAutocompleteFieldBlanked(this)\" onblur=\"onBlurAutocomplete(this)\" value=\"Edema\"/>"));
				// Verify the condition status - 'Inactive'
				assertTrue(html.contains(
				    "<input type=\"radio\" id=\"w9_1\" name=\"w9\" value=\"inactive\" checked=\"true\" onMouseDown=\"radioDown(this)\" onClick=\"radioClicked(this)\"/>"));
				// Verify the onset date - '2017-01-12'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w11-display', '#w11', '2017-01-12')</script>"));
				// Verify the end date - '2019-01-15'
				assertTrue(html.contains(
				    "<script>setupDatePicker('dd/mm/yy', '110,20','en-GB', '#w12-display', '#w12', '2019-01-15')</script>"));

			}

		}.run();
	}

}
