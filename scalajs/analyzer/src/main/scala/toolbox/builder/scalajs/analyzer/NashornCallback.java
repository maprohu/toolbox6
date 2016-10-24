package toolbox.builder.scalajs.analyzer;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * Created by pappmar on 24/10/2016.
 */
public interface NashornCallback {
    Object apply(ScriptObjectMirror value);
}
