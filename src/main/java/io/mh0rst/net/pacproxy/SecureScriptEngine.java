/*
 * PacProxy - A HTTP proxy driven by a PAC file.
 * Copyright (C) 2017 Moritz Horstmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.mh0rst.net.pacproxy;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * Provides a nashorn scripting engine only exposing the PacFunctions class.
 */
@SuppressWarnings("restriction")
public class SecureScriptEngine {

    static ScriptEngine newNashornEngine() {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        ScriptEngine scriptEngine = factory.getScriptEngine(new PacClassFilter());
        Bindings bindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.remove("exit");
        bindings.remove("quit");
        return scriptEngine;
    }

    private static class PacClassFilter implements ClassFilter {

        @Override
        public boolean exposeToScripts(String clazz) {
            return PacFunctions.class.getName().equals(clazz);
        }

    }
}
