package org.wordpress.android.fluxc.release;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.example.test.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestJetpack extends ReleaseStack_Base {
    enum TestEvents {
        NONE,
        FETCHED_WPCOM_THEMES,
        FETCHED_INSTALLED_THEMES,
        FETCHED_CURRENT_THEME,
        ACTIVATED_THEME,
        INSTALLED_THEME,
        DELETED_THEME,
        REMOVED_THEME,
        REMOVED_SITE_THEMES,
        SITE_CHANGED,
        SITE_REMOVED
    }

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;
    private ThemeModel mCurrentTheme;
    private ThemeModel mActivatedTheme;
    private ThemeModel mInstalledTheme;
    private ThemeModel mDeletedTheme;

    private TestEvents mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
        mCurrentTheme = null;
        mActivatedTheme = null;
    }

    public void testFetchInstalledThemes() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // verify that installed themes list is empty first
        assertTrue(mThemeStore.getThemesForSite(jetpackSite).size() == 0);

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // verify themes are available for the site
        assertTrue(mThemeStore.getThemesForSite(jetpackSite).size() > 0);

        signOutWPCom();
    }

    public void testFetchCurrentTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // fetch active theme
        fetchCurrentThemes(jetpackSite);
        assertNotNull(mCurrentTheme);

        signOutWPCom();
    }

    public void testActivateTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // make sure there are at least 2 themes, one that's active and one that will be activated
        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        assertTrue(themes.size() > 1);

        // fetch active theme
        fetchCurrentThemes(jetpackSite);
        assertNotNull(mCurrentTheme);

        // select a different theme to activate
        ThemeModel themeToActivate = mCurrentTheme.getThemeId().equals(themes.get(0).getThemeId())
                ? themes.get(1) : themes.get(0);
        assertNotNull(themeToActivate);

        // activate it
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACTIVATED_THEME;
        SiteThemePayload payload = new SiteThemePayload(jetpackSite, themeToActivate);
        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // mActivatedTheme is set in onThemeActivated
        assertNotNull(mActivatedTheme);
        assertEquals(mActivatedTheme.getThemeId(), themeToActivate.getThemeId());

        signOutWPCom();
    }

    public void testInstallTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        final String themeId = "edin-wpcom";
        final ThemeModel themeToInstall = new ThemeModel();
        themeToInstall.setName("Edin");
        themeToInstall.setThemeId(themeId);

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        // make sure installed themes were successfully fetched
        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        assertFalse(themes.isEmpty());

        // delete edin before attempting to install
        if (listContainsThemeWithId(themes, themeId)) {
            // find local ThemeModel with matching themeId for delete call
            ThemeModel listTheme = getThemeFromList(themes, themeId);
            assertNotNull(listTheme);

            // delete existing theme from site
            themeToInstall.setId(listTheme.getId());
            deleteTheme(jetpackSite, themeToInstall);

            // mDeletedTheme is set in onThemeDeleted
            assertNotNull(mDeletedTheme);
            assertEquals(themeId, mDeletedTheme.getThemeId());

            // make sure theme is no longer available for site (delete was successful)
            assertFalse(listContainsThemeWithId(mThemeStore.getThemesForSite(jetpackSite), themeId));
            mActivatedTheme = null;
        }

        // install the theme
        installTheme(jetpackSite, themeToInstall);
        assertTrue(listContainsThemeWithId(mThemeStore.getThemesForSite(jetpackSite), themeId));

        signOutWPCom();
    }

    public void testDeleteTheme() throws InterruptedException {
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();
        assertTrue(mThemeStore.getThemesForSite(jetpackSite).isEmpty());

        final String themeId = "edin-wpcom";
        final ThemeModel themeToDelete = new ThemeModel();
        themeToDelete.setName("Edin");
        themeToDelete.setThemeId(themeId);

        // fetch installed themes
        fetchInstalledThemes(jetpackSite);

        List<ThemeModel> themes = mThemeStore.getThemesForSite(jetpackSite);
        assertFalse(themes.isEmpty());
        ThemeModel listTheme = getThemeFromList(themes, themeId);

        // install edin before attempting to delete
        if (listTheme == null) {
            installTheme(jetpackSite, themeToDelete);

            // mInstalledTheme is set in onThemeInstalled
            assertNotNull(mInstalledTheme);
            assertEquals(themeId, mInstalledTheme.getThemeId());

            // make sure theme is available for site (install was successful)
            listTheme = getThemeFromList(mThemeStore.getThemesForSite(jetpackSite), themeId);
            assertNotNull(listTheme);
        }

        // fetch active theme
        fetchCurrentThemes(jetpackSite);

        // if Edin is active update site's active theme to something else
        if (themeId.equals(mCurrentTheme.getThemeId())) {
            mCountDownLatch = new CountDownLatch(1);
            mNextEvent = TestEvents.ACTIVATED_THEME;
            ThemeModel themeToActivate = getOtherTheme(themes, themeId);
            assertNotNull(themeToActivate);
            SiteThemePayload payload = new SiteThemePayload(jetpackSite, themeToActivate);
            mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(payload));
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }

        themeToDelete.setId(listTheme.getId());
        deleteTheme(jetpackSite, themeToDelete);
        assertFalse(listContainsThemeWithId(mThemeStore.getThemesForSite(jetpackSite), themeId));

        signOutWPCom();
    }

    public void testRemoveTheme() throws InterruptedException {
        // sign in and fetch WP.com themes and installed themes
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // verify initial state, no themes in store
        assertEquals(0, mThemeStore.getWpComThemes().size());
        assertEquals(0, mThemeStore.getThemesForSite(jetpackSite).size());

        // fetch themes for site and WP.com themes
        fetchInstalledThemes(jetpackSite);
        fetchWpComThemes();

        final List<ThemeModel> wpComThemes = mThemeStore.getWpComThemes();
        final List<ThemeModel> installedThemes = mThemeStore.getThemesForSite(jetpackSite);
        assertTrue(installedThemes.size() > 0);
        assertTrue(wpComThemes.size() > 0);

        // remove a theme from each and verify
        final ThemeModel wpComRemove = wpComThemes.get(0);
        final ThemeModel installedRemove = installedThemes.get(0);
        removeTheme(wpComRemove);
        assertEquals(wpComThemes.size() - 1, mThemeStore.getWpComThemes().size());
        removeTheme(installedRemove);
        assertEquals(installedThemes.size() - 1, mThemeStore.getThemesForSite(jetpackSite).size());

        // sign out
        signOutWPCom();
    }

    public void testRemoveSiteThemes() throws InterruptedException {
        // sign in and fetch WP.com themes and installed themes
        final SiteModel jetpackSite = signIntoWpComAccountWithJetpackSite();

        // verify initial state, no themes in store
        assertEquals(0, mThemeStore.getWpComThemes().size());
        assertEquals(0, mThemeStore.getThemesForSite(jetpackSite).size());

        // fetch themes for site and WP.com themes
        fetchInstalledThemes(jetpackSite);
        fetchWpComThemes();

        final int wpComThemesCount = mThemeStore.getWpComThemes().size();
        assertTrue(wpComThemesCount > 0);
        assertTrue(mThemeStore.getThemesForSite(jetpackSite).size() > 0);

        // remove the site's themes
        removeSiteThemes(jetpackSite);

        // verify they are removed and that WP.com themes are still there
        assertEquals(wpComThemesCount, mThemeStore.getWpComThemes().size());
        assertEquals(0, mThemeStore.getThemesForSite(jetpackSite).size());

        // sign out
        signOutWPCom();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteThemesChanged(ThemeStore.OnSiteThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.origin == ThemeAction.FETCH_INSTALLED_THEMES) {
            assertEquals(mNextEvent, TestEvents.FETCHED_INSTALLED_THEMES);
            mCountDownLatch.countDown();
        } else if (event.origin == ThemeAction.REMOVE_SITE_THEMES) {
            assertEquals(mNextEvent, TestEvents.REMOVED_SITE_THEMES);
            mCountDownLatch.countDown();
        } else {
            throw new AssertionError("Unexpected event occurred from origin: " + event.origin);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onWpComThemesChanged(ThemeStore.OnWpComThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.FETCHED_WPCOM_THEMES);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(ThemeStore.OnCurrentThemeFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        assertTrue(mNextEvent == TestEvents.FETCHED_CURRENT_THEME);
        assertNotNull(event.theme);
        mCurrentTheme = event.theme;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mNextEvent == TestEvents.ACTIVATED_THEME);
        mActivatedTheme = event.theme;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeInstalled(ThemeStore.OnThemeInstalled event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mNextEvent == TestEvents.INSTALLED_THEME);
        mInstalledTheme = event.theme;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeDeleted(ThemeStore.OnThemeDeleted event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mNextEvent == TestEvents.DELETED_THEME);
        mDeletedTheme = event.theme;
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemeRemoved(ThemeStore.OnThemeRemoved event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.REMOVED_THEME, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(AccountStore.OnAccountChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(SiteStore.OnSiteRemoved event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private SiteModel signIntoWpComAccountWithJetpackSite() throws InterruptedException {
        // sign into a WP.com account with a Jetpack site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        // verify Jetpack site is available
        final SiteModel jetpackSite = getJetpackSite();
        assertNotNull(jetpackSite);
        return jetpackSite;
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload = new AccountStore.AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSiteStore.getSitesCount() > 0);
    }

    private void signOutWPCom() throws InterruptedException {
        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchWpComThemes() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_WPCOM_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchInstalledThemes(@NonNull SiteModel jetpackSite) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_INSTALLED_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(jetpackSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchCurrentThemes(@NonNull SiteModel jetpackSite) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_CURRENT_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(jetpackSite));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void removeTheme(@NonNull ThemeModel theme) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.REMOVED_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newRemoveThemeAction(theme));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void removeSiteThemes(@NonNull SiteModel site) throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.REMOVED_SITE_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newRemoveSiteThemesAction(site));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void installTheme(SiteModel site, ThemeModel theme) throws InterruptedException {
        SiteThemePayload install = new SiteThemePayload(site, theme);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.INSTALLED_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(install));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void deleteTheme(SiteModel site, ThemeModel theme) throws InterruptedException {
        SiteThemePayload delete = new SiteThemePayload(site, theme);
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.DELETED_THEME;
        mDispatcher.dispatch(ThemeActionBuilder.newDeleteThemeAction(delete));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private SiteModel getJetpackSite() {
        for (SiteModel site : mSiteStore.getSites()) {
            if (site.isJetpackConnected()) {
                return site;
            }
        }
        return null;
    }

    private ThemeModel getThemeFromList(List<ThemeModel> list, String themeId) {
        for (ThemeModel theme : list) {
            if (themeId.equals(theme.getThemeId())) {
                return theme;
            }
        }
        return null;
    }

    private boolean listContainsThemeWithId(List<ThemeModel> list, String themeId) {
        return getThemeFromList(list, themeId) != null;
    }

    private ThemeModel getOtherTheme(List<ThemeModel> themes, String idToIgnore) {
        for (ThemeModel theme : themes) {
            if (!idToIgnore.equals(theme.getThemeId())) {
                return theme;
            }
        }
        return null;
    }
}
