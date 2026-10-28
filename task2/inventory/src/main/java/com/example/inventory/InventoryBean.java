package com.example.inventory;

import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

import java.util.Map;
import java.util.function.Supplier;

public class InventoryBean implements ScriptBean
{
    private static String PACKAGE = "com.enonic.cms.";
    private Supplier<ContentService> contentServiceSupplier;
    @Override
    public void initialize( final BeanContext context )
    {
        this.contentServiceSupplier = context.getService( ContentService.class );
    }

    public Object count(String project, String branch, String path) {
        //Set up context
        Context context = ContextBuilder.from(ContextAccessor.current())
                .repositoryId(PACKAGE + project)
                .branch(branch)
                .build();

        context.callWith(() -> {
            ContentService contentService = contentServiceSupplier.get();
            boolean pathExists = contentService.contentExists(ContentPath.from(path));
            return pathExists;
        });
        // Check if path exists or not first
        // If yes, run query to fetch grouped by content type, return count
        return Map.of( "exists", true );
    }
}
