/*
 * Copyright 2022 Szymon Micyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonix.scripts.harventer.converter

import groovy.json.JsonSlurper
import groovy.json.StringEscapeUtils
import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.transform.CompileDynamic
import net.simonix.scripts.harventer.form.StringRequestContext
import net.simonix.scripts.harventer.model.Defaults
import net.simonix.scripts.harventer.model.Header
import net.simonix.scripts.harventer.model.Param
import net.simonix.scripts.harventer.model.Request
import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.disk.DiskFileItemFactory

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main converter class.
 *
 * Converter reads input JSON file (HAR), creates model for script generation based on given tamplate.
 */
@CompileDynamic
class Converter {

    static final String ENCODING_DEFAULT = 'UTF-8'

    ConverterConfig config

    Request request(Map entry) {
        Map request = entry.request as Map

        URL url = new URL(request.url as String)

        LocalDateTime requestDateTime = LocalDateTime.parse(entry.startedDateTime as String, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        long requestWaitDuration = entry.time as long

        Request data = new Request(url: url, method: request.method, requestDateTime: requestDateTime, requestWaitDuration: requestWaitDuration)

        data.headers = request.headers?.findAll { filterHeaders(it) }?.collect { header(it) }?.sort { a, b -> a.name.compareTo(b.name) }

        data.params = []

        List<Param> params = request.queryString?.collect { param(it) } ?: []
        data.params.addAll(params)

        Object postData = request.postData
        if (postData) {
            params = postData.params?.collect { param(it) } ?: []
            data.params.addAll(params)

            if (postData.mimeType =~ /^multipart\/form-data/) {
                data.multipart = true

                params = extractMultipartFormParams(postData.text as String, postData.mimeType as String) ?: []
                data.params.addAll(params)
            }
        }

        data.params = data.params.sort { a, b -> a.name.compareTo(b.name) }

        return data
    }

    Header header(Object value) {
        if (config.headerVariables) {
            String variable = config.headerVariables[value.name as String]
            if (variable) {
                return new Header(name: value.name, value: "\${${variable}}")
            }
        }

        return new Header(name: value.name, value: StringEscapeUtils.escapeJava(value.value as String))
    }

    Param param(Object value) {
        if (config.paramsVariables) {
            String variable = config.paramsVariables[value.name as String]
            if (variable) {
                return new Param(name: value.name, value: "\${${variable}}")
            }
        }

        return new Param(name: value.name, value: StringEscapeUtils.escapeJava(value.value as String))
    }

    List<Param> extractMultipartFormParams(String data, String type) {
        DiskFileItemFactory factory = new DiskFileItemFactory()

        FileUpload parser = new FileUpload(factory)

        List<FileItem> items = parser.parseRequest(new StringRequestContext(data, type, ENCODING_DEFAULT))

        return items.findAll { it.formField }
                .collect { param([name: it.fieldName, value: it.getString(ENCODING_DEFAULT)]) }
    }

    boolean filterByContent(Object entry) {
        String mimeType = entry.response.content.mimeType

        if (mimeType) {
            if (config.contentTypeIncludes && config.contentTypeExcludes) {
                return mimeType =~ config.contentTypeIncludes && !(mimeType =~ config.contentTypeExcludes)
            }

            if (config.contentTypeIncludes) {
                return mimeType =~ config.contentTypeIncludes
            }

            if (config.contentTypeExcludes) {
                return !(mimeType =~ config.contentTypeExcludes)
            }
        }

        return true
    }

    boolean filterByUrl(Object entry) {
        String url = entry.request.url

        if (config.urlIncludes && config.urlExcludes) {
            return url =~ config.urlIncludes && !(url =~ config.urlExcludes)
        }

        if (config.urlIncludes) {
            return url =~ config.urlIncludes
        }

        if (config.urlExcludes) {
            return !(url =~ config.urlExcludes)
        }

        return true
    }

    boolean filterHeaders(Object header) {
        String name = header.name

        if (config.headerNameIncludes && config.headerNameExcludes) {
            return name =~ config.headerNameIncludes && !(name =~ config.headerNameExcludes)
        }

        if (config.headerNameIncludes) {
            return name =~ config.headerNameIncludes
        }

        if (config.headerNameExcludes) {
            return !(name =~ config.headerNameExcludes)
        }

        return false
    }

    /**
     * Converts JSON to Groovy DSL script
     */
    void convert() {
        JsonSlurper jsonSlurper = new JsonSlurper()
        Object json = jsonSlurper.parse(new FileReader(new File(config.inputFile)))

        LocalDateTime pageStartTime = json.log.pages.collect { LocalDateTime.parse(it.startedDateTime as String, DateTimeFormatter.ISO_ZONED_DATE_TIME) }.min()

        List<Request> requests = json.log.entries
                .findAll { filterByContent(it) && filterByUrl(it) }
                .collect { request(it as Map) }
                .sort { a, b -> (a.requestDateTime <=> b.requestDateTime) }

        Request first = requests?.first()

        if(!pageStartTime) {
            pageStartTime = first.requestDateTime
        }

        // assume first request as default one
        String protocol = first?.url?.protocol ?: ''
        String host = first?.url?.host ?: ''
        int port = first?.url?.port ?: 80

        def calcDuration = { LocalDateTime previous, Request request ->
            LocalDateTime current = request.requestDateTime
            Duration duration = Duration.between(previous, current)

            long totalRequestDuration = duration.toMillis() - request.requestWaitDuration
            return totalRequestDuration > 0 ? totalRequestDuration : 0
        }

        TemplateEngine engine = new GStringTemplateEngine()
        Map model = [
                requests: requests,
                defaults: new Defaults(protocol: protocol, host: host, port: port),
                compact : config.compact,
                version : config.version,
                vars    : config.paramsVariables + config.headerVariables + config.urlVariables,
                pageStartTime   : pageStartTime,
                calcDuration    : calcDuration,
                thinkTimeEnabled: config.thinkTimeEnabled,
                baseName: config.outputScript - ~/\.\w+$/,
                users: config.users,
                rampUp: config.rampUp,
                loops: config.loops
        ]

        Template template = engine.createTemplate(getTemplateReader(config.templateScript))
        Writable output = template.make(model)

        new File(config.outputScript).withWriter { writer ->
            writer.write(output)
        }
    }

    private static Reader getTemplateReader(String templateFileName) {
        return new InputStreamReader(Converter.getResourceAsStream(templateFileName))
    }
}
