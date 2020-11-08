/*
 * Copyright 2020 Szymon Micyk
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

import groovy.transform.CompileStatic
import groovy.transform.ToString

import java.util.regex.Pattern

/**
 * Keeps configuration for converter
 */
@ToString
@CompileStatic
class ConverterConfig {

    Pattern contentTypeIncludes
    Pattern contentTypeExcludes

    Pattern urlIncludes
    Pattern urlExcludes

    Pattern headerNameIncludes
    Pattern headerNameExcludes

    String version
    boolean compact = false
    boolean thinkTimeEnabled = false

    int users
    int rampUp

    String templateScript
    String outputScript
    String inputFile

    Map<String, String> paramsVariables = [:]
    Map<String, String> headerVariables = [:]
    Map<String, String> urlVariables = [:]

    Pattern setContentTypeIncludes(String contentTypeIncludes) {
        this.contentTypeIncludes = contentTypeIncludes ? Pattern.compile(contentTypeIncludes) : null
    }

    Pattern setContentTypeExcludes(String contentTypeExcludes) {
        this.contentTypeExcludes = contentTypeExcludes ? Pattern.compile(contentTypeExcludes) : null
    }

    Pattern setUrlIncludes(String urlIncludes) {
        this.urlIncludes = urlIncludes ? Pattern.compile(urlIncludes) : null
    }

    Pattern setUrlExcludes(String urlExcludes) {
        this.urlExcludes = urlExcludes ? Pattern.compile(urlExcludes) : null
    }

    Pattern setHeaderNameIncludes(String headerNameIncludes) {
        this.headerNameIncludes = headerNameIncludes ? Pattern.compile(headerNameIncludes) : null
    }

    Pattern setHeaderNameExcludes(String headerNameExcludes) {
        this.headerNameExcludes = headerNameExcludes ? Pattern.compile(headerNameExcludes) : null
    }
}
