/*
 * This file is part of Pustefix.
 *
 * Pustefix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Pustefix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Pustefix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.schlund.pfixcore.generator.casters;
import java.util.ArrayList;

import org.pustefixframework.generated.CoreStatusCodes;

import de.schlund.pfixcore.generator.IWrapperParamCaster;
import de.schlund.pfixcore.generator.SimpleCheck;
import de.schlund.pfixxml.RequestParam;
import de.schlund.util.statuscodes.StatusCode;
import de.schlund.util.statuscodes.StatusCodeHelper;

/**
 * ToByte.java
 *
 *
 * Created: Wed Mar 23 14:58:36 2005
 *
 * @author <a href="mailto:tanjev.stuhr@schlund.de">Tanjev Stuhr</a>
 *
 *
 */

public class ToByte extends SimpleCheck implements IWrapperParamCaster {
    private Byte[]    value = null;
    private StatusCode scode;

    public ToByte() {
        scode = CoreStatusCodes.CASTER_ERR_TO_BYTE;
    }
    
    public void setScodeCastError(String fqscode) {
        scode = StatusCodeHelper.getStatusCodeByName(fqscode);
    }

    public Object[] getValue() {
        return value;
    }
    
    public void castValue(RequestParam[] param) {
        reset();
        ArrayList<Byte> out = new ArrayList<Byte>();
        String par;
        Byte  val;
        for (int i = 0; i < param.length; i++) {
            try {
                par = param[i].getValue().replace(',', '.');
                val = new Byte(par);
                out.add(val);
            } catch (NumberFormatException e) {
                addSCode(scode);
                break;
            }
        }
        if (!errorHappened()) {
            value = out.toArray(new Byte[] {});
        }
    }
    
}// ToByte
