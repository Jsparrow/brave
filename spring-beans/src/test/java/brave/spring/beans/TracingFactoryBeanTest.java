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

import brave.Clock;
import brave.ErrorParser;
import brave.Tracing;
import brave.TracingCustomizer;
import brave.handler.FinishedSpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.ExtraFieldPropagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import zipkin2.Endpoint;
import zipkin2.reporter.Reporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TracingFactoryBeanTest {
  public static final Clock CLOCK = mock(Clock.class);
  public static final ErrorParser ERROR_PARSER = mock(ErrorParser.class);
public static final FinishedSpanHandler FIREHOSE_HANDLER = mock(FinishedSpanHandler.class);
public static final TracingCustomizer CUSTOMIZER_ONE = mock(TracingCustomizer.class);
public static final TracingCustomizer CUSTOMIZER_TWO = mock(TracingCustomizer.class);
XmlBeans context;

@After public void close() {
    if (context != null) {
		context.close();
	}
    Tracing current = Tracing.current();
    if (current != null) {
		current.close();
	}
  }

@Test public void autoCloses() {
    context = new XmlBeans(""
      + "<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\"/>\n"
    );
    context.getBean("tracing", Tracing.class);

    assertThat(Tracing.current()).isNotNull();

    context.close();

    assertThat(Tracing.current()).isNull();

    context = null;
  }

@Test public void localServiceName() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"localServiceName\" value=\"brave-webmvc-example\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.finishedSpanHandler.delegate.converter.localEndpoint")
      .extracting("serviceName")
      .isEqualTo("brave-webmvc-example");
  }

@Test public void localEndpoint() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"localEndpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n").append("  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n").append("  <property name=\"ip\" value=\"1.2.3.4\"/>\n").append("  <property name=\"port\" value=\"8080\"/>\n").append("</bean>").toString()
      , new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"localEndpoint\" ref=\"localEndpoint\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.finishedSpanHandler.delegate.converter.localEndpoint")
      .isEqualTo(Endpoint.newBuilder()
        .serviceName("brave-webmvc-example")
        .ip("1.2.3.4")
        .port(8080).build());
  }

@Test public void endpoint() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"endpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n").append("  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n").append("  <property name=\"ip\" value=\"1.2.3.4\"/>\n").append("  <property name=\"port\" value=\"8080\"/>\n").append("</bean>").toString()
      , new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"endpoint\" ref=\"endpoint\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.finishedSpanHandler.delegate.converter.localEndpoint")
      .isEqualTo(Endpoint.newBuilder()
        .serviceName("brave-webmvc-example")
        .ip("1.2.3.4")
        .port(8080).build());
  }

@Test public void spanReporter() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"spanReporter\">\n").append("    <util:constant static-field=\"zipkin2.reporter.Reporter.CONSOLE\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.finishedSpanHandler.delegate.spanReporter")
      .isEqualTo(Reporter.CONSOLE);
  }

@Test public void finishedSpanHandlers() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"finishedSpanHandlers\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".FIREHOSE_HANDLER\"/>\n").append("  </property>\n").append("</bean>")
			.toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.finishedSpanHandler.handlers")
      .satisfies(a -> assertThat((FinishedSpanHandler[]) a).startsWith(FIREHOSE_HANDLER));
  }

@Test public void clock() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"clock\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".CLOCK\"/>\n").append("  </property>\n").append("</bean>")
			.toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.clock")
      .isEqualTo(CLOCK);
  }

@Test public void errorParser() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"errorParser\">\n").append("    <util:constant static-field=\"").append(getClass().getName()).append(".ERROR_PARSER\"/>\n").append("  </property>\n").append("</bean>")
			.toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("errorParser")
      .isEqualTo(ERROR_PARSER);
  }

@Test public void sampler() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"sampler\">\n").append("    <util:constant static-field=\"brave.sampler.Sampler.NEVER_SAMPLE\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.sampler")
      .isEqualTo(Sampler.NEVER_SAMPLE);
  }

@Test public void currentTraceContext() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"currentTraceContext\">\n").append("    <bean class=\"brave.spring.beans.CurrentTraceContextFactoryBean\"/>\n").append("  </property>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.currentTraceContext")
      .isInstanceOf(ThreadLocalCurrentTraceContext.class);
  }

@Test public void propagationFactory() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"propagationFactory\" class=\"brave.propagation.ExtraFieldPropagation\" factory-method=\"newFactory\">\n").append("  <constructor-arg index=\"0\">\n").append("    <util:constant static-field=\"brave.propagation.B3Propagation.FACTORY\"/>\n").append("  </constructor-arg>\n").append("  <constructor-arg index=\"1\">\n").append("    <list>\n").append("      <value>x-vcap-request-id</value>\n")
			.append("      <value>x-amzn-trace-id</value>\n").append("    </list>").append("  </constructor-arg>\n").append("</bean>").toString(), new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"propagationFactory\" ref=\"propagationFactory\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class).propagation())
      .isInstanceOf(ExtraFieldPropagation.class)
      .extracting("factory.fieldNames")
      .isEqualToComparingFieldByField(new String[] {"x-vcap-request-id", "x-amzn-trace-id"});
  }

@Test public void traceId128Bit() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"traceId128Bit\" value=\"true\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.traceId128Bit")
      .isEqualTo(true);
  }

@Test public void supportsJoin() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"supportsJoin\" value=\"true\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("tracing", Tracing.class))
      .extracting("tracer.supportsJoin")
      .isEqualTo(true);
  }

@Test public void customizers() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"tracing\" class=\"brave.spring.beans.TracingFactoryBean\">\n").append("  <property name=\"customizers\">\n").append("    <list>\n").append("      <util:constant static-field=\"").append(getClass().getName()).append(".CUSTOMIZER_ONE\"/>\n").append("      <util:constant static-field=\"")
			.append(getClass().getName()).append(".CUSTOMIZER_TWO\"/>\n").append("    </list>\n").append("  </property>").append("</bean>").toString()
    );

    context.getBean("tracing", Tracing.class);

    verify(CUSTOMIZER_ONE).customize(any(Tracing.Builder.class));
    verify(CUSTOMIZER_TWO).customize(any(Tracing.Builder.class));
  }
}
