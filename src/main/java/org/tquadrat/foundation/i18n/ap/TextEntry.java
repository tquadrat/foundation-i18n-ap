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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Locale;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;

/**
 *  Entries for resource bundle properties files.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TextEntry.java 1130 2024-05-05 16:16:09Z tquadrat $
 *  @since 0.1.0
 *
 *  @param  key The resource bundle key for the text or message.
 *  @param  isMessage   {@code true} if the entry is for a message,
 *      {@code false} if not.
 *  @param  locale  The locale for this translation of a text or a message.
 *  @param  description The description for the text or message.
 *  @param  text    The text or message itself for the given locale;
 *      newlines will be replaced by the respective escape sequence
 *      (&quot;{@code \n}&quot;).
 *  @param  className   The fully qualified name of the class that defines
 *      the text or message.
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: TextEntry.java 1130 2024-05-05 16:16:09Z tquadrat $" )
@API( status = INTERNAL, since = "0.1.0" )
public record TextEntry( String key, boolean isMessage, Locale locale, String description, String text, String className )
{
    /**
     *  Creates a new instance of {@code TexEntry}.
     *
     *  @param  key The resource bundle key for the text or message.
     *  @param  isMessage   {@code true} if the entry is for a message,
     *      {@code false} if not.
     *  @param  locale  The locale for this translation of a text or a message.
     *  @param  description The description for the text or message.
     *  @param  text    The text or message itself for the given locale;
     *      newlines will be replaced by the respective escape sequence
     *      (&quot;{@code \n}&quot;).
     *  @param  className   The fully qualified name of the class that defines
     *      the text or message.
     */
    @SuppressWarnings( "ConstructorWithTooManyParameters" )
    public TextEntry
    {
        text = text.replace( "\n", "\\n" );
    }   //  TextEntry
}
//  record TextEntry

/*
 *  End of File
 */