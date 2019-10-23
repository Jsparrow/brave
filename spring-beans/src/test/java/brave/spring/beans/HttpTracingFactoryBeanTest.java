/*
 * Copyright 2013-2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package brave.spring.beans;

import brave.Tracing;
import brave.http.HttpClientParser;
import brave.http.HttpSampler;
import brave.http.HttpServerParser;
import brave.http.HttpTracing;
import brave.http.HttpTracingCustomizer;
import brave.sampler.SamplerFunctions;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HttpTracingFactoryBeanTest {

  public static Tracing TRACING = mock(Tracing.class);
  public static HttpClientParser CLIENT_PARSER = mock(HttpClientParser.class);
  public static HttpServerParser SERVER_PARSER = mock(HttpServerParser.class);
public static final HttpTracingCustomizer CUSTOMIZER_ONE = mock(HttpTracingCustomizer.class);
public static final HttpTracingCustomizer CUSTOMIZER_TWO = mock(HttpTracingCustomizer.class);
XmlBeans context;

@After public void close() {
    if (context != null) {
		context.close();
	}
  }

@Test public void tracing() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("</bean>")
			.toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class))
      .extracting("tracing")
      .isEqualTo(TRACING);
  }

@Test public void clientParser() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"clientParser\">\n")
			.append("    <util:constant static-field=\"").append(getClass().getName()).append(".CLIENT_PARSER\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class))
      .extracting("clientParser")
      .isEqualTo(CLIENT_PARSER);
  }

@Test public void serverParser() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"serverParser\">\n")
			.append("    <util:constant static-field=\"").append(getClass().getName()).append(".SERVER_PARSER\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class))
      .extracting("serverParser")
      .isEqualTo(SERVER_PARSER);
  }

@Test public void clientSampler() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"clientSampler\">\n")
			.append("    <util:constant static-field=\"brave.http.HttpSampler.NEVER_SAMPLE\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class))
      .extracting("clientSampler")
      .isEqualTo(HttpSampler.NEVER_SAMPLE);
  }

@Test public void clientRequestSampler() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"clientSampler\">\n")
			.append("    <bean class=\"brave.sampler.SamplerFunctions\" factory-method=\"neverSample\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class).clientRequestSampler())
      .isEqualTo(SamplerFunctions.neverSample());
  }

@Test public void serverSampler() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"serverSampler\">\n")
			.append("    <util:constant static-field=\"brave.http.HttpSampler.NEVER_SAMPLE\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class))
      .extracting("serverSampler")
      .isEqualTo(HttpSampler.NEVER_SAMPLE);
  }

@Test public void serverRequestSampler() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"serverSampler\">\n")
			.append("    <bean class=\"brave.sampler.SamplerFunctions\" factory-method=\"neverSample\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("httpTracing", HttpTracing.class).serverRequestSampler())
      .isEqualTo(SamplerFunctions.neverSample());
  }

@Test public void customizers() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"httpTracing\" class=\"brave.spring.beans.HttpTracingFactoryBean\">\n").append("  <property name=\"tracing\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".TRACING\"/>\n").append("  </property>\n").append("  <property name=\"customizers\">\n")
			.append("    <list>\n").append("      <util:constant static-field=\"").append(getClass().getName()).append(".CUSTOMIZER_ONE\"/>\n").append("      <util:constant static-field=\"").append(getClass().getName()).append(".CUSTOMIZER_TWO\"/>\n").append("    </list>\n")
			.append("  </property>").append("</bean>").toString()
    );

    context.getBean("httpTracing", HttpTracing.class);

    verify(CUSTOMIZER_ONE).customize(any(HttpTracing.Builder.class));
    verify(CUSTOMIZER_TWO).customize(any(HttpTracing.Builder.class));
  }
}
