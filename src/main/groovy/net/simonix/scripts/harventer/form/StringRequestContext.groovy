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
package net.simonix.scripts.harventer.form

import groovy.transform.CompileStatic
import org.apache.commons.fileupload.RequestContext

/**
 * Implementation of {@link RequestContext} for parsing <pre>multipart/form-data</pre> from string
 */
@CompileStatic
class StringRequestContext implements RequestContext {

    String encoding
    String type
    int length
    BufferedInputStream stream

    StringRequestContext(String text, String type, String encoding) {
        this.stream = new BufferedInputStream(new ByteArrayInputStream(text.getBytes(encoding)))
        this.type = type
        this.encoding = encoding
        this.length = text.size()
    }

    @Override
    String getCharacterEncoding() {
        return encoding
    }

    @Override
    String getContentType() {
        return type
    }

    @Override
    int getContentLength() {
        return length
    }

    @Override
    InputStream getInputStream() throws IOException {
        return stream
    }
}

