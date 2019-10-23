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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import zipkin2.Endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class EndpointFactoryBeanTest {
  XmlBeans context;

  @After public void close() {
    if (context != null) {
		context.close();
	}
  }

  @Test public void serviceName() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"endpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n").append("  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("endpoint", Endpoint.class))
      .isEqualTo(Endpoint.newBuilder().serviceName("brave-webmvc-example").build());
  }

  @Test public void ip() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"endpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n").append("  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n").append("  <property name=\"ip\" value=\"1.2.3.4\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("endpoint", Endpoint.class))
      .isEqualTo(Endpoint.newBuilder()
        .serviceName("brave-webmvc-example")
        .ip("1.2.3.4")
        .build());
  }

  @Test public void ip_malformed() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"endpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n").append("  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n").append("  <property name=\"ip\" value=\"localhost\"/>\n").append("</bean>").toString()
    );

    try {
      context.getBean("endpoint", Endpoint.class);
      failBecauseExceptionWasNotThrown(BeanCreationException.class);
    } catch (BeanCreationException e) {
      assertThat(e)
        .hasMessageContaining("endpoint.ip: localhost is not an IP literal");
    }
  }

  @Test public void port() {
    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"endpoint\" class=\"brave.spring.beans.EndpointFactoryBean\">\n").append("  <property name=\"serviceName\" value=\"brave-webmvc-example\"/>\n").append("  <property name=\"ip\" value=\"1.2.3.4\"/>\n").append("  <property name=\"port\" value=\"8080\"/>\n").append("</bean>").toString()
    );

    assertThat(context.getBean("endpoint", Endpoint.class))
      .isEqualTo(Endpoint.newBuilder()
        .serviceName("brave-webmvc-example")
        .ip("1.2.3.4")
        .port(8080).build());
  }
}
