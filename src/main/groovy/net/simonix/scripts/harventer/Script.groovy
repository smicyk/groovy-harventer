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
package net.simonix.scripts.harventer

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import groovy.transform.CompileDynamic
import net.simonix.scripts.harventer.converter.Converter
import net.simonix.scripts.harventer.converter.ConverterConfig

/**
 * Main entry point of the converter.
 */
@CompileDynamic
class Script {

    static final String ARG_NAME_FILE = 'file'
    static final String ARG_NAME_PROPERTY = 'param=variable'
    static final String ARG_NAME_PATTERN = 'pattern'
    static final String ARG_NAME_NUMBER = 'number'

    /**
     * Define and parse command line arguments.
     *
     * @param args list of arguments
     * @return parsed command line arguments
     */
    @SuppressWarnings(['LineLength', 'DuplicateStringLiteral', 'DuplicateNumberLiteral'])
    static OptionAccessor parseArguments(String... args) {
        CliBuilder cliBuilder = new CliBuilder(usage: 'harventer [options]')
        cliBuilder.with {
            h(longOpt: 'help', args: 0, 'Show help')
            i(longOpt: 'input-file', args: 1, argName: ARG_NAME_FILE, 'Input *.har file')
            o(longOpt: 'output-file', args: 1, argName: ARG_NAME_FILE, 'Output *.groovy file')
            c(longOpt: 'compact', args: 0, 'If present the param and headers will be generated in compact form')
            t(longOpt: 'think-time', args: 0, 'If present each HTTP request has think time based on real execution')
            P(longOpt: 'param-variables', args: 2, valueSeparator: '=', argName: ARG_NAME_PROPERTY, 'Substitute param with variable (applies to request params)')
            H(longOpt: 'header-variables', args: 2, valueSeparator: '=', argName: ARG_NAME_PROPERTY, 'Substitute header with variable (applies to request headers)')
            U(longOpt: 'url-variables', args: 2, valueSeparator: '=', argName: ARG_NAME_PROPERTY, 'Substitute part of URL with variable')
            _(longOpt: 'dsl-version', args: 1, 'Specify which DSL version to use for generated script')
            _(longOpt: 'include-headers', args: 1, argName: ARG_NAME_PATTERN, 'Regex pattern for header name to include')
            _(longOpt: 'exclude-headers', args: 1, argName: ARG_NAME_PATTERN, 'Regex pattern for header name to exclude (by default all headers are excluded)')
            _(longOpt: 'include-urls', args: 1, argName: ARG_NAME_PATTERN, 'Regex pattern for URL to include')
            _(longOpt: 'exclude-urls', args: 1, argName: ARG_NAME_PATTERN, 'Regex pattern for URL to exclude (by default .css, .js, .bmp, .css, .js, .gif, .ico, .jpg, .jpeg, .png, .swf, .woff, .woff2)')
            _(longOpt: 'include-types', args: 1, argName: ARG_NAME_PATTERN, 'Regex pattern for response content type to include')
            _(longOpt: 'exclude-types', args: 1, argName: ARG_NAME_PATTERN, 'Regex pattern for response content type to exclude (by default css, javascript, images and binary types are excluded)')
            _(longOpt: 'users', args: 1, type: Integer, argName: ARG_NAME_NUMBER, 'Number of users for default group')
            _(longOpt: 'ramp-up', args: 1, type: Integer, argName: ARG_NAME_NUMBER, 'Ramp up time for test plan')
            _(longOpt: 'loops', args: 1, type: Integer, argName: ARG_NAME_NUMBER, 'Loops number for users')
        }

        return validateOptions(cliBuilder, args)
    }

    /**
     * Main method.
     *
     * @param args command line arguments
     */
    static void main(String... args) {
        OptionAccessor options = parseArguments(args)

        ConverterConfig config = loadDefaultConfig()
        config = loadOptions(options, config)

        Converter converter = new Converter(config: config)
        converter.convert()
    }

    /**
     * Parses and validates command line parameters. Checks required parameters
     *
     * @param cliBuilder command line builder
     * @param args command line arguments
     * @return parsed command line options
     */
    static OptionAccessor validateOptions(CliBuilder cliBuilder, String... args) {
        OptionAccessor options = cliBuilder.parse(args)

        if (!options) {
            System.err << 'Error while parsing command-line options.\n'
            System.exit(1)
        }

        if (options.h) {
            cliBuilder.usage()

            System.exit(0)
        }

        if (!options.i || !options.o) {
            System.err << 'Parameters \'o\' and \'i\' are required.\n'
            cliBuilder.usage()

            System.exit(1)
        }

        return options
    }

    /**
     * Loan default configuration from properties files.
     *
     * @return config object for converter
     */
    static ConverterConfig loadDefaultConfig() {
        // read properties
        Properties properties = new Properties()
        this.getResourceAsStream('application.properties').with {
            properties.load(it)
        }

        ConverterConfig config = new ConverterConfig()
        config.with {
            contentTypeExcludes = properties.'converter.content_type_excludes'
            contentTypeIncludes = properties.'converter.content_type_includes'

            urlExcludes = properties.'converter.url_excludes'
            urlIncludes = properties.'converter.url_includes'

            headerNameExcludes = properties.'converter.header_name_excludes'
            headerNameIncludes = properties.'converter.header_name_includes'
            thinkTimeEnabled = properties.'converter.think_time_enabled'.toBoolean()

            users = Integer.parseInt(properties.'converter.users')
            rampUp = Integer.parseInt(properties.'converter.ramp_up')
            loops = Integer.parseInt(properties.'converter.loops')

            config.templateScript = properties.'converter.template_script'
        }

        return config
    }

    /**
     * Load configuration from command options.
     *
     * @param options command line options
     * @param config default configuration
     *
     * @return updated converter configuration
     */
    static ConverterConfig loadOptions(OptionAccessor options, ConverterConfig config) {
        // override default values
        if (options.'exclude-types') {
            config.contentTypeExcludes = options.'exclude-types'
        }

        if (options.'include-types') {
            config.contentTypeIncludes = options.'include-types'
        }

        if (options.'exclude-urls') {
            config.urlExcludes = options.'exclude-urls'
        }

        if (options.'include-urls') {
            config.urlIncludes = options.'include-urls'
        }

        if (options.'exclude-headers') {
            config.headerNameExcludes = options.'exclude-headers'
        }

        if (options.'include-headers') {
            config.headerNameIncludes = options.'include-headers'
        }

        if (options.'dsl-version') {
            config.version = options.'dsl-version'
        }

        if (options.'users') {
            config.users = options.'users'
        }

        if (options.'ramp-up') {
            config.rampUp = options.'ramp-up'
        }

        if (options.'loops') {
            config.loops = options.'loops'
        }

        if (options.t) {
            config.thinkTimeEnabled = true
        }

        if (options.c) {
            config.compact = true
        }

        if (options.Ps) {
            config.paramsVariables = processOptionVariables(options.Ps as List<String>)
        }

        if (options.Hs) {
            config.headerVariables = processOptionVariables(options.Hs as List<String>)
        }

        config.inputFile = options.i
        config.outputScript = options.o

        return config
    }

    @SuppressWarnings('DuplicateNumberLiteral')
    private static Map<String, String> processOptionVariables(List<String> properties) {
        Map<String, String> variables = [:]
        for (int i = 0; i < properties.size(); i += 2) {
            String param = properties[i]
            String variable = properties[i + 1]

            variables[param] = variable
        }

        return variables
    }
}
