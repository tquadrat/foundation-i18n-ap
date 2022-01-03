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

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.tools.Diagnostic.Kind.ERROR;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.i18n.I18nUtil.composeMessageKey;
import static org.tquadrat.foundation.i18n.I18nUtil.composeTextKey;
import static org.tquadrat.foundation.i18n.TextUse.NAME;
import static org.tquadrat.foundation.i18n.TextUse.STRING;
import static org.tquadrat.foundation.i18n.TextUse.TEXTUSE_DEFAULT;
import static org.tquadrat.foundation.i18n.TextUse.TXT;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.lang.Objects.requireNotEmptyArgument;
import static org.tquadrat.foundation.util.JavaUtils.isAddMethod;
import static org.tquadrat.foundation.util.JavaUtils.isGetter;
import static org.tquadrat.foundation.util.JavaUtils.isSetter;
import static org.tquadrat.foundation.util.JavaUtils.retrievePropertyName;
import static org.tquadrat.foundation.util.StringUtils.capitalize;
import static org.tquadrat.foundation.util.StringUtils.format;
import static org.tquadrat.foundation.util.SystemUtils.retrieveLocale;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor9;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.ap.APHelper;
import org.tquadrat.foundation.ap.IllegalAnnotationError;
import org.tquadrat.foundation.exception.UnsupportedEnumError;
import org.tquadrat.foundation.i18n.Message;
import org.tquadrat.foundation.i18n.Text;
import org.tquadrat.foundation.i18n.TextUse;
import org.tquadrat.foundation.i18n.Texts;
import org.tquadrat.foundation.i18n.Translation;

/**
 *  A visitor class that collects the texts for resource bundle properties
 *  files from the annotations.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TextCollector.java 933 2021-07-03 13:32:17Z tquadrat $
 *  @since 0.0.2
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: TextCollector.java 933 2021-07-03 13:32:17Z tquadrat $" )
@API( status = INTERNAL, since = "0.1.0" )
public class TextCollector extends SimpleElementVisitor9<Void,Map<Locale,SortedMap<String,TextEntry>>>
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  Some helper utilities for the work with
     *  {@link Element}
     *  instances.
     */
    private final Elements m_ElementUtils;

    /**
     *  The prefix for the message ids.
     */
    private final String m_MessagePrefix;

    /**
     *  The processing environment.
     */
    private final APHelper m_Environment;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new {@code TextCollector} instance.
     *
     *  @param  environment The processing environment for the annotation
     *      processor.
     *  @param  messagePrefix   The configured message prefix.
     */
    public TextCollector( final APHelper environment, final String messagePrefix )
    {
        m_Environment = requireNonNullArgument( environment, "environment" );
        m_ElementUtils = m_Environment.getElementUtils();

        m_MessagePrefix = requireNotEmptyArgument( messagePrefix, "messagePrefix" );
    }   // TextCollector()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Adds the text entries to the texts map.
     *
     *  @param  texts   The texts map.
     *  @param  element The annotated element.
     *  @param  key The resource bundle key.
     *  @param  description The description for the text.
     *  @param  className   The fully qualified name of the class that defines
     *      the text.
     *  @param  isMessage   {@code true} if the text is a message,
     *      {@code false} otherwise.
     *  @param  translations    The texts in the various languages.
     */
    @SuppressWarnings( {"MethodCanBeVariableArityMethod", "OptionalGetWithoutIsPresent"} )
    private final void addTextEntry( @SuppressWarnings( "BoundedWildcard" ) final Map<Locale,SortedMap<String,TextEntry>> texts, final Element element, final String key, final String description, final String className, final boolean isMessage, final Translation [] translations )
    {
        for( final var translation : requireNonNullArgument( translations, "translations" ) )
        {
            //---* Create the entry *------------------------------------------
            final var locale = retrieveLocale( translation.language() ).get();
            final var text = translation.text();
            final var entry = new TextEntry( key, isMessage, locale, description, text, className );

            //---* Get the text map *------------------------------------------
            final var textMap = texts.computeIfAbsent( locale, k -> new TreeMap<>() );

            //---* Add the entry *---------------------------------------------
            if( textMap.containsKey( key ) )
            {
                final var message = format(
                    """
                    Key '%s' is not unique (Annotation: %s in class '%s')
                    Check translation locale in case the key is not a real duplicate""",
                    key, isMessage ? "@Message" : "@Text", className );
                m_Environment.printMessage( ERROR, message, element );
                throw new IllegalAnnotationError( message );
            }
            textMap.put( key, entry );
        }
    }   //  addTextEntry()

    /**
     *  Processes a text annotation.
     *
     *  @param  texts   The texts map.
     *  @param  element The annotated element.
     *  @param  annotation  The text annotation.
     *  @param  className   The fully qualified name of the class that defines
     *      the text.
     */
    private final void processTextAnnotation( final Map<Locale,SortedMap<String,TextEntry>> texts, final Element element, final Text annotation, final String className )
    {
        //---* The description of the text *-----------------------------------
        final var description = annotation.description();

        //---* The text use and id *-------------------------------------------
        var textUse = annotation.use();
        var id = annotation.id();
        KindSwitch: switch( element.getKind() )
        {
            case ENUM_CONSTANT ->
            {
                if( textUse == TEXTUSE_DEFAULT ) textUse = STRING;
                if( id.isBlank() ) id = element.getSimpleName().toString();
            }

            case METHOD ->
            {
                if( id.isBlank() )
                {
                    if( isGetter( element ) || isSetter( element ) || isAddMethod( element ) )
                    {
                        /*
                         * Only for getters, setters and "add" methods, we can
                         * infer the id from the name of the property.
                         */
                        id = capitalize( retrievePropertyName( (ExecutableElement) element ) );
                        if( textUse == TEXTUSE_DEFAULT ) textUse = NAME;
                    }
                    else
                    {
                        final var message = format( "Missing id for Element '%s'", element.getSimpleName().toString() );
                        m_Environment.printMessage( ERROR, message, element );
                        throw new IllegalAnnotationError( message );
                    }
                }
            }

            case FIELD ->
            {
                if( id.isBlank() )
                {
                    /*
                     * Get the name of the variable that is annotated; this is
                     * used as the id for the text if none is set explicitly.
                     */
                    id = element.getSimpleName().toString();
                    if( id.startsWith( "m_" ) )
                    {
                        id = id.substring( 2 );
                    }
                    else
                    {
                        final var pos = id.indexOf( '_' );
                        if( pos > 1 )
                        {
                            try
                            {
                                textUse = TextUse.valueOf( id.substring( 0, pos ) );
                            }
                            catch( final IllegalArgumentException e )
                            {
                                final var message = format( "Id '%s' is invalid: %s", id, e.toString() );
                                m_Environment.printMessage( ERROR, message, element );
                                throw new IllegalAnnotationError( message, e );
                            }
                            id = id.substring( pos + 1 );
                        }
                    }
                }
            }

            //$CASES-OMITTED$
            default -> throw new UnsupportedEnumError( element.getKind() );
        }   //  KindSwitch:

        if( textUse == TEXTUSE_DEFAULT ) textUse = TXT;

        //---* Add the new text *----------------------------------------------
        addTextEntry( texts, element, composeTextKey( className, textUse, id ), description, className, false, annotation.translations() );
    }   //  processTextAnnotation()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Void visitExecutable( final ExecutableElement element, final Map<Locale,SortedMap<String,TextEntry>> texts )
    {
        if( element.getKind() == METHOD )
        {
            //---* Get the defining class *------------------------------------
            final var definingClass = (TypeElement) element.getEnclosingElement();
            final var className = m_ElementUtils.getBinaryName( definingClass ).toString();

            // ---* Get the annotation *------------------------------------
            final var textAnnotation = element.getAnnotation( Text.class );
            final var textsAnnotation = element.getAnnotation( Texts.class );

            if( nonNull( textsAnnotation ) )
            {
                for( final var annotation : textsAnnotation.value() )
                {
                    processTextAnnotation( texts, element, annotation, className );
                }
            }
            else if( nonNull( textAnnotation ) )
            {
                processTextAnnotation( texts, element, textAnnotation, className );
            }
        }

        //---* Done *----------------------------------------------------------
        return defaultAction( element, texts );
    }   //  visitExecutable()

    /**
     * {@inheritDoc}
     */
    @Override
    public final Void visitVariable( final VariableElement element, final Map<Locale,SortedMap<String,TextEntry>> texts )
    {
        if( element.getKind().isField() )
        {
            //---* Get the defining class *------------------------------------
            final var definingClass = (TypeElement) element.getEnclosingElement();
            final var className = m_ElementUtils.getBinaryName( definingClass ).toString();

            // ---* Get the annotation *------------------------------------
            final var msgAnnotation = element.getAnnotation( Message.class );
            final var textAnnotation = element.getAnnotation( Text.class );
            final var textsAnnotation = element.getAnnotation( Texts.class );

            if( nonNull( msgAnnotation ) )
            {
                /*
                 * Get the constant value for the variable; this is used as the
                 * id for the message.
                 */
                final var value = element.getConstantValue();

                /*
                 * Get the name of the variable that is annotated; this is used
                 * as the id for the message if none is set explicitly.
                 */
                final var variableName = element.getSimpleName();

                //---* The description of the message *------------------------
                final var description = msgAnnotation.description();

                //---* The message id *----------------------------------------
                String key = null;
                if( value instanceof Integer msgId )
                {
                    key = composeMessageKey( m_MessagePrefix, msgId.intValue() );
                }
                else if( nonNull( value ) )
                {
                    key = composeMessageKey( m_MessagePrefix, value.toString() );
                }
                else
                {
                    key = composeMessageKey( m_MessagePrefix, variableName.toString() );
                }

                //---* Add the new text *--------------------------------------
                addTextEntry( texts, element, key, description, className, true, msgAnnotation.translations() );
            }

            if( nonNull( textAnnotation ) )
            {
                processTextAnnotation( texts, element, textAnnotation, className );
            }

            if( nonNull( textsAnnotation ) )
            {
                for( final var annotation : textsAnnotation.value() )
                {
                    processTextAnnotation( texts, element, annotation, className );
                }
            }
        }

        //---* Done *----------------------------------------------------------
        return defaultAction( element, texts );
    }   // visitVariable()
}
// class TextCollector

/*
 *  End of File
 */