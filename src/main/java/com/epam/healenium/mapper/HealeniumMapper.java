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
package com.epam.healenium.mapper;

import com.epam.healenium.model.HealingResultDto;
import com.epam.healenium.model.Locator;
import com.epam.healenium.model.RequestDto;
import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.openqa.selenium.By;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HealeniumMapper {

    default RequestDto buildDto(By by, StackTraceElement element){
        String[] locatorParts = by.toString().split(":");
        RequestDto dto = new RequestDto()
                .setLocator(locatorParts[1].trim())
                .setType(locatorParts[0].trim());
        if(element != null){
            dto.setClassName(element.getClassName());
            dto.setMethodName(element.getMethodName());
        }
        return dto;
    }

    default RequestDto buildDto(By by, StackTraceElement element, List<Node> nodePath){
        RequestDto dto = buildDto(by, element);
        dto.setNodePath(nodePath);
        return dto;

    }

    default RequestDto buildDto(By by, StackTraceElement element, String page, List<Scored<By>> healingResults, Scored<By> selected, byte[] screenshot){
        RequestDto dto = buildDto(by, element);
        dto.setPageContent(page)
                .setResults(buildResultDto(healingResults))
                .setUsedResult(buildResultDto(selected))
                .setScreenshot(screenshot);
        return dto;
    }

    default HealingResultDto buildResultDto(Scored<By> scored){
        return new HealingResultDto(byToLocator(scored.getValue()), scored.getScore());
    }

    default List<HealingResultDto> buildResultDto(Collection<Scored<By>> scored){
        return scored.stream().map(this::buildResultDto).collect(Collectors.toList());
    }

    default Locator byToLocator(By by){
        String[] locatorParts = by.toString().split(":");
        return new Locator(locatorParts[1].trim(), locatorParts[0].trim());
    }

    default List<Locator> byToLocator(Collection<By> by){
        return by.stream().map(this::byToLocator).collect(Collectors.toList());
    }
}
