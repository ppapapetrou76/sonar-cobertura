/*
 * SonarQube Cobertura Plugin
 * Copyright (C) 2013 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cobertura.www;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.cobertura.CoberturaSensor;

import java.util.List;

public final class CoberturaPlugin extends SonarPlugin {

  public static final String COBERTURA_REPORT_PATH_PROPERTY = "sonar.cobertura.reportPath";

  public List getExtensions() {
    return ImmutableList.of(
        PropertyDefinition.builder(COBERTURA_REPORT_PATH_PROPERTY)
            .category(CoreProperties.CATEGORY_JAVA)
            .subCategory("Cobertura")
            .name("Report path")
            .description("Path (absolute or relative) to Cobertura xml report file.")
            .onlyOnQualifiers(Qualifiers.PROJECT)
            .build(),

        CoberturaSensor.class);
  }

}
