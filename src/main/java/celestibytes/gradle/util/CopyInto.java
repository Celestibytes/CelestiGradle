
package celestibytes.gradle.util;

import celestibytes.gradle.delayed.DelayedString;

import com.google.common.base.Strings;

import org.gradle.api.file.CopySpec;

import groovy.lang.Closure;

import java.util.HashMap;

public class CopyInto extends Closure<Object>
{
    private static final long serialVersionUID = 5765302350387129352L;
    
    private String dir;
    private String[] filters;
    private HashMap<String, Object> expands = new HashMap<String, Object>();
    
    public CopyInto(String dir)
    {
        super(null);
        this.dir = dir;
        filters = new String[] {};
    }
    
    public CopyInto(String dir, String... filters)
    {
        super(null);
        this.dir = dir;
        this.filters = filters;
    }
    
    public CopyInto addExpand(String key, String value)
    {
        expands.put(key, value);
        return this;
    }
    
    public CopyInto addExpand(String key, DelayedString value)
    {
        expands.put(key, value);
        return this;
    }
    
    @Override
    public Object call(Object... args)
    {
        CopySpec spec = (CopySpec) getDelegate();
        
        // do filters
        for (String s : filters)
        {
            if (s.startsWith("!"))
            {
                spec.exclude(s.substring(1));
            }
            else
            {
                spec.include(s);
            }
        }
        
        // expands
        
        if (!expands.isEmpty())
        {
            spec.expand(expands);
        }
        
        if (!Strings.isNullOrEmpty(dir))
        {
            spec.into(dir);
        }
        
        return null;
    }
};
