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

import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContextCustomizer;
import java.util.List;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CurrentTraceContextFactoryBeanTest {
  public static final CurrentTraceContextCustomizer
	    CUSTOMIZER_ONE = mock(CurrentTraceContextCustomizer.class);
public static final CurrentTraceContextCustomizer CUSTOMIZER_TWO = mock(CurrentTraceContextCustomizer.class);
	XmlBeans context;

	@After public void close() {
	    if (context != null) {
			context.close();
		}
	  }

	@Test public void scopeDecorators() {
	    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"currentTraceContext\" class=\"brave.spring.beans.CurrentTraceContextFactoryBean\">\n").append("  <property name=\"scopeDecorators\">\n").append("    <list>\n").append("      <bean class=\"brave.propagation.StrictScopeDecorator\" factory-method=\"create\"/>\n").append("    </list>\n").append("  </property>").append("</bean>")
				.toString()
	    );
	
	    assertThat(context.getBean("currentTraceContext", CurrentTraceContext.class))
	      .extracting("scopeDecorators")
	      .satisfies(e -> assertThat((List) e).isNotEmpty());
	  }

	@Test public void customizers() {
	    context = new XmlBeans(new StringBuilder().append("").append("<bean id=\"currentTraceContext\" class=\"brave.spring.beans.CurrentTraceContextFactoryBean\">\n").append("  <property name=\"customizers\">\n").append("    <list>\n").append("      <util:constant static-field=\"").append(getClass().getName()).append(".CUSTOMIZER_ONE\"/>\n")
				.append("      <util:constant static-field=\"").append(getClass().getName()).append(".CUSTOMIZER_TWO\"/>\n").append("    </list>\n").append("  </property>").append("</bean>").toString()
	    );
	
	    context.getBean("currentTraceContext", CurrentTraceContext.class);
	
	    verify(CUSTOMIZER_ONE).customize(any(CurrentTraceContext.Builder.class));
	    verify(CUSTOMIZER_TWO).customize(any(CurrentTraceContext.Builder.class));
	  }
}
