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

package org.pustefixframework.webservices.jsonws;

import org.pustefixframework.webservices.json.JSONValue;

/**
 * JSONObject which can be initialized with arbitrary JSON strings.
 * It can be used by custom serializers to create JSON representations
 * of JS objects which don't base on the supported JS base datatypes
 * 
 * @author mleidig@schlund.de
 */
public class CustomJSONObject implements JSONValue {

    String jsonStr;
    
	public CustomJSONObject(String jsonStr) {
	    this.jsonStr=jsonStr;
	}

    public String toJSONString() {
        return jsonStr;
    }

}
