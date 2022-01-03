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

import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apiguardian.api.API;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Tests whether the DTD for the additional texts can be found as expected.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 */
@ClassVersion( sourceVersion = "$Id: TestFindDTD.java 922 2021-05-23 18:32:17Z tquadrat $" )
@API( status = STABLE, since = "0.1.0" )
@DisplayName( "org.tquadrat.foundation.i18n.ap.TestFindDTD" )
public class TestFindDTD extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Tries to find the DTD for the additional texts.
     *
     *  @throws Exception   Something went wrong unexpectedly.
     */
    @Test
    final void testFindDTD() throws Exception
    {
        skipThreadTest();

        final var dtdName = "AdditionalText.dtd";

        final var url1 = getClass().getResource( "/" + dtdName );
        assertNotNull( url1 );
        assertTrue( url1.toString().contains( dtdName ) );

        final var url2 = getClass().getClassLoader().getResource( dtdName );
        assertNotNull( url2 );
        assertTrue( url2.toString().contains( dtdName ) );

        assertEquals( url1, url2 );
    }   //  testFindDTD()
}
//  class TestFindDTD

/*
 *  End of File
 */