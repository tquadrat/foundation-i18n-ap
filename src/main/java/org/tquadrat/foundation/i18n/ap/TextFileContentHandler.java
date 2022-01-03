/*
 * ============================================================================
 * Copyright © 2002-2021 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
 * Licensed to the public under the agreements of the GNU Lesser General Public
 * License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.tquadrat.foundation.i18n.ap;

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.i18n.I18nUtil.ADDITIONAL_TEXT_FILE;
import static org.tquadrat.foundation.lang.CommonConstants.UTF8;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.util.StringUtils.stream;
import static org.tquadrat.foundation.util.SystemUtils.retrieveLocale;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *  The implementation for a
 *  {@link DefaultHandler}
 *  that handles the files for additional text resources.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TextFileContentHandler.java 887 2021-03-28 19:25:19Z tquadrat $
 *  @since 0.1.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: TextFileContentHandler.java 887 2021-03-28 19:25:19Z tquadrat $" )
@API( status = INTERNAL, since = "0.1.0" )
public final class TextFileContentHandler extends DefaultHandler
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The key for the current text entry.
     */
    private String m_CurrentKey = null;

    /**
     *  The description for the current text entry.
     */
    private final StringBuilder m_CurrentDescription = new StringBuilder();

    /**
     *  The locale for the current translation.
     */
    private Locale m_CurrentLocale = null;

    /**
     *  The current text.
     */
    private final StringBuilder m_CurrentText = new StringBuilder();

    /**
     *  Flag that indicates if currently a description is being processed.
     */
    private boolean m_IsDescription = false;

    /**
     *  The document locator.
     */
    @SuppressWarnings( {"unused", "FieldCanBeLocal"} )
    private Locator m_Locator = null;

    /**
     *  The map that holds the texts. The key of this map is the locale for the
     *  text or message translation, while the values are Maps with the text
     *  entries itself, using the message or text id as the id.
     */
    private final Map<Locale,SortedMap<String,TextEntry>> m_Texts;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new {@code TextFileContentHandler} instance.
     *
     *  @param  texts   The texts for the resources.
     */
    public TextFileContentHandler( final Map<Locale,SortedMap<String,TextEntry>> texts )
    {
        m_Texts = requireNonNullArgument( texts, "texts" );
    }   //  TextFileContentHandler()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public final void characters( final char [] ch, final int start, final int length ) throws SAXException
    {
        if( length > 0 )
        {
            final var text = new String( ch, start, length ).lines().collect( joining() );
            if( m_IsDescription )
            {
                if( !text.isBlank() ) m_CurrentDescription.append( text.trim() );
            }
            else
            {
                m_CurrentText.append( text );
            }
        }
    }   //  characters()

    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings( "incomplete-switch" )
    @Override
    public final void endElement( final String uri, final String localName, final String qName ) throws SAXException
    {
        switch( localName )
        {
            //---* The root element *------------------------------------------
            case "texts" ->
            {
                m_CurrentKey = null;
                m_CurrentDescription.setLength( 0 );
                m_CurrentLocale = null;
                m_CurrentText.setLength( 0 );
                m_IsDescription = false;
            }

            //---* A single text entry *---------------------------------------
            case "text" ->
            {
                m_CurrentKey = null;
                m_CurrentDescription.setLength( 0 );
                m_CurrentText.setLength( 0 );
                m_IsDescription = false;
            }

            //---* A text resource description *-------------------------------
            case "description" -> m_IsDescription = false;

            //---* A text resource description *-------------------------------
            case "translation" ->
            {
                final var translations = m_Texts.computeIfAbsent( m_CurrentLocale, k -> new TreeMap<>() );
                translations.put( m_CurrentKey, new TextEntry( m_CurrentKey, false, m_CurrentLocale, m_CurrentDescription.toString(), m_CurrentText.toString(), ADDITIONAL_TEXT_FILE ) );

                m_CurrentLocale = null;
                m_CurrentText.setLength( 0 );
                m_IsDescription = false;
            }
        }
    }   //  endElement()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void ignorableWhitespace( final char [] ch, final int start, final int length ) throws SAXException
    {
        if( (length > 0) && !m_IsDescription )
        {
            stream( new String( ch, start, length ), '\n' ).forEach( m_CurrentText::append );
        }
    }   //  ignorableWhitespace()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final InputSource resolveEntity( final String publicId, final String systemId ) throws IOException, SAXException
    {
        InputSource retValue = null;

        if( nonNull( systemId ) && "http://dtd.tquadrat.org/AdditionalText.dtd".equals( systemId ) )
        {
            try( final var inputStream = getClass().getResourceAsStream( "/AdditionalText.dtd" ) )
            {
                if( nonNull( inputStream ) )
                {
                    final var buffer = new String( inputStream.readAllBytes(), UTF8 );
                    retValue = new InputSource( new StringReader( buffer ) );
                }
            }
        }

        if( isNull( retValue ) ) retValue = super.resolveEntity( publicId, systemId );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  resolveEntity()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void setDocumentLocator( final Locator locator ) { m_Locator = locator; }

    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings( {"incomplete-switch", "OptionalGetWithoutIsPresent"} )
    @Override
    public final void startElement( final String uri, final String localName, final String qName, final Attributes attributes ) throws SAXException
    {
        switch( localName )
        {
            //---* The root element *------------------------------------------
            case "texts" ->
            {
                m_CurrentKey = null;
                m_CurrentDescription.setLength( 0 );
                m_CurrentLocale = null;
                m_CurrentText.setLength( 0 );
                m_IsDescription = false;
            }

            //---* A single text entry *---------------------------------------
            case "text" ->
            {
                m_CurrentKey = attributes.getValue( "key" );
                m_CurrentDescription.setLength( 0 );
                m_CurrentLocale = null;
                m_CurrentText.setLength( 0 );
                m_IsDescription = false;
            }

            //---* A text resource description *-------------------------------
            case "description" ->
            {
                m_CurrentDescription.setLength( 0 );
                m_IsDescription = true;
            }

            //---* A text translation *----------------------------------------
            case "translation" ->
            {
                m_CurrentText.setLength( 0 );
                final var language = attributes.getValue( "language" );
                m_CurrentLocale = retrieveLocale( language ).get();
                m_IsDescription = false;
            }
        }
    }   //  startElement()
}
//  class TextFileContentHandler

/*
 *  End of File
 */