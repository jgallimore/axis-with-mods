/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis.components.encoding;

import org.apache.axis.i18n.Messages;

import java.io.IOException;
import java.io.Writer;

/**
 * UTF-8 Encoder.
 * 
 * @author <a href="mailto:jens@void.fm">Jens Schumann</a>
 * @see <a href="http://encoding.org">encoding.org</a>
 * @see <a href="http://czyborra.com/utf/#UTF-8">UTF 8 explained</a>
 */
class UTF8Encoder extends AbstractXMLEncoder {
    /**
     * gets the encoding supported by this encoder
     * 
     * @return string
     */
    public String getEncoding() {
        return XMLEncoderFactory.ENCODING_UTF_8;
    }

    /**
     * write the encoded version of a given string
     * 
     * @param writer    writer to write this string to
     * @param xmlString string to be encoded
     */
    public void writeEncoded(Writer writer, String xmlString)
            throws IOException {
        if (xmlString == null) {
            return;
        }
        int length = xmlString.length();
        char character;
        for (int i = 0; i < length; i++) {
            character = xmlString.charAt( i );
            switch (character) {
                // we don't care about single quotes since axis will
                // use double quotes anyway
                case '&':
                    writer.write(AMP);
                    break;
                case '"':
                    writer.write(QUOTE);
                    break;
                case '<':
                    writer.write(LESS);
                    break;
                case '>':
                    writer.write(GREATER);
                    break;
                case '\n':
                    writer.write(LF);
                    break;
                case '\r':
                    writer.write(CR);
                    break;
                case '\t':
                    writer.write(TAB);
                    break;
                default:
                    if (character < 0x20) {
                        throw new IllegalArgumentException(Messages.getMessage(
                                "invalidXmlCharacter00",
                                Integer.toHexString(character),
                                xmlString.substring(0, i)));
                    } else if (character > 0x7F) {
                        writer.write("&#x");
                        writer.write(Integer.toHexString(character).toUpperCase());
                        writer.write(";");
                    } else {
                        writer.write(character);
                    }
                    break;
            }
        }
    }
}
