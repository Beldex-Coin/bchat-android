package com.beldex.libsignal.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class JsonUtil {

  private static final String TAG = JsonUtil.class.getSimpleName();

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
  }

  public static <T> T fromJson(byte[] serialized, Class<T> clazz) throws IOException {
    return fromJson(new String(serialized), clazz);
  }

  public static <T> T fromJson(String serialized, TypeReference<T> typeReference) throws IOException {
    return objectMapper.readValue(serialized, typeReference);
  }

  public static <T> T fromJson(String serialized, Class<T> clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public static<T> T fromJson(String serialized, JavaType clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public static <T> T fromJson(InputStream serialized, Class<T> clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public static <T> T fromJson(Reader serialized, Class<T> clazz) throws IOException {
    return objectMapper.readValue(serialized, clazz);
  }

  public  static JsonNode fromJson(String serialized) throws IOException {
    return objectMapper.readTree(serialized);
  }

  public static String toJsonThrows(Object object) throws IOException {
    return objectMapper.writeValueAsString(object);
  }

  public static String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      Log.w(TAG, e);
      return "";
    }
  }

  public static ObjectMapper getMapper() {
    return objectMapper;
  }

  public static class SaneJSONObject {
    private final JSONObject delegate;

    public SaneJSONObject(JSONObject delegate) {
      this.delegate = delegate;
    }

    public String getString(String name) throws JSONException {
      if (delegate.isNull(name)) return null;
      else                       return delegate.getString(name);
    }

    public long getLong(String name) throws JSONException {
      return delegate.getLong(name);
    }

    public boolean isNull(String name) {
      return delegate.isNull(name);
    }

    public int getInt(String name) throws JSONException {
      return delegate.getInt(name);
    }
  }
}
