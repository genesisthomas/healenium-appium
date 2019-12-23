package com.epam.healenium.appium.elementcreators;

import com.epam.healenium.engine.elementcreators.ElementCreator;
import com.epam.sha.treecomparing.Node;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class XPathCreator implements ElementCreator {

    @Override
    public String create(Node node) {
        Node current = node;
        Deque<String> path = new ArrayDeque<>();

        while (current != null) {
            String item = current.getTag();
            String id = current.getId();
            String resourceId = current.getOtherAttributes().getOrDefault("resource-id", "");
            String text = current.getOtherAttributes().getOrDefault("text", "");
            if (!StringUtils.isEmpty(id)) {
                item += "[@id = '" + id + "']";
            } else if (!StringUtils.isEmpty(resourceId)) {
                item += "[@resource-id = '" + resourceId + "']";
            } else if (!StringUtils.isEmpty(text)) {
                item += "[@text = '" + text + "']";
            }
            path.addFirst(item);
            if (!StringUtils.isEmpty(id) || !StringUtils.isEmpty(resourceId)) {
                break;
            }
            current = current.getParent();
        }
        String result = path.stream().collect(Collectors.joining("/", "//", ""));
        log.debug("Node selector: {}", result);
        return result;
    }
}
