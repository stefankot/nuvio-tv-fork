package com.nuvio.tv.ainarrator

import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class ZonosArm32InstallerTest {
    @Test fun allowOwnersGithubReleaseButNotRandomDownloadHost() {
        assertTrue(Arm32GitHubSource.isApproved(
            URL("https://github.com/stefankot/nuvio-tv-fork/releases/download/zonos-arm32-v1/zonos3-worker-armeabi-v7a.apk"),
            false))
        assertFalse(Arm32GitHubSource.isApproved(
            URL("https://github.com/other-owner/nuvio-tv-fork/releases/download/v1/worker.apk"),
            false))
        assertFalse(Arm32GitHubSource.isApproved(
            URL("https://github.com/stefankot/nuvio-tv-fork/raw/main/worker.apk"),
            false))
        assertFalse(Arm32GitHubSource.isApproved(
            URL("http://github.com/stefankot/nuvio-tv-fork/releases/download/v1/worker.apk"),
            false))
        assertFalse(Arm32GitHubSource.isApproved(
            URL("https://192.168.1.12/releases/download/v1/worker.apk"),
            false))
        assertFalse(Arm32GitHubSource.isApproved(
            URL("https://release-assets.githubusercontent.com/example/worker.apk"),
            false))
    }

    @Test fun githubCanRedirectOnlyToApprovedOfficialReleaseCdn() {
        assertTrue(Arm32GitHubSource.isApproved(
            URL("https://release-assets.githubusercontent.com/asset?id=1"), true))
        assertTrue(Arm32GitHubSource.isApproved(
            URL("https://objects.githubusercontent.com/some-signed-blob"), true))
        assertFalse(Arm32GitHubSource.isApproved(URL("https://example.net/worker.apk"), true))
        assertFalse(Arm32GitHubSource.isApproved(URL("http://objects.githubusercontent.com/a"), true))
    }
}
