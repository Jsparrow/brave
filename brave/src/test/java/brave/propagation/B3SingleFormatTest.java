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
package brave.propagation;

import brave.internal.Platform;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static brave.propagation.B3SingleFormat.parseB3SingleFormat;
import static brave.propagation.B3SingleFormat.writeB3SingleFormat;
import static brave.propagation.B3SingleFormat.writeB3SingleFormatAsBytes;
import static brave.propagation.B3SingleFormat.writeB3SingleFormatWithoutParentId;
import static brave.propagation.B3SingleFormat.writeB3SingleFormatWithoutParentIdAsBytes;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
// Added to declutter console: tells power mock not to mess with implicit classes we aren't testing
@PowerMockIgnore({"org.apache.logging.*", "javax.script.*"})
@PrepareForTest({Platform.class, B3SingleFormat.class})
public class B3SingleFormatTest {
  String traceIdHigh = "0000000000000009";
  String traceId = "0000000000000001";
  String parentId = "0000000000000002";
  String spanId = "0000000000000003";

  Platform platform = mock(Platform.class);

  @Before public void setupLogger() {
    mockStatic(Platform.class);
    when(Platform.get()).thenReturn(platform);
  }

  /** Either we asserted on the log messages or there weren't any */
  @After public void ensureNothingLogged() {
    verifyNoMoreInteractions(platform);
  }

  @Test public void writeB3SingleFormat_notYetSampled() {
    TraceContext context = TraceContext.newBuilder().traceId(1).spanId(3).build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormat_notYetSampled_128() {
    TraceContext context = TraceContext.newBuilder().traceIdHigh(9).traceId(1).spanId(3).build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceIdHigh).append(traceId).append("-").append(spanId).toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormat_unsampled() {
    TraceContext context = TraceContext.newBuilder().traceId(1).spanId(3).sampled(false).build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-0").toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormat_sampled() {
    TraceContext context = TraceContext.newBuilder().traceId(1).spanId(3).sampled(true).build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-1").toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormat_debug() {
    TraceContext context = TraceContext.newBuilder().traceId(1).spanId(3).debug(true).build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-d").toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormat_parent() {
    TraceContext context =
      TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).sampled(true).build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-1-").append(parentId).toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormat_largest() {
    TraceContext context =
      TraceContext.newBuilder()
        .traceIdHigh(9)
        .traceId(1)
        .parentId(2)
        .spanId(3)
        .sampled(true)
        .build();

    assertThat(writeB3SingleFormat(context))
      .isEqualTo(new StringBuilder().append(traceIdHigh).append(traceId).append("-").append(spanId).append("-1-").append(parentId).toString())
      .isEqualTo(new String(writeB3SingleFormatAsBytes(context), UTF_8));
  }

  @Test public void parseB3SingleFormat_largest() {
    assertThat(
      parseB3SingleFormat(new StringBuilder().append(traceIdHigh).append(traceId).append("-").append(spanId).append("-1-").append(parentId).toString())
    ).extracting(TraceContextOrSamplingFlags::context).isEqualToComparingFieldByField(
      TraceContext.newBuilder()
        .traceIdHigh(9)
        .traceId(1)
        .parentId(2)
        .spanId(3)
        .sampled(true)
        .build()
    );
  }

  @Test public void writeB3SingleFormatWithoutParent_notYetSampled() {
    TraceContext context = TraceContext.newBuilder().traceId(1).spanId(3).build();

    assertThat(writeB3SingleFormatWithoutParentId(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).toString())
      .isEqualTo(new String(writeB3SingleFormatWithoutParentIdAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormatWithoutParent_unsampled() {
    TraceContext context =
      TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).sampled(false).build();

    assertThat(writeB3SingleFormatWithoutParentId(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-0").toString())
      .isEqualTo(new String(writeB3SingleFormatWithoutParentIdAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormatWithoutParent_sampled() {
    TraceContext context =
      TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).sampled(true).build();

    assertThat(writeB3SingleFormatWithoutParentId(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-1").toString())
      .isEqualTo(new String(writeB3SingleFormatWithoutParentIdAsBytes(context), UTF_8));
  }

  @Test public void writeB3SingleFormatWithoutParent_debug() {
    TraceContext context =
      TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).debug(true).build();

    assertThat(writeB3SingleFormatWithoutParentId(context))
      .isEqualTo(new StringBuilder().append(traceId).append("-").append(spanId).append("-d").toString())
      .isEqualTo(new String(writeB3SingleFormatWithoutParentIdAsBytes(context), UTF_8));
  }

  /** for example, parsing a w3c context */
  @Test public void parseB3SingleFormat_middleOfString() {
    String input = new StringBuilder().append("b3=").append(traceId).append(traceId).append("-").append(spanId).append(",").toString();
    assertThat(parseB3SingleFormat(input, 3, input.length() - 1).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceIdHigh(1).traceId(1).spanId(3).build()
      );
  }

  /** for example, parsing a w3c context */
  @Test public void parseB3SingleFormat_middleOfString_debugOnly() {
    String input = "b2=foo,b3=d,b4=bar";
    assertThat(parseB3SingleFormat(input, 10, 11).samplingFlags())
      .isSameAs(SamplingFlags.DEBUG);
  }

  @Test public void parseB3SingleFormat_middleOfString_incorrectOffset() {
    String input = "b2=foo,b3=d,b4=bar";
    assertThat(parseB3SingleFormat(input, 10, 12))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: truncated", null);
  }

  @Test public void parseB3SingleFormat_idsNotYetSampled() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceId(1).spanId(3).build()
      );
  }

  @Test public void parseB3SingleFormat_idsNotYetSampled128() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append(traceId).append("-").append(spanId).toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceIdHigh(1).traceId(1).spanId(3).build()
      );
  }

  @Test public void parseB3SingleFormat_idsUnsampled() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-0").toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceId(1).spanId(3).sampled(false).build()
      );
  }

  @Test public void parseB3SingleFormat_parent_unsampled() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-0-").append(parentId).toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).sampled(false).build()
      );
  }

  @Test public void parseB3SingleFormat_parent_debug() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-d-").append(parentId).toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).debug(true).build()
      );
  }

  @Test public void parseB3SingleFormat_idsWithDebug() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-d").toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceId(1).spanId(3).debug(true).build()
      );
  }

  @Test public void parseB3SingleFormat_sampledFalse() {
    assertThat(parseB3SingleFormat("0"))
      .isEqualTo(TraceContextOrSamplingFlags.NOT_SAMPLED);
  }

  @Test public void parseB3SingleFormat_sampled() {
    assertThat(parseB3SingleFormat("1"))
      .isEqualTo(TraceContextOrSamplingFlags.SAMPLED);
  }

  @Test public void parseB3SingleFormat_debug() {
    assertThat(parseB3SingleFormat("d"))
      .isEqualTo(TraceContextOrSamplingFlags.DEBUG);
  }

  @Test public void parseB3SingleFormat_malformed_traceId() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId.substring(0, 15)).append("?-").append(spanId).toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: expected a 16 or 32 lower hex trace ID at offset 0", null);
  }

  @Test public void parseB3SingleFormat_malformed_id() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId.substring(0, 15)).append("?").toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: expected a 16 lower hex span ID at offset {0}", 17, null);
  }

  @Test public void parseB3SingleFormat_malformed_sampled_parentid() {
    assertThat(
      parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-1-").append(parentId.substring(0, 15)).append("?").toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: expected a 16 lower hex parent ID at offset {0}", 36,
      null);
  }

  @Test public void parseB3SingleFormat_malformed_invalid_delimiter_before_parent() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-1!").append(parentId).toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: expected a hyphen(-) delimiter at offset {0}", 35, null);
  }

  // odd but possible to not yet sample a child
  @Test public void parseB3SingleFormat_parentid_notYetSampled() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-").append(parentId).toString()).context())
      .isEqualToComparingFieldByField(
        TraceContext.newBuilder().traceId(1).parentId(2).spanId(3).build()
      );
  }

  @Test public void parseB3SingleFormat_malformed_parentid_notYetSampled() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-").append(parentId.substring(0, 15)).append("?").toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: expected a 16 lower hex parent ID at offset {0}", 34,
      null);
  }

  @Test public void parseB3SingleFormat_malformed() {
    assertThat(parseB3SingleFormat("not-a-tumor"))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: truncated", null);
  }

  @Test public void parseB3SingleFormat_malformed_uuid() {
    assertThat(parseB3SingleFormat("b970dafd-0d95-40aa-95d8-1d8725aebe40"))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: expected a 16 or 32 lower hex trace ID at offset 0", null);
  }

  @Test public void parseB3SingleFormat_empty() {
    assertThat(parseB3SingleFormat("")).isNull();

    verify(platform).log("Invalid input: empty", null);
  }

  @Test public void parseB3SingleFormat_hyphenNotSampled() {
    assertThat(parseB3SingleFormat("-")).isNull();

    verify(platform).log("Invalid input: expected 0, 1 or d for sampled at offset {0}", 0, null);
  }

  @Test public void parseB3SingleFormat_truncated() {
    List<String> truncated = Arrays.asList(
      "-1",
      "1-",
      traceId.substring(0, 15),
      traceId,
      traceId + "-",
      new StringBuilder().append(traceId.substring(0, 15)).append("-").append(spanId).toString(),
      new StringBuilder().append(traceId).append("-").append(spanId.substring(0, 15)).toString(),
      new StringBuilder().append(traceId).append("-").append(spanId).append("-").toString(),
      new StringBuilder().append(traceId).append("-").append(spanId).append("-1-").toString(),
      new StringBuilder().append(traceId).append("-").append(spanId).append("-1-").append(parentId.substring(0, 15)).toString()
    );
    truncated.forEach(b3 -> {
      assertThat(parseB3SingleFormat(b3))
        .withFailMessage(new StringBuilder().append("expected ").append(b3).append(" to not parse").toString()).isNull();
      verify(platform).log("Invalid input: truncated", null);
      reset(platform);
    });
  }

  @Test public void parseB3SingleFormat_traceIdTooLong() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append(traceId).append("a").append("-").append(spanId).toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: trace ID is too long", null);
  }

  @Test public void parseB3SingleFormat_spanIdTooLong() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("a").toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: span ID is too long", null);
  }

  @Test public void parseB3SingleFormat_parentIdTooLong() {
    assertThat(parseB3SingleFormat(new StringBuilder().append(traceId).append("-").append(spanId).append("-").append(parentId).append("a").toString()))
      .isNull(); // instead of raising exception

    verify(platform).log("Invalid input: parent ID is too long", null);
  }
}
