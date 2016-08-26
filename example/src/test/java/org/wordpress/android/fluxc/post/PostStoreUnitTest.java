package org.wordpress.android.fluxc.post;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.PostStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(RobolectricTestRunner.class)
public class PostStoreUnitTest {
    private PostStore mPostStore = new PostStore(new Dispatcher(), Mockito.mock(PostRestClient.class),
            Mockito.mock(PostXMLRPCClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new SingleStoreWellSqlConfigForTests(appContext, PostModel.class);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testInsertNullPost() {
        assertEquals(0, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(null));

        assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        PostModel postModel = new PostModel();
        postModel.setRemotePostId(42);
        PostModel result = PostSqlUtils.insertPostForResult(postModel);

        assertEquals(1, PostTestUtils.getPostsCount());
        assertEquals(42, PostTestUtils.getPosts().get(0).getRemotePostId());
        assertEquals(postModel, result);
    }

    @Test
    public void testInsertWithLocalChanges() {
        PostModel postModel = PostTestUtils.generateSampleUploadedPost();
        postModel.setIsLocallyChanged(true);
        PostSqlUtils.insertPostForResult(postModel);

        String newTitle = "A different title";
        postModel.setTitle(newTitle);

        assertEquals(0, PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(postModel));
        assertEquals("A test post", PostTestUtils.getPosts().get(0).getTitle());

        assertEquals(1, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel));
        assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());
    }

    @Test
    public void testInsertWithoutLocalChanges() {
        PostModel postModel = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(postModel);

        String newTitle = "A different title";
        postModel.setTitle(newTitle);

        assertEquals(1, PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(postModel));
        assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());

        newTitle = "Another different title";
        postModel.setTitle(newTitle);

        assertEquals(1, PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel));
        assertEquals(newTitle, PostTestUtils.getPosts().get(0).getTitle());
    }

    @Test
    public void testGetPostsForSite() {
        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setLocalSiteId(8);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        SiteModel site1 = new SiteModel();
        site1.setId(uploadedPost1.getLocalSiteId());

        SiteModel site2 = new SiteModel();
        site2.setId(uploadedPost2.getLocalSiteId());

        assertEquals(2, PostTestUtils.getPostsCount());

        assertEquals(1, mPostStore.getPostsCountForSite(site1));
        assertEquals(1, mPostStore.getPostsCountForSite(site2));
    }

    @Test
    public void testGetPublishedPosts() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(localDraft);

        assertEquals(2, PostTestUtils.getPostsCount());
        assertEquals(2, mPostStore.getPostsCountForSite(site));

        assertEquals(1, mPostStore.getUploadedPostsCountForSite(site));
    }

    @Test
    public void testGetPostByLocalId() {
        PostModel post = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(post);

        assertEquals(post, mPostStore.getPostByLocalPostId(post.getId()));
    }

    @Test
    public void testDeleteUploadedPosts() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setRemotePostId(9);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(localDraft);

        PostModel locallyChangedPost = PostTestUtils.generateSampleLocallyChangedPost();
        PostSqlUtils.insertPostForResult(locallyChangedPost);

        assertEquals(4, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deleteUploadedPostsForSite(site, false);

        assertEquals(2, mPostStore.getPostsCountForSite(site));
    }

    @Test
    public void testDeletePost() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel uploadedPost1 = PostTestUtils.generateSampleUploadedPost();
        PostSqlUtils.insertPostForResult(uploadedPost1);

        PostModel uploadedPost2 = PostTestUtils.generateSampleUploadedPost();
        uploadedPost2.setRemotePostId(9);
        PostSqlUtils.insertPostForResult(uploadedPost2);

        PostModel localDraft = PostTestUtils.generateSampleLocalDraftPost();
        PostSqlUtils.insertPostForResult(localDraft);

        PostModel locallyChangedPost = PostTestUtils.generateSampleLocallyChangedPost();
        PostSqlUtils.insertPostForResult(locallyChangedPost);

        assertEquals(4, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deletePost(uploadedPost1);

        assertEquals(null, mPostStore.getPostByLocalPostId(uploadedPost1.getId()));
        assertEquals(3, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deletePost(uploadedPost2);
        PostSqlUtils.deletePost(localDraft);

        assertNotEquals(null, mPostStore.getPostByLocalPostId(locallyChangedPost.getId()));
        assertEquals(1, mPostStore.getPostsCountForSite(site));

        PostSqlUtils.deletePost(locallyChangedPost);

        assertEquals(null, mPostStore.getPostByLocalPostId(locallyChangedPost.getId()));
        assertEquals(0, mPostStore.getPostsCountForSite(site));
        assertEquals(0, PostTestUtils.getPostsCount());
    }

    @Test
    public void testPostAndPageSeparation() {
        SiteModel site = new SiteModel();
        site.setId(6);

        PostModel post = new PostModel();
        post.setLocalSiteId(6);
        post.setRemotePostId(42);
        PostSqlUtils.insertPostForResult(post);

        PostModel page = new PostModel();
        page.setIsPage(true);
        page.setLocalSiteId(6);
        page.setRemotePostId(43);
        PostSqlUtils.insertPostForResult(page);

        assertEquals(2, PostTestUtils.getPostsCount());

        assertEquals(1, mPostStore.getPostsCountForSite(site));
        assertEquals(1, mPostStore.getPagesCountForSite(site));

        assertEquals(false, PostTestUtils.getPosts().get(0).isPage());
        assertEquals(true, PostTestUtils.getPosts().get(1).isPage());
    }
}
