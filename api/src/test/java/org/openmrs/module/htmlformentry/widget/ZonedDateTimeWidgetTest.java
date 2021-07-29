package org.openmrs.module.htmlformentry.widget;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openmrs.module.htmlformentry.FormEntryContext.Mode.EDIT;
import static org.openmrs.module.htmlformentry.FormEntryContext.Mode.VIEW;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.DATETIME_FALLBACK_FORMAT;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;

public class ZonedDateTimeWidgetTest extends BaseHtmlFormEntryTest {
	
	private ZonedDateTimeWidget widget = new ZonedDateTimeWidget();
	
	private FormEntryContext formEntryContext = mock(FormEntryContext.class);
	
	private AdministrationService administrationService = mock(AdministrationService.class);
	
	@Before
	public void before() throws ParseException {
		widget.setInitialValue(new SimpleDateFormat("yyyy/mm/dd HH:mm:ss Z").parse("2021/01/30 18:09:35 +0300"));
		widget.setHidden(false);
		
		when(formEntryContext.getFieldName(widget)).thenReturn("w1");
		when(administrationService.getGlobalProperty(HtmlFormEntryConstants.GP_TIMEZONE_CONVERSIONS)).thenReturn("true");
		when(administrationService.getGlobalProperty(HtmlFormEntryConstants.FORMATTER_DATETIME, DATETIME_FALLBACK_FORMAT))
		        .thenReturn("dd-MM-yyyy, HH:mm:ss");
		
		//Set Client Timezone
		Context.getAuthenticatedUser().setUserProperty(HtmlFormEntryConstants.UP_CLIENT_TIMEZONE, "Europe/Lisbon");
	}
	
	@Test
	public void generateHtml_shouldDisplaySubmittedDateAsUTCInViewMode() {
		
		// setup
		when(formEntryContext.getMode()).thenReturn(VIEW);
		
		String html = widget.generateHtml(formEntryContext);
		
		// verify
		Assert.assertEquals("<span class=\"value\">30-01-2021, 15:09:35</span>", html);
	}
	
	@Test
	public void generateHtml_shouldDisplaySubmittedDateAsUTCInEditMode() {
		// setup
		when(formEntryContext.getMode()).thenReturn(EDIT);
		// replay
		String html = widget.generateHtml(formEntryContext);
		
		// verify
		Assert.assertEquals(
		    "<input type=\"text\" size=\"10\" id=\"w1-display\"/><input type=\"hidden\" name=\"w1\" id=\"w1\" /><script>setupDatePicker('dd/mm/yy', 'null','en-GB', '#w1-display', '#w1', '2021-01-30')</script><select class=\"hfe-hours\" name=\"w1hours\"><option value=\"0\">00</option><option value=\"1\">01</option><option value=\"2\">02</option><option value=\"3\">03</option><option value=\"4\">04</option><option value=\"5\">05</option><option value=\"6\">06</option><option value=\"7\">07</option><option value=\"8\">08</option><option value=\"9\">09</option><option value=\"10\">10</option><option value=\"11\">11</option><option value=\"12\">12</option><option value=\"13\">13</option><option value=\"14\">14</option><option value=\"15\" selected=\"true\">15</option><option value=\"16\">16</option><option value=\"17\">17</option><option value=\"18\">18</option><option value=\"19\">19</option><option value=\"20\">20</option><option value=\"21\">21</option><option value=\"22\">22</option><option value=\"23\">23</option></select>:<select class=\"hfe-minutes\" name=\"w1minutes\"><option value=\"0\">00</option><option value=\"1\">01</option><option value=\"2\">02</option><option value=\"3\">03</option><option value=\"4\">04</option><option value=\"5\">05</option><option value=\"6\">06</option><option value=\"7\">07</option><option value=\"8\">08</option><option value=\"9\" selected=\"true\">09</option><option value=\"10\">10</option><option value=\"11\">11</option><option value=\"12\">12</option><option value=\"13\">13</option><option value=\"14\">14</option><option value=\"15\">15</option><option value=\"16\">16</option><option value=\"17\">17</option><option value=\"18\">18</option><option value=\"19\">19</option><option value=\"20\">20</option><option value=\"21\">21</option><option value=\"22\">22</option><option value=\"23\">23</option><option value=\"24\">24</option><option value=\"25\">25</option><option value=\"26\">26</option><option value=\"27\">27</option><option value=\"28\">28</option><option value=\"29\">29</option><option value=\"30\">30</option><option value=\"31\">31</option><option value=\"32\">32</option><option value=\"33\">33</option><option value=\"34\">34</option><option value=\"35\">35</option><option value=\"36\">36</option><option value=\"37\">37</option><option value=\"38\">38</option><option value=\"39\">39</option><option value=\"40\">40</option><option value=\"41\">41</option><option value=\"42\">42</option><option value=\"43\">43</option><option value=\"44\">44</option><option value=\"45\">45</option><option value=\"46\">46</option><option value=\"47\">47</option><option value=\"48\">48</option><option value=\"49\">49</option><option value=\"50\">50</option><option value=\"51\">51</option><option value=\"52\">52</option><option value=\"53\">53</option><option value=\"54\">54</option><option value=\"55\">55</option><option value=\"56\">56</option><option value=\"57\">57</option><option value=\"58\">58</option><option value=\"59\">59</option></select><select class=\"hfe-seconds\" name=\"w1seconds\"><option value=\"0\">00</option><option value=\"1\">01</option><option value=\"2\">02</option><option value=\"3\">03</option><option value=\"4\">04</option><option value=\"5\">05</option><option value=\"6\">06</option><option value=\"7\">07</option><option value=\"8\">08</option><option value=\"9\">09</option><option value=\"10\">10</option><option value=\"11\">11</option><option value=\"12\">12</option><option value=\"13\">13</option><option value=\"14\">14</option><option value=\"15\">15</option><option value=\"16\">16</option><option value=\"17\">17</option><option value=\"18\">18</option><option value=\"19\">19</option><option value=\"20\">20</option><option value=\"21\">21</option><option value=\"22\">22</option><option value=\"23\">23</option><option value=\"24\">24</option><option value=\"25\">25</option><option value=\"26\">26</option><option value=\"27\">27</option><option value=\"28\">28</option><option value=\"29\">29</option><option value=\"30\">30</option><option value=\"31\">31</option><option value=\"32\">32</option><option value=\"33\">33</option><option value=\"34\">34</option><option value=\"35\" selected=\"true\">35</option><option value=\"36\">36</option><option value=\"37\">37</option><option value=\"38\">38</option><option value=\"39\">39</option><option value=\"40\">40</option><option value=\"41\">41</option><option value=\"42\">42</option><option value=\"43\">43</option><option value=\"44\">44</option><option value=\"45\">45</option><option value=\"46\">46</option><option value=\"47\">47</option><option value=\"48\">48</option><option value=\"49\">49</option><option value=\"50\">50</option><option value=\"51\">51</option><option value=\"52\">52</option><option value=\"53\">53</option><option value=\"54\">54</option><option value=\"55\">55</option><option value=\"56\">56</option><option value=\"57\">57</option><option value=\"58\">58</option><option value=\"59\">59</option></select><input type=\"hidden\" class=\"hfe-timezone\" name=\"w1timezone\"></input>",
		    html);
	}
	
}
