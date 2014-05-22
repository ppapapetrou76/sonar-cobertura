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

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;
import org.sonar.plugins.java.api.JavaResourceLocator;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.text.ParseException;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;

public class CoberturaReportParser {

  private final JavaResourceLocator javaResourceLocator;
  private final SensorContext context;

  private CoberturaReportParser(SensorContext context, JavaResourceLocator javaResourceLocator) {
    this.context = context;
    this.javaResourceLocator = javaResourceLocator;
  }

  /**
   * Parse a Cobertura xml report and create measures accordingly
   */
  public static void parseReport(File xmlFile, SensorContext context, JavaResourceLocator javaResourceLocator) {
    new CoberturaReportParser(context, javaResourceLocator).parse(xmlFile);
  }

  private void parse(File xmlFile) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          rootCursor.advance();
          collectPackageMeasures(rootCursor.descendantElementCursor("package"));
        }
      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private void collectPackageMeasures(SMInputCursor pack) throws XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {
        String className = sanitizeFilename(entry.getKey());
        Resource resource = javaResourceLocator.findResourceByClassName(className);
        if (resourceExists(resource)) {
          for (Measure measure : entry.getValue().createMeasures()) {
            context.saveMeasure(resource, measure);
          }
        }
      }
    }
  }

  private boolean resourceExists(Resource<?> file) {
    return context.getResource(file) != null;
  }

  private void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> builderByFilename) throws XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = clazz.getAttrValue("filename");
      CoverageMeasuresBuilder builder = builderByFilename.get(fileName);
      if (builder == null) {
        builder = CoverageMeasuresBuilder.create();
        builderByFilename.put(fileName, builder);
      }
      collectFileData(clazz, builder);
    }
  }

  private void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      try {
        builder.setHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));
      } catch (ParseException e) {
        throw new XmlParserException(e);
      }

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }

  private static String sanitizeFilename(String s) {
    String fileName = FilenameUtils.removeExtension(s);
    fileName = fileName.replace('/', '.').replace('\\', '.');
    return fileName;
  }

}
