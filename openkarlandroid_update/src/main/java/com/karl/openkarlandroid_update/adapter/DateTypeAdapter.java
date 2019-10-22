package com.karl.openkarlandroid_update.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class DateTypeAdapter implements JsonDeserializer<Date> {

    private DateFormat format;

    public DateTypeAdapter() {

    }

    public DateTypeAdapter(DateFormat format) {

        this.format = format;
    }

    public synchronized Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {

        if (!(json instanceof JsonPrimitive)) {
            throw new JsonParseException("This is not a primitive value");
        }

        String jsonStr = json.getAsString();

        if (format != null) {

            try {
                return format.parse(jsonStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return new Date(Long.parseLong(jsonStr));
    }
}
