/*
 * ============================================================================
 *  Copyright © 2002-2021 by Thomas Thrien.
 *  All Rights Reserved.
 * ============================================================================
 *  Licensed to the public under the agreements of the GNU Lesser General Public
 *  License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *       http://www.gnu.org/licenses/lgpl.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

package org.tquadrat.foundation.i18n.ap;

import static java.lang.String.join;
import static java.text.Normalizer.Form.NFKC;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_PATH;
import static org.apiguardian.api.API.Status.STABLE;
import static org.tquadrat.foundation.i18n.I18nUtil.ADDITIONAL_TEXT_FILE;
import static org.tquadrat.foundation.i18n.I18nUtil.ADDITIONAL_TEXT_LOCATION;
import static org.tquadrat.foundation.i18n.I18nUtil.DEFAULT_BASEBUNDLENAME;
import static org.tquadrat.foundation.i18n.I18nUtil.DEFAULT_MESSAGE_PREFIX;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_STRING;
import static org.tquadrat.foundation.lang.CommonConstants.ISO8859_1;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.util.CharSetUtils.convertUnicodeToASCII;
import static org.tquadrat.foundation.util.StringUtils.format;
import static org.tquadrat.foundation.util.StringUtils.isEmptyOrBlank;
import static org.tquadrat.foundation.util.StringUtils.isNotEmptyOrBlank;
import static org.tquadrat.foundation.util.StringUtils.splitString;
import static org.tquadrat.foundation.util.StringUtils.stream;
import static org.tquadrat.foundation.util.SystemUtils.retrieveLocale;
import static org.tquadrat.foundation.util.stringconverter.LocaleStringConverter.MSG_InvalidLocaleFormat;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.ap.APBase;
import org.tquadrat.foundation.ap.AnnotationProcessingError;
import org.tquadrat.foundation.ap.IllegalAnnotationError;
import org.tquadrat.foundation.i18n.BaseBundleName;
import org.tquadrat.foundation.i18n.Message;
import org.tquadrat.foundation.i18n.MessagePrefix;
import org.tquadrat.foundation.i18n.Text;
import org.tquadrat.foundation.i18n.Texts;
import org.tquadrat.foundation.i18n.UseAdditionalTexts;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  The annotation processor for the module {@code org.tquadrat.foundation.i18n}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: I18nAnnotationProcessor.java 997 2022-01-26 14:55:05Z tquadrat $
 *  @since 0.0.1
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: I18nAnnotationProcessor.java 997 2022-01-26 14:55:05Z tquadrat $" )
@API( status = STABLE, since = "0.0.1" )
@SupportedSourceVersion( SourceVersion.RELEASE_17 )
@SupportedOptions( { APBase.ADD_DEBUG_OUTPUT, APBase.MAVEN_GOAL, ADDITIONAL_TEXT_LOCATION } )
public class I18nAnnotationProcessor extends APBase
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The base bundle name.
     */
    private String m_BaseBundleName;

    /**
     *  The default language.
     */
    private Locale m_DefaultLanguage = ENGLISH;

    /**
     *  The message prefix.
     */
    private String m_MessagePrefix;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new {@code I18NAnnotationProcessor} instance.
     */
    @SuppressWarnings( "RedundantNoArgConstructor" )
    public I18nAnnotationProcessor() { super(); }

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Generates the resource bundles from the given texts.
     *
     *  @param  textFileLocation    The provided location for
     *      {@value org.tquadrat.foundation.i18n.I18nUtil#ADDITIONAL_TEXT_FILE}.
     *  @param  texts    The texts.
     *  @param  elements    The annotated elements.
     */
    @SuppressWarnings( {"NestedTryStatement", "AssignmentToNull", "OptionalUsedAsFieldOrParameterType"} )
    private final void generateResourceBundle( final Optional<String> textFileLocation, final Map<Locale,SortedMap<String,TextEntry>> texts, final Element... elements )
    {
        final var filer = getFiler();

        try
        {
            Optional<InputSource> searchResult = Optional.empty();

            //---* Load the additional texts *---------------------------------
            if( textFileLocation.isPresent() ) searchResult = searchAdditionalTextsOnProvidedLocation( textFileLocation.get() );
            if( searchResult.isEmpty() ) searchResult = searchAdditionalTextsOnConfiguredLocation();
            if( searchResult.isEmpty() ) searchResult = searchAdditionalTextsOnSourceTree( filer );

            //---* Do the parsing *--------------------------------------------
            if( searchResult.isPresent() )
            {
                final var inputSource = searchResult.get();
                parseTextsFile( texts, inputSource );
            }
        }
        catch( final IOException e )
        {
            printMessage( ERROR, format( "Unable to read file '%s': %s", ADDITIONAL_TEXT_FILE, e.getMessage() ) );
            throw new AnnotationProcessingError( e );
        }
        catch( final SAXException e )
        {
            printMessage( ERROR, format( "Unable to parse file '%s': %s", ADDITIONAL_TEXT_FILE, e.getMessage() ) );
            throw new AnnotationProcessingError( e );
        }
        catch( final ParserConfigurationException e )
        {
            printMessage( ERROR, format( "Unable to create SAXParser: %s", e.getMessage() ) );
            throw new AnnotationProcessingError( e );
        }

        //---* Write the resource bundle properties files *--------------------
        final var filenameParts = splitString( nonNull( m_BaseBundleName ) ? m_BaseBundleName : DEFAULT_BASEBUNDLENAME, '.' );
        final var filename = new StringBuilder();

        //---* Loop over the locales *-----------------------------------------
        LocaleLoop: for( final var localeEntries : texts.entrySet() )
        {
            /*
             * This loop cannot be translated to use Map.forEach() because
             * some statements may throw checked exceptions (in particular,
             * IOException). Changing that would require to modify the API for
             * this method significantly.
             */
            final var locale = localeEntries.getKey();

            //---* Create the path name *--------------------------------------
            filename.setLength( 0 );
            filename.append( join( "/", filenameParts ) );
            if( locale != m_DefaultLanguage )
            {
                filename.append( '_' ).append( locale.toString() );
            }
            filename.append( ".properties" );

            //---* Write to the location for the generated sources *-----------
            var pathName = filename.toString();
            try
            {
                final var bundleFile = filer.createResource( SOURCE_OUTPUT, EMPTY_STRING, pathName, elements );
                pathName = bundleFile.toUri().toString();
                printMessage( NOTE, format( "Creating Resource File: %s", pathName ) );
                try( final var writer = new OutputStreamWriter( bundleFile.openOutputStream(), ISO8859_1 ) )
                {
                    writeResourceBundleFile( localeEntries.getValue().values(), writer );
                }
            }
            catch( final IOException e )
            {
                printMessage( ERROR, format( "Unable to write resource bundle file '%s': %s", pathName, e.getMessage() ) );
                throw new AnnotationProcessingError( e );
            }

            //---* Write to the location for the classes *---------------------
            /*
             * For some yet unknown reasons, sometimes one, sometimes the other
             * location works; now we decided to write to both.
             */
            pathName = filename.toString();
            try
            {
                final var bundleFile = filer.createResource( CLASS_OUTPUT, EMPTY_STRING, pathName, elements );
                pathName = bundleFile.toUri().toString();
                printMessage( NOTE, format( "Creating Resource File: %s", pathName ) );
                try( final var writer = new OutputStreamWriter( bundleFile.openOutputStream(), ISO8859_1 ) )
                {
                    writeResourceBundleFile( localeEntries.getValue().values(), writer );
                }
            }
            catch( final IOException e )
            {
                printMessage( ERROR, format( "Unable to write resource bundle file '%s': %s", pathName, e.getMessage() ) );
                throw new AnnotationProcessingError( e );
            }
        }   //  LocaleLoop:
    }   //  generateResourceBundle()

    /**
     *  {@inheritDoc}
     */
    @Override
    protected final Collection<Class<? extends Annotation>> getSupportedAnnotationClasses()
    {
        final Collection<Class<? extends Annotation>> retValue = List.of
        (
            BaseBundleName.class, Message.class, MessagePrefix.class, Text.class, Texts.class, UseAdditionalTexts.class
        );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getSupportedAnnotationClasses()

    /**
     *  Parses a texts XML file.
     *
     *  @param  texts   The data structure that takes the parsed texts.
     *  @param  inputSource The XML input stream.
     *  @throws IOException Reading the input failed.
     *  @throws ParserConfigurationException    It was not possible to obtain a
     *      {@link SAXParserFactory}
     *      or it could not be configured properly.
     *  @throws SAXException    Parsing the input file failed.
     */
    private final void parseTextsFile( final Map<Locale,SortedMap<String,TextEntry>> texts, final InputSource inputSource ) throws IOException, ParserConfigurationException, SAXException
    {
        /*
         * Obtain an instance of an XMLReader implementation from a system
         * property.
         */
        final var saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setValidating( true );
        saxParserFactory.setNamespaceAware( true );
        final var saxParser = saxParserFactory.newSAXParser();

        //---* Create a content handler *--------------------------------------
        final var contentHandler = new TextFileContentHandler( requireNonNullArgument( texts, "texts" ) );

        //---* Do the parsing *------------------------------------------------
        saxParser.parse( requireNonNullArgument( inputSource, "inputSource" ), contentHandler );
    }   //  parseTextsFile()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment )
    {
        //---* Tell them who we are *------------------------------------------
        final var message = annotations.isEmpty() ? "No annotations to process" : annotations.stream()
            .map( TypeElement::getQualifiedName )
            .collect( joining( "', '", "Processing the annotation" + (annotations.size() > 1 ? "s '" : " '"), "'" ) );
        printMessage( NOTE, message );

        final var retValue = !roundEnvironment.errorRaised() && !annotations.isEmpty();
        if( retValue )
        {
            final Map<Locale,SortedMap<String,TextEntry>> texts = new HashMap<>();
            final Collection<Element> processedElements = new HashSet<>();

            if( !annotations.isEmpty() )
            {
                //---* Get the message prefix *--------------------------------
                retrieveAnnotatedField( roundEnvironment, MessagePrefix.class )
                    .ifPresent( variableElement -> m_MessagePrefix = variableElement.getConstantValue().toString() );

                //---* Get the base bundle name *------------------------------
                retrieveAnnotatedField( roundEnvironment, BaseBundleName.class )
                    .ifPresent( variableElement ->
                    {
                        m_BaseBundleName = variableElement.getConstantValue().toString();
                        final var annotation = variableElement.getAnnotation( BaseBundleName.class );
                        final var defaultLanguageCode = annotation.defaultLanguage();
                        m_DefaultLanguage = retrieveLocale( defaultLanguageCode ).orElseThrow( () -> new IllegalArgumentException( format( MSG_InvalidLocaleFormat, defaultLanguageCode ) ) );
                    } );

                /*
                 *  Collect the elements that are annotated with @Text or
                 *  @Message.
                 */
                if( annotations.stream()
                    .map( TypeElement::getQualifiedName )
                    .map( Object::toString )
                    .anyMatch( n -> n.equals( Texts.class.getName() ) || n.equals( Text.class.getName() ) || n.equals( Message.class.getName() ) ) )
                {
                    final var textCollector = new TextCollector( this, nonNull( m_MessagePrefix ) ? m_MessagePrefix : DEFAULT_MESSAGE_PREFIX );
                    for( final var element : roundEnvironment.getElementsAnnotatedWith( Message.class ) )
                    {
                        if( element instanceof VariableElement )
                        {
                            element.accept( textCollector, texts );
                            processedElements.add( element );
                        }
                        else
                        {
                            printMessage( ERROR, format( MSG_IllegalAnnotationUse, element.getSimpleName().toString(), BaseBundleName.class.getSimpleName() ), element );
                            throw new IllegalAnnotationError( BaseBundleName.class );
                        }
                    }

                    final Collection<Element> textAnnotatedElements = new HashSet<>( roundEnvironment.getElementsAnnotatedWith( Text.class ) );
                    textAnnotatedElements.addAll( roundEnvironment.getElementsAnnotatedWith( Texts.class ) );
                    for( final var element : textAnnotatedElements )
                    {
                        if( (element instanceof VariableElement) || (element.getKind() == METHOD) )
                        {
                            element.accept( textCollector, texts );
                            processedElements.add( element );
                        }
                        else
                        {
                            printMessage( ERROR, format( MSG_IllegalAnnotationUse, element.getSimpleName().toString(), BaseBundleName.class.getSimpleName() ), element );
                            throw new IllegalAnnotationError( Text.class );
                        }
                    }
                }
            }

            /*
             * Even when no text or message annotation was found, there could
             * be still some additional texts.
             */
            final List<Element> useAdditionalTextsAnnotatedElements = new ArrayList<>( roundEnvironment.getElementsAnnotatedWith( UseAdditionalTexts.class ) );
            final Optional<String> textFileLocation = switch( useAdditionalTextsAnnotatedElements.size() )
            {
                case 0 -> Optional.empty();
                case 1 ->
                    {
                        final var annotation = useAdditionalTextsAnnotatedElements.get( 0 ).getAnnotation( UseAdditionalTexts.class );
                        final var location = annotation.location();
                        yield isEmptyOrBlank( location ) ? Optional.empty() : Optional.of( location );
                    }
                default ->
                {
                    final var msg = format( MSG_MultipleElements, UseAdditionalTexts.class.getSimpleName() );
                    printMessage( ERROR, msg );
                    throw new IllegalAnnotationError( msg, UseAdditionalTexts.class );
                }
            };

            printMessage( NOTE, "Generate ResourceBundles" );
            generateResourceBundle( textFileLocation, texts, processedElements.toArray( Element []::new ) );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  process()

    /**
     *  Searches the file with the additional texts
     *  ({@value org.tquadrat.foundation.i18n.I18nUtil#ADDITIONAL_TEXT_FILE})
     *  on the location configured with the annotation processor option
     *  {@value org.tquadrat.foundation.i18n.I18nUtil#ADDITIONAL_TEXT_LOCATION}.
     *
     *  @return An instance of
     *      {@link Optional}
     *      that holds the
     *      {@link InputSource}
     *      for the file.
     */
    private final Optional<InputSource> searchAdditionalTextsOnConfiguredLocation()
    {
        Optional<InputSource> retValue = Optional.empty();

        final var option = getOption( ADDITIONAL_TEXT_LOCATION );
        if( option.isPresent() )
        {
            final var folder = new File( option.get() );
            if( folder.exists() && folder.isDirectory() )
            {
                var textFile = new File( folder, ADDITIONAL_TEXT_FILE );
                var textFileName = textFile.getAbsolutePath();
                try
                {
                    textFile = textFile.getCanonicalFile().getAbsoluteFile();
                    textFileName = textFile.getAbsolutePath();
                    if( textFile.exists() && textFile.isFile() )
                    {
                        final var inputStream = new FileInputStream( textFile );

                        //---* Create the return value *-----------------------
                        retValue = Optional.of( new InputSource( inputStream ) );

                        textFileName = textFile.toURI().toURL().toExternalForm();
                        printMessage( NOTE, format( "Reading additional texts from '%s' (configured location)", textFileName ) );
                    }
                }
                catch( @SuppressWarnings( "OverlyBroadCatchBlock" ) final IOException e )
                {
                    printMessage( NOTE, format( "Cannot open file '%s' with additional texts", textFileName ) );
                }
            }
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  searchAdditionalTextsOnConfiguredLocation

    /**
     *  Searches the file with the additional texts
     *  ({@value org.tquadrat.foundation.i18n.I18nUtil#ADDITIONAL_TEXT_FILE})
     *  on the location provided by the annotation
     *  {@link UseAdditionalTexts &#64;UseAdditionalTexts}.
     *
     *  @param  location    The location for the file with the additional
     *      texts.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the
     *      {@link InputSource}
     *      for the file.
     */
    private final Optional<InputSource> searchAdditionalTextsOnProvidedLocation( final String location )
    {
        Optional<InputSource> retValue = Optional.empty();

        final var folder = new File( location );
        if( folder.exists() && folder.isDirectory() )
        {
            var textFile = new File( folder, ADDITIONAL_TEXT_FILE );
            var textFileName = textFile.getAbsolutePath();
            try
            {
                textFile = textFile.getCanonicalFile().getAbsoluteFile();
                textFileName = textFile.getAbsolutePath();
                if( textFile.exists() && textFile.isFile() )
                {
                    final var inputStream = new FileInputStream( textFile );

                    //---* Create the return value *---------------------------
                    retValue = Optional.of( new InputSource( inputStream ) );

                    textFileName = textFile.toURI().toURL().toExternalForm();
                    printMessage( NOTE, format( "Reading additional texts from '%s' (provided location)", textFileName ) );
                }
            }
            catch( @SuppressWarnings( "OverlyBroadCatchBlock" ) final IOException e )
            {
                printMessage( NOTE, format( "Cannot open file '%s' with additional texts", textFileName ) );
            }
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  searchAdditionalTextsOnProvidedLocation

    /**
     *  Searches the file with the additional texts
     *  ({@value org.tquadrat.foundation.i18n.I18nUtil#ADDITIONAL_TEXT_FILE})
     *  on the location determined by
     *  {@link javax.tools.StandardLocation#SOURCE_PATH}.
     *
     *  @param filer    The
     *      {@link Filer}
     *      instance that provides the {@code SOURCE_PATH} location.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the
     *      {@link InputSource}
     *      for the file.
     */
    @SuppressWarnings( "AssignmentToNull" )
    private final Optional<InputSource> searchAdditionalTextsOnSourceTree( final Filer filer )
    {
        Optional<InputSource> retValue = Optional.empty();

        FileObject textFile;
        try
        {
            textFile = filer.getResource( SOURCE_PATH, EMPTY_STRING, ADDITIONAL_TEXT_FILE );
        }
        catch( @SuppressWarnings( "unused" ) final IOException e )
        {
            printMessage( NOTE, format( "Cannot open file '%s' with additional texts", ADDITIONAL_TEXT_FILE ) );
            textFile = null;
        }
        if( nonNull( textFile ) )
        {
            var textFileName = ADDITIONAL_TEXT_FILE;
            try
            {
                textFileName = textFile.toUri().toURL().toExternalForm();

                //---* Create the input stream *-------------------------------
                final var inputStream = textFile.openInputStream();

                //---* Create the input source *-------------------------------
                final var inputSource = new InputSource( inputStream );

                //---* Create the return value *-------------------------------
                retValue = Optional.of( inputSource );

                printMessage( NOTE, format( "Reading additional texts from '%s' (source tree)", textFileName ) );
            }
            catch( @SuppressWarnings( "OverlyBroadCatchBlock" ) final IllegalArgumentException | IOException e )
            {
                printMessage( ERROR, format( "Unable to read file '%s' with additional texts: %s", textFileName, e.toString() ) );
                throw new AnnotationProcessingError( e );
            }
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  searchAdditionalTextsOnSourceTree()

    /**
     *  Write the resource bundle properties to the given
     *  {@link Writer}.
     *
     *  @param  data    The properties.
     *  @param  writer  The target {@code Writer} instance.
     *  @throws IOException Writing the resource bundle file failed.
     */
    @SuppressWarnings( "resource" )
    private final void writeResourceBundleFile( final Collection<TextEntry> data, final Writer writer ) throws IOException
    {
        requireNonNullArgument( writer, "writer" );

        final var encoder = ISO8859_1.newEncoder();

        writer.append(
            """
            # suppress inspection "TrailingSpacesInProperty" for whole file
            """ );

        //---* Loop over the text entries *------------------------------------
        TextLoop: for( final var entry : requireNonNullArgument( data, "data" ) )
        {
            var text = entry.text();
            if( !encoder.canEncode( text ) ) text = convertUnicodeToASCII( NFKC, text );

            final var description = stream( entry.description(), '\n' )
                .collect( joining( "\n# ", "# ", "\n" ) );
            writer.append( description );
            if( isNotEmptyOrBlank( entry.className() ) )
            {
                writer.append( "# Defined in: " )
                    .append( entry.className() )
                    .append( '\n' );
            }
            writer.append( entry.key() )
                .append( '=' )
                .append( text )
                .append( '\n' )
                .append( '\n' );
        }   //  TextLoop:
    }   //  writeResourceBundleFile()
}
//  class I18nAnnotationProcessor

/*
 *  End of File
 */