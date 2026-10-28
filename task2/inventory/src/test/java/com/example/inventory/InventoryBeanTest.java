package com.example.inventory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.enonic.xp.aggregation.Aggregations;
import com.enonic.xp.aggregation.Bucket;
import com.enonic.xp.aggregation.BucketAggregation;
import com.enonic.xp.aggregation.Buckets;
import com.enonic.xp.branch.Branch;
import com.enonic.xp.content.ContentIds;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentQuery;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.FindContentIdsByQueryResult;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.repository.RepositoryId;
import com.enonic.xp.script.bean.BeanContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryBeanTest
{
    private static final String PROJECT = "default";

    private static final String BRANCH = "draft";

    private static final String PATH = "/mysite";

    private ContentService contentService;

    private InventoryBean inventoryBean;

    @BeforeEach
    void setUp()
    {
        contentService = mock( ContentService.class );

        final BeanContext beanContext = mock( BeanContext.class );
        when( beanContext.getService( ContentService.class ) ).thenReturn( () -> contentService );

        inventoryBean = new InventoryBean();
        inventoryBean.initialize( beanContext );
    }

    @Test
    void count_returnsTotalAndCountPerType_forKnownContent()
    {
        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenReturn( true );
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn(
            findResultWithTypeCounts( 7, Map.of( "base:folder", 4L, "portal:site", 1L, "portal:template-folder", 2L ) ) );

        final InventoryResult result = (InventoryResult) inventoryBean.count( PROJECT, BRANCH, PATH );

        assertEquals( 7, result.total() );
        assertEquals( 4L, result.byType().get( "base:folder" ) );
        assertEquals( 1L, result.byType().get( "portal:site" ) );
        assertEquals( 2L, result.byType().get( "portal:template-folder" ) );
        assertEquals( 3, result.byType().size() );
    }

    @Test
    void count_returnsRequestedProjectBranchAndPath()
    {
        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenReturn( true );
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn( findResultWithTypeCounts( 0, Map.of() ) );

        final InventoryResult result = (InventoryResult) inventoryBean.count( PROJECT, BRANCH, PATH );

        assertEquals( PROJECT, result.project() );
        assertEquals( BRANCH, result.branch() );
        assertEquals( PATH, result.path() );
    }

    @Test
    void count_returnsZeroTotalAndNoTypes_whenPathHasNoDescendants()
    {
        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenReturn( true );
        when( contentService.find( any( ContentQuery.class ) ) ).thenReturn( findResultWithTypeCounts( 0, Map.of() ) );

        final InventoryResult result = (InventoryResult) inventoryBean.count( PROJECT, BRANCH, PATH );

        assertEquals( 0, result.total() );
        assertEquals( 0, result.byType().size() );
    }

    @Test
    void count_returnsNull_whenPathDoesNotExist()
    {
        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenReturn( false );

        final Object result = inventoryBean.count( PROJECT, BRANCH, PATH );

        assertNull( result );
    }

    @Test
    void count_doesNotSearch_whenPathDoesNotExist()
    {
        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenReturn( false );

        inventoryBean.count( PROJECT, BRANCH, PATH );

        verify( contentService, never() ).find( any( ContentQuery.class ) );
    }

    @Test
    void count_checksPathExistenceInsideRequestedRepositoryAndBranch()
    {
        final AtomicReference<RepositoryId> repositoryDuringCall = new AtomicReference<>();
        final AtomicReference<Branch> branchDuringCall = new AtomicReference<>();

        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenAnswer( invocation -> {
            repositoryDuringCall.set( ContextAccessor.current().getRepositoryId() );
            branchDuringCall.set( ContextAccessor.current().getBranch() );
            return false;
        } );

        inventoryBean.count( "myproject", "master", PATH );

        assertEquals( RepositoryId.from( "com.enonic.cms.myproject" ), repositoryDuringCall.get() );
        assertEquals( Branch.from( "master" ), branchDuringCall.get() );
    }

    @Test
    void count_searchesInsideRequestedRepositoryAndBranch()
    {
        final AtomicReference<RepositoryId> repositoryDuringCall = new AtomicReference<>();
        final AtomicReference<Branch> branchDuringCall = new AtomicReference<>();

        when( contentService.contentExists( ContentPath.from( PATH ) ) ).thenReturn( true );
        when( contentService.find( any( ContentQuery.class ) ) ).thenAnswer( invocation -> {
            repositoryDuringCall.set( ContextAccessor.current().getRepositoryId() );
            branchDuringCall.set( ContextAccessor.current().getBranch() );
            return findResultWithTypeCounts( 0, Map.of() );
        } );

        inventoryBean.count( "myproject", "master", PATH );

        assertEquals( RepositoryId.from( "com.enonic.cms.myproject" ), repositoryDuringCall.get() );
        assertEquals( Branch.from( "master" ), branchDuringCall.get() );
    }

    /**
     * Builds what the search would return: a total, plus a "byType" aggregation with one bucket per content type.
     */
    private FindContentIdsByQueryResult findResultWithTypeCounts( final long total, final Map<String, Long> countPerType )
    {
        final Buckets.Builder buckets = Buckets.create();
        countPerType.forEach( ( type, count ) -> buckets.add( Bucket.create().key( type ).docCount( count ).build() ) );

        final BucketAggregation byTypeAggregation = new BucketAggregation.Builder( "byType" ).buckets( buckets.build() ).build();

        return FindContentIdsByQueryResult.create()
            .contents( ContentIds.empty() )
            .totalHits( total )
            .aggregations( Aggregations.create().add( byTypeAggregation ).build() )
            .build();
    }
}
