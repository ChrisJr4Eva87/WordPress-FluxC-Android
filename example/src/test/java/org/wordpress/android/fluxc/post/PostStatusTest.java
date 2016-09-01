package org.wordpress.android.fluxc.post;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.utils.DateTimeUtils;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PostStatusTest {
    @Test
    public void testPostStatusFromPost() {
        PostModel post = new PostModel();
        post.setStatus("publish");

        // Test published post with past date
        post.setDateCreated(DateTimeUtils.iso8601UTCFromDate(new Date()));
        assertEquals(PostStatus.PUBLISHED, PostStatus.fromPost(post));

        // Test "published" post with future date
        post.setDateCreated(DateTimeUtils.iso8601UTCFromDate(new Date(System.currentTimeMillis() + 500000)));
        assertEquals(PostStatus.SCHEDULED, PostStatus.fromPost(post));
    }

    @Test
    public void testPostStatusFromPostWithNoDateCreated() {
        PostModel post = new PostModel();
        post.setStatus("publish");

        assertEquals(PostStatus.PUBLISHED, PostStatus.fromPost(post));
    }
}
