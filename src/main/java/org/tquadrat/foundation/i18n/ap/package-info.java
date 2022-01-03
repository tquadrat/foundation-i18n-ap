/*
 * ============================================================================
 * Copyright © 2002-2021 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
 * Licensed to the public under the agreements of the GNU Lesser General Public
 * License, version 3.0 (the "License"). You may obtain a copy of the License at
 * http://www.gnu.org/licenses/lgpl.html
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 *  <p>{@summary This project provides an annotation processor that
 *  externalises messages and text that were defined by the annotations from
 *  the <code>org.tquadrat.foundation.i18n</code> module.}</p>
 *
 *  <h2>Use the Annotation Processor with plain <code>javac</code></h2>
 *  <p>That {@code javac} can find the annotation processor requires to add the
 *  {@code *.jar} file with the package {@code org.tquadrat.foundation.i18n.ap}
 *  to the {@code CLASSPATH}; from there {@code javac} will detect the
 *  annotation processor automatically; or the class name for the annotation
 *  processor,
 *  {@link org.tquadrat.foundation.i18n.ap.I18nAnnotationProcessor}, can be
 *  provided explicitly with the {@code -processor} option.</p>
 *  <p>Finally, the destination for the generated sources has to be set with
 *  the {@code -s} option.</p>
 *
 *  <div style="border-style: solid; border-radius: 8px; margin-left: 10px; padding-left:5px; padding-right:5px;">
 *  <p>The
 *  {@href https://docs.oracle.com/en/java/javase/15/docs/specs/man/javac.html documentation for <code>javac</code>}
 *  says &quot;<cite>[&hellip;]Unless annotation processing is disabled with
 *  the {@code -proc:none} option, the compiler searches for any annotation
 *  processors that are available. The search path can be specified with the
 *  {@code -processorpath} option. If no path is specified, then the user class
 *  path is used. Processors are located by means of service
 *  provider-configuration files named
 *  {@code META-INF/services/javax.annotation.processing.Processor} on the
 *  search path. Such files should contain the names of any annotation
 *  processors to be used, listed one per line. Alternatively, processors can
 *  be specified explicitly, using the {@code -processor}
 *  option.[&hellip;]</cite>&quot;</p> </div>
 *
 *  <h2>Configure Maven to use the Annotation Processor</h2>
 *  <p>For Maven, the following lines must be added to the <code>pom.xml</code>
 *  of the project to build:</p>
 *  <blockquote><pre><code>&hellip;
 *  &lt;dependencies&gt;
 *      &hellip;
 *      &lt;dependency&gt;
 *          &lt;groupId&gt;org.tquadrat.library&lt;/groupId&gt;
 *          &lt;artifactId&gt;org.tquadrat.foundation.i18n&lt;/artifactId&gt;
 *          &lt;version&gt;<i>proper_version</i>&lt;/version&gt;
 *          &lt;scope&gt;compile&lt;/scope&gt;
 *      &lt;/dependency&gt;
 *      &hellip;
 *  &lt;/dependencies&gt;
 *  &hellip;
 *  &lt;build&gt;
 *      &hellip;
 *      &lt;plugins&gt;
 *          &hellip;
 *          &lt;plugin&gt;
 *              &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
 *              &lt;artifactId&gt;maven-compiler-plugin&lt;/artifactId&gt;
 *              &hellip;
 *              &lt;configuration&gt;
 *                  &hellip;
 *                  &lt;annotationProcessorPaths&gt;
 *                      &lt;annotationProcessorPath&gt;
 *                          &lt;groupId&gt;org.tquadrat.tool&lt;/groupId&gt;
 *                          &lt;artifactId&gt;org.tquadrat.foundation.i18n.ap&lt;/artifactId&gt;
 *                          &lt;version&gt;<i>proper_version</i>&lt;/version&gt;
 *                      &lt;/annotationProcessorPath&gt;
 *                      &hellip;
 *                  &lt;/annotationProcessorPaths&gt;
 *                  &hellip;
 *              &lt;/configuration&gt;
 *              &hellip;
 *              &lt;executions&gt;
 *                  &hellip;
 *                  &lt;execution&gt;
 *                      &lt;id&gt;default-compile&lt;/id&gt;
 *                      &lt;phase&gt;compile&lt;/phase&gt;
 *                      &lt;goals&gt;
 *                          &lt;goal&gt;compile&lt;/goal&gt;
 *                      &lt;/goals&gt;
 *                      &lt;configuration&gt;
 *                          &hellip;
 *                          &lt;annotationProcessors&gt;
 *                              &lt;annotationProcessor&gt;org.tquadrat.foundation.i18n.ap.I18NAnnotationProcessor&lt;/annotationProcessor&gt;
 *                              &hellip;
 *                          &lt;/annotationProcessors&gt;
 *                          &hellip;
 *                          &lt;compilerArgs&gt;
 *                              &hellip;
 *                              &lt;arg&gt;-Aorg.tquadrat.foundation.ap.maven.goal=compile&lt;/arg&gt;
 *                              &hellip;
 *                          &lt;/compilerArgs&gt;
 *                          &hellip;
 *                      &lt;/configuration&gt;
 *                      &hellip;
 *                  &lt;/execution&gt;
 *                  &hellip;
 *              &lt;/executions&gt;
 *              &hellip;
 *          &lt;/plugin&gt;
 *          &hellip;
 *      &lt;/plugins&gt;
 *      &hellip;
 *  &lt;/build&gt;</code></pre></blockquote>
 *
 *  <h2>Configure Gradle to use the Annotation Processor</h2>
 *  <p>To add the annotation processor to the Gradle build configuration, place
 *  the following line to the respective {@code build.gradle} file:</p>
 *  <blockquote><pre><code>  &hellip;
 *  dependencies {
 *      implementation 'org:tquadrat.library:org.tquadrat.foundation.i18n:&lt;<i>proper version</i>&gt;' // For an application
 *      api 'org:tquadrat.library:org.tquadrat.foundation.i18n:&lt;<i>proper version</i>&gt;' // For a library
 *
 *      annotationProcessor 'org:tquadrat.library:org.tquadrat.foundation.i18n.ap:&lt;<i>proper version</i>&gt;'
 *  }
 *  </code></pre></blockquote>
 */

package org.tquadrat.foundation.i18n.ap;

/*
 *  End of File
 */