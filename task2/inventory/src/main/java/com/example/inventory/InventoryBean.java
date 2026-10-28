package com.example.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.query.aggregation.TermsAggregationQuery;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

public class InventoryBean
    implements ScriptBean
{
    // Each content project is stored in its own repository, named com.enonic.cms.<project>.
    private static final String REPOSITORY_PREFIX = "com.enonic.cms.";

    private static final String BY_TYPE = "byType";

    // A terms aggregation returns only 10 buckets by default, which would silently drop content types. 0 means all.
    private static final int ALL_TYPES = 0;

    private Supplier<ContentService> contentServiceSupplier;

    @Override
    public void initialize( final BeanContext context )
    {
        // Looked up lazily: the service is only fetched when get() is called in count().
        this.contentServiceSupplier = context.getService( ContentService.class );
    }

    /**
     * Counts all descendants of the path, in total and per content type.
     *
     * @return the inventory, or {@code null} if the path does not exist
     */
    public Object count( final String project, final String branch, final String path )
    {
        final Context context = contextFor( project, branch );

        return context.callWith( () -> inventoryOf( project, branch, path ) );
    }

    private Context contextFor( final String project, final String branch )
    {
        return ContextBuilder.from( ContextAccessor.current() )
            .repositoryId( REPOSITORY_PREFIX + project )
            .branch( branch )
            .build();
    }

    private InventoryResult inventoryOf( final String project, final String branch, final String path )
    {
        final ContentService contentService = contentServiceSupplier.get();
        final ContentPath contentPath = ContentPath.from( path );

        if ( !contentService.contentExists( contentPath ) )
        {
            return null;
        }

        final FindContentIdsByQueryResult result = contentService.find( descendantsGroupedByType( contentPath ) );

        return new InventoryResult( project, branch, path, result.getTotalHits(), countsByType( result ) );
    }

    private ContentQuery descendantsGroupedByType(final ContentPath path)
    {
        final TermsAggregationQuery groupByType = TermsAggregationQuery.create( BY_TYPE )
            .fieldName( ContentPropertyNames.TYPE )
            .size( ALL_TYPES )
            .build();

        return ContentQuery.create()
            .parentPath(path)
            .recursive(true)
            .size(0)
            .aggregationQuery( groupByType )
            .build();
    }

    private Map<String, Long> countsByType( final FindContentIdsByQueryResult result )
    {
        final BucketAggregation aggregation = (BucketAggregation) result.getAggregations().get( BY_TYPE );

        final Map<String, Long> byType = new HashMap<>();
        for ( final Bucket bucket : aggregation.getBuckets() )
        {
            byType.put( bucket.getKey(), bucket.getDocCount() );
        }
        return byType;
    }
}
