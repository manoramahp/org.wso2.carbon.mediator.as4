/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.mediator.as4.datasources;

import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Data source implementation which can be initialised with an input stream.
 */
public class InputStreamDataSource implements DataSource {
    public static final String DEFAULT_TYPE = "application/octet-stream";
    private final InputStream in;
    private final String ctype;

    /**
     * Constructor which takes input stream.
     *
     * @param in
     */
    public InputStreamDataSource(InputStream in) {
        this(in, null);
    }

    /**
     * Constructor which takes input stream and type.
     *
     * @param in
     * @param ctype
     */
    public InputStreamDataSource(InputStream in, String ctype) {
        this.in = in;
        this.ctype = (ctype != null ? ctype : DEFAULT_TYPE);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public String getContentType() {
        return this.ctype;
    }

    @Override
    public String getName() {
        return null;
    }
}
