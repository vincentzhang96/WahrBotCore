package com.divinitor.discord.wahrbot.core.util.gson.adapters;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * A Gson type adapter for a {@link Version}
 */
public class VersionTypeAdapter extends TypeAdapter<Version> {
    @Override
    public void write(JsonWriter jsonWriter, Version version) throws IOException {
        jsonWriter.value(version.toString());
    }

    @Override
    public Version read(JsonReader jsonReader) throws IOException {
        return Version.valueOf(jsonReader.nextString());
    }
}
