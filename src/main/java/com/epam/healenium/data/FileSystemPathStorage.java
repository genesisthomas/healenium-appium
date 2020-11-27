/**
 * Healenium-appium Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.data;

import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.NodeBuilder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.typesafe.config.Config;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FileSystemPathStorage implements PathStorage {
    private static final Logger LOGGER = LogManager.getLogger(FileSystemPathStorage.class);
    /**
     * Maximum file length usually varies, lowering to possible for most systems.
     */
    private static final int MAX_FILE_LENGTH = 128;
    private static final String FILENAME_REGEX = "[\\w\\-]+";
    private static final String REPORT_FILE = "index.html";
    private final Path basePath;
    private final Path reportsPath;
    private final ObjectMapper objectMapper;

    /**
     * Creates a file system bound storage.
     *
     * @param config a file system path to the place the storage will be physically allocated. Supports relative paths; in
     *               this case the path starts from a working directory.
     */
    public FileSystemPathStorage(Config config) {
        this.objectMapper = initMapper();
        this.basePath = Paths.get(config.getString("basePath"));
        this.reportsPath = Paths.get(config.getString("reportPath"));
        basePath.toFile().mkdirs();
        reportsPath.toFile().mkdirs();
    }

    private ObjectMapper initMapper() {
        SimpleModule module = new SimpleModule("node");
        module.addSerializer(Node.class, new NodeSerializer());
        module.addDeserializer(Node.class, new NodeDeserializer());
        ObjectMapper objectMapper = new ObjectMapper().registerModule(module);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }

    @Override
    public synchronized void persistLastValidPath(Object locator, String context, List<Node> nodes) {
        LOGGER.info("* persistLastValidPath start: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        Path path = getPersistedNodePath(locator, context);
        byte[] newContent;
        try {
            newContent = objectMapper.writeValueAsBytes(nodes);
            Files.write(path, newContent);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not map the contents to JSON!", e);
        } catch (IOException e) {
            LOGGER.error("Failed to persist last valid path", e);
        }
        LOGGER.info("* persistLastValidPath finish: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
    }

    @Override
    public synchronized List<Node> getLastValidPath(Object locator, String context) {
        Path path = getPersistedNodePath(locator, context);
        if (Files.exists(path)) {
            try {
                byte[] bytes = Files.readAllBytes(path);
                //noinspection unchecked
                return objectMapper.readValue(bytes, List.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.emptyList();
    }

    public synchronized void saveLocatorInfo(LocatorInfo info) throws IOException {
        new ObjectMapper().writeValue(reportsPath.resolve("data.json").toFile(), info);
        Path target = reportsPath.resolve(REPORT_FILE);
        if (!Files.exists(target)) {
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream source = classLoader.getResourceAsStream(REPORT_FILE);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public boolean isNodePathPersisted(Object locator, String context){
        return Files.exists(getPersistedNodePath(locator, context));
    }

    private Path getPersistedNodePath(Object locator, String context) {
        return basePath.resolve(getFileName(context) + "_" + locator.hashCode());
    }

    private String getFileName(String context) {
        if (context.matches(FILENAME_REGEX) && context.length() < MAX_FILE_LENGTH) {
            return context;
        }
        return DigestUtils.md5Hex(context);
    }

    private static class NodeSerializer extends JsonSerializer<Node> {

        @Override
        public void serializeWithType(Node value, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeId = typeSer.typeId(value, Node.class, JsonToken.START_OBJECT);
            typeSer.writeTypePrefix(gen, typeId);
            serialize(value, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeId);
        }

        @Override
        public void serialize(Node value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStringField("tag", value.getTag());
            gen.writeNumberField("index", value.getIndex());
            gen.writeStringField("innerText", value.getInnerText());
            gen.writeStringField("id", value.getId());
            gen.writeStringField("classes", String.join(" ", value.getClasses()));
            gen.writeObjectField("other", value.getOtherAttributes());
            gen.flush();
        }
    }

    private static class NodeDeserializer extends JsonDeserializer<Node> {

        @Override
        public Node deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            ObjectCodec codec = parser.getCodec();
            TreeNode tree = parser.readValueAsTree();
            String tag = codec.treeToValue(tree.path("tag"), String.class);
            Integer index = codec.treeToValue(tree.path("index"), Integer.class);
            String innerText = codec.treeToValue(tree.path("innerText"), String.class);
            String id = codec.treeToValue(tree.path("id"), String.class);
            String classes = codec.treeToValue(tree.path("classes"), String.class);
            //noinspection unchecked
            Map<String, String> attributes = codec.treeToValue(tree.path("other"), Map.class);
            attributes.put("id", id);
            attributes.put("class", classes);
            return new NodeBuilder()
                    .setTag(tag)
                    .setIndex(index)
                    .addContent(innerText)
                    .setAttributes(attributes)
                    .build();
        }
    }
}