## Groovy Harventer 

Converts .HAR files to [Groovy JMeter DSL](https://github.com/smicyk/groovy-jmeter) scripts.

The [HTTP Archive format](http://www.softwareishard.com/blog/har-12-spec/) is used store communication data (HTTP protocol) between the server and the client.

The .HAR files can be obtained by various tools (including all major browsers).

### Prerequisites

Before start, you should have:

- [Java 11+](https://openjdk.java.net/)

### How to start

Download [latest](https://github.com/smicyk/groovy-harventer/releases/latest) version from releases page and unpack in any folder. Go to __bin__ folder and execute following command:
```shell script
$ harventer -i yourharfile.har -o yourdslscript.groovy
```

If in trouble try check 'help' option:
```shell script
$ harventer --help

usage: harventer [options]
 -c,--compact                             If present the param and headers
                                          will be generated in compact
                                          form
    --dsl-version <arg>                   Specify which DSL version to use
                                          for generated script
    --exclude-headers <pattern>           Regex pattern for header name to
                                          exclude (by default all headers
                                          are excluded)
    --exclude-types <pattern>             Regex pattern for response
                                          content type to exclude (by
                                          default css, javascript, images
                                          and binary types are excluded)
    --exclude-urls <pattern>              Regex pattern for URL to exclude
                                          (by default .css, .js, .bmp,
                                          .css, .js, .gif, .ico, .jpg,
                                          .jpeg, .png, .swf, .woff,
                                          .woff2)
 -h,--help                                Show help
 -H,--header-variables <param=variable>   Substitute header with variable
                                          (applies to request headers)
 -i,--input-file <file>                   Input *.har file
    --include-headers <pattern>           Regex pattern for header name to
                                          include
    --include-types <pattern>             Regex pattern for response
                                          content type to include
    --include-urls <pattern>              Regex pattern for URL to include
    --loops=<number>                      Loops number for users
 -o,--output-file <file>                  Output *.groovy file
 -P,--param-variables <param=variable>    Substitute param with variable
                                          (applies to request params)
    --ramp-up <number>                    Ramp up time for test plan
 -t,--think-time                          If present each HTTP request has
                                          think time based on real
                                          execution
 -U,--url-variables <param=variable>      Substitute part of URL with
                                          variable
    --users <number>                      Number of users for default
                                          group
```

### Examples

Below you can find several examples of command execution:

* the converter excludes all headers for HTTP request, but you can provide which you want to have in the script (use '|' to put several names)

  ```shell script
  $ harventer -i yourharfile.har -o yourdslscript.groovy --include-headers "X-CSRF-TOKEN|Host"
  ```
  The output from the command should look like code below (the converter adds header X-CSRF-TOKEN and Host if they exist):
  ```groovy
  start {
      plan {
          summary(path: 'yourdslscript.jtl', enabled: true)
          group(users: 1, rampUp: 1, loops: 1) {

              // defaults comes from the first request available in .HAR file
              defaults(protocol: 'http', domain: 'localhost', port: 80)

              http('POST /app') {
                  // headers are automatically added to http request
                  headers {
                      header(name: 'X-CSRF-TOKEN', value: '2429304892384092384093')
                      header(name: 'Host', value: 'localhost')
                  }
                  params {
                      param(name: 'param1', value: 'value1')
                  }
              }
          }
      }
  }
  ```

* the __--haeder-variables__ option creates JMeter variables placeholders for matching headers

  ```
  $ harventer -i yourharfile.har -o yourdslscript.groovy --include-headers "X-CSRF-TOKEN" --header-variables "X-CSRF-TOKEN=var_csrfToken"
  ```

  The output is very similar to previous one but this time, values for headers comes from defined variable:

  ```groovy
  start {
      plan {
          summary(path: 'yourdslscript.jtl', enabled: true)
          group(users: 1, rampUp: 1, loops: 1) {
              variables {
                  // variable defined on the command line
                  variable(name: 'var_csrfToken', value: '')
              }

              // defaults comes from the first request available in .HAR file
              defaults(protocol: 'http', domain: 'localhost', port: 80)

              http('POST /app') {
                  // headers are automatically added to http request
                  headers {
                      header(name: 'X-CSRF-TOKEN', value: '${var_csrfToken')
                  }
                  params {
                      param(name: 'param1', value: 'value1')
                  }
              }
          }
      }
  }
  ```
  The options __--param-variables__ works pretty much the same but for HTTP parameters

* Next example shows how to automatically add users, ramp up and loops for you test plan, with __--users__,  __--ramp-up__ and __--loops__ options:

  ```shell script
  $ harventer -i yourharfile.har -o yourdslscript.groovy --users 10 --rampUp 60 --loops 10
  ```

  ```groovy
  start {
      plan {
          group(users: 10, rampUp: 60, loops: 10) {
              // rest of script
          }
      }
  }
  ```

* The __--think-time__ option add small wait time between each sample execution. Converter extracts times from .HAR files. With this option the script can more precisely simulate real user interaction.

  ```shell script
  $ harventer -i yourharfile.har -o yourdslscript.groovy --think-time
  ```
  Output from the command:
  ```groovy
  start {
      plan {
          group(users: 1, rampUp: 1, loops: 1) {
              http('POST /app') { }
              flow(name: 'Think Time', action: 'pause', duration: 0) {
                  uniform_timer(name: 'Pause', deloay: '716', range: 100)
              }

              http('GET /app/form') { }
              flow(name: 'Think Time', action: 'pause', duration: 0) {
                  uniform_timer(name: 'Pause', deloay: '300', range: 100)
              }
          }
      }
  }
  ```