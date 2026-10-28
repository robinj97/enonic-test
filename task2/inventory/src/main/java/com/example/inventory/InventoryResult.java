package com.example.inventory;

import java.util.Map;

import com.enonic.xp.script.serializer.MapGenerator;
import com.enonic.xp.script.serializer.MapSerializable;

public record InventoryResult(String project, String branch, String path, long total, Map<String, Long> byType)
    implements MapSerializable
{
    @Override
    public void serialize( final MapGenerator gen )
    {
        gen.value( "project", project );
        gen.value( "branch", branch );
        gen.value( "path", path );
        gen.value( "total", total );
        gen.map( "byType" );
        byType.forEach( gen::value );
        gen.end();
    }
}
