package org.openmrs.module.htmlformentry.element;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.LocationWidget;
import org.openmrs.module.htmlformentry.widget.PersonStubWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;

/**
 * Holds the widgets used to represent an Encounter details, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for Encounter details.
 */
public class EncounterDetailSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private DateWidget dateWidget;
	private ErrorWidget dateErrorWidget;
	private TimeWidget timeWidget;
	private ErrorWidget timeErrorWidget;
	private PersonStubWidget providerWidget;
	private ErrorWidget providerErrorWidget;
	private LocationWidget locationWidget;
	private ErrorWidget locationErrorWidget;
	private CheckboxWidget voidWidget;
	private ErrorWidget voidErrorWidget;

	/**
	 * Construct a new EncounterDetailSubmissionElement
	 * @param context
	 * @param parameters
	 */
	public EncounterDetailSubmissionElement(FormEntryContext context, Map<String, Object> parameters) {
		
		// Register Date and Time widgets, if appropriate
		if (Boolean.TRUE.equals(parameters.get("date"))) {
			
			dateWidget = new DateWidget();
			dateErrorWidget = new ErrorWidget();
			
			if (context.getExistingEncounter() != null) {
				dateWidget.setInitialValue(context.getExistingEncounter().getEncounterDatetime());
			} 
			else if (parameters.get("defaultDate") != null) {
				dateWidget.setInitialValue(parameters.get("defaultDate"));
			}
			
			if (parameters.get("disallowMultipleEncountersOnDate") != null && StringUtils.hasText((String) parameters.get("disallowMultipleEncountersOnDate"))){
				dateWidget.setOnChangeFunction("existingEncounterOnDate(this, '" + parameters.get("disallowMultipleEncountersOnDate") + "') ");
			}
			
			if ("true".equals(parameters.get("showTime"))) {
				timeWidget = new TimeWidget();
				timeErrorWidget = new ErrorWidget();
				if (context.getExistingEncounter() != null) {
					timeWidget.setInitialValue(context.getExistingEncounter().getEncounterDatetime());
				} 
				else if (parameters.get("defaultDate") != null) {
					timeWidget.setInitialValue(parameters.get("defaultDate"));
				}
				context.registerWidget(timeWidget);
				context.registerErrorWidget(timeWidget, timeErrorWidget);
			}
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, dateErrorWidget);
		}
		
		// Register Provider widgets, if appropriate
		if (Boolean.TRUE.equals(parameters.get("provider"))) {
			
			providerWidget = new PersonStubWidget();
			providerErrorWidget = new ErrorWidget();
			
			List<PersonStub> options = new ArrayList<PersonStub>();
//			boolean sortOptions = false;
			
			// If specific persons are specified, display only those persons in order
			String personsParam = (String)parameters.get("persons");
			if (personsParam != null) {
				for (String s : personsParam.split(",")) {
					Person p = HtmlFormEntryUtil.getPerson(s);
					if (p == null) {
						throw new RuntimeException("Cannot find Person: " + s);
					}
					options.add(new PersonStub(p));
				}
				removeNonProviders(options);
			}
			
			// Only if specific person ids are not passed in do we get by user Role
			if (options.isEmpty()) {
				
				List<PersonStub> users = new ArrayList<PersonStub>();
				
				// If the "role" attribute is passed in, limit to users with this role
				if (parameters.get("role") != null) {
					Role role = Context.getUserService().getRole((String) parameters.get("role"));
					if (role == null) {
						throw new RuntimeException("Cannot find role: " + parameters.get("role"));
					}
					else {
						users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(role.getRole());
					}
				}
				
				// Otherwise, use default options appropriate to the underlying OpenMRS version 
				else {
					if (openmrsVersionDoesNotSupportProviders()) {
						// limit to users with the default OpenMRS PROVIDER role,
						String defaultRole = OpenmrsConstants.PROVIDER_ROLE;
						Role role = Context.getUserService().getRole(defaultRole);
						if (role != null) {
							users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(role.getRole());
						}
						// If this role isn't used, default to all Users
						if (users.isEmpty()) {
							users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(null);
						}
					}
					else {
						// in OpenMRS 1.9+, get all suitable providers
						users = getAllProvidersThatArePersonsAsPersonStubs();
					}
				}
				options.addAll(users);
//				sortOptions = true;
			}
			
			// Set default values as appropriate
			Person defaultProvider = null;
			if (context.getExistingEncounter() != null) {
				defaultProvider = context.getExistingEncounter().getProvider();
				if (!options.contains(new PersonStub(defaultProvider))) {
					options.add(new PersonStub(defaultProvider));
				}
			}
			else {
				String defParam = (String) parameters.get("default");
				if (StringUtils.hasText(defParam)) {
					if ("currentuser".equalsIgnoreCase(defParam)) {
						defaultProvider = Context.getAuthenticatedUser().getPerson();
					} 
					else {
						defaultProvider = HtmlFormEntryUtil.getPerson(defParam);
					}
					if (defaultProvider == null) {
						throw new IllegalArgumentException("Invalid default provider specified for encounter: " + defParam);
					}
				}
			}
			
//			if (sortOptions) {
//				Collections.sort(options, new PersonByNameComparator());
//			}
			
			providerWidget.setOptions(options);
			providerWidget.setInitialValue(new PersonStub(defaultProvider));
			
			context.registerWidget(providerWidget);
			context.registerErrorWidget(providerWidget, providerErrorWidget);
		}
		
		// Register Location widgets, if appropriate
		if (Boolean.TRUE.equals(parameters.get("location"))) {
			
			locationWidget = new LocationWidget();
			locationErrorWidget = new ErrorWidget();
			
			// If the "order" attribute is passed in, limit to the specified locations in order
			if (parameters.get("order") != null) {
				List<Location> locations = new ArrayList<Location>();
				String[] temp = ((String) parameters.get("order")).split(",");
				for (String s : temp) {
					Location loc = HtmlFormEntryUtil.getLocation(s);
					if (loc == null) {
						throw new RuntimeException("Cannot find location: " + loc);
					}
					locations.add(loc);
				}
				locationWidget.setOptions(locations);
			}
			
			// Set default values
			Location defaultLocation = null;
			if (context.getExistingEncounter() != null) {
				defaultLocation = context.getExistingEncounter().getLocation(); 
			} 
			else {
				String defaultLocId = (String) parameters.get("default");
				if (StringUtils.hasText(defaultLocId)) {
					defaultLocation = HtmlFormEntryUtil.getLocation(defaultLocId);
				}
			}
			defaultLocation = defaultLocation == null ? context.getDefaultLocation() : defaultLocation;
			locationWidget.setInitialValue(defaultLocation);
			context.registerWidget(locationWidget);
			context.registerErrorWidget(locationWidget, locationErrorWidget);
		}
		
		if (Boolean.TRUE.equals(parameters.get("showVoidEncounter")) && context.getMode() == Mode.EDIT) { //only show void option if the encounter already exists.  And VIEW implies not voided.
			voidWidget = new CheckboxWidget();
			voidWidget.setLabel(" " + Context.getMessageSourceService().getMessage("general.voided"));
			voidErrorWidget = new ErrorWidget();
			if (context.getExistingEncounter() != null && context.getExistingEncounter().isVoided().equals(true))
				voidWidget.setInitialValue("true");
			context.registerWidget(voidWidget);
			context.registerErrorWidget(voidWidget, voidErrorWidget);
		}
		
	}
	
    /**
     * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
     * without having to branch the module. We should remove this method when do a proper
     * implementation.
     * 
     * @return
     */
    private boolean openmrsVersionDoesNotSupportProviders() {
        return OpenmrsConstants.OPENMRS_VERSION_SHORT.startsWith("1.6") || OpenmrsConstants.OPENMRS_VERSION_SHORT.startsWith("1.7") || OpenmrsConstants.OPENMRS_VERSION_SHORT.startsWith("1.8");
    }

    /**
     * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
     * without having to branch the module. We should remove this method when do a proper
     * implementation. 
     * 
     * @param options
     */
    private void removeNonProviders(List<PersonStub> persons) {
        if (openmrsVersionDoesNotSupportProviders())
            return;
        Set<Integer> legalPersonIds = getAllProviderPersonIds();
        for (Iterator<PersonStub> i = persons.iterator(); i.hasNext(); ) {
            PersonStub candidate = i.next();
            if (!legalPersonIds.contains(candidate.getId()))
                i.remove();
        }
    }

    /**
     * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
     * without having to branch the module. We should remove this method when do a proper
     * implementation. 
     * 
     * @return all providers that are attached to persons
     */
    private List<Object> getAllProvidersThatArePersons() {
    	if (openmrsVersionDoesNotSupportProviders())
    		throw new RuntimeException("Programming error in HTML Form Entry module. This method should not be called before OpenMRS 1.9.");
    	try {
    		Object providerService = Context.getService(Context.loadClass("org.openmrs.api.ProviderService"));
    		Method getProvidersMethod = providerService.getClass().getMethod("getAllProviders");
    		@SuppressWarnings("rawtypes")
            List allProviders = (List) getProvidersMethod.invoke(providerService);
    		List<Object> ret = new ArrayList<Object>();
    		for (Object provider : allProviders) {
    			Person person = (Person) PropertyUtils.getProperty(provider, "person");
    			if (person != null)
    				ret.add(provider);
    		}
    		return ret;
    	}
    	catch (Exception ex) {
    		throw new RuntimeException("Programming error in HTML Form Entry module. This method should be safe!", ex);
    	}
    }

    /**
     * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
     * without having to branch the module. We should remove this method when do a proper
     * implementation. 
     * 
     * @return person stubs for all providers that are attached to persons
     */
    private List<PersonStub> getAllProvidersThatArePersonsAsPersonStubs() {
    	try {
    		List<PersonStub> ret = new ArrayList<PersonStub>();
    		for (Object provider : getAllProvidersThatArePersons()) {
    			Person person = (Person) PropertyUtils.getProperty(provider, "person");
    			ret.add(new PersonStub(person));
    		}
    		return ret;
    	} catch (Exception ex) {
    		throw new RuntimeException("Programming error in HTML Form Entry module. This method should be safe!", ex);
    	}
    }
    
    /**
     * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
     * without having to branch the module. We should remove this method when do a proper
     * implementation. 
     * 
     * @return personIds of all providers that are attached to persons
     */
    private Set<Integer> getAllProviderPersonIds() {
    	try {
    		Set<Integer> ret = new HashSet<Integer>();
    		for (Object candidate : getAllProvidersThatArePersons()) {
    			Person person = (Person) PropertyUtils.getProperty(candidate, "person");
    			if (person != null)
    				ret.add(person.getPersonId());
    		}
    		return ret;
    	}
    	catch (Exception ex) {
    		throw new RuntimeException("Programming error in HTML Form Entry module. This method should be safe!", ex);
    	}
    }
        
	/**
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
	 */
	@Override
    public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		if (dateWidget != null) {
			ret.append(dateWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(dateErrorWidget.generateHtml(context));
		}
		if (timeWidget != null) {
			ret.append("&nbsp;");
			ret.append(timeWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(timeErrorWidget.generateHtml(context));
		}
		if (providerWidget != null) {
			ret.append(providerWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(providerErrorWidget.generateHtml(context));
		}
		if (locationWidget != null) {
			ret.append(locationWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(locationErrorWidget.generateHtml(context));
		}
		if (voidWidget != null){
			if (context.getMode() == Mode.EDIT) //only show void option if the encounter already exists.
				ret.append(voidWidget.generateHtml(context));
		}
		return ret.toString();
	}

	/**
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		
		try {
			if (dateWidget != null) {
				Date date = (Date) dateWidget.getValue(context, submission);
				if (timeWidget != null) {
					Date time = (Date) timeWidget.getValue(context, submission);
					date = HtmlFormEntryUtil.combineDateAndTime(date, time);
				}
				if (date == null)
					throw new Exception("htmlformentry.error.required");
				if (OpenmrsUtil.compare((Date) date, new Date()) > 0)
					throw new Exception("htmlformentry.error.cannotBeInFuture");
			}
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(context
					.getFieldName(dateErrorWidget), Context
					.getMessageSourceService().getMessage(ex.getMessage())));
		}

		try {
			if (providerWidget != null) {
				Object provider = providerWidget.getValue(context, submission);
				if (provider == null)
					throw new Exception("required");
			}
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(context
					.getFieldName(providerErrorWidget), Context
					.getMessageSourceService().getMessage(ex.getMessage())));
		}
		
		try {
			if (locationWidget != null) {
				Object location = locationWidget.getValue(context, submission);
				if (location == null)
					throw new Exception("required");
			}
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(context
					.getFieldName(locationErrorWidget), Context
					.getMessageSourceService().getMessage(ex.getMessage())));
		}
		return ret;
	}

	/**
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		if (dateWidget != null) {
			Date date = (Date) dateWidget.getValue(session.getContext(), submission);
			session.getSubmissionActions().getCurrentEncounter().setEncounterDatetime(date);
		}
		if (timeWidget != null) {
			Date time = (Date) timeWidget.getValue(session.getContext(), submission);
			Encounter e = session.getSubmissionActions().getCurrentEncounter();
			Date dateAndTime = HtmlFormEntryUtil.combineDateAndTime(e.getEncounterDatetime(), time);
			e.setEncounterDatetime(dateAndTime);
		}
		if (providerWidget != null) {
			Person person = (Person) providerWidget.getValue(session.getContext(), submission);
			session.getSubmissionActions().getCurrentEncounter().setProvider(person);
		}
		if (locationWidget != null) {
			Location location = (Location) locationWidget.getValue(session.getContext(), submission);
			session.getSubmissionActions().getCurrentEncounter().setLocation(location);
		}
		if (voidWidget != null){
			if ("true".equals(voidWidget.getValue(session.getContext(), submission))){	
				session.setVoidEncounter(true);
			} else if ("false".equals(voidWidget.getValue(session.getContext(), submission))){
				//nothing..  the session.voidEncounter property will be false, and the encounter will be un-voided if already voided
				//otherwise, nothing will happen.  99% of the time the encounter won't be voided to begin with.
			}
		}
	}
}
