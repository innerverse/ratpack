/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.http.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.http.MediaType;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ratpack.util.Exceptions.toException;
import static ratpack.util.Exceptions.uncheck;

public class DefaultMediaType implements MediaType {

  public static final String CHARSET_KEY = "charset";

  private final String type;
  protected final Map<String, String> params;
  private final String string;

  private static final int CACHE_SIZE = 200;

  private static final Cache<String, MediaType> CACHE = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();

  public static MediaType get(final String contentType) {
    String contentType1 = contentType;
    if (contentType1 == null) {
      contentType1 = "";
    } else {
      contentType1 = contentType1.trim();
    }

    final String finalContentType = contentType1;
    try {
      return CACHE.get(contentType1, () -> new DefaultMediaType(finalContentType));
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  public DefaultMediaType(String value) {
    ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.builder();
    if (value == null) {
      type = null;
    } else {
      value = value.trim();
      if (value.length() == 0) {
        type = null;
      } else {
        String[] parts = value.split(";");
        type = parts[0].toLowerCase();
        if (parts.length > 1) {
          for (int i = 1; i < parts.length; ++i) {
            String part = parts[i].trim();
            String keyPart;
            String valuePart;
            if (part.contains("=")) {
              String[] valueSplit = part.split("=", 2);
              keyPart = valueSplit[0].toLowerCase();
              valuePart = valueSplit[1];
            } else {
              keyPart = part.toLowerCase();
              valuePart = "";
            }
            paramsBuilder.put(keyPart, valuePart);
          }
        }
      }
    }

    params = paramsBuilder.build();
    string = generateString();
  }

  public String getType() {
    return type;
  }

  public Map<String, String> getParams() {
    return Collections.unmodifiableMap(params);
  }

  public String getCharset() {
    return params.get(CHARSET_KEY);
  }

  public String getCharset(String defaultCharset) {
    return params.containsKey(CHARSET_KEY) ? params.get(CHARSET_KEY) : defaultCharset;
  }

  public boolean isText() {
    return getType() != null && getType().startsWith("text/");
  }

  public boolean isJson() {
    return getType() != null && getType().equals(APPLICATION_JSON);
  }

  public boolean isForm() {
    return getType() != null && getType().equals(APPLICATION_FORM);
  }

  @Override
  public boolean isHtml() {
    return getType() != null && getType().equals(TEXT_HTML);
  }

  public boolean isEmpty() {
    return getType() == null;
  }

  @Override
  public String toString() {
    return string;
  }

  private String generateString() {
    if (isEmpty()) {
      return "";
    } else {
      StringBuilder s = new StringBuilder(getType());
      for (Map.Entry<String, String> param : getParams().entrySet()) {
        s.append(";").append(param.getKey());
        if (!param.getValue().isEmpty()) {
          s.append("=").append(param.getValue());
        }
      }
      return s.toString();
    }
  }
}
