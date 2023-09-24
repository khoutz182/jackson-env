package com.elmsoftware.env.settingproviderimpl;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class AwsSsmSettingProviderTest {

	@Mock
	private Consumer<ProviderExceptionHandler.ExceptionInfo> handler;

	@Mock
	private AWSSimpleSystemsManagement ssm;

	private AwsSsmSettingProvider provider;

	@Captor
	private ArgumentCaptor<GetParametersRequest> getParametersRequestArgumentCaptor;

	@Before
	public void beforeAwsSsmSettingProviderTest() {
		provider = new AwsSsmSettingProvider(ssm, "prefix");
	}

	@After
	public void after() {
		verifyNoMoreInteractions(ssm);
	}

	@Test
	public void should_get_value_from_ssm_with_default_prefix() {

		// setup
		provider = new AwsSsmSettingProvider(ssm, "prefix");

		final String expected = "snapped-the-frame";
		final GetParametersResult parameterResult = new GetParametersResult()
			.withParameters(
				new Parameter()
					.withValue(expected)
					.withName("/foo/prefix/my-key")
			);

		when(ssm.getParameters(any())).thenReturn(parameterResult);

		// run test
		final String actual = provider.getProperty("foo", "my-key");

		// verify mocks / capture values
		verify(ssm).getParameters(getParametersRequestArgumentCaptor.capture());

		// assert results

		// did we get the expected value?
		assertEquals(expected, actual);

		// was the request what we expected?
		final List<String> nameList = getParametersRequestArgumentCaptor.getValue().getNames();
		assertTrue(nameList.contains("/foo/prefix/my-key"));

	}

	@Test
	public void should_blow_if_setting_can_not_be_found() {

		// setup
		final String[] expectedValues = {
			"/fml/prefix/snapped-the-frame",
			"/fml/snapped-the-frame",
			"/global/snapped-the-frame"
		};

		when(ssm.getParameter(any())).thenThrow(new RuntimeException("no parameter defined"));

		// run test
		try {
			provider.getProperty("fml", "snapped-the-frame");
			fail("no parameter was found - this should have failed.");
		} catch (final Exception e) {
			// this is expected
			assertEquals(
				"unable to find parameter using pattern /fml/prefix/snapped-the-frame",
				e.getLocalizedMessage()
			);
		}

		// verify mocks / capture values
		verify(ssm).getParameters(getParametersRequestArgumentCaptor.capture());

		// assert results
		final List<String> nameList = getParametersRequestArgumentCaptor.getValue().getNames();

		for (final String value : expectedValues) {
			assertTrue("name list should include '" + value + "'", nameList.contains(value));
		}

	}

	@Test
	public void should_use_custom_exception_handler() {

		provider = new AwsSsmSettingProvider(ssm, "prefix", handler);

		// setup
		final String[] expectedValues = {
			"/fml/prefix/snapped-the-frame",
			"/fml/snapped-the-frame",
			"/global/snapped-the-frame"
		};

		when(ssm.getParameters(any())).thenThrow(new RuntimeException("no parameter defined"));

		// run test
		try {
			provider.getProperty("fml", "snapped-the-frame");
			fail("no parameter was found - this should have failed.");
		} catch (final Exception e) {
			// this is expected
			assertEquals(
				"unable to find parameter using pattern /fml/prefix/snapped-the-frame",
				e.getLocalizedMessage()
			);
		}

		// verify mocks / capture values
		verify(ssm).getParameters(getParametersRequestArgumentCaptor.capture());
		verify(handler).accept(any(ProviderExceptionHandler.ExceptionInfo.class));

		// assert results
		final List<String> nameList = getParametersRequestArgumentCaptor.getValue().getNames();

		for (final String value : expectedValues) {
			assertTrue("name list should include '" + value + "'", nameList.contains(value));
		}

	}

	@Test
	public void should_show_custom_exception_handler() {

		provider = new AwsSsmSettingProvider(
			ssm,
			"prefix",
			exceptionInfo -> {
				log.warn("oh noes!!! exception -> {}", exceptionInfo);
				throw new RuntimeException(exceptionInfo.getException());
			}
		);

		// setup
		final String[] expectedValues = {
			"/fml/prefix/snapped-the-frame",
			"/fml/snapped-the-frame",
			"/global/snapped-the-frame"
		};

		when(ssm.getParameters(any())).thenThrow(new RuntimeException("no parameter defined"));

		// run test
		try {
			provider.getProperty("fml", "snapped-the-frame");
			fail("no parameter was found - this should have failed.");
		} catch (final Exception e) {
			// this is expected
			assertEquals(
				"java.lang.RuntimeException: no parameter defined",
				e.getLocalizedMessage()
			);
		}

		// verify mocks / capture values
		verify(ssm).getParameters(getParametersRequestArgumentCaptor.capture());

		// assert results
		final List<String> actualValues = getParametersRequestArgumentCaptor.getValue().getNames();

		assertEquals(expectedValues.length, actualValues.size());
		for (String expectedValue : expectedValues) {
			assertTrue(actualValues.contains(expectedValue));
		}
	}
}
