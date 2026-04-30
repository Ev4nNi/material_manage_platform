package com.material.platform.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface MetadataExtractor {

    Map<String, Object> extract(InputStream inputStream, String fileName) throws IOException;
}
