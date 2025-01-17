/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.dgn;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.SummaryExtractor;
import org.apache.tika.sax.XHTMLContentHandler;

/**
 * This is a VERY LIMITED parser. It parses metadata out of dgn8 files.
 */
public class DGN8Parser extends AbstractParser {

    Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.image("vnd.dgn; version=8"));

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata,
                      ParseContext context) throws IOException, SAXException, TikaException {
        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();
        SummaryExtractor summaryExtractor = new SummaryExtractor(metadata);
        final DirectoryNode root;
        TikaInputStream tstream = TikaInputStream.cast(stream);
        POIFSFileSystem mustCloseFs = null;
        try {
            if (tstream == null) {
                mustCloseFs = new POIFSFileSystem(CloseShieldInputStream.wrap(stream));
                root = mustCloseFs.getRoot();
            } else {
                final Object container = tstream.getOpenContainer();
                if (container instanceof POIFSFileSystem) {
                    root = ((POIFSFileSystem) container).getRoot();
                } else if (container instanceof DirectoryNode) {
                    root = (DirectoryNode) container;
                } else {
                    POIFSFileSystem fs = null;
                    if (tstream.hasFile()) {
                        fs = new POIFSFileSystem(tstream.getFile(), true);
                    } else {
                        fs = new POIFSFileSystem(CloseShieldInputStream.wrap(tstream));
                    }
                    // tstream will close the fs, no need to close this below
                    tstream.setOpenContainer(fs);
                    root = fs.getRoot();                }
            }
            summaryExtractor.parseSummaries(root);
        } finally {
            IOUtils.closeQuietly(mustCloseFs);
        }
        xhtml.endDocument();
    }
}
